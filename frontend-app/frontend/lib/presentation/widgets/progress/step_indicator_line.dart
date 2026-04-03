import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';

class StepIndicatorLine extends StatelessWidget {
  final int currentStep;
  final int totalSteps;

  final double completedDotSize;
  final double currentDotSize;
  final double pendingDotSize;
  final double lineWidth;
  final double lineHeight;

  const StepIndicatorLine({
    super.key,
    required this.currentStep,
    this.totalSteps = 4,
    this.completedDotSize = 15,
    this.currentDotSize = 18,
    this.pendingDotSize = 15,
    this.lineWidth = 44,
    this.lineHeight = 4,
  }) : assert(currentStep >= 1),
  assert(totalSteps >= 2),
  assert(currentStep <= totalSteps);

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: List.generate(totalSteps * 2 - 1, (index) {
          if (index.isEven) {
            final step = (index ~/ 2) + 1;
            return _buildDot(step);
          } else {
            final lineIndex = (index ~/ 2) + 1;
            return _buildLine(lineIndex);
          }
        }),
      ),
    );
  }

  Widget _buildDot(int step) {
    final bool isCompleted = step < currentStep;
    final bool isCurrent = step == currentStep;

    final double size = isCurrent
    ? currentDotSize
    : isCompleted
    ? completedDotSize
    : pendingDotSize;

    final Color color = isCurrent
    ? AppTheme.primaryColor
    : isCompleted
    ? AppTheme.successText
    : AppTheme.borderStrongColor;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOut,
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: color,
      ),
    );
  }

  Widget _buildLine(int lineIndex) {
    final bool isCompletedLine = lineIndex < currentStep;
    final bool isActiveTransitionLine = lineIndex == currentStep - 1;

    if (isCompletedLine) {
      return Container(
        width: lineWidth,
        height: lineHeight,
        margin: const EdgeInsets.symmetric(horizontal: 2),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(999),
          color: AppTheme.successText,
        ),
      );
    }

    if (isActiveTransitionLine) {
      return Container(
        width: lineWidth,
        height: lineHeight,
        margin: const EdgeInsets.symmetric(horizontal: 2),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(999),
          gradient: LinearGradient(
            begin: Alignment.centerLeft,
            end: Alignment.centerRight,
            colors: [
              AppTheme.successText,
              AppTheme.primaryColor,
              AppTheme.borderColor,
            ],
            stops: const [0.0, 0.55, 1.0],
          ),
        ),
      );
    }

    return Container(
      width: lineWidth,
      height: lineHeight,
      margin: const EdgeInsets.symmetric(horizontal: 2),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(999),
        color: AppTheme.borderColor,
      ),
    );
  }
}