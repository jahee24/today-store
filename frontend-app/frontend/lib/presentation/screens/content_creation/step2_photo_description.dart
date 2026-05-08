import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/providers/content_creation_provider.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
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
    context.push('/step3');
  }

  bool _everyPhotoHasDescription(int imageCount, List<String> descriptions) {
    if (imageCount == 0) return false;
    for (var i = 0; i < imageCount; i++) {
      final text = i < descriptions.length ? descriptions[i] : '';
      if (text.trim().isEmpty) return false;
    }
    return true;
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final contentState = ref.watch(contentCreationProvider);
    final images = contentState.images;
    final canProceed = _everyPhotoHasDescription(
      images.length,
      contentState.descriptions,
    );

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
                currentStep: 2,
                totalSteps: 4,
              ),

              SizedBox(height: h(28)),

              Text(
                '사진을 설명해주세요',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
              ),
              SizedBox(height: h(8)),
              Text(
                'AI가 더 정확한 콘텐츠를 만들어요',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),

              SizedBox(height: h(20)),

              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      ...List.generate(images.length, (index) {
                        return Padding(
                          padding: EdgeInsets.only(
                            bottom: index == images.length - 1 ? 0 : h(14),
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

                      SizedBox(height: h(20)),

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
                      SizedBox(height: h(8)),

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

              SizedBox(height: h(18)),

              PrimaryButton(
                text: '다음 →',
                onPressed: canProceed ? _handleNext : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}