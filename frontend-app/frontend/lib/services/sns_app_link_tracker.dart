import 'package:hive_flutter/hive_flutter.dart';

import 'external_app_launcher.dart';

/// 기기에 앱이 설치되어 있고, 앱을 통해 한 번이라도 열었는지 로컬에 기록합니다.
class SnsAppLinkTracker {
  SnsAppLinkTracker._();

  static const _boxName = 'sns_app_link_tracker';
  static bool _initialized = false;

  static Future<void> _ensureInitialized() async {
    if (_initialized) {
      return;
    }
    await Hive.initFlutter();
    if (!Hive.isBoxOpen(_boxName)) {
      await Hive.openBox<bool>(_boxName);
    }
    _initialized = true;
  }

  static Future<Box<bool>> _box() async {
    await _ensureInitialized();
    return Hive.box<bool>(_boxName);
  }

  static Future<void> markOpened(StoreListingApp app) async {
    final box = await _box();
    await box.put(app.name, true);
  }

  static Future<bool> hasOpened(StoreListingApp app) async {
    final box = await _box();
    return box.get(app.name, defaultValue: false) ?? false;
  }

  /// 앱 설치 + 앱을 통한 이동 이력이 모두 있을 때만 연동됨으로 표시합니다.
  static Future<bool> isLinked(StoreListingApp app) async {
    final installed = await ExternalAppLauncher.isAppInstalled(app);
    if (!installed) {
      return false;
    }
    return hasOpened(app);
  }

  static Future<Map<StoreListingApp, bool>> linkedStatusForAll() async {
    final results = <StoreListingApp, bool>{};
    for (final app in StoreListingApp.values) {
      results[app] = await isLinked(app);
    }
    return results;
  }
}
