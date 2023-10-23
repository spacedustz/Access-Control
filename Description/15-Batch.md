## 📘 Window Batch 파일 작성

윈도우 시작 시, 아래의 배치파일들이 실행되며, 서버들이 자동으로 시작되고 웹페이지 2개가 열리게 됩니다.

총 3개의 배치 파일을 작성 했습니다.

- Open-Server : 카운팅 서버와 감시 서버를 실행 시키고 60초마다 헬스체크를 해 프로세스가 죽으면 재 실행 합니다.
- Close-Server : 배치 파일이 먹통일 때 수동으로 프로세스들을 종료합니다.
- Open-Viewer : 서버들이 켜지는 시간을 고려해 10초 후, 현황판/관리 페이지를 열게 합니다.

<br>

> 📌 **Open-Server.bat**

카운팅 서버와 감시 서버가 죽으면 60초마다 프로세스를 확인해서,

**프로스세가 실행되어 있지 않으면 자동으로 재 시작 해주는 윈도우 배치 파일을 작성합니다.

```shell
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
```

<br>

**Jar 실행 / 감시 서버 실행 완료**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-health.png)

<br>

> 📌 **Close-Server.bat**

수동으로 서버들의 프로세스를 강제 종료하는 배치 파일 입니다.

```shell
@echo off 
taskkill /F /IM java.exe /T 
taskkill /F /IM cvediart.exe /T
```

<br>

> 📌 **Open-Viewer.bat**

윈도우 시작 시, 서버들이 실행되기 까지의 시간 (10초 정도) 기다린 후 현황판, 관리자 페이지를 열게 합니다.

```shell
@echo off
chcp 65001 > nul 2>&1

echo 서버가 실행되길 대기하는 중 ...
echo 10초 후, 현황판과 관리 페이지가 열립니다.

(
  echo off
  timeout /t 10 /nobreak
  start chrome http://localhost:8090 http://localhost:8090/admin
)
```

<br>

**터미널**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-batch2.png)

<br>

> 📌 **윈도우 시작 시 배치 파일 실행**

`C:\ProgramData\Microsoft\Windows\Start Menu\Programs\Startup` 경로에 Open-Server, Close-Server 파일을 놔두면 됩니다.