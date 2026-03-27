from __future__ import annotations

from typing import Any

from .generator import generate_draft
from .platform_formatter import build_platform_payload
from .schemas import GenerateRequest, build_response


def run_generation(payload: dict[str, Any]) -> dict[str, Any]:
    req = GenerateRequest.from_payload(payload)
    draft = generate_draft(req)
    platform_payload = build_platform_payload(req, draft)
    return build_response(
        request=req,
        platform_payload=platform_payload,
        uploaded_image_urls=req.image_urls,
    )

