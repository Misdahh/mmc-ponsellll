const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {onRequest} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();
const MIDTRANS_SERVER_KEY = defineSecret("MIDTRANS_SERVER_KEY");
const MIDTRANS_IS_PRODUCTION = defineSecret("MIDTRANS_IS_PRODUCTION");
const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");
const RESEND_API_KEY = defineSecret("RESEND_API_KEY");
const RESEND_FROM = defineSecret("RESEND_FROM");

// Resolves the public username to its Firebase Authentication email. This is
// intentionally a callable endpoint that does not require a prior login so a
// user can sign in for the first time from any new phone/device. Passwords are
// never handled or returned here; Firebase Authentication verifies the password.
exports.lookupUsername = onCall(async (request) => {
  const username = String((request.data || {}).username || "").trim().toLowerCase();
  if (!/^[a-z0-9._-]{3,40}$/.test(username)) {
    throw new HttpsError("invalid-argument", "Username tidak valid.");
  }
  const snap = await db.collection("users").where("username", "==", username).limit(1).get();
  if (snap.empty) return {exists: false};
  const data = snap.docs[0].data() || {};
  return {
    exists: true,
    email: String(data.email || ""),
    uid: String(data.uid || snap.docs[0].id)
  };
});


// Real AI chat for the Android app. The API key stays on the server and is never shipped in the APK.
exports.chatWithAI = onCall({secrets: [OPENAI_API_KEY]}, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Silakan login terlebih dahulu.");
  const message = String((request.data || {}).message || "").trim();
  if (!message || message.length > 4000) {
    throw new HttpsError("invalid-argument", "Pesan AI harus diisi dan maksimal 4000 karakter.");
  }

  const uid = request.auth.uid;
  const messagesRef = db.collection("aiChats").doc(uid).collection("messages");
  await messagesRef.add({role: "user", text: message, createdAt: admin.firestore.FieldValue.serverTimestamp()});

  const historySnap = await messagesRef.orderBy("createdAt", "desc").limit(12).get();
  const history = historySnap.docs.reverse().map(doc => {
    const d = doc.data();
    return {role: d.role === "assistant" ? "assistant" : "user", content: String(d.text || "").slice(0, 4000)};
  });

  const apiResponse = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${OPENAI_API_KEY.value()}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: process.env.OPENAI_MODEL || "gpt-6-luna",
      instructions: "Anda adalah asisten AI untuk aplikasi MMC PONSEL. Jawab dalam bahasa Indonesia dengan ramah, ringkas, jelas, dan membantu. Jangan mengaku sebagai manusia. Untuk pertanyaan yang membutuhkan data terkini, jelaskan bahwa Anda perlu sumber terkini jika tidak tersedia.",
      input: history
    })
  });

  const result = await apiResponse.json();
  if (!apiResponse.ok) {
    console.error("OpenAI API error", result);
    throw new HttpsError("internal", "Layanan AI sedang tidak tersedia.");
  }

  const answer = String(result.output_text || "Maaf, AI belum memberikan jawaban.").trim();
  await messagesRef.add({role: "assistant", text: answer, createdAt: admin.firestore.FieldValue.serverTimestamp()});
  return {answer};
});

exports.createMidtransTransaction = onCall({secrets: [MIDTRANS_SERVER_KEY, MIDTRANS_IS_PRODUCTION]}, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Silakan login terlebih dahulu.");
  const {orderId, grossAmount, productName, email} = request.data || {};
  if (!orderId || !Number.isInteger(grossAmount) || grossAmount <= 0) {
    throw new HttpsError("invalid-argument", "Data pembayaran tidak valid.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists || orderSnap.data().uid !== request.auth.uid) {
    throw new HttpsError("permission-denied", "Pesanan tidak ditemukan atau bukan milik pengguna.");
  }

  const production = MIDTRANS_IS_PRODUCTION.value() === "true";
  const endpoint = production ? "https://app.midtrans.com/snap/v1/transactions" : "https://app.sandbox.midtrans.com/snap/v1/transactions";
  const auth = Buffer.from(`${MIDTRANS_SERVER_KEY.value()}:`).toString("base64");
  const body = {
    transaction_details: {order_id: `MMC-${orderId}`, gross_amount: grossAmount},
    item_details: [{id: orderId, price: grossAmount, quantity: 1, name: String(productName || "Produk MMC PONSEL").slice(0, 50)}],
    customer_details: {email: email || ""},
  };

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {"Accept": "application/json", "Content-Type": "application/json", "Authorization": `Basic ${auth}`},
    body: JSON.stringify(body),
  });
  const result = await response.json();
  if (!response.ok || !result.redirect_url) {
    console.error("Midtrans error", result);
    throw new HttpsError("internal", "Gateway pembayaran gagal membuat transaksi.");
  }
  await orderRef.update({paymentGateway: "midtrans", midtransOrderId: `MMC-${orderId}`, paymentUrl: result.redirect_url, status: "Menunggu Pembayaran", paymentStatus: "pending"});
  return {redirectUrl: result.redirect_url, token: result.token || null};
});

