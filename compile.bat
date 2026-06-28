@echo off
REM ============================================
REM 直接用 javac 编译 linkedknowledge 项目
REM 不依赖 maven，绕开 lombok processor 问题
REM ============================================

setlocal enabledelayedexpansion

REM 1) 找 JDK 17
set "JAVA_HOME=C:\Users\g\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM 2) 进入项目
cd /d "C:\Users\g\Desktop\linkedknowledge"

REM 3) 清空 target
if exist target\classes rmdir /s /q target\classes
mkdir target\classes

REM 4) 找 lombok jar
for /f "delims=" %%i in ('dir /b "%USERPROFILE%\.m2\repository\org\projectlombok\lombok\1.18.38\lombok-1.18.38.jar" 2^>nul') do (
    set "LOMBOK_JAR=%USERPROFILE%\.m2\repository\org\projectlombok\lombok\1.18.38\lombok-1.18.38.jar"
)

if not defined LOMBOK_JAR (
    echo [错误] 找不到 lombok-1.18.38.jar，请先跑：mvn dependency:resolve
    exit /b 1
)

echo [1/3] lombok jar: %LOMBOK_JAR%

REM 5) 用 mvn 拿 classpath
echo [2/3] 拿 classpath...
call mvn -q dependency:build-classpath -DincludeScope=compile -Dmdep.outputFile=target\cp.txt 2>nul
if errorlevel 1 (
    echo [错误] mvn dependency:build-classpath 失败
    exit /b 1
)

set /p CP=<target\cp.txt
set "FULL_CP=%LOMBOK_JAR%;%CP%"

REM 6) javac 编译
echo [3/3] javac 编译...
dir /s /b src\main\java\*.java > target\sources.txt
javac -encoding UTF-8 -d target\classes -classpath "%FULL_CP%" -processorpath "%LOMBOK_JAR%" @target\sources.txt
if errorlevel 1 (
    echo.
    echo [编译失败] 看上面报错
    exit /b 1
)

echo.
echo [编译成功] class 在 target\classes\
endlocal