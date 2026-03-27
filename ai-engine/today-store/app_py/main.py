from __future__ import annotations

from typing import Any

from .pipeline import run_generation


def handle_generate_request(payload: dict[str, Any]) -> dict[str, Any]:
    """
    API 핸들러에서 바로 호출 가능한 진입 함수.
    framework(FastAPI/Flask) 의존성 없이 순수 함수 형태로 유지한다.
    """
    return run_generation(payload)

