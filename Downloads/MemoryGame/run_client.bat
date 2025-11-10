@echo off
cd /d "%~dp0"
echo Starting CLIENT...
java -cp "target/classes" com.memorygame.client.ClientApp
pause