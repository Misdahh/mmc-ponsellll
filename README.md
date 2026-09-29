# MMC PONSEL Android — Online

MMC PONSEL sekarang memakai **Firebase sebagai backend online**, bukan penyimpanan lokal sebagai sumber data toko.

## Fitur online
- Firebase Authentication: Google + Facebook.
- Cloud Firestore untuk katalog produk.
- Produk yang ditambah admin langsung tersimpan di server dan dapat muncul di perangkat pengguna lain.
- Keranjang pengguna disimpan di `users/{uid}/cart`.
- Checkout membuat dokumen pesanan online di `orders`.
- Permintaan service tersimpan di `serviceRequests`.
- Penawaran jual HP tersimpan di `sellRequests`.
- Chat pengguna ↔ admin memakai Firestore realtime listener.
- Profil/alamat pengguna disimpan di Firestore.
- Tidak ada katalog produk contoh yang ditanam sebagai sumber utama aplikasi.

Cloud Firestore memang menyediakan sinkronisasi data realtime antar perangkat yang terhubung. Firebase Authentication mengikat data cloud ke identitas pengguna. Lihat dokumentasi resmi Firebase untuk Authentication dan Firestore.

## Login pengguna
1. Splash/loading animasi.
2. Login Google atau Facebook menggunakan Firebase Authentication.
3. Setelah login, pengguna masuk ke katalog online.

Google memakai Credential Manager + Firebase Authentication. Facebook memakai Facebook Login + Firebase Authentication.

## Admin
- Username: `miss`
- Password: `miss22`
- Tambah barang ke `products`.
- Lihat/balas chat pengguna.

Untuk produksi, kredensial admin sebaiknya dipindahkan ke mekanisme role server-side/Firebase custom claims. Password yang ditanam atau diverifikasi di APK tidak boleh dianggap sebagai kontrol keamanan utama.

## Firebase wajib
Tambahkan `app/google-services.json` dari Firebase Console.

Aktifkan:
- Authentication → Google
- Authentication → Facebook
- Authentication → Email/Password untuk akun admin
- Cloud Firestore

Tambahkan SHA-1/SHA-256 Android ke Firebase dan konfigurasi Facebook App ID/App Secret + OAuth redirect URI.

## Firestore rules
Gunakan `firestore.rules` pada project Firebase Anda. Rules membatasi katalog/operasi admin berdasarkan akun admin dan membatasi data pengguna berdasarkan `uid`.

## Build
GitHub Actions di `.github/workflows/android.yml` akan membangun APK dengan Android SDK dan Gradle. APK produksi/release tetap memerlukan konfigurasi Firebase/Meta milik pemilik aplikasi dan signing keystore milik pemilik aplikasi.

## Catatan pembayaran
Checkout saat ini membuat **pesanan online**, tetapi belum menarik pembayaran kartu/e-wallet secara otomatis. Untuk pembayaran nyata, hubungkan gateway seperti Midtrans/Xendit menggunakan kredensial merchant milik Anda dan backend/server-side yang aman.

## Pembayaran asli Midtrans
Versi 1.5 menambahkan alur pembayaran Midtrans Snap melalui Firebase Cloud Functions. APK tidak menyimpan Server Key. Untuk Production, isi secret `MIDTRANS_SERVER_KEY`, set `MIDTRANS_IS_PRODUCTION=true`, deploy Functions, lalu pasang URL notification/webhook pada Dashboard Midtrans. Midtrans menyediakan Snap untuk web/app dan mendukung berbagai metode pembayaran yang diaktifkan pada akun merchant. See official docs: https://docs.midtrans.com/docs/payment-overview

## Sambutan pengguna
Beranda menampilkan kartu sambutan hangat setelah login, menyapa pengguna dengan nama akun bila tersedia dan menyampaikan ucapan terima kasih atas kepercayaan kepada MMC PONSEL. Kartu sambutan juga mencantumkan **Miss Cell — Developer & Pencipta MMC PONSEL**.

## Developer publik
Di halaman login, beranda, dan profil terdapat kartu Developer/Pencipta:
- Miss Cell
- WhatsApp: 083830655780
- Facebook: misdah
- TikTok: @tegaris82
- YouTube: @tegaris82

## Member khusus
- Username member: `miss`
- Password member: dibuat bebas saat akun member pertama kali dibuat, minimal 6 karakter.
- Hak member: menambah barang dan membalas chat pengguna.
- Member tidak diberi hak mengubah/menghapus barang atau mengelola pesanan.
- Akun member Firebase menggunakan `member.miss@mmcponsel.app` dan diberi custom claim `member=true` melalui Cloud Function `ensureMemberRole`.
- Aktifkan Firebase Authentication Email/Password dan deploy Cloud Functions sebelum login member digunakan.


## Foto Developer
Foto developer Miss Cell ditampilkan pada kartu Developer di login, beranda, dan halaman Developer agar dapat dilihat semua pengguna. Aset berada di `app/src/main/res/drawable-nodpi/developer_miss_cell.jpg`.

## Grup Solusi Admin & Member
v1.9 menambahkan ruang grup online khusus Admin dan Member pada `solutionGroup/main/messages`. Pesan tersinkron realtime melalui Firestore. Pengguna biasa tidak diberi akses oleh Firestore Rules. Admin dan Member dapat saling bertukar solusi, pengalaman service, ide jual-beli, dan tips pengelolaan toko.
