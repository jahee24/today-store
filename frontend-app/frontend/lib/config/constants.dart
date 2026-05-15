import 'package:flutter/widgets.dart';

class AppKeys {
  static const String naverMapClientId = String.fromEnvironment(
    'NAVER_MAP_CLIENT_ID',
  );
  static const String naverMapClientSecret = String.fromEnvironment(
    'NAVER_MAP_CLIENT_SECRET',
  );
  static const String kakaoRestApiKey = String.fromEnvironment(
    'KAKAO_REST_API_KEY',
  );
}

class AppLayout {
  AppLayout._();

  static const double designWidth = 390;
  static const double designHeight = 844;

  static const double minScale = 0.9;
  static const double maxScale = 1.15;

  static double scale(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    final widthScale = size.width / designWidth;
    final heightScale = size.height / designHeight;
    final value = widthScale < heightScale ? widthScale : heightScale;
    return value.clamp(minScale, maxScale);
  }

  static double h(BuildContext context, double base) {
    return (base * scale(context)).clamp(base * minScale, base * maxScale);
  }

  static double f(BuildContext context, double base) {
    return (base * scale(context)).clamp(base * 0.92, base * 1.08);
  }

  static double textScale(BuildContext context) {
    final userScale = MediaQuery.textScalerOf(context).scale(1.0);
    return userScale.clamp(0.95, 1.05);
  }
}