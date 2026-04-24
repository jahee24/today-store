import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';

class ImageResultViewScreen extends StatelessWidget {
  const ImageResultViewScreen({super.key});

  void _handleBack(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/dashboard');
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    const tiles = <_MockImageTile>[
      _MockImageTile(Color(0xFFE8DEF8), Icons.image_outlined),
      _MockImageTile(Color(0xFFDCEEF5), Icons.brush_outlined),
      _MockImageTile(Color(0xFFF5E6DC), Icons.storefront_outlined),
      _MockImageTile(Color(0xFFE8F5E9), Icons.auto_awesome_outlined),
      _MockImageTile(Color(0xFFFCE4EC), Icons.palette_outlined),
      _MockImageTile(Color(0xFFE3F2FD), Icons.photo_camera_outlined),
    ];

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 18, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BackArrowButton(onTap: () => _handleBack(context)),
                  const SizedBox(width: 14),
                  Text(
                    '이미지 생성 결과',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                '다양한 구도로 변환된 매장 사진이에요.',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 22),
              Expanded(
                child: GridView.builder(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    mainAxisSpacing: 14,
                    crossAxisSpacing: 14,
                    childAspectRatio: 0.92,
                  ),
                  itemCount: tiles.length,
                  itemBuilder: (context, index) {
                    final t = tiles[index];
                    return ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: Material(
                        color: t.color,
                        child: InkWell(
                          onTap: () {},
                          child: Center(
                            child: Icon(
                              t.icon,
                              size: 48,
                              color: AppTheme.textSecondary.withOpacity(0.65),
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    flex: 3,
                    child: SizedBox(
                      height: 68,
                      child: OutlinedButton(
                        onPressed: () {},
                        style: OutlinedButton.styleFrom(
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
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    flex: 6,
                    child: PrimaryButton(
                      text: '앨범에 저장',
                      onPressed: () {},
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

class _MockImageTile {
  const _MockImageTile(this.color, this.icon);
  final Color color;
  final IconData icon;
}
