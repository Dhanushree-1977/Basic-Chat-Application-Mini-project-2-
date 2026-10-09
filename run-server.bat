@echo off
set GSON=%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set MYSQL=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar
set BCRYPT=%USERPROFILE%\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar

echo ===================================================
echo   Starting TLS Encrypted Real-Time Chat Server...
echo ===================================================
java -cp "target\classes;%GSON%;%MYSQL%;%BCRYPT%" com.realtimechat.Main server
pause
