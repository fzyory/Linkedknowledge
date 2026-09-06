@echo off
REM 临时设置 Java 17 环境变量
set JAVA_HOME=C:\Users\g\.jdks\ms-17.0.19
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo 已切换到 Java 17
echo ========================================
java -version
echo.
echo 现在可以运行 Maven 命令了
echo 例如: mvn spring-boot:run
echo ========================================
cmd /k
