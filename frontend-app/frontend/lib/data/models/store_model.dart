class StoreSnsModel {
  final String? instagram;
  final String? naver;
  final String? karrot;

  const StoreSnsModel ({
    this.instagram,
    this.naver,
    this.karrot,
  });

  Map<String, dynamic> toJson() {
    return {
      if (instagram != null && instagram!.trim().isNotEmpty)
      'instagram': instagram!.trim(),
      if (naver != null && naver!.trim().isNotEmpty) 'naver': naver!.trim(),
      if (karrot != null && karrot!.trim().isNotEmpty) 'karrot': karrot!.trim(),
    };
  }

  factory StoreSnsModel.fromJson(Map<String, dynamic> json) {
    return StoreSnsModel(
      instagram: json['instagram'] as String?,
      naver: json['naver'] as String?,
      karrot: json['karrot'] as String?,
    );
  }

  bool get isEmpty {
    final hasInstagram = instagram != null && instagram!.trim().isNotEmpty;
    final hasNaver = naver != null && naver!.trim().isNotEmpty;
    final hasKarrot = karrot != null && karrot!.trim().isNotEmpty;
    return !hasInstagram && !hasNaver && !hasKarrot;
  }
}

class StoreCreateRequest {
  final String storeName;
  final String businessType;
  final String address;
  final double latitude;
  final double longitude;
  final String? preferredStyle;
  final StoreSnsModel? sns;

  const StoreCreateRequest({
    required this.storeName,
    required this.businessType,
    required this.address,
    required this.latitude,
    required this.longitude,
    this.preferredStyle,
    this.sns,
  });

  Map<String, dynamic> toJson() {
    return {
      'storeName': storeName.trim(),
      'businessType': businessType.trim(),
      'address': address.trim(),
      'latitude': latitude,
      'longitude': longitude,
      if (preferredStyle != null && preferredStyle!.trim().isNotEmpty)
      'preferredStyle': preferredStyle!.trim(),
      if (sns != null && !sns!.isEmpty) 'sns': sns!.toJson(),
    };
  }
}

class StoreCreateResponse {
  final String id;
  final DateTime? createdAt;

  const StoreCreateResponse({
    required this.id,
    this.createdAt,
  });

  factory StoreCreateResponse.fromJson(Map<String, dynamic> json) {
    return StoreCreateResponse(
      id: json['id'] as String? ?? '',
      createdAt: json['createdAt'] != null
      ? DateTime.tryParse(json['createdAt'] as String)
      : null,
    );
  }
}

class StoreErrorResponse {
  final int? status;
  final String? code;
  final String message;
  final List<dynamic> errors;
  final DateTime? timestamp;

  const StoreErrorResponse({
    this.status,
    this.code,
    required this.message,
    this.errors = const [],
    this.timestamp,
  });

  factory StoreErrorResponse.fromJson(Map<String, dynamic> json) {
    return StoreErrorResponse(
      status: json['status'] as int?,
      code: json['code'] as String?,
      message: json['message'] as String? ?? '알 수 없는 오류가 발생했어요.',
      errors: (json['errors'] as List?) ?? const [],
      timestamp: json['timestamp'] != null
      ? DateTime.tryParse(json['timestamp'] as String)
      : null,
    );
  }
}