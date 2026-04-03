import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../data/models/processing_model.dart';

class ProcessingLoadingScreen extends StatefulWidget {
  const ProcessingLoadingScreen({super.key});

  @override
  State<ProcessingLoadingScreen> createState() =>
      _ProcessingLoadingScreenState();
}

class _ProcessingLoadingScreenState extends State<ProcessingLoadingScreen> {
    Timer? _timer;

    // 나중에 백엔드 진행률 값으로 교체
    ProcessingUiState uiState = const ProcessingUiState(
        progress: 10,
        title: 'AI가 콘텐츠를 만들고 있어요',
        subtitle: '이미지 업로드 중...',
        steps: [
            ProcessingStepItem(
                label: '이미지 업로드',
                status: ProcessingStepStatus.inProgress,
            ),
            ProcessingStepItem(
                label: '이미지 분석',
                status: ProcessingStepStatus.pending,
            ),
            ProcessingStepItem(
                label: '텍스트 생성 중',
                status: ProcessingStepStatus.pending,
            ),
            ProcessingStepItem(
                label: '스타일 적용',
                status: ProcessingStepStatus.pending
            ),
        ],
    );
    
    @override
    void initState() {
        super.initState();
        _startMockProgress();
    }

    @override
    void dispose() {
        _timer?.cancel();
        super.dispose();
    }

    void _startMockProgress() {
        _timer = Timer.periodic(const Duration(milliseconds: 700), (timer) {
            final nextProgress = uiState.progress + 15;

            if (nextProgress >= 100) {
                setState(() {
                    uiState = const ProcessingUiState(
                        progress: 100, 
                        title: 'AI가 콘텐츠를 만들고 있어요', 
                        subtitle: '스타일 적용 완료\n결과를 정리하고 있어요...', 
                        steps: [
                            ProcessingStepItem(
                                label: '이미지 업로드', 
                                status: ProcessingStepStatus.completed,
                            ),
                            ProcessingStepItem(
                                label: '이미지 분석', 
                                status: ProcessingStepStatus.completed,
                            ),
                            ProcessingStepItem(
                                label: '텍스트 생성 중', 
                                status: ProcessingStepStatus.completed,
                            ),
                            ProcessingStepItem(
                                label: '스타일 적용', 
                                status: ProcessingStepStatus.completed,
                            ),
                        ],
                    );
                });

                timer.cancel();

                Future.delayed(const Duration(milliseconds: 500), () {
                    if (!mounted) return;
                    context.go('/result');
                });

                return;
            }

            setState(() {
                uiState = _buildStateByProgress(nextProgress);
            });
        });
    }

    ProcessingUiState _buildStateByProgress(int progress) {
        if (progress < 25) {
            return ProcessingUiState(
                progress: progress, 
                title: 'AI가 콘텐츠를 만들고 있어요', 
                subtitle: '이미지 업로드 중...', 
                steps: const [
                    ProcessingStepItem(
                        label: '이미지 업로드', 
                        status: ProcessingStepStatus.inProgress,
                    ),
                    ProcessingStepItem(
                        label: '이미지 분석', 
                        status: ProcessingStepStatus.pending,
                    ),
                    ProcessingStepItem(
                        label: '텍스트 생성 중', 
                        status: ProcessingStepStatus.pending,
                    ),
                    ProcessingStepItem(
                        label: '스타일 적용', 
                        status: ProcessingStepStatus.pending,
                    ),
                ],
            );
        } else if (progress < 50) {
            return ProcessingUiState(
                progress: progress, 
                title: 'AI가 콘텐츠를 만들고 있어요', 
                subtitle: '이미지 업로드 완료\n이미지 분석 중...', 
                steps: const [
                    ProcessingStepItem(
                        label: '이미지 업로드', 
                        status: ProcessingStepStatus.completed,
                    ),
                    ProcessingStepItem(
                        label: '이미지 분석', 
                        status: ProcessingStepStatus.inProgress,
                    ),
                    ProcessingStepItem(
                        label: '텍스트 생성 중', 
                        status: ProcessingStepStatus.pending,
                    ),
                    ProcessingStepItem(
                        label: '스타일 적용', 
                        status: ProcessingStepStatus.pending,
                    ),
                ],
            );
        } else if (progress < 80) {
            return ProcessingUiState(
                progress: progress, 
                title: 'AI가 콘텐츠를 만들고 있어요', 
                subtitle: '이미지 분석 완료\n텍스트 생성 중...', 
                steps: const [
                    ProcessingStepItem(
                        label: '이미지 업로드', 
                        status: ProcessingStepStatus.completed,
                    ),
                    ProcessingStepItem(
                        label: '이미지 분석', 
                        status: ProcessingStepStatus.completed,
                    ),
                    ProcessingStepItem(
                        label: '텍스트 생성 중', 
                        status: ProcessingStepStatus.inProgress,
                    ),
                    ProcessingStepItem(
                        label: '스타일 적용', 
                        status: ProcessingStepStatus.pending,
                    ),
                ],
            );
        } else {
            return ProcessingUiState(
                progress: progress, 
                title: 'AI가 콘텐츠를 만들고 있어요', 
                subtitle: '텍스트 생성 완료\n스타일 적용 중...', 
                steps: const [
                    ProcessingStepItem(
                        label: '이미지 업로드', 
                        status: ProcessingStepStatus.completed,
                    ),
                    ProcessingStepItem(
                        label: '이미지 분석', 
                        status: ProcessingStepStatus.completed,
                    ),
                    ProcessingStepItem(
                        label: '텍스트 생성 중', 
                        status: ProcessingStepStatus.completed,
                    ),
                    ProcessingStepItem(
                        label: '스타일 적용', 
                        status: ProcessingStepStatus.inProgress,
                    ),
                ],
            );
        }
    }

