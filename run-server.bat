@echo off
cd /d "%~dp0"
title Real-Time Chat Server (TLS Port 5000)
echo ===================================================
echo   Starting TLS Encrypted Real-Time Chat Server...
echo ===================================================

set "JAVA_BIN=java"
if exist "C:\Program Files\Java\jdk-23\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-23\bin\java.exe"
)

"%JAVA_BIN%" -cp "target\classes;lib\*" com.realtimechat.Main server
pause
