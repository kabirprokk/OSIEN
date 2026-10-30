@echo off
REM Osien Forge — double-click to install. No coding needed.
REM Opens a window, installs everything, tells you what to do next.
title Osien Forge Installer
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\setup.ps1"
echo.
echo ========================================================
echo  Install finished! You can close this window now.
echo  Next: open studio.html (double-click it) and copy
echo  a command like  /start  into your AI chat.
echo ========================================================
pause
