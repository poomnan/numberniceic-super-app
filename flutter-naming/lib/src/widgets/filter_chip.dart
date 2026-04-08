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
  final bool isLoading;
  final VoidCallback onTap;
  final Color? activeChipColor;
  final Color? activeTextColor;

  const FilterChipWidget({
    super.key,
    required this.label,
    required this.icon,
    required this.isActive,
    required this.onTap,
    this.isLoading = false,
    this.activeChipColor,
    this.activeTextColor,
  });

  @override
  Widget build(BuildContext context) {
    final finalActiveColor = activeChipColor ?? AppColors.accent;
    final finalActiveTextColor =
        activeTextColor ??
        (activeChipColor != null ? Colors.white : AppColors.textLight);

    return GestureDetector(
      onTap: isLoading ? null : onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: isActive ? finalActiveColor : Colors.white.withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: isActive
                ? finalActiveColor
                : AppColors.secondary.withValues(alpha: 0.2),
            width: 1.5,
          ),
          boxShadow: isActive
              ? [
                  BoxShadow(
                    color: finalActiveColor.withValues(alpha: 0.2),
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
            if (isLoading)
              SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: isActive ? finalActiveTextColor : finalActiveColor,
                ),
              )
            else
              Icon(
                icon,
                size: 18,
                color: isActive
                    ? finalActiveTextColor
                    : AppColors.textGray.withValues(alpha: 0.7),
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
