import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/models/content_model.dart';
import '../../../data/providers/content_creation_provider.dart';
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
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
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
                    contentId: contentId.trim(),
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
            ],
          ),
        ),
      ),
    );
  }
}

class _ResultBody extends ConsumerStatefulWidget {
  const _ResultBody({
    required this.contentId,
    required this.detail,
    required this.textTheme,
    required this.onBack,
    required this.headlineFromBody,
    required this.generationLabel,
    required this.onShare,
  });

  final String contentId;
  final ContentDetail detail;
  final TextTheme textTheme;
  final VoidCallback onBack;
  final String Function(String) headlineFromBody;
  final String Function(String) generationLabel;
  final VoidCallback onShare;

  @override
  ConsumerState<_ResultBody> createState() => _ResultBodyState();
}

class _ResultBodyState extends ConsumerState<_ResultBody> {
  static const double _actionButtonHeight = 62;
  bool _isPreparingRegenerate = false;

  Future<void> _goToStep2ForRegenerate() async {
    if (_isPreparingRegenerate) {
      return;
    }

    setState(() => _isPreparingRegenerate = true);
    try {
      final requestId = widget.detail.requestId.trim();
      if (requestId.isEmpty) {
        if (mounted) {
          context.push('/step2');
        }
        return;
      }

      final repository = ref.read(contentRepositoryProvider);
      final requestDetail = await repository.getRequestDetail(requestId: requestId);
      final imageUrls = requestDetail.images
          .map((e) => e.url.trim())
          .where((e) => e.isNotEmpty)
          .toList();

      final notifier = ref.read(contentCreationProvider.notifier);
      final downloadedFiles = <XFile>[];
      for (var i = 0; i < imageUrls.length; i++) {
        final url = imageUrls[i];
        final bytes = await repository.contentApi.dio
            .get<List<int>>(url, options: Options(responseType: ResponseType.bytes))
            .then((res) => res.data);
        if (bytes == null || bytes.isEmpty) {
          continue;
        }
        final tempDir = await getTemporaryDirectory();
        final file = File(
          '${tempDir.path}${Platform.pathSeparator}regen_source_${DateTime.now().millisecondsSinceEpoch}_$i.jpg',
        );
        await file.writeAsBytes(bytes, flush: true);
        downloadedFiles.add(XFile(file.path));
      }

      notifier.setImages(downloadedFiles);
      for (var i = 0; i < downloadedFiles.length; i++) {
        final fromServer = i < requestDetail.imageDescriptions.length
            ? requestDetail.imageDescriptions[i].trim()
            : '';
        notifier.setDescription(i, fromServer.isNotEmpty ? fromServer : '사진 ${i + 1}');
      }
      notifier.setExtraRequest(requestDetail.additionalNote);

      final concept = requestDetail.concept.trim();
      const allowedStyles = {'감성적', '정보제공', '전문성', '친근'};
      if (allowedStyles.contains(concept)) {
        notifier.setSelectedStyle(concept);
      }

      if (!mounted) {
        return;
      }
      if (downloadedFiles.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('이전 이미지가 없어 새로 선택해 주세요.')),
        );
      }
      context.push('/step2');
    } catch (_) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('이전 입력값을 불러오지 못했어요. step2에서 다시 입력해 주세요.')),
      );
      context.push('/step2');
    } finally {
      if (mounted) {
        setState(() => _isPreparingRegenerate = false);
      }
    }
  }

  Future<void> _deleteCurrentContent() async {
    final contentId = widget.contentId.trim();
    if (contentId.isEmpty) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('삭제할 콘텐츠 정보를 찾을 수 없어요.')),
      );
      return;
    }

    final shouldDelete = await showDialog<bool>(
          context: context,
          builder: (_) => AlertDialog(
            title: const Text('콘텐츠 삭제'),
            content: const Text('이 콘텐츠를 삭제할까요?'),
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
      await repository.deleteContent(contentId: contentId);
      ref.invalidate(dashboardDataProvider);
      ref.invalidate(contentDetailProvider(contentId));
      if (!mounted) return;
      context.go('/history');
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('삭제 중 오류가 발생했어요.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final ig = widget.detail.contentData.instagram;
    final titleText = widget.headlineFromBody(ig.text);
    final bodyText = ig.text.trim().isEmpty ? '본문이 없어요.' : ig.text;
    final hashtags = ig.hashtagsLine;
    final heroUrl =
        widget.detail.images.isNotEmpty ? widget.detail.images.first.url : '';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            BackArrowButton(onTap: widget.onBack),
            SizedBox(width: h(12)),
            Expanded(
              child: Text(
                '생성 결과',
                style: widget.textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
            ),
            TextButton(
              onPressed: _deleteCurrentContent,
              child: Text(
                '삭제',
                style: widget.textTheme.titleSmall?.copyWith(
                  color: AppTheme.dangerText,
                  fontWeight: FontWeight.w700,
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
            '✨ ${widget.generationLabel(widget.detail.generationType)}',
            style: widget.textTheme.titleMedium?.copyWith(
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
                          style: widget.textTheme.headlineSmall?.copyWith(
                            fontSize: f(22),
                            fontWeight: FontWeight.w700,
                            color: AppTheme.textPrimary,
                            height: 1.45,
                          ),
                        ),
                        SizedBox(height: h(22)),
                        Text(
                          bodyText,
                          style: widget.textTheme.bodyLarge?.copyWith(
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
                            style: widget.textTheme.bodyLarge?.copyWith(
                              fontSize: f(17),
                              fontWeight: FontWeight.w500,
                              color: AppTheme.primaryColor,
                              height: 1.7,
                            ),
                          ),
                        ],
                        _PlatformExtra(
                          label: '당근',
                          text: widget.detail.contentData.karrot.text,
                          tags: widget.detail.contentData.karrot.hashtagsLine,
                          textTheme: widget.textTheme,
                        ),
                        _PlatformExtra(
                          label: '네이버',
                          text: widget.detail.contentData.naver.text,
                          tags: widget.detail.contentData.naver.hashtagsLine,
                          textTheme: widget.textTheme,
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
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(
              flex: 3,
              child: SizedBox(
                height: h(_actionButtonHeight),
                child: OutlinedButton(
                  onPressed: _isPreparingRegenerate ? null : _goToStep2ForRegenerate,
                  style: OutlinedButton.styleFrom(
                    minimumSize: Size.zero,
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    padding: EdgeInsets.symmetric(horizontal: h(8)),
                    side: const BorderSide(color: AppTheme.primaryColor, width: 2),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20),
                    ),
                    backgroundColor: AppTheme.surfaceColor,
                  ),
                  child: Text(
                    _isPreparingRegenerate ? '불러오는 중...' : '재생성',
                    style: widget.textTheme.titleMedium?.copyWith(
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
                height: _actionButtonHeight,
                fontSize: 18,
                fontWeight: FontWeight.w700,
                onPressed: widget.onShare,
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
