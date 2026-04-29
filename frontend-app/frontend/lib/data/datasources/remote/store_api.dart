import 'package:dio/dio.dart';

import 'api_client.dart';
import '../../models/store_model.dart';

class StoreApi {
  final ApiClient apiClient;

  StoreApi({
    required this.apiClient,
  });

  Dio get _dio => apiClient.dio;

  Future<StoreCreateResponse> createStore(StoreCreateRequest request) async {
    final response = await _dio.post(
      '/api/v1/stores',
      data: request.toJson(),
    );

    return StoreCreateResponse.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  Future<StoreProfileModel> getMyStore() async {
    final response = await _dio.get('/api/v1/stores/me');
    return StoreProfileModel.fromJson(response.data as Map<String, dynamic>);
  }

  Future<StoreUpdateResponse> updateMyStore(StoreUpdateRequest request) async {
    final response = await _dio.patch(
      '/api/v1/stores/me',
      data: request.toJson(),
    );
    return StoreUpdateResponse.fromJson(response.data as Map<String, dynamic>);
  }
}