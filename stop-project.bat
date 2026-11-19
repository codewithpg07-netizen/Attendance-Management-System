@echo off
setlocal EnableDelayedExpansion
title Smart Attendance System - Stopping...
color 0C
cls

echo.
echo  ================================================================
echo     SMART STUDENT ATTENDANCE MANAGEMENT SYSTEM
echo     Stopping all components...
echo  ================================================================
echo.

set "MARIA_DIR=C:\Users\HP\mariadb\mariadb-10.4.34-winx64\bin"
set "DB_PORT=3307"
set "APP_PORT=3217"

REM -- Stop Spring Boot (Java process on port 3217) ------------------
echo  [1/2] Stopping Spring Boot application (port %APP_PORT%)...
for /f "tokens=5" %%P in ('netstat -ano 2^>nul ^| findstr ":%APP_PORT% " ^| findstr "LISTENING"') do (
    echo  [..] Terminating Java process (PID %%P)...
    taskkill /F /PID %%P >nul 2>&1
)
echo  [OK] Spring Boot stopped.
echo.

REM -- Stop MariaDB gracefully ---------------------------------------
echo  [2/2] Stopping MariaDB (port %DB_PORT%)...
"%MARIA_DIR%\mysqladmin.exe" -P %DB_PORT% -u root -p"Shakila@123" shutdown >nul 2>&1
timeout /t 2 /nobreak >nul

for /f "tokens=5" %%P in ('netstat -ano 2^>nul ^| findstr ":%DB_PORT% " ^| findstr "LISTENING"') do (
    echo  [..] Terminating process on port %DB_PORT% (PID %%P)...
    taskkill /F /PID %%P >nul 2>&1
)

echo  [OK] MariaDB stopped.
echo.

echo  ================================================================
echo   All services stopped successfully.
echo  ================================================================
echo.
pause
