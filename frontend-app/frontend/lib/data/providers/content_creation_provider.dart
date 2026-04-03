import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

class ContentCreationState {
  final List<XFile> images;
  final List<String> descriptions;
  final String extraRequest;
  final String? selectedStyle;

  const ContentCreationState({
    this.images = const [],
    this.descriptions = const [],
    this.extraRequest = '',
    this.selectedStyle,
  });

  ContentCreationState copyWith({
    List<XFile>? images,
    List<String>? descriptions,
    String? extraRequest,
    String? selectedStyle,
    bool clearSelectedStyle = false,
  }) {
    return ContentCreationState(
      images: images ?? this.images,
      descriptions: descriptions ?? this.descriptions,
      extraRequest: extraRequest ?? this.extraRequest,
      selectedStyle: clearSelectedStyle
          ? null
          : (selectedStyle ?? this.selectedStyle),
    );
  }
}

class ContentCreationNotifier extends StateNotifier<ContentCreationState> {
  ContentCreationNotifier() : super(const ContentCreationState());

  void setImages(List<XFile> images) {
    final currentDescriptions = List<String>.from(state.descriptions);

    if (currentDescriptions.length < images.length) {
      currentDescriptions.addAll(
        List.filled(images.length - currentDescriptions.length, ''),
      );
    } else if (currentDescriptions.length > images.length) {
      currentDescriptions.removeRange(images.length, currentDescriptions.length);
    }

    state = state.copyWith(
      images: List<XFile>.from(images),
      descriptions: currentDescriptions,
    );
  }

  void removeImageAt(int index) {
    if (index < 0 || index >= state.images.length) return;

    final updatedImages = List<XFile>.from(state.images)..removeAt(index);
    final updatedDescriptions = List<String>.from(state.descriptions);

    if (index < updatedDescriptions.length) {
      updatedDescriptions.removeAt(index);
    }

    state = state.copyWith(
      images: updatedImages,
      descriptions: updatedDescriptions,
    );
  }

  void setDescription(int index, String value) {
    final updated = List<String>.from(state.descriptions);

    if (index >= updated.length) {
      updated.addAll(List.filled(index - updated.length + 1, ''));
    }

    updated[index] = value;

    state = state.copyWith(descriptions: updated);
  }

  void setExtraRequest(String value) {
    state = state.copyWith(extraRequest: value);
  }

  void setSelectedStyle(String style) {
    state = state.copyWith(selectedStyle: style);
  }

  void clearSelectedStyle() {
    state = state.copyWith(clearSelectedStyle: true);
  }

  void reset() {
    state = const ContentCreationState();
  }
}

final contentCreationProvider =
    StateNotifierProvider<ContentCreationNotifier, ContentCreationState>(
  (ref) => ContentCreationNotifier(),
);