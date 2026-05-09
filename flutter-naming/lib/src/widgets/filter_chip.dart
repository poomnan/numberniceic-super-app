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
  final bool isLocked;
  final GestureTapDownCallback? onTapDown;
  final bool disabled;
  final String? disabledTooltip;

  const FilterChipWidget({
    super.key,
    required this.label,
    required this.icon,
    required this.isActive,
    required this.onTap,
    this.isLoading = false,
    this.activeChipColor,
    this.activeTextColor,
    this.isLocked = false,
    this.onTapDown,
    this.disabled = false,
    this.disabledTooltip,
  });

  @override
  Widget build(BuildContext context) {
    final finalActiveColor = activeChipColor ?? AppColors.accent;
    final finalActiveTextColor =
        activeTextColor ??
        (activeChipColor != null ? Colors.white : AppColors.textLight);

    final Color inactiveTextColor = disabled
        ? AppColors.textGray.withValues(alpha: 0.45)
        : isLocked
        ? AppColors.textGray.withValues(alpha: 0.9)
        : AppColors.textLight;

    final chip = GestureDetector(
      onTapDown: disabled ? null : onTapDown,
      onTap: disabled ? null : (isLoading ? null : onTap),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: disabled
              ? Colors.white.withValues(alpha: 0.25)
              : isActive
              ? finalActiveColor
              : isLocked
              ? const Color(0xFFF6F2FF)
              : Colors.white.withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: disabled
                ? AppColors.textGray.withValues(alpha: 0.15)
                : isActive
                ? finalActiveColor
                : isLocked
                ? const Color(0xFFD8CCFF)
                : AppColors.secondary.withValues(alpha: 0.2),
            width: 1.5,
          ),
          boxShadow: isActive && !disabled
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
                disabled
                    ? Icons.block_rounded
                    : (isLocked ? Icons.lock_rounded : icon),
                size: 18,
                color: disabled
                    ? AppColors.textGray.withValues(alpha: 0.35)
                    : isActive
                    ? finalActiveTextColor
                    : isLocked
                    ? const Color(0xFF8B5CF6)
                    : AppColors.textGray.withValues(alpha: 0.7),
              ),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: disabled
                    ? AppColors.textGray.withValues(alpha: 0.45)
                    : isActive
                    ? finalActiveTextColor
                    : inactiveTextColor,
                fontSize: 14,
                fontWeight: isActive && !disabled
                    ? FontWeight.w700
                    : FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );

    if (disabled && disabledTooltip != null) {
      return Tooltip(message: disabledTooltip!, child: chip);
    }
    return chip;
  }
}
