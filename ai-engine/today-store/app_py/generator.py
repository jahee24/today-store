from __future__ import annotations

import json
import os
import re

from google import genai
from google.genai import types

from .config import MODEL_NAME
from .guardrails import normalize_hashtags, remove_hashtag_tail_from_body, soft_wrap_long_text
from .image_fetcher import fetch_image_bytes
from .prompt_builder import build_prompt
from .schemas import GenerateRequest, GeneratedDraft


def _get_api_key() -> str:
    key = os.getenv("GEMINI_API_KEY", "").strip()
    if not key:
        raise ValueError("GEMINI_API_KEY가 설정되어 있지 않습니다.")
    return key


def _extract_json_object(text: str) -> dict:
    cleaned = (text or "").strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"\s*```$", "", cleaned)

    try:
        parsed = json.loads(cleaned)
        if isinstance(parsed, dict):
            return parsed
    except json.JSONDecodeError:
        pass

    match = re.search(r"\{[\s\S]*\}", cleaned)
    if not match:
        raise ValueError("응답에서 JSON 객체를 찾지 못했습니다.")
    parsed = json.loads(match.group(0))
    if not isinstance(parsed, dict):
        raise ValueError("응답 JSON이 객체 형태가 아닙니다.")
    return parsed


def generate_draft(req: GenerateRequest) -> GeneratedDraft:
    client = genai.Client(api_key=_get_api_key())
    prompt = build_prompt(req)

    parts = []
    for image_url in req.image_urls:
        data, mime_type = fetch_image_bytes(image_url)
        parts.append(types.Part.from_bytes(data=data, mime_type=mime_type))
    parts.append(prompt)

    response = client.models.generate_content(
        model=MODEL_NAME,
        contents=parts,
    )
    raw_output = (response.text or "").strip()
    parsed = _extract_json_object(raw_output)

    photo_info = str(parsed.get("photo_info", "")).strip()
    text = str(parsed.get("본문", "")).strip()
    hashtags = normalize_hashtags(parsed.get("해시태그", []))
    text = remove_hashtag_tail_from_body(text, hashtags)
    text = soft_wrap_long_text(text)

    return GeneratedDraft(
        text=text,
        hashtags=hashtags,
        photo_info=photo_info,
    )

