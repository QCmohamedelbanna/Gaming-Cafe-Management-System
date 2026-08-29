@echo off
setlocal EnableExtensions

rem One-command developer release build:
rem   1) isolated frontend npm ci + Vite build (performed by Maven)
rem   2) Spring Boot executable jar
rem   3) Windows app image and installer

set "ROOT_DIR=%~dp0.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"
set "BACKEND_DIR=%ROOT_DIR%\backend"

if not exist "%BACKEND_DIR%\pom.xml" (
    echo ERROR: backend\pom.xml was not found.
    exit /b 1
)

where mvn.cmd >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven 3.9 or newer is required on the build machine.
    exit /b 1
)

pushd "%BACKEND_DIR%"
call mvn.cmd -B clean package
set "BUILD_EXIT=%ERRORLEVEL%"
popd

if not "%BUILD_EXIT%"=="0" (
    echo ERROR: Maven/frontend production build failed with exit code %BUILD_EXIT%.
    exit /b %BUILD_EXIT%
)

call "%ROOT_DIR%\scripts\package-windows.bat"
set "PACKAGE_EXIT=%ERRORLEVEL%"
if not "%PACKAGE_EXIT%"=="0" (
    echo ERROR: Windows packaging failed with exit code %PACKAGE_EXIT%.
    exit /b %PACKAGE_EXIT%
)

echo.
if /I "%GAMING_CAFE_APP_IMAGE_ONLY%"=="1" (
    echo Production app-image build completed.
    echo App image: "%BACKEND_DIR%\target\installer\Gaming Cafe"
    exit /b 0
)

if not exist "%BACKEND_DIR%\target\installer\GamingCafeSetup.exe" (
    echo ERROR: The packaging step completed without creating the installer.
    exit /b 1
)

echo Production build completed.
echo Installer: "%BACKEND_DIR%\target\installer\GamingCafeSetup.exe"
exit /b 0
