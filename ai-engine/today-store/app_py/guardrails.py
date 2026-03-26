from __future__ import annotations

import re


HASHTAG_TOKEN_PATTERN = re.compile(r"#\S+")


def normalize_hashtags(value: object) -> list[str]:
    if isinstance(value, list):
        tokens = [str(x).strip() for x in value if str(x).strip()]
    elif isinstance(value, str):
        tokens = [x.strip() for x in value.split() if x.strip()]
    else:
        tokens = []

    normalized: list[str] = []
    seen = set()
    for token in tokens:
        if not token.startswith("#"):
            token = f"#{token.lstrip('#')}"
        if token not in seen:
            seen.add(token)
            normalized.append(token)
    return normalized


def remove_hashtag_tail_from_body(body: str, hashtags: list[str]) -> str:
    body_text = (body or "").strip()
    if not body_text:
        return ""
    if not hashtags:
        return body_text

    lines = body_text.splitlines()
    if not lines:
        return body_text

    last_line = lines[-1].strip()
    last_line_tokens = HASHTAG_TOKEN_PATTERN.findall(last_line)
    if last_line_tokens and all(tok in hashtags for tok in last_line_tokens):
        non_hashtag = HASHTAG_TOKEN_PATTERN.sub("", last_line).strip()
        if not non_hashtag:
            lines.pop()
    return "\n".join(lines).rstrip()


def soft_wrap_long_text(text: str) -> str:
    cleaned = " ".join((text or "").split())
    if not cleaned:
        return ""
    if "\n" in text:
        return text.strip()

    # 매우 긴 단일 라인을 문장 단위로 가볍게 분리
    chunks = re.split(r"(?<=[.!?])\s+", cleaned)
    chunks = [c.strip() for c in chunks if c.strip()]
    if len(chunks) >= 2:
        return "\n".join(chunks[:4]).strip()
    return cleaned

