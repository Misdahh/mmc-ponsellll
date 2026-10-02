# MMC PONSEL v1.48 — OTP Email Online Aman

Versi ini sudah menghubungkan layar **Lupa Password** Android ke Firebase Cloud Functions:

1. Pengguna memasukkan **username atau email**.
2. Server membuat OTP 6 digit secara acak.
3. Server menyimpan **hash OTP**, bukan OTP plaintext.
4. Resend mengirim OTP ke email akun.
5. OTP berlaku 10 menit, maksimal 5 percobaan, dan permintaan baru dibatasi 60 detik.
6. Setelah OTP benar, server mengubah password Firebase Authentication.

## Konfigurasi Resend

API key **tidak dimasukkan ke Android** dan tidak ditulis di `index.js` sebagai teks biasa.
Gunakan Firebase Secrets:

```bash
firebase functions:secrets:set RESEND_API_KEY
firebase functions:secrets:set RESEND_FROM
firebase deploy --only functions
```

Saat diminta `RESEND_API_KEY`, masukkan API key Resend Anda di terminal. Jangan kirim API key ke source code atau APK.

Contoh `RESEND_FROM`:

```text
MMC PONSEL <noreply@domainanda.com>
```

Alamat/domain pengirim harus sudah diverifikasi di Resend.

## Setelah deploy

Pasang/build APK v1.48. Menu **Lupa Password** sekarang memakai OTP online, bukan link reset Firebase lama.

> Catatan: project ini tidak menyertakan API key. Secret harus dimasukkan sendiri pada project Firebase Anda.
