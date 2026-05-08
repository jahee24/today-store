import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';

class ContentBaseCard extends StatelessWidget {
  final Widget imageSlot;
  final Widget content;
  final EdgeInsetsGeometry padding;
  final double borderRadius;

  const ContentBaseCard({
    super.key,
    required this.imageSlot,
    required this.content,
    this.padding = const EdgeInsets.all(18),
    this.borderRadius = 24,
  });

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    return Container(
      width: double.infinity,
      padding: padding,
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(h(borderRadius)),
        border: Border.all(color: AppTheme.borderColor),
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
          imageSlot,
          SizedBox(width: h(12)),
          Expanded(child: content),
        ],
      ),
    );
  }
}