@echo off
echo 正在启动SCMInterface服务...
cd /d %~dp0
cd scminterface-admin
mvn spring-boot:run
pause

