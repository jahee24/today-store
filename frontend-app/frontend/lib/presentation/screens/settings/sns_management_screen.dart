import 'package:flutter/material.dart';

import '../../../config/app_theme.dart';

class SnsManagementScreen extends StatelessWidget {
  const SnsManagementScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
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
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'SNS 계정 관리',
                      style: textTheme.headlineSmall?.copyWith(
                        fontSize: 23,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                '연동된 SNS 계정을 관리하세요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textSecondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 18),
              _SnsCard(
                brand: 'Instagram',
                accountText: '@delicious_cafe',
                isConnected: true,
                iconBackground: const Color(0xFFF7E3EB),
                iconForeground: const Color(0xFFC13584),
                iconLabel: '▣',
              ),
              const SizedBox(height: 12),
              _SnsCard(
                brand: 'Facebook',
                accountText: '연동되지 않음',
                isConnected: false,
                iconBackground: const Color(0xFFE9EEFF),
                iconForeground: const Color(0xFF4267B2),
                iconLabel: 'f',
              ),
              const SizedBox(height: 12),
              _SnsCard(
                brand: '네이버 블로그',
                accountText: '연동되지 않음',
                isConnected: false,
                iconBackground: const Color(0xFFE8F8EC),
                iconForeground: const Color(0xFF2DB400),
                iconLabel: 'N',
              ),
              const SizedBox(height: 18),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                decoration: BoxDecoration(
                  color: const Color(0xFFF3F2FF),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('💡', style: TextStyle(fontSize: 16)),
                    const SizedBox(width: 8),
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
    required this.isConnected,
    required this.iconBackground,
    required this.iconForeground,
    required this.iconLabel,
  });

  final String brand;
  final String accountText;
  final bool isConnected;
  final Color iconBackground;
  final Color iconForeground;
  final String iconLabel;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(18),
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
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: iconBackground,
              borderRadius: BorderRadius.circular(14),
            ),
            alignment: Alignment.center,
            child: Text(
              iconLabel,
              style: TextStyle(
                color: iconForeground,
                fontSize: 30,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          const SizedBox(width: 14),
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
                const SizedBox(height: 3),
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
          const SizedBox(width: 12),
          SizedBox(
            height: 44,
            child: OutlinedButton(
              onPressed: () {},
              // Connected(연동 해제): text color == border color
              // Not connected(연동하기): border color == fill color, text color white
              style: OutlinedButton.styleFrom(
                minimumSize: const Size(110, 44),
                backgroundColor: isConnected
                    ? AppTheme.surfaceColor
                    : const Color(0xFFA49BEF),
                foregroundColor: isConnected
                    ? const Color(0xFF7B6FE2)
                    : Colors.white,
                side: BorderSide(
                  color: isConnected
                      ? const Color(0xFF7B6FE2)
                      : const Color(0xFFA49BEF),
                  width: 1.7,
                ),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
              child: Text(
                isConnected ? '연동 해제' : '연동하기',
                style: textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: isConnected ? const Color(0xFF7B6FE2) : Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
