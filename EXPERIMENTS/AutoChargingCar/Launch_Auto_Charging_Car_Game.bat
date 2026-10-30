@echo off
cd /d "%~dp0browser-game"
if not exist node_modules (
  echo First run: npm install
  pause
  exit /b 1
)
start "Auto Charging Car game server" /min cmd /c npm run dev
timeout /t 3 /nobreak >nul
start "" http://127.0.0.1:5173
