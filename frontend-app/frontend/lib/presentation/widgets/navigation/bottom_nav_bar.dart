import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../buttons/creation_action_button.dart';

class AppBottomNavBar extends StatelessWidget {
  final int currentIndex;

  const AppBottomNavBar({super.key, required this.currentIndex});

  Future<void> _showCreateOptions(BuildContext context) async {
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (sheetContext) {
        final h = (double v) => AppLayout.h(sheetContext, v);
        final f = (double v) => AppLayout.f(sheetContext, v);
        return SafeArea(
          child: Padding(
            padding: EdgeInsets.fromLTRB(h(14), 0, h(14), h(14)),
            child: Container(
              width: double.infinity,
              padding: EdgeInsets.all(h(20)),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [AppTheme.primaryColor, AppTheme.infoText],
                ),
                borderRadius: BorderRadius.circular(h(24)),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '새 홍보 콘텐츠 만들기',
                    style: TextStyle(
                      color: AppTheme.textOnPrimary,
                      fontWeight: FontWeight.w700,
                      fontSize: f(23),
                    ),
                  ),
                  SizedBox(height: h(4)),
                  Text(
                    'AI가 알아서 만들어 드려요',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.85),
                      fontWeight: FontWeight.w500,
                      fontSize: f(17),
                    ),
                  ),
                  SizedBox(height: h(14)),
                  Row(
                    children: [
                      Expanded(
                        child: CreationActionButton(
                          label: '+ 문구 생성',
                          onPressed: () {
                            Navigator.of(sheetContext).pop();
                            context.push('/step1');
                          },
                        ),
                      ),
                      SizedBox(width: h(10)),
                      Expanded(
                        child: CreationActionButton(
                          label: '+ 이미지 생성',
                          filled: false,
                          onPressed: () {
                            Navigator.of(sheetContext).pop();
                            context.push('/image-step1');
                          },
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  void _handleTap(BuildContext context, int index) {
    switch (index) {
      case 0:
        context.go('/dashboard');
        break;
      case 1:
        _showCreateOptions(context);
        break;
      case 2:
        context.go('/history');
        break;
      case 3:
        context.go('/settings');
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return BottomNavigationBar(
      currentIndex: currentIndex,
      onTap: (index) => _handleTap(context, index),
      items: const [
        BottomNavigationBarItem(icon: Icon(Icons.home_rounded), label: '홈'),
        BottomNavigationBarItem(
          icon: Icon(Icons.add_box_outlined),
          label: '만들기',
        ),
        BottomNavigationBarItem(icon: Icon(Icons.history), label: '이력'),
        BottomNavigationBarItem(
          icon: Icon(Icons.settings_outlined),
          activeIcon: Icon(Icons.settings_rounded),
          label: '설정',
        ),
      ],
    );
  }
}
