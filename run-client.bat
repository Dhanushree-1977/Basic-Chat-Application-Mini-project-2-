@echo off
cd /d "%~dp0"
title Real-Time Chat Desktop Client
echo ===================================================
echo   Starting Real-Time Chat Desktop Client (GUI)...
echo ===================================================

set "JAVA_BIN=javaw"
if exist "C:\Program Files\Java\jdk-23\bin\javaw.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-23\bin\javaw.exe"
)

start "Real-Time Chat Client" "%JAVA_BIN%" -cp "target\classes;lib\*" com.realtimechat.Main client
