enum ProcessingStepStatus {
  completed,
  inProgress,
  pending,
}

class ProcessingStepItem {
  final String label;
  final ProcessingStepStatus status;

  const ProcessingStepItem({
    required this.label,
    required this.status,
  });

  ProcessingStepItem copyWith({
    String? label,
    ProcessingStepStatus? status,
  }) {
    return ProcessingStepItem(
      label: label ?? this.label,
      status: status ?? this.status,
    );
  }
}

class ProcessingUiState {
  final int progress;
  final String title;
  final String subtitle;
  final List<ProcessingStepItem> steps;

  const ProcessingUiState({
    required this.progress,
    required this.title,
    required this.subtitle,
    required this.steps,
  });

  ProcessingUiState copyWith({
    int? progress,
    String? title,
    String? subtitle,
    List<ProcessingStepItem>? steps,
  }) {
    return ProcessingUiState(
      progress: progress ?? this.progress,
      title: title ?? this.title,
      subtitle: subtitle ?? this.subtitle,
      steps: steps ?? this.steps,
    );
  }
}