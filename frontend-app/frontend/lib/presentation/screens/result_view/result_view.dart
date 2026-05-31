import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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

class ResultViewScreen extends ConsumerWidget {
  const ResultViewScreen({super.key});

  void _handleBack(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/dashboard');
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
                  'ID 정보가 누락되었습니다.',
                  style: textTheme.bodyLarge?.copyWith(color: AppTheme.textTertiary),
                ),
              ],
            ),
          ),
        ),
      );
    }

    final async = ref.watch(contentDetailProvider(contentId));

    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: AppTheme.backgroundColor,
        body: SafeArea(
          child: async.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Padding(
              padding: EdgeInsets.all(h(20)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  BackArrowButton(onTap: () => _handleBack(context)),
                  SizedBox(height: h(20)),
                  Text('결과를 불러오지 못했어요', style: textTheme.titleLarge),
                  Text(e.toString()),
                ],
              ),
            ),
            data: (detail) => _ResultBody(
              contentId: contentId.trim(),
              detail: detail,
              textTheme: textTheme,
              onBack: () => _handleBack(context),
            ),
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
  });

  final String contentId;
  final ContentDetail detail;
  final TextTheme textTheme;
  final VoidCallback onBack;

  @override
  ConsumerState<_ResultBody> createState() => _ResultBodyState();
}

class _ResultBodyState extends ConsumerState<_ResultBody> {
  late TextEditingController _instaController;
  late TextEditingController _karrotController;
  late TextEditingController _naverController;

  // 원본 텍스트 저장 (변경 여부 확인용)
  String _origInsta = '';
  String _origKarrot = '';
  String _origNaver = '';

