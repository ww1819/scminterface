@echo off
setlocal enabledelayedexpansion

REM ===== SCMInterface 生产环境启动脚本 =====
REM 用法:
REM   start-prod.bat start
REM   start-prod.bat stop
REM   start-prod.bat restart
REM   start-prod.bat status
REM   start-prod.bat logs

cd /d %~dp0

set APP_NAME=scminterface-admin
set APP_PROFILE=druid
set APP_PORT=8088
set APP_HOME=%~dp0
set APP_JAR=%APP_HOME%scminterface-admin\target\%APP_NAME%.jar
set LOG_DIR=%APP_HOME%logs
set PID_FILE=%LOG_DIR%\%APP_NAME%.pid
set LOG_FILE=%LOG_DIR%\%APP_NAME%.log

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

if "%~1"=="" goto usage
if /i "%~1"=="start" goto start
if /i "%~1"=="stop" goto stop
if /i "%~1"=="restart" goto restart
if /i "%~1"=="status" goto status
if /i "%~1"=="logs" goto logs
goto usage

:start
if not exist "%APP_JAR%" (
  echo [ERROR] 未找到可执行包: %APP_JAR%
  echo [TIP] 先执行打包: mvn clean package -DskipTests
  exit /b 1
)

call :is_running
if "!RUNNING!"=="1" (
  echo [INFO] %APP_NAME% 已在运行，PID=!RUN_PID!
  exit /b 0
)

echo [INFO] 正在启动 %APP_NAME% ...
set JAVA_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai
set SPRING_OPTS=--spring.profiles.active=%APP_PROFILE% --server.port=%APP_PORT%

powershell -NoProfile -Command ^
  "$p = Start-Process -FilePath 'java' -ArgumentList '%JAVA_OPTS% -jar ""%APP_JAR%"" %SPRING_OPTS%' -RedirectStandardOutput '%LOG_FILE%' -RedirectStandardError '%LOG_FILE%' -PassThru -WindowStyle Hidden; $p.Id | Out-File -Encoding ascii '%PID_FILE%'"

timeout /t 2 /nobreak >nul
call :is_running
if "!RUNNING!"=="1" (
  echo [OK] 启动成功，PID=!RUN_PID!
  echo [OK] 日志文件: %LOG_FILE%
  exit /b 0
) else (
  echo [ERROR] 启动失败，请检查日志: %LOG_FILE%
  exit /b 1
)

:stop
call :is_running
if not "!RUNNING!"=="1" (
  echo [INFO] %APP_NAME% 未运行
  if exist "%PID_FILE%" del /f /q "%PID_FILE%" >nul 2>nul
  exit /b 0
)

echo [INFO] 正在停止 %APP_NAME%，PID=!RUN_PID! ...
taskkill /PID !RUN_PID! /T /F >nul 2>nul
timeout /t 1 /nobreak >nul
call :is_running
if "!RUNNING!"=="1" (
  echo [ERROR] 停止失败，请手动检查进程 PID=!RUN_PID!
  exit /b 1
)
if exist "%PID_FILE%" del /f /q "%PID_FILE%" >nul 2>nul
echo [OK] 已停止
exit /b 0

:restart
call "%~f0" stop
if errorlevel 1 exit /b 1
call "%~f0" start
exit /b %errorlevel%

:status
call :is_running
if "!RUNNING!"=="1" (
  echo [INFO] %APP_NAME% 运行中，PID=!RUN_PID!
  echo [INFO] 日志文件: %LOG_FILE%
  exit /b 0
)
echo [INFO] %APP_NAME% 未运行
exit /b 1

:logs
if not exist "%LOG_FILE%" (
  echo [INFO] 日志文件不存在: %LOG_FILE%
  exit /b 0
)
powershell -NoProfile -Command "Get-Content -Path '%LOG_FILE%' -Tail 200 -Wait"
exit /b 0

:is_running
set RUNNING=0
set RUN_PID=
if not exist "%PID_FILE%" goto :eof
set /p CURRENT_PID=<"%PID_FILE%"
if "%CURRENT_PID%"=="" goto :eof
tasklist /FI "PID eq %CURRENT_PID%" | findstr /R /C:" %CURRENT_PID% " >nul 2>nul
if %errorlevel%==0 (
  set RUNNING=1
  set RUN_PID=%CURRENT_PID%
) else (
  del /f /q "%PID_FILE%" >nul 2>nul
)
goto :eof

:usage
echo 用法: %~n0 ^<start^|stop^|restart^|status^|logs^>
exit /b 1

