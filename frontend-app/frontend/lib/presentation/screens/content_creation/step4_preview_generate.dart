import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../data/providers/content_creation_provider.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';
import '../../widgets/progress/step_indicator_line.dart';

class Step4PreviewGenerate extends ConsumerWidget {
  const Step4PreviewGenerate({super.key});

  void _handleBack(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/step3');
    }
  }

  void _handleGenerate(BuildContext context) {
    context.push('/processing?mode=text');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final contentState = ref.watch(contentCreationProvider);

    final images = contentState.images;
    final descriptions = contentState.descriptions;
    final selectedStyle = contentState.selectedStyle ?? '감성적';
    final extraRequest = contentState.extraRequest.trim();

    final isDescriptionComplete = images.isNotEmpty &&
    descriptions.length >= images.length &&
    descriptions.take(images.length).every((e) => e.trim().isNotEmpty);

    final styleMeta = _styleMetaMap[selectedStyle] ?? _styleMetaMap['감성적']!;

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 18, 24, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BackArrowButton(onTap: () => _handleBack(context)),
                  const SizedBox(width: 14),
                  Text(
                    '콘텐츠 만들기',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 26),

              const StepIndicatorLine(currentStep: 4, totalSteps: 4),

              const SizedBox(height: 34),
              
              Text(
                '모든 준비가 완료됐어요!',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '설정을 확인하고 생성을 시작하세요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),

              const SizedBox(height: 24),

              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      _PreviewSummaryCard(
                        images: images,
                        imageCount: images.length,
                        isDescriptionComplete: isDescriptionComplete,
                        styleLabel: selectedStyle,
                        styleEmoji: styleMeta.emoji,
                        extraRequest: extraRequest,
                      ),
                      const SizedBox(height: 18),

                      _CreditNoticeCard(
                        remainingCreditText: '남은 크레딧: 4회 (Free 플랜)',
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 20),

              PrimaryButton(
                text: 'AI 콘텐츠 생성하기',
                prefixIcon: const Text(
                  '✨',
                  style: TextStyle(fontSize: 22),
                ),
                onPressed: images.isEmpty ? null : () => _handleGenerate(context),
              ),
              const SizedBox(height: 14),

              Center(
                child: Text(
                  '생성에는 약 30초~1분이 소요됩니다',
                  style: textTheme.bodyMedium?.copyWith(
                    fontSize: 14,
                    color: AppTheme.textTertiary,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PreviewSummaryCard extends StatelessWidget {
  final List<dynamic> images;
  final int imageCount;
  final bool isDescriptionComplete;
  final String styleLabel;
  final String styleEmoji;
  final String extraRequest;

  const _PreviewSummaryCard({
    required this.images,
    required this.imageCount,
    required this.isDescriptionComplete,
    required this.styleLabel,
    required this.styleEmoji,
    required this.extraRequest,
  });

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: AppTheme.borderColor),
        boxShadow: const [
          BoxShadow(
            color: AppTheme.shadowColor,
            blurRadius: 12,
            offset: Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        children: [
          ClipRRect(
            borderRadius: const BorderRadiusGeometry.vertical(top: Radius.circular(24)),
            child: SizedBox(
              height: 180,
            child: Row(
              children: [
                Expanded(
                  child: _PreviewImageTile(
                    imagePath: images.isNotEmpty ? images[0].path : null,
                    fallbackColor: const Color(0xFFF4E3E3),
                  ),
                ),
                Expanded(
                  child: _PreviewImageTile(
                    imagePath: images.length > 1 ? images[1].path : null,
                    fallbackColor: const Color(0xFFE4F0EC),
                  ),
                ),
              ],
            ),
          ),
      ),

      Padding(
        padding: const EdgeInsets.fromLTRB(22, 16, 22, 22),
        child: Column(
          children: [
            _SummaryRow(
              label: '업로드 사진',
              valueWidget: Text(
                '${imageCount}장',
                style: textTheme.titleLarge?.copyWith(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
            ),
            const SizedBox(height: 14),
            const Divider(height: 1, color: AppTheme.dividerColor),
            const SizedBox(height: 14),

            _SummaryRow(
              label: '사진 설명',
              valueWidget: Text(
                isDescriptionComplete ? '입력 완료 ✓' : '입력 미완료',
                style: textTheme.titleMedium?.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: isDescriptionComplete
                  ? AppTheme.successText
                  : AppTheme.dangerText,
                ),
              ),
            ),
            const SizedBox(height: 14),
            const Divider(height: 1, color: AppTheme.dividerColor),
            const SizedBox(height: 14),

            _SummaryRow(
              label: '스타일',
              valueWidget: Text(
                '$styleEmoji $styleLabel',
                style: textTheme.titleMedium?.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: AppTheme.primaryColor,
                ),
              ),
            ),
            const SizedBox(height: 14),
            const Divider(height: 1, color: AppTheme.dividerColor),
            const SizedBox(height: 14),

            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                '추가 요청사항',
                style: textTheme.titleMedium?.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textSecondary,
                ),
              ),
            ),
            const SizedBox(height: 12),

            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(
                horizontal: 18,
                vertical: 16,
              ),
              decoration: BoxDecoration(
                color: AppTheme.fillLighter,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Text(
                extraRequest.isEmpty ? '없음' : extraRequest,
                style: textTheme.bodyLarge?.copyWith(
                  fontSize: 16,
                  color: extraRequest.isEmpty
                  ? AppTheme.textTertiary
                  : AppTheme.textSecondary,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ],
        ),
      ),
        ],
      ),
    );
  }
}

class _PreviewImageTile extends StatelessWidget {
  final String? imagePath;
  final Color fallbackColor;

  const _PreviewImageTile({
    required this.imagePath,
    required this.fallbackColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: fallbackColor,
      child: Center(
        child: imagePath != null
            ? ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: Image.file(
                  File(imagePath!),
                  width: 84,
                  height: 84,
                  fit: BoxFit.cover,
                ),
              )
            : const Text(
                '📷',
                style: TextStyle(fontSize: 46),
              ),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  final String label;
  final Widget valueWidget;

  const _SummaryRow({
    required this.label,
    required this.valueWidget,
  });

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Row(
      children: [
        Text(
          label,
          style: textTheme.titleMedium?.copyWith(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: AppTheme.textSecondary,
          ),
        ),
        const Spacer(),
        valueWidget,
      ],
    );
  }
}

class _CreditNoticeCard extends StatelessWidget {
  final String remainingCreditText;

  const _CreditNoticeCard({
    required this.remainingCreditText,
  });

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: 22,
        vertical: 20,
      ),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEA),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: const Color(0xFFE8D85E),
          width: 1.5,
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '✨',
            style: TextStyle(fontSize: 28),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '크레딧 1회 차감',
                  style: textTheme.titleLarge?.copyWith(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.warningText,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  remainingCreditText,
                  style: textTheme.bodyLarge?.copyWith(
                    fontSize: 15,
                    color: AppTheme.textSecondary,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StyleMeta {
  final String emoji;

  const _StyleMeta({
    required this.emoji,
  });
}

const Map<String, _StyleMeta> _styleMetaMap = {
  '감성적': _StyleMeta(emoji: '💜'),
  '정보제공': _StyleMeta(emoji: '📋'),
  '전문성': _StyleMeta(emoji: '🏅'),
  '친근': _StyleMeta(emoji: '😊'),
};