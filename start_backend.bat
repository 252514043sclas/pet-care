@echo off
echo ==============================================
echo  Starting Pet Care Backend Services (XAMPP)
echo ==============================================

:: Check if MySQL is running
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] MySQL is already running.
) else (
    echo Starting MySQL...
    start /B "" "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
    timeout /t 2 >nul
    echo [OK] MySQL started.
)

:: Check if Apache is running
tasklist /FI "IMAGENAME eq httpd.exe" 2>NUL | find /I /N "httpd.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] Apache is already running.
) else (
    echo Starting Apache...
    start /B "" "C:\xampp\apache\bin\httpd.exe" -d "C:/xampp/apache"
    timeout /t 2 >nul
    echo [OK] Apache started.
)

echo.
echo Checking API health:
powershell -Command "try { $res = Invoke-RestMethod -Uri 'http://localhost/petcare/public/api/health-check' -TimeoutSec 3; Write-Host '[OK] Pet Care API is online at http://localhost/petcare/public/api/' -ForegroundColor Green } catch { Write-Host '[FAIL] Could not connect to API: ' $_.Exception.Message -ForegroundColor Red }"
echo.
echo Backend ready!

