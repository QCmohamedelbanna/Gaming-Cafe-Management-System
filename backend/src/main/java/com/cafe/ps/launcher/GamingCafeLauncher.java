package com.cafe.ps.launcher;

import com.cafe.ps.PlaystationCafeApplication;
import com.cafe.ps.config.RuntimePaths;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Silent desktop entry point used by jpackage. It owns the process-wide lock,
 * starts Spring in the same JVM, waits for the identity endpoint, and only
 * then opens the default browser.
 */
public final class GamingCafeLauncher {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);
    private static final DateTimeFormatter LOG_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static FileChannel instanceChannel;
    private static FileLock instanceLock;
    private static Path launcherLog;

    private GamingCafeLauncher() {
    }

    public static void main(String[] args) {
        Path dataDirectory = RuntimePaths.defaultDataDirectory()
                .toAbsolutePath()
                .normalize();
        launcherLog = prepareLauncherLog(dataDirectory);
        BootstrapCredentials bootstrapCredentials = null;

        try {
            try {
                acquireLock(dataDirectory.resolve("gaming-cafe.lock"));
            } catch (ExistingInstanceHandledException handled) {
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(
                    GamingCafeLauncher::releaseLock,
                    "gaming-cafe-launcher-shutdown"
            ));

            bootstrapCredentials = prepareBootstrapCredentials(dataDirectory);

            Probe initialProbe = probe();
            if (initialProbe == Probe.GAMING_CAFE) {
                openBrowser();
                releaseLock();
                return;
            }
            if (initialProbe == Probe.OCCUPIED) {
                int port = RuntimePaths.applicationPort();
                fail(
                        "Port " + port + " is already in use",
                        "Another application is using port " + port
                                + ". Close it or configure the conflict before starting Gaming Cafe."
                );
                releaseLock();
                return;
            }

            ConfigurableApplicationContext context = startApplication(args);
            if (!waitUntilReady(STARTUP_TIMEOUT)) {
                closeQuietly(context);
                fail(
                        "Gaming Cafe did not become ready",
                        "The application could not start. Review the launcher log at:\n"
                                + launcherLog
                );
                releaseLock();
                return;
            }

            openBrowser();
            if (bootstrapCredentials != null) {
                showBootstrapCredentials(bootstrapCredentials);
            }
        } catch (Exception exception) {
            writeLog("Startup failed: " + exception.getMessage(), exception);
            fail(
                    "Gaming Cafe could not start",
                    "Review the launcher log at:\n" + launcherLog
            );
            releaseLock();
        }
    }

    private static BootstrapCredentials prepareBootstrapCredentials(Path dataDirectory) {
        Path databasePath = RuntimePaths.databasePath(dataDirectory);
        if (!databaseNeedsBootstrap(databasePath)) return null;

        String configuredUsername = RuntimePaths.firstNonBlank(
                System.getProperty("ADMIN_USERNAME"),
                System.getenv("ADMIN_USERNAME")
        );
        String configuredPassword = RuntimePaths.firstNonBlank(
                System.getProperty("ADMIN_PASSWORD"),
                System.getenv("ADMIN_PASSWORD")
        );
        if (configuredUsername != null && configuredPassword != null) return null;

        String username = configuredUsername == null ? "admin" : configuredUsername;
        String password = configuredPassword == null
                ? generateBootstrapPassword()
                : configuredPassword;
        System.setProperty("app.default-admin-username", username);
        System.setProperty("app.default-admin-password", password);

        return configuredPassword == null
                ? new BootstrapCredentials(username, password)
                : null;
    }

    private static boolean databaseNeedsBootstrap(Path databasePath) {
        if (!Files.exists(databasePath)) return true;
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + databasePath
            ); Statement statement = connection.createStatement()) {
                try (ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM app_users"
                )) {
                    return !result.next() || result.getLong(1) == 0;
                }
            }
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "The SQLite runtime driver is missing from the packaged application",
                    exception
            );
        } catch (SQLException exception) {
            String message = exception.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("no such table")) {
                return true;
            }
            throw new IllegalStateException(
                    "Unable to inspect the Gaming Cafe database at " + databasePath,
                    exception
            );
        }
    }

    private static String generateBootstrapPassword() {
        byte[] random = new byte[24];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static void showBootstrapCredentials(BootstrapCredentials credentials) {
        try {
            JOptionPane.showMessageDialog(
                    null,
                    "Initial administrator created.\n\n"
                            + "Username: " + credentials.username() + "\n"
                            + "Password: " + credentials.password() + "\n\n"
                            + "Store this password securely and change it after signing in.\n"
                            + "It will not be shown again.",
                    "Gaming Cafe first-run setup",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            writeLog("Unable to show first-run administrator setup dialog", exception);
        }
    }

    private static ConfigurableApplicationContext startApplication(String[] args) {
        System.setProperty("app.launcher-managed", "true");
        List<String> applicationArgs = new ArrayList<>(Arrays.asList(args));
        if (applicationArgs.stream().noneMatch(
                argument -> argument.startsWith("--spring.profiles.active="))) {
            applicationArgs.add("--spring.profiles.active=prod");
        }

        writeLog("Starting Spring Boot on port " + RuntimePaths.applicationPort(), null);
        SpringApplication application = new SpringApplication(PlaystationCafeApplication.class);
        application.setAdditionalProfiles("prod");
        return application.run(applicationArgs.toArray(String[]::new));
    }

    private static void acquireLock(Path lockPath) throws IOException {
        Files.createDirectories(lockPath.toAbsolutePath().normalize().getParent());
        instanceChannel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        );
        try {
            instanceLock = instanceChannel.tryLock();
        } catch (OverlappingFileLockException ignored) {
            instanceLock = null;
        }
        if (instanceLock == null) {
            closeChannelOnly();
            waitForExistingInstance();
            throw new ExistingInstanceHandledException();
        }
    }

    private static void waitForExistingInstance() {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(STARTUP_TIMEOUT.toSeconds());
        while (System.nanoTime() < deadline) {
            if (probe() == Probe.GAMING_CAFE) {
                openBrowser();
                return;
            }
            sleep(250);
        }
        fail(
                "Gaming Cafe is already starting",
                "The existing process did not report ready within 60 seconds.\n"
                        + "Review the logs under the Gaming Cafe data directory."
        );
    }

    private static boolean waitUntilReady(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (probe() == Probe.GAMING_CAFE) return true;
            sleep(250);
        }
        return false;
    }

    private static Probe probe() {
        int port = RuntimePaths.applicationPort();
        URI statusUri = URI.create("http://127.0.0.1:" + port + "/api/system/status");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder(statusUri)
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 200
                    && response.body().contains("\"application\":\"gaming-cafe\"")) {
                return Probe.GAMING_CAFE;
            }
            return Probe.OCCUPIED;
        } catch (Exception ignored) {
            return isPortOpen(port) ? Probe.OCCUPIED : Probe.FREE;
        }
    }

    private static boolean isPortOpen(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void openBrowser() {
        int port = RuntimePaths.applicationPort();
        URI uri = URI.create("http://localhost:" + port + "/");
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("Desktop browser integration is unavailable");
            }
            Desktop.getDesktop().browse(uri);
            writeLog("Opened browser at " + uri, null);
        } catch (Exception exception) {
            writeLog("Unable to open the default browser at " + uri, exception);
            fail(
                    "Gaming Cafe is ready",
                    "Open this address in your browser:\n" + uri
            );
        }
    }

    private static Path prepareLauncherLog(Path dataDirectory) {
        Path primary = dataDirectory.resolve("logs").resolve("launcher.log");
        try {
            Files.createDirectories(primary.getParent());
            return primary;
        } catch (IOException ignored) {
            Path fallback = Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "GamingCafe",
                    "launcher.log"
            );
            try {
                Files.createDirectories(fallback.getParent());
            } catch (IOException ignoredFallback) {
                // The next write will be best effort only.
            }
            return fallback;
        }
    }

    private static void writeLog(String message, Throwable exception) {
        if (launcherLog == null) return;
        StringBuilder line = new StringBuilder()
                .append(LOG_TIME.format(LocalDateTime.now()))
                .append(" ")
                .append(message)
                .append(System.lineSeparator());
        if (exception != null) {
            line.append(exception).append(System.lineSeparator());
            for (StackTraceElement element : exception.getStackTrace()) {
                line.append("  at ").append(element).append(System.lineSeparator());
            }
        }
        try {
            Files.writeString(
                    launcherLog,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // There is no console in the packaged launcher; the dialog below
            // still tells the user how to reach the configured application.
        }
    }

    private static void fail(String title, String message) {
        writeLog(title + ": " + message.replace('\n', ' '), null);
        try {
            JOptionPane.showMessageDialog(
                    null,
                    message,
                    title,
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ignored) {
            // A headless environment cannot show a dialog; file logging is
            // still available for support.
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(ConfigurableApplicationContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception exception) {
                writeLog("Unable to close a failed Spring context", exception);
            }
        }
    }

    private static void releaseLock() {
        try {
            if (instanceLock != null && instanceLock.isValid()) instanceLock.release();
        } catch (IOException exception) {
            writeLog("Unable to release the application lock", exception);
        } finally {
            instanceLock = null;
            closeChannelOnly();
        }
    }

    private static void closeChannelOnly() {
        try {
            if (instanceChannel != null && instanceChannel.isOpen()) instanceChannel.close();
        } catch (IOException exception) {
            writeLog("Unable to close the application lock file", exception);
        } finally {
            instanceChannel = null;
        }
    }

    private enum Probe {
        GAMING_CAFE,
        OCCUPIED,
        FREE
    }

    private record BootstrapCredentials(String username, String password) {
    }

    private static final class ExistingInstanceHandledException extends RuntimeException {
        private ExistingInstanceHandledException() {
            super("An existing Gaming Cafe instance was handled by the launcher");
        }
    }
}
