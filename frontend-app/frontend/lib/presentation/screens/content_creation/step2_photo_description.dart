import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/providers/content_creation_provider.dart';

import '../../../config/app_theme.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';
import '../../widgets/cards/photo_description_card.dart';
import '../../widgets/inputs/request_text_field.dart';
import '../../widgets/progress/step_indicator_line.dart';

class Step2PhotoDescription extends ConsumerStatefulWidget {
  const Step2PhotoDescription({super.key});

  @override
  ConsumerState<Step2PhotoDescription> createState() => _Step2PhotoDescriptionState();
}

class _Step2PhotoDescriptionState extends ConsumerState<Step2PhotoDescription> {
  late final List<TextEditingController> _photoControllers;
  final TextEditingController _extraRequestController = TextEditingController();

  @override
  void initState() {
    super.initState();
    
    final contentState = ref.read(contentCreationProvider);

    _photoControllers = List.generate(
      contentState.images.length,
      (index) => TextEditingController(
        text: index < contentState.descriptions.length
        ? contentState.descriptions[index]
        : '' ,
      ),
    );

    _extraRequestController.text = contentState.extraRequest;
  }

  @override
  void dispose() {
    for (final controller in _photoControllers) {
      controller.dispose();
    }
    _extraRequestController.dispose();
    super.dispose();
  }

  void _handleBack() {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/step1');
    }
  }

  void _handleNext() {
    context.go('/step3');
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final contentState = ref.watch(contentCreationProvider);
    final images = contentState.images;

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 18, 24, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BackArrowButton(onTap: _handleBack),
                  const SizedBox(width: 14),
                  Text(
                    '콘텐츠 만들기',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 26),

              const StepIndicatorLine(
                currentStep: 2,
                totalSteps: 4,
              ),

              const SizedBox(height: 34),

              Text(
                '사진을 설명해주세요',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'AI가 더 정확한 콘텐츠를 만들어요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),

              const SizedBox(height: 24),

              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      ...List.generate(images.length, (index) {
                        return Padding(
                          padding: EdgeInsets.only(
                            bottom: index == images.length - 1 ? 0 : 18,
                          ),
                          child: PhotoDescriptionCard(
                            label: '사진 ${index + 1}',
                            controller: _photoControllers[index],
                            hintText: '사진에 대한 설명을 입력해주세요',
                            imagePath: images[index].path,
                            onChanged: (value) {
                              ref.read(contentCreationProvider.notifier).setDescription(index, value);
                            }
                          ),
                        );
                      }),

                      const SizedBox(height: 24),

                      Align(
                        alignment: Alignment.centerLeft,
                        child: Text(
                          '추가 요청사항 (선택)',
                          style: textTheme.titleMedium?.copyWith(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ),
                      const SizedBox(height: 10),

                      RequestTextField(
                        controller: _extraRequestController,
                        hintText: '예) 2030 여성 타겟, 밝은 톤으로 작성해줘',
                        maxLines: 4,
                        onChanged: (value) {
                          ref.read(contentCreationProvider.notifier).setExtraRequest(value);
                        },
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 20),

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