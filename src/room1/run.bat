@echo off
setlocal
rem Team bookshelf and rooms - JDK 17+
cd /d "%~dp0..\.."
if not exist bin mkdir bin
set "JDK_BIN="
if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_BIN=%JAVA_HOME%\bin\"
set "TEAM_ROOMS="
if exist src\room2\Room2Panel.java set "TEAM_ROOMS=src\room2\Room2Panel.java"
if exist src\room3\Room3Panel.java set "TEAM_ROOMS=%TEAM_ROOMS% src\room3\Room3Panel.java"
"%JDK_BIN%javac.exe" --release 17 -encoding UTF-8 -d bin -sourcepath src src\common\MainFrame.java %TEAM_ROOMS%
if errorlevel 1 (
    echo.
    echo Compile failed. Check that a JDK 17+ is installed and javac is on PATH.
    pause
    exit /b 1
)
"%JDK_BIN%java.exe" -cp "resources;bin" common.MainFrame
