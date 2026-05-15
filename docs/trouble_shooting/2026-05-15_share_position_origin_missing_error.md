# [2026-05-15] iOS/iPad에서 Share.share 호출 시 sharePositionOrigin 누락으로 인한 앱 크래시

## 발생 현황
iOS 시뮬레이터(또는 아이패드)에서 콘텐츠 공유 버튼 클릭 시 `PlatformException(error, sharePositionOrigin: argument must be set, ...)` 발생하며 앱이 비정상 종료됨.

## 원인 분석
- `share_plus` 패키지는 아이패드와 같은 대화면 기기에서 공유 시트가 어느 위치에서 팝업될지 지정하는 `sharePositionOrigin` 파라미터를 필수로 요구함.
- 기존 코드에서는 단순히 `Share.share(text)`만 호출하여 위치 정보가 누락됨.

## 해결 방법
- `_shareContent` 메서드 내에서 `context.findRenderObject()`를 사용하여 현재 위젯의 위치와 크기 정보를 가져옴.
- `Share.share` 호출 시 `sharePositionOrigin` 파라미터에 해당 영역 정보를 전달함.

```dart
void _shareContent(String text) {
  final box = context.findRenderObject() as RenderBox?;
  Share.share(
    text,
    sharePositionOrigin: box != null ? box.localToGlobal(Offset.zero) & box.size : null,
  );
}
```

## 방지 대책
- iOS/iPad 지원이 포함된 프로젝트에서는 공유 기능 구현 시 반드시 `sharePositionOrigin`을 설정하는 습관을 가짐.
- `BuildContext`가 사용 가능한 위치에서 공유 로직을 실행하도록 구조화.
