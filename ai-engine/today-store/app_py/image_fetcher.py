from __future__ import annotations

from pathlib import Path
from urllib.parse import urlparse
from urllib.request import Request, urlopen

from .config import MIME_BY_EXT, REQUEST_TIMEOUT_SECONDS


def fetch_image_bytes(url: str) -> tuple[bytes, str]:
    # 기본 전략: signed URL 그대로 GET
    request = Request(url, method="GET")
    with urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
        content = response.read()
        content_type = response.headers.get("Content-Type", "").split(";")[0].strip()

    mime_type = content_type or guess_mime_from_url(url)
    if not mime_type:
        mime_type = "application/octet-stream"
    return content, mime_type


def guess_mime_from_url(url: str) -> str:
    parsed = urlparse(url)
    suffix = Path(parsed.path).suffix.lower()
    return MIME_BY_EXT.get(suffix, "")

