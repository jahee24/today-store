import 'dart:io';

import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import 'content_base_card.dart';

class PhotoDescriptionCard extends StatelessWidget {
  final String label;
  final TextEditingController controller;
  final String hintText;
  final String imagePath;
  final ValueChanged<String>? onChanged;

  const PhotoDescriptionCard({
    super.key,
    required this.label,
    required this.controller,
    required this.hintText,
    required this.imagePath,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    return ContentBaseCard(
      imageSlot: Container(
        width: h(84),
        height: h(84),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(h(16)),
          color: AppTheme.fillLight,
          image: DecorationImage(
            image: FileImage(File(imagePath)),
            fit: BoxFit.cover,
          ),
        ),
      ), 
      content: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontSize: f(15),
              fontWeight: FontWeight.w700,
              color: AppTheme.textSecondary,
            ),
          ),
          SizedBox(height: h(8)),
          TextField(
            controller: controller,
            maxLines: 2,
            onChanged: onChanged,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
              color: AppTheme.textPrimary,
              height: 1.45,
            ),
            decoration: InputDecoration(
              hintText: hintText,
              hintStyle: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w400,
                color: AppTheme.textTertiary,
              ),
              filled: true,
              fillColor: AppTheme.surfaceColor,
              contentPadding: EdgeInsets.symmetric(
                horizontal: h(14),
                vertical: h(14),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(h(16)),
                borderSide: const BorderSide(
                  color:AppTheme.borderStrongColor,
                  width: 1.5,
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
          ),
        ],
      ),
    );
  }
}