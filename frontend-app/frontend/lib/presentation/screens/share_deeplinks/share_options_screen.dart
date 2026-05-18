import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:async';
import 'dart:io';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/models/content_model.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../../services/external_app_launcher.dart';
import '../../../services/sns_app_link_tracker.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/dialogs/app_install_dialog.dart';

/// 공유 채널 선택 (딥링크·복사 등).
class ShareOptionsScreen extends ConsumerStatefulWidget {
  const ShareOptionsScreen({super.key});

  @override
  ConsumerState<ShareOptionsScreen> createState() => _ShareOptionsScreenState();
}

class _ShareOptionsScreenState extends ConsumerState<ShareOptionsScreen> {
  static const MethodChannel _shareChannel = MethodChannel('today_store/share');

  /// 사용자가 탭하기 전까지는 강조 없음 (당근·네이버와 동일한 기본 테두리).
  int? _selectedIndex;

  /// 데스크톱·웹 호버 시 해당 카드만 대표색 테두리.
  int? _hoveredIndex;
  bool _isInstagramSharing = false;
  bool _isCopyingText = false;

  bool _primaryBorder(int index) {
    if (_hoveredIndex != null) {
      return _hoveredIndex == index;
    }
    if (_selectedIndex == null) {
      return false;
    }
    return _selectedIndex == index;
  }

