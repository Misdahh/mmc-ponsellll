const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {onRequest} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();
const MIDTRANS_SERVER_KEY = defineSecret("MIDTRANS_SERVER_KEY");
const MIDTRANS_IS_PRODUCTION = defineSecret("MIDTRANS_IS_PRODUCTION");

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
  if (user.email !== "miss@mmcponsel.app") {
    throw new HttpsError("permission-denied", "Akun ini bukan akun admin MMC PONSEL.");
  }
  await admin.auth().setCustomUserClaims(user.uid, {...(user.customClaims || {}), admin: true});
  return {ok: true};
});

// Deletes a Firebase Authentication account and its public profile.
// Only the owner admin account can call this function.
exports.adminDeleteUser = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Login diperlukan.");
  const caller = await admin.auth().getUser(request.auth.uid);
  if (caller.email !== "miss@mmcponsel.app") {
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