exports.midtransNotification = onRequest({secrets: [MIDTRANS_SERVER_KEY]}, async (req, res) => {
  if (req.method !== "POST") return res.status(405).send("Method Not Allowed");
  const body = req.body || {};
  const orderId = String(body.order_id || "").replace(/^MMC-/, "");
  if (!orderId) return res.status(400).send("Missing order_id");
  const status = String(body.transaction_status || "pending");
  const fraud = String(body.fraud_status || "");
  const grossAmount = String(body.gross_amount || "");
  const signature = String(body.signature_key || "");
  const expectedSignature = crypto.createHash("sha512").update(`${body.order_id}${body.status_code}${grossAmount}${MIDTRANS_SERVER_KEY.value()}`).digest("hex");
  if (!signature || signature !== expectedSignature) return res.status(403).send("Invalid signature");
  const ref = db.collection("orders").doc(orderId);
  const snap = await ref.get();
  if (!snap.exists) return res.status(404).send("Order not found");
  let paymentStatus = "pending";
  let appStatus = "Menunggu Pembayaran";
  if (status === "settlement" || (status === "capture" && fraud !== "deny")) { paymentStatus = "paid"; appStatus = "Pembayaran Berhasil"; }
  else if (["cancel", "deny", "expire"].includes(status)) { paymentStatus = "failed"; appStatus = "Pembayaran Gagal"; }
  else if (status === "refund" || status === "partial_refund") { paymentStatus = "refunded"; appStatus = "Dana Dikembalikan"; }
  await ref.update({paymentStatus, status: appStatus, midtransTransactionStatus: status, updatedAt: admin.firestore.FieldValue.serverTimestamp()});
  return res.status(200).send("OK");
});

// Member role: the dedicated member account may receive the member custom claim.
exports.ensureMemberRole = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Login diperlukan.");
  const user = await admin.auth().getUser(request.auth.uid);
  if (user.email !== "member.miss@mmcponsel.app") {
    throw new HttpsError("permission-denied", "Akun ini bukan akun member MMC PONSEL.");
  }
  await admin.auth().setCustomUserClaims(user.uid, {...(user.customClaims || {}), member: true});
  return {ok: true};
});


// Admin role is granted only to the fixed owner account.
exports.ensureAdminRole = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Login diperlukan.");
  const user = await admin.auth().getUser(request.auth.uid);
  // Admin is identified by the fixed Firebase Authentication email.
  // Email verification is intentionally NOT required because the owner may
  // create the account directly from Firebase Console. A valid Firebase
  // password login already proves possession of the account credentials.
  if ((user.email || "").toLowerCase() !== "admin1@mmcponsel.app") {
    throw new HttpsError("permission-denied", "Akun Firebase bukan akun admin MMC PONSEL.");
  }
  await admin.auth().setCustomUserClaims(user.uid, {...(user.customClaims || {}), admin: true});
  return {ok: true};
});

// Deletes a Firebase Authentication account and its public profile.
// Only the owner admin account can call this function.
exports.adminDeleteUser = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Login diperlukan.");
  const caller = await admin.auth().getUser(request.auth.uid);
  if (caller.email !== "admin1@mmcponsel.app") {
    throw new HttpsError("permission-denied", "Hanya admin yang boleh menghapus pengguna.");
  }
  const uid = String((request.data || {}).uid || "");
  if (!uid || uid === request.auth.uid) {
    throw new HttpsError("invalid-argument", "UID pengguna tidak valid.");
  }
  await admin.auth().deleteUser(uid);
  await db.collection("users").doc(uid).delete();
  return {ok: true};
});


// v1.48: Real online email OTP password reset.
// OTP is generated and verified server-side. Resend credentials are Firebase Secrets only.
async function findUserForPasswordReset(identifier) {
  const value = String(identifier || "").trim().toLowerCase();
  if (!value) return null;
  let snap = await db.collection("users").where("usernameLower", "==", value).limit(1).get();
  if (!snap.empty) return snap.docs[0];
  snap = await db.collection("users").where("email", "==", value).limit(1).get();
  if (!snap.empty) return snap.docs[0];
  return null;
}

