# Gaming Cafe Windows client installation

Gaming Cafe is distributed as a desktop-style web application. The installer
contains the Spring Boot application, the React production bundle, and a
jpackage-generated Java runtime. The client does not need IntelliJ, Maven,
Node.js, npm, Git, Java, or the source repository.

## Client installation

1. Obtain `backend\target\installer\GamingCafeSetup.exe` from the release
   build machine.
2. Run `GamingCafeSetup.exe` and complete the Windows installer.
3. Start **Gaming Cafe** from the Start Menu or desktop shortcut.
4. The launcher starts Spring Boot with the `prod` profile, waits for
   `GET /api/system/status`, and opens the default browser at the local URL.
5. Sign in with the first-run administrator credentials shown once by the
   launcher. Change the password after the first login.

The installed application serves the React bundle from Spring Boot. It does
not start Vite or require a manually entered browser URL.

## Persistent files

The normal Windows location is:

```text
C:\ProgramData\GamingCafe\
    data\
        gaming-cafe.db
    config\
        gaming-cafe.properties
        gaming-cafe.properties.example
    logs\
        gaming-cafe.log
        launcher.log
        audit.log
    backup\
        gaming-cafe-YYYY-MM-DD-HHmmss.db
```

The installer writes application binaries under `C:\Program Files\GamingCafe`.
Business data, configuration, logs, and backups remain outside that directory,
so an upgrade replaces application files without replacing client data. The
launcher uses a per-user application-data fallback when ProgramData is not
writable by the account running the application. An explicitly configured
`GAMING_CAFE_DATA_DIR` is always honored.

The production configuration is loaded from
`config\gaming-cafe.properties`. On first production launch, Gaming Cafe
creates the directory and a safe `gaming-cafe.properties.example` template if
they do not exist. It never creates a fake secret and never replaces an
existing configuration file.

## Production configuration

Copy the example to `gaming-cafe.properties` and have the deployment operator
replace the placeholders privately before enabling Tuya:

```properties
device-control.mode=tuya
tuya.enabled=true
tuya.endpoint=https://openapi.tuyaeu.com
tuya.client-id=<Tuya project client id>
tuya.client-secret=<Tuya project client secret>
tuya.connect-timeout=2s
tuya.request-timeout=5s
tuya.max-attempts=2
```

The file is read by Spring Boot only. React never receives these values, and
the file must not be copied into the frontend bundle, committed to Git, or
included in a release archive. Keep the file readable by the account that
runs Gaming Cafe and restrict access to the deployment administrators and the
application account according to the shop's Windows policy.

Configuration precedence is:

1. Spring command-line and system properties.
2. Process environment variables such as `TUYA_CLIENT_ID`.
3. The external production properties file.
4. Packaged application defaults.

An absent production file leaves the application in safe mock mode. If
`device-control.mode=tuya` and `tuya.enabled=true` are explicitly selected
without both credentials, startup stops with a configuration error. The
launcher log identifies the support location; it never prints credential
values.

The root `.env` is a development-only file used by the documented
`mvn spring-boot:run` workflow. The installed client does not depend on it.

## First-run administrator

On a fresh SQLite database with no users, the launcher generates a cryptographically
random password unless `ADMIN_USERNAME` and `ADMIN_PASSWORD` were explicitly
provided by the deployment environment. It displays generated credentials one
time in a setup dialog after Spring Boot is ready. The password is hashed in
the database and is never written to application, launcher, audit, or Tuya
logs. Change it immediately after signing in and store it in the shop's
approved password-management process.

## Launch and port behavior

Production binds to `127.0.0.1:8080` by default. A second shortcut launch uses
the existing single-instance lock and opens the existing application instead
of starting another SQLite writer. If the port is occupied by another
process, the launcher does not kill it or choose a random port; it shows a
clear error and points to `%PROGRAMDATA%\GamingCafe\logs\launcher.log`.

## Tuya troubleshooting

Tuya calls stay in Spring Boot through `TuyaCloudClient` and
`DeviceControlService`. Token caching, HMAC signing, bounded retries, and
timeouts remain backend behavior. Hardware failures are recorded separately
from session, billing, payment, and inventory transactions.

Tuya Cloud can reject an otherwise correctly configured request when the shop
public IP is not authorized. An error equivalent to:

```text
your ip(...) don't have access to this API
```

