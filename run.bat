@echo off
REM ---------------------------------------------------------------------------
REM  Compiles and starts Performetrics.
REM
REM  Needs nothing but a JDK and the jars in lib\.
REM
REM  The compiler and the runtime are taken from JAVA_HOME when it is set, so
REM  that a machine with two JDKs installed cannot end up compiling with one
REM  and running with the other.
REM ---------------------------------------------------------------------------
setlocal

cd /d "%~dp0"

if not exist "bin" mkdir "bin"

set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    if exist "%JAVA_HOME%\bin\java.exe"  set "JAVA=%JAVA_HOME%\bin\java.exe"
)

echo.
echo === Compiling ===
echo.

"%JAVAC%" -encoding UTF-8 -cp "lib/*" -d "bin" -sourcepath "src" src\App.java

if errorlevel 1 (
    echo.
    echo Compilation FAILED.
    exit /b 1
)

echo.
echo === Starting Performetrics ===
echo.

"%JAVA%" -cp "bin;lib/*" App

endlocal