exports.requestPasswordResetOtp = onCall({secrets: [RESEND_API_KEY, RESEND_FROM]}, async (request) => {
  const identifier = String(request.data?.username || request.data?.identifier || "").trim().toLowerCase();
  if (!identifier) throw new HttpsError("invalid-argument", "Username atau email wajib diisi.");

  const userDoc = await findUserForPasswordReset(identifier);
  if (!userDoc) throw new HttpsError("not-found", "Akun tidak ditemukan.");

  const data = userDoc.data() || {};
  const email = String(data.email || "").trim().toLowerCase();
  if (!email) throw new HttpsError("failed-precondition", "Akun belum memiliki email pemulihan.");

  const existingOtp = await db.collection("passwordResetOtps").doc(userDoc.id).get();
  if (existingOtp.exists) {
    const old = existingOtp.data();
    if (old.lastSentAt && Date.now() - Number(old.lastSentAt) < 60 * 1000) {
      throw new HttpsError("resource-exhausted", "Tunggu 60 detik sebelum meminta kode baru.");
    }
  }

  const code = String(crypto.randomInt(100000, 1000000));
  const codeHash = crypto.createHash("sha256").update(code).digest("hex");
  const expiresAt = Date.now() + 10 * 60 * 1000;

  await db.collection("passwordResetOtps").doc(userDoc.id).set({
    uid: userDoc.id, email, codeHash, expiresAt,
    attempts: 0, lastSentAt: Date.now(), createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await sendOtpEmail(email, code);

  return { ok: true, maskedEmail: email.replace(/^(.{2}).*(@.*)$/, "$1***$2") };
});

exports.verifyPasswordResetOtp = onCall(async (request) => {
  const identifier = String(request.data?.username || request.data?.identifier || "").trim().toLowerCase();
  const code = String(request.data?.code || "").trim();
  const newPassword = String(request.data?.newPassword || "");

  if (!identifier || !/^\d{6}$/.test(code) || newPassword.length < 6)
    throw new HttpsError("invalid-argument", "Data verifikasi tidak valid.");

  const userDoc = await findUserForPasswordReset(identifier);
  if (!userDoc) throw new HttpsError("not-found", "Akun tidak ditemukan.");
  const uid = userDoc.id;

  const ref = db.collection("passwordResetOtps").doc(uid);
  const otpSnap = await ref.get();
  if (!otpSnap.exists) throw new HttpsError("failed-precondition", "Kode belum diminta.");
  const otp = otpSnap.data();

  if (Date.now() > Number(otp.expiresAt)) {
    await ref.delete();
    throw new HttpsError("deadline-exceeded", "Kode sudah kedaluwarsa.");
  }
  if (Number(otp.attempts || 0) >= 5)
    throw new HttpsError("resource-exhausted", "Terlalu banyak percobaan.");

  const hash = crypto.createHash("sha256").update(code).digest("hex");
  if (hash !== otp.codeHash) {
    await ref.update({ attempts: admin.firestore.FieldValue.increment(1) });
    throw new HttpsError("permission-denied", "Kode verifikasi salah.");
  }

  await admin.auth().updateUser(uid, { password: newPassword });
  await ref.delete();
  return { ok: true };
});


// v1.48 production email OTP provider.
// The API key is server-only and is never shipped in the Android app.
async function sendOtpEmail(to, code) {
  const apiKey = RESEND_API_KEY.value();
  const from = RESEND_FROM.value();
  if (!apiKey || !from) {
    throw new HttpsError("failed-precondition", "Layanan email OTP belum dikonfigurasi di server.");
  }
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      from: from,
      to: [to],
      subject: "MMC PONSEL - Kode OTP Reset Password",
      text: `Kode OTP MMC PONSEL Anda adalah ${code}. Kode berlaku 10 menit. Jangan bagikan kode ini kepada siapa pun.`,
      html: `<p>Kode OTP MMC PONSEL Anda:</p><h2 style="letter-spacing:4px">${code}</h2><p>Kode berlaku 10 menit. Jangan bagikan kode ini kepada siapa pun.</p>`
    })
  });
  if (!response.ok) {
    const detail = await response.text();
    console.error("Resend email error", response.status, detail);
    throw new HttpsError("internal", "Email OTP gagal dikirim. Silakan coba lagi.");
  }
}