    @override
    Widget build(BuildContext context) {
        final textTheme = Theme.of(context).textTheme;
            return Scaffold(
                backgroundColor: AppTheme.backgroundColor,
                body: SafeArea(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(24, 24, 24, 32),
                      child: Column(
                        children: [
                            const Spacer(flex: 2),
                            
                            _ProgressCircle(progress: uiState.progress),
                            
                            const SizedBox(height: 36),
                            
                            Text(
                                uiState.title,
                                textAlign: TextAlign.center,
                                style: textTheme.headlineMedium?.copyWith(
                                fontWeight: FontWeight.w700,
                                color: AppTheme.textPrimary,
                                ),
                            ),
                            
                            const SizedBox(height: 16),
                            
                            Text(
                                uiState.subtitle,
                                textAlign: TextAlign.center,
                                style: textTheme.bodyLarge?.copyWith(
                                    color: AppTheme.textTertiary,
                                    fontWeight: FontWeight.w500,
                                    height: 1.5,
                                ),
                            ),

                            const SizedBox(height: 40),

                            _ProcessingStepCard(
                                steps: uiState.steps,
                            ),
                            
                            const Spacer(flex: 3),
                            ],
                        ),
                    ),
                ),
            );
    }
}

class _ProgressCircle extends StatelessWidget {
  final int progress;

  const _ProgressCircle({
    required this.progress,
  });

  @override
  Widget build(BuildContext context) {
    final safeProgress = progress.clamp(0, 100);

    return SizedBox(
      width: 140,
      height: 140,
      child: Stack(
        alignment: Alignment.center,
        children: [
          SizedBox(
            width: 140,
            height: 140,
            child: CircularProgressIndicator(
              value: safeProgress / 100,
              strokeWidth: 10,
              backgroundColor: const Color(0xFFE9EAF6),
              valueColor: const AlwaysStoppedAnimation<Color>(
                AppTheme.primaryColor,
              ),
              strokeCap: StrokeCap.round,
            ),
          ),
          Text(
            '$safeProgress%',
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  fontSize: 34,
                  fontWeight: FontWeight.w700,
                  color: AppTheme.primaryColor,
                ),
          ),
        ],
      ),
    );
  }
}

class _ProcessingStepCard extends StatelessWidget {
  final List<ProcessingStepItem> steps;

  const _ProcessingStepCard({
    required this.steps,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 360,
      constraints: const BoxConstraints(maxWidth: double.infinity),
      padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 22),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        children: steps
            .map(
              (step) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 10),
                child: _ProcessingStepRow(step: step),
              ),
            )
            .toList(),
      ),
    );
  }
}

class _ProcessingStepRow extends StatelessWidget {
  final ProcessingStepItem step;

  const _ProcessingStepRow({
    required this.step,
  });

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    late final Widget leading;
    late final Color textColor;
    late final FontWeight fontWeight;

    switch (step.status) {
      case ProcessingStepStatus.completed:
        leading = Text(
          '✓',
          style: textTheme.titleLarge?.copyWith(
            color: AppTheme.successText,
            fontWeight: FontWeight.w700,
          ),
        );
        textColor = AppTheme.textSecondary;
        fontWeight = FontWeight.w600;
        break;

      case ProcessingStepStatus.inProgress:
        leading = Container(
          width: 18,
          height: 18,
          decoration: const BoxDecoration(
            color: AppTheme.primaryColor,
            shape: BoxShape.circle,
          ),
        );
        textColor = AppTheme.textPrimary;
        fontWeight = FontWeight.w700;
        break;

      case ProcessingStepStatus.pending:
        leading = Container(
          width: 18,
          height: 18,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(
              color: AppTheme.borderStrongColor,
              width: 1.6,
            ),
          ),
        );
        textColor = AppTheme.textHint;
        fontWeight = FontWeight.w500;
        break;
    }

    return Row(
      children: [
        SizedBox(
          width: 22,
          child: Center(child: leading),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            step.label,
            style: textTheme.titleMedium?.copyWith(
              fontSize: 16,
              color: textColor,
              fontWeight: fontWeight,
            ),
          ),
        ),
      ],
    );
  }
}