means that the Tuya Developer Platform Authorization IP Allowlist may need to
include the shop's current public IP. A dynamic ISP address can change, so
the allowlist may need updating after the network address changes. Check the
project region, subscribed APIs, linked Smart Life account, device ID, and
allowlist before rotating credentials. Do not reset the Client Secret merely
because the current public IP was rejected.

The application diagnostic endpoint exposes only parsed device information:
`GET /api/devices/{id}/power/diagnostics`. It does not expose the Client
Secret, access token, HMAC signature, or local key.

## Backups and restore

An authenticated administrator can create a consistent live backup with:

```text
POST /api/system/backup
```

The result is written under `C:\ProgramData\GamingCafe\backup` using SQLite's
online backup API. The backup contains the SQLite business database only; it
does not contain the external Tuya secret file. Before an installer upgrade,
create and verify a backup.

For maintenance restores, stop Gaming Cafe first and use the matching
`scripts\db-restore.ps1` or `scripts/db-restore.sh` helper from the source
repository. The restore helper validates SQLite integrity, requires an
explicit `RESTORE` confirmation, and creates a pre-restore backup. It never
silently overwrites a live database. The client application itself does not
need `sqlite3.exe` for normal operation or for API backups.

## Upgrade and uninstall expectations

The jpackage installer uses the stable upgrade UUID
`8d8f4a61-7094-4cc2-a83d-1b9178f94707`. Installing a newer version over an
existing version updates the application under Program Files while retaining
the ProgramData database, configuration, logs, and backups. Uninstalling the
application must not be treated as a request to delete business data; retain
or archive the ProgramData directory according to the shop's policy.

## Build-machine requirements

Only the release/build machine needs:

- JDK 17 or newer, including `jpackage` on PATH.
- Maven 3.9 or newer.
- WiX Toolset 3.x with `candle.exe` and `light.exe` on PATH. The current
  script documents WiX Toolset 3.14.1 as the supported tested assumption.
- Node/npm are provisioned automatically by `frontend-maven-plugin` during the
  Maven release build. The pinned toolchain is Node `v22.22.2` and npm
  `10.8.2`. Direct frontend development additionally needs Node.js 20.19+
  and npm 10.8.2+.
- A disposable MySQL instance only when running the opt-in MySQL integration
  profile.

WiX, Maven, Node.js, npm, and Java are not client installation prerequisites.

## Clean Windows acceptance plan

Run this plan on a Windows PC that has no Java, Maven, Node.js, npm, Git,
IntelliJ, or source repository. A real clean Windows run is still a manual
release activity when no such environment is available.

| ID | Test | Expected result |
| --- | --- | --- |
| TC01 | Run the installer | `GamingCafeSetup.exe` installs successfully. |
| TC02 | Inspect Start Menu/Desktop | A **Gaming Cafe** shortcut exists. |
| TC03 | First launch | Spring Boot starts automatically. |
| TC04 | Browser launch | The default browser opens automatically. |
| TC05 | Frontend | React UI loads from Spring Boot. |
| TC06 | Backend API | UI API calls work. |
| TC07 | Database | SQLite is created in the persistent data directory. |
| TC08 | Restart | Data survives application restart. |
| TC09 | Windows restart | The application launches again and data remains. |
| TC10 | Initial admin | First-run admin credentials are generated/displayed securely once. |
| TC11 | Tuya configuration | The production properties file is read successfully. |
| TC12 | Tuya power status | Refresh returns the real smart-plug status. |
| TC13 | Tuya ON | A phone-charger test load turns on. |
| TC14 | Tuya OFF | The phone-charger test load turns off. |
| TC15 | Start session | A session starts and configured Tuya control powers the test load ON. |
| TC16 | End session | The session completes and current `DIRECT_POWER` behavior powers the test load OFF. |
| TC17 | No internet | Business/session/billing state stays correct; hardware reports a separate error. |
| TC18 | Internet restored | Refresh/manual hardware control works again. |
| TC19 | Duplicate launch | The second launch reuses/opens the existing app. |
| TC20 | Port conflict | The user receives a clear conflict message and log location. |
| TC21 | Upgrade installation | Database, config, logs, and backups survive while application files update. |

For TC12–TC18, use only a phone charger or another approved low-risk test
load. Do not connect a PS4 or PS5 to `DIRECT_POWER`. `DIRECT_POWER` is a
phone-charger PoC and is not a safe console shutdown or rest-mode mechanism.

This repository has not been executed on a clean Windows environment by the
development agent. Implementation complete; clean Windows acceptance remains
manual/pending.
