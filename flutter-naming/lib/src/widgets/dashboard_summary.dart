import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

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
    final List<({String label, String value, Color accent, bool isActive})>
    stats = [
      (
        label: 'เลขศาสตร์',
        value: numerologyGood,
        accent: const Color(0xFFB45309),
        isActive: true,
      ),
      (
        label: 'พลังเงา',
        value: shadowGood,
        accent: const Color(0xFF6366F1),
        isActive: true,
      ),
    ];

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFF7D6), Color(0xFFF1E8FF), Color(0xFFE8F7F3)],
        ),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ANCHOR: Count by Ranking (แสดงรายชื่อที่ผ่านเกณฑ์ดีที่สุดตามเงื่อนไข)
          Text(
            'แสดงรายชื่อที่ผ่านเกณฑ์จัดอันดับ',
            style: GoogleFonts.sarabun(
              color: const Color(0xFF6B4E16).withValues(alpha: 0.82),
              fontSize: 12.5,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
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
                      isActive: stat.isActive,
                    ),
                  ),
                )
                .toList(),
          ),

          // Recommended Days (if provided)
          if (!isLoading &&
              recommendedDays != null &&
              recommendedDays!.isNotEmpty) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.56),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.calendar_month_rounded,
                    size: 14,
                    color: Color(0xFF7A5C12),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'ฤกษ์ดีสำหรับ: ',
                    style: GoogleFonts.sarabun(
                      color: const Color(0xFF7A5C12).withValues(alpha: 0.76),
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Flexible(
                    child: Text(
                      recommendedDays!,
                      style: GoogleFonts.prompt(
                        color: const Color(0xFF3D2600),
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
      ),
    );
  }

  Widget _buildSimpleStat({
    required String label,
    required String value,
    required Color accent,
    bool isLoading = false,
    bool isActive = true,
  }) {
    final Color labelColor = isActive
        ? accent.withValues(alpha: 0.9)
        : const Color(0xFF94A3B8);
    final Color valueColor = isActive
        ? const Color(0xFF1F2937)
        : const Color(0xFF94A3B8);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              style: GoogleFonts.sarabun(
                color: labelColor,
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(width: 8),
            if (isLoading)
              SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(accent),
                ),
              )
            else
              Text(
                value,
                style: GoogleFonts.prompt(
                  color: valueColor,
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  height: 1,
                ),
              ),
          ],
        ),
      ],
    );
  }
}
