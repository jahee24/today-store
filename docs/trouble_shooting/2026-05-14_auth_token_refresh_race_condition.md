# [2026-05-14] 인증 토큰 갱신 레이스 컨디션 (Mismatched refresh token)

### 증상

앱 사용 중 간헐적으로 로그아웃되거나, 대시보드 진입 시 데이터 로딩에 실패하며 로그인 화면으로 튕김.
백엔드 로그에서 `Mismatched refresh token` 에러가 다수 발견됨.

### 원인 분석

1.  **동시성 문제 (Race Condition)**:
    - 대시보드 진입 시 여러 API(가게 정보, 통계, 최근 요청 등)가 동시에 호출됨.
    - 토큰이 만료된 시점에 이 요청들이 동시에 401 Unauthorized 응답을 받음.
    - 각 요청이 개별적으로 `ApiClient`의 Interceptor를 통해 `/api/v1/auth/refresh`를 호출함.
2.  **Refresh Token Rotation 정책**:
    - 백엔드는 보안을 위해 Refresh Token을 한 번 사용하면 폐기하고 새 토큰을 발급함(Rotation).
    - 첫 번째 요청이 토큰을 성공적으로 갱신한 직후, 두 번째 요청이 이미 폐기된 구버전 Refresh Token으로 갱신을 시도하면서 `Mismatched refresh token` 에러가 발생.
    - 결과적으로 모든 요청이 실패하고 사용자는 강제 로그아웃됨.

### 해결 방법

#### 1. 중앙 집중형 네트워크 Provider 구축
파편화되어 있던 `apiClientProvider`와 `tokenServiceProvider`를 `network_provider.dart`로 통합하여 앱 전체에서 단일 인스턴스를 공유하도록 함.

#### 2. `_refreshFuture`를 이용한 갱신 요청 단일화
`ApiClient` 내부에 `Future<void>? _refreshFuture` 변수를 도입하여 동시 요청을 제어함.

```dart
// api_client.dart 내 Interceptor 로직
onError: (error, handler) async {
  if (statusCode == 401) {
    try {
      // 이미 갱신 중이라면 그 Future를 기다리고, 아니면 새로 시작
      _refreshFuture ??= _performTokenRefresh();
      await _refreshFuture;

      // 갱신 성공 후 새 토큰으로 재시도
      return handler.resolve(await _retry(error.requestOptions));
    } catch (e) {
      // 갱신 실패 시 로그아웃 처리
      onAuthFailure?.call();
      return handler.reject(error);
    }
  }
}
```

#### 3. 반응형 라우팅 연동
`GoRouter`를 `routerProvider`로 전환하고 `AuthStatus`를 감시하게 하여, 세션 만료 시 즉시 로그인 화면으로 자동 리다이렉트되도록 구현.

### 결과 및 교훈

- **결과**: 여러 API가 동시에 호출되어도 토큰 갱신은 단 한 번만 발생하며, 나머지 요청들은 갱신 완료를 기다렸다가 안전하게 재시도됨.
- **교훈**: OAuth2 기반의 인증 시스템에서 Refresh Token Rotation을 사용하는 경우, 프론트엔드 네트워크 계층에서의 동시성 제어(Concurrency Control)는 필수적임.

---
> **관련 파일**: 
> - `lib/data/providers/network_provider.dart`
> - `lib/data/datasources/remote/api_client.dart`
> - `lib/config/routes.dart`
