import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/models/content_model.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';

class ResultViewScreen extends ConsumerWidget {
  const ResultViewScreen({super.key});

  void _handleBack(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/dashboard');
    }
  }

  String _headlineFromBody(String body) {
    final first = body.split('\n').map((e) => e.trim()).firstWhere(
          (e) => e.isNotEmpty,
          orElse: () => '',
        );
    if (first.isEmpty) return '생성된 문구';
    if (first.length <= 80) return first;
    return '${first.substring(0, 80)}…';
  }

  String _generationLabel(String type) {
    switch (type.toUpperCase()) {
      case 'ALL':
        return '전체 채널';
      case 'INSTAGRAM_ONLY':
        return '인스타그램';
      default:
        return type.isEmpty ? '생성 결과' : type;
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final contentId = GoRouterState.of(context).uri.queryParameters['contentId'] ?? '';

    if (contentId.trim().isEmpty) {
      return Scaffold(
        backgroundColor: AppTheme.backgroundColor,
        body: SafeArea(
          child: Padding(
            padding: EdgeInsets.all(h(20)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                BackArrowButton(onTap: () => _handleBack(context)),
                SizedBox(height: h(20)),
                Text(
                  '콘텐츠를 불러올 수 없어요',
                  style: textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                Text(
                  'contentId가 없습니다. 생성이 완료된 뒤 다시 열어주세요.',
                  style: textTheme.bodyLarge?.copyWith(color: AppTheme.textTertiary),
                ),
              ],
            ),
          ),
        ),
      );
    }

    final async = ref.watch(contentDetailProvider(contentId));

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.fromLTRB(h(20), h(16), h(20), h(20)),
          child: async.when(
            loading: () => Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                BackArrowButton(onTap: () => _handleBack(context)),
                const Expanded(
                  child: Center(child: CircularProgressIndicator()),
                ),
              ],
            ),
            error: (e, _) => Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                BackArrowButton(onTap: () => _handleBack(context)),
                SizedBox(height: h(20)),
                Text(
                  '결과를 불러오지 못했어요',
                  style: textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                Text(
                  e.toString(),
                  style: textTheme.bodyMedium?.copyWith(color: AppTheme.textTertiary),
                ),
              ],
            ),
            data: (detail) => _ResultBody(
              detail: detail,
              textTheme: textTheme,
              onBack: () => _handleBack(context),
              headlineFromBody: _headlineFromBody,
              generationLabel: _generationLabel,
              onShare: () {
                final id = contentId.trim();
                final path = id.isEmpty
                    ? '/share'
                    : '/share?contentId=${Uri.encodeComponent(id)}';
                context.push(path);
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _ResultBody extends StatelessWidget {
  const _ResultBody({
    required this.detail,
    required this.textTheme,
    required this.onBack,
    required this.headlineFromBody,
    required this.generationLabel,
    required this.onShare,
  });

  final ContentDetail detail;
  final TextTheme textTheme;
  final VoidCallback onBack;
  final String Function(String) headlineFromBody;
  final String Function(String) generationLabel;
  final VoidCallback onShare;

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final ig = detail.contentData.instagram;
    final titleText = headlineFromBody(ig.text);
    final bodyText = ig.text.trim().isEmpty ? '본문이 없어요.' : ig.text;
    final hashtags = ig.hashtagsLine;
    final heroUrl = detail.images.isNotEmpty ? detail.images.first.url : '';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            BackArrowButton(onTap: onBack),
            SizedBox(width: h(12)),
            Text(
              '생성 결과',
              style: textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w700,
                color: AppTheme.textPrimary,
              ),
            ),
            const Spacer(),
            GestureDetector(
              onTap: () {},
              child: Text(
                '수정',
                style: textTheme.titleMedium?.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: AppTheme.primaryColor,
                ),
              ),
            ),
          ],
        ),
        SizedBox(height: h(18)),
        Container(
          padding: EdgeInsets.symmetric(horizontal: h(12), vertical: h(9)),
          decoration: BoxDecoration(
            color: const Color(0xFFF2F0FF),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Text(
            '✨ ${generationLabel(detail.generationType)}',
            style: textTheme.titleMedium?.copyWith(
              fontSize: f(16),
              fontWeight: FontWeight.w700,
              color: AppTheme.primaryColor,
            ),
          ),
        ),
        SizedBox(height: h(12)),
        Expanded(
          child: SingleChildScrollView(
            child: Container(
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
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ClipRRect(
                    borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
                    child: SizedBox(
                      height: h(240),
                      width: double.infinity,
                      child: heroUrl.isNotEmpty
                          ? Image.network(
                              heroUrl,
                              fit: BoxFit.cover,
                              loadingBuilder: (context, child, loadingProgress) {
                                if (loadingProgress == null) return child;
                                return const Center(child: CircularProgressIndicator());
                              },
                              errorBuilder: (context, error, stackTrace) => Container(
                                color: const Color(0xFFF4E3E3),
                                child: const Center(
                                  child: Text('📸', style: TextStyle(fontSize: 54)),
                                ),
                              ),
                            )
                          : Container(
                              color: const Color(0xFFF4E3E3),
                              child: const Center(
                                child: Text('📸', style: TextStyle(fontSize: 54)),
                              ),
                            ),
                    ),
                  ),
                  Padding(
                    padding: EdgeInsets.fromLTRB(h(20), h(22), h(20), h(20)),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          titleText,
                          style: textTheme.headlineSmall?.copyWith(
                            fontSize: f(22),
                            fontWeight: FontWeight.w700,
                            color: AppTheme.textPrimary,
                            height: 1.45,
                          ),
                        ),
                        SizedBox(height: h(22)),
                        Text(
                          bodyText,
                          style: textTheme.bodyLarge?.copyWith(
                            fontSize: f(18),
                            fontWeight: FontWeight.w500,
                            color: AppTheme.textSecondary,
                            height: 1.75,
                          ),
                        ),
                        if (hashtags.isNotEmpty) ...[
                          SizedBox(height: h(20)),
                          const Divider(color: AppTheme.dividerColor),
                          const SizedBox(height: 18),
                          Text(
                            hashtags,
                            style: textTheme.bodyLarge?.copyWith(
                              fontSize: f(17),
                              fontWeight: FontWeight.w500,
                              color: AppTheme.primaryColor,
                              height: 1.7,
                            ),
                          ),
                        ],
                        _PlatformExtra(
                          label: '당근',
                          text: detail.contentData.karrot.text,
                          tags: detail.contentData.karrot.hashtagsLine,
                          textTheme: textTheme,
                        ),
                        _PlatformExtra(
                          label: '네이버',
                          text: detail.contentData.naver.text,
                          tags: detail.contentData.naver.hashtagsLine,
                          textTheme: textTheme,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        SizedBox(height: h(16)),
        Row(
          children: [
            Expanded(
              flex: 3,
              child: SizedBox(
                height: h(62),
                child: OutlinedButton(
                  onPressed: () {},
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: AppTheme.primaryColor, width: 2),
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
                text: '공유하기 →',
                onPressed: onShare,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _PlatformExtra extends StatelessWidget {
  const _PlatformExtra({
    required this.label,
    required this.text,
    required this.tags,
    required this.textTheme,
  });

  final String label;
  final String text;
  final String tags;
  final TextTheme textTheme;

  @override
  Widget build(BuildContext context) {
    if (text.trim().isEmpty && tags.trim().isEmpty) {
      return const SizedBox.shrink();
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 20),
        Text(
          label,
          style: textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w700,
            color: AppTheme.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        if (text.trim().isNotEmpty)
          Text(
            text,
            style: textTheme.bodyLarge?.copyWith(
              height: 1.6,
              color: AppTheme.textSecondary,
            ),
          ),
        if (tags.trim().isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(
            tags,
            style: textTheme.bodyMedium?.copyWith(
              color: AppTheme.primaryColor,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ],
    );
  }
}
