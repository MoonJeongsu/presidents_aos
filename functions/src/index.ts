import * as crypto from "crypto";
import {
  DocumentReference,
  FieldValue,
  getFirestore,
} from "firebase-admin/firestore";
import { initializeApp } from "firebase-admin/app";
import { getStorage } from "firebase-admin/storage";
import { onRequest } from "firebase-functions/v2/https";
import { v2 as TranslateV2 } from "@google-cloud/translate";
import textToSpeech from "@google-cloud/text-to-speech";

const STORAGE_BUCKET = "presidential-speeches-a9f00.firebasestorage.app";
const TTS_VOICE = "en-US-Neural2-J";

initializeApp({
  storageBucket: STORAGE_BUCKET,
});

const db = getFirestore();
const translateClient = new TranslateV2.Translate();
const ttsClient = new textToSpeech.TextToSpeechClient();

const DAILY_QUOTA = 40;
const REWARD_BONUS = 20;
const MAX_DAILY_REWARDS = 3;
const MAX_TEXT_LENGTH = 5000;

type QuotaState = {
  ref: DocumentReference;
  used: number;
  bonus: number;
  rewardsGranted: number;
  date: string;
};

function hashText(text: string): string {
  return crypto.createHash("sha256").update(text.trim()).digest("hex");
}

function todayKeyUtc(): string {
  return new Date().toISOString().slice(0, 10);
}

function buildPublicStorageUrl(bucketName: string, objectPath: string): string {
  const encodedPath = encodeURIComponent(objectPath).replace(/%2F/g, "%2F");
  return `https://firebasestorage.googleapis.com/v0/b/${bucketName}/o/${encodedPath}?alt=media`;
}

async function getQuotaState(collection: string, clientId: string): Promise<QuotaState> {
  const ref = db.collection(collection).doc(clientId);
  const snap = await ref.get();
  const today = todayKeyUtc();

  if (!snap.exists || snap.data()?.date !== today) {
    return {
      ref,
      used: 0,
      bonus: 0,
      rewardsGranted: 0,
      date: today,
    };
  }

  const data = snap.data() ?? {};
  return {
    ref,
    used: Number(data.used ?? 0),
    bonus: Number(data.bonus ?? 0),
    rewardsGranted: Number(data.rewardsGranted ?? 0),
    date: today,
  };
}

async function persistQuota(state: QuotaState): Promise<void> {
  await state.ref.set(
    {
      date: state.date,
      used: state.used,
      bonus: state.bonus,
      rewardsGranted: state.rewardsGranted,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
}

function quotaLimitOf(quota: QuotaState): number {
  return DAILY_QUOTA + quota.bonus;
}

async function consumeQuota(quota: QuotaState): Promise<
  { ok: true; quotaLimit: number } | { ok: false; quotaLimit: number }
> {
  const quotaLimit = quotaLimitOf(quota);
  if (quota.used >= quotaLimit) {
    return { ok: false, quotaLimit };
  }
  quota.used += 1;
  await persistQuota(quota);
  return { ok: true, quotaLimit };
}

function readJsonBody(req: { body?: unknown }): Record<string, unknown> | null {
  if (req.body == null) {
    return null;
  }
  if (typeof req.body === "object") {
    return req.body as Record<string, unknown>;
  }
  if (typeof req.body === "string") {
    try {
      return JSON.parse(req.body) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
  return null;
}

function readStringField(body: Record<string, unknown>, key: string): string | null {
  const value = body[key];
  if (typeof value !== "string") {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

async function getAudioUrlIfCached(sourceHash: string): Promise<string | null> {
  const bucket = getStorage().bucket(STORAGE_BUCKET);
  const objectPath = `audio/${sourceHash}.mp3`;
  const file = bucket.file(objectPath);
  const [exists] = await file.exists();
  if (!exists) {
    return null;
  }
  return buildPublicStorageUrl(STORAGE_BUCKET, objectPath);
}

export const translateSentence = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "method_not_allowed" });
    return;
  }

  const body = readJsonBody(req);
  if (body == null) {
    res.status(400).json({ error: "invalid_json" });
    return;
  }

  const text = readStringField(body, "text");
  const clientId = readStringField(body, "clientId");
  if (text == null || clientId == null) {
    res.status(400).json({ error: "invalid_request" });
    return;
  }

  if (text.length > MAX_TEXT_LENGTH) {
    res.status(400).json({ error: "text_too_long" });
    return;
  }

  try {
    const sourceHash = hashText(text);
    const cacheRef = db.collection("translations").doc(sourceHash);
    const cacheSnap = await cacheRef.get();
    const quota = await getQuotaState("quotas", clientId);

    if (cacheSnap.exists) {
      const consumed = await consumeQuota(quota);
      if (!consumed.ok) {
        res.status(429).json({
          error: "quota_exceeded",
          quotaUsed: quota.used,
          quotaLimit: consumed.quotaLimit,
        });
        return;
      }
      const cached = cacheSnap.data() ?? {};
      res.status(200).json({
        translatedText: String(cached.translatedText ?? ""),
        fromCache: true,
        quotaUsed: quota.used,
        quotaLimit: consumed.quotaLimit,
      });
      return;
    }

    const quotaLimit = quotaLimitOf(quota);
    if (quota.used >= quotaLimit) {
      res.status(429).json({
        error: "quota_exceeded",
        quotaUsed: quota.used,
        quotaLimit,
      });
      return;
    }

    const [translatedText] = await translateClient.translate(text, "ko");
    await cacheRef.set({
      sourceHash,
      sourceText: text,
      translatedText,
      charCount: text.length,
      createdAt: FieldValue.serverTimestamp(),
    });

    quota.used += 1;
    await persistQuota(quota);

    res.status(200).json({
      translatedText,
      fromCache: false,
      quotaUsed: quota.used,
      quotaLimit,
    });
  } catch (error) {
    console.error("translateSentence failed", error);
    res.status(500).json({ error: "internal_error" });
  }
});

export const grantTranslationBonus = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "method_not_allowed" });
    return;
  }

  const body = readJsonBody(req);
  if (body == null) {
    res.status(400).json({ error: "invalid_json" });
    return;
  }

  const clientId = readStringField(body, "clientId");
  if (clientId == null) {
    res.status(400).json({ error: "invalid_request" });
    return;
  }

  try {
    const quota = await getQuotaState("quotas", clientId);

    if (quota.rewardsGranted >= MAX_DAILY_REWARDS) {
      res.status(429).json({
        error: "reward_limit_reached",
        quotaUsed: quota.used,
        quotaLimit: DAILY_QUOTA + quota.bonus,
        rewardsGranted: quota.rewardsGranted,
        maxRewards: MAX_DAILY_REWARDS,
      });
      return;
    }

    quota.bonus += REWARD_BONUS;
    quota.rewardsGranted += 1;
    await persistQuota(quota);

    res.status(200).json({
      bonusGranted: REWARD_BONUS,
      quotaUsed: quota.used,
      quotaLimit: DAILY_QUOTA + quota.bonus,
      rewardsGranted: quota.rewardsGranted,
      maxRewards: MAX_DAILY_REWARDS,
    });
  } catch (error) {
    console.error("grantTranslationBonus failed", error);
    res.status(500).json({ error: "internal_error" });
  }
});

