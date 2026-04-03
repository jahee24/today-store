import 'package:flutter/material.dart';
import '../../../../config/app_theme.dart';

// 스타일 옵션 선택 카드
class StyleOptionCard extends StatelessWidget {
    final String title;
  final String emoji;
  final String description;
  final String details;
  final String? exampleText;
  final bool isSelected;
  final VoidCallback onTap;

  const StyleOptionCard({
    super.key,
    required this.title,
    required this.emoji,
    required this.description,
    required this.details,
    this.exampleText,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final borderColor = isSelected
    ? AppTheme.primaryColor
    : AppTheme.borderColor;

    final exampleBgColor = isSelected
    ? const Color(0xFFF3F2FF)
    : const Color(0xFFF8F8FA);

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
        width: double.infinity,
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 24),
        decoration: BoxDecoration(
          color: AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: borderColor,
            width: isSelected ? 2.6 : 1.2,
          ),
          boxShadow: const [
            BoxShadow(
              color: AppTheme.shadowColor,
              blurRadius: 12,
              offset: Offset(0, 3),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        emoji,
                        style: const TextStyle(fontSize: 22),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          title,
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                fontSize: 22,
                                fontWeight: FontWeight.w700,
                                color: AppTheme.textPrimary,
                              ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text(
                    description,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          fontSize: 16,
                          color: AppTheme.textSecondary,
                          fontWeight: FontWeight.w500,
                          height: 1.45,
                        ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    details,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          fontSize: 16,
                          color: AppTheme.textSecondary,
                          fontWeight: FontWeight.w500,
                          height: 1.45,
                        ),
                  ),
                  if (isSelected && exampleText != null) ...[
                    const SizedBox(height: 18),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 16,
                      ),
                      decoration: BoxDecoration(
                        color: exampleBgColor,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Text(
                        exampleText!,
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                              fontSize: 15,
                              fontStyle: FontStyle.italic,
                              color: const Color(0xFF6E6E78),
                              fontWeight: FontWeight.w500,
                              height: 1.5,
                            ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(width: 16),
            _SelectionIndicator(isSelected: isSelected),
          ],
        ),
      ),
    );
  }
}

class _SelectionIndicator extends StatelessWidget {
  final bool isSelected;

  const _SelectionIndicator({
    required this.isSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          color: isSelected
              ? AppTheme.primaryColor
              : AppTheme.borderStrongColor,
          width: 2.6,
        ),
      ),
      child: isSelected
          ? Center(
              child: Container(
                width: 16,
                height: 16,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppTheme.primaryColor,
                ),
              ),
            )
          : null,
    );
  }
}