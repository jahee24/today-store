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

class StoreUpdateRequest {
  final String? storeName;
  final String? businessType;
  final String? address;
  final double? latitude;
  final double? longitude;
  final String? preferredStyle;
  final String? snsInstagram;
  final String? snsNaver;
  final String? snsKarrot;

  const StoreUpdateRequest({
    this.storeName,
    this.businessType,
    this.address,
    this.latitude,
    this.longitude,
    this.preferredStyle,
    this.snsInstagram,
    this.snsNaver,
    this.snsKarrot,
  });

  Map<String, dynamic> toJson() {
    return {
      if (storeName != null && storeName!.trim().isNotEmpty)
        'storeName': storeName!.trim(),
      if (businessType != null && businessType!.trim().isNotEmpty)
        'businessType': businessType!.trim(),
      if (address != null && address!.trim().isNotEmpty) 'address': address!.trim(),
      if (latitude != null) 'latitude': latitude,
      if (longitude != null) 'longitude': longitude,
      if (preferredStyle != null && preferredStyle!.trim().isNotEmpty)
        'preferredStyle': preferredStyle!.trim(),
      if (snsInstagram != null && snsInstagram!.trim().isNotEmpty)
        'snsInstagram': snsInstagram!.trim(),
      if (snsNaver != null && snsNaver!.trim().isNotEmpty) 'snsNaver': snsNaver!.trim(),
      if (snsKarrot != null && snsKarrot!.trim().isNotEmpty)
        'snsKarrot': snsKarrot!.trim(),
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
      id: (json['id'] ?? '').toString(),
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      if (createdAt != null) 'createdAt': createdAt!.toIso8601String(),
    };
  }
}

class StoreUpdateResponse {
  final String id;
  final DateTime? updatedAt;

  const StoreUpdateResponse({
    required this.id,
    this.updatedAt,
  });

  factory StoreUpdateResponse.fromJson(Map<String, dynamic> json) {
    return StoreUpdateResponse(
      id: (json['id'] ?? '').toString(),
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }
}

class StoreProfileModel {
  final String id;
  final String storeName;
  final String businessType;
  final String address;
  final double latitude;
  final double longitude;
  final String? preferredStyle;
  final StoreSnsModel? sns;

  const StoreProfileModel({
    required this.id,
    required this.storeName,
    required this.businessType,
    required this.address,
    required this.latitude,
    required this.longitude,
    this.preferredStyle,
    this.sns,
  });

  factory StoreProfileModel.fromJson(Map<String, dynamic> json) {
    final snsJson = json['sns'] as Map<String, dynamic>?;
    return StoreProfileModel(
      id: (json['id'] ?? '').toString(),
      storeName: (json['storeName'] ?? '').toString(),
      businessType: (json['businessType'] ?? '').toString(),
      address: (json['address'] ?? '').toString(),
      latitude: _asDouble(json['latitude']) ?? 0,
      longitude: _asDouble(json['longitude']) ?? 0,
      preferredStyle: json['preferredStyle']?.toString(),
      sns: snsJson == null ? null : StoreSnsModel.fromJson(snsJson),
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
      status: _asInt(json['status']),
      code: json['code'] as String?,
      message: json['message'] as String? ?? '알 수 없는 오류가 발생했어요.',
      errors: (json['errors'] as List?) ?? const [],
      timestamp: json['timestamp'] != null
          ? DateTime.tryParse(json['timestamp'].toString())
          : null,
    );
  }
}

int? _asInt(dynamic value) {
  if (value is int) {
    return value;
  }
  if (value is String) {
    return int.tryParse(value);
  }
  return null;
}

double? _asDouble(dynamic value) {
  if (value is double) {
    return value;
  }
  if (value is int) {
    return value.toDouble();
  }
  if (value is String) {
    return double.tryParse(value);
  }
  return null;
}