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

    final authRepository = ref.read(authRepositoryProvider);
    final isAuthenticated = await authRepository.tryAutoLogin();

    if (!isAuthenticated) {
      return SplashStatus.unauthenticated;
    }

    final storeProfile = await ref.read(currentStoreProfileProvider.future);
    if (storeProfile == null) {
      return SplashStatus.needsProfileSetup;
    }

    return SplashStatus.authenticated;
  }
}