import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../presentation/models/address_pick_result.dart';
import '../../../presentation/models/address_search_item.dart';

class NaverLocalApi {
  final Dio _geocodeDio;
  final Dio _reverseDio;

  NaverLocalApi({
    required String clientId,
    required String clientSecret,
  })  : _geocodeDio = Dio(
          BaseOptions(
            baseUrl: 'https://maps.apigw.ntruss.com/map-geocode/v2',
            connectTimeout: const Duration(seconds: 5),
            receiveTimeout: const Duration(seconds: 5),
            headers: {
              'X-NCP-APIGW-API-KEY-ID': clientId,
              'X-NCP-APIGW-API-KEY': clientSecret,
            },
          ),
        ),
        _reverseDio = Dio(
          BaseOptions(
            baseUrl: 'https://maps.apigw.ntruss.com/map-reversegeocode/v2',
            connectTimeout: const Duration(seconds: 5),
            receiveTimeout: const Duration(seconds: 5),
            headers: {
              'X-NCP-APIGW-API-KEY-ID': clientId,
              'X-NCP-APIGW-API-KEY': clientSecret,
            },
          ),
        );

  Future<List<AddressSearchItem>> searchAddress(String query) async {
    final trimmed = query.trim();
    if (trimmed.isEmpty) return [];

    try {
      final response = await _geocodeDio.get(
        '/geocode',
        queryParameters: {
          'query': trimmed,
          'count': 15,
        },
      );

      final addresses = response.data['addresses'] as List? ?? const [];
      return addresses
          .whereType<Map<String, dynamic>>()
          .map(AddressSearchItem.fromNaverGeocodeJson)
          .where((item) => item.latitude != 0 && item.longitude != 0)
          .toList();
    } catch (e) {
      debugPrint('네이버 주소 검색 오류: $e');
      rethrow;
    }
  }

  Future<AddressPickResult> coordToAddress({
    required double latitude,
    required double longitude,
  }) async {
    try {
      final response = await _reverseDio.get(
        '/gc',
        queryParameters: {
          'coords': '$longitude,$latitude',
          'orders': 'roadaddr,addr',
          'output': 'json',
        },
      );

      final results = response.data['results'] as List? ?? const [];
      String roadAddress = '';
      String jibunAddress = '';

      for (final result in results.whereType<Map<String, dynamic>>()) {
        final name = result['name']?.toString() ?? '';
        final region = result['region'] as Map<String, dynamic>? ?? const {};
        final land = result['land'] as Map<String, dynamic>? ?? const {};
        final assembled = _assembleAddress(region: region, land: land);
        if (assembled.isEmpty) continue;

        if (name == 'roadaddr') {
          roadAddress = assembled;
        } else if (name == 'addr') {
          jibunAddress = assembled;
        }
      }

      final display = roadAddress.isNotEmpty ? roadAddress : jibunAddress;

      return AddressPickResult(
        roadAddress: display,
        jibunAddress: jibunAddress.isNotEmpty ? jibunAddress : null,
        latitude: latitude,
        longitude: longitude,
      );
    } catch (e) {
      debugPrint('네이버 좌표 주소 변환 오류: $e');
      rethrow;
    }
  }

  String _assembleAddress({
    required Map<String, dynamic> region,
    required Map<String, dynamic> land,
  }) {
    final area1 = (region['area1'] as Map<String, dynamic>? ?? const {})['name']
            ?.toString() ??
        '';
    final area2 = (region['area2'] as Map<String, dynamic>? ?? const {})['name']
            ?.toString() ??
        '';
    final area3 = (region['area3'] as Map<String, dynamic>? ?? const {})['name']
            ?.toString() ??
        '';
    final area4 = (region['area4'] as Map<String, dynamic>? ?? const {})['name']
            ?.toString() ??
        '';
    final number1 = land['number1']?.toString() ?? '';
    final number2 = land['number2']?.toString() ?? '';
    final name = land['name']?.toString() ?? '';

    final buffer = <String>[
      area1,
      area2,
      area3,
      area4,
      if (name.isNotEmpty) name,
      if (number1.isNotEmpty && number2.isNotEmpty) '$number1-$number2',
      if (number1.isNotEmpty && number2.isEmpty) number1,
    ];

    return buffer.where((s) => s.trim().isNotEmpty).join(' ').trim();
  }
}
