import 'package:flutter/material.dart';
import '../utils/colors.dart';

/// Custom Filter Chip สำหรับฟิลเตอร์ในแอปตั้งชื่อ
///
/// ตัวอย่างการใช้งาน:
/// ```dart
/// FilterChipWidget(
///   label: 'เลขศาสตร์ดี',
///   icon: Icons.calculate,
///   isActive: _filterSat,
///   onTap: () => setState(() => _filterSat = !_filterSat),
/// )
/// ```
class FilterChipWidget extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool isActive;
  final VoidCallback onTap;
  final Color? activeColor;
  final Color? activeTextColor;

  const FilterChipWidget({
    super.key,
    required this.label,
    required this.icon,
    required this.isActive,
    required this.onTap,
    this.activeColor,
    this.activeTextColor,
  });

  @override
  Widget build(BuildContext context) {
    final finalActiveColor = activeColor ?? AppColors.accent;
    final finalActiveTextColor =
        activeTextColor ??
        (activeColor != null ? Colors.white : AppColors.textLight);

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: isActive ? finalActiveColor : Colors.white.withOpacity(0.5),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: isActive
                ? finalActiveColor
                : AppColors.secondary.withOpacity(0.2),
            width: 1.5,
          ),
          boxShadow: isActive
              ? [
                  BoxShadow(
                    color: finalActiveColor.withOpacity(0.2),
                    blurRadius: 8,
                    spreadRadius: 1,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 18,
              color: isActive
                  ? finalActiveTextColor
                  : AppColors.textGray.withOpacity(0.7),
            ),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: isActive ? finalActiveTextColor : AppColors.textLight,
                fontSize: 14,
                fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
