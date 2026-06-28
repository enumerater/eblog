@echo off
chcp 65001 > nul
title eblog 微服务 — 启动器

echo ============================================
echo  eblog 微服务启动脚本
echo  请确保先启动: MySQL, Redis, Nacos
echo ============================================
echo.

:: 配置 Java 和 Maven
set JAVA_HOME=E:\java_main\jdk23
set MAVEN_HOME=E:\java_main\apache-maven-3.9.9
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

:: ─── 启动顺序: Nacos → Auth → Gateway ───

echo [1/3] 编译公共模块...
cd /d %~dp0..\common
call mvn clean install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [失败] 公共模块编译失败
    pause
    exit /b 1
)
echo [成功] 公共模块编译完成

echo.
echo [2/3] 启动 Auth Service...
start "auth-service" cmd /c "cd /d %~dp0..\services\auth-service && mvn spring-boot:run -DskipTests"
echo Auth Service 启动中 (端口: 8081) ...
timeout /t 15 /nobreak > nul

echo.
echo [3/3] 启动 Gateway...
start "gateway-service" cmd /c "cd /d %~dp0..\services\gateway-service && mvn spring-boot:run -DskipTests"
echo Gateway 启动中 (端口: 8080) ...

echo.
echo ============================================
echo  所有服务启动完成!
echo  Gateway:  http://localhost:8080
echo  Auth:     http://localhost:8081
echo  Nacos:    http://localhost:8848/nacos
echo ============================================
echo  提示: 关闭窗口即可停止服务
echo.
pause
