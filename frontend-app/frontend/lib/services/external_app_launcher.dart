import 'dart:io';

import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

/// 스토어에서 설치를 유도할 수 있는 외부 앱(Instagram, 당근, 네이버 등).
enum StoreListingApp {
  instagram,
  daangn,
  naver,
}

extension StoreListingAppX on StoreListingApp {
  String get displayName {
    switch (this) {
      case StoreListingApp.instagram:
        return 'Instagram';
      case StoreListingApp.daangn:
        return '당근마켓';
      case StoreListingApp.naver:
        return '네이버';
    }
  }

  /// 앱이 설치된 것으로 간주할 때 사용하는 커스텀 스킴(또는 앱 전용 URI).
  Uri get primaryLaunchUri {
    switch (this) {
      case StoreListingApp.instagram:
        return Uri.parse('instagram://app');
      case StoreListingApp.daangn:
        return Uri.parse('daangn://');
      case StoreListingApp.naver:
        return Uri.parse('naversearchapp://');
    }
  }

  Uri get playStoreUri {
    switch (this) {
      case StoreListingApp.instagram:
        return Uri.parse(
          'https://play.google.com/store/apps/details?id=com.instagram.android',
        );
      case StoreListingApp.daangn:
        return Uri.parse(
          'https://play.google.com/store/apps/details?id=com.towneers.www',
        );
      case StoreListingApp.naver:
        return Uri.parse(
          'https://play.google.com/store/apps/details?id=com.nhn.android.search',
        );
    }
  }

  Uri get appStoreUri {
    switch (this) {
      case StoreListingApp.instagram:
        return Uri.parse('itms-apps://itunes.apple.com/app/id389801252');
      case StoreListingApp.daangn:
        return Uri.parse('itms-apps://itunes.apple.com/app/id1018769995');
      case StoreListingApp.naver:
        return Uri.parse('itms-apps://itunes.apple.com/app/id393499958');
    }
  }

  Uri get appStoreWebUri {
    switch (this) {
      case StoreListingApp.instagram:
        return Uri.parse('https://apps.apple.com/app/instagram/id389801252');
      case StoreListingApp.daangn:
        return Uri.parse(
          'https://apps.apple.com/kr/app/%EB%8B%B9%EA%B7%BC%EB%A7%88%EC%BC%93/id1018769995',
        );
      case StoreListingApp.naver:
        return Uri.parse(
          'https://apps.apple.com/kr/app/%EB%84%A4%EC%9D%B4%EB%B2%84/id393499958',
        );
    }
  }

  Uri get storeUri =>
      Platform.isIOS ? appStoreUri : playStoreUri;

  Uri? get marketUri {
    if (Platform.isIOS) {
      return null;
    }
    switch (this) {
      case StoreListingApp.instagram:
        return Uri.parse('market://details?id=com.instagram.android');
      case StoreListingApp.daangn:
        return Uri.parse('market://details?id=com.towneers.www');
      case StoreListingApp.naver:
        return Uri.parse('market://details?id=com.nhn.android.search');
    }
  }
}

class ExternalAppLauncher {
  ExternalAppLauncher._();

  static const MethodChannel _shareChannel = MethodChannel('today_store/share');

  static String _nativeAppKey(StoreListingApp app) {
    switch (app) {
      case StoreListingApp.instagram:
        return 'instagram';
      case StoreListingApp.daangn:
        return 'daangn';
      case StoreListingApp.naver:
        return 'naver';
    }
  }

  /// OS에 앱 패키지(iOS는 URL 스킴)로 설치 여부를 확인합니다.
  static Future<bool> isAppInstalled(StoreListingApp app) async {
    if (Platform.isAndroid || Platform.isIOS) {
      try {
        final installed = await _shareChannel.invokeMethod<bool>(
          'isAppInstalled',
          {'app': _nativeAppKey(app)},
        );
        if (installed != null) {
          return installed;
        }
      } catch (_) {
        // 채널 미구현 등 → 아래로 폴백
      }
    }
    return canLaunchUrl(app.primaryLaunchUri);
  }

  static StoreListingApp? parseAppKey(String? raw) {
    switch (raw?.trim().toLowerCase()) {
      case 'instagram':
        return StoreListingApp.instagram;
      case 'daangn':
      case 'karrot':
        return StoreListingApp.daangn;
      case 'naver':
        return StoreListingApp.naver;
      default:
        return null;
    }
  }

  /// 해당 앱이 열릴 가능성이 있는지(설치 여부 추정) 확인합니다.
  static Future<bool> canOpenApp(StoreListingApp app) async {
    return isAppInstalled(app);
  }

  static Future<bool> openApp(StoreListingApp app) async {
    if (app == StoreListingApp.daangn) {
      final uris = [Uri.parse('daangn://'), Uri.parse('karrot://')];
      for (final uri in uris) {
        if (await canLaunchUrl(uri) &&
            await launchUrl(uri, mode: LaunchMode.externalApplication)) {
          return true;
        }
      }
      if (await isAppInstalled(app)) {
        for (final uri in uris) {
          try {
            if (await launchUrl(uri, mode: LaunchMode.externalApplication)) {
              return true;
            }
          } catch (_) {}
        }
      }
      return false;
    }

    final uri = app.primaryLaunchUri;
    if (await canLaunchUrl(uri)) {
      return launchUrl(uri, mode: LaunchMode.externalApplication);
    }
    if (await isAppInstalled(app)) {
      try {
        return await launchUrl(uri, mode: LaunchMode.externalApplication);
      } catch (_) {
        return false;
      }
    }
    return false;
  }

  static Future<bool> openStore(StoreListingApp app) async {
    if (Platform.isAndroid) {
      final market = app.marketUri;
      if (market != null && await canLaunchUrl(market)) {
        final ok = await launchUrl(market, mode: LaunchMode.externalApplication);
        if (ok) {
          return true;
        }
      }
    }

    final storeUri = app.storeUri;
    // iOS에서 itms-apps:// 가 실패할 수 있으므로(시뮬레이터 등) 체크
    if (await canLaunchUrl(storeUri)) {
      final ok = await launchUrl(storeUri, mode: LaunchMode.externalApplication);
      if (ok) return true;
    }

    // 폴백: 웹 브라우저로 열기
    final webUri = Platform.isIOS ? app.appStoreWebUri : app.playStoreUri;
    if (await canLaunchUrl(webUri)) {
      return launchUrl(webUri, mode: LaunchMode.externalApplication);
    }

    return false;
  }
}
