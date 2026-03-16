import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/providers/auth_provider.dart';

// 상태 정의
enum SplashStatus { loading, authenticated, unauthenticated }

// provider 정의
final splashProvider = AsyncNotifierProvider<SplashNotifier, SplashStatus>(SplashNotifier.new);

// Notifier
class SplashNotifier extends AsyncNotifier<SplashStatus> {
  @override
  Future<SplashStatus> build() async {
    await Future.delayed(const Duration(milliseconds: 1800));

    final authRepository = ref.read(authRepositoryProvider);
    final success = await authRepository.tryAutoLogin();

    return success ? SplashStatus.authenticated : SplashStatus.unauthenticated;
  }
}