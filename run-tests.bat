@echo off
set GSON=%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set MYSQL=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar
set BCRYPT=%USERPROFILE%\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar
set JUNIT_API=%USERPROFILE%\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.10.2\junit-jupiter-api-5.10.2.jar
set JUNIT_COMM=%USERPROFILE%\.m2\repository\org\junit\platform\junit-platform-commons\1.10.2\junit-platform-commons-1.10.2.jar
set OPENTEST=%USERPROFILE%\.m2\repository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar

set CP=target\classes;target\test-classes;%GSON%;%MYSQL%;%BCRYPT%;%JUNIT_API%;%JUNIT_COMM%;%OPENTEST%

echo ===================================================
echo   Running JUnit 5 Test Suite...
echo ===================================================
java -cp "%CP%" com.realtimechat.TestRunner
pause
