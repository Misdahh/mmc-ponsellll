# Firebase Admin Login MMC PONSEL

Login admin sekarang memakai **Firebase Authentication Email/Password**. Aplikasi tidak lagi membuat akun admin otomatis dan tidak menyimpan password admin di source code.

## 1. Aktifkan Email/Password

Di Firebase Console buka **Authentication → Sign-in method → Email/Password**, lalu aktifkan provider tersebut. Firebase mendukung login password melalui SDK Android.

## 2. Buat akun admin

Di **Authentication → Users**, buat satu akun admin dengan email:

`admin1@mmcponsel.app`

Buat password admin sendiri dan tandai/verifikasi email akun tersebut. Jangan menaruh password itu di GitHub atau source code.

## 3. Deploy Cloud Functions

Dari root project jalankan:

```bash
firebase deploy --only functions
```

Function `ensureAdminRole` memeriksa bahwa akun yang sedang login adalah `admin1@mmcponsel.app` dan emailnya sudah terverifikasi, kemudian memberikan custom claim `admin: true`. Custom claims harus ditetapkan dari lingkungan server menggunakan Firebase Admin SDK.

## 4. Cara login di aplikasi

Halaman login umum tidak menampilkan login admin. Tekan lama logo hacker untuk membuka **Login Admin**, lalu masukkan:

- Username: `admin1`
- Password: password akun Firebase admin yang dibuat pada langkah 2

Aplikasi kemudian memanggil `ensureAdminRole` dan menyegarkan sesi admin.

## 5. Keamanan

Hak akses admin tidak boleh hanya bergantung pada tampilan aplikasi. Firestore/Storage/Cloud Functions harus memeriksa custom claim `admin`. Firebase mendokumentasikan custom claims sebagai mekanisme role-based access control yang divalidasi melalui ID token.
