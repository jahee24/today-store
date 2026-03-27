from __future__ import annotations

from .schemas import GeneratedDraft, GenerateRequest


def build_platform_payload(req: GenerateRequest, draft: GeneratedDraft) -> dict:
    if req.sns == "instagram":
        return {
            "text": draft.text,
            "hashtags": draft.hashtags,
            "style": req.preferred_style,
        }

    if req.sns == "karrot":
        tags = [h.lstrip("#") for h in draft.hashtags]
        prefix = f"[{req.address} {req.store_name}] "
        return {
            "text": f"{prefix}{draft.text}".strip(),
            "tags": tags,
            "style": "지역 정보성",
        }

    # naver
    keywords = _keywords_from_hashtags(draft.hashtags, req)
    return {
        "text": f"{req.store_name} {draft.text}".strip(),
        "keywords": keywords,
        "style": "정보 중심",
    }


def _keywords_from_hashtags(hashtags: list[str], req: GenerateRequest) -> list[str]:
    out = []
    seen = set()
    for token in [req.business_type, req.store_name] + [h.lstrip("#") for h in hashtags]:
        normalized = token.strip()
        if normalized and normalized not in seen:
            seen.add(normalized)
            out.append(normalized)
    return out[:8]

