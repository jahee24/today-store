import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
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
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
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
          padding: EdgeInsets.fromLTRB(h(20), h(16), h(20), h(18)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BackArrowButton(onTap: () => _handleBack(context)),
                  SizedBox(width: h(12)),
                  Text(
                    '콘텐츠 만들기',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(22)),

              const StepIndicatorLine(currentStep: 4, totalSteps: 4),

              SizedBox(height: h(28)),
              
              Text(
                '모든 준비가 완료됐어요!',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
              SizedBox(height: h(8)),
              Text(
                '설정을 확인하고 생성을 시작하세요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),

              SizedBox(height: h(20)),

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
                    ],
                  ),
                ),
              ),

              SizedBox(height: h(18)),

              PrimaryButton(
                text: 'AI 콘텐츠 생성하기',
                prefixIcon: Text(
                  '✨',
                  style: TextStyle(fontSize: f(22)),
                ),
                onPressed: images.isEmpty ? null : () => _handleGenerate(context),
              ),
              SizedBox(height: h(12)),

              Center(
                child: Text(
                  '생성에는 약 30초~1분이 소요됩니다',
                  style: textTheme.bodyMedium?.copyWith(
                    fontSize: f(14),
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
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(h(22)),
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
            borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
            child: SizedBox(
              height: h(170),
              width: double.infinity,
              child: images.length <= 1
                  ? _PreviewImageTile(
                      imagePath: images.isNotEmpty ? images[0].path : null,
                      fallbackColor: const Color(0xFFF4E3E3),
                    )
                  : Row(
                      children: [
                        Expanded(
                          child: _PreviewImageTile(
                            imagePath: images[0].path,
                            fallbackColor: const Color(0xFFF4E3E3),
                          ),
                        ),
                        Expanded(
                          child: _PreviewImageTile(
                            imagePath: images[1].path,
                            fallbackColor: const Color(0xFFE4F0EC),
                          ),
                        ),
                      ],
                    ),
            ),
          ),

          Padding(
            padding: EdgeInsets.fromLTRB(h(18), h(14), h(18), h(18)),
            child: Column(
              children: [
                _SummaryRow(
                  label: '업로드 사진',
                  valueWidget: Text(
                    '${imageCount}장',
                    style: textTheme.titleLarge?.copyWith(
                      fontSize: f(18),
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ),
                SizedBox(height: h(12)),
                const Divider(height: 1, color: AppTheme.dividerColor),
                SizedBox(height: h(12)),

                _SummaryRow(
                  label: '사진 설명',
                  valueWidget: Text(
                    isDescriptionComplete ? '입력 완료 ✓' : '입력 미완료',
                    style: textTheme.titleMedium?.copyWith(
                      fontSize: f(16),
                      fontWeight: FontWeight.w700,
                      color: isDescriptionComplete
                          ? AppTheme.successText
                          : AppTheme.dangerText,
                    ),
                  ),
                ),
                SizedBox(height: h(12)),
                const Divider(height: 1, color: AppTheme.dividerColor),
                SizedBox(height: h(12)),

                _SummaryRow(
                  label: '스타일',
                  valueWidget: Text(
                    '$styleEmoji $styleLabel',
                    style: textTheme.titleMedium?.copyWith(
                      fontSize: f(16),
                      fontWeight: FontWeight.w700,
                      color: AppTheme.primaryColor,
                    ),
                  ),
                ),
                SizedBox(height: h(12)),
                const Divider(height: 1, color: AppTheme.dividerColor),
                SizedBox(height: h(12)),

                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    '추가 요청사항',
                    style: textTheme.titleMedium?.copyWith(
                      fontSize: f(16),
                      fontWeight: FontWeight.w600,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ),
                SizedBox(height: h(10)),

                Container(
                  width: double.infinity,
                  padding: EdgeInsets.symmetric(
                    horizontal: h(14),
                    vertical: h(14),
                  ),
                  decoration: BoxDecoration(
                    color: AppTheme.fillLighter,
                    borderRadius: BorderRadius.circular(h(14)),
                  ),
                  child: Text(
                    extraRequest.isEmpty ? '없음' : extraRequest,
                    style: textTheme.bodyLarge?.copyWith(
                      fontSize: f(16),
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
    final h = (double v) => AppLayout.h(context, v);
    return Container(
      color: fallbackColor,
      child: Center(
        child: imagePath != null
            ? ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: Image.file(
                  File(imagePath!),
                  width: h(80),
                  height: h(80),
                  fit: BoxFit.cover,
                ),
              )
            : Text(
                '📷',
                style: TextStyle(fontSize: AppLayout.f(context, 42)),
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
            fontSize: AppLayout.f(context, 16),
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