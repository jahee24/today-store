# [2026-05-13] Spring Boot Cold Start로 인한 초기 API 401

### 증상

앱 실행 직후 `/api/v1/users/me` → 401 응답.
설정 화면에서 사용자 정보 로딩 실패.

### 원인

Cloud Run의 **Cold Start** (서버 재시작 후 첫 요청 처리까지 약 12~13초 소요).
서버가 완전히 초기화되기 전에 앱이 API 요청을 보내면 401 반환.
이후 Refresh Token으로 자동 갱신되어 정상화됨.

### 확인 방법

Cloud Run 로그에서 Spring Boot 시작 시간 확인:
```
Started TodayStoreApplication in 12.712 seconds
```

### 대응

Cold Start는 서버 인프라 문제로 프론트엔드에서 직접 해결 불가.
백엔드 팀에 Cloud Run **최소 인스턴스 1 설정** 요청 또는 **Warm-up 요청** 구현 제안.

---
> **참고**: 프론트엔드에서는 401 발생 시 무조건 에러 처리를 하기보다, 서버가 기동 중일 가능성을 염두에 두고 재시도 로직을 보완하는 것도 방법임.
