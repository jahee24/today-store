import 'package:flutter/material.dart';
import '../../../../config/app_theme.dart';
import '../../../../config/constants.dart';

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
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final borderColor = isSelected
    ? AppTheme.primaryColor
    : AppTheme.borderColor;

    final exampleBgColor = isSelected
    ? const Color(0xFFF3F2FF)
    : const Color(0xFFF8F8FA);

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(h(22)),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
        width: double.infinity,
        padding: EdgeInsets.fromLTRB(h(20), h(20), h(20), h(20)),
        decoration: BoxDecoration(
          color: AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(h(22)),
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
                        style: TextStyle(fontSize: f(22)),
                      ),
                      SizedBox(width: h(10)),
                      Expanded(
                        child: Text(
                          title,
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                fontSize: f(22),
                                fontWeight: FontWeight.w700,
                                color: AppTheme.textPrimary,
                              ),
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: h(10)),
                  Text(
                    description,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          fontSize: f(16),
                          color: AppTheme.textSecondary,
                          fontWeight: FontWeight.w500,
                          height: 1.45,
                        ),
                  ),
                  SizedBox(height: h(2)),
                  Text(
                    details,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          fontSize: f(16),
                          color: AppTheme.textSecondary,
                          fontWeight: FontWeight.w500,
                          height: 1.45,
                        ),
                  ),
                  if (isSelected && exampleText != null) ...[
                    SizedBox(height: h(14)),
                    Container(
                      width: double.infinity,
                      padding: EdgeInsets.symmetric(
                        horizontal: h(14),
                        vertical: h(14),
                      ),
                      decoration: BoxDecoration(
                        color: exampleBgColor,
                        borderRadius: BorderRadius.circular(h(14)),
                      ),
                      child: Text(
                        exampleText!,
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                              fontSize: f(15),
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
            SizedBox(width: h(12)),
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
    final h = (double v) => AppLayout.h(context, v);
    return Container(
      width: h(30),
      height: h(30),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          color: isSelected
              ? AppTheme.primaryColor
              : AppTheme.borderStrongColor,
          width: h(2.4),
        ),
      ),
      child: isSelected
          ? Center(
              child: Container(
                width: h(14),
                height: h(14),
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