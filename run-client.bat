@echo off
title Real-Time Chat Desktop Client
echo ===================================================
echo   Starting Real-Time Chat Desktop Client (GUI)...
echo ===================================================
start javaw -cp "target\classes;lib\*" com.realtimechat.Main client
