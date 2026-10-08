@echo off
REM Starts MySQL manually from C:\MySQLData (used when Windows Service not configured)
REM Run this if App shows "Database unreachable" - keeps DB alive
set MYSQLD="C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe"
if not exist "C:\MySQLData" (
  echo Data dir not found at C:\MySQLData
  echo Run: "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --initialize-insecure --datadir=C:\MySQLData
  pause
  exit /b
)
echo Starting MySQL on port 3306 with datadir C:\MySQLData...
echo Do NOT close this window while using the App. Press Ctrl+C to stop.
"%MYSQLD%" --datadir=C:\MySQLData --console --port=3306
