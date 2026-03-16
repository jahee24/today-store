import 'package:flutter/material.dart';

// 재생성 버튼
class SecondaryButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final double height;
  final Widget? prefixIcon;

  const SecondaryButton({
    super.key,
    required this.text,
    this.onPressed,
    this.height = 72,
    this.prefixIcon,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: height,
      child: OutlinedButton(
        onPressed: onPressed,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (prefixIcon != null) ...[prefixIcon!, const SizedBox(width: 8)],
            Text(text),
          ],
        ),
      ),
    );
  }
}
