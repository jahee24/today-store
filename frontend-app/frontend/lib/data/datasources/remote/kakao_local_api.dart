import 'package:dio/dio.dart';

import '../../../presentation/models/address_search_item.dart';

class KakaoLocalApi {
  final Dio _dio;

  KakaoLocalApi({
    required String restApiKey,
  }) : _dio = Dio(
    BaseOptions(
      baseUrl: 'https://dapi.kakao.com',
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 5),
      headers: {
        'Authorization': 'KakaoAK $restApiKey',
      },
    ),
  );

  Future<List<AddressSearchItem>> searchAddress(String query) async {
    final trimmed = query.trim();
    if (trimmed.isEmpty) return [];
    
    final response = await _dio.get(
      '/v2/local/search/address.json',
      queryParameters: {
        'query': trimmed,
        'analyze_type': 'similar',
        'page': 1,
        'size': 15,
      },
    );

    final documents = (response.data['documents'] as List? ?? const []);
    return documents
        .map((e) => AddressSearchItem.fromKakaoJson(e as Map<String, dynamic>))
        .toList();
  }
}