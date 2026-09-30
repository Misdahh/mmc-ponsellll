# MMC PONSEL v1.30

## Login Admin Firebase privat
- Username admin tetap `miss` dan tidak ditampilkan pada halaman login umum maupun keterangan login admin.
- Login admin tetap menggunakan Firebase Authentication pada akun `miss@mmcponsel.app`.
- Password admin **tidak disimpan di source code/APK**. Untuk password yang diminta, set password akun Firebase tersebut menjadi `miss11` melalui Firebase Console.
- Cloud Function `ensureAdminRole` tetap memverifikasi akun admin dan memberikan custom claim `admin: true`.

Firebase Authentication mendukung login email/password, dan custom claims dapat dipakai untuk kontrol akses admin.
