import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:in_app_purchase/in_app_purchase.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// PremiumManager - Singleton service for managing premium status and trial usage
///
/// ระบบ Premium:
/// - Free: ค้นหาชื่อได้ไม่จำกัด (toggle ปิดทั้งสอง)
/// - Free Trial: ใช้ toggle "เลขศาสตร์ดี" / "พลังเงาดี" ได้ 10 ครั้ง
/// - Premium: ซื้อขาดครั้งเดียว → ใช้ได้ตลอดไป
class PremiumManager extends ChangeNotifier {
  // Singleton
  static final PremiumManager _instance = PremiumManager._internal();
  factory PremiumManager() => _instance;
  PremiumManager._internal();

  // Constants
  static const int trialLimit = 3;
  static const bool _isTest = bool.fromEnvironment('FLUTTER_TEST');
  static const String _keyTrialCount = 'premium_trial_count';
  static const String _keyIsPremium = 'is_premium';
  static const String _keyPurchaseDate = 'premium_purchase_date';

  static const String productId = 'premium_lifetime';
  static const Set<String> _kIds = <String>{productId};

  // In-memory state
  bool _isPremium = false;
  int _trialUsed = 0;
  bool _isInitialized = false;
  bool _isPurchasePending = false;
  String? _purchaseError;

  // IAP
  final InAppPurchase _iap = InAppPurchase.instance;
  StreamSubscription<List<PurchaseDetails>>? _subscription;

  // Set to true to bypass all paywall gates during development.
  static const bool _bypassForTesting = true;
  // ──────────────────────────────────────────────────────

  // Getters
  bool get isPremium => _bypassForTesting ? true : _isPremium;
  int get trialUsed => _trialUsed;
  int get trialRemaining => (trialLimit - _trialUsed).clamp(0, trialLimit);
  bool get hasTrialLeft => _bypassForTesting ? true : (_trialUsed < trialLimit);
  bool get isPurchasePending => _isPurchasePending;
  String? get purchaseError => _purchaseError;

  /// Check if user can use premium features (toggle filters)
  /// Returns true if user is premium OR still has trial left
  bool get canUsePremiumFeature =>
      _bypassForTesting ? true : (_isPremium || hasTrialLeft);

  /// Initialize from SharedPreferences — call once at app startup
  Future<void> init() async {
    if (_isInitialized) return;
    final prefs = await SharedPreferences.getInstance();
    _isPremium = prefs.getBool(_keyIsPremium) ?? false;
    _trialUsed = prefs.getInt(_keyTrialCount) ?? 0;

    if (_isTest) {
      _isInitialized = true;
      notifyListeners();
      return;
    }

    // Listen to purchase updates
    final Stream<List<PurchaseDetails>> purchaseUpdated = _iap.purchaseStream;
    _subscription = purchaseUpdated.listen(
      (purchaseDetailsList) {
        _listenToPurchaseUpdated(purchaseDetailsList);
      },
      onDone: () {
        _subscription?.cancel();
      },
      onError: (error) {
        debugPrint("IAP Error: $error");
      },
    );

    _isInitialized = true;
    notifyListeners();
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  /// Increment trial count — call when user searches with premium toggle(s) ON
  /// Returns the new trial count
  Future<int> incrementTrial() async {
    if (_isPremium) return _trialUsed; // Premium users don't count
    _trialUsed++;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_keyTrialCount, _trialUsed);
    notifyListeners();
    return _trialUsed;
  }

  /// Buy Premium Product
  Future<void> buyPremium() async {
    _purchaseError = null;
    _isPurchasePending = true;
    notifyListeners();

    try {
      final bool available = await _iap.isAvailable();
      if (!available) {
        _purchaseError = "Store not available";
        _isPurchasePending = false;
        notifyListeners();
        return;
      }

      final ProductDetailsResponse response = await _iap.queryProductDetails(
        _kIds,
      );
      if (response.notFoundIDs.isNotEmpty) {
        // If product not found, maybe just unlock for testing in debug mode?
        if (kDebugMode) {
          debugPrint("Product not found in store, unlocking for testing...");
          await unlockPremium();
          _isPurchasePending = false;
          notifyListeners();
          return;
        }
        _purchaseError = "Product not found";
        _isPurchasePending = false;
        notifyListeners();
        return;
      }

      final List<ProductDetails> products = response.productDetails;
      if (products.isEmpty) {
        _purchaseError = "No products found";
        _isPurchasePending = false;
        notifyListeners();
        return;
      }

      final ProductDetails productDetails = products.first;
      final PurchaseParam purchaseParam = PurchaseParam(
        productDetails: productDetails,
      );

      // Initiate purchase
      await _iap.buyNonConsumable(purchaseParam: purchaseParam);
    } catch (e) {
      _purchaseError = "Error: $e";
      _isPurchasePending = false;
      notifyListeners();
    }
  }

  /// Restore purchase — for users who reinstall the app
  Future<void> restorePurchase() async {
    _purchaseError = null;
    _isPurchasePending = true;
    notifyListeners();
    try {
      await _iap.restorePurchases();
      // Status will be updated in listener
    } catch (e) {
      _purchaseError = "Restore failed: $e";
      _isPurchasePending = false;
      notifyListeners();
    }
  }

  /// Internal: Handle purchase updates
  void _listenToPurchaseUpdated(List<PurchaseDetails> purchaseDetailsList) {
    for (final PurchaseDetails purchaseDetails in purchaseDetailsList) {
      if (purchaseDetails.status == PurchaseStatus.pending) {
        _isPurchasePending = true;
        notifyListeners();
      } else {
        if (purchaseDetails.status == PurchaseStatus.error) {
          _purchaseError = purchaseDetails.error?.message ?? "Unknown error";
          _isPurchasePending = false;
          notifyListeners();
        } else if (purchaseDetails.status == PurchaseStatus.canceled) {
          _purchaseError = "Purchase canceled";
          _isPurchasePending = false;
          notifyListeners();
        } else if (purchaseDetails.status == PurchaseStatus.purchased ||
            purchaseDetails.status == PurchaseStatus.restored) {
          unlockPremium(); // Persist locally
          _isPurchasePending = false;
          notifyListeners();
        }

        if (purchaseDetails.pendingCompletePurchase) {
          _iap.completePurchase(purchaseDetails);
        }
      }
    }
  }

  /// Unlock premium — call after successful purchase
  Future<void> unlockPremium() async {
    _isPremium = true;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyIsPremium, true);
    await prefs.setString(_keyPurchaseDate, DateTime.now().toIso8601String());
    notifyListeners();
  }

  /// Reset trial (for testing only — remove in production)
  Future<void> resetForTesting() async {
    _isPremium = false;
    _trialUsed = 0;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyIsPremium);
    await prefs.remove(_keyTrialCount);
    await prefs.remove(_keyPurchaseDate);
    notifyListeners();
  }
}
