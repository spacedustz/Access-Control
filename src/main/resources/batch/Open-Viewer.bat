@echo off
chcp 65001 > nul 2>&1

echo 서버가 실행되길 대기하는 중 ...
echo 10초 후, 현황판과 관리 페이지가 열립니다.

(
  echo off
  timeout /t 10 /nobreak
  start chrome http://localhost:8090 http://localhost:8090/admin
)