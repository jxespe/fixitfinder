const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();

function normalizePhoneToE164(raw) {
  const digits = String(raw || "").replace(/\D/g, "");
  if (!digits) {
    return null;
  }
  if (digits.startsWith("63") && digits.length >= 11) {
    return `+${digits}`;
  }
  if (digits.startsWith("0") && digits.length >= 10) {
    return `+63${digits.slice(1)}`;
  }
  if (digits.length === 10) {
    return `+63${digits}`;
  }
  if (raw && String(raw).trim().startsWith("+")) {
    return `+${digits}`;
  }
  return `+${digits}`;
}

function normalizeStoredPhone(phone) {
  if (!phone || typeof phone !== "string") {
    return null;
  }
  const t = phone.trim();
  if (t.startsWith("+")) {
    return t;
  }
  return normalizePhoneToE164(t);
}

/** Firestore profiles may use different field names for the same number. */
function pickPhoneFromProfile(profile) {
  if (!profile || typeof profile !== "object") {
    return null;
  }
  const raw =
    profile.phone ||
    profile.phoneNumber ||
    profile.mobile ||
    profile.phoneE164 ||
    null;
  if (raw == null || raw === "") {
    return null;
  }
  return normalizeStoredPhone(String(raw));
}

function isStrongPassword(pwd) {
  if (!pwd || pwd.length < 8) {
    return false;
  }
  return /[A-Za-z]/.test(pwd) && /\d/.test(pwd);
}

const PASSWORD_RESET_CALLABLE_OPTS = {
  region: "us-central1",
  invoker: "public",
};

function mapAuthLookupError(e) {
  if (e instanceof HttpsError) {
    throw e;
  }
  const code = e?.errorInfo?.code || e?.code || "";
  if (code === "auth/user-not-found") {
    throw new HttpsError(
      "not-found",
      "No account matches that email or phone number."
    );
  }
  if (code === "auth/invalid-email") {
    throw new HttpsError("invalid-argument", "Invalid email address.");
  }
  if (code === "auth/invalid-phone-number") {
    throw new HttpsError("invalid-argument", "Invalid phone number.");
  }
  console.error("preparePasswordReset auth error", code, e?.message || e);
  throw new HttpsError(
    "internal",
    "Unable to verify your account right now. Please try again."
  );
}

/**
 * Unauthenticated: resolve account by email or phone for SMS OTP step.
 * roleFilter: "user" | "provider" — must match Firestore profile collection.
 */
exports.preparePasswordReset = onCall(PASSWORD_RESET_CALLABLE_OPTS, async (request) => {
  const identifier = String(request.data?.identifier || "").trim();
  const roleFilter = String(request.data?.roleFilter || "user").toLowerCase();
  if (!identifier) {
    throw new HttpsError("invalid-argument", "Enter your email or phone number.");
  }

  let userRecord;
  try {
    if (identifier.includes("@")) {
      userRecord = await admin
        .auth()
        .getUserByEmail(identifier.toLowerCase());
    } else {
      const e164 = normalizePhoneToE164(identifier);
      if (!e164) {
        throw new HttpsError("invalid-argument", "Invalid phone number.");
      }
      userRecord = await admin.auth().getUserByPhoneNumber(e164);
    }
  } catch (e) {
    mapAuthLookupError(e);
  }

  const uid = userRecord.uid;
  const db = admin.firestore();
  const userSnap = await db.collection("users").doc(uid).get();
  const provSnap = await db.collection("providers").doc(uid).get();

  const wantProvider = roleFilter === "provider";
  if (wantProvider && !provSnap.exists) {
    throw new HttpsError(
      "not-found",
      "No provider account matches that email or phone number."
    );
  }

  // Consumer flow: Auth already matched — do not require a Firestore users/{uid} doc
  // (some accounts exist only in Auth or only under providers after partial signup).
  if (!wantProvider && provSnap.exists && !userSnap.exists) {
    throw new HttpsError(
      "failed-precondition",
      "This account is registered as a service provider. Use Provider Login, then Forgot password there."
    );
  }

  const userData = userSnap.data() || {};
  const provData = provSnap.data() || {};
  const profile = wantProvider ? provData : { ...provData, ...userData };

  let phoneE164 = userRecord.phoneNumber || pickPhoneFromProfile(profile);
  if (!phoneE164) {
    throw new HttpsError(
      "failed-precondition",
      "This account has no phone number on file. Contact support to reset your password."
    );
  }

  let email =
    (userRecord.email && userRecord.email.trim()) ||
    (profile.email && String(profile.email).trim()) ||
    "";
  if (!email && identifier.includes("@")) {
    email = identifier.toLowerCase().trim();
  }
  email = email ? email.toLowerCase() : "";

  return { phoneE164, email };
});

