import 'package:flutter/material.dart';

class AppColors {
  // Modern Auspicious Aura theme - Vibrant purples, Emerald greens, and bright golds
  static const Color primary = Color(
    0xFF8E24AA,
  ); // Vibrant Purple for spirituality and power
  static const Color secondary = Color(
    0xFF00897B,
  ); // Emerald Green for wealth and prosperity (เขียวเหนี่ยวทรัพย์)
  static const Color accent = Color(0xFFFFD700); // Bright Gold for wealth
  static const Color buttonGradientStart = Color(
    0xFF9C27B0,
  ); // Purple gradient start
  static const Color buttonGradientEnd = Color(0xFF00897B); // Emerald gradient end
  static const Color bgDark = Color(
    0xFFFDFCFE,
  ); // Very Bright Aura background
  static const Color bgDarker = Color(
    0xFFFAF5FF,
  ); // Vibrant light lavender background
  static const Color textLight = Color(
    0xFF212121,
  ); // Deep charcoal for modern clarity
  static const Color textGray = Color(0xFF757575); // Medium gray text
  static const Color inputText = Color(
    0xFF212121,
  ); // Dark for input fields
  static const Color inputBackground = Color(
    0xFFFFFFFF,
  ); // Pure white for input backgrounds
  static const Color success = Color(0xFF4CAF50); // Vibrant green for success

  static const Color glass = Color(0xCCFFFFFF); // White glass effect
  static const Color glassBorder = Color(0x338E24AA); // Purple border

  static const LinearGradient primaryGradient = LinearGradient(
    colors: [Color(0xFF9C27B0), Color(0xFF00897B)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient goldGradient = LinearGradient(
    colors: [
      Color(0xFFFFD700), // Bright gold
      Color(0xFFFFE082), // Light gold
      Color(0xFFFFD700), // Bright gold
      Color(0xFFFFB300), // Amber gold
    ],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const List<Color> avatarBorders = [
    Color(0xFFFF6D00), // Sunday - Deep Orange (Replaced Red)
    Color(0xFFFFEB3B), // Monday - Yellow
    Color(0xFFF06292), // Tuesday - Light Pink
    Color(0xFF4CAF50), // Wednesday - Green
    Color(0xFFFF9800), // Thursday - Orange
    Color(0xFF2196F3), // Friday - Blue
    Color(0xFF9C27B0), // Saturday - Purple
  ];

  static const LinearGradient magicalPurpleGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF6A11CB), // Radiant Purple
      Color(0xFF00897B), // Emerald Green
      Color(0xFF6A11CB), // Radiant Purple
    ],
    stops: [0.0, 0.5, 1.0],
  );
}
