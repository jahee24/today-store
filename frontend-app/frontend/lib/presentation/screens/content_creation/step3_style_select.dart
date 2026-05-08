import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/providers/content_creation_provider.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';
import '../../widgets/progress/step_indicator_line.dart';
import '../../widgets/style_selector/style_option_card.dart';

class Step3StyleSelect extends ConsumerStatefulWidget {
  const Step3StyleSelect({super.key});

  @override
  ConsumerState<Step3StyleSelect> createState() => _Step3StyleSelectState();
}

class _Step3StyleSelectState extends ConsumerState<Step3StyleSelect> {
  final List<_StyleOptionData> styleOptions = const [
    _StyleOptionData(
      keyValue: '감성적',
      emoji: '💜',
      title: '감성적',
      description: '따뜻하고 스토리텔링 중심의 톤',
      details: '감성적 형용사, 이모지 활용',
      exampleText: '"오늘도 향기로운 하루를 선물해 드릴게요. 딸기가 한가득 올라간 시즌 한정 라떼..."',
    ),
    _StyleOptionData(
      keyValue: '정보제공',
      emoji: '📋',
      title: '정보제공',
      description: '객관적이고 상세한 정보 전달',
      details: '가격, 스펙, 명확한 구성',
    ),
    _StyleOptionData(
      keyValue: '전문성',
      emoji: '🏅',
      title: '전문성',
      description: '신뢰감 있는 전문적 톤',
      details: '브랜드 가치 강조, 전문 용어',
    ),
    _StyleOptionData(
      keyValue: '친근',
      emoji: '😊',
      title: '친근',
      description: '이웃 같은 편안한 대화 톤',
      details: '구어체, 공감 유도',
    ),
  ];

  void _handleBack() {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/step2');
    }
  }

  void _handleNext() {
    context.push('/step4');
  }

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      final current = ref.read(contentCreationProvider).selectedStyle;
      if (current == null) {
        ref.read(contentCreationProvider.notifier).setSelectedStyle('감성적');
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final contentState = ref.watch(contentCreationProvider);
    final selectedStyle = contentState.selectedStyle ?? '감성적';

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
                  BackArrowButton(onTap: _handleBack),
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

              const StepIndicatorLine(
                currentStep: 3,
                totalSteps: 4,
              ),

              SizedBox(height: h(28)),

              Text(
                '어떤 스타일로 만들까요?',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
              SizedBox(height: h(8)),
              Text(
                '원하는 정보 전달 톤을 선택해주세요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),

              SizedBox(height: h(20)),

              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: styleOptions.map((option) {
                      final isSelected = selectedStyle == option.keyValue;

                      return Padding(
                        padding: EdgeInsets.only(bottom: h(14)),
                        child: StyleOptionCard(
                          title: option.title,
                          emoji: option.emoji,
                          description: option.description,
                          details: option.details,
                          exampleText: option.exampleText,
                          isSelected: isSelected,
                          onTap: () {
                            ref.read(contentCreationProvider.notifier).setSelectedStyle(option.keyValue);
                          },
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),

              SizedBox(height: h(14)),

              PrimaryButton(
                text: '다음 →',
                onPressed: _handleNext,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StyleOptionData {
  final String keyValue;
  final String emoji;
  final String title;
  final String description;
  final String details;
  final String? exampleText;

  const _StyleOptionData({
    required this.keyValue,
    required this.emoji,
    required this.title,
    required this.description,
    required this.details,
    this.exampleText,
  });
}