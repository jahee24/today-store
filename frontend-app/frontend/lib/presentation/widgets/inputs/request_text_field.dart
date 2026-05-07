import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';

class RequestTextField extends StatelessWidget {
  final TextEditingController controller;
  final String hintText;
  final int maxLines;
  final ValueChanged<String>? onChanged;

  const RequestTextField({
    super.key,
    required this.controller,
    required this.hintText,
    this.maxLines = 4,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    return TextField(
      controller: controller,
      maxLines: maxLines,
      onChanged: onChanged,
      style: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w500,
        color: AppTheme.textPrimary,
        height: 1.4,
      ),
      decoration: InputDecoration(
        hintText: hintText,
        hintStyle: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w400,
          color: AppTheme.textTertiary,
        ),
        filled: true,
        fillColor: Colors.white,
        contentPadding: EdgeInsets.symmetric(
          horizontal: h(14),
          vertical: h(14),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(h(16)),
          borderSide: const BorderSide(
            color: AppTheme.borderStrongColor,
            width: 1.4,
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(h(16)),
          borderSide: const BorderSide(
            color: AppTheme.primaryColor,
            width: 1.6,
          ),
        ),
      ),
    );
  }
}