import re
import urllib.parse
from typing import Dict, Optional, List
import requests
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from deep_translator import GoogleTranslator, MyMemoryTranslator

app = FastAPI(
    title="Tamil Notify API",
    description="Backend API for Tamil Notification Translator and Sync Engine",
    version="1.0.0"
)

# Enable CORS for all origins (Netlify frontend, localhost, mobile)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Regex patterns for sensitive and structured token protection
URL_PATTERN = re.compile(r'https?://[^\s]+', re.IGNORECASE)
EMAIL_PATTERN = re.compile(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b')
CURRENCY_PATTERN = re.compile(r'(?:[₹$€£¥]\s*\d+(?:[.,]\d+)?)|(?:\b(?:Rs\.?|INR|USD|EUR|GBP)\s*\d+(?:[.,]\d+)?)|(?:\d+(?:[.,]\d+)?\s*(?:USD|EUR|INR|Rs))', re.IGNORECASE)
OTP_PATTERN = re.compile(r'(\b(?:otp|code|pin|password|verification|auth|secret)\b[\s:=#-]{0,8})(\d{4,8})\b', re.IGNORECASE)
DATE_TIME_PATTERN = re.compile(r'\b(?:\d{1,2}:\d{2}(?::\d{2})?\s*(?:[AaPp][Mm])?)|(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b')
PHONE_PATTERN = re.compile(r'\+?\d{1,3}[-.\s]?(?:\(\d{1,4}\)|\d{1,4})[-.\s]?\d{2,4}[-.\s]?\d{3,4}')

def protect_tokens(text: str):
    if not text:
        return text, {}

    token_map = {}
    counter = 0
    current_text = text

    # First protect OTPs with prefix
    def replace_otp(m):
        nonlocal counter
        prefix = m.group(1)
        otp_val = m.group(2)
        token = f"TKX{counter}Z"
        token_map[token] = otp_val
        counter += 1
        return f"{prefix}{token}"

    current_text = OTP_PATTERN.sub(replace_otp, current_text)

    patterns = [
        URL_PATTERN,
        EMAIL_PATTERN,
        CURRENCY_PATTERN,
        DATE_TIME_PATTERN,
        PHONE_PATTERN
    ]

    for pat in patterns:
        def replace_match(m):
            nonlocal counter
            val = m.group(0)
            if val.startswith("TKX") and val.endswith("Z"):
                return val
            token = f"TKX{counter}Z"
            token_map[token] = val
            counter += 1
            return token

        current_text = pat.sub(replace_match, current_text)

    return current_text, token_map

def restore_tokens(translated: str, token_map: Dict[str, str]) -> str:
    restored = translated
    for token, val in token_map.items():
        pattern = re.compile(re.escape(token), re.IGNORECASE)
        restored = pattern.sub(val, restored)
    return restored

def execute_translation(text: str, source_lang: str, target_lang: str) -> str:
    """Translates text with reliable multi-engine fallbacks."""
    # Engine 1: Direct Google API
    try:
        url = f"https://translate.googleapis.com/translate_a/single?client=gtx&sl={source_lang}&tl={target_lang}&dt=t&q={urllib.parse.quote(text)}"
        resp = requests.get(url, timeout=5, headers={"User-Agent": "Mozilla/5.0"})
        if resp.status_code == 200:
            data = resp.json()
            if data and len(data) > 0 and data[0]:
                translated_parts = [segment[0] for segment in data[0] if segment and segment[0]]
                if translated_parts:
                    return "".join(translated_parts)
    except Exception:
        pass

    # Engine 2: deep-translator GoogleTranslator
    try:
        translator = GoogleTranslator(source=source_lang, target=target_lang)
        return translator.translate(text)
    except Exception:
        pass

    # Engine 3: MyMemoryTranslator
    try:
        mm = MyMemoryTranslator(source=source_lang, target=target_lang)
        return mm.translate(text)
    except Exception:
        pass

    return text

class TranslateRequest(BaseModel):
    text: str
    source_lang: str = "en"
    target_lang: str = "ta"
    preserve_tokens: bool = True

class TranslateResponse(BaseModel):
    original_text: str
    translated_text: str
    target_lang: str
    tokens_protected: int
    success: bool

@app.get("/api/health")
def health_check():
    return {
        "status": "ok",
        "service": "Tamil Notify API",
        "version": "1.0.0",
        "endpoints": ["/api/health", "/api/translate"]
    }

@app.post("/api/translate", response_model=TranslateResponse)
def translate_text(req: TranslateRequest):
    if not req.text.strip():
        return TranslateResponse(
            original_text="",
            translated_text="",
            target_lang=req.target_lang,
            tokens_protected=0,
            success=True
        )

    try:
        if req.preserve_tokens:
            masked_text, token_map = protect_tokens(req.text)
            raw_translation = execute_translation(masked_text, req.source_lang, req.target_lang)
            final_translation = restore_tokens(raw_translation, token_map)
            tokens_count = len(token_map)
        else:
            final_translation = execute_translation(req.text, req.source_lang, req.target_lang)
            tokens_count = 0

        return TranslateResponse(
            original_text=req.text,
            translated_text=final_translation,
            target_lang=req.target_lang,
            tokens_protected=tokens_count,
            success=True
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
