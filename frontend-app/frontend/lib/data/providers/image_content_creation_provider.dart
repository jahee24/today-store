import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../models/content_model.dart';

class ImageContentCreationState {
  final List<XFile> images;
  final String? requestId;
  final String? inputImageId;
  final List<ImageVariationItem> variations;

  const ImageContentCreationState({
    this.images = const [],
    this.requestId,
    this.inputImageId,
    this.variations = const [],
  });

  ImageContentCreationState copyWith({
    List<XFile>? images,
    String? requestId,
    String? inputImageId,
    List<ImageVariationItem>? variations,
    bool clearRequestId = false,
    bool clearInputImageId = false,
    bool clearVariations = false,
  }) {
    return ImageContentCreationState(
      images: images ?? this.images,
      requestId: clearRequestId ? null : (requestId ?? this.requestId),
      inputImageId: clearInputImageId ? null : (inputImageId ?? this.inputImageId),
      variations: clearVariations ? const [] : (variations ?? this.variations),
    );
  }
}

class ImageContentCreationNotifier
    extends StateNotifier<ImageContentCreationState> {
  ImageContentCreationNotifier() : super(const ImageContentCreationState());

  void setImages(List<XFile> images) {
    state = state.copyWith(
      images: List<XFile>.from(images),
      clearVariations: true,
      clearInputImageId: true,
      clearRequestId: true,
    );
  }

  void removeImageAt(int index) {
    if (index < 0 || index >= state.images.length) return;
    final next = List<XFile>.from(state.images)..removeAt(index);
    state = state.copyWith(images: next);
  }

  void reset() {
    state = const ImageContentCreationState();
  }

  void setRequestContext({
    required String requestId,
    required String inputImageId,
  }) {
    state = state.copyWith(
      requestId: requestId,
      inputImageId: inputImageId,
    );
  }

  void setVariations(List<ImageVariationItem> variations) {
    state = state.copyWith(variations: List<ImageVariationItem>.from(variations));
  }
}

final imageContentCreationProvider = StateNotifierProvider<
    ImageContentCreationNotifier, ImageContentCreationState>(
  (ref) => ImageContentCreationNotifier(),
);
