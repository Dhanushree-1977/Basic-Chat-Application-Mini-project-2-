@echo off
title Real-Time Chat Unit Tests
echo ===================================================
echo   Running JUnit 5 Test Suite...
echo ===================================================
java -cp "target\classes;target\test-classes;lib\*" com.realtimechat.TestRunner
pause