export const synthesizeSentence = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "method_not_allowed" });
    return;
  }

  const body = readJsonBody(req);
  if (body == null) {
    res.status(400).json({ error: "invalid_json" });
    return;
  }

  const text = readStringField(body, "text");
  const clientId = readStringField(body, "clientId");
  if (text == null || clientId == null) {
    res.status(400).json({ error: "invalid_request" });
    return;
  }

  if (text.length > MAX_TEXT_LENGTH) {
    res.status(400).json({ error: "text_too_long" });
    return;
  }

  try {
    const sourceHash = hashText(text);
    const cachedAudioUrl = await getAudioUrlIfCached(sourceHash);
    const quota = await getQuotaState("tts_quotas", clientId);

    if (cachedAudioUrl != null) {
      const consumed = await consumeQuota(quota);
      if (!consumed.ok) {
        res.status(429).json({
          error: "quota_exceeded",
          quotaUsed: quota.used,
          quotaLimit: consumed.quotaLimit,
        });
        return;
      }
      res.status(200).json({
        audioUrl: cachedAudioUrl,
        fromCache: true,
        quotaUsed: quota.used,
        quotaLimit: consumed.quotaLimit,
      });
      return;
    }

    const quotaLimit = quotaLimitOf(quota);
    if (quota.used >= quotaLimit) {
      res.status(429).json({
        error: "quota_exceeded",
        quotaUsed: quota.used,
        quotaLimit,
      });
      return;
    }

    const [response] = await ttsClient.synthesizeSpeech({
      input: { text },
      voice: {
        languageCode: "en-US",
        name: TTS_VOICE,
      },
      audioConfig: {
        audioEncoding: "MP3",
        speakingRate: 0.95,
        pitch: 0,
      },
    });

    const audioContent = response.audioContent;
    if (audioContent == null || !(audioContent instanceof Uint8Array)) {
      res.status(500).json({ error: "synthesis_failed" });
      return;
    }

    const bucket = getStorage().bucket(STORAGE_BUCKET);
    const objectPath = `audio/${sourceHash}.mp3`;
    const file = bucket.file(objectPath);
    await file.save(Buffer.from(audioContent), {
      contentType: "audio/mpeg",
      metadata: {
        cacheControl: "public,max-age=31536000",
      },
    });

    quota.used += 1;
    await persistQuota(quota);

    res.status(200).json({
      audioUrl: buildPublicStorageUrl(STORAGE_BUCKET, objectPath),
      fromCache: false,
      quotaUsed: quota.used,
      quotaLimit,
    });
  } catch (error) {
    console.error("synthesizeSentence failed", error);
    res.status(500).json({ error: "internal_error" });
  }
});

export const grantTtsBonus = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "method_not_allowed" });
    return;
  }

  const body = readJsonBody(req);
  if (body == null) {
    res.status(400).json({ error: "invalid_json" });
    return;
  }

  const clientId = readStringField(body, "clientId");
  if (clientId == null) {
    res.status(400).json({ error: "invalid_request" });
    return;
  }

  try {
    const quota = await getQuotaState("tts_quotas", clientId);

    if (quota.rewardsGranted >= MAX_DAILY_REWARDS) {
      res.status(429).json({
        error: "reward_limit_reached",
        quotaUsed: quota.used,
        quotaLimit: DAILY_QUOTA + quota.bonus,
        rewardsGranted: quota.rewardsGranted,
        maxRewards: MAX_DAILY_REWARDS,
      });
      return;
    }

    quota.bonus += REWARD_BONUS;
    quota.rewardsGranted += 1;
    await persistQuota(quota);

    res.status(200).json({
      bonusGranted: REWARD_BONUS,
      quotaUsed: quota.used,
      quotaLimit: DAILY_QUOTA + quota.bonus,
      rewardsGranted: quota.rewardsGranted,
      maxRewards: MAX_DAILY_REWARDS,
    });
  } catch (error) {
    console.error("grantTtsBonus failed", error);
    res.status(500).json({ error: "internal_error" });
  }
});
