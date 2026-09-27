// Token protection patterns matching Android TokenProtector specifications
const URL_PATTERN = /https?:\/\/[^\s]+/gi;
const EMAIL_PATTERN = /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g;
const CURRENCY_PATTERN = /(?:[₹$€£¥]\s*\d+(?:[.,]\d+)?)|(?:\b(?:Rs\.?|INR|USD|EUR|GBP)\s*\d+(?:[.,]\d+)?)|(?:\d+(?:[.,]\d+)?\s*(?:USD|EUR|INR|Rs))/gi;
const OTP_PATTERN = /(\b(?:otp|code|pin|password|verification|auth|secret)\b[\s:=#-]{0,8})(\d{4,8})\b/gi;
const DATE_TIME_PATTERN = /\b(?:\d{1,2}:\d{2}(?::\d{2})?\s*(?:[AaPp][Mm])?)|(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b/g;
const PHONE_PATTERN = /\+?\d{1,3}[-.\s]?(?:\(\d{1,4}\)|\d{1,4})[-.\s]?\d{2,4}[-.\s]?\d{3,4}/g;

export function protectTokens(text) {
  if (!text) return { maskedText: '', tokenMap: {} };

  const tokenMap = {};
  let counter = 0;
  let currentText = text;

  // 1. Protect OTPs preserving prefix keyword
  currentText = currentText.replace(OTP_PATTERN, (match, prefix, otpVal) => {
    const token = `TKX${counter}Z`;
    tokenMap[token] = otpVal;
    counter++;
    return `${prefix}${token}`;
  });

  // 2. Protect remaining patterns
  const otherPatterns = [
    URL_PATTERN,
    EMAIL_PATTERN,
    CURRENCY_PATTERN,
    DATE_TIME_PATTERN,
    PHONE_PATTERN
  ];

  for (const pattern of otherPatterns) {
    currentText = currentText.replace(pattern, (match) => {
      if (match.startsWith('TKX') && match.endsWith('Z')) return match;
      const token = `TKX${counter}Z`;
      tokenMap[token] = match;
      counter++;
      return token;
    });
  }

  return { maskedText: currentText, tokenMap };
}

export function restoreTokens(translatedText, tokenMap) {
  let restored = translatedText;
  for (const [token, originalVal] of Object.entries(tokenMap)) {
    const regex = new RegExp(`\\b${token}\\b|${token}`, 'gi');
    restored = restored.replace(regex, originalVal);
  }
  return restored;
}

const BACKEND_API_URL = 'https://backend-phi-lemon-83.vercel.app/api/translate';

export async function translateNotificationText(text, targetLang = 'ta') {
  if (!text.trim()) return '';

  // Attempt 1: Call Vercel Python Backend
  try {
    const res = await fetch(BACKEND_API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text,
        source_lang: 'en',
        target_lang: targetLang,
        preserve_tokens: true
      })
    });

    if (res.ok) {
      const data = await res.json();
      if (data.translated_text) {
        return data.translated_text;
      }
    }
  } catch (err) {
    console.warn('Backend unavailable, falling back to direct translator:', err);
  }

  // Attempt 2: Resilient Client-side Token-Protected Translation Fallback
  try {
    const { maskedText, tokenMap } = protectTokens(text);
    const gtxUrl = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=${targetLang}&dt=t&q=${encodeURIComponent(maskedText)}`;
    const gtxRes = await fetch(gtxUrl);
    if (gtxRes.ok) {
      const data = await gtxRes.json();
      if (data && data[0]) {
        const rawTranslated = data[0].map(part => part[0]).join('');
        return restoreTokens(rawTranslated, tokenMap);
      }
    }
  } catch (gtxErr) {
    console.error('Direct translation error:', gtxErr);
  }

  return text; // Safe fallback
}
