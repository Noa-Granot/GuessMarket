@echo off
REM Guess Market - exercise 2
REM %~dp0 is the folder this file sits in, so the program runs
REM correctly from any directory and from anywhere it is copied to.

setlocal

set "HERE=%~dp0"

if not exist "%HERE%ui.jar" (
    echo ERROR: ui.jar was not found next to this batch file.
    echo Make sure ui.jar, engine.jar, lib and lib-fx are all in:
    echo   %HERE%
    pause
    exit /b 1
)

if not exist "%HERE%lib-fx\javafx.controls.jar" (
    echo ERROR: the JavaFX files were not found.
    echo The folder lib-fx has to sit next to this batch file and contain
    echo both the javafx jar files and the dll files.
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

java -Djava.library.path="%HERE%lib-fx" ^
     --module-path "%HERE%lib-fx" ^
     --add-modules javafx.controls,javafx.fxml ^
     --enable-native-access=javafx.graphics ^
     -cp "%HERE%ui.jar;%HERE%engine.jar;%HERE%lib\*;%HERE%lib-fx\*" ^
     guessmarket.ui.fx.GuessMarketApp

if errorlevel 1 (
    echo.
    echo The program stopped with an error. The message above says why.
    pause
)

endlocal