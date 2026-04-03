import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';

class ResultViewScreen extends StatelessWidget {
  const ResultViewScreen({super.key});

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

    // 실제 결과 데이터로 교체
    const selectedStyle = '감성적';
    const styleLabel = '감성적 스타일';
    const titleText = '오늘도 향기로운 하루를 선물해 드릴게요 ☕';
    const bodyText =
        '딸기가 한가득 올라간 시즌 한정 라떼, 지금 맛보지 않으면 후회할걸요? 🍓✨\n\n달콤한 딸기와 부드러운 우유의 완벽한 하모니';
    const hashtags =
        '#딸기라떼 #카페추천 #시즌한정 #맛있는카페 #오늘의메뉴 #카페스타그램';

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
                  BackArrowButton(
                    onTap: () => _handleBack(context),
                  ),
                  const SizedBox(width: 14),
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

              const SizedBox(height: 20),

              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFF2F0FF),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Text(
                  '💜 $styleLabel',
                  style: textTheme.titleMedium?.copyWith(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.primaryColor,
                  ),
                ),
              ),

              const SizedBox(height: 14),

              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 18,
                  vertical: 16,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFF3F2FF),
                  borderRadius: BorderRadius.circular(18),
                ),
                child: Text(
                  '✨ $selectedStyle 스타일로 생성된 결과',
                  style: textTheme.titleMedium?.copyWith(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.primaryColor,
                  ),
                ),
              ),

              const SizedBox(height: 18),

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
                          borderRadius: const BorderRadius.vertical(
                            top: Radius.circular(24),
                          ),
                          child: Container(
                            height: 260,
                            width: double.infinity,
                            color: const Color(0xFFF4E3E3),
                            child: const Center(
                              child: Text(
                                '📸',
                                style: TextStyle(fontSize: 54),
                              ),
                            ),
                          ),
                        ),

                        Padding(
                          padding: const EdgeInsets.fromLTRB(24, 26, 24, 24),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                titleText,
                                style: textTheme.headlineSmall?.copyWith(
                                  fontSize: 22,
                                  fontWeight: FontWeight.w700,
                                  color: AppTheme.textPrimary,
                                  height: 1.45,
                                ),
                              ),

                              const SizedBox(height: 26),

                              Text(
                                bodyText,
                                style: textTheme.bodyLarge?.copyWith(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w500,
                                  color: AppTheme.textSecondary,
                                  height: 1.75,
                                ),
                              ),

                              const SizedBox(height: 24),
                              const Divider(color: AppTheme.dividerColor),
                              const SizedBox(height: 18),

                              Text(
                                hashtags,
                                style: textTheme.bodyLarge?.copyWith(
                                  fontSize: 17,
                                  fontWeight: FontWeight.w500,
                                  color: AppTheme.primaryColor,
                                  height: 1.7,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 18),

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
                      text: '공유하기 →',
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