  bool _isPreparingRegenerate = false;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _initControllers();
  }

  void _initControllers() {
    final data = widget.detail.contentData;
    _origInsta = '${data.instagram.text}\n\n${data.instagram.hashtagsLine}'.trim();
    _origKarrot = '${data.karrot.text}\n\n${data.karrot.hashtagsLine}'.trim();
    _origNaver = '${data.naver.text}\n\n${data.naver.hashtagsLine}'.trim();

    _instaController = TextEditingController(text: _origInsta);
    _karrotController = TextEditingController(text: _origKarrot);
    _naverController = TextEditingController(text: _origNaver);

    // 변경 감지를 위해 리스너 추가
    _instaController.addListener(() => setState(() {}));
    _karrotController.addListener(() => setState(() {}));
    _naverController.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _instaController.dispose();
    _karrotController.dispose();
    _naverController.dispose();
    super.dispose();
  }

  bool _isChanged(String channel) {
    if (channel == '인스타그램') return _instaController.text.trim() != _origInsta;
    if (channel == '당근마켓') return _karrotController.text.trim() != _origKarrot;
    if (channel == '네이버 블로그') return _naverController.text.trim() != _origNaver;
    return false;
  }

  Future<void> _saveChannel(String label) async {
    if (_isSaving) return;
    setState(() => _isSaving = true);

    try {
      final repository = ref.read(contentRepositoryProvider);
      final contentId = widget.contentId;

      String? insta, karrot, naver;
      if (label == '인스타그램') insta = _instaController.text.trim();
      if (label == '당근마켓') karrot = _karrotController.text.trim();
      if (label == '네이버 블로그') naver = _naverController.text.trim();

      await repository.updateContent(
        contentId: contentId,
        instagramText: insta,
        karrotText: karrot,
        naverText: naver,
      );

      // 성공 시 원본 데이터 갱신
      setState(() {
        if (insta != null) _origInsta = insta;
        if (karrot != null) _origKarrot = karrot;
        if (naver != null) _origNaver = naver;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$label 문구가 저장되었습니다.')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('저장 중 오류가 발생했습니다: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  // 채널별 로딩 상태 관리
  final Map<String, bool> _channelLoading = {
    '인스타그램': false,
    '당근마켓': false,
    '네이버 블로그': false,
  };

  Future<void> _partialRegenerate(String label) async {
    if (_channelLoading[label] == true) return;
    
    setState(() => _channelLoading[label] = true);

    try {
      final repository = ref.read(contentRepositoryProvider);
      final requestId = widget.detail.requestId.trim();

      // 1. 재생성 요청 시작
      final genResponse = await repository.generateContent(requestId: requestId);
      final taskId = genResponse.taskId;

      // 2. 폴링 시작 (최대 1분)
      ContentDetail? newDetail;
      for (int i = 0; i < 30; i++) {
        await Future.delayed(const Duration(seconds: 2));
        final status = await repository.getTaskStatus(apiLogId: taskId);
        
        if (status.isSuccess) {
          // 3. 성공 시 새로운 데이터 가져오기
          if (status.result != null) {
             newDetail = await repository.getContent(contentId: status.result!);
          }
          break;
        } else if (status.isError) {
          throw Exception('콘텐츠 생성에 실패했습니다.');
        }
      }

      if (newDetail != null) {
        // 4. 특정 채널만 업데이트 (나머지는 유지)
        final newData = newDetail.contentData;
        setState(() {
          if (label == '인스타그램') {
            _instaController.text = '${newData.instagram.text}\n\n${newData.instagram.hashtagsLine}'.trim();
            _origInsta = _instaController.text;
          } else if (label == '당근마켓') {
            _karrotController.text = '${newData.karrot.text}\n\n${newData.karrot.hashtagsLine}'.trim();
            _origKarrot = _karrotController.text;
          } else if (label == '네이버 블로그') {
            _naverController.text = '${newData.naver.text}\n\n${newData.naver.hashtagsLine}'.trim();
            _origNaver = _naverController.text;
          }
        });

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('$label 문구가 새로 생성되었습니다!')),
          );
        }
      } else {
        throw Exception('생성된 데이터를 불러올 수 없습니다.');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('재생성 중 오류 발생: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _channelLoading[label] = false);
    }
  }

  // 기존 전체 재생성 로직 (필요시 대비해 유지)
  Future<void> _goToFullRegenerate() async {
    if (_isPreparingRegenerate) return;
    setState(() => _isPreparingRegenerate = true);

    try {
      final requestId = widget.detail.requestId.trim();
      if (requestId.isEmpty) {
        context.push('/step2');
        return;
      }

      final repository = ref.read(contentRepositoryProvider);
      final requestDetail = await repository.getRequestDetail(requestId: requestId);
      final imageUrls = requestDetail.images.map((e) => e.url.trim()).where((e) => e.isNotEmpty).toList();

      final notifier = ref.read(contentCreationProvider.notifier);
      final downloadedFiles = <XFile>[];
      for (var i = 0; i < imageUrls.length; i++) {
        final url = imageUrls[i];
        final bytes = await repository.contentApi.dio
            .get<List<int>>(url, options: Options(responseType: ResponseType.bytes))
            .then((res) => res.data);
        if (bytes == null) continue;
        final tempDir = await getTemporaryDirectory();
        final file = File('${tempDir.path}/regen_${DateTime.now().millisecondsSinceEpoch}_$i.jpg');
        await file.writeAsBytes(bytes);
        downloadedFiles.add(XFile(file.path));
      }

      notifier.setImages(downloadedFiles);
      for (var i = 0; i < downloadedFiles.length; i++) {
        final desc = i < requestDetail.imageDescriptions.length ? requestDetail.imageDescriptions[i] : '';
        notifier.setDescription(i, desc.isNotEmpty ? desc : '사진 ${i + 1}');
      }
      notifier.setExtraRequest(requestDetail.additionalNote);
      if (['감성적', '정보제공', '전문성', '친근'].contains(requestDetail.concept)) {
        notifier.setSelectedStyle(requestDetail.concept);
      }

      if (mounted) context.push('/step2');
    } catch (_) {
      if (mounted) context.push('/step2');
    } finally {
      if (mounted) setState(() => _isPreparingRegenerate = false);
    }
  }

  Future<void> _deleteCurrentContent() async {
    final contentId = widget.contentId.trim();
    if (contentId.isEmpty) return;

    final shouldDelete = await showDialog<bool>(
          context: context,
          builder: (_) => AlertDialog(
            title: const Text('콘텐츠 삭제'),
            content: const Text('이 콘텐츠를 정말 삭제할까요?'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('취소'),
              ),
              TextButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('삭제', style: TextStyle(color: AppTheme.dangerText)),
              ),
            ],
          ),
        ) ??
        false;

    if (shouldDelete) {
      try {
        final repository = ref.read(contentRepositoryProvider);
        await repository.deleteContent(contentId: contentId);
        ref.invalidate(dashboardDataProvider);
        if (mounted) {
          context.go('/history');
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('삭제 중 오류가 발생했습니다: $e')),
          );
        }
      }
    }
  }

  void _copyToClipboard(String text) {
    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('복사되었습니다!')),
    );
  }

  void _shareContent(String text) {
    // 딥링크 공유 화면으로 이동 (인스타그램/당근/네이버 앱 열기 지원)
    context.push('/share?contentId=${widget.contentId}');
  }

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return Column(
      children: [
        Padding(
          padding: EdgeInsets.fromLTRB(h(20), h(12), h(12), h(12)),
          child: Row(
            children: [
              BackArrowButton(onTap: widget.onBack),
              SizedBox(width: h(12)),
              Expanded(
                child: Text(
                  '홍보 콘텐츠 생성 결과',
                  style: widget.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
                ),
              ),
              IconButton(
                onPressed: _deleteCurrentContent,
                icon: const Icon(Icons.delete_outline, color: AppTheme.textTertiary),
                tooltip: '삭제',
              ),
            ],
          ),
        ),
        TabBar(
          indicatorColor: AppTheme.primaryColor,
          labelColor: AppTheme.primaryColor,
          unselectedLabelColor: AppTheme.textTertiary,
          labelStyle: widget.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          tabs: const [
            Tab(text: '인스타그램'),
            Tab(text: '당근마켓'),
            Tab(text: '네이버 블로그'),
          ],
        ),
        Expanded(
          child: TabBarView(
            children: [
              _ChannelResultCard(
                label: '인스타그램',
                controller: _instaController,
                imageUrl: widget.detail.images.isNotEmpty ? widget.detail.images.first.url : '',
                onCopy: () => _copyToClipboard(_instaController.text),
                onShare: () => _shareContent(_instaController.text),
                onRegenerate: () => _partialRegenerate('인스타그램'),
                onSave: () => _saveChannel('인스타그램'),
                isRegenerating: _channelLoading['인스타그램'] ?? false,
                isChanged: _isChanged('인스타그램'),
                isSaving: _isSaving,
                brandColor: const Color(0xFFE1306C),
              ),
              _ChannelResultCard(
                label: '당근마켓',
                controller: _karrotController,
                imageUrl: widget.detail.images.isNotEmpty ? widget.detail.images.first.url : '',
                onCopy: () => _copyToClipboard(_karrotController.text),
                onShare: () => _shareContent(_karrotController.text),
                onRegenerate: () => _partialRegenerate('당근마켓'),
                onSave: () => _saveChannel('당근마켓'),
                isRegenerating: _channelLoading['당근마켓'] ?? false,
                isChanged: _isChanged('당근마켓'),
                isSaving: _isSaving,
                brandColor: const Color(0xFFFF7E36),
              ),
              _ChannelResultCard(
                label: '네이버 블로그',
                controller: _naverController,
                imageUrl: widget.detail.images.isNotEmpty ? widget.detail.images.first.url : '',
                onCopy: () => _copyToClipboard(_naverController.text),
                onShare: () => _shareContent(_naverController.text),
                onRegenerate: () => _partialRegenerate('네이버 블로그'),
                onSave: () => _saveChannel('네이버 블로그'),
                isRegenerating: _channelLoading['네이버 블로그'] ?? false,
                isChanged: _isChanged('네이버 블로그'),
                isSaving: _isSaving,
                brandColor: const Color(0xFF03C75A),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ChannelResultCard extends StatelessWidget {
  const _ChannelResultCard({
    required this.label,
    required this.controller,
    required this.imageUrl,
    required this.onCopy,
    required this.onShare,
    required this.onRegenerate,
    required this.onSave,
    required this.isRegenerating,
    required this.isChanged,
    required this.isSaving,
    required this.brandColor,
  });

  final String label;
  final TextEditingController controller;
  final String imageUrl;
  final VoidCallback onCopy;
  final VoidCallback onShare;
  final VoidCallback onRegenerate;
  final VoidCallback onSave;
  final bool isRegenerating;
  final bool isChanged;
  final bool isSaving;
  final Color brandColor;

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final textTheme = Theme.of(context).textTheme;

    return SingleChildScrollView(
      padding: EdgeInsets.all(h(20)),
      child: Column(
        children: [
          Container(
            decoration: BoxDecoration(
              color: AppTheme.surfaceColor,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: AppTheme.borderColor),
              boxShadow: [
                BoxShadow(
                  color: brandColor.withOpacity(0.08),
                  blurRadius: 15,
                  offset: const Offset(0, 6),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (imageUrl.isNotEmpty)
                  ClipRRect(
                    borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
                    child: Image.network(
                      imageUrl,
                      height: h(220),
                      fit: BoxFit.cover,
                    ),
                  ),
                Padding(
                  padding: EdgeInsets.all(h(20)),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            decoration: BoxDecoration(
                              color: brandColor.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              label,
                              style: textTheme.labelLarge?.copyWith(
                                color: brandColor,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                          if (isChanged)
                            TextButton.icon(
                              onPressed: isSaving ? null : onSave,
                              icon: isSaving
                                  ? const SizedBox(
                                      width: 14,
                                      height: 14,
                                      child: CircularProgressIndicator(strokeWidth: 2),
                                    )
                                  : const Icon(Icons.check_circle_outline, size: 18),
                              label: Text(isSaving ? '저장 중' : '저장하기'),
                              style: TextButton.styleFrom(
                                foregroundColor: AppTheme.primaryColor,
                                textStyle: const TextStyle(fontWeight: FontWeight.w700),
                              ),
                            )
                          else
                            const Icon(Icons.edit_note_rounded, color: AppTheme.textTertiary),
                        ],
                      ),
                      SizedBox(height: h(16)),
                      Container(
                        decoration: BoxDecoration(
                          color: AppTheme.backgroundColor.withOpacity(0.5),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(
                            color: isChanged ? brandColor.withOpacity(0.3) : AppTheme.borderColor,
                            width: 1.5,
                          ),
                        ),
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                        child: TextField(
                          controller: controller,
                          maxLines: null,
                          style: textTheme.bodyLarge?.copyWith(
                            fontSize: f(17),
                            height: 1.6,
                            color: AppTheme.textPrimary,
                          ),
                          decoration: const InputDecoration(
                            border: InputBorder.none,
                            hintText: '이곳을 눌러 문구를 직접 수정해 보세요.',
                            hintStyle: TextStyle(color: AppTheme.textTertiary, fontSize: 14),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          SizedBox(height: h(20)),
          Row(
            children: [
              Expanded(
                child: _ActionButton(
                  icon: Icons.copy_rounded,
                  label: '복사하기',
                  onTap: onCopy,
                  color: AppTheme.textPrimary,
                ),
              ),
              SizedBox(width: h(10)),
              Expanded(
                child: _ActionButton(
                  icon: Icons.share_rounded,
                  label: '공유하기',
                  onTap: onShare,
                  color: brandColor,
                ),
              ),
              SizedBox(width: h(10)),
              Expanded(
                child: _ActionButton(
                  icon: Icons.refresh_rounded,
                  label: '재생성',
                  onTap: onRegenerate,
                  color: AppTheme.primaryColor,
                  isLoading: isRegenerating,
                ),
              ),
            ],
          ),
          SizedBox(height: h(40)),
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
    required this.color,
    this.isLoading = false,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Color color;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: isLoading ? null : onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: isLoading ? color.withOpacity(0.05) : Colors.transparent,
          border: Border.all(color: color.withOpacity(0.3)),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          children: [
            if (isLoading)
              SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2.5,
                  valueColor: AlwaysStoppedAnimation<Color>(color),
                ),
              )
            else
              Icon(icon, color: color, size: 24),
            const SizedBox(height: 4),
            Text(
              isLoading ? '생성 중' : label,
              style: TextStyle(
                color: color,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
