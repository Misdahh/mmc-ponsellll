# MMC PONSEL — Setup OTP Tanpa Terminal (v1.49)

Versi ini menambahkan workflow GitHub Actions sehingga Cloud Functions dapat dideploy dari browser.
API key Resend tetap berada di GitHub Secret → Google Secret Manager → Firebase Functions.
API key tidak masuk ke APK.

## Yang perlu disiapkan di browser

### A. Buat repository GitHub
1. Buka GitHub dan buat repository baru, misalnya `mmc-ponsel`.
2. Upload seluruh isi folder project ini ke repository.
3. Pastikan file `.github/workflows/deploy-firebase.yml` ikut ter-upload.
4. Gunakan branch `main`.

### B. Buat Service Account untuk deployment
Di Google Cloud Console:
1. Pilih project Firebase MMC PONSEL.
2. Buka IAM & Admin → Service Accounts.
3. Buat service account, misalnya `mmc-ponsel-deployer`.
4. Berikan permission yang diperlukan untuk deployment dan Secret Manager:
   - Cloud Functions Admin
   - Service Account User
   - Secret Manager Admin
   - Firebase Admin (jika tersedia pada project Anda)
5. Buat JSON key untuk service account dan download.

Jaga file JSON tersebut. Jangan masukkan ke source code.

### C. Tambahkan GitHub Secrets
Di repository GitHub:
Settings → Secrets and variables → Actions → New repository secret.

Tambahkan:

**Wajib**
- `FIREBASE_PROJECT_ID` = ID project Firebase
- `FIREBASE_SERVICE_ACCOUNT` = seluruh isi file JSON service account
- `RESEND_API_KEY` = API key Resend baru (`re_...`)
- `RESEND_FROM` = contoh `MMC PONSEL <noreply@domain-terverifikasi.com>`

**Opsional sesuai fitur yang dipakai**
- `OPENAI_API_KEY`
- `MIDTRANS_SERVER_KEY`
- `MIDTRANS_IS_PRODUCTION` = `true` atau `false`

Jangan memasukkan API key ke `app/`, `AndroidManifest.xml`, atau `functions/index.js`.

### D. Jalankan deployment dari browser
1. Buka tab **Actions** pada repository GitHub.
2. Pilih workflow **Deploy Firebase from Browser**.
3. Tekan **Run workflow**.
4. Tunggu sampai semua langkah berwarna hijau.

Workflow akan:
- login ke Google Cloud memakai service account;
- membuat/update Secret Manager untuk Resend;
- menjalankan deployment Firebase Cloud Functions;
- tidak meminta CMD/Termux.

## Penting
API key Resend yang pernah dikirim melalui chat sebaiknya dicabut di Resend.
Buat API key baru dan masukkan hanya ke GitHub Secret `RESEND_API_KEY`.

`RESEND_FROM` harus merupakan sender/domain yang sudah diverifikasi di Resend.

## Jika deployment gagal
Buka:
GitHub → Actions → Deploy Firebase from Browser → klik run yang gagal.

Pesan error biasanya menunjukkan permission yang belum diberikan pada service account.

## Hasil
Setelah workflow berhasil, fungsi:
- `requestPasswordResetOtp`
- `verifyPasswordResetOtp`

akan tersedia online dan aplikasi Android dapat meminta OTP email tanpa menyimpan API key Resend di APK.
