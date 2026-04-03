import 'package:flutter/material.dart';

class AppTheme {
  AppTheme._();

  // Brand
  static const Color primaryColor = Color(0xFF4F46E5);

  // Background
  static const Color backgroundColor = Color(0xFFF9FAFB);
  static const Color surfaceColor = Colors.white;

  // Text
  static const Color textPrimary = Color(0xFF1F2233);
  static const Color textSecondary = Color(0xFF6B7280);
  static const Color textTertiary = Color(0xFF9CA3AF);
  static const Color textHint = Color(0xFFB6BCC8);
  static const Color textOnPrimary = Colors.white;

  // Border / Divider
  static const Color borderColor = Color(0xFFE5E7EB);
  static const Color borderStrongColor = Color(0xFFD1D5DB);
  static const Color dividerColor = Color(0xFFEAECEF);

  // Neutral fill
  static const Color fillLight = Color(0xFFF3F4F6);
  static const Color fillLighter = Color(0xFFF7F8FA);
  static const Color disabledBg = Color(0xFFF3F4F6);
  static const Color disabledText = Color(0xFFB0B7C3);

  // Status / Accent
  static const Color successText = Color(0xFF5B9B4C);
  static const Color successBg = Color(0xFFEAF6E5);

  static const Color infoText = Color(0xFF6D63F6);
  static const Color infoBg = Color(0xFFEEEAFE);

  static const Color warningText = Color(0xFFE8892E);
  static const Color warningBg = Color(0xFFFFF4E8);

  static const Color dangerText = Color(0xFFE25555);
  static const Color dangerBg = Color(0xFFFFEEEE);

  static const Color shadowColor = Color(0x14000000);
  static const Color progressInactive = Color(0xFFE5E7EB);

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      fontFamily: 'NotoSansKR',
      scaffoldBackgroundColor: backgroundColor,
      
      colorScheme: const ColorScheme.light(
        primary: primaryColor,
        onPrimary: Colors.white,
        surface: surfaceColor,
        onSurface: textPrimary,
      ),

      textTheme: const TextTheme(
        headlineLarge: TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w700,
          color: textPrimary,
          height: 1.2,
          letterSpacing: -0.8,
        ),
        headlineMedium: TextStyle(
          fontSize: 28,
          fontWeight: FontWeight.w700,
          color: textPrimary,
          height: 1.25,
        ),
        headlineSmall: TextStyle(
          fontSize: 24,
          fontWeight: FontWeight.w700,
          color: textPrimary,
        ),
        titleLarge: TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w700,
          color: textPrimary,
        ),
        titleMedium: TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.w600,
          color: textPrimary,
        ),
        titleSmall: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: textPrimary,
        ),
        bodyLarge: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w400,
          color: textPrimary,
          height: 1.5,
        ),
        bodyMedium: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w400,
          color: textSecondary,
          height: 1.5,
        ),
        bodySmall: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w400,
          color: textTertiary,
        ),
        labelLarge: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: textOnPrimary,
          ),
        labelMedium: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color:textPrimary,
        ),
        labelSmall: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: textSecondary,
        )
      ),

      appBarTheme: const AppBarTheme(
        backgroundColor: backgroundColor,
        foregroundColor: textPrimary,
        elevation: 0,
        centerTitle: false,
        surfaceTintColor: Colors.transparent,
      ),

      cardTheme: CardThemeData(
        color: surfaceColor,
        elevation: 0,
        margin: EdgeInsets.zero,
        shadowColor: shadowColor,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(
            color: borderColor,
          ),
        ),
      ),

      dividerTheme: const DividerThemeData(
        color: dividerColor,
        thickness: 1,
      ),

      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surfaceColor,

        hintStyle: const TextStyle(
          fontSize: 16,
          color:textHint,
        ),

        contentPadding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 22,
        ),

        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(
            color: borderColor,
            width: 1.4,
          ),
        ),

        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(
            color: primaryColor,
            width: 1.8,
          ),
        ),
      ),

      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          elevation: 0,
          backgroundColor: primaryColor,
          foregroundColor: Colors.white,
          disabledBackgroundColor: disabledBg,
          disabledForegroundColor: disabledText,
          minimumSize: const Size(double.infinity, 72),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          textStyle: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),

      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          backgroundColor: surfaceColor,
          foregroundColor: textPrimary,
          minimumSize: const Size(double.infinity, 72),
          side: const BorderSide(
            color: borderColor,
            width: 1.4,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
        ),
      ),

      chipTheme: ChipThemeData(
        backgroundColor: surfaceColor,
        disabledColor: disabledBg,
        selectedColor: surfaceColor,
        surfaceTintColor: Colors.transparent,
        padding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 12,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(999),
          side: const BorderSide(
            color: borderStrongColor,
            width: 1.3,
          ),
        ),
        labelStyle: const TextStyle(
          fontSize: 16,
          color: textPrimary,
        ),
      ),

      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        type: BottomNavigationBarType.fixed,
        backgroundColor: surfaceColor,
        elevation: 0,
        selectedItemColor: primaryColor,
        unselectedItemColor: Color(0xFFC9CDD4),
        selectedLabelStyle: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w500,
        ),
        unselectedLabelStyle: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w500,
        ),
        showUnselectedLabels: true,
      ),

      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: primaryColor,
        circularTrackColor: Color(0xFFE8EAFE),
        linearTrackColor: progressInactive,
      ),
    );
  }
}