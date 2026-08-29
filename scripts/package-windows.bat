@echo off
setlocal EnableExtensions

rem Packages the already-built thin jar and runtime dependencies with
rem jpackage. The generated launcher has no console window and invokes the
rem Java GamingCafeLauncher main class.

set "ROOT_DIR=%~dp0.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"
set "BACKEND_DIR=%ROOT_DIR%\backend"
set "TARGET_DIR=%BACKEND_DIR%\target"
set "JPACKAGE_INPUT=%TARGET_DIR%\jpackage"
set "OUTPUT_DIR=%TARGET_DIR%\installer"
set "THIN_JAR=%TARGET_DIR%\gaming-cafe.jar.original"

if not exist "%THIN_JAR%" (
    echo ERROR: "%THIN_JAR%" was not found.
    echo Run scripts\build-production.bat or mvn clean package first.
    exit /b 1
)

where jpackage.exe >nul 2>&1
if errorlevel 1 (
    echo ERROR: jpackage was not found. Install a JDK 17 or newer on the build machine.
    exit /b 1
)

where mvn.cmd >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven was not found; it is needed to read the project version.
    exit /b 1
)

set "APP_VERSION="
for /f "delims=" %%V in ('mvn.cmd -q -f "%BACKEND_DIR%\pom.xml" help:evaluate "-Dexpression=project.version" "-DforceStdout"') do if not defined APP_VERSION set "APP_VERSION=%%V"
if not defined APP_VERSION (
    echo ERROR: Could not read the application version from backend\pom.xml.
    exit /b 1
)

echo(%APP_VERSION%| findstr /r /x "[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*" >nul
if errorlevel 1 (
    echo ERROR: jpackage requires a numeric x.y.z version; found "%APP_VERSION%".
    exit /b 1
)

if not exist "%JPACKAGE_INPUT%" mkdir "%JPACKAGE_INPUT%"
if not exist "%JPACKAGE_INPUT%\lib" mkdir "%JPACKAGE_INPUT%\lib"
copy /Y "%THIN_JAR%" "%JPACKAGE_INPUT%\gaming-cafe.jar" >nul
if errorlevel 1 (
    echo ERROR: Could not stage the application jar for jpackage.
    exit /b 1
)

if exist "%OUTPUT_DIR%" rmdir /S /Q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"
if errorlevel 1 (
    echo ERROR: Could not create the packaging output directory.
    exit /b 1
)

echo Creating bundled Java app image...
jpackage.exe --type app-image --name "Gaming Cafe" --app-version "%APP_VERSION%" --vendor "Gaming Cafe" --description "Gaming Cafe Management System" --input "%JPACKAGE_INPUT%" --dest "%OUTPUT_DIR%" --main-jar gaming-cafe.jar --main-class com.cafe.ps.launcher.GamingCafeLauncher --java-options "-Dfile.encoding=UTF-8" --java-options "-Djava.awt.headless=false"
if errorlevel 1 (
    echo ERROR: jpackage app-image creation failed.
    exit /b 1
)

if /I "%GAMING_CAFE_APP_IMAGE_ONLY%"=="1" (
    echo App-image-only mode enabled; skipping the installer.
    echo App image: "%OUTPUT_DIR%\Gaming Cafe"
    exit /b 0
)

where candle.exe >nul 2>&1
if errorlevel 1 (
    echo ERROR: WiX Toolset 3 candle.exe was not found on PATH.
    echo Install WiX Toolset 3.14.1 or a compatible WiX 3 release and rerun this script, or set GAMING_CAFE_APP_IMAGE_ONLY=1 for an app image.
    exit /b 1
)
where light.exe >nul 2>&1
if errorlevel 1 (
    echo ERROR: WiX Toolset 3 light.exe was not found on PATH.
    echo Install WiX Toolset 3.14.1 or a compatible WiX 3 release and rerun this script, or set GAMING_CAFE_APP_IMAGE_ONLY=1 for an app image.
    exit /b 1
)

echo Creating Windows installer...
jpackage.exe --type exe --name "Gaming Cafe" --app-version "%APP_VERSION%" --vendor "Gaming Cafe" --description "Gaming Cafe Management System" --input "%JPACKAGE_INPUT%" --dest "%OUTPUT_DIR%" --outfile GamingCafeSetup.exe --main-jar gaming-cafe.jar --main-class com.cafe.ps.launcher.GamingCafeLauncher --java-options "-Dfile.encoding=UTF-8" --java-options "-Djava.awt.headless=false" --install-dir GamingCafe --win-menu --win-menu-group "Gaming Cafe" --win-shortcut --win-upgrade-uuid 8d8f4a61-7094-4cc2-a83d-1b9178f94707
if errorlevel 1 (
    echo ERROR: Windows installer creation failed.
    exit /b 1
)

if not exist "%OUTPUT_DIR%\GamingCafeSetup.exe" (
    echo ERROR: jpackage reported success but the installer was not found.
    exit /b 1
)

echo Installer created: "%OUTPUT_DIR%\GamingCafeSetup.exe"
exit /b 0
