@echo off
cd /d "%~dp0"
title Real-Time Chat System Launcher

echo ========================================================
echo   Launching Real-Time Chat Server and Both Clients
echo ========================================================

REM Resolve Java executable
set "JAVA_BIN=java"
if exist "C:\Program Files\Java\jdk-23\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-23\bin\java.exe"
)

echo [1/3] Starting TLS Server on Port 5000...
start "Real-Time Chat Server (Port 5000)" "%JAVA_BIN%" -cp "target\classes;lib\*" com.realtimechat.Main server

echo Waiting for Server to initialize...
ping 127.0.0.1 -n 3 >nul

echo [2/3] Launching Client 1 (Alice)...
start "Real-Time Chat Client 1" "%JAVA_BIN%" -cp "target\classes;lib\*" com.realtimechat.Main client

echo Waiting for Client 1...
ping 127.0.0.1 -n 2 >nul

echo [3/3] Launching Client 2 (Bob)...
start "Real-Time Chat Client 2" "%JAVA_BIN%" -cp "target\classes;lib\*" com.realtimechat.Main client

echo ========================================================
echo   Server and Both Clients have been opened!
echo ========================================================
