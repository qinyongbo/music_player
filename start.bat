@echo off
REM ========================================================
REM Music Player - Startup Script
REM ========================================================

setlocal enabledelayedexpansion

echo.
echo ========================================================
echo      Music Player Pro - Startup Script
echo ========================================================
echo.

REM Check Java version
echo [Step 1] Checking Java environment...
java -version >nul 2>&1
if !errorlevel! neq 0 (
    echo ERROR: Java not found, please install JDK 18 or higher
    echo Download: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo OK Java environment is ready

REM Check VLC installation
echo.
echo [Step 2] Checking VLC installation...
if not exist "D:\Program Files\VideoLAN\VLC" (
    echo ERROR: VLC not found, please ensure VLC is installed in: D:\Program Files\VideoLAN\VLC
    echo Download: https://www.videolan.org/vlc/
    pause
    exit /b 1
)
echo OK VLC is installed

REM Check if compilation is needed
echo.
echo [Step 3] Checking compilation status...
if not exist "target\music-player-2.0.0-shaded.jar" (
    echo INFO: Build artifact not found, compiling project...
    call mvn clean package -DskipTests
    if !errorlevel! neq 0 (
        echo ERROR: Compilation failed
        pause
        exit /b 1
    )
    echo OK Build completed successfully
) else (
    echo OK Build artifact exists
)

REM Start the application
echo.
echo [Step 4] Starting Music Player...
echo.
start "" javaw -Dfile.encoding=UTF-8 -Dvlc.path="D:\Program Files\VideoLAN\VLC" -jar target\music-player-2.0.0-shaded.jar

echo OK Application started!
echo.
echo ========================================================
echo Tip: To view logs, run the following command:
echo java -Dfile.encoding=UTF-8 -Dvlc.path="D:\Program Files\VideoLAN\VLC" -jar target\music-player-2.0.0-shaded.jar
echo ========================================================
echo.
