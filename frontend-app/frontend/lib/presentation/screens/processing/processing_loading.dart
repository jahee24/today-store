import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../data/models/processing_model.dart';
import '../../../data/providers/content_creation_provider.dart';
import '../../../data/providers/dashboard_provider.dart';

enum ProcessingMode { text, image }

class ProcessingLoadingScreen extends ConsumerStatefulWidget {
  const ProcessingLoadingScreen({
    super.key,
    this.mode = ProcessingMode.text,
  });

  final ProcessingMode mode;

  @override
  ConsumerState<ProcessingLoadingScreen> createState() =>
      _ProcessingLoadingScreenState();
}

class _ProcessingLoadingScreenState extends ConsumerState<ProcessingLoadingScreen> {
  Timer? _timer;
  Timer? _progressClimbTimer;
  String? _requestId;
  String? _taskId;
  bool _isPolling = false;

  late ProcessingUiState uiState;

  @override
  void initState() {
    super.initState();
    uiState = _initialState();
    if (widget.mode == ProcessingMode.image) {
      _startMockProgress();
      return;
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _startTextGenerationFlow();
    });
  }

  void _cancelProgressClimbTimer() {
    _progressClimbTimer?.cancel();
    _progressClimbTimer = null;
  }

  @override
  void dispose() {
    _timer?.cancel();
    _cancelProgressClimbTimer();
    super.dispose();
  }

  ProcessingUiState _initialState() {
    if (widget.mode == ProcessingMode.image) {
      return const ProcessingUiState(
        progress: 10,
        title: 'AI가 이미지를 만들고 있어요',
        subtitle: '사진 업로드 중...',
        steps: [
          ProcessingStepItem(
            label: '사진 업로드',
            status: ProcessingStepStatus.inProgress,
          ),
          ProcessingStepItem(
            label: '구도 분석',
            status: ProcessingStepStatus.pending,
          ),
          ProcessingStepItem(
            label: '이미지 합성',
            status: ProcessingStepStatus.pending,
          ),
          ProcessingStepItem(
            label: '마무리',
            status: ProcessingStepStatus.pending,
          ),
        ],
      );
    }

    return const ProcessingUiState(
      progress: 0,
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
          status: ProcessingStepStatus.pending,
        ),
      ],
    );
  }

  void _startMockProgress() {
    _timer = Timer.periodic(const Duration(milliseconds: 700), (timer) {
      final nextProgress = uiState.progress + 15;

      if (nextProgress >= 100) {
        setState(() {
          uiState = _completedState();
        });

        timer.cancel();

        Future.delayed(const Duration(milliseconds: 500), () {
          if (!mounted) return;
          if (widget.mode == ProcessingMode.image) {
            context.go('/image-result');
          } else {
            context.go('/result');
          }
        });

        return;
      }

      setState(() {
        uiState = _buildImageStateByProgress(nextProgress);
      });
    });
  }

  Future<void> _startTextGenerationFlow() async {
    try {
      final contentState = ref.read(contentCreationProvider);
      if (contentState.images.isEmpty) {
        _handleGenerationFailure('업로드된 사진이 없어요. 다시 시도해 주세요.');
        return;
      }

      final imageDescriptions = List.generate(contentState.images.length, (index) {
        final raw =
            index < contentState.descriptions.length ? contentState.descriptions[index] : '';
        final trimmed = raw.trim();
        return trimmed.isNotEmpty ? trimmed : '사진 ${index + 1}';
      });
      final concept = _buildConcept(contentState);

      final repository = ref.read(contentRepositoryProvider);
      final created = await repository.createContentRequest(
        concept: concept,
        additionalNote: contentState.extraRequest.trim().isEmpty
            ? null
            : contentState.extraRequest.trim(),
        imageDescriptions: imageDescriptions,
        imagePaths: contentState.images.map((image) => image.path).toList(),
      );

      await Future.delayed(const Duration(seconds: 4));
      if (!mounted) return;
      setState(() {
        uiState = _textStateAfterUploadOk();
      });

      final generated = await repository.generateContent(requestId: created.requestId);
      _requestId = generated.requestId;
      _taskId = generated.taskId;

      await Future.delayed(const Duration(seconds: 4));
      if (!mounted) return;
      setState(() {
        uiState = _textStateRunningBackendProgress(50);
      });
      _startPercentClimbTimer();

      _timer = Timer.periodic(const Duration(seconds: 2), (_) {
        _pollTaskStatus();
      });
      unawaited(_pollTaskStatus());
    } catch (e) {
      _timer?.cancel();
      _cancelProgressClimbTimer();
      _handleGenerationFailure(_extractErrorMessage(e));
    }
  }

  Future<void> _pollTaskStatus() async {
    final taskId = _taskId;
    if (taskId == null || taskId.isEmpty || _isPolling) {
      return;
    }
    _isPolling = true;
    try {
      final repository = ref.read(contentRepositoryProvider);
      final task = await repository.getTaskStatus(apiLogId: taskId);

      if (task.isSuccess) {
        _timer?.cancel();
        _cancelProgressClimbTimer();
        await _animateTextSuccessAndNavigate(task.result ?? '');
        return;
      }

      if (task.isError) {
        _timer?.cancel();
        _cancelProgressClimbTimer();
        if (mounted) {
          setState(() {
            uiState = uiState.copyWith(
              progress: uiState.progress.clamp(0, 99),
            );
          });
        }
        _handleGenerationFailure(task.errorMessage ?? '콘텐츠 생성에 실패했어요.');
        return;
      }

    } catch (e) {
      _timer?.cancel();
      _cancelProgressClimbTimer();
      if (mounted) {
        setState(() {
          uiState = uiState.copyWith(
            progress: uiState.progress.clamp(0, 99),
          );
        });
      }
      _handleGenerationFailure(_extractErrorMessage(e));
    } finally {
      _isPolling = false;
    }
  }

  String _buildConcept(ContentCreationState state) {
    final style = state.selectedStyle?.trim();
    if (style != null && style.isNotEmpty) {
      return style.length > 120 ? style.substring(0, 120) : style;
    }
    return '감성적';
  }

  void _handleGenerationFailure(String message) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    context.go('/step3');
  }

  /// 작업 완료(본문 `success`) + HTTP 200: 99% → 100% → 1초 뒤 결과 화면.
  Future<void> _animateTextSuccessAndNavigate(String contentId) async {
    if (!mounted) return;
    setState(() {
      uiState = ProcessingUiState(
        progress: 99,
        title: 'AI가 콘텐츠를 만들고 있어요',
        subtitle: '거의 다 완료됐어요\n스타일을 마무리하고 있어요',
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
    });
    await Future.delayed(const Duration(milliseconds: 450));
    if (!mounted) return;
    setState(() {
      uiState = _completedState();
    });
    await Future.delayed(const Duration(seconds: 1));
    if (!mounted) return;
    final requestId = _requestId ?? '';
    context.go('/result?requestId=$requestId&contentId=$contentId');
  }

  void _startPercentClimbTimer() {
    if (_progressClimbTimer != null) return;
    _progressClimbTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) {
        _cancelProgressClimbTimer();
        return;
      }
      setState(() {
        final next = (uiState.progress + 1).clamp(0, 99);
        uiState = _textStateRunningBackendProgress(next);
      });
    });
  }

  ProcessingUiState _textStateRunningBackendProgress(int progress) {
    final p = progress.clamp(0, 99);
    return ProcessingUiState(
      progress: p,
      title: 'AI가 콘텐츠를 만들고 있어요',
      subtitle: '텍스트 생성과 스타일 적용 중이에요',
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
  }

  String _extractErrorMessage(Object error) {
    if (error is DioException) {
      final data = error.response?.data;
      if (data is Map<String, dynamic>) {
        final message = data['message']?.toString();
        if (message != null && message.trim().isNotEmpty) {
          return message;
        }
      }
    }
    return '콘텐츠 생성 중 문제가 발생했어요.';
  }

  ProcessingUiState _completedState() {
    if (widget.mode == ProcessingMode.image) {
      return const ProcessingUiState(
        progress: 100,
        title: 'AI가 이미지를 만들고 있어요',
        subtitle: '거의 다 됐어요!\n결과 화면으로 이동해요...',
        steps: [
          ProcessingStepItem(
            label: '사진 업로드',
            status: ProcessingStepStatus.completed,
          ),
          ProcessingStepItem(
            label: '구도 분석',
            status: ProcessingStepStatus.completed,
          ),
          ProcessingStepItem(
            label: '이미지 합성',
            status: ProcessingStepStatus.completed,
          ),
          ProcessingStepItem(
            label: '마무리',
            status: ProcessingStepStatus.completed,
          ),
        ],
      );
    }

    return const ProcessingUiState(
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
  }

  /// `POST /api/v1/contents/request` 가 2xx로 끝난 뒤(업로드·요청 접수). 진행률 25%.
  ProcessingUiState _textStateAfterUploadOk() {
    return const ProcessingUiState(
      progress: 25,
      title: 'AI가 콘텐츠를 만들고 있어요',
      subtitle: '이미지 업로드가 완료됐어요\n이미지 분석을 진행 중이에요',
      steps: [
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
  }

  ProcessingUiState _buildImageStateByProgress(int progress) {
    if (progress < 25) {
      return ProcessingUiState(
        progress: progress,
        title: 'AI가 이미지를 만들고 있어요',
        subtitle: '사진 업로드 중...',
        steps: const [
          ProcessingStepItem(
            label: '사진 업로드',
            status: ProcessingStepStatus.inProgress,
          ),
          ProcessingStepItem(
            label: '구도 분석',
            status: ProcessingStepStatus.pending,
          ),
          ProcessingStepItem(
            label: '이미지 합성',
            status: ProcessingStepStatus.pending,
          ),
          ProcessingStepItem(
            label: '마무리',
            status: ProcessingStepStatus.pending,
          ),
        ],
      );
    }
    if (progress < 50) {
      return ProcessingUiState(
        progress: progress,
        title: 'AI가 이미지를 만들고 있어요',
        subtitle: '업로드 완료\n구도 분석 중...',
        steps: const [
          ProcessingStepItem(
            label: '사진 업로드',
            status: ProcessingStepStatus.completed,
          ),
          ProcessingStepItem(
            label: '구도 분석',
            status: ProcessingStepStatus.inProgress,
          ),
          ProcessingStepItem(
            label: '이미지 합성',
            status: ProcessingStepStatus.pending,
          ),
          ProcessingStepItem(
            label: '마무리',
            status: ProcessingStepStatus.pending,
          ),
        ],
      );
    }
    if (progress < 80) {
      return ProcessingUiState(
        progress: progress,
        title: 'AI가 이미지를 만들고 있어요',
        subtitle: '분석 완료\n이미지 합성 중...',
        steps: const [
          ProcessingStepItem(
            label: '사진 업로드',
            status: ProcessingStepStatus.completed,
          ),
          ProcessingStepItem(
            label: '구도 분석',
            status: ProcessingStepStatus.completed,
          ),
          ProcessingStepItem(
            label: '이미지 합성',
            status: ProcessingStepStatus.inProgress,
          ),
          ProcessingStepItem(
            label: '마무리',
            status: ProcessingStepStatus.pending,
          ),
        ],
      );
    }
    return ProcessingUiState(
      progress: progress,
      title: 'AI가 이미지를 만들고 있어요',
      subtitle: '합성 완료\n마무리 중...',
      steps: const [
        ProcessingStepItem(
          label: '사진 업로드',
          status: ProcessingStepStatus.completed,
        ),
        ProcessingStepItem(
          label: '구도 분석',
          status: ProcessingStepStatus.completed,
        ),
        ProcessingStepItem(
          label: '이미지 합성',
          status: ProcessingStepStatus.completed,
        ),
        ProcessingStepItem(
          label: '마무리',
          status: ProcessingStepStatus.inProgress,
        ),
      ],
    );
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
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(flex: 2),
              Align(
                alignment: Alignment.center,
                child: _ProgressCircle(progress: uiState.progress),
              ),
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
  const _ProgressCircle({required this.progress});

  final int progress;

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
  const _ProcessingStepCard({required this.steps});

  final List<ProcessingStepItem> steps;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
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
  const _ProcessingStepRow({required this.step});

  final ProcessingStepItem step;

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
