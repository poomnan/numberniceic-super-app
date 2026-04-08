import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/premium_manager.dart';

/// Paywall Dialog — แสดงเมื่อ trial หมด
///
/// Returns true if user purchased premium, false otherwise
Future<bool> showPaywallDialog(BuildContext context) async {
  final result = await showDialog<bool>(
    context: context,
    barrierDismissible: true,
    builder: (dialogContext) {
      return const PaywallDialogContent();
    },
  );
  return result ?? false;
}

class PaywallDialogContent extends StatefulWidget {
  const PaywallDialogContent({super.key});

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

    return Dialog(
      backgroundColor: Colors.transparent,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 360),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)],
          ),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: Colors.amber.withValues(alpha: 0.3), width: 1.5),
          boxShadow: [
            BoxShadow(
              color: Colors.amber.withValues(alpha: 0.1),
              blurRadius: 30,
              spreadRadius: 5,
            ),
          ],
        ),
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Lock icon with glow
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    colors: [Colors.amber.shade700, Colors.amber.shade400],
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.amber.withValues(alpha: 0.4),
                      blurRadius: 20,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                child: isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Icon(
                        Icons.lock_open_rounded,
                        color: Colors.white,
                        size: 36,
                      ),
              ),
              const SizedBox(height: 20),

              // Title
              Text(
                "ช่วยคัดชื่อให้ละเอียดขึ้น",
                style: GoogleFonts.prompt(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),

              // Subtitle
              Text(
                "ปลดล็อกตัวกรองและการเปรียบเทียบชื่อ เพื่อช่วยคุณตัดสินใจได้มั่นใจขึ้น",
                style: GoogleFonts.sarabun(color: Colors.white60, fontSize: 14),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),

              // Features list
              _buildFeatureItem(
                Icons.group_add,
                "รู้จักความหมายชื่อ +3 แสนชื่อ",
              ),
              const SizedBox(height: 12),
              _buildFeatureItem(
                Icons.auto_awesome,
                "หาเลขศาสตร์พลังเงาไม่จำกัด",
              ),
              const SizedBox(height: 12),
              _buildFeatureItem(
                Icons.auto_awesome,
                "ใช้เงื่อนไขตามตำราตั้งชื่อ 4D",
              ),
              const SizedBox(height: 12),
              _buildFeatureItem(
                Icons.all_inclusive,
                "ซื้อครั้งเดียว ใช้ได้ตลอดไป",
              ),
              const SizedBox(height: 28),

              // Error Message
              if (error != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16.0),
                  child: Text(
                    error,
                    style: const TextStyle(
                      color: Colors.redAccent,
                      fontSize: 12,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),

              // Price
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.amber.withValues(alpha: 0.3)),
                ),
                child: Text(
                  "฿259 ครั้งเดียว ใช้ได้ตลอด",
                  style: GoogleFonts.prompt(
                    color: Colors.amber.shade300,
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // Buy button
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: isLoading
                      ? null
                      : () {
                          _premiumManager.buyPremium();
                        },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.transparent,
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
                            ? [Colors.grey, Colors.grey]
                            : [Colors.amber.shade700, Colors.amber.shade500],
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
                              Icons.lock_open,
                              color: Colors.white,
                              size: 20,
                            ),
                            const SizedBox(width: 8),
                          ],
                          Text(
                            isLoading
                                ? "กำลังดำเนินการ..."
                                : "ปลดล็อกเพื่อคัดชื่อ",
                            style: GoogleFonts.prompt(
                              color: Colors.white,
                              fontSize: 17,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),

              // Later button
              TextButton(
                onPressed: isLoading
                    ? null
                    : () => Navigator.pop(context, false),
                child: Text(
                  "ขอดูก่อนค่ะ",
                  style: GoogleFonts.sarabun(
                    color: Colors.white38,
                    fontSize: 14,
                  ),
                ),
              ),

              // Restore purchase link
              TextButton(
                onPressed: isLoading
                    ? null
                    : () {
                        _premiumManager.restorePurchase();
                      },
                child: Text(
                  "กู้คืนสิทธิ์ที่ซื้อแล้ว",
                  style: GoogleFonts.sarabun(
                    color: Colors.white24,
                    fontSize: 12,
                    decoration: TextDecoration.underline,
                    decorationColor: Colors.white24,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFeatureItem(IconData icon, String text) {
    return Row(
      children: [
        Icon(icon, color: Colors.amber.shade400, size: 20),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            text,
            style: GoogleFonts.sarabun(
              color: Colors.white.withValues(alpha: 0.85),
              fontSize: 15,
            ),
          ),
        ),
      ],
    );
  }
}
