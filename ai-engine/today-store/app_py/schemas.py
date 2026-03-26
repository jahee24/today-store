from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Literal
from uuid import uuid4

from .config import MAX_IMAGES

PreferredStyle = Literal["plain", "clean", "friendly", "meme"]
SnsType = Literal["instagram", "naver", "karrot"]


@dataclass(slots=True)
class GenerateRequest:
    store_name: str
    business_type: str
    address: str
    lat: float
    lng: float
    preferred_style: PreferredStyle
    sns: SnsType
    additional_note: str
    image_urls: list[str]

    @staticmethod
    def from_payload(payload: dict[str, Any]) -> "GenerateRequest":
        image_urls = _extract_image_urls(payload)
        if not image_urls:
            raise ValueError("이미지 URL이 필요합니다. imageUrls 또는 images[].url 또는 imageUrl을 전달해 주세요.")
        if len(image_urls) > MAX_IMAGES:
            raise ValueError(f"이미지는 최대 {MAX_IMAGES}개까지 허용됩니다.")

        store_name = str(payload.get("storeName", "")).strip()
        business_type = str(payload.get("businessType", "")).strip()
        address = str(payload.get("address", "")).strip()
        preferred_style = str(payload.get("preferredStyle", "")).strip()
        sns = str(payload.get("sns", "")).strip()

        if not store_name:
            raise ValueError("storeName은 필수입니다.")
        if not business_type:
            raise ValueError("businessType은 필수입니다.")
        if not address:
            raise ValueError("address는 필수입니다.")
        if preferred_style not in {"plain", "clean", "friendly", "meme"}:
            raise ValueError("preferredStyle은 plain/clean/friendly/meme 중 하나여야 합니다.")
        if sns not in {"instagram", "naver", "karrot"}:
            raise ValueError("sns는 instagram/naver/karrot 중 하나여야 합니다.")

        try:
            lat = float(payload["lat"])
            lng = float(payload["lng"])
        except Exception as exc:
            raise ValueError("lat/lng는 숫자여야 합니다.") from exc

        return GenerateRequest(
            store_name=store_name,
            business_type=business_type,
            address=address,
            lat=lat,
            lng=lng,
            preferred_style=preferred_style,  # type: ignore[arg-type]
            sns=sns,  # type: ignore[arg-type]
            additional_note=str(payload.get("additionalNote", "")).strip(),
            image_urls=image_urls,
        )


@dataclass(slots=True)
class GeneratedDraft:
    text: str
    hashtags: list[str]
    photo_info: str


def build_response(
    request: GenerateRequest,
    platform_payload: dict[str, Any],
    uploaded_image_urls: list[str],
) -> dict[str, Any]:
    return {
        "contentId": str(uuid4()),
        "platforms": {
            request.sns: platform_payload,
        },
        "images": uploaded_image_urls,
        "createdAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    }


def _extract_image_urls(payload: dict[str, Any]) -> list[str]:
    urls: list[str] = []

    image_urls = payload.get("imageUrls")
    if isinstance(image_urls, list):
        urls.extend(str(x).strip() for x in image_urls if str(x).strip())

    images = payload.get("images")
    if isinstance(images, list):
        for item in images:
            if isinstance(item, dict):
                raw = str(item.get("url", "")).strip()
                if raw:
                    urls.append(raw)
            elif isinstance(item, str) and item.strip():
                urls.append(item.strip())

    single = str(payload.get("imageUrl", "")).strip()
    if single:
        urls.append(single)

    # 순서 유지 중복 제거
    deduped: list[str] = []
    seen = set()
    for url in urls:
        if url not in seen:
            seen.add(url)
            deduped.append(url)
    return deduped

