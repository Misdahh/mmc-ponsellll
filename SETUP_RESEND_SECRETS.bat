@echo off
echo Masukkan API Key Resend saat Firebase meminta secret.
firebase functions:secrets:set RESEND_API_KEY
if errorlevel 1 exit /b 1

echo.
echo Masukkan RESEND_FROM, contoh: MMC PONSEL ^<noreply@domainanda.com^>
firebase functions:secrets:set RESEND_FROM
if errorlevel 1 exit /b 1

echo.
set /p DEPLOY=Deploy Cloud Functions sekarang? (Y/N): 
if /I "%DEPLOY%"=="Y" firebase deploy --only functions
