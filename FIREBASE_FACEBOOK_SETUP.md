# Login Google + Facebook MMC PONSEL

Login sekarang menggunakan **Firebase Authentication** dengan provider Google dan Facebook.

## 1. Google
1. Buat project di Firebase Console.
2. Tambahkan Android app dengan package `com.mmcponsel.app`.
3. Tambahkan SHA-1/SHA-256 untuk keystore yang dipakai build.
4. Aktifkan Authentication > Sign-in method > Google.
5. Download `google-services.json` dan letakkan di `app/google-services.json`.
6. Pastikan OAuth client tipe **Web application** tersedia; Firebase akan membuat resource `default_web_client_id` yang dipakai Credential Manager.

## 2. Facebook
1. Buat app di Meta for Developers dan aktifkan Facebook Login.
2. Isi `app/src/main/res/values/strings.xml`:
   - `facebook_app_id`
   - `facebook_client_token`
3. Di Firebase Authentication aktifkan Facebook dan masukkan App ID + App Secret.
4. Di Meta/Facebook Login, tambahkan OAuth redirect URI yang diberikan Firebase, biasanya berbentuk:
   `https://<PROJECT_ID>.firebaseapp.com/__/auth/handler`
5. Ganti placeholder scheme `fbYOUR_FACEBOOK_APP_ID` pada `AndroidManifest.xml` menjadi `fb<ID_APP_FACEBOOK>`.

Firebase mendokumentasikan bahwa Google Android sign-in menggunakan Credential Manager dan client ID Web/server, sedangkan Facebook Login memerlukan App ID/App Secret, provider Firebase yang diaktifkan, dan redirect URI yang sesuai.

## Penting
- Jangan commit App Secret Facebook, service-account JSON, keystore, atau password ke GitHub.
- `google-services.json` bukan pengganti konfigurasi provider: Google dan Facebook tetap harus diaktifkan di Firebase Console.
- Tanpa konfigurasi Firebase/Meta milik pemilik aplikasi, tombol login tidak dapat menjadi akun real. Source code sudah menyiapkan alurnya; kredensial produksi harus milik Anda sendiri.

## Firestore untuk Produk & Chat Admin

1. Di Firebase Console, aktifkan **Cloud Firestore**.
2. Aktifkan **Authentication → Sign-in method → Email/Password** agar akun admin internal dapat dibuat saat pertama kali login.
3. Login admin di aplikasi menggunakan:
   - Username: `miss`
   - Password: `miss22`
   - Akun Firebase internal yang dipakai aplikasi: `admin1@mmcponsel.app`
4. Deploy isi `firestore.rules` ke Firestore Rules.
5. Produk admin disimpan pada collection `products`.
6. Chat pengguna disimpan pada `chats/{uid}/messages`.

Untuk produksi, lebih aman memakai Firebase custom claims/server-side untuk role admin daripada mengandalkan kredensial yang tertanam di aplikasi. Firebase Security Rules memang mendukung pembatasan berdasarkan `auth.token`/custom claims.
