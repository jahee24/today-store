# [2026-05-15] 모델 필드명 불일치로 인한 result_view.dart 컴파일 에러

## 발생 현황
`develop` 브랜치 동기화 후 `result_view.dart`에서 `ContentGenerateResponse`와 `ContentTaskResponse` 모델의 특정 필드(`apiLogId`, `contentId`)를 찾을 수 없어 빌드가 실패함.

## 원인 분석
- 최신 `content_model.dart`에서는 `apiLogId` 대신 `taskId`를 사용하도록 정의되어 있었음.
- `ContentTaskResponse` 모델에는 `contentId` 필드가 명시되어 있지 않고, 생성 결과 ID가 `result` 필드에 담겨 오는 구조였음.
- 상태 체크 로직에서 대문자 `'SUCCESS'`를 직접 비교했으나, 모델 팩토리에서는 소문자 `'success'`로 변환하여 처리하고 있었음.

## 해결 방법
- `genResponse.apiLogId`를 `genResponse.taskId`로 수정.
- `status.status == 'SUCCESS'` 대신 모델에서 제공하는 `status.isSuccess` 게터 사용.
- `status.contentId` 대신 `status.result` 필드를 사용하여 `contentId` 획득.

## 방지 대책
- 모델 클래스 변경 시 이를 참조하는 UI 코드의 컴파일 에러 여부를 즉시 확인.
- 모델에서 제공하는 `isSuccess`, `isError` 등 헬퍼 메서드를 적극 활용하여 직접적인 문자열 비교 지양.
