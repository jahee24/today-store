# [2026-05-15] 이메일 변경 후 JWT 무효화로 인한 세션 붕괴

## 발생 현황
프로필 수정 화면에서 이메일을 변경하면, "저장하기" 버튼은 정상 작동하지만 이후 설정 화면에서 **"사용자 정보 불러오기 실패"** 메시지가 표시되며 로그아웃도 불가능해지는 현상 발생.

## 증상
1. 이메일 수정 → PATCH 요청 성공 (200)
2. 설정 화면 복귀 → `GET /api/v1/users/me` 실패 (404)
3. 로그아웃 시도 → 인증 실패로 로그아웃 불가
4. 앱 재시작 + 재로그인 시에는 정상 작동

## 원인 분석

### 1단계: JWT `sub` 클레임이 이메일 기반
백엔드 JWT 토큰을 디코딩하면 `sub` 필드가 사용자 UUID가 아닌 **이메일**로 설정되어 있음:

```json
{
  "sub": "jahee0128@naver.com",
  "auth": "",
  "jti": "11789363-af55-45d2-9fe0-4d470012e00f",
  "iat": 1778810707,
  "exp": 1778812507
}
```

### 2단계: 이메일 변경 시 토큰 불일치 발생
```
[요청] PATCH /api/v1/users/me  { email: "new@email.com" }
[응답] 200 OK → DB에서 이메일 변경 완료
       └→ 응답에 새 accessToken + refreshToken 포함! (sub: "new@email.com")
[문제] 프론트엔드가 응답의 새 토큰을 무시하고 구 토큰(sub: "old@email.com") 유지
[결과] 이후 모든 API 호출이 구 이메일로 사용자 조회 → 404 Not Found
```

### 3단계: 프론트엔드의 토큰 미갱신
- `AuthApi.updateMyProfile`은 `UserModel`만 반환하고 있었음
- 응답 JSON에 포함된 `accessToken`, `refreshToken` 필드를 완전히 무시
- `UserModel.fromJson`은 `id`, `email`, `name`, `lastLoginAt`만 파싱

### 백엔드 서버 로그 증거
```
09:26:56  GET  /api/v1/users/me  → 200 (정상)
--- 이메일 수정 시점 (PATCH 성공, 하지만 프론트가 새 토큰 미저장) ---
09:27:20  GET  /api/v1/contents/requests  → 404 (구 토큰으로 조회 실패)
```

## 해결 방법

### `auth_api.dart` — 응답에서 토큰 추출
`updateMyProfile`의 반환 타입을 Dart Record로 변경하여 `UserModel`과 함께 토큰도 반환:

```dart
Future<({UserModel user, String? accessToken, String? refreshToken})>
    updateMyProfile({ String? name, String? email }) async {
  // ...
  final data = response.data as Map<String, dynamic>;
  return (
    user: UserModel.fromJson(data),
    accessToken: data['accessToken'] as String?,
    refreshToken: data['refreshToken'] as String?,
  );
}
```

### `auth_repository.dart` — 새 토큰 자동 저장
```dart
Future<UserModel> updateMyProfile({ String? name, String? email }) async {
  final result = await authApi.updateMyProfile(name: name, email: email);

  // 이메일 변경 시 백엔드가 새 JWT를 내려줌 → 저장해야 세션 유지
  if (result.accessToken != null && result.accessToken!.isNotEmpty) {
    await tokenService.saveAccessToken(result.accessToken!);
  }
  if (result.refreshToken != null && result.refreshToken!.isNotEmpty) {
    await tokenService.saveRefreshToken(result.refreshToken!);
  }

  return result.user;
}
```

## 방지 대책
- **API 응답 스펙 확인**: 백엔드 API가 토큰을 함께 내려주는 경우, 프론트엔드에서 반드시 해당 토큰을 저장하는 로직을 구현
- **JWT sub 설계 주의**: `sub` 클레임에 변경 가능한 값(이메일)을 사용하면 변경 시 세션이 깨지므로, UUID 등 불변 식별자 사용을 권장 (백엔드 개선 사항)
- **프로필 수정 후 통합 테스트**: 이메일/이름 변경 후 다른 API 호출이 정상 동작하는지까지 검증하는 테스트 케이스 추가
