import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_naver_map/flutter_naver_map.dart';
import 'package:geolocator/geolocator.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/datasources/remote/naver_local_api.dart';
import '../../../services/permission_service.dart';
import '../../models/address_pick_result.dart';
import '../../widgets/buttons/primary_button.dart';

class MapLocationPickerScreen extends StatefulWidget {
  const MapLocationPickerScreen({super.key});

  @override
  State<MapLocationPickerScreen> createState() =>
      _MapLocationPickerScreenState();
}

class _MapLocationPickerScreenState extends State<MapLocationPickerScreen> {
  NaverMapController? _mapController;
  late final NaverLocalApi _naverLocalApi;

  double _latitude = 37.5666;
  double _longitude = 126.9790;

  String _roadAddress = '주소를 불러오는 중...';
  String _jibunAddress = '';

  bool _isLoading = true;
  bool _isAddressLoading = false;
  bool _isSubmitting = false;
  bool _isPermissionDenied = false;
  bool _isAddressResolved = false;

  Timer? _cameraIdleDebounce;

  @override
  void initState() {
    super.initState();

    _naverLocalApi = NaverLocalApi(
      clientId: AppKeys.naverMapClientId,
      clientSecret: AppKeys.naverMapClientSecret,
    );

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initializeCurrentLocation();
    });
  }

  @override
  void dispose() {
    _cameraIdleDebounce?.cancel();
    super.dispose();
  }

  Future<void> _initializeCurrentLocation() async {
    final permission = await PermissionService.ensureLocation();

    if (!mounted) return;

    if (permission != AppPermissionResult.granted) {
      setState(() {
        _isLoading = false;
        _isPermissionDenied = true;
        _roadAddress = '위치 권한이 필요합니다.';
        _jibunAddress = '현재 위치로 주소를 찾으려면 위치 권한을 허용해주세요.';
      });

      await _showLocationPermissionDialog(permission);
      return;
    }

    await _moveToCurrentLocation(showErrorDialog: true);
  }

  Future<void> _moveToCurrentLocation({
    bool showErrorDialog = false,
  }) async {
    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();

      if (!serviceEnabled) {
        if (!mounted) return;

        setState(() {
          _isLoading = false;
          _roadAddress = '위치 서비스를 켜주세요.';
          _jibunAddress = '기기의 위치 서비스가 꺼져 있습니다.';
        });

        if (showErrorDialog) {
          await _showLocationServiceDialog();
        }
        return;
      }

      final permission = await PermissionService.ensureLocation();

      if (!mounted) return;

      if (permission != AppPermissionResult.granted) {
        setState(() {
          _isLoading = false;
          _isPermissionDenied = true;
          _roadAddress = '위치 권한이 필요합니다.';
          _jibunAddress = '현재 위치로 주소를 찾으려면 위치 권한을 허용해주세요.';
        });

        await _showLocationPermissionDialog(permission);
        return;
      }

      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );

      if (!mounted) return;

      setState(() {
        _latitude = position.latitude;
        _longitude = position.longitude;
        _isLoading = false;
        _isPermissionDenied = false;
      });

      await _moveCamera(_latitude, _longitude);
      await _updateAddressFromLatLng(_latitude, _longitude);
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _isLoading = false;
        _roadAddress = '현재 위치를 가져오지 못했어요.';
        _jibunAddress = '잠시 후 다시 시도해주세요.';
      });

      debugPrint('현재 위치 가져오기 실패: $e');
    }
  }

  Future<void> _moveCamera(double lat, double lng) async {
    final controller = _mapController;
    if (controller == null) return;

    await controller.updateCamera(
      NCameraUpdate.scrollAndZoomTo(
        target: NLatLng(lat, lng),
        zoom: 16,
      ),
    );
  }

  void _handleCameraIdle() {
    if (_mapController == null) return;

    _cameraIdleDebounce?.cancel();

    _cameraIdleDebounce = Timer(const Duration(milliseconds: 350), () async {
      final controller = _mapController;
      if (controller == null) return;

      final cameraPosition = await controller.getCameraPosition();

      if (!mounted) return;

      final lat = cameraPosition.target.latitude;
      final lng = cameraPosition.target.longitude;

      setState(() {
        _latitude = lat;
        _longitude = lng;
      });

      await _updateAddressFromLatLng(lat, lng);
    });
  }

  Future<void> _updateAddressFromLatLng(double lat, double lng) async {
    if (!mounted) return;

    setState(() {
      _isAddressLoading = true;
      _isAddressResolved = false;
    });

    try {
      if (AppKeys.naverMapClientId.isEmpty ||
          AppKeys.naverMapClientSecret.isEmpty) {
        throw Exception('Naver API key is missing');
      }

      final result = await _naverLocalApi.coordToAddress(
        latitude: lat,
        longitude: lng,
      );

      if (!mounted) return;

      setState(() {
        _roadAddress = result.displayAddress.isNotEmpty
            ? result.displayAddress
            : '선택한 위치';
        _jibunAddress = result.jibunAddress ?? '';
        _isAddressLoading = false;
        _isAddressResolved = result.displayAddress.trim().isNotEmpty;
      });
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _roadAddress = '주소를 불러오지 못했어요';
        _jibunAddress = '네트워크 또는 API 설정을 확인한 뒤 다시 시도해주세요.';
        _isAddressLoading = false;
        _isAddressResolved = false;
      });

      debugPrint('좌표 주소 변환 실패: $e');
    }
  }

  void _confirmLocation() {
    if (_isPermissionDenied || _isSubmitting || _isAddressLoading || !_isAddressResolved) return;

    setState(() {
      _isSubmitting = true;
    });

    context.pop(
      AddressPickResult(
        roadAddress: _roadAddress,
        jibunAddress: _jibunAddress.isNotEmpty ? _jibunAddress : null,
        latitude: _latitude,
        longitude: _longitude,
      ),
    );
  }

  Future<void> _showLocationPermissionDialog(
    AppPermissionResult result,
  ) async {
    if (!mounted) return;

    final isPermanentlyDenied =
        result == AppPermissionResult.permanentlyDenied;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('위치 권한이 필요해요'),
          content: Text(
            isPermanentlyDenied
                ? '현재 위치를 가져오려면 설정에서 위치 권한을 허용해주세요.'
                : '현재 위치를 기준으로 가게 주소를 찾기 위해 위치 권한이 필요합니다.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('닫기'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();

                if (isPermanentlyDenied) {
                  Geolocator.openAppSettings();
                } else {
                  _initializeCurrentLocation();
                }
              },
              child: Text(isPermanentlyDenied ? '설정 열기' : '다시 허용'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _showLocationServiceDialog() async {
    if (!mounted) return;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('위치 서비스가 꺼져 있어요'),
          content: const Text('현재 위치를 가져오려면 기기의 위치 서비스를 켜주세요.'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('확인'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();
                Geolocator.openLocationSettings();
              },
              child: const Text('설정 열기'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    return Scaffold(
      backgroundColor: AppTheme.surfaceColor,
      body: SafeArea(
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : Stack(
                children: [
                  Positioned.fill(
                    child: NaverMap(
                      options: NaverMapViewOptions(
                        initialCameraPosition: NCameraPosition(
                          target: NLatLng(_latitude, _longitude),
                          zoom: 16,
                        ),
                        locationButtonEnable: false,
                        scrollGesturesEnable: true,
                        zoomGesturesEnable: true,
                        tiltGesturesEnable: true,
                      ),
                      onMapReady: (controller) async {
                        _mapController = controller;
                        await _moveCamera(_latitude, _longitude);
                      },
                      onCameraIdle: _handleCameraIdle,
                    ),
                  ),

                  Positioned(
                    left: 0,
                    right: 0,
                    top: 0,
                    child: Container(
                      height: h(66),
                      color: AppTheme.surfaceColor,
                      child: Stack(
                        alignment: Alignment.center,
                        children: [
                          Positioned(
                            left: 12,
                            child: IconButton(
                              onPressed: () => context.pop(),
                              icon: const Icon(
                                Icons.arrow_back_ios_new_rounded,
                                color: AppTheme.textPrimary,
                              ),
                            ),
                          ),
                          const Text(
                            '지도에서 위치 확인',
                            style: TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w700,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                  Center(
                    child: Padding(
                      padding: EdgeInsets.only(bottom: h(38)),
                      child: Icon(
                        Icons.location_on,
                        size: h(52),
                        color: Color(0xFF333333),
                      ),
                    ),
                  ),

                  Positioned(
                    right: h(16),
                    bottom: h(214),
                    child: GestureDetector(
                      onTap: () => _moveToCurrentLocation(
                        showErrorDialog: true,
                      ),
                      child: Container(
                        width: h(54),
                        height: h(54),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          shape: BoxShape.circle,
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.12),
                              blurRadius: 16,
                              offset: const Offset(0, 6),
                            ),
                          ],
                        ),
                        child: const Icon(
                          Icons.my_location_rounded,
                          color: AppTheme.textPrimary,
                          size: 30,
                        ),
                      ),
                    ),
                  ),

                  Positioned(
                    left: 0,
                    right: 0,
                    bottom: 0,
                    child: Container(
                      padding: EdgeInsets.fromLTRB(h(20), h(20), h(20), h(16)),
                      decoration: const BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.vertical(
                          top: Radius.circular(28),
                        ),
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (_isAddressLoading)
                            const LinearProgressIndicator(minHeight: 2),
                          if (_isAddressLoading) SizedBox(height: h(12)),
                          Text(
                            _roadAddress,
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w800,
                              color: AppTheme.textPrimary,
                              height: 1.3,
                            ),
                          ),
                          SizedBox(height: h(8)),
                          Text(
                            _jibunAddress,
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w500,
                              color: AppTheme.textSecondary,
                              height: 1.4,
                            ),
                          ),
                          SizedBox(height: h(14)),
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 16,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFFF1F1),
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: const Text(
                              '지도의 표시와 실제 주소가 맞는지 확인해주세요.',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w700,
                                color: Color(0xFFFF5A5A),
                              ),
                            ),
                          ),
                          SizedBox(height: h(14)),
                          if (!_isAddressResolved) ...[
                            TextButton(
                              onPressed: _isAddressLoading
                                  ? null
                                  : () => _updateAddressFromLatLng(_latitude, _longitude),
                              child: const Text('주소 다시 확인'),
                            ),
                            SizedBox(height: h(6)),
                          ],
                          PrimaryButton(
                            text: '이 위치로 주소 등록',
                            onPressed: _isPermissionDenied || !_isAddressResolved
                                ? null
                                : _confirmLocation,
                            isLoading: _isSubmitting,
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}