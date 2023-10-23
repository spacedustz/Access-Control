@echo off
chcp 65001 > nul 2>&1

:loop

REM Java 프로세스가 실행 중인지 확인
netstat -ano | find "8090" >nul
if errorlevel 1 (
  start java -jar C:\Users\root\Desktop\count.jar
  echo 서버를 실행합니다.
) else (
  echo 서버가 이미 실행 중입니다.
)

REM cvediart 프로세스가 실행 중인지 확인
tasklist | findstr "cvediart.exe" >nul
if errorlevel 1 (
  E:
  cd Tools\Cvedia-2023.4.0
  start CVEDIA-RT
  echo Cvedia 서버를 실행합니다.
) else (
  echo Cvedia 서버가 이미 실행 중입니다.
)

REM 60초 대기
timeout /t 5 /nobreak >nul
echo 대기중 ...

REM 루프 재시작
goto loop
echo Health Check Looping