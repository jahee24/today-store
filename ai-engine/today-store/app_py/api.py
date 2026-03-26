from __future__ import annotations

from fastapi import FastAPI, HTTPException

from .main import handle_generate_request


app = FastAPI(title="Today Store API", version="1.0.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/generate")
def generate(payload: dict) -> dict:
    try:
        return handle_generate_request(payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"internal_error: {exc}") from exc

