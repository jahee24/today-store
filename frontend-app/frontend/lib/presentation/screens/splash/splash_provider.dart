import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/providers/auth_provider.dart';
import '../../../data/providers/store_provider.dart';

// 상태 정의
enum SplashStatus {
  loading,
  authenticated,
  needsProfileSetup,
  unauthenticated,
}

// provider 정의
final splashProvider = AsyncNotifierProvider<SplashNotifier, SplashStatus>(SplashNotifier.new);

// Notifier
class SplashNotifier extends AsyncNotifier<SplashStatus> {
  @override
  Future<SplashStatus> build() async {
    await Future.delayed(const Duration(milliseconds: 1800));
    
    final bool isAuthenticated;
    try {
      isAuthenticated = await ref.read(authProvider.notifier).tryAutoLogin();
    } catch (_) {
      return SplashStatus.unauthenticated;
    }

    if (!isAuthenticated) {
      return SplashStatus.unauthenticated;
    }

    try {
      final storeProfile = await ref.read(currentStoreProfileProvider.future);
      if (storeProfile == null) {
        return SplashStatus.needsProfileSetup;
      }
      return SplashStatus.authenticated;
    } catch (_) {
      // Store profile fetch failure should not block app entry.
      return SplashStatus.authenticated;
    }
  }
}