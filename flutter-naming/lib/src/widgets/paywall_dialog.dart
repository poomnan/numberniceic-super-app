import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/premium_manager.dart';

/// Paywall Dialog — แสดงเมื่อ trial หมด
///
/// Returns true if user purchased premium, false otherwise
Future<bool> showPaywallDialog(
  BuildContext context, {
  String? seedName,
  int? seedSat,
  int? seedSha,
  String? seedSatType,
  String? seedShaType,
  int? matchedCount,
  List<String> previewNames = const [],
}) async {
  final result = await showDialog<bool>(
    context: context,
    barrierDismissible: true,
    builder: (dialogContext) {
      return PaywallDialogContent(
        seedName: seedName,
        seedSat: seedSat,
        seedSha: seedSha,
        seedSatType: seedSatType,
        seedShaType: seedShaType,
        matchedCount: matchedCount,
        previewNames: previewNames,
      );
    },
  );
  return result ?? false;
}

class PaywallDialogContent extends StatefulWidget {
  const PaywallDialogContent({
    super.key,
    this.seedName,
    this.seedSat,
    this.seedSha,
    this.seedSatType,
    this.seedShaType,
    this.matchedCount,
    this.previewNames = const [],
  });

  final String? seedName;
  final int? seedSat;
  final int? seedSha;
  final String? seedSatType;
  final String? seedShaType;
  final int? matchedCount;
  final List<String> previewNames;

  @override
  State<PaywallDialogContent> createState() => _PaywallDialogContentState();
}

class _PaywallDialogContentState extends State<PaywallDialogContent> {
  final PremiumManager _premiumManager = PremiumManager();

  @override
  void initState() {
    super.initState();
    _premiumManager.addListener(_onPremiumUpdate);
  }

  @override
  void dispose() {
    _premiumManager.removeListener(_onPremiumUpdate);
    super.dispose();
  }

  bool _isPopped = false;

  void _onPremiumUpdate() {
    if (!mounted || _isPopped) return;

    // Close dialog on success
    if (_premiumManager.isPremium) {
      _isPopped = true;
      Navigator.of(context).pop(true);
      return;
    }

    // Force rebuild to show loading/error
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final bool isLoading = _premiumManager.isPurchasePending;
    final String? error = _premiumManager.purchaseError;
    final String? seedName = widget.seedName?.trim().isNotEmpty == true
        ? widget.seedName!.trim()
        : null;
    final bool hasSeed = seedName != null;
    final int? matchedCount =
        widget.matchedCount != null && widget.matchedCount! > 0
        ? widget.matchedCount
        : null;
    final List<String> previewNames = widget.previewNames
        .map((name) => name.trim())
        .where((name) => name.isNotEmpty)
        .take(3)
        .toList();
    final List<String> benefits = hasSeed
        ? [
            matchedCount == null
                ? 'ปลดล็อครายชื่อที่มีพลังเลขใกล้เคียงกับ "$seedName"'
                : 'พบ $matchedCount รายชื่อที่คัดจากพลังเลขของ "$seedName"',
            'เปรียบเทียบเลขศาสตร์และพลังเงาแบบละเอียด',
            'ค้นหารายชื่อมงคลได้ไม่จำกัด',
          ]
        : const [
            'รู้จักความหมายชื่อ +3 แสนชื่อ',
            'หาเลขศาสตร์พลังเงาไม่จำกัด',
            'ใช้เงื่อนไขตามตำราตั้งชื่อ 4D',
          ];

    return Dialog(
      backgroundColor: Colors.transparent,
      insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: 380,
          maxHeight: MediaQuery.sizeOf(context).height * 0.9,
        ),
        child: Container(
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF201737), Color(0xFF121D3E), Color(0xFF071D3A)],
            ),
            borderRadius: BorderRadius.circular(24),
            border: Border.all(
              color: const Color(0xFFFFD700).withValues(alpha: 0.36),
              width: 1.5,
            ),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFFFFA000).withValues(alpha: 0.18),
                blurRadius: 34,
                spreadRadius: 4,
                offset: const Offset(0, 14),
              ),
            ],
          ),
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 66,
                  height: 66,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: const LinearGradient(
                      colors: [Color(0xFFFFA000), Color(0xFFFFD700)],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFFFFD700).withValues(alpha: 0.36),
                        blurRadius: 26,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                  child: isLoading
                      ? const Padding(
                          padding: EdgeInsets.all(18),
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2.5,
                          ),
                        )
                      : const Icon(
                          Icons.diamond_rounded,
                          color: Colors.white,
                          size: 34,
                        ),
                ),
                const SizedBox(height: 18),
                Text(
                  hasSeed ? "ปลดล็อคชื่อที่คู่กับคุณ" : "ปลดล็อคพรีเมียม",
                  style: GoogleFonts.prompt(
                    color: Colors.white,
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  hasSeed
                      ? "ดูรายชื่อมงคลที่คัดจากพลังเลขของชื่อที่คุณเลือก"
                      : "เปิดตัวกรองและการเปรียบเทียบชื่อแบบละเอียด เพื่อเลือกชื่อได้มั่นใจขึ้น",
                  style: GoogleFonts.sarabun(
                    color: Colors.white.withValues(alpha: 0.68),
                    fontSize: 14,
                    height: 1.45,
                  ),
                  textAlign: TextAlign.center,
                ),
                if (hasSeed) ...[
                  const SizedBox(height: 18),
                  _buildSeedPill(seedName),
                ],
                if (previewNames.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  _buildPreviewNames(previewNames),
                ],
                const SizedBox(height: 22),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.06),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.08),
                    ),
                  ),
                  child: Column(
                    children: [
                      for (int index = 0; index < benefits.length; index++) ...[
                        _buildFeatureItem(
                          index == 0
                              ? Icons.auto_awesome_rounded
                              : index == 1
                              ? Icons.insights_rounded
                              : Icons.all_inclusive_rounded,
                          benefits[index],
                        ),
                        if (index != benefits.length - 1)
                          const SizedBox(height: 12),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                _buildPriceCard(),
                if (error != null) ...[
                  const SizedBox(height: 14),
                  Text(
                    error,
                    style: GoogleFonts.sarabun(
                      color: Colors.redAccent.shade100,
                      fontSize: 12,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
                const SizedBox(height: 20),
                _buildBuyButton(isLoading),
                const SizedBox(height: 10),
                TextButton(
                  onPressed: isLoading
                      ? null
                      : () {
                          _premiumManager.restorePurchase();
                        },
                  child: Text(
                    "กู้คืนสิทธิ์ที่ซื้อแล้ว",
                    style: GoogleFonts.sarabun(
                      color: Colors.white.withValues(alpha: 0.46),
                      fontSize: 13,
                      decoration: TextDecoration.underline,
                      decorationColor: Colors.white38,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: isLoading
                      ? null
                      : () => Navigator.pop(context, false),
                  child: Text(
                    "ยังไม่ปลดล็อคตอนนี้",
                    style: GoogleFonts.sarabun(
                      color: Colors.white.withValues(alpha: 0.34),
                      fontSize: 13,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildFeatureItem(IconData icon, String text) {
    return Row(
      children: [
        Icon(icon, color: const Color(0xFFFFD700), size: 19),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            text,
            style: GoogleFonts.sarabun(
              color: Colors.white.withValues(alpha: 0.88),
              fontSize: 14.5,
              height: 1.35,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSeedPill(String seedName) {
    final String satText = _formatNumberLabel(
      'SAT',
      widget.seedSat,
      widget.seedSatType,
    );
    final String shaText = _formatNumberLabel(
      'SHA',
      widget.seedSha,
      widget.seedShaType,
    );

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            const Color(0xFFFFD700).withValues(alpha: 0.16),
            Colors.white.withValues(alpha: 0.06),
          ],
        ),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFFFFD700).withValues(alpha: 0.34),
        ),
      ),
      child: Column(
        children: [
          Text(
            '"$seedName"',
            style: GoogleFonts.prompt(
              color: Colors.white,
              fontSize: 24,
              fontWeight: FontWeight.w700,
            ),
            textAlign: TextAlign.center,
          ),
          if (satText.isNotEmpty || shaText.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              [satText, shaText].where((item) => item.isNotEmpty).join(' · '),
              style: GoogleFonts.sarabun(
                color: const Color(0xFFFFD700),
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildPriceCard() {
    final String priceText = _premiumManager.premiumPriceLabel;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 13),
      decoration: BoxDecoration(
        color: const Color(0xFFFFA000).withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFFFFD700).withValues(alpha: 0.42),
        ),
      ),
      child: Text(
        "ซื้อครั้งเดียว $priceText",
        style: GoogleFonts.prompt(
          color: const Color(0xFFFFD700),
          fontSize: 18,
          fontWeight: FontWeight.w700,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }

  Widget _buildPreviewNames(List<String> names) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
      ),
      child: Text.rich(
        TextSpan(
          text: 'ตัวอย่างชื่อที่พบ: ',
          style: GoogleFonts.sarabun(
            color: Colors.white.withValues(alpha: 0.66),
            fontSize: 13,
            height: 1.35,
          ),
          children: [
            TextSpan(
              text: names.join(', '),
              style: GoogleFonts.prompt(
                color: Colors.white,
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
        textAlign: TextAlign.center,
      ),
    );
  }

  Widget _buildBuyButton(bool isLoading) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(14),
        boxShadow: [
          if (!isLoading)
            BoxShadow(
              color: const Color(0xFFFFA000).withValues(alpha: 0.32),
              blurRadius: 20,
              offset: const Offset(0, 8),
            ),
        ],
      ),
      child: SizedBox(
        width: double.infinity,
        height: 54,
        child: ElevatedButton(
          onPressed: isLoading
              ? null
              : () {
                  _premiumManager.buyPremium();
                },
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.transparent,
            disabledBackgroundColor: Colors.transparent,
            shadowColor: Colors.transparent,
            padding: EdgeInsets.zero,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
          child: Ink(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: isLoading
                    ? const [Color(0xFF6B7280), Color(0xFF4B5563)]
                    : const [Color(0xFFFFA000), Color(0xFFFFD700)],
              ),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Container(
              alignment: Alignment.center,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (!isLoading) ...[
                    const Icon(
                      Icons.diamond_rounded,
                      color: Color(0xFF241600),
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                  ],
                  Text(
                    isLoading ? "กำลังดำเนินการ..." : "ปลดล็อคการค้นหา",
                    style: GoogleFonts.prompt(
                      color: isLoading ? Colors.white : const Color(0xFF241600),
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  String _formatNumberLabel(String label, int? value, String? type) {
    if (value == null || value <= 0) return '';
    final normalizedType = type?.trim();
    if (normalizedType == null || normalizedType.isEmpty) {
      return '$label $value';
    }
    return '$label $value $normalizedType';
  }
}
