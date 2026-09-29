# MMC PONSEL Firebase Functions

Functions ini menangani pembayaran Midtrans dan role member.

## Deploy
```bash
npm install
firebase deploy --only functions
```

## Member
Akun member memakai username aplikasi `miss` dan email internal `member.miss@mmcponsel.app`. Password ditentukan bebas oleh member saat akun pertama kali dibuat (minimum 6 karakter). Setelah login, aplikasi memanggil `ensureMemberRole` untuk memberi custom claim `member=true`.

## Security
Jangan menaruh Midtrans Server Key di APK. Simpan sebagai Firebase Secret:
```bash
firebase functions:secrets:set MIDTRANS_SERVER_KEY
firebase functions:secrets:set MIDTRANS_IS_PRODUCTION
```
