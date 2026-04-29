import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../services/token_service.dart';
import '../datasources/remote/api_client.dart';
import '../datasources/remote/store_api.dart';
import '../models/store_model.dart';
import '../repositories/store_repository.dart';

class StoreState {
  final bool isLoading;
  final String? errorMessage;
  final StoreCreateResponse? createdStore;
  final StoreUpdateResponse? updatedStore;

  const StoreState({
    this.isLoading = false,
    this.errorMessage,
    this.createdStore,
    this.updatedStore,
  });

  StoreState copyWith({
    bool? isLoading,
    String? errorMessage,
    StoreCreateResponse? createdStore,
    StoreUpdateResponse? updatedStore,
    bool clearError = false,
    bool clearCreatedStore = false,
    bool clearUpdatedStore = false,
  }) {
    return StoreState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      createdStore: clearCreatedStore
      ? null
      : (createdStore ?? this.createdStore),
      updatedStore: clearUpdatedStore ? null : (updatedStore ?? this.updatedStore),
    );
  }
}

final tokenServiceProvider = Provider<TokenService>((ref) {
  return TokenService();
});

final apiClientProvider = Provider<ApiClient>((ref) {
  final tokenService = ref.watch(tokenServiceProvider);
  return ApiClient(tokenService: tokenService);
});

final storeApiProvider = Provider<StoreApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return StoreApi(apiClient: apiClient);
});

final storeRepositoryProvider = Provider<StoreRepository>((ref) {
  final storeApi = ref.watch(storeApiProvider);
  return StoreRepository(storeApi: storeApi);
});

final currentStoreProfileProvider = FutureProvider<StoreProfileModel?>((ref) async {
  final repository = ref.read(storeRepositoryProvider);
  try {
    return await repository.getMyStore();
  } on DioException catch (e) {
    if (e.response?.statusCode == 404) {
      return null;
    }
    rethrow;
  }
});

class StoreNotifier extends StateNotifier<StoreState> {
  final StoreRepository repository;

  StoreNotifier({
    required this.repository,
  }) : super(const StoreState());

  Future<void> createStore({
    required String storeName,
    required String businessType,
    required String address,
    required double latitude,
    required double longitude,
    String? preferredStyleLabel,
    String? instagram,
    String? naver,
    String? karrot,
  }) async {
    if (storeName.trim().isEmpty || businessType.trim().isEmpty || address.trim().isEmpty) {
      state = state.copyWith(errorMessage: '상호명, 업종, 주소는 필수입니다');
      return;
    }

    try {
      state = state.copyWith(
        isLoading: true,
        clearError: true,
        clearCreatedStore: true,
        clearUpdatedStore: true,
      );

     final created = await repository.createStore(
        storeName: storeName, 
        businessType: businessType, 
        address: address, 
        latitude: latitude, 
        longitude: longitude,
        preferredStyleLabel: preferredStyleLabel,
        instagram: instagram,
        naver: naver,
        karrot: karrot,  
      );

      state = state.copyWith(
        isLoading: false,
        createdStore: created,
        clearError: true,
      );
    } on DioException catch (e) {
      final data = e.response?.data;
      String message = '가게 등록 중 문제가 발생했어요';

      if (data is Map<String, dynamic>) {
        final parsed = StoreErrorResponse.fromJson(data);
        message = parsed.message;
      }

      state = state.copyWith(
        isLoading: false,
        errorMessage: message,
      );
    } catch (_) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: '가게 등록 중 문제가 발생했어요.',
      );
    }
  }

  Future<void> updateStore({
    String? storeName,
    String? businessType,
    String? address,
    double? latitude,
    double? longitude,
    String? preferredStyleLabel,
    String? instagram,
    String? naver,
    String? karrot,
  }) async {
    final hasNoChanges =
        storeName == null &&
        businessType == null &&
        address == null &&
        latitude == null &&
        longitude == null &&
        preferredStyleLabel == null &&
        instagram == null &&
        naver == null &&
        karrot == null;

    if (hasNoChanges) {
      state = state.copyWith(errorMessage: '변경된 내용이 없어요.');
      return;
    }

    try {
      state = state.copyWith(
        isLoading: true,
        clearError: true,
        clearUpdatedStore: true,
      );

      final updated = await repository.updateMyStore(
        storeName: storeName,
        businessType: businessType,
        address: address,
        latitude: latitude,
        longitude: longitude,
        preferredStyleLabel: preferredStyleLabel,
        instagram: instagram,
        naver: naver,
        karrot: karrot,
      );

      state = state.copyWith(
        isLoading: false,
        updatedStore: updated,
        clearError: true,
      );
    } on DioException catch (e) {
      final data = e.response?.data;
      String message = '가게 정보 수정 중 문제가 발생했어요';

      if (data is Map<String, dynamic>) {
        final parsed = StoreErrorResponse.fromJson(data);
        message = parsed.message;
      }

      state = state.copyWith(
        isLoading: false,
        errorMessage: message,
      );
    } catch (_) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: '가게 정보 수정 중 문제가 발생했어요.',
      );
    }
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }

  void clearCreatedStore() {
    state = state.copyWith(clearCreatedStore: true);
  }

  void clearUpdatedStore() {
    state = state.copyWith(clearUpdatedStore: true);
  }
}

final storeProvider = StateNotifierProvider<StoreNotifier, StoreState>((ref) {
  final repository = ref.watch(storeRepositoryProvider);
  return StoreNotifier(repository: repository);
});