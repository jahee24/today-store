import 'package:flutter/material.dart';

// 다음으로, 다음->, AI 콘텐츠 생성하기, 공유하기-> 버튼
class PrimaryButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final double height;
  final bool isLoading;
  final Widget? prefixIcon;
  final double? fontSize;
  final FontWeight fontWeight;

  const PrimaryButton({
    super.key,
    required this.text,
    this.onPressed,
    this.height = 70,
    this.isLoading = false,
    this.prefixIcon,
    this.fontSize = 20,
    this.fontWeight = FontWeight.w800,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: height,
      child: ElevatedButton(
        onPressed: isLoading ? null : onPressed,
        child: isLoading
        ? const SizedBox(
          width: 22,
          height: 22,
          child: CircularProgressIndicator(
            strokeWidth: 2.4,
            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
          ),
        )
        : Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (prefixIcon != null) ...[
              prefixIcon!,
              const SizedBox(width: 8),
            ],
            Text(
              text,
              style: TextStyle(
                fontSize: fontSize,
                fontWeight: fontWeight,
              )
            ),
          ],
        ),
      ),
    );
  }
}
