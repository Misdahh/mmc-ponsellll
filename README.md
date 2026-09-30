# MMC PONSEL 2.3

Project Android native untuk MMC PONSEL dengan Firebase.

## UI yang diperbarui
- Beranda dengan header brand, pencarian, banner, layanan utama, katalog produk, dan bottom navigation.
- Halaman kategori dengan filter dan pilihan merek.
- Halaman service berbentuk daftar kartu.
- Detail produk, checkout, transaksi, profil, chat, admin, dan member tetap tersedia.
- Tombol kembali di halaman sekunder dan tombol Back Android mengikuti riwayat halaman.
- Tema/animasi latar berbeda berdasarkan halaman.
- Firebase Authentication dan Firestore tetap digunakan.
- Package: `com.mmcponsel.app`
- Firebase project: `mmc-ponsel`

## Build
Buka di Android Studio lalu Sync Project dan Build APK.

Untuk Google Sign-In, aktifkan provider Google di Firebase Authentication dan tambahkan SHA-1/SHA-256 sesuai keystore build.

Untuk Facebook Login, isi App ID/Client Token Meta dan konfigurasi provider Facebook di Firebase.

## MMC PONSEL 3.0 — Marketplace fields

The product form includes product name, price, category/subcategory, condition, brand, model/type, color, RAM, storage, stock, SKU, location, shipping, damage notes, description, and product photo upload to Firebase Storage.

### APK build

The repository contains a GitHub Actions workflow at `.github/workflows/android.yml` that builds `app-debug.apk` and uploads it as the `mmc-ponsel-debug-apk` artifact. The build was not executed in this offline workspace because Gradle/Android dependencies could not be downloaded here.
