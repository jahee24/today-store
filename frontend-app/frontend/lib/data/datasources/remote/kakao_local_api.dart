import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../presentation/models/address_search_item.dart';
import '../../../presentation/models/address_pick_result.dart';

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
    
    try {
    final addressResponse = await _dio.get(
      '/v2/local/search/address.json',
      queryParameters: {
        'query': trimmed,
        'analyze_type': 'similar',
        'page': 1,
        'size': 15,
      },
    );

    final keywordResponse = await _dio.get(
      '/v2/local/search/keyword.json',
      queryParameters: {
        'query': trimmed,
        'page': 1,
        'size': 15,
      },
    );

    final addressDocuments = addressResponse.data['documents'] as List? ?? const [];
    final keywordDocuments = keywordResponse.data['documents'] as List? ?? const [];

    final addressItems = addressDocuments
        .map((e) => AddressSearchItem.fromKakaoJson(e as Map<String, dynamic>))
        .toList();

    final keywordItems = keywordDocuments
        .map((e) => AddressSearchItem.fromKakaoKeywordJson(e as Map<String, dynamic>))
        .toList();

    final Map<String, AddressSearchItem> unique = {};

    for (final item in [...addressItems, ...keywordItems]) {
      if (item.latitude == 0 || item.longitude == 0) continue;

      final key = '${item.roadAddress},${item.jibunAddress},${item.latitude},${item.longitude}';
      unique[key] = item;
    }

    return unique.values.toList();
  } catch (e) {
    debugPrint('카카오 주소 검색 오류: $e');
    rethrow;
  }
  }

  Future<AddressPickResult> coordToAddress({
    required double latitude,
    required double longitude,
  }) async {
    final response = await _dio.get(
      '/v2/local/geo/coord2address.json',
      queryParameters: {
        'x': longitude,
        'y': latitude,
        'input_coord': 'WGS84',
      },
    );

    final documents = response.data['documents'] as List? ?? const [];

    if (documents.isEmpty) {
      return AddressPickResult(
        latitude: latitude,
        longitude: longitude,
        roadAddress: '',
        jibunAddress: null,
      );
    }

    final first = documents.first as Map<String, dynamic>;
    final road = first['road_address'] as Map<String, dynamic>?;
    final address = first['address'] as Map<String, dynamic>?;

    final roadAddress = road?['address_name'] as String? ?? '';
    final jibunAddress = address?['address_name'] as String? ?? '';

    return AddressPickResult(
      latitude: latitude,
      longitude: longitude,
      roadAddress: roadAddress.isNotEmpty ? roadAddress : jibunAddress,
      jibunAddress: jibunAddress.isNotEmpty ? jibunAddress : null,
    );
  }
}