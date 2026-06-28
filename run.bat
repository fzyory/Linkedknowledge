@echo off
REM ============================================
REM 一键启动 linkedknowledge Spring Boot
REM 用 javac 编译后再启动
REM ============================================

setlocal enabledelayedexpansion

cd /d "C:\Users\g\Desktop\linkedknowledge"

REM 1) 编译
echo [1/3] 编译...
call compile.bat
if errorlevel 1 exit /b 1

REM 2) 拷贝 application.yml 到 classes
echo [2/3] 拷贝配置...
copy /y src\main\resources\application.yml target\classes\ >nul

REM 3) 启动 Spring Boot
echo [3/3] 启动 Spring Boot...
echo API Key 请确保环境变量 MINIMAX_CN_API_KEY 已设置
echo 启动后访问 http://localhost:8080/hello 测试

java ^
  -DMAVEN_OPTS= ^
  -DMINIMAX_CN_API_KEY=%MINIMAX_CN_API_KEY% ^
  -cp "target\classes;%USERPROFILE%\.m2\repository\org\projectlombok\lombok\1.18.38\lombok-1.18.38.jar;%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot\3.3.5\spring-boot-3.3.5.jar;target\cp.txt" ^
  -Dloader.main=com.LinkedKnowledge.LinkedKnowledgeApplication ^
  org.springframework.boot.loader.launch.PropertiesLauncher

endlocal