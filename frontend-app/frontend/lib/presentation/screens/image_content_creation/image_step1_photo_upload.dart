import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/providers/image_content_creation_provider.dart';
import '../../../services/image_service.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';
import '../../widgets/cards/dotted_lined_card.dart';
import '../../widgets/dialogs/permission_alert_dialog.dart';
import '../../widgets/progress/step_indicator_line.dart';

class ImageStep1PhotoUpload extends ConsumerStatefulWidget {
  const ImageStep1PhotoUpload({super.key});

  @override
  ConsumerState<ImageStep1PhotoUpload> createState() =>
      _ImageStep1PhotoUploadState();
}

class _ImageStep1PhotoUploadState extends ConsumerState<ImageStep1PhotoUpload> {
  static const int _maxImages = 8;

  final List<XFile> _images = [];

  Future<void> _openSourceSheet() async {
    if (_images.length >= _maxImages) {
      _showSnackBar('사진은 최대 $_maxImages장까지 업로드할 수 있어요.');
      return;
    }

    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 44,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppTheme.borderColor,
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
                const SizedBox(height: 14),
                Text(
                  '사진 추가',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                ),
                const SizedBox(height: 18),
                _SourceTile(
                  icon: Icons.photo_library_outlined,
                  title: '앨범에서 선택',
                  onTap: () async {
                    Navigator.pop(context);
                    await _pickFromGallery();
                  },
                ),
                const SizedBox(height: 12),
                _SourceTile(
                  icon: Icons.photo_camera_outlined,
                  title: '카메라로 촬영',
                  onTap: () async {
                    Navigator.pop(context);
                    await _pickFromCamera();
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _pickFromGallery() async {
    try {
      final remainCount = _maxImages - _images.length;
      final picked = await ImageService.pickFromGallery(
        remainCount: remainCount,
      );
      if (picked.isEmpty) return;
      setState(() => _images.addAll(picked));
    } on PermissionDeniedException {
      await _showPermissionDialog(
        title: '사진에 대한 액세스\n권한이 없어요.',
        description: '설정 앱에서 권한을 수정할 수 있어요.',
      );
    } catch (_) {
      _showSnackBar('사진을 불러오는 중 문제가 발생했어요.');
    }
  }

  Future<void> _pickFromCamera() async {
    try {
      if (_images.length >= _maxImages) {
        _showSnackBar('사진은 최대 $_maxImages장까지 업로드할 수 있어요.');
        return;
      }
      final picked = await ImageService.pickFromCamera();
      if (picked == null) return;
      setState(() => _images.add(picked));
    } on PermissionDeniedException {
      await _showPermissionDialog(
        title: '카메라에 대한 액세스\n권한이 없어요.',
        description: '설정 앱에서 권한을 수정할 수 있어요.',
      );
    } catch (_) {
      _showSnackBar('카메라를 여는 중 문제가 발생했어요.');
    }
  }

  @override
  void initState() {
    super.initState();
    _images.addAll(ref.read(imageContentCreationProvider).images);
  }

  void _removeImage(int index) {
    if (index < 0 || index >= _images.length) return;
    setState(() => _images.removeAt(index));
  }

  void _handleNext() {
    if (_images.isEmpty) {
      _showSnackBar('사진을 1장 이상 추가해주세요.');
      return;
    }
    ref.read(imageContentCreationProvider.notifier).setImages(_images);
    context.push('/processing?mode=image');
  }

  void _showSnackBar(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  Future<void> _showPermissionDialog({
    required String title,
    required String description,
  }) async {
    if (!mounted) return;
    await PermissionAlertDialog.show(
      context,
      title: title,
      description: description,
    );
  }

  void _handleBack() {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/dashboard');
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final horizontalPadding = AppLayout.h(context, 20);
    final uploadBoxHeight = AppLayout.h(context, 250);
    final thumbnailSize = AppLayout.h(context, 96);
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.fromLTRB(
            horizontalPadding,
            h(16),
            horizontalPadding,
            h(18),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BackArrowButton(onTap: _handleBack),
                  SizedBox(width: h(12)),
                  Text(
                    '이미지 콘텐츠 만들기',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(22)),
              const StepIndicatorLine(currentStep: 1, totalSteps: 2),
              SizedBox(height: h(28)),
              Text(
                '매장 사진을 올려주세요',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
              SizedBox(height: h(8)),
              Text(
                '최대 $_maxImages장까지 업로드할 수 있어요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              SizedBox(height: h(20)),
              SizedBox(
                height: uploadBoxHeight,
                child: DottedLinedCard(onTap: _openSourceSheet),
              ),
              SizedBox(height: h(14)),
              Wrap(
                spacing: h(10),
                runSpacing: h(10),
                children: [
                  ...List.generate(_images.length, (index) {
                    return _ImageThumbnailCard(
                      imagePath: _images[index].path,
                      size: thumbnailSize,
                      onRemove: () => _removeImage(index),
                    );
                  }),
                  if (_images.length < _maxImages)
                    _AddThumbnailCard(
                      size: thumbnailSize,
                      onTap: _openSourceSheet,
                    ),
                ],
              ),
              SizedBox(height: h(10)),
              Text(
                '${_images.length}/$_maxImages장 업로드됨',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const Spacer(),
              PrimaryButton(
                text: '생성 시작 →',
                fontSize: f(21),
                onPressed: _images.isEmpty ? null : _handleNext,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ImageThumbnailCard extends StatelessWidget {
  const _ImageThumbnailCard({
    required this.imagePath,
    required this.size,
    required this.onRemove,
  });

  final String imagePath;
  final double size;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            color: AppTheme.fillLight,
            borderRadius: BorderRadius.circular(18),
            image: DecorationImage(
              image: FileImage(File(imagePath)),
              fit: BoxFit.cover,
            ),
          ),
        ),
        Positioned(
          top: -8,
          right: -8,
          child: InkWell(
            onTap: onRemove,
            borderRadius: BorderRadius.circular(999),
            child: Container(
              width: 28,
              height: 28,
              decoration: const BoxDecoration(
                color: AppTheme.dangerText,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.close, size: 16, color: Colors.white),
            ),
          ),
        ),
      ],
    );
  }
}

class _AddThumbnailCard extends StatelessWidget {
  const _AddThumbnailCard({required this.size, required this.onTap});

  final double size;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: AppTheme.borderStrongColor, width: 2),
        ),
        child: const Icon(Icons.add, size: 34, color: AppTheme.textHint),
      ),
    );
  }
}

class _SourceTile extends StatelessWidget {
  const _SourceTile({
    required this.icon,
    required this.title,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 18),
        decoration: BoxDecoration(
          color: AppTheme.fillLighter,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: AppTheme.borderColor),
        ),
        child: Row(
          children: [
            Icon(icon, color: AppTheme.textPrimary),
            const SizedBox(width: 12),
            Text(
              title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontSize: 16,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}