/**
 * Authenticated (after phone OTP): set new password without old password.
 */
exports.completePasswordReset = onCall(PASSWORD_RESET_CALLABLE_OPTS, async (request) => {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Sign in with the verification code first.");
  }
  const password = request.data?.password;
  if (!isStrongPassword(password)) {
    throw new HttpsError(
      "invalid-argument",
      "Password must be at least 8 characters and include letters and numbers."
    );
  }
  await admin.auth().updateUser(request.auth.uid, { password: String(password) });
  return { success: true };
});

const REMINDER_WINDOW_MS = 60 * 60 * 1000;
const REMINDER_LATE_GRACE_MS = 10 * 60 * 1000;

function formatTime(timestamp) {
  try {
    const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
    return new Intl.DateTimeFormat("en-US", {
      hour: "numeric",
      minute: "2-digit",
    }).format(date);
  } catch (e) {
    return "soon";
  }
}

function buildUserMessage(serviceName, providerName, scheduledAt) {
  const timeLabel = formatTime(scheduledAt);
  if (serviceName) {
    return `Your ${serviceName} appointment will start at ${timeLabel} today.`;
  }
  if (providerName) {
    return `Your appointment with ${providerName} will start at ${timeLabel} today.`;
  }
  return `Your appointment will start at ${timeLabel} today.`;
}

function buildProviderMessage(serviceName, customerName, scheduledAt) {
  const timeLabel = formatTime(scheduledAt);
  if (serviceName) {
    return `Your ${serviceName} job will start at ${timeLabel} today.`;
  }
  if (customerName) {
    return `Your job for ${customerName} will start at ${timeLabel} today.`;
  }
  return `Your job will start at ${timeLabel} today.`;
}

function formatScheduleLabel(data) {
  const scheduledAt = data?.scheduledAt?.toDate
    ? data.scheduledAt.toDate()
    : null;
  if (scheduledAt) {
    const dateLabel = new Intl.DateTimeFormat("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    }).format(scheduledAt);
    return dateLabel;
  }
  const dateKey = data?.dateKey || "";
  const timeSlot = data?.timeSlot || "";
  return [dateKey, timeSlot].filter(Boolean).join(" ");
}

function buildStatusMessage(status, providerName, serviceName, bookingNumber, scheduleLabel) {
  const readable = (status || "updated").toLowerCase();
  let statusLabel = readable;
  if (readable === "on process" || readable === "on-process" || readable === "ongoing") {
    statusLabel = "on process";
  }
  if (readable === "accepted") statusLabel = "accepted";
  if (readable === "declined") statusLabel = "declined";
  if (readable === "finished") statusLabel = "finished";
  if (readable === "cancelled") statusLabel = "cancelled";
  if (readable === "rescheduled") statusLabel = "rescheduled";
  let base = "Your booking";
  if (bookingNumber) {
    base += ` #${bookingNumber}`;
  }
  if (statusLabel === "rescheduled") {
    base += " was rescheduled.";
  } else {
    base += ` is ${statusLabel}.`;
  }
  if (serviceName) {
    base += ` ${serviceName}`;
  }
  if (providerName) {
    base += ` with ${providerName}`;
  }
  if (statusLabel === "rescheduled" && scheduleLabel) {
    base += `. New schedule: ${scheduleLabel}`;
  }
  return base.trim();
}

/**
 * Data-only FCM so Android does not show a system notification while the app handles delivery.
 * Client uses targetUserId to queue when logged out and to suppress tray for the wrong account.
 */
async function sendToToken(token, title, body, targetUserId, data) {
  if (!token) {
    return { success: false, reason: "missing_token" };
  }
  const payload = {
    title: String(title ?? ""),
    body: String(body ?? ""),
    targetUserId: String(targetUserId ?? ""),
    clickAction: "OPEN_APP",
  };
  const extra = data && typeof data === "object" ? data : {};
  for (const [k, v] of Object.entries(extra)) {
    if (v === undefined || v === null) continue;
    payload[String(k)] = typeof v === "string" ? v : String(v);
  }
  const message = {
    token,
    data: payload,
    android: {
      priority: "high",
    },
  };
  await admin.messaging().send(message);
  return { success: true };
}

async function saveNotificationRecord(payload) {
  if (!payload || !payload.userId) {
    return;
  }
  await admin.firestore().collection("notifications").add({
    ...payload,
    seen: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}

exports.sendReminderNotifications = onSchedule("every 1 minutes", async () => {
    const now = Date.now();
    const windowEnd = new Date(now + REMINDER_WINDOW_MS);
    const lateCutoff = now - REMINDER_LATE_GRACE_MS;

    const snapshot = await admin
      .firestore()
      .collection("bookings")
      .where("status", "==", "accepted")
      .where("scheduledAt", "<=", windowEnd)
      .get();

    const tasks = snapshot.docs.map(async (doc) => {
      const data = doc.data() || {};
      const scheduledAt = data.scheduledAt?.toDate
        ? data.scheduledAt.toDate()
        : null;
      if (!scheduledAt) {
        return;
      }
      const scheduledMs = scheduledAt.getTime();
      if (scheduledMs < lateCutoff) {
        return;
      }

      const reminderSentUser = data.reminderSentUser === true;
      const reminderSentProvider = data.reminderSentProvider === true;
      if (reminderSentUser && reminderSentProvider) {
        return;
      }

      const bookingId = doc.id;
      const providerName = data.providerName || "";
      const serviceName = data.serviceName || "";
      const customerName = data.customerName || data.bookedBy || "";

      const updates = {};
      const sendOps = [];

      if (!reminderSentUser && data.userId) {
        const userSnap = await admin
          .firestore()
          .collection("users")
          .doc(data.userId)
          .get();
        const token = userSnap.get("fcmToken");
        const title = "Upcoming appointment";
        const body = buildUserMessage(serviceName, providerName, scheduledAt);
        sendOps.push(
          sendToToken(token, title, body, data.userId, {
            bookingId,
            role: "user",
            type: "reminder",
          })
            .then(() =>
              saveNotificationRecord({
                userId: data.userId,
                role: "user",
                title,
                message: body,
                source: "reminder_push",
                bookingId,
              })
            )
            .then(() => {
              updates.reminderSentUser = true;
              updates.reminderSentUserAt =
                admin.firestore.FieldValue.serverTimestamp();
            })
        );
      }

      if (!reminderSentProvider && data.providerId) {
        const providerSnap = await admin
          .firestore()
          .collection("providers")
          .doc(data.providerId)
          .get();
        const token = providerSnap.get("fcmToken");
        const title = "Upcoming job";
        const body = buildProviderMessage(serviceName, customerName, scheduledAt);
        sendOps.push(
          sendToToken(token, title, body, data.providerId, {
            bookingId,
            role: "provider",
            type: "reminder",
          })
            .then(() =>
              saveNotificationRecord({
                userId: data.providerId,
                role: "provider",
                title,
                message: body,
                source: "reminder_push",
                bookingId,
              })
            )
            .then(() => {
              updates.reminderSentProvider = true;
              updates.reminderSentProviderAt =
                admin.firestore.FieldValue.serverTimestamp();
            })
        );
      }

      if (sendOps.length === 0) {
        return;
      }
      await Promise.allSettled(sendOps);
      if (Object.keys(updates).length > 0) {
        updates.reminderLastAttemptAt =
          admin.firestore.FieldValue.serverTimestamp();
        await doc.ref.set(updates, { merge: true });
      }
    });

    await Promise.allSettled(tasks);
    return null;
});

exports.onBookingAccepted = onDocumentWritten("bookings/{bookingId}", async (event) => {
  const after = event.data?.after;
  if (!after || !after.exists) {
    return;
  }
  const data = after.data() || {};
  if (data.status !== "accepted") {
    return;
  }

  const scheduledAt = data.scheduledAt?.toDate
    ? data.scheduledAt.toDate()
    : null;
  if (!scheduledAt) {
    return;
  }

  const now = Date.now();
  const scheduledMs = scheduledAt.getTime();
  const triggerAt = scheduledMs - REMINDER_WINDOW_MS;
  const shouldSendNow = triggerAt <= now && scheduledMs >= now - REMINDER_LATE_GRACE_MS;
  if (!shouldSendNow) {
    return;
  }

  const updates = {};
  const bookingId = event.params?.bookingId || after.id;
  const providerName = data.providerName || "";
  const serviceName = data.serviceName || "";
  const customerName = data.customerName || data.bookedBy || "";

  const sendOps = [];

  if (!data.reminderSentUser && data.userId) {
    const userSnap = await admin
      .firestore()
      .collection("users")
      .doc(data.userId)
      .get();
    const token = userSnap.get("fcmToken");
    const title = "Upcoming appointment";
    const body = buildUserMessage(serviceName, providerName, scheduledAt);
    sendOps.push(
      sendToToken(token, title, body, data.userId, {
        bookingId,
        role: "user",
        type: "reminder",
      })
        .then(() =>
          saveNotificationRecord({
            userId: data.userId,
            role: "user",
            title,
            message: body,
            source: "reminder_push",
            bookingId,
          })
        )
        .then(() => {
          updates.reminderSentUser = true;
          updates.reminderSentUserAt =
            admin.firestore.FieldValue.serverTimestamp();
        })
    );
  }

  if (!data.reminderSentProvider && data.providerId) {
    const providerSnap = await admin
      .firestore()
      .collection("providers")
      .doc(data.providerId)
      .get();
    const token = providerSnap.get("fcmToken");
    const title = "Upcoming job";
    const body = buildProviderMessage(serviceName, customerName, scheduledAt);
    sendOps.push(
      sendToToken(token, title, body, data.providerId, {
        bookingId,
        role: "provider",
        type: "reminder",
      })
        .then(() =>
          saveNotificationRecord({
            userId: data.providerId,
            role: "provider",
            title,
            message: body,
            source: "reminder_push",
            bookingId,
          })
        )
        .then(() => {
          updates.reminderSentProvider = true;
          updates.reminderSentProviderAt =
            admin.firestore.FieldValue.serverTimestamp();
        })
    );
  }

  if (sendOps.length === 0) {
    return;
  }
  await Promise.allSettled(sendOps);
  if (Object.keys(updates).length > 0) {
    updates.reminderLastAttemptAt =
      admin.firestore.FieldValue.serverTimestamp();
    await after.ref.set(updates, { merge: true });
  }
});

exports.onBookingCreated = onDocumentWritten("bookings/{bookingId}", async (event) => {
  const after = event.data?.after;
  const before = event.data?.before;
  if (!after || !after.exists || (before && before.exists)) {
    return;
  }
  const data = after.data() || {};
  const bookingId = event.params?.bookingId || after.id;
  if (!data.userId) {
    return;
  }
  const userSnap = await admin
    .firestore()
    .collection("users")
    .doc(data.userId)
    .get();
  const token = userSnap.get("fcmToken");
  const scheduleLabel = formatScheduleLabel(data);
  const bookingNumber = data.bookingNumber || bookingId;
  const providerName = data.providerName || "";
  const serviceName = data.serviceName || "";
  const initialStatus = String(data.status || "pending").toLowerCase();
  const title = "Booking update";
  let body = buildStatusMessage(
    initialStatus,
    providerName,
    serviceName,
    bookingNumber,
    ""
  );
  if (scheduleLabel) {
    body += ` Scheduled for ${scheduleLabel}.`;
  }

  await Promise.allSettled([
    sendToToken(token, title, body, data.userId, {
      bookingId,
      role: "user",
      type: "booking_created",
    }),
    saveNotificationRecord({
      userId: data.userId,
      role: "user",
      title,
      message: body,
      source: "booking_created",
      bookingId,
    }),
  ]);
});

exports.onBookingStatusChanged = onDocumentWritten("bookings/{bookingId}", async (event) => {
  const after = event.data?.after;
  const before = event.data?.before;
  if (!after || !after.exists || !before || !before.exists) {
    return;
  }
  const afterData = after.data() || {};
  const beforeData = before.data() || {};
  const newStatus = (afterData.status || "").toLowerCase();
  const oldStatus = (beforeData.status || "").toLowerCase();
  if (!newStatus || newStatus === oldStatus) {
    return;
  }
  if (!afterData.userId) {
    return;
  }
  const alreadyNotified = (afterData.lastNotifiedStatus || "").toLowerCase() === newStatus;
  if (alreadyNotified) {
    return;
  }
  const bookingId = event.params?.bookingId || after.id;
  const bookingNumber = afterData.bookingNumber || bookingId;
  const providerName = afterData.providerName || "";
  const serviceName = afterData.serviceName || "";
  const title = "Booking update";
  const scheduleLabel = newStatus === "rescheduled" ? formatScheduleLabel(afterData) : "";
  const message = buildStatusMessage(newStatus, providerName, serviceName, bookingNumber, scheduleLabel);

  const userSnap = await admin
    .firestore()
    .collection("users")
    .doc(afterData.userId)
    .get();
  const token = userSnap.get("fcmToken");

  await Promise.allSettled([
    sendToToken(token, title, message, afterData.userId, {
      bookingId,
      role: "user",
      type: "booking_status",
      status: newStatus,
    }),
    saveNotificationRecord({
      userId: afterData.userId,
      role: "user",
      title,
      message,
      source: "booking_status",
      bookingId,
      status: newStatus,
    }),
  ]);

  await after.ref.set(
    {
      lastNotifiedStatus: newStatus,
      lastNotifiedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
});

exports.onChatMessageCreated = onDocumentWritten(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const after = event.data?.after;
    const before = event.data?.before;
    if (!after || !after.exists || (before && before.exists)) {
      return;
    }
    const message = after.data() || {};
    const conversationId = event.params?.conversationId || "";
    const senderId = message.senderId || "";
    const text = message.text || "";
    const type = message.type || "text";
    const preview = type === "image" ? "Photo" : text || "New message";

    const convoSnap = await admin
      .firestore()
      .collection("conversations")
      .doc(conversationId)
      .get();
    if (!convoSnap.exists) {
      return;
    }
    const convo = convoSnap.data() || {};
    const userId = convo.userId || "";
    const providerId = convo.providerId || "";
    const providerName = convo.providerName || "Service Provider";
    const userName = convo.userName || "Customer";

    let targetUserId = "";
    let role = "";
    let title = "New message";
    if (senderId === userId) {
      targetUserId = providerId;
      role = "provider";
      title = `New message from ${userName}`;
    } else {
      targetUserId = userId;
      role = "user";
      title = `New message from ${providerName}`;
    }
    if (!targetUserId) {
      return;
    }
    const targetSnap = await admin
      .firestore()
      .collection(role === "provider" ? "providers" : "users")
      .doc(targetUserId)
      .get();
    const token = targetSnap.get("fcmToken");

    await Promise.allSettled([
      sendToToken(token, title, preview, targetUserId, {
        conversationId,
        role,
        type: "message",
        chatTitle: title,
        avatarUri: "",
      }),
      saveNotificationRecord({
        userId: targetUserId,
        role,
        title,
        message: preview,
        source: "chat_message",
        conversationId,
      }),
    ]);
  }
);
