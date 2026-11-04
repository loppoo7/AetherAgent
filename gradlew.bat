@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (gradle %*) else (echo Gradle is not installed. Install Gradle 8.9 or generate the official wrapper. & exit /b 1)
