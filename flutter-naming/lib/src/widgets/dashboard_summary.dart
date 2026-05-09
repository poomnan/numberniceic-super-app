import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/colors.dart';

/// Dashboard Summary Widget - แสดงสถิติผลการค้นหาแบบพรีเมียม
class DashboardSummary extends StatelessWidget {
  final int totalNames;
  final String excellentNames;
  final String numerologyGood;
  final String shadowGood;
  final bool isLoading;
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
    this.isLoading = false,
    this.isSatActive = true,
    this.isShaActive = true,
    this.recommendedDays,
    this.onInfoTap,
  });

  @override
  Widget build(BuildContext context) {
    final String displayedNumerologyGood = isSatActive ? numerologyGood : '0';
    final String displayedShadowGood = isShaActive ? shadowGood : '0';
    final List<({String label, String value, Color accent})> stats = [
      (
        label: 'ทั้งหมด',
        value: totalNames.toString(),
        accent: const Color(0xFFDBB632),
      ),
      (
        label: 'เลขศาสตร์',
        value: displayedNumerologyGood,
        accent: const Color(0xFF10B981),
      ),
      (
        label: 'พลังเงา',
        value: displayedShadowGood,
        accent: const Color(0xFF6366F1),
      ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Header
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 10),
          // ANCHOR: Count by Ranking (แสดงรายชื่อที่ผ่านเกณฑ์ดีที่สุดตามเงื่อนไข)

          child: Text(
            'แสดงรายชื่อที่ผ่านเกณฑ์ดีที่สุดตามเงื่อนไข',
            style: GoogleFonts.sarabun(
              color: AppColors.textGray.withValues(alpha: 0.6),
              fontSize: 12,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.2,
            ),
          ),
        ),
        // ANCHOR: Count by Ranking (รายชื่อวิเคราะห์ได้ตามเงื่อนไข)
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: stats
              .map(
                (stat) => Expanded(
                  child: _buildSimpleStat(
                    label: stat.label,
                    value: stat.value,
                    accent: stat.accent,
                    isLoading: isLoading,
                  ),
                ),
              )
              .toList(),
        ),

        // Recommended Days (if provided)
        if (!isLoading && recommendedDays != null && recommendedDays!.isNotEmpty) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: AppColors.bgDarker,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(
                color: AppColors.accent.withValues(alpha: 0.2),
              ),
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
                    color: AppColors.textGray.withValues(alpha: 0.7),
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Flexible(
                  child: Text(
                    recommendedDays!,
                    style: GoogleFonts.prompt(
                      color: AppColors.textLight.withValues(alpha: 0.9),
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

  Widget _buildSimpleStat({
    required String label,
    required String value,
    required Color accent,
    bool isLoading = false,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text(
          label,
          style: GoogleFonts.sarabun(
            color: accent.withValues(alpha: 0.9),
            fontSize: 12,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.1,
          ),
        ),
        const SizedBox(height: 4),
        if (isLoading)
          SizedBox(
            width: 22,
            height: 22,
            child: CircularProgressIndicator(
              strokeWidth: 2.4,
              valueColor: AlwaysStoppedAnimation<Color>(accent),
            ),
          )
        else
          Text(
            value,
            style: GoogleFonts.prompt(
              color: AppColors.textLight,
              fontSize: 18,
              fontWeight: FontWeight.w900,
            ),
          ),
      ],
    );
  }
}
