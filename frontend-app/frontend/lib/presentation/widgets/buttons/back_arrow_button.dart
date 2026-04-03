import 'package:flutter/material.dart';

import '../../../config/app_theme.dart';

class BackArrowButton extends StatelessWidget {
  final VoidCallback onTap;
  final double size;
  final double iconSize;
  final Color? backgroundColor;
  final Color? iconColor;
  final double borderRadius;

  const BackArrowButton({
    super.key,
    required this.onTap,
    this.size = 48,
    this.iconSize = 20,
    this.backgroundColor,
    this.iconColor,
    this.borderRadius = 16,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(borderRadius),
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: backgroundColor ?? AppTheme.borderColor,
          borderRadius: BorderRadius.circular(borderRadius),
        ),
        child: Icon(
          Icons.arrow_back_ios_new_rounded,
          size: iconSize,
          color: iconColor ?? AppTheme.textPrimary,
        ),
      ),
    );
  }
}