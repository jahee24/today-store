import 'package:image_picker/image_picker.dart';
import 'permission_service.dart';

class PermissionDeniedException implements Exception {
  final bool openSettingsRecommended;

  PermissionDeniedException({required this.openSettingsRecommended});
}

class ImageService {
  ImageService._();

  static final ImagePicker _picker = ImagePicker();

  static Future<List<XFile>> pickFromGallery({
    required int remainCount,
  }) async {
    if (remainCount <= 0) 
      return [];
    
    final permission = await PermissionService.ensurePhotos();

    if (permission == AppPermissionResult.denied) {
      throw PermissionDeniedException(openSettingsRecommended: true);
    }

    if (permission == AppPermissionResult.permanentlyDenied) {
      throw PermissionDeniedException(openSettingsRecommended: true);
    }

    final files = await _picker.pickMultiImage(
      imageQuality: 85,
      maxWidth: 1600,
    );

    return files.take(remainCount).toList();
  }

  static Future<XFile?> pickFromCamera() async {
    final permission = await PermissionService.ensureCamera();

    if (permission == AppPermissionResult.denied) {
      throw PermissionDeniedException(openSettingsRecommended: true);
    }

    if (permission == AppPermissionResult.permanentlyDenied) {
      throw PermissionDeniedException(openSettingsRecommended: true);
    }

    return _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 85,
      maxWidth: 1600,
    );
  }
}