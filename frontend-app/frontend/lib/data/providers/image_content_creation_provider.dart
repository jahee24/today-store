import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

class ImageContentCreationState {
  final List<XFile> images;

  const ImageContentCreationState({this.images = const []});

  ImageContentCreationState copyWith({List<XFile>? images}) {
    return ImageContentCreationState(
      images: images ?? this.images,
    );
  }
}

class ImageContentCreationNotifier
    extends StateNotifier<ImageContentCreationState> {
  ImageContentCreationNotifier() : super(const ImageContentCreationState());

  void setImages(List<XFile> images) {
    state = state.copyWith(images: List<XFile>.from(images));
  }

  void removeImageAt(int index) {
    if (index < 0 || index >= state.images.length) return;
    final next = List<XFile>.from(state.images)..removeAt(index);
    state = state.copyWith(images: next);
  }

  void reset() {
    state = const ImageContentCreationState();
  }
}

final imageContentCreationProvider = StateNotifierProvider<
    ImageContentCreationNotifier, ImageContentCreationState>(
  (ref) => ImageContentCreationNotifier(),
);
