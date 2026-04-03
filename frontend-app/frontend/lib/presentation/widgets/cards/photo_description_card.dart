import 'dart:io';

import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import 'content_base_card.dart';

class PhotoDescriptionCard extends StatelessWidget {
  final String label;
  final TextEditingController controller;
  final String hintText;
  final String imagePath;

  const PhotoDescriptionCard({
    super.key,
    required this.label,
    required this.controller,
    required this.hintText,
    required this.imagePath,
  });

  @override
  Widget build(BuildContext context) {
    return ContentBaseCard(
      imageSlot: Container(
        width: 92,
        height: 92,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(18),
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
              fontSize: 15,
              fontWeight: FontWeight.w700,
              color: AppTheme.textSecondary,
            ),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: controller,
            maxLines: 2,
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
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 18,
                vertical: 16,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(18),
                borderSide: const BorderSide(
                  color:AppTheme.borderStrongColor,
                  width: 1.5,
                ),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(18),
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