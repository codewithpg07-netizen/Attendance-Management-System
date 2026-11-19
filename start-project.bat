@echo off
setlocal EnableDelayedExpansion
title Smart Attendance System - Launcher
color 0A
cls

echo.
echo  ================================================================
echo     SMART STUDENT ATTENDANCE MANAGEMENT SYSTEM
echo     Powered by Spring Boot + MariaDB
echo  ================================================================
echo.

REM -- Paths -----------------------------------------------------------
set "MARIA_DIR=C:\Users\HP\mariadb\mariadb-10.4.34-winx64\bin"
set "DATA_DIR=C:\Users\HP\mariadb\data"
set "APP_DIR=%~dp0studentAttendanceSystemProject"
set "JAR=target\studentAttendanceSystemProject-0.0.1-SNAPSHOT.jar"
set "DB_PORT=3307"
set "APP_PORT=3217"

REM -- Detect LAN IP (Wi-Fi first, then any) --------------------------
for /f "tokens=2 delims=:" %%A in ('ipconfig ^| findstr /C:"IPv4" ^| findstr /V "169.254"') do (
    set "LAN_IP=%%A"
    set "LAN_IP=!LAN_IP: =!"
)
if not defined LAN_IP set "LAN_IP=localhost"

echo  Detected LAN IP : %LAN_IP%
echo.

REM ====================================================================
echo  [STEP 1/3]  Checking MariaDB (port %DB_PORT%)...
echo  ----------------------------------------------------------------
netstat -ano | findstr ":%DB_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo  [OK] MariaDB already running on port %DB_PORT%.
) else (
    echo  [..] Starting MariaDB on port %DB_PORT%...
    start /b "" "%MARIA_DIR%\mysqld.exe" --datadir="%DATA_DIR%" --port=%DB_PORT% --console
    echo  [..] Waiting for MariaDB to initialize (6 seconds)...
    timeout /t 6 /nobreak >nul
    netstat -ano | findstr ":%DB_PORT% " | findstr "LISTENING" >nul 2>&1
    if !errorlevel! equ 0 (
        echo  [OK] MariaDB started successfully on port %DB_PORT%.
    ) else (
        echo  [!!] WARNING: MariaDB may not have started.
        echo  [!!] Check if another service is blocking port %DB_PORT%.
        echo.
        pause
        exit /b 1
    )
)

echo.

REM ====================================================================
echo  [STEP 2/3]  Checking if App is already running (port %APP_PORT%)...
echo  ----------------------------------------------------------------
netstat -ano | findstr ":%APP_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo  [OK] App already running on port %APP_PORT%.
    goto :open_browser
)

echo  [..] Starting Spring Boot Application...
echo.
echo  ----------------------------------------------------------------
echo   App URL (this PC):  http://localhost:%APP_PORT%
echo   App URL (Phone):    http://%LAN_IP%:%APP_PORT%
echo   Admin Login:        admin@example.com  /  admin123
echo  ----------------------------------------------------------------
echo.
echo  [..] Please wait -- app starts in ~10 seconds...
echo.

start /b "" cmd /c "cd /d "%APP_DIR%" && java -jar %JAR% > "%APP_DIR%\app.log" 2>&1"

set /a ATTEMPTS=0
:wait_loop
timeout /t 2 /nobreak >nul
set /a ATTEMPTS+=1
netstat -ano | findstr ":%APP_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 goto :app_ready
if %ATTEMPTS% LSS 30 goto :wait_loop

echo  [!!] App did not start within 60 seconds.
echo  [!!] Check log at: %APP_DIR%\app.log
echo.
pause
exit /b 1

:app_ready
echo  [OK] Application is RUNNING!
echo.

REM ====================================================================
:open_browser
echo  [STEP 3/3]  Opening browser...
echo  ----------------------------------------------------------------
timeout /t 1 /nobreak >nul
start "" "http://localhost:%APP_PORT%/login"

echo.
echo  ================================================================
echo   SYSTEM IS READY
echo  ================================================================
echo.
echo   Browser (this PC) : http://localhost:%APP_PORT%/login
echo   Phone / Tablet    : http://%LAN_IP%:%APP_PORT%/login
echo.
echo   ADMIN     : admin@example.com    / admin123
echo   TEACHER   : [teacher email]      / [password]
echo   STUDENT   : [student email]      / [password]
echo.
echo   Log file  : %APP_DIR%\app.log
echo.
echo   To STOP the system, run: stop-project.bat
echo.
echo  ================================================================
pause
