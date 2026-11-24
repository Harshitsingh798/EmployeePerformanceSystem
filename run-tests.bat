@echo off
REM ---------------------------------------------------------------------------
REM  Runs the JUnit 5 test suite.
REM
REM  This script needs nothing but a JDK and the jars in lib\ - Maven is not
REM  required. If you do have Maven, "mvn test" runs the same tests.
REM
REM  The DAO integration tests need a running MySQL with the seed data loaded.
REM  Without a database they are skipped rather than failed.
REM ---------------------------------------------------------------------------
setlocal

cd /d "%~dp0"

if not exist "bin\test" mkdir "bin\test"

REM Use one JDK for both compiling and running, so a machine with two JDKs
REM installed cannot compile with one and run with the other.
set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    if exist "%JAVA_HOME%\bin\java.exe"  set "JAVA=%JAVA_HOME%\bin\java.exe"
)

echo.
echo === Compiling the application and the tests ===
echo.

"%JAVAC%" -Xlint:all -encoding UTF-8 -cp "lib/*" -d "bin\test" -sourcepath "src;src/test/java" ^
    src\test\java\dao\DaoIntegrationTest.java ^
    src\test\java\model\EvaluationTest.java ^
    src\test\java\model\FeedbackTest.java ^
    src\test\java\model\GoalTest.java ^
    src\test\java\util\PasswordUtilTest.java ^
    src\test\java\util\ReportExporterTest.java ^
    src\test\java\util\ValidatorTest.java

if errorlevel 1 (
    echo.
    echo Compilation FAILED.
    exit /b 1
)

echo.
echo === Running the tests ===
echo.

"%JAVA%" -jar lib\junit-platform-console-standalone-1.10.2.jar execute ^
    -cp "bin\test;lib\flatlaf-3.4.1.jar;lib\jfreechart-1.5.4.jar;lib\mysql-connector-j-8.0.33.jar" ^
    --scan-classpath="bin\test" ^
    --details=tree ^
    --disable-ansi-colors ^
    --fail-if-no-tests

set EXIT_CODE=%ERRORLEVEL%

echo.
if "%EXIT_CODE%"=="0" (
    echo All tests PASSED.
) else (
    echo Tests FAILED - see the output above.
)

exit /b %EXIT_CODE%
