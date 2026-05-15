# [2026-05-13] 이미지 재생성 중간 실패 - 폴링 타임아웃

### 증상

이미지 재생성(베리에이션) 요청 후 약 1분이 지나면 **"이미지 생성이 지연되고 있어요"** 오류와 함께 처음 화면으로 돌아감.
RunComfy 서버에서는 정상적으로 작업이 시작되었으나 결과가 오기 전에 앱이 포기함.

### 로그로 확인

```
15:03:04  POST /vary → 202 (RunComfy 작업 시작 접수)
15:03:11  서버: "Successfully initiated RunComfy variation: f3d704ff"
15:03:13
~ 
15:04:05  GET /variations 폴링 × 30회 → 243 B (빈 결과, 아직 처리 중)
          ← 여기서 앱이 타임아웃으로 실패 처리
```

`243 B` = variations 빈 배열 응답 = RunComfy가 아직 처리 중인 상태.

### 원인

`processing_loading.dart`의 `maxAttempts = 30` (30회 × 2초 = **최대 60초**).
RunComfy 이미지 베리에이션은 **최대 10분** 소요 → 폴링이 결과 오기 전에 종료.

```dart
// 문제가 된 코드
const maxAttempts = 30;  // 60초만 기다림
```

### 해결

```dart
// processing_loading.dart
// RunComfy 이미지 생성은 최대 10분 소요 → 2초 × 300회 = 600초(10분) 대기
const maxAttempts = 300;

// 진행률도 300회에 걸쳐 서서히 올라가도록 조정
final progress = (50 + (attempt + 1) * 0.15).round().clamp(50, 95);
```

사용자 안내 메시지도 수정:
```dart
subtitle: '분석 완료\n이미지 합성 중이에요 (최대 10분 소요)',
```

### 향후 개선 방향

현재 구조는 로딩 화면에서 계속 대기해야 하는 방식 (UX 불편).
장기적으로는 **백그라운드 처리 + 알림 방식** 검토 필요:

1. 작업 요청 후 대시보드로 즉시 이동
2. 백엔드 완료 시 FCM 푸시 알림 or 이력 탭 뱃지 표시
3. 사용자가 이력 탭 진입 시 완료된 이미지 확인

---
> **관련 파일**: `frontend-app/frontend/lib/presentation/screens/processing/processing_loading.dart`
