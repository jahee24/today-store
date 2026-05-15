import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gal/gal.dart';
import 'package:go_router/go_router.dart';
import 'package:path_provider/path_provider.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/models/content_model.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../../data/providers/image_content_creation_provider.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';

class ImageResultViewScreen extends ConsumerStatefulWidget {
  const ImageResultViewScreen({super.key});

  @override
  ConsumerState<ImageResultViewScreen> createState() => _ImageResultViewScreenState();
}

class _ImageResultViewScreenState extends ConsumerState<ImageResultViewScreen> {
  bool _isSaving = false;

  void _handleBack(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/dashboard');
    }
  }

  Future<void> _saveAllImages(List<ImageVariationItem> variations) async {
    if (_isSaving || variations.isEmpty) return;

    setState(() => _isSaving = true);
    int successCount = 0;

    try {
      // 1. 권한 확인
      final hasAccess = await Gal.hasAccess();
      if (!hasAccess) {
        final granted = await Gal.requestAccess();
        if (!granted) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('갤러리 접근 권한이 필요합니다.')),
            );
          }
          return;
        }
      }

      final dio = Dio();
      final tempDir = await getTemporaryDirectory();

      for (var i = 0; i < variations.length; i++) {
        final url = variations[i].url;
        final response = await dio.get(
          url,
          options: Options(responseType: ResponseType.bytes),
        );
        
        if (response.data != null) {
          final file = File('${tempDir.path}/save_image_${DateTime.now().millisecondsSinceEpoch}_$i.jpg');
          await file.writeAsBytes(response.data as List<int>);
          await Gal.putImage(file.path);
          successCount++;
        }
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$successCount개의 이미지를 앨범에 저장했어요.')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('이미지 저장 중 오류가 발생했습니다.')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final imageState = ref.watch(imageContentCreationProvider);
    final variations = imageState.variations;
    final contentId = GoRouterState.of(context).uri.queryParameters['contentId'] ?? '';

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.fromLTRB(h(20), h(16), h(20), h(20)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BackArrowButton(onTap: () => _handleBack(context)),
                  SizedBox(width: h(12)),
                  Text(
                    '이미지 생성 결과',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  const Spacer(),
                  TextButton(
                    onPressed: imageState.requestId == null
                        ? null
                        : () async {
                            final requestId = imageState.requestId;
                            if (requestId == null || requestId.isEmpty) return;
                            final shouldDelete =
                                await showDialog<bool>(
                                  context: context,
                                  builder: (_) => AlertDialog(
                                    title: const Text('생성 이력 삭제'),
                                    content: const Text(
                                      '이 이미지 생성 이력을 삭제할까요?',
                                    ),
                                    actions: [
                                      TextButton(
                                        onPressed: () => Navigator.of(context).pop(false),
                                        child: const Text('취소'),
                                      ),
                                      TextButton(
                                        onPressed: () => Navigator.of(context).pop(true),
                                        child: const Text(
                                          '삭제',
                                          style: TextStyle(color: AppTheme.dangerText),
                                        ),
                                      ),
                                    ],
                                  ),
                                ) ??
                                false;
                            if (!shouldDelete) return;

                            try {
                              final repository = ref.read(contentRepositoryProvider);
                              if (contentId.trim().isNotEmpty) {
                                await repository.deleteContent(contentId: contentId.trim());
                              } else {
                                await repository.deleteGenerationRequest(requestId: requestId);
                              }
                              ref.read(imageContentCreationProvider.notifier).reset();
                              ref.invalidate(dashboardDataProvider);
                              if (context.mounted) {
                                context.go('/history');
                              }
                            } catch (_) {
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('삭제 중 오류가 발생했어요.')),
                                );
                              }
                            }
                          },
                    child: Text(
                      '삭제',
                      style: textTheme.titleSmall?.copyWith(
                        color: AppTheme.dangerText,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(8)),
              Text(
                variations.isEmpty
                    ? '아직 생성된 이미지가 없어요.'
                    : '다양한 구도로 변환된 매장 사진이에요.',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              SizedBox(height: h(18)),
              Expanded(
                child: variations.isEmpty
                    ? const Center(
                        child: Text(
                          '생성된 이미지가 없습니다',
                          style: TextStyle(color: AppTheme.textTertiary),
                        ),
                      )
                    : GridView.builder(
                        gridDelegate:
                            SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          mainAxisSpacing: h(12),
                          crossAxisSpacing: h(12),
                          childAspectRatio: 0.92,
                        ),
                        itemCount: variations.length,
                        itemBuilder: (context, index) {
                          final item = variations[index];
                          return ClipRRect(
                            borderRadius: BorderRadius.circular(20),
                            child: Stack(
                              fit: StackFit.expand,
                              children: [
                                Image.network(
                                  item.url,
                                  fit: BoxFit.cover,
                                  errorBuilder: (_, __, ___) => Container(
                                    color: const Color(0xFFE8DEF8),
                                    alignment: Alignment.center,
                                    child: const Icon(
                                      Icons.broken_image_outlined,
                                      size: 42,
                                      color: AppTheme.textSecondary,
                                    ),
                                  ),
                                ),
                                Positioned(
                                  left: 8,
                                  right: 8,
                                  bottom: 8,
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 6,
                                    ),
                                    decoration: BoxDecoration(
                                      color: Colors.black.withOpacity(0.45),
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Text(
                                      item.angleType,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
              ),
              SizedBox(height: h(10)),
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Expanded(
                    flex: 3,
                    child: SizedBox(
                      height: h(62),
                      child: OutlinedButton(
                        onPressed: () => context.go('/processing?mode=image'),
                        style: OutlinedButton.styleFrom(
                          minimumSize: Size.zero,
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          padding: EdgeInsets.symmetric(horizontal: h(8)),
                          side: const BorderSide(
                            color: AppTheme.primaryColor,
                            width: 2,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(20),
                          ),
                          backgroundColor: AppTheme.surfaceColor,
                        ),
                        child: Text(
                          '재생성',
                          style: textTheme.titleMedium?.copyWith(
                            fontSize: f(18),
                            fontWeight: FontWeight.w700,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: h(12)),
                  Expanded(
                    flex: 6,
                    child: PrimaryButton(
                      text: '앨범에 저장',
                      height: 62,
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      isLoading: _isSaving,
                      onPressed: variations.isEmpty ? null : () => _saveAllImages(variations),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
