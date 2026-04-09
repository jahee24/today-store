from __future__ import annotations

import os

MODEL_NAME = os.getenv("MODEL_NAME", "gemini-3.1-flash-lite-preview")
MAX_IMAGES = 5
REQUEST_TIMEOUT_SECONDS = 15

MIME_BY_EXT = {
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".webp": "image/webp",
    ".gif": "image/gif",
}

STYLE_MAP = {
    "plain": "정보제공",
    "clean": "전문성",
    "friendly": "친근",
    "meme": "감성적",
}