  void _handleBack(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/dashboard');
    }
  }

  Future<void> _shareToInstagram() async {
    if (_isInstagramSharing) return;
    setState(() => _isInstagramSharing = true);
    try {
      final contentId = GoRouterState.of(context).uri.queryParameters['contentId'] ?? '';
      if (contentId.trim().isEmpty) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('공유할 콘텐츠 정보를 찾을 수 없어요.')),
        );
        return;
      }

      final repository = ref.read(contentRepositoryProvider);
      final detail = await repository.getContent(contentId: contentId.trim());
      final imageUrl = detail.images.isNotEmpty ? detail.images.first.url.trim() : '';
      if (imageUrl.isEmpty) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('공유할 이미지가 없어요.')),
        );
        return;
      }

      final bytes = await repository.contentApi.dio
          .get<List<int>>(imageUrl, options: Options(responseType: ResponseType.bytes))
          .then((res) => res.data);
      if (bytes == null || bytes.isEmpty) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('이미지를 불러오지 못했어요.')),
        );
        return;
      }

      final tempDir = await getTemporaryDirectory();
      final file = File(
        '${tempDir.path}${Platform.pathSeparator}today_store_instagram_${DateTime.now().millisecondsSinceEpoch}.jpg',
      );
      await file.writeAsBytes(bytes, flush: true);

      final openedInstagram = await _shareChannel.invokeMethod<bool>(
            'shareImageToInstagram',
            {'filePath': file.path},
          ) ??
          false;
      if (openedInstagram) {
        await SnsAppLinkTracker.markOpened(StoreListingApp.instagram);
        return;
      }

      if (!mounted) return;
      final instagramInstalled =
          await ExternalAppLauncher.isAppInstalled(StoreListingApp.instagram);
      if (!mounted) return;
      if (!instagramInstalled) {
        await AppInstallDialog.show(context, app: StoreListingApp.instagram);
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Instagram을 열 수 없어요. 잠시 후 다시 시도해 주세요.'),
        ),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Instagram 공유 중 오류가 발생했어요.')),
      );
    } finally {
      if (mounted) {
        setState(() => _isInstagramSharing = false);
      }
    }
  }

  Future<void> _openDaangn() async {
    setState(() => _selectedIndex = 1);
    final installed = await ExternalAppLauncher.canOpenApp(StoreListingApp.daangn);
    if (!mounted) return;
    if (!installed) {
      await AppInstallDialog.show(context, app: StoreListingApp.daangn);
      return;
    }
    final opened = await ExternalAppLauncher.openApp(StoreListingApp.daangn);
    if (!mounted) return;
    if (opened) {
      await SnsAppLinkTracker.markOpened(StoreListingApp.daangn);
      return;
    }
    await AppInstallDialog.show(context, app: StoreListingApp.daangn);
  }

  Future<void> _openNaver() async {
    setState(() => _selectedIndex = 2);
    final installed = await ExternalAppLauncher.canOpenApp(StoreListingApp.naver);
    if (!mounted) return;
    if (!installed) {
      await AppInstallDialog.show(context, app: StoreListingApp.naver);
      return;
    }
    final opened = await ExternalAppLauncher.openApp(StoreListingApp.naver);
    if (!mounted) return;
    if (opened) {
      await SnsAppLinkTracker.markOpened(StoreListingApp.naver);
      return;
    }
    await AppInstallDialog.show(context, app: StoreListingApp.naver);
  }

  String _composeShareCopyText(ContentDetail detail) {
    final buf = StringBuffer();
    final ig = detail.contentData.instagram;
    final main = ig.text.trim();
    if (main.isNotEmpty) {
      buf.writeln(main);
    }
    final ht = ig.hashtagsLine.trim();
    if (ht.isNotEmpty) {
      if (buf.isNotEmpty) {
        buf.writeln();
      }
      buf.writeln(ht);
    }
    _appendPlatformCopy(buf, '당근', detail.contentData.karrot);
    _appendPlatformCopy(buf, '네이버 스마트플레이스', detail.contentData.naver);
    return buf.toString().trim();
  }

  void _appendPlatformCopy(StringBuffer buf, String label, ContentPlatformCopy p) {
    final t = p.text.trim();
    final tags = p.hashtagsLine.trim();
    if (t.isEmpty && tags.isEmpty) {
      return;
    }
    buf.writeln();
    buf.writeln('[$label]');
    if (t.isNotEmpty) {
      buf.writeln(t);
    }
    if (tags.isNotEmpty) {
      buf.writeln(tags);
    }
  }

  Future<void> _copyGeneratedText() async {
    if (_isCopyingText) {
      return;
    }
    final contentId = GoRouterState.of(context).uri.queryParameters['contentId']?.trim() ?? '';
    if (contentId.isEmpty) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('복사할 콘텐츠 정보를 찾을 수 없어요.')),
      );
      return;
    }

    setState(() => _isCopyingText = true);
    try {
      final repository = ref.read(contentRepositoryProvider);
      final detail = await repository.getContent(contentId: contentId);
      final text = _composeShareCopyText(detail);
      if (text.isEmpty) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('복사할 문구가 없어요.')),
        );
        return;
      }
      await Clipboard.setData(ClipboardData(text: text));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('클립보드에 복사했어요.')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('문구를 불러오지 못했어요.')),
      );
    } finally {
      if (mounted) {
        setState(() => _isCopyingText = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

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
                    '공유하기',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(24)),
              Text(
                '어디에 공유할까요?',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                  height: 1.25,
                ),
              ),
              SizedBox(height: h(8)),
              Text(
                '여러 플랫폼에 동시에 공유할 수 있어요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              SizedBox(height: h(20)),
              Expanded(
                child: ListView(
                  children: [
                    MouseRegion(
                      onEnter: (_) => setState(() => _hoveredIndex = 0),
                      onExit: (_) => setState(() => _hoveredIndex = null),
                      child: _ShareOptionCard(
                        emphasized: _primaryBorder(0),
                        backgroundColor: _primaryBorder(0)
                            ? AppTheme.infoBg.withValues(alpha: 0.35)
                            : AppTheme.surfaceColor,
                        leading: _GradientIconBox(
                          child: Icon(
                            Icons.folder_open_rounded,
                            color: Colors.white.withValues(alpha: 0.96),
                            size: 26,
                          ),
                        ),
                        title: 'Instagram',
                        subtitle: 'Instagram 앱으로 이동하여 작성',
                        trailing: Icon(
                          Icons.arrow_forward_ios_rounded,
                          size: 16,
                          color: AppTheme.textTertiary,
                        ),
                        onTap: () async {
                          setState(() => _selectedIndex = 0);
                          await _shareToInstagram();
                        },
                      ),
                    ),
                    SizedBox(height: h(12)),
                    MouseRegion(
                      onEnter: (_) => setState(() => _hoveredIndex = 1),
                      onExit: (_) => setState(() => _hoveredIndex = null),
                      child: _ShareOptionCard(
                        emphasized: _primaryBorder(1),
                        backgroundColor: _primaryBorder(1)
                            ? AppTheme.infoBg.withValues(alpha: 0.35)
                            : AppTheme.surfaceColor,
                        leading: Container(
                          width: 56,
                          height: 56,
                          decoration: BoxDecoration(
                            color: const Color(0xFFFF8A3D),
                            borderRadius: BorderRadius.circular(14),
                          ),
                          alignment: Alignment.center,
                          child: const Text(
                            '🥕',
                            style: TextStyle(fontSize: 28, height: 1),
                          ),
                        ),
                        title: '당근마켓',
                        subtitle: '당근마켓 앱으로 이동하여 작성',
                        trailing: Icon(
                          Icons.arrow_forward_ios_rounded,
                          size: 16,
                          color: AppTheme.textTertiary,
                        ),
                        onTap: () async {
                          await _openDaangn();
                        },
                      ),
                    ),
                    SizedBox(height: h(12)),
                    MouseRegion(
                      onEnter: (_) => setState(() => _hoveredIndex = 2),
                      onExit: (_) => setState(() => _hoveredIndex = null),
                      child: _ShareOptionCard(
                        emphasized: _primaryBorder(2),
                        backgroundColor: _primaryBorder(2)
                            ? AppTheme.infoBg.withValues(alpha: 0.35)
                            : AppTheme.surfaceColor,
                        leading: Container(
                          width: 56,
                          height: 56,
                          decoration: BoxDecoration(
                            color: const Color(0xFF03C75A),
                            borderRadius: BorderRadius.circular(14),
                          ),
                          child: const Center(
                            child: Text(
                              'N',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 24,
                                fontWeight: FontWeight.w800,
                                height: 1,
                              ),
                            ),
                          ),
                        ),
                        title: '네이버 스마트플레이스',
                        subtitle: '네이버 스마트플레이스 앱으로 이동하여 작성',
                        trailing: Icon(
                          Icons.arrow_forward_ios_rounded,
                          size: 16,
                          color: AppTheme.textTertiary,
                        ),
                        onTap: () async {
                          await _openNaver();
                        },
                      ),
                    ),
                    SizedBox(height: h(12)),
                    MouseRegion(
                      onEnter: (_) => setState(() => _hoveredIndex = 3),
                      onExit: (_) => setState(() => _hoveredIndex = null),
                      child: _ShareOptionCard(
                        emphasized: _primaryBorder(3),
                        backgroundColor: _primaryBorder(3)
                            ? AppTheme.infoBg.withValues(alpha: 0.4)
                            : AppTheme.fillLight,
                        leading: Container(
                          width: 56,
                          height: 56,
                          decoration: BoxDecoration(
                            color: const Color(0xFFE8EAF6),
                            borderRadius: BorderRadius.circular(14),
                          ),
                          child: Icon(
                            Icons.content_paste_outlined,
                            color: AppTheme.primaryColor.withValues(alpha: 0.9),
                            size: 26,
                          ),
                        ),
                        title: '텍스트 복사',
                        subtitle: '클립보드에 복사하여 자유롭게 사용',
                        trailing: Icon(
                          Icons.arrow_forward_ios_rounded,
                          size: 16,
                          color: AppTheme.textTertiary,
                        ),
                        onTap: () {
                          setState(() => _selectedIndex = 3);
                          unawaited(_copyGeneratedText());
                        },
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(height: h(10)),
              SizedBox(
                width: double.infinity,
                height: h(62),
                child: OutlinedButton(
                  onPressed: () => context.go('/dashboard'),
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: AppTheme.primaryColor, width: 2),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20),
                    ),
                    backgroundColor: AppTheme.surfaceColor,
                  ),
                  child: Text(
                    '대시보드로 돌아가기',
                    style: textTheme.titleMedium?.copyWith(
                      fontSize: f(18),
                      fontWeight: FontWeight.w700,
                      color: AppTheme.primaryColor,
                    ),
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

class _GradientIconBox extends StatelessWidget {
  const _GradientIconBox({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 56,
      height: 56,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(14),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xFFF58529),
            Color(0xFFDD2A7B),
            Color(0xFF8134AF),
          ],
        ),
      ),
      child: Center(child: child),
    );
  }
}

class _ShareOptionCard extends StatelessWidget {
  const _ShareOptionCard({
    required this.leading,
    required this.title,
    required this.subtitle,
    required this.trailing,
    required this.onTap,
    required this.backgroundColor,
    required this.emphasized,
  });

  final Widget leading;
  final String title;
  final String subtitle;
  final Widget trailing;
  final VoidCallback onTap;
  final Color backgroundColor;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final borderColor = emphasized ? AppTheme.primaryColor : AppTheme.borderStrongColor;
    final borderWidth = emphasized ? 2.0 : 1.2;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        splashColor: AppTheme.primaryColor.withValues(alpha: 0.12),
        highlightColor: AppTheme.primaryColor.withValues(alpha: 0.06),
        child: Ink(
          decoration: BoxDecoration(
            color: backgroundColor,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: borderColor, width: borderWidth),
            boxShadow: emphasized
                ? [
                    BoxShadow(
                      color: AppTheme.primaryColor.withValues(alpha: 0.12),
                      blurRadius: 12,
                      offset: const Offset(0, 4),
                    ),
                  ]
                : const [
                    BoxShadow(
                      color: AppTheme.shadowColor,
                      blurRadius: 6,
                      offset: Offset(0, 2),
                    ),
                  ],
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 20),
            child: Row(
              children: [
                leading,
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: AppTheme.textPrimary,
                          fontSize: 17,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: textTheme.bodyMedium?.copyWith(
                          color: AppTheme.textTertiary,
                          fontWeight: FontWeight.w500,
                          height: 1.35,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                trailing,
              ],
            ),
          ),
        ),
      ),
    );
  }
}
