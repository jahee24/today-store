import 'package:flutter/material.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../services/external_app_launcher.dart';
import '../../../services/sns_app_link_tracker.dart';
import '../../widgets/dialogs/app_install_dialog.dart';

class SnsManagementScreen extends StatefulWidget {
  const SnsManagementScreen({super.key});

  @override
  State<SnsManagementScreen> createState() => _SnsManagementScreenState();
}

class _SnsManagementScreenState extends State<SnsManagementScreen> {
  Map<StoreListingApp, bool> _linked = {};

  @override
  void initState() {
    super.initState();
    _loadLinkedStatus();
  }

  Future<void> _loadLinkedStatus() async {
    final status = await SnsAppLinkTracker.linkedStatusForAll();
    if (!mounted) {
      return;
    }
    setState(() => _linked = status);
  }

  Future<void> _openApp(StoreListingApp app) async {
    final installed = await ExternalAppLauncher.canOpenApp(app);
    if (!mounted) {
      return;
    }
    if (!installed) {
      await AppInstallDialog.show(context, app: app);
      return;
    }

    final opened = await ExternalAppLauncher.openApp(app);
    if (!mounted) {
      return;
    }
    if (opened) {
      await SnsAppLinkTracker.markOpened(app);
      await _loadLinkedStatus();
      return;
    }
    await AppInstallDialog.show(context, app: app);
  }

  String _accountText(StoreListingApp app) {
    return _linked[app] == true ? '연동됨' : '연동되지 않음';
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(h(20), h(14), h(20), h(20)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: IconButton.styleFrom(
                      backgroundColor: AppTheme.fillLight,
                    ),
                    icon: const Icon(Icons.arrow_back_rounded),
                  ),
                  SizedBox(width: h(8)),
                  Expanded(
                    child: Text(
                      'SNS 계정 관리',
                      style: textTheme.headlineSmall?.copyWith(
                        fontSize: f(23),
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(18)),
              Text(
                '연동된 SNS 계정을 관리하세요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textSecondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              SizedBox(height: h(14)),
              _SnsCard(
                brand: 'Instagram',
                accountText: _accountText(StoreListingApp.instagram),
                iconBackground: const Color(0xFFF7E3EB),
                iconForeground: const Color(0xFFC13584),
                iconLabel: '▣',
                onShortcut: () => _openApp(StoreListingApp.instagram),
              ),
              SizedBox(height: h(10)),
              _SnsCard(
                brand: '당근마켓',
                accountText: _accountText(StoreListingApp.daangn),
                iconBackground: const Color(0xFFFFF0E5),
                iconForeground: const Color(0xFFFF8A3D),
                iconLabel: '🥕',
                onShortcut: () => _openApp(StoreListingApp.daangn),
              ),
              SizedBox(height: h(10)),
              _SnsCard(
                brand: '네이버 스마트플레이스',
                accountText: _accountText(StoreListingApp.naver),
                iconBackground: const Color(0xFFE8F8EC),
                iconForeground: const Color(0xFF03C75A),
                iconLabel: 'N',
                onShortcut: () => _openApp(StoreListingApp.naver),
              ),
              SizedBox(height: h(14)),
              Container(
                width: double.infinity,
                padding: EdgeInsets.symmetric(horizontal: h(12), vertical: h(12)),
                decoration: BoxDecoration(
                  color: const Color(0xFFF3F2FF),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('💡', style: TextStyle(fontSize: f(16))),
                    SizedBox(width: h(8)),
                    Expanded(
                      child: Text(
                        'SNS 계정을 연동하면 생성된 콘텐츠를 바로 공유할 수 있어요',
                        style: textTheme.bodyMedium?.copyWith(
                          color: AppTheme.primaryColor,
                          fontWeight: FontWeight.w600,
                          height: 1.4,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SnsCard extends StatelessWidget {
  const _SnsCard({
    required this.brand,
    required this.accountText,
    required this.iconBackground,
    required this.iconForeground,
    required this.iconLabel,
    required this.onShortcut,
  });

  final String brand;
  final String accountText;
  final Color iconBackground;
  final Color iconForeground;
  final String iconLabel;
  final VoidCallback onShortcut;

  static const Color _shortcutFill = Color(0xFFA49BEF);

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    return Container(
      width: double.infinity,
      padding: EdgeInsets.symmetric(horizontal: h(12), vertical: h(12)),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(h(16)),
        border: Border.all(color: AppTheme.borderColor),
        boxShadow: const [
          BoxShadow(
            color: AppTheme.shadowColor,
            blurRadius: 8,
            offset: Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: h(52),
            height: h(52),
            decoration: BoxDecoration(
              color: iconBackground,
              borderRadius: BorderRadius.circular(h(12)),
            ),
            alignment: Alignment.center,
            child: Text(
              iconLabel,
              style: TextStyle(
                color: iconForeground,
                fontSize: f(28),
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          SizedBox(width: h(12)),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  brand,
                  style: textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: AppTheme.textPrimary,
                  ),
                ),
                SizedBox(height: h(3)),
                Text(
                  accountText,
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppTheme.textSecondary,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          SizedBox(width: h(10)),
          SizedBox(
            height: h(42),
            child: OutlinedButton(
              onPressed: onShortcut,
              style: OutlinedButton.styleFrom(
                minimumSize: Size(h(98), h(42)),
                backgroundColor: _shortcutFill,
                foregroundColor: Colors.white,
                side: const BorderSide(
                  color: _shortcutFill,
                  width: 1.7,
                ),
                padding: EdgeInsets.symmetric(horizontal: h(14), vertical: h(8)),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(h(16)),
                ),
              ),
              child: Text(
                '바로가기',
                style: textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
