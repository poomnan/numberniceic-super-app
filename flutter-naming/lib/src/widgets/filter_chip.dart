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
  final Color? inactiveBorderColor;
  final Gradient? activeGradient;
  final Gradient? inactiveGradient;
  final Color? customInactiveTextColor;
  final List<BoxShadow>? customBoxShadow;
  final VoidCallback? onDisabledTap;

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
    this.inactiveBorderColor,
    this.activeGradient,
    this.inactiveGradient,
    this.customInactiveTextColor,
    this.customBoxShadow,
    this.onDisabledTap,
  });

  @override
  Widget build(BuildContext context) {
    final finalActiveColor = activeChipColor ?? AppColors.accent;
    final finalActiveTextColor =
        activeTextColor ??
        (activeChipColor != null ? Colors.white : AppColors.textLight);

    final Color inactiveTextColor = customInactiveTextColor ?? (disabled
        ? const Color(0xFF94A3B8) // Slate 400 for disabled state
        : isLocked
        ? AppColors.textGray.withValues(alpha: 0.9)
        : AppColors.textLight);

    final chip = GestureDetector(
      onTapDown: disabled ? null : onTapDown,
      onTap: disabled 
          ? onDisabledTap 
          : (isLoading ? null : onTap),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: disabled
              ? const Color(0xFFF1F5F9) // Slate 100 solid background for disabled look
              : isActive
              ? (activeGradient == null ? finalActiveColor : null)
              : isLocked
              ? const Color(0xFFF6F2FF)
              : (inactiveGradient == null ? Colors.white.withValues(alpha: 0.5) : null),
          gradient: disabled
              ? null
              : isActive
              ? activeGradient
              : (isLocked ? null : inactiveGradient),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: disabled
                ? const Color(0xFFCBD5E1) // Slate 300 for solid disabled border
                : isActive
                ? (activeGradient != null ? Colors.transparent : finalActiveColor)
                : isLocked
                ? const Color(0xFFD8CCFF)
                : (inactiveBorderColor ??
                      AppColors.secondary.withValues(alpha: 0.2)),
            width: 1.5,
          ),
          boxShadow: customBoxShadow ?? (isActive && !disabled
              ? [
                  BoxShadow(
                    color: finalActiveColor.withValues(alpha: 0.2),
                    blurRadius: 8,
                    spreadRadius: 1,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null),
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
                isLocked ? Icons.lock_rounded : icon,
                size: 18,
                color: disabled
                    ? const Color(0xFF94A3B8) // Slate 400
                    : isActive
                    ? finalActiveTextColor
                    : isLocked
                    ? const Color(0xFF8B5CF6)
                    : (customInactiveTextColor ?? AppColors.textGray.withValues(alpha: 0.7)),
              ),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: disabled
                    ? const Color(0xFF94A3B8) // Slate 400
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
