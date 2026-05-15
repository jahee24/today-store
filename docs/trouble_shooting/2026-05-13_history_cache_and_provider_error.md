# [2026-05-13] 이력 화면 캐시 문제 & FutureProvider.autoDispose 타입 오류

### 증상

1. **이력 탭에 데이터가 표시되지 않음**
   - 로그아웃 후 재로그인해도 콘텐츠 이력 목록이 비어있거나 이전 상태 그대로 유지됨
   - 서버 API 호출 자체가 발생하지 않음 (Cloud Run 로그에 `/api/v1/contents/requests` 요청 없음)

2. **`FutureProvider.autoDispose` 타입 불일치 오류**
   ```
   type 'FutureProvider<ContentRequestsResponse>' is not a subtype of type
   'AutoDisposeFutureProvider<ContentRequestsResponse>' of 'function result'
   ```
   - 앱 화면 전체가 빨간 에러 화면으로 표시됨

---

### 원인 분석

#### 이력 캐시 문제

`historyRequestsProvider`가 `FutureProvider`로 선언되어 있어 **앱 세션 동안 결과를 캐시**함.

```dart
// 문제가 된 코드
final historyRequestsProvider = FutureProvider<ContentRequestsResponse>((ref) {
  final repository = ref.read(contentRepositoryProvider);
  return repository.getContentRequests(page: 1, size: 30);
});
```

`FutureProvider`는 처음 호출 시 결과를 캐시하고 이후 재호출하지 않음.
로그아웃 → 재로그인해도 `HistoryScreen` 위젯이 dispose되지 않으면
provider가 살아있어 이전 캐시(빈 데이터 또는 에러 상태)를 그대로 보여줌.

#### autoDispose 타입 오류

캐시 문제 해결을 위해 `FutureProvider.autoDispose`로 변경을 시도했으나,
**provider 타입 변경은 hot reload로 반영되지 않음**.
기존 provider 인스턴스(일반 `FutureProvider`)가 메모리에 살아있는 상태에서
새 타입(`AutoDisposeFutureProvider`)을 watch 하려다 타입 불일치 발생.

> **핵심**: Riverpod provider의 타입(autoDispose 여부) 변경은 반드시 **Hot Restart** 필요.
> Hot Reload만으로는 provider 인스턴스가 교체되지 않아 타입 불일치 런타임 에러 발생.

---

### 해결 방법

`FutureProvider.autoDispose` 대신, **`initState`에서 `ref.invalidate()`로 명시적 갱신**하는 방식 채택.

```dart
// history_screen.dart

// 1. provider는 일반 FutureProvider 유지
final historyRequestsProvider = FutureProvider<ContentRequestsResponse>((ref) {
  final repository = ref.read(contentRepositoryProvider);
  return repository.getContentRequests(page: 1, size: 30);
});

// 2. State 클래스에 initState 추가
class _HistoryScreenState extends ConsumerState<HistoryScreen> {
  @override
  void initState() {
    super.initState();
    // 화면 진입마다 최신 이력 fetch (로그아웃 후 재로그인 시에도 갱신)
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.invalidate(historyRequestsProvider);
    });
  }
  // ...
}
```

#### `addPostFrameCallback`을 쓰는 이유
`initState` 내에서는 아직 위젯 트리가 완전히 빌드되기 전이라 `ref`를 직접 사용하면 안 됨.
`addPostFrameCallback`으로 첫 프레임 렌더링 이후에 실행하도록 지연 처리.

#### 추가로 적용한 UX 개선
```dart
// 에러 상태에 '다시 시도' 버튼 추가
error: (_, __) => Center(
  child: Column(
    children: [
      Text('콘텐츠 이력을 불러오지 못했어요.'),
      TextButton(
        onPressed: () => ref.invalidate(historyRequestsProvider),
        child: const Text('다시 시도'),
      ),
    ],
  ),
),

// 목록에 pull-to-refresh 추가
RefreshIndicator(
  onRefresh: () async => ref.invalidate(historyRequestsProvider),
  child: ListView.separated(...),
),
```

---

### 교훈

| 상황 | 올바른 방법 |
|---|---|
| Provider 타입 변경 (autoDispose 추가/제거) | **Hot Restart** (R) 필요. Hot Reload (r) 불가 |
| 화면 진입 시마다 데이터 갱신 | `initState` + `addPostFrameCallback` + `ref.invalidate()` |
| autoDispose가 필요한 경우 | 위젯 unmount 시 자동 해제가 필요한 로컬 상태에만 사용. 전역 공유 데이터엔 일반 Provider + 명시적 invalidate 권장 |

---
> **관련 파일**: `frontend-app/frontend/lib/presentation/screens/history/history_screen.dart`
