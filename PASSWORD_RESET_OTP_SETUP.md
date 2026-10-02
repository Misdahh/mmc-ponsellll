# MMC PONSEL v1.46 - Reset Password via Email OTP

Flow:
1. User enters username.
2. Backend finds the registered recovery email.
3. A 6-digit OTP is sent to that email.
4. OTP expires after 10 minutes and has a 5-attempt limit.
5. After successful verification, Firebase Auth password is replaced.

Configure Firebase Functions secrets:
firebase functions:secrets:set SMTP_HOST SMTP_PORT SMTP_USER SMTP_PASS SMTP_FROM
firebase deploy --only functions

Recommended: use a transactional SMTP provider or Gmail SMTP with an App Password. Never put SMTP credentials in the Android app.
