@echo off
REM ---------------------------------------------------------------
REM  Guess Market - exercise 1
REM  Launches the console application.
REM
REM  %~dp0 is the folder this batch file sits in, with a trailing
REM  backslash. Every path below is built from it, so the program
REM  runs correctly no matter which directory it is started from
REM  and no matter where the folder is copied to.
REM ---------------------------------------------------------------

setlocal

set "HERE=%~dp0"

if not exist "%HERE%ui.jar" (
    echo ERROR: ui.jar was not found next to this batch file.
    echo Make sure ui.jar, engine.jar and the lib folder are all in:
    echo   %HERE%
    pause
    exit /b 1
)

java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java was not found on this machine.
    echo This program needs Java 25. Install it, then run this file again.
    pause
    exit /b 1
)

java -cp "%HERE%ui.jar;%HERE%engine.jar;%HERE%lib\*" guessmarket.ui.console.Main

endlocal
