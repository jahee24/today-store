class AddressSearchItem {
  final String addressName;
  final String roadAddress;
  final String jibunAddress;
  final double latitude;
  final double longitude;

  const AddressSearchItem({
    required this.addressName,
    required this.roadAddress,
    required this.jibunAddress,
    required this.latitude,
    required this.longitude,
  });

  factory AddressSearchItem.fromKakaoJson(Map<String, dynamic> json) {
    final road = json['road_address'] as Map<String, dynamic>?;
    final address = json['address'] as Map<String, dynamic>?;

    final roadAddress = road?['address_name'] as String? ?? '';
    final jibunAddress = address?['address_name'] as String? ?? '';
    final display = roadAddress.isNotEmpty ? roadAddress : jibunAddress;

    return AddressSearchItem(
      addressName: display,
      roadAddress: roadAddress,
      jibunAddress: jibunAddress,
      latitude: double.tryParse(json['y']?.toString() ?? '') ?? 0,
      longitude: double.tryParse(json['x']?.toString() ?? '') ?? 0,
    );
  }
}