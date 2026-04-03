import 'package:flutter/material.dart';
import 'package:flutter_naver_map/flutter_naver_map.dart';

class MapLocationPickerScreen extends StatelessWidget {
  const MapLocationPickerScreen({super.key});

  @override
  Widget build(BuildContext context) {
    const initialPosition = NLatLng(37.5666, 126.9790);
    
    return Scaffold(
      appBar: AppBar(
        title: const Text('지도에서 위치 선택'),
      ),
      body: NaverMap(
        options: const NaverMapViewOptions(
          initialCameraPosition: NCameraPosition(
            target: initialPosition, 
            zoom: 14,
          ),
        ),
        onMapReady: (controller) {
          debugPrint('네이버 지도 준비 완료');
        },
      ),
    );
  }
}