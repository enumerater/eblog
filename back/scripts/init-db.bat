@echo off
chcp 65001 > nul
title eblog — 初始化数据库

echo ============================================
echo  eblog 数据库初始化脚本
echo  请确保 MySQL 已启动 (root/1234@localhost)
echo ============================================
echo.

set MYSQL_HOME=E:\java_main\mysql-8.0
set MYSQL_CMD=%MYSQL_HOME%\bin\mysql.exe
if not exist "%MYSQL_CMD%" (
    set MYSQL_CMD=mysql
)

:: 执行所有初始化 SQL
for %%f in (%~dp0..\sql\init\*.sql) do (
    echo 执行: %%f
    "%MYSQL_CMD%" -u root -p1234 -h 127.0.0.1 < "%%f"
    if %ERRORLEVEL% neq 0 (
        echo [失败] %%f
    ) else (
        echo [成功] %%f
    )
)

echo.
echo ============================================
echo  数据库初始化完成!
echo  已创建数据库: db_auth, db_article_write, db_article_read, db_comment
echo ============================================
pause
