import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:kakao_flutter_sdk_common/kakao_flutter_sdk_common.dart';
import 'package:flutter_naver_map/flutter_naver_map.dart';

import 'config/routes.dart';
import 'config/app_theme.dart';
import 'config/constants.dart';
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  KakaoSdk.init(
    nativeAppKey: 'bc46d69e741705ca1430b64afe7d93be',
  );

  await FlutterNaverMap().init(
    clientId: AppKeys.naverMapClientId,
    onAuthFailed: (ex) {
      debugPrint('네이버 지도 인증 실패: $ex');
    },
  );

  runApp(
    const ProviderScope(
      child: MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      routerConfig: router,
      builder: (context, child) {
        final mediaQuery = MediaQuery.of(context);
        final baseTextScale = AppLayout.textScale(context);
        final deviceScale =
            (mediaQuery.size.width / AppLayout.designWidth).clamp(0.85, 1.0);
        final responsiveTextScale =
            (baseTextScale * deviceScale).clamp(0.9, 1.05);

        final effectiveChild = child ?? const SizedBox.shrink();
        return MediaQuery(
          data: mediaQuery.copyWith(
            textScaler: TextScaler.linear(responsiveTextScale),
          ),
          child: ColoredBox(
            color: AppTheme.backgroundColor,
            child: Transform.scale(
              scale: deviceScale,
              alignment: Alignment.topCenter,
              child: Align(
                alignment: Alignment.topCenter,
                child: SizedBox(
                  width: mediaQuery.size.width / deviceScale,
                  height: mediaQuery.size.height / deviceScale,
                  child: effectiveChild,
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
