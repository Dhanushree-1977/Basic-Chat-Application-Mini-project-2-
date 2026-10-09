@echo off
title Real-Time Chat Server (TLS Port 5000)
echo ===================================================
echo   Starting TLS Encrypted Real-Time Chat Server...
echo ===================================================
java -cp "target\classes;lib\*" com.realtimechat.Main server
pause
