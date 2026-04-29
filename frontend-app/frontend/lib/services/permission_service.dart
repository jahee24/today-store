import 'dart:io';

import 'package:permission_handler/permission_handler.dart';

enum AppPermissionType {
  camera,
  photos,
  location,
}

enum AppPermissionResult {
  granted,
  denied,
  permanentlyDenied,
}

class PermissionService {
  PermissionService._();

  static Future<AppPermissionResult> ensure(AppPermissionType type) async {
    final permission = _mapPermission(type);

    if (permission == null) {
      return AppPermissionResult.granted;
    }

    final status = await permission.status;

    // 이미 허용된 상태
    if (status.isGranted || status.isLimited) {
      return AppPermissionResult.granted;
    }
    
    // 설정 화면 유도가 필요한 상태
    if (status.isPermanentlyDenied || status.isRestricted) {
      return AppPermissionResult.permanentlyDenied;
    }

    if (Platform.isAndroid) {
      final shouldShowRationale = await permission.shouldShowRequestRationale;
      if (shouldShowRationale) {
        return AppPermissionResult.denied;
      }
    }

    // 처음 요청하는 경우 시스템 권한 팝업 띄움
    final result = await permission.request();

    if (result.isGranted || result.isLimited) {
      return AppPermissionResult.granted;
    }

    if (result.isPermanentlyDenied || result.isRestricted) {
      return AppPermissionResult.permanentlyDenied;
    }

    return AppPermissionResult.denied;
  }

  static Future<AppPermissionResult> ensureCamera() async {
    return ensure(AppPermissionType.camera);
  }

  static Future<AppPermissionResult> ensurePhotos() async {
    return ensure(AppPermissionType.photos);
  }

  static Future<AppPermissionResult> ensureLocation() async {
    return ensure(AppPermissionType.location);
  }

  static Permission? _mapPermission(AppPermissionType type) {
    switch (type) {
      case AppPermissionType.camera:
      return Permission.camera;

      case AppPermissionType.photos:
      if (Platform.isIOS) {
        return Permission.photos;
      }
      if (Platform.isAndroid) {
        return Permission.photos;
      }
      return null;

      case AppPermissionType.location:
      return Permission.locationWhenInUse;
    }
  }
}