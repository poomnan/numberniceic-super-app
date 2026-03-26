import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/colors.dart';

/// Dashboard Summary Widget - แสดงสถิติผลการค้นหาแบบพรีเมียม
class DashboardSummary extends StatelessWidget {
  final int totalNames;
  final String excellentNames;
  final String numerologyGood;
  final String shadowGood;
  final bool isSatActive;
  final bool isShaActive;
  final String? recommendedDays;
  final VoidCallback? onInfoTap;

  const DashboardSummary({
    super.key,
    required this.totalNames,
    required this.excellentNames,
    required this.numerologyGood,
    required this.shadowGood,
    this.isSatActive = true,
    this.isShaActive = true,
    this.recommendedDays,
    this.onInfoTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Header
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 10),
          child: Text(
            'พบรายชื่อวิเคราะห์ได้ตามเงื่อนไข',
            style: GoogleFonts.sarabun(
              color: AppColors.textGray.withOpacity(0.6),
              fontSize: 12,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.2,
            ),
          ),
        ),
        // Premium Stat Row - ดีไซน์ใหม่ให้พรีเมียมและโดดเด่นเสมอ
        IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: _buildModernStatChip(
                  icon: Icons.emoji_events_rounded,
                  label: 'ดีเยี่ยม',
                  value: excellentNames,
                  // สีทองอร่ามสำหรับ Excellent
                  color: const Color(0xFFDBB632), 
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [Color(0xFFFFF9E6), Color(0xFFFDE68A)],
                  ),
                  isProminent: true,
                  isActive: isSatActive && isShaActive,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildModernStatChip(
                  icon: Icons.calculate_rounded,
                  label: 'เลขศาสตร์',
                  value: numerologyGood,
                  // สีเขียวมรกตพรีเมียม
                  color: const Color(0xFF10B981),
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [Color(0xFFECFDF5), Color(0xFFA7F3D0)],
                  ),
                  isActive: isSatActive,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildModernStatChip(
                  icon: Icons.blur_on_rounded,
                  label: 'พลังเงา',
                  value: shadowGood,
                  // สีน้ำเงินครามพรีเมียม
                  color: const Color(0xFF6366F1),
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [Color(0xFFEEF2FF), Color(0xFFC7D2FE)],
                  ),
                  isActive: isShaActive,
                ),
              ),
            ],
          ),
        ),

        // Recommended Days (if provided)
        if (recommendedDays != null && recommendedDays!.isNotEmpty) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: AppColors.bgDarker,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppColors.accent.withOpacity(0.2)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(
                  Icons.calendar_month_rounded,
                  size: 14,
                  color: AppColors.textGray,
                ),
                const SizedBox(width: 8),
                Text(
                  'ฤกษ์ดีสำหรับ: ',
                  style: GoogleFonts.sarabun(
                    color: AppColors.textGray.withOpacity(0.7),
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Flexible(
                  child: Text(
                    recommendedDays!,
                    style: GoogleFonts.prompt(
                      color: AppColors.textLight.withOpacity(0.9),
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildModernStatChip({
    required IconData icon,
    required String label,
    required String value,
    required Color color,
    required Gradient gradient,
    bool isProminent = false,
    bool isActive = true,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: color.withOpacity(isActive ? 0.4 : 0.1),
          width: isActive ? 1.5 : 1.0,
        ),
        gradient: gradient,
        boxShadow: [
          if (isActive)
            BoxShadow(
              color: color.withOpacity(0.08),
              blurRadius: 8,
              offset: const Offset(0, 3),
            ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Small Compact Icon
          Container(
            padding: const EdgeInsets.all(5),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.7),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 14, color: color),
          ),
          const SizedBox(width: 8),
          // Label & Value side by side or stacked compactly
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: GoogleFonts.sarabun(
                    color: color.withOpacity(0.8),
                    fontSize: 10,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.2,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  value,
                  style: GoogleFonts.prompt(
                    color: AppColors.textLight.withOpacity(0.9),
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                    height: 1.1,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
