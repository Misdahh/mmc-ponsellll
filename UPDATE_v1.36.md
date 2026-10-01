# MMC PONSEL v1.36
- Pesanan servis dipublikasikan secara realtime kepada pengguna yang login.
- Pengguna dapat melihat jenis servis, keluhan, status, dan pemesan.
- Ditambahkan chat pribadi antar pengguna melalui `privateChats/{chatId}`.
- Firestore Rules membatasi chat pribadi hanya kepada peserta chat.
- Pesanan servis tetap tersimpan di Firestore collection `serviceRequests`.
