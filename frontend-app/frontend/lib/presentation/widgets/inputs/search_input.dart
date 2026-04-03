import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';

// 검색 전용 입력창
class SearchInput extends StatelessWidget {
  final TextEditingController? controller;
  final String hintText;
  final ValueChanged<String>? onChanged;

  const SearchInput({
    super.key,
    this.controller,
    this.hintText = '콘텐츠 검색...',
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      onChanged: onChanged,
      decoration: InputDecoration(
        hintText: hintText,
        prefixIcon: const Icon(
          Icons.search,
          color: AppTheme.textTertiary,
        ),
      ),
    );
  }
}