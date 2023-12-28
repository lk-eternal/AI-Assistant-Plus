@echo off
chcp 65001 > nul

setlocal enabledelayedexpansion

set MAVEN_HOME=C:\Apps\apache-maven-3.8.4
set JAVA_HOME=C:\Apps\Java\jdk-17.0.2
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
set JAVA_OPTS=-Xms256m -Xmx1024m
set MAVEN_OPTS=-Xms256m -Xmx1024m

set current_dir=%~dp0
set git_url=https://gitee.com/of/AI-Assistant-Plus.git
set project_dir=C:\Users\wukong\Desktop\run\AI-Assistant-Plus
set jar_file_name=ai-assistant-plus-0.1.0.jar
set jar_file_path=%project_dir%\target\%jar_file_name%

set check_interval=10

:check_for_updates
set has_update=1
:: 克隆或更新 Git 仓库
if not exist "%project_dir%" (
  echo 克隆 Git 仓库...
  git clone %git_url% %project_dir%
) else (
  cd /d %project_dir%
  git pull | findstr /RC:"Already up to date." >nul 2>&1 && set has_update=0
)

:: 检查端口 80 是否被占用
netstat -ano | findstr 0.0.0.0:80  >nul
if not errorlevel 1 if !has_update! == 0 (
  rem 没有更新且80端口正常使用中,无需重新部署
  timeout /t %check_interval% /nobreak > nul
  goto :check_for_updates
)

echo 开始构建应用...
cd /d %project_dir%
call %MAVEN_HOME%\bin\mvn -DskipTests clean install -f "%project_dir%\pom.xml"

for /F "tokens=5 delims= " %%P in ('netstat -ano ^| findstr 0.0.0.0:80') do taskkill /F /PID %%P
echo 已关闭使用端口 80 的进程。

echo 开始启动应用...
copy %jar_file_path% %current_dir%
cd /d %current_dir%
start /b %JAVA_HOME%\bin\java %JAVA_TOOL_OPTIONS% -jar %current_dir%%jar_file_name%
echo 已启动应用

timeout /t %check_interval% /nobreak > nul
goto :check_for_updates

pause