# MMC PONSEL v1.31 — Perbaikan Login Admin Firebase

- Username admin `miss` tidak ditampilkan sebagai menu/tombol.
- Jika `miss` dimasukkan pada form login utama, aplikasi mengautentikasi akun Firebase admin `miss@mmcponsel.app`.
- Password tidak ditanam di APK/source code; password diatur di Firebase Authentication.
- Hak `admin` tetap diberikan oleh Cloud Function `ensureAdminRole` setelah autentikasi berhasil.
- Akses long-press logo untuk halaman admin privat tetap tersedia.
