class AddressPickResult {
  final String roadAddress;
  final String? jibunAddress;
  final double latitude;
  final double longitude;

  const AddressPickResult({
    required this.roadAddress,
    this.jibunAddress,
    required this.latitude,
    required this.longitude,
  });

  String get displayAddress => 
  roadAddress.trim().isNotEmpty ? roadAddress : (jibunAddress ?? '');
}