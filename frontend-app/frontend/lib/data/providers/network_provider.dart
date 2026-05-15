import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../services/token_service.dart';
import '../datasources/remote/api_client.dart';

/// 인증 실패(세션 만료) 이벤트를 전달하기 위한 스트림
final authFailureStreamProvider = StreamProvider<void>((ref) {
  final controller = StreamController<void>.broadcast();
  ref.onDispose(() => controller.close());
  return controller.stream;
});

/// 인증 실패 이벤트를 발생시키는 함수를 제공하는 Provider
final authFailureTriggerProvider = Provider<void Function()>((ref) {
  return () {
    // ignore: unused_result
    ref.read(authFailureStreamProvider.stream).timeout(const Duration(milliseconds: 0));
    // 실제로는 StreamController에 이벤트를 넣어야 함. 
    // 아래와 같이 구조를 변경함.
  };
});

/// 전역적으로 사용될 TokenService Provider
final tokenServiceProvider = Provider<TokenService>((ref) {
  return TokenService();
});

/// 인증 실패 이벤트를 관리하는 전역 Controller
final _authFailureControllerProvider = Provider<StreamController<void>>((ref) {
  final controller = StreamController<void>.broadcast();
  ref.onDispose(() => controller.close());
  return controller;
});

/// 전역적으로 사용될 ApiClient Provider
final apiClientProvider = Provider<ApiClient>((ref) {
  final tokenService = ref.watch(tokenServiceProvider);
  final controller = ref.watch(_authFailureControllerProvider);
  
  return ApiClient(
    tokenService: tokenService,
    onAuthFailure: () => controller.add(null),
  );
});

/// AuthNotifier가 구독할 스트림
final authFailureEventProvider = StreamProvider<void>((ref) {
  return ref.watch(_authFailureControllerProvider).stream;
});
