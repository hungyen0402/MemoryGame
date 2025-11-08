@echo off
cd /d "%~dp0"
echo Starting SERVER...
java -cp "target/classes;database/data" com.memorygame.server.Server
pause