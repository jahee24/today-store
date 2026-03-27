# app_py

서버 배포용 코드 모듈입니다.

## 엔트리포인트
- `main.py`의 `handle_generate_request(payload)`
- FastAPI 서버: `api.py`의 `app`

## 입력 예시
```python
payload = {
    "storeName": "홍길동 카페",
    "businessType": "카페",
    "address": "강남구 테헤란로 1",
    "lat": 37.1234,
    "lng": 127.1234,
    "preferredStyle": "friendly",
    "sns": "instagram",
    "additionalNote": "오늘 특별 메뉴 홍보",
    "imageUrls": [
        "https://signed-url-1",
        "https://signed-url-2"
    ],
}
```

## 응답 형태
- 요청된 `sns`만 `platforms`에 포함됩니다.
- `images`는 입력 URL을 그대로 반영합니다.

## 서버 실행
```bash
pip install -r requirements.txt
python -m app_py.run_server
```

## API
- `GET /health`
- `POST /generate`

