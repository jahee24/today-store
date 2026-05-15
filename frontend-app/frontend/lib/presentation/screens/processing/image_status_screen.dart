import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../../data/providers/image_content_creation_provider.dart';

/// 이력 화면에서 이미지 베리에이션 항목을 열었을 때 사용하는 상태 확인 화면.
/// - variations 없음(대기 중): 폴링 로딩 표시
/// - variations 있음(완료): 이미지 결과 화면으로 자동 이동
/// - API 에러(실패): 에러 안내 표시
class ImageStatusScreen extends ConsumerStatefulWidget {
  const ImageStatusScreen({
    super.key,
    required this.requestId,
    required this.inputImageId,
  });

  final String requestId;
  final String inputImageId;

  @override
  ConsumerState<ImageStatusScreen> createState() => _ImageStatusScreenState();
}

class _ImageStatusScreenState extends ConsumerState<ImageStatusScreen> {
  _StatusState _status = _StatusState.loading;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _startPolling());
  }

  Future<void> _startPolling() async {
    while (mounted) {
      try {
        final repository = ref.read(contentRepositoryProvider);
        final variations = await repository.getImageVariations(
          inputImageId: widget.inputImageId,
        );

        if (!mounted) return;

        if (variations.isNotEmpty) {
          // 완료 → provider에 저장 후 결과 화면으로 이동
          ref.read(imageContentCreationProvider.notifier).setVariations(variations);
          ref.read(imageContentCreationProvider.notifier).setRequestContext(
                requestId: widget.requestId,
                inputImageId: widget.inputImageId,
              );
          context.pushReplacement('/image-result');
          return;
        }

        // 아직 대기 중 → 3초 후 재시도
        await Future.delayed(const Duration(seconds: 3));
      } catch (e) {
        if (!mounted) return;
        setState(() {
          _status = _StatusState.failed;
          _errorMessage = '이미지 생성에 실패했어요.';
        });
        return;
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppTheme.backgroundColor,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20),
          color: AppTheme.textPrimary,
          onPressed: () {
            if (context.canPop()) {
              context.pop();
            } else {
              context.go('/dashboard');
            }
          },
        ),
        title: Text(
          '이미지 생성 결과',
          style: textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
      ),
      body: SafeArea(
        child: switch (_status) {
          _StatusState.loading => _buildLoadingView(textTheme, h),
          _StatusState.failed => _buildFailedView(textTheme, h),
        },
      ),
    );
  }

  Widget _buildLoadingView(TextTheme textTheme, double Function(double) h) {
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: h(32)),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(),
            SizedBox(height: h(24)),
            Text(
              '이미지 생성 중이에요',
              style: textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: AppTheme.textPrimary,
              ),
            ),
            SizedBox(height: h(10)),
            Text(
              'AI가 이미지를 합성하고 있어요.\n완료되면 자동으로 결과가 표시돼요.',
              textAlign: TextAlign.center,
              style: textTheme.bodyMedium?.copyWith(
                color: AppTheme.textTertiary,
                height: 1.6,
              ),
            ),
            SizedBox(height: h(32)),
            OutlinedButton(
              onPressed: () {
                if (context.canPop()) {
                  context.pop();
                } else {
                  context.go('/dashboard');
                }
              },
              child: const Text('이력으로 돌아가기'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFailedView(TextTheme textTheme, double Function(double) h) {
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: h(32)),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.error_outline_rounded,
              size: h(52),
              color: AppTheme.textTertiary,
            ),
            SizedBox(height: h(20)),
            Text(
              '이미지 생성에 실패했어요',
              style: textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: AppTheme.textPrimary,
              ),
            ),
            SizedBox(height: h(10)),
            Text(
              _errorMessage ?? '알 수 없는 오류가 발생했어요.',
              textAlign: TextAlign.center,
              style: textTheme.bodyMedium?.copyWith(
                color: AppTheme.textTertiary,
                height: 1.6,
              ),
            ),
            SizedBox(height: h(32)),
            FilledButton(
              onPressed: () {
                if (context.canPop()) {
                  context.pop();
                } else {
                  context.go('/dashboard');
                }
              },
              child: const Text('돌아가기'),
            ),
          ],
        ),
      ),
    );
  }
}

enum _StatusState { loading, failed }
