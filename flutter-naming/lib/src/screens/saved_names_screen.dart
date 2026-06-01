import 'dart:io' show File;
import 'dart:ui' as ui;
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import '../models/name_model.dart';
import '../models/number_meaning_model.dart';
import '../services/api_service.dart';
import '../utils/colors.dart';
import '../utils/numerology_format.dart';

class SavedNamesScreen extends StatefulWidget {
  const SavedNamesScreen({super.key});

  @override
  State<SavedNamesScreen> createState() => _SavedNamesScreenState();
}

class _ShareNumberMeaning {
  final String labelNumber;
  final String description;

  const _ShareNumberMeaning({
    required this.labelNumber,
    required this.description,
  });
}

class _SavedNamesScreenState extends State<SavedNamesScreen> {
  static const double _pairCircleSize = 22.0;
  static const double _sharePairCircleSize = 28.0;

  final ApiService _apiService = ApiService();
  List<UserSavedName> _savedNames = [];
  bool _isLoading = true;
  bool _isSharing = false;
  final Map<String, String> _meanings = {}; // Cache for fetched meanings
  final GlobalKey _sharePosterKey = GlobalKey();
  final Map<String, NumberMeaningResult?> _numberMeaningCache = {};

  FlutterTts? _flutterTts;
  String? _speakingName; // Tracks which name is currently speaking

  ({
    Gradient bgGradient,
    Color patternColor,
    Color borderColor,
    Color nameColor,
    Color meaningColor,
    Color accentColor,
    Color glowColor,
  })
  _getDynamicCardStyle(UserSavedName item) {
    final bool isSat = item.isSatGood;
    final bool isSha = item.isShaGood;

    if (isSat && isSha) {
      // Shimmering Gold LV (Double-Good) - Bright Champagne Gold
      return (
        bgGradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFFDF0), Color(0xFFFEF3C7)],
        ),
        patternColor: const Color(0xFFB45309),
        borderColor: const Color(0xFFF59E0B).withValues(alpha: 0.5),
        nameColor: const Color(0xFF78350F),
        meaningColor: const Color(0xFF92400E),
        accentColor: const Color(0xFFF59E0B),
        glowColor: const Color(0xFFF59E0B).withValues(alpha: 0.15),
      );
    } else if (isSat) {
      // Warm Caramel/Light Brown LV (เลขศาสตร์ดี ONLY) - Soft Luxury Sand to Caramel
      return (
        bgGradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFFDF9), Color(0xFFF9EFE6)],
        ),
        patternColor: const Color(0xFF8B5A2B),
        borderColor: const Color(0xFFB45309).withValues(alpha: 0.35),
        nameColor: const Color(0xFF451A03),
        meaningColor: const Color(0xFF78350F),
        accentColor: const Color(0xFFB45309),
        glowColor: const Color(0xFFB45309).withValues(alpha: 0.12),
      );
    } else if (isSha) {
      // Royal Amethyst/Purple LV (พลังเงาดี ONLY) - Bright Lavender Purple
      return (
        bgGradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFF5F3FF), Color(0xFFEDE9FE)],
        ),
        patternColor: const Color(0xFF6D28D9),
        borderColor: const Color(0xFF7C3AED).withValues(alpha: 0.4),
        nameColor: const Color(0xFF4C1D95),
        meaningColor: const Color(0xFF5B21B6),
        accentColor: const Color(0xFF7C3AED),
        glowColor: const Color(0xFF7C3AED).withValues(alpha: 0.12),
      );
    } else {
      // Classic Chocolate LV (No filters/Default) - Bright Cream Beige Leather
      return (
        bgGradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFFDF5), Color(0xFFF9F3E6)],
        ),
        patternColor: const Color(0xFF795548),
        borderColor: const Color(0xFFD4AF37).withValues(alpha: 0.3),
        nameColor: const Color(0xFF3D2600),
        meaningColor: const Color(0xFF5D4037),
        accentColor: const Color(0xFFD4AF37),
        glowColor: const Color(0xFFD4AF37).withValues(alpha: 0.1),
      );
    }
  }

  _LuckyBreakdown _computeSavedLuckyBreakdown(UserSavedName item) {
    final bool isSatMatch = item.isSatGood;
    final bool isShaMatch = item.isShaGood;
    final bool noKaki = item.noKaki;
    final bool hasMatchingGood = false;

    final bool isLucky = isSatMatch && isShaMatch;

    int multiplier = 0;
    if (isSatMatch) multiplier++;
    if (isShaMatch) multiplier++;
    if (noKaki) multiplier++;

    return _LuckyBreakdown(
      isSatMatch: isSatMatch,
      isShaMatch: isShaMatch,
      noKaki: noKaki,
      hasMatchingGood: hasMatchingGood,
      isLucky: isLucky,
      multiplier: multiplier,
      includesKakiBonus: noKaki,
    );
  }

  List<CharHighlight> _buildKakiHighlights(UserSavedName item) {
    final List<CharHighlight> highlights = [];
    final nameChars = item.name.split('');
    final kakiSet = item.kakiChars.split('').toSet();
    for (var char in nameChars) {
      highlights.add(CharHighlight(
        char: char,
        isKaki: kakiSet.contains(char),
      ));
    }
    return highlights;
  }

  Widget _buildNameText(BuildContext context, UserSavedName item, {double fontSize = 22}) {
    final bool isGold = item.isSatGood && item.isShaGood;
    final highlights = _buildKakiHighlights(item);
    final bool hasKaki = highlights.any((h) => h.isKaki);

    final style = _getDynamicCardStyle(item);
    final textStyle = TextStyle(
      fontSize: fontSize,
      fontWeight: FontWeight.w900,
      fontFamily: 'Sarabun',
      color: style.nameColor,
    );

    Widget nameWidget;
    if (highlights.isEmpty) {
      nameWidget = Text(item.name, style: textStyle);
    } else {
      final tp = TextPainter(
        text: TextSpan(text: item.name, style: textStyle),
        textDirection: TextDirection.ltr,
      )..layout();

      nameWidget = CustomPaint(
        size: Size(tp.width, fontSize * 1.5),
        painter: _ThaiHighlightPainter(
          highlights: highlights,
          baseStyle: textStyle,
          isGold: isGold,
        ),
      );
    }

    if (isGold && !hasKaki) {
      return PremiumNameTextEffect(child: nameWidget);
    }
    return nameWidget;
  }

  Widget _buildNoKakiBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFF10B981).withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFF10B981).withValues(alpha: 0.4),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.check_circle_rounded,
            color: Color(0xFF34D399),
            size: 12,
          ),
          const SizedBox(width: 4),
          Text(
            "ไม่มีกาลกิณี",
            style: GoogleFonts.prompt(
              color: const Color(0xFF34D399),
              fontSize: 10,
              fontWeight: FontWeight.bold,
              height: 1.2,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildKakiWarningBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFFEF4444).withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFFEF4444).withValues(alpha: 0.4),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.warning_amber_rounded,
            color: Color(0xFFF87171),
            size: 12,
          ),
          const SizedBox(width: 4),
          Text(
            "มีกาลกิณี",
            style: GoogleFonts.prompt(
              color: const Color(0xFF94A3B8),
              fontSize: 10,
              fontWeight: FontWeight.bold,
              height: 1.2,
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _initTts() async {
    _flutterTts = FlutterTts();
    try {
      await _flutterTts!.setLanguage('th-TH');
      await _flutterTts!.setSpeechRate(0.35);
      await _flutterTts!.setPitch(1.0);
      await _flutterTts!.setVolume(1.0);
      await _flutterTts!.awaitSpeakCompletion(true);
    } catch (_) {}

    _flutterTts!.setCompletionHandler(() {
      if (mounted) setState(() => _speakingName = null);
    });
    _flutterTts!.setCancelHandler(() {
      if (mounted) setState(() => _speakingName = null);
    });
    _flutterTts!.setErrorHandler((_) {
      if (mounted) setState(() => _speakingName = null);
    });
  }

  Future<void> _speakNameAndMeaning(UserSavedName item) async {
    if (_flutterTts == null) {
      await _initTts();
    }

    if (_speakingName == item.name) {
      await _flutterTts!.stop();
      setState(() => _speakingName = null);
      return;
    }

    setState(() => _speakingName = item.name);

    final meaningText = _getDisplayMeaning(item).isNotEmpty
        ? _getDisplayMeaning(item)
        : (item.analysis.isNotEmpty
            ? item.analysis.split('\n').first
            : item.rootWord);

    final textToSpeak = "${item.name} แปลว่า $meaningText";

    try {
      await _flutterTts!.speak(textToSpeak);
    } catch (_) {
      if (mounted) setState(() => _speakingName = null);
    }
  }

  @override
  void dispose() {
    _flutterTts?.stop();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _loadSavedNames();
  }

  Future<void> _loadSavedNames() async {
    setState(() => _isLoading = true);
    try {
      final deviceId = await _apiService.getDeviceId();
      final names = await _apiService.listSavedNames(deviceId: deviceId);
      setState(() => _savedNames = names);
      // Fetch meanings for items that don't have one yet (legacy data)
      _fetchMissingMeanings();
    } catch (e) {
      // Error
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _fetchMissingMeanings() async {
    for (final item in _savedNames) {
      if (item.meaning.isEmpty && !_meanings.containsKey(item.name)) {
        final meaning = await _apiService.getNameMeaning(item.name);
        if (meaning != null && meaning.trim().isNotEmpty && mounted) {
          setState(() {
            _meanings[item.name] = meaning.trim();
          });
        }
      }
    }
  }

  /// Get the display meaning for a saved name item
  String _getDisplayMeaning(UserSavedName item) {
    if (item.meaning.isNotEmpty) return item.meaning;
    if (_meanings.containsKey(item.name)) return _meanings[item.name]!;
    return ''; // Will show loading or fallback
  }

  Future<NumberMeaningResult?> _getNumberMeaningCached(int number) async {
    final key = zeroPad(number);
    if (_numberMeaningCache.containsKey(key)) {
      return _numberMeaningCache[key];
    }

    final result = await _apiService.getNumberMeaning(key);
    _numberMeaningCache[key] = result;
    return result;
  }

  List<String> _extractDisplayPairs(int number) {
    return toPairList(number);
  }

  Future<_ShareNumberMeaning?> _getShareNumberMeaning(int number) async {
    final pairs = _extractDisplayPairs(number);
    final parts = <NumberMeaningResult>[];

    for (final pair in pairs) {
      final value = int.tryParse(pair);
      if (value == null) continue;
      final meaning = await _getNumberMeaningCached(value);
      if (meaning != null && meaning.description.trim().isNotEmpty) {
        parts.add(meaning);
      }
    }

    if (parts.isEmpty) return null;

    if (parts.length == 1) {
      return _ShareNumberMeaning(
        labelNumber: pairs.join(', '),
        description: parts.first.description.trim(),
      );
    }

    final combinedDescription = parts
        .map((p) => '${p.number}: ${p.description.trim()}')
        .join(' • ');

    return _ShareNumberMeaning(
      labelNumber: pairs.join(', '),
      description: combinedDescription,
    );
  }

  Future<void> _shareSavedNameCard(UserSavedName item) async {
    if (_isSharing) return;

    final messenger = ScaffoldMessenger.of(context);
    setState(() => _isSharing = true);

    try {
      final satMeaning = await _getShareNumberMeaning(item.satSum);
      final shaMeaning = await _getShareNumberMeaning(item.shaSum);
      final imageBytes = await _captureSharePoster(
        _buildSavedSharePoster(
          item,
          satMeaning: satMeaning,
          shaMeaning: shaMeaning,
        ),
      );
      if (!mounted) return;
      await _showSharePreview(item, imageBytes);
    } catch (error, stackTrace) {
      debugPrint('Share saved card failed: $error');
      debugPrintStack(stackTrace: stackTrace);
      await Clipboard.setData(ClipboardData(text: _buildShareCaption(item)));
      if (!mounted) return;
      messenger.showSnackBar(
        const SnackBar(
          content: Text('แชร์รูปภาพไม่สำเร็จ ระบบคัดลอกข้อความไว้ให้แล้ว'),
          backgroundColor: Colors.deepOrange,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isSharing = false);
      }
    }
  }

  Future<void> _showSharePreview(
    UserSavedName item,
    Uint8List imageBytes,
  ) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
            child: Container(
              decoration: BoxDecoration(
                color: const Color(0xFFF9F7FF),
                borderRadius: BorderRadius.circular(28),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.primary.withValues(alpha: 0.14),
                    blurRadius: 28,
                    offset: const Offset(0, 18),
                  ),
                ],
              ),
              child: SingleChildScrollView(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(18, 18, 18, 22),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              'Preview ก่อนแชร์',
                              style: GoogleFonts.prompt(
                                color: AppColors.textLight,
                                fontSize: 20,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                          IconButton(
                            onPressed: () => Navigator.pop(sheetContext),
                            icon: const Icon(Icons.close_rounded),
                          ),
                        ],
                      ),
                      Text(
                        'ภาพนี้คือ card ชื่อที่บันทึกจริงที่จะถูกแชร์ไปยัง social',
                        style: GoogleFonts.sarabun(
                          color: AppColors.textGray,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 16),
                      ConstrainedBox(
                        constraints: BoxConstraints(
                          maxHeight:
                              MediaQuery.of(sheetContext).size.height * 0.48,
                        ),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(24),
                          child: Image.memory(imageBytes, fit: BoxFit.contain),
                        ),
                      ),
                      const SizedBox(height: 18),
                      Row(
                        children: [
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: () async {
                                Navigator.pop(sheetContext);
                                await _sharePreviewImage(item, imageBytes);
                              },
                              style: FilledButton.styleFrom(
                                backgroundColor: AppColors.primary,
                                foregroundColor: Colors.white,
                                padding: const EdgeInsets.symmetric(
                                  vertical: 14,
                                ),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(18),
                                ),
                              ),
                              icon: const Icon(Icons.ios_share_rounded),
                              label: const Text('แชร์เลย'),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: () async {
                                await Clipboard.setData(
                                  ClipboardData(text: _buildShareCaption(item)),
                                );
                                if (!sheetContext.mounted) return;
                                Navigator.pop(sheetContext);
                                if (!mounted) return;
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text('คัดลอกข้อความแชร์ไว้แล้ว'),
                                    backgroundColor: AppColors.success,
                                  ),
                                );
                              },
                              style: OutlinedButton.styleFrom(
                                foregroundColor: AppColors.textLight,
                                padding: const EdgeInsets.symmetric(
                                  vertical: 14,
                                ),
                                side: BorderSide(
                                  color: AppColors.primary.withValues(
                                    alpha: 0.2,
                                  ),
                                ),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(18),
                                ),
                              ),
                              icon: const Icon(Icons.content_copy_rounded),
                              label: const Text('คัดลอกข้อความ'),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Future<void> _sharePreviewImage(
    UserSavedName item,
    Uint8List imageBytes,
  ) async {
    final messenger = ScaffoldMessenger.of(context);
    final shareBox = context.findRenderObject() as RenderBox?;

    try {
      final tempDir = await getTemporaryDirectory();
      final file = File('${tempDir.path}/${_sharePosterFileName(item)}');
      await file.writeAsBytes(imageBytes, flush: true);

      await Share.shareXFiles(
        [XFile(file.path, mimeType: 'image/png')],
        subject: 'ชื่อมงคล ${item.name}',
        sharePositionOrigin: shareBox == null
            ? null
            : shareBox.localToGlobal(Offset.zero) & shareBox.size,
      );
    } catch (error, stackTrace) {
      debugPrint('Share saved preview image failed: $error');
      debugPrintStack(stackTrace: stackTrace);
      await Clipboard.setData(ClipboardData(text: _buildShareCaption(item)));
      if (!mounted) return;
      messenger.showSnackBar(
        const SnackBar(
          content: Text('แชร์รูปภาพไม่สำเร็จ ระบบคัดลอกข้อความไว้ให้แล้ว'),
          backgroundColor: Colors.deepOrange,
        ),
      );
    }
  }

  String _sharePosterFileName(UserSavedName item) {
    final safeName = item.name
        .replaceAll(RegExp(r'[^\wก-๙]+'), '_')
        .replaceAll(RegExp(r'_+'), '_');
    return 'saved_name_$safeName.png';
  }

  String _buildShareCaption(UserSavedName item) {
    final meaning = _getDisplayMeaning(item).isNotEmpty
        ? _getDisplayMeaning(item)
        : item.analysis;
    return '"${item.name}"\n'
        '$meaning\n'
        'เลขศาสตร์ ${item.satSum} คือสัญลักษณ์ที่สะท้อนพลังตัวเลขของชื่อ\n'
        'พลังเงา ${item.shaSum} คือสัญลักษณ์ที่บอกแรงดึงดูดและอิทธิพลของชื่อ\n'
        'เปลี่ยนชีวิตด้วยแรงดึงดูด --ชื่อดี.com';
  }

  Future<Uint8List> _captureSharePoster(Widget poster) async {
    final overlay = Overlay.maybeOf(context, rootOverlay: true);
    if (overlay == null) {
      throw StateError('Overlay not available for share poster capture');
    }

    late OverlayEntry entry;
    entry = OverlayEntry(
      builder: (context) {
        return IgnorePointer(
          child: Material(
            color: Colors.transparent,
            child: Center(
              child: Opacity(
                opacity: 0.01,
                child: RepaintBoundary(key: _sharePosterKey, child: poster),
              ),
            ),
          ),
        );
      },
    );

    overlay.insert(entry);
    await WidgetsBinding.instance.endOfFrame;
    await WidgetsBinding.instance.endOfFrame;
    await Future.delayed(const Duration(milliseconds: 30));

    try {
      RenderRepaintBoundary? boundary;
      for (var i = 0; i < 10; i++) {
        boundary =
            _sharePosterKey.currentContext?.findRenderObject()
                as RenderRepaintBoundary?;
        if (boundary != null &&
            boundary.debugNeedsPaint == false &&
            boundary.debugNeedsLayout == false) {
          break;
        }
        await Future.delayed(const Duration(milliseconds: 20));
      }

      if (boundary == null ||
          boundary.debugNeedsPaint ||
          boundary.debugNeedsLayout) {
        throw StateError('Share poster boundary not ready');
      }

      final image = await boundary.toImage(pixelRatio: 3);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      if (byteData == null) {
        throw StateError('Share poster bytes unavailable');
      }
      return byteData.buffer.asUint8List();
    } finally {
      entry.remove();
    }
  }

  Future<void> _deleteName(int id) async {
    final success = await _apiService.deleteSavedName(id);
    if (success) {
      setState(() {
        _savedNames.removeWhere((n) {
          if (n.id == id) {
            ApiService.savedNamesCache.remove(n.name);
            return true;
          }
          return false;
        });
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("ลบออกจากคลังชื่อแล้วค่ะ")),
        );
      }
    }
  }

  Future<void> _confirmDelete(UserSavedName item) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: AppColors.primary, width: 2),
          ),
          title: Text(
            "ลบชื่อออกจากคลัง?",
            style: GoogleFonts.sarabun(
              fontWeight: FontWeight.bold,
              color: AppColors.textLight,
            ),
          ),
          content: Text(
            "แน่ใจนะคะว่าจะลบ “${item.name}” ออกจากคลังชื่อ",
            style: GoogleFonts.sarabun(color: AppColors.textGray),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text(
                "ยกเลิก",
                style: TextStyle(color: AppColors.textGray),
              ),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text(
                "ลบ",
                style: TextStyle(color: Colors.redAccent),
              ),
            ),
          ],
        );
      },
    );

    if (ok == true) {
      await _deleteName(item.id);
    }
  }

  Future<void> _confirmClearAll() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: AppColors.primary, width: 2),
          ),
          title: Text(
            "ล้างคลังรายชื่อทั้งหมด?",
            style: GoogleFonts.sarabun(
              fontWeight: FontWeight.bold,
              color: AppColors.textLight,
            ),
          ),
          content: Text(
            "แน่ใจนะคะว่าจะล้างรายชื่อทั้งหมดออกเพื่อความปลอดภัยของข้อมูลส่วนตัว? การกระทำนี้ไม่สามารถย้อนคืนได้",
            style: GoogleFonts.sarabun(color: AppColors.textGray),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text(
                "ยกเลิก",
                style: TextStyle(color: AppColors.textGray),
              ),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text(
                "ล้างข้อมูลทั้งหมด",
                style: TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        );
      },
    );

    if (ok == true) {
      await _clearAllNames();
    }
  }

  Future<void> _clearAllNames() async {
    setState(() => _isLoading = true);
    try {
      final success = await _apiService.clearAllSavedNames();
      if (success) {
        setState(() {
          _savedNames.clear();
        });
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text("ล้างข้อมูลรายชื่อทั้งหมดเรียบร้อยแล้วค่ะ")),
          );
        }
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text("เกิดข้อผิดพลาดในการล้างข้อมูล"),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _chooseUseMode(UserSavedName item) async {
    await showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF0B1220),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        item.name,
                        style: GoogleFonts.sarabun(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.pop(context),
                      icon: const Icon(Icons.close, color: Colors.white54),
                    ),
                  ],
                ),
              ),
              ListTile(
                leading: const Icon(
                  Icons.search_rounded,
                  color: Colors.white70,
                ),
                title: Text(
                  "ใช้เป็นความหมายที่อยากได้",
                  style: GoogleFonts.sarabun(color: Colors.white),
                ),
                subtitle: Text(
                  "เช่น “อ่อนโยน”, “ฉลาด”, “แข็งแรง”",
                  style: GoogleFonts.sarabun(
                    color: Colors.white54,
                    fontSize: 12,
                  ),
                ),
                onTap: () => Navigator.pop(context, {
                  "mode": "semantic",
                  "name": item.name,
                  "meaning": _getDisplayMeaning(item),
                }),
              ),
              ListTile(
                leading: const Icon(
                  Icons.compare_arrows_rounded,
                  color: Colors.white70,
                ),
                title: Text(
                  "ถอดรหัสชื่อ เลขศาสตร์ พลังเงา",
                  style: GoogleFonts.sarabun(color: Colors.white),
                ),
                subtitle: Text(
                  "เพื่อดูความหมายของรายชื่อที่คล้ายกันกับชื่อต้นแบบ",
                  style: GoogleFonts.sarabun(
                    color: Colors.white54,
                    fontSize: 12,
                  ),
                ),
                onTap: () => Navigator.pop(context, {
                  "mode": "decode",
                  "name": item.name,
                  "meaning": _getDisplayMeaning(item),
                }),
              ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    ).then((value) {
      if (!mounted) return;
      if (value != null) {
        Navigator.pop(context, value);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgDark, // Premium Cream
      appBar: AppBar(
        title: Text(
          "รายชื่อที่บันทึก",
          style: GoogleFonts.sarabun(
            fontWeight: FontWeight.bold,
            color: const Color(0xFF3D2600),
          ),
        ),
        iconTheme: const IconThemeData(color: Color(0xFF3D2600)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
        actions: _savedNames.isEmpty
            ? null
            : [
                IconButton(
                  tooltip: "ล้างคลังรายชื่อทั้งหมด",
                  icon: const Icon(
                    Icons.delete_sweep_rounded,
                    color: Color(0xFF3D2600),
                  ),
                  onPressed: _confirmClearAll,
                ),
              ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _savedNames.isEmpty
          ? _buildEmptyState()
          : RefreshIndicator(
              onRefresh: _loadSavedNames,
              child: ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: _savedNames.length,
                itemBuilder: (context, index) {
                  return _buildSavedNameCard(_savedNames[index]);
                },
              ),
            ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.collections_bookmark_rounded,
            size: 80,
            color: AppColors.textLight.withValues(alpha: 0.1),
          ),
          const SizedBox(height: 16),
          Text(
            "ยังไม่มีชื่อในคลังเลยค่ะ",
            style: GoogleFonts.sarabun(
              color: AppColors.textLight.withValues(alpha: 0.4),
              fontSize: 18,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back_rounded, size: 18),
            label: Text(
              "กลับไปค้นหาชื่อ",
              style: GoogleFonts.prompt(fontWeight: FontWeight.bold),
            ),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSavedNameCard(UserSavedName item) {
    final style = _getDynamicCardStyle(item);
    final lucky = _computeSavedLuckyBreakdown(item);

    String luckText;
    List<Color> gradientColors;
    if (lucky.isLucky) {
      if (lucky.multiplier <= 1) {
        luckText = 'Lucky ✨';
      } else {
        String prefix = lucky.multiplier >= 4
            ? 'Super'
            : (lucky.multiplier == 3
                  ? 'Triple'
                  : 'Double');
        luckText = '$prefix Lucky x${lucky.multiplier}';
      }

      if (lucky.multiplier >= 4) {
        gradientColors = [
          const Color(0xFFDBB632),
          const Color(0xFFFF8C00),
          const Color(0xFFFF4FA3),
          const Color(0xFFB517FF),
        ];
      } else if (lucky.multiplier == 3) {
        gradientColors = [
          const Color(0xFFDBB632),
          const Color(0xFFFF8C00),
          const Color(0xFFFF4FA3),
        ];
      } else {
        gradientColors = [
          const Color(0xFFDBB632),
          const Color(0xFFFF8C00),
        ];
      }
    } else {
      luckText = 'น่าเสียดาย';
      gradientColors = [
        const Color(0xFF71717A),
        const Color(0xFF3F3F46),
      ];
    }

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        gradient: style.bgGradient,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: style.borderColor,
          width: 1.5,
        ),
        boxShadow: [
          BoxShadow(
            color: style.glowColor,
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(18),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () => _chooseUseMode(item),
            child: Stack(
              children: [
                Positioned.fill(
                  child: CustomPaint(
                    painter: _LVMonogramPatternPainter(color: style.patternColor),
                  ),
                ),
                // Main content
                Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 44, 20, 16),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Left: Name + badges + meaning
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.center,
                              children: [
                                // Name
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  crossAxisAlignment: CrossAxisAlignment.center,
                                  children: [
                                    Flexible(
                                      child: FittedBox(
                                        fit: BoxFit.scaleDown,
                                        alignment: Alignment.center,
                                        child: _buildNameText(context, item, fontSize: 24),
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    _buildSectionSpeakButton(
                                      compact: true,
                                      isSpeaking: _speakingName == item.name,
                                      onTap: () => _speakNameAndMeaning(item),
                                      tooltip: "อ่านออกเสียงภาษาไทย",
                                      icon: _speakingName == item.name
                                          ? Icons.volume_up_rounded
                                          : Icons.mic_rounded,
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Wrap(
                                  spacing: 6,
                                  runSpacing: 4,
                                  alignment: WrapAlignment.center,
                                  children: [
                                    if (item.noKaki) _buildNoKakiBadge(),
                                    if (!item.noKaki) _buildKakiWarningBadge(),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                // Meaning / Analysis preview
                                Text(
                                  _getDisplayMeaning(item).isNotEmpty
                                      ? "\"${_getDisplayMeaning(item)}\""
                                      : (item.analysis.isNotEmpty
                                            ? "\"${item.analysis.split('\n').first}\""
                                            : "\"${item.rootWord}\""),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    color: style.meaningColor,
                                    fontSize: 14,
                                    height: 1.5,
                                    fontFamily: 'Sarabun',
                                  ),
                                ),
                                const SizedBox(height: 8),
                                // Date
                                Text(
                                  "บันทึกเมื่อ ${item.createdAt.day}/${item.createdAt.month}/${item.createdAt.year}",
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    color: style.meaningColor.withValues(alpha: 0.6),
                                    fontSize: 10,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ],
                            ),
                          ),

                          const SizedBox(width: 8),

                          // Right: Score circles without labels
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              _buildScoreWithLabel(
                                item.satSum,
                                item.isSatGood,
                                "",
                                item.satPairType,
                              ),
                              const SizedBox(height: 12),
                              _buildScoreWithLabel(
                                item.shaSum,
                                item.isShaGood,
                                "",
                                item.shaPairType,
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    if (_shouldShowPhoneticInsight(item)) ...[
                      Padding(
                        padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
                        child: _buildPhoneticInsightCard(item),
                      ),
                    ],
                    // Action row at bottom spanning full width
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          // Delete button
                          InkWell(
                            onTap: () => _confirmDelete(item),
                            borderRadius: BorderRadius.circular(12),
                            child: Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: Colors.red.withValues(alpha: 0.05),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: Colors.red.withValues(alpha: 0.15),
                                ),
                              ),
                              child: const Icon(
                                Icons.delete_outline_rounded,
                                size: 20,
                                color: Colors.red,
                              ),
                            ),
                          ),
                          const Spacer(),
                          // Root word button (Match image 1 - Gold)
                          InkWell(
                            onTap: () => _showSavedAnalysisDialog(item),
                            borderRadius: BorderRadius.circular(12),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 8,
                              ),
                              decoration: BoxDecoration(
                                gradient: AppColors.goldGradient,
                                borderRadius: BorderRadius.circular(12),
                                boxShadow: [
                                  BoxShadow(
                                    color: AppColors.accent.withValues(
                                      alpha: 0.3,
                                    ),
                                    blurRadius: 8,
                                    offset: const Offset(0, 2),
                                  ),
                                ],
                              ),
                              child: const Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    Icons.auto_stories_rounded,
                                    color: Color(0xFF3D2600),
                                    size: 14,
                                  ),
                                  SizedBox(width: 6),
                                  Text(
                                    "ดูรากศัพท์",
                                    style: TextStyle(
                                      color: Color(0xFF3D2600),
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          InkWell(
                            onTap: () => _shareSavedNameCard(item),
                            borderRadius: BorderRadius.circular(12),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 14,
                                vertical: 8,
                              ),
                              decoration: BoxDecoration(
                                gradient: const LinearGradient(
                                  colors: [
                                    Color(0xFFBAE6FD),
                                    Color(0xFF7DD3FC),
                                  ],
                                  begin: Alignment.topLeft,
                                  end: Alignment.bottomRight,
                                ),
                                borderRadius: BorderRadius.circular(12),
                                boxShadow: [
                                  BoxShadow(
                                    color: const Color(
                                      0xFF7DD3FC,
                                    ).withValues(alpha: 0.22),
                                    blurRadius: 12,
                                    offset: const Offset(0, 5),
                                  ),
                                ],
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    Icons.ios_share_rounded,
                                    size: 15,
                                    color: const Color(
                                      0xFF0F4C81,
                                    ).withValues(alpha: _isSharing ? 0.75 : 1),
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    'แชร์',
                                    style: GoogleFonts.sarabun(
                                      color: const Color(0xFF0F4C81).withValues(
                                        alpha: _isSharing ? 0.75 : 1,
                                      ),
                                      fontSize: 13,
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                // Rank badge at top-left
                Positioned(
                  top: 0,
                  left: 0,
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(12, 8, 16, 8),
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          (item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                  ? const Color(0xFFD4AF37) // Gold
                                  : item.rankPosition == 2
                                      ? const Color(0xFFC0C0C0) // Silver
                                      : item.rankPosition == 3
                                          ? const Color(0xFFCD7F32) // Bronze
                                          : style.accentColor)
                              .withValues(alpha: 0.16),
                          (item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                  ? const Color(0xFFD4AF37)
                                  : item.rankPosition == 2
                                      ? const Color(0xFFC0C0C0)
                                      : item.rankPosition == 3
                                          ? const Color(0xFFCD7F32)
                                          : style.accentColor)
                              .withValues(alpha: 0.04),
                        ],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: const BorderRadius.only(
                        topLeft: Radius.circular(18),
                        bottomRight: Radius.circular(8),
                      ),
                      border: Border.all(
                        color: (item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                ? const Color(0xFFD4AF37)
                                : item.rankPosition == 2
                                    ? const Color(0xFFC0C0C0)
                                    : item.rankPosition == 3
                                        ? const Color(0xFFCD7F32)
                                        : style.accentColor)
                            .withValues(alpha: 0.35),
                        width: 1,
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (item.rankPosition <= 3 || item.rankPosition == 0) ...[
                          Icon(
                            Icons.emoji_events,
                            size: 13,
                            color: item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                ? const Color(0xFFD4AF37)
                                : item.rankPosition == 2
                                    ? const Color(0xFFC0C0C0)
                                    : item.rankPosition == 3
                                        ? const Color(0xFFCD7F32)
                                        : style.accentColor,
                          ),
                          const SizedBox(width: 5),
                        ],
                        Text(
                          item.rankPosition > 0
                              ? 'อันดับ #${item.rankPosition} | ${item.displayRankScoreExact.toStringAsFixed(2)} คะแนน'
                              : 'คะแนนชื่อดี | ${item.displayRankScoreExact.toStringAsFixed(2)} คะแนน',
                          style: GoogleFonts.prompt(
                            color: item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                ? const Color(0xFFB8860B)
                                : item.rankPosition == 2
                                    ? const Color(0xFF64748B)
                                    : item.rankPosition == 3
                                        ? const Color(0xFFCD7F32)
                                        : style.nameColor,
                            fontSize: 11,
                            fontWeight: FontWeight.w900,
                            letterSpacing: 0.2,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                // Lucky badge at top-right
                Positioned(
                  top: 0,
                  right: 0,
                  child: GestureDetector(
                    onTap: () => _showLuckExplanationDialog(item),
                    child: Container(
                      padding: const EdgeInsets.fromLTRB(16, 8, 12, 8),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: gradientColors,
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: const BorderRadius.only(
                          bottomLeft: Radius.circular(8),
                          topRight: Radius.circular(18),
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.2),
                            blurRadius: 8,
                            offset: const Offset(-2, 2),
                          ),
                        ],
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.3),
                          width: 1,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            lucky.isLucky
                                ? Icons.auto_awesome
                                : Icons.info_outline_rounded,
                            color: Colors.white,
                            size: 14,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            luckText,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              letterSpacing: 0.5,
                              shadows: [
                                Shadow(
                                  color: Colors.black26,
                                  offset: Offset(0, 1),
                                  blurRadius: 2,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
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

  Widget _buildSectionSpeakButton({
    required bool compact,
    required bool isSpeaking,
    required VoidCallback onTap,
    required String tooltip,
    required IconData icon,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: EdgeInsets.symmetric(
          horizontal: compact ? 10 : 12,
          vertical: compact ? 7 : 8,
        ),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: isSpeaking
                ? [const Color(0xFF22C55E), const Color(0xFF16A34A)]
                : [const Color(0xFFEFF6FF), const Color(0xFFBFDBFE)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(15),
          border: Border.all(
            color: isSpeaking
                ? const Color(0xFF16A34A)
                : const Color(0xFF93C5FD),
            width: 1,
          ),
        ),
        child: Icon(
          icon,
          size: compact ? 14 : 16,
          color: isSpeaking ? Colors.white : const Color(0xFF1D4ED8),
        ),
      ),
    );
  }

  bool _shouldShowPhoneticInsight(UserSavedName item) {
    return true;
  }

  String _buildPhoneticInsightText(UserSavedName item) {
    if (item.phoneticSummary.trim().isNotEmpty) {
      return item.phoneticSummary.trim();
    }

    // Generate a premium phonetic summary deterministically based on the name length and characters
    final nameTrimmed = item.name.trim();
    if (nameTrimmed.length <= 4) {
      return "ชื่อสั้น กระชับ ฟังดูเป็นธรรมชาติอย่างยิ่ง ออกเสียงง่ายเรียกสบายในชีวิตประจำวัน";
    }

    final int hash = nameTrimmed.hashCode.abs();
    final int index = hash % 4;

    switch (index) {
      case 0:
        return "โทนเสียงละมุน นุ่มลึก และจังหวะลงตัว ฟังแล้วติดหูและน่าเกรงขาม";
      case 1:
        return "ออกเสียงลื่น ปากเปิดง่าย และน้ำเสียงฟังนุ่มละมุนดูเป็นมิตร";
      case 2:
        return "น้ำหนักเสียงแน่น จังหวะดี เรียกแล้วฟังชัดเจนและเปี่ยมด้วยพลัง";
      default:
        return "เสียงค่อนข้างหวาน ละมุนหู และฟังราบรื่นเรียบหรูดูมีระดับ";
    }
  }

  Widget _buildPhoneticInsightCard(UserSavedName item, {bool showSpeakButton = true}) {
    final phoneticText = _buildPhoneticInsightText(item);
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          decoration: BoxDecoration(
            color: const Color(0xFFFAF6F2), // Soft warm sand/linen
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFE8D3C3), width: 1.2), // Soft warm tan/beige border
          ),
          child: Padding(
            padding: EdgeInsets.only(right: showSpeakButton ? 54 : 14),
            child: Text(
              phoneticText,
              style: GoogleFonts.sarabun(
                color: const Color(0xFF4E3629), // Elegant dark warm-brown text
                fontSize: 14,
                height: 1.35,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
        if (showSpeakButton)
          Positioned(
            top: 0,
            bottom: 0,
            right: 10,
            child: Align(
              alignment: Alignment.centerRight,
              child: _buildPhoneticSpeakButton(phoneticText),
            ),
          ),
      ],
    );
  }

  Widget _buildPhoneticSpeakButton(String text) {
    final key = "phonetic:$text";
    final isSpeaking = _speakingName == key;
    return Tooltip(
      message: "อ่านออกเสียงคำอธิบาย",
      child: GestureDetector(
        onTap: () => _speakPhoneticText(text),
        child: Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: isSpeaking
                  ? [const Color(0xFFD97706), const Color(0xFFB45309)] // Warm gold/amber when active
                  : [const Color(0xFFFDFBF7), const Color(0xFFE8DCD0)], // Warm cream to soft tan
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            shape: BoxShape.circle,
            border: Border.all(
              color: isSpeaking
                  ? const Color(0xFFB45309)
                  : const Color(0xFFD2B48C),
              width: 1.2,
            ),
            boxShadow: [
              BoxShadow(
                color:
                    (isSpeaking
                            ? const Color(0xFFD97706)
                            : const Color(0xFFD2B48C))
                        .withValues(alpha: 0.15),
                blurRadius: 6,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Icon(
            isSpeaking ? Icons.volume_up_rounded : Icons.mic_rounded,
            size: 14,
            color: isSpeaking ? Colors.white : const Color(0xFF8B5A2B), // White when active, warm brown otherwise
          ),
        ),
      ),
    );
  }

  Future<void> _speakPhoneticText(String text) async {
    if (_flutterTts == null) {
      await _initTts();
    }

    final key = "phonetic:$text";
    if (_speakingName == key) {
      await _flutterTts!.stop();
      setState(() => _speakingName = null);
      return;
    }

    setState(() => _speakingName = key);

    try {
      await _flutterTts!.speak(text);
    } catch (_) {
      if (mounted) setState(() => _speakingName = null);
    }
  }

  Widget _buildSavedSharePoster(
    UserSavedName item, {
    _ShareNumberMeaning? satMeaning,
    _ShareNumberMeaning? shaMeaning,
  }) {
    final style = _getDynamicCardStyle(item);
    final lucky = _computeSavedLuckyBreakdown(item);

    String luckText;
    List<Color> gradientColors;
    if (lucky.isLucky) {
      if (lucky.multiplier <= 1) {
        luckText = 'Lucky ✨';
      } else {
        String prefix = lucky.multiplier >= 4
            ? 'Super'
            : (lucky.multiplier == 3
                  ? 'Triple'
                  : 'Double');
        luckText = '$prefix Lucky x${lucky.multiplier}';
      }

      if (lucky.multiplier >= 4) {
        gradientColors = [
          const Color(0xFFDBB632),
          const Color(0xFFFF8C00),
          const Color(0xFFFF4FA3),
          const Color(0xFFB517FF),
        ];
      } else if (lucky.multiplier == 3) {
        gradientColors = [
          const Color(0xFFDBB632),
          const Color(0xFFFF8C00),
          const Color(0xFFFF4FA3),
        ];
      } else {
        gradientColors = [
          const Color(0xFFDBB632),
          const Color(0xFFFF8C00),
        ];
      }
    } else {
      luckText = 'น่าเสียดาย!?';
      gradientColors = [
        const Color(0xFF71717A),
        const Color(0xFF3F3F46),
      ];
    }

    final meaning = _getDisplayMeaning(item).isNotEmpty
        ? _getDisplayMeaning(item)
        : (item.analysis.isNotEmpty
              ? item.analysis.split('\n').first
              : item.rootWord);

    return Container(
      width: 430,
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 20),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFFBF2), Color(0xFFF8F5FF), Color(0xFFF2FBF8)],
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.72),
              borderRadius: BorderRadius.circular(18),
              border: Border.all(
                color: style.borderColor.withValues(alpha: 0.25),
              ),
            ),
            child: Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [style.accentColor, style.patternColor],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(
                    Icons.auto_awesome_rounded,
                    color: Colors.white,
                    size: 17,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'ชื่อดี.com',
                        style: GoogleFonts.prompt(
                          color: style.nameColor,
                          fontSize: 20,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      Text(
                        'เปลี่ยนชีวิตด้วยแรงดึงดูดจากชื่อดี',
                        style: GoogleFonts.sarabun(
                          color: style.meaningColor,
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          ClipRRect(
            borderRadius: BorderRadius.circular(26),
            child: Container(
              width: double.infinity,
              decoration: BoxDecoration(
                gradient: style.bgGradient,
                borderRadius: BorderRadius.circular(26),
                border: Border.all(color: style.borderColor, width: 1.5),
                boxShadow: [
                  BoxShadow(
                    color: style.glowColor,
                    blurRadius: 18,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Stack(
                children: [
                  Positioned.fill(
                    child: CustomPaint(
                      painter: _LVMonogramPatternPainter(color: style.patternColor),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(18, 26, 18, 18),
                    child: Column(
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  colors: [
                                    (item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                            ? const Color(0xFFD4AF37) // Gold
                                            : item.rankPosition == 2
                                                ? const Color(0xFFC0C0C0) // Silver
                                                : item.rankPosition == 3
                                                    ? const Color(0xFFCD7F32) // Bronze
                                                    : style.accentColor) // Modern Purple
                                        .withValues(alpha: 0.16),
                                    (item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                            ? const Color(0xFFD4AF37)
                                            : item.rankPosition == 2
                                                ? const Color(0xFFC0C0C0)
                                                : item.rankPosition == 3
                                                    ? const Color(0xFFCD7F32)
                                                    : style.accentColor)
                                        .withValues(alpha: 0.04),
                                  ],
                                  begin: Alignment.topLeft,
                                  end: Alignment.bottomRight,
                                ),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: (item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                          ? const Color(0xFFD4AF37)
                                          : item.rankPosition == 2
                                              ? const Color(0xFFC0C0C0)
                                              : item.rankPosition == 3
                                                  ? const Color(0xFFCD7F32)
                                                  : style.accentColor)
                                      .withValues(alpha: 0.35),
                                  width: 1,
                                ),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  if (item.rankPosition <= 3 || item.rankPosition == 0) ...[
                                    Icon(
                                      Icons.emoji_events,
                                      size: 12,
                                      color: item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                          ? const Color(0xFFD4AF37)
                                          : item.rankPosition == 2
                                              ? const Color(0xFFC0C0C0)
                                              : item.rankPosition == 3
                                                  ? const Color(0xFFCD7F32)
                                                  : style.accentColor,
                                    ),
                                    const SizedBox(width: 4),
                                  ],
                                  Text(
                                    item.rankPosition > 0
                                        ? 'อันดับ #${item.rankPosition} | ${item.displayRankScoreExact.toStringAsFixed(2)} คะแนน'
                                        : 'คะแนนชื่อดี | ${item.displayRankScoreExact.toStringAsFixed(2)} คะแนน',
                                    style: GoogleFonts.prompt(
                                      color: item.rankPosition == 1 || (item.rankPosition == 0 && lucky.isLucky)
                                          ? const Color(0xFFB8860B)
                                          : item.rankPosition == 2
                                              ? const Color(0xFF64748B)
                                              : item.rankPosition == 3
                                                  ? const Color(0xFFCD7F32)
                                                  : style.nameColor,
                                      fontSize: 10,
                                      fontWeight: FontWeight.w900,
                                      letterSpacing: 0.2,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            GestureDetector(
                              onTap: () => _showLuckExplanationDialog(item),
                              child: Container(
                                padding: const EdgeInsets.fromLTRB(12, 6, 10, 6),
                                decoration: BoxDecoration(
                                  gradient: LinearGradient(
                                    colors: gradientColors,
                                    begin: Alignment.topLeft,
                                    end: Alignment.bottomRight,
                                  ),
                                  borderRadius: BorderRadius.circular(16),
                                  boxShadow: [
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.14),
                                      blurRadius: 10,
                                      offset: const Offset(0, 4),
                                    ),
                                  ],
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Icon(
                                      lucky.isLucky
                                          ? Icons.auto_awesome
                                          : Icons.info_outline_rounded,
                                      color: Colors.white,
                                      size: 13,
                                    ),
                                    const SizedBox(width: 5),
                                    Text(
                                      luckText,
                                      style: GoogleFonts.prompt(
                                        color: Colors.white,
                                        fontSize: 10.5,
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.center,
                                children: [
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    crossAxisAlignment: CrossAxisAlignment.center,
                                    children: [
                                      Flexible(
                                        child: FittedBox(
                                          fit: BoxFit.scaleDown,
                                          alignment: Alignment.center,
                                          child: _buildNameText(context, item, fontSize: 28),
                                        ),
                                      ),
                                    ],
                                  ),
                                  if (meaning.isNotEmpty) ...[
                                    const SizedBox(height: 6),
                                    Text(
                                      "\"$meaning\"",
                                      textAlign: TextAlign.center,
                                      maxLines: 3,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(
                                        color: style.meaningColor,
                                        fontSize: 15,
                                        height: 1.5,
                                        fontFamily: 'Sarabun',
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                            const SizedBox(width: 10),
                            SizedBox(
                              width: 92,
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  _buildShareScoreDisplay(
                                    item.satSum,
                                    item.isSatGood,
                                    'เลขศาสตร์',
                                    pairType: item.satPairType,
                                    labelColor: style.meaningColor,
                                  ),
                                  const SizedBox(height: 10),
                                  _buildShareScoreDisplay(
                                    item.shaSum,
                                    item.isShaGood,
                                    'พลังเงา',
                                    pairType: item.shaPairType,
                                    labelColor: style.meaningColor,
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),
                        _buildPhoneticInsightCard(item, showSpeakButton: false),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildShareScoreDisplay(
    int score,
    bool isGood,
    String label, {
    String pairType = '',
    Color? labelColor,
  }) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        _buildScorePairOrSingle(score, isGood, _sharePairCircleSize, pairType),
        const SizedBox(height: 6),
        Text(
          label,
          style: GoogleFonts.sarabun(
            color: labelColor ?? AppColors.textGray,
            fontSize: 10,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }

  Widget _buildScorePairOrSingle(
    int score,
    bool isGood,
    double size,
    String pairType,
  ) {
    final pairs = toPairList(score);
    if (pairs.length > 1) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (int i = 0; i < pairs.length; i++) ...[
            if (i > 0) const SizedBox(width: 4),
            _buildGradientCircle(pairs[i], isGood, size, pairType),
          ],
        ],
      );
    }
    return _buildGradientCircle(pairs.first, isGood, size, pairType);
  }

  Widget _buildScoreWithLabel(
    int score,
    bool isGood,
    String label,
    String pairType,
  ) {
    Widget circle;
    const double size = _pairCircleSize;

    final pairs = toPairList(score);
    if (pairs.length > 1) {
      circle = Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (int i = 0; i < pairs.length; i++) ...[
            if (i > 0) const SizedBox(width: 4),
            _buildGradientCircle(pairs[i], isGood, size, pairType),
          ],
        ],
      );
    } else {
      circle = _buildGradientCircle(pairs.first, isGood, size, pairType);
    }

    return circle;
  }

  Widget _buildGradientCircle(
    String score,
    bool isGood,
    double size,
    String pairType,
  ) {
    Color lightColor;
    Color darkColor;

    if (isGood) {
      lightColor = const Color(0xFF4ADE80); // Green-400
      darkColor = const Color(0xFF16A34A); // Green-600
    } else {
      lightColor = const Color(0xFFF87171); // Red-400
      darkColor = const Color(0xFFDC2626); // Red-600
    }

    return InkWell(
      onTap: () => _showNumberMeaningDialog(score, isGood),
      borderRadius: BorderRadius.circular(50),
      child: Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [lightColor, darkColor],
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.3),
              blurRadius: 6,
              offset: const Offset(0, 4),
            ),
            BoxShadow(
              color: Colors.white.withValues(alpha: 0.2),
              blurRadius: 0,
              offset: const Offset(-1, -1),
            ),
          ],
          border: Border.all(
            color: Colors.white.withValues(alpha: 0.15),
            width: 1,
          ),
        ),
        child: Text(
          score,
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: size * 18 / 44,
            shadows: const [
              Shadow(
                color: Colors.black26,
                offset: Offset(0, 1),
                blurRadius: 2,
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showSavedAnalysisDialog(UserSavedName item) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: AppColors.primary, width: 2),
          ),
          title: Column(
            children: [
              const Icon(
                Icons.article_rounded,
                color: AppColors.textGray,
                size: 40,
              ),
              const SizedBox(height: 12),
              Text(
                item.name,
                style: GoogleFonts.sarabun(
                  color: AppColors.textLight,
                  fontWeight: FontWeight.bold,
                  fontSize: 28,
                ),
              ),
              Divider(
                color: AppColors.textGray.withValues(alpha: 0.1),
                height: 32,
                thickness: 1,
              ),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                if (_getDisplayMeaning(item).isNotEmpty) ...[
                  Text(
                    _getDisplayMeaning(item),
                    style: GoogleFonts.sarabun(
                      color: AppColors.textLight,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                if (item.rootWord.isNotEmpty) ...[
                  RichText(
                    text: TextSpan(
                      style: GoogleFonts.sarabun(
                        color: AppColors.textGray,
                        fontSize: 16,
                      ),
                      children: [
                        const TextSpan(
                          text: "ที่มาและรากศัพท์: ",
                          style: TextStyle(color: AppColors.textGray),
                        ),
                        TextSpan(
                          text: item.rootWord,
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            color: AppColors.textLight,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                Text(
                  item.analysis,
                  style: GoogleFonts.sarabun(
                    color: AppColors.textGray,
                    height: 1.8,
                    fontSize: 16,
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text(
                "ปิด",
                style: TextStyle(color: AppColors.textGray),
              ),
            ),
          ],
        );
      },
    );
  }

  void _showNumberMeaningDialog(String number, bool isGood) {
    showDialog(
      context: context,
      builder: (context) {
        return FutureBuilder<NumberMeaningResult?>(
          future: _apiService.getNumberMeaning(number),
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const AlertDialog(
                backgroundColor: AppColors.bgDark,
                content: SizedBox(
                  height: 100,
                  child: Center(child: CircularProgressIndicator()),
                ),
              );
            }

            if (snapshot.hasError || snapshot.data == null) {
              return AlertDialog(
                backgroundColor: AppColors.bgDark,
                title: const Text(
                  "ข้อผิดพลาด",
                  style: TextStyle(color: AppColors.textLight),
                ),
                content: const Text(
                  "ไม่สามารถดึงข้อมูลคำทำนายได้",
                  style: TextStyle(color: AppColors.textGray),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text(
                      "ปิด",
                      style: TextStyle(color: AppColors.textGray),
                    ),
                  ),
                ],
              );
            }

            final data = snapshot.data!;
            return AlertDialog(
              backgroundColor: AppColors.bgDark,
              shadowColor: AppColors.primary.withValues(alpha: 0.1),
              surfaceTintColor: Colors.transparent,
              elevation: 20,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
                side: const BorderSide(color: AppColors.primary, width: 2),
              ),
              title: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: isGood
                          ? AppColors.success
                          : const Color(0xFFEF4444),
                      shape: BoxShape.circle,
                      boxShadow: [
                        BoxShadow(
                          color:
                              (isGood
                                      ? AppColors.success
                                      : const Color(0xFFEF4444))
                                  .withValues(alpha: 0.3),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Text(
                      number,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 20,
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          "เลขศาสตร์ $number",
                          style: GoogleFonts.prompt(
                            color: AppColors.textGray.withValues(alpha: 0.7),
                            fontSize: 12,
                          ),
                        ),
                        Text(
                          data.description,
                          style: GoogleFonts.prompt(
                            color: AppColors.textLight,
                            fontWeight: FontWeight.bold,
                            fontSize: 18,
                            height: 1.2,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              content: ConstrainedBox(
                constraints: BoxConstraints(
                  maxHeight: MediaQuery.of(context).size.height * 0.5,
                ),
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Divider(
                        color: AppColors.textGray.withValues(alpha: 0.1),
                        height: 24,
                      ),
                      ..._buildVipDetailParts(
                        data.detail.replaceAll("\\n", "\n"),
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text(
                    "ปิด",
                    style: TextStyle(
                      color: AppColors.textGray,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }

  List<Widget> _buildVipDetailParts(String detailText) {
    final generalStyle = GoogleFonts.sarabun(
      color: AppColors.textGray,
      height: 1.7,
      fontSize: 15,
      letterSpacing: 0.1,
    );
    final goodColor = const Color(0xFF16A34A);
    final badColor = const Color(0xFFDC2626);

    final parts = detailText.split(RegExp(r'ด้านดี\s*คือ'));
    final List<Widget> widgets = [];

    if (parts[0].trim().isNotEmpty) {
      widgets.add(Text(parts[0].trim(), style: generalStyle));
    }

    if (parts.length > 1) {
      final goodBadParts = parts[1].split(RegExp(r'ด้านเสีย\s*คือ'));
      if (goodBadParts[0].trim().isNotEmpty) {
        if (widgets.isNotEmpty) {
          widgets.add(const SizedBox(height: 16));
        }
        widgets.add(
          Text(
            '📗 ด้านดี',
            style: GoogleFonts.prompt(
              color: goodColor,
              fontWeight: FontWeight.w700,
              fontSize: 15,
            ),
          ),
        );
        widgets.add(const SizedBox(height: 4));
        widgets.add(
          Text(
            goodBadParts[0].trim(),
            style: GoogleFonts.sarabun(
              color: goodColor,
              height: 1.7,
              fontSize: 15,
              letterSpacing: 0.1,
            ),
          ),
        );
      }
      if (goodBadParts.length > 1 && goodBadParts[1].trim().isNotEmpty) {
        if (widgets.isNotEmpty) {
          widgets.add(const SizedBox(height: 16));
        }
        widgets.add(
          Text(
            '📕 ด้านเสีย',
            style: GoogleFonts.prompt(
              color: badColor,
              fontWeight: FontWeight.w700,
              fontSize: 15,
            ),
          ),
        );
        widgets.add(const SizedBox(height: 4));
        widgets.add(
          Text(
            goodBadParts[1].trim(),
            style: GoogleFonts.sarabun(
              color: badColor,
              height: 1.7,
              fontSize: 15,
              letterSpacing: 0.1,
            ),
          ),
        );
      }
    }

    if (widgets.isEmpty) {
      widgets.add(Text(detailText, style: generalStyle));
    }

    return widgets;
  }

  void _showLuckExplanationDialog(UserSavedName item) {
    final bool isLucky = item.isSatGood && item.isShaGood;

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: const Color(
            0xFF2A1F0A,
          ), // Consistent with flip back-side color
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
            side: BorderSide(
              color: isLucky
                  ? const Color(0xFFDBB632).withValues(alpha: 0.4)
                  : Colors.white24,
              width: 1.5,
            ),
          ),
          contentPadding: const EdgeInsets.all(24),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Icon(
                        isLucky
                            ? Icons.auto_awesome_rounded
                            : Icons.info_outline_rounded,
                        color: isLucky
                            ? const Color(0xFFDBB632)
                            : const Color(0xFF9CA3AF),
                        size: 20,
                      ),
                      const SizedBox(width: 10),
                      Text(
                        isLucky ? 'ที่มาของโชค' : 'ทำไมถึงน่าเสียดาย?',
                        style: GoogleFonts.prompt(
                          color: isLucky
                              ? const Color(0xFFDBB632)
                              : const Color(0xFF9CA3AF),
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  GestureDetector(
                    onTap: () => Navigator.pop(context),
                    child: const Icon(
                      Icons.close_rounded,
                      color: Colors.white38,
                      size: 20,
                    ),
                  ),
                ],
              ),
              const Divider(color: Colors.white12, height: 24),
              Text(
                isLucky
                    ? 'Double Lucky x2 ได้มาจาก:'
                    : 'วิเคราะห์ข้อบกพร่องของชื่อนี้:',
                style: GoogleFonts.sarabun(
                  color: isLucky ? Colors.white70 : const Color(0xFFFCA5A5),
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 20),
              _buildCriteriaRowInDialog(
                Icons.looks_one_rounded,
                'เลขศาสตร์ (ตัวเลขมงคล)',
                item.isSatGood,
                item.isSatGood
                    ? 'เลขรวม ${item.satSum} — ดีมาก ✓'
                    : 'เลขรวม ${item.satSum} — ไม่ผ่าน',
              ),
              const SizedBox(height: 16),
              _buildCriteriaRowInDialog(
                Icons.blur_on_rounded,
                'พลังเงา (เลขเงา)',
                item.isShaGood,
                item.isShaGood
                    ? 'เลขเงา ${item.shaSum} — ดีมาก ✓'
                    : 'เลขเงา ${item.shaSum} — ไม่ผ่าน',
              ),
              const SizedBox(height: 24),
              Center(
                child: Text(
                  'แตะด้านนอกหรือกดปิดเพื่อกลับ',
                  style: GoogleFonts.sarabun(
                    color: Colors.white24,
                    fontSize: 11,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildCriteriaRowInDialog(
    IconData icon,
    String label,
    bool passed,
    String detail,
  ) {
    final color = passed ? const Color(0xFF4ADE80) : const Color(0xFFF87171);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          passed ? Icons.check_circle_rounded : Icons.cancel_rounded,
          color: color,
          size: 18,
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: GoogleFonts.sarabun(
                  color: color,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                detail,
                style: GoogleFonts.sarabun(color: Colors.white38, fontSize: 12),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class PremiumNameTextEffect extends StatefulWidget {
  final Widget child;

  const PremiumNameTextEffect({super.key, required this.child});

  @override
  State<PremiumNameTextEffect> createState() => _PremiumNameTextEffectState();
}

class _PremiumNameTextEffectState extends State<PremiumNameTextEffect>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _sweep;
  late final Animation<double> _glow;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3800),
    )..repeat();
    _sweep = Tween<double>(
      begin: -1.0,
      end: 2.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    _glow = Tween<double>(begin: 0.35, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOutSine),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ShaderMask buildGradientText() {
      return ShaderMask(
        blendMode: BlendMode.srcIn,
        shaderCallback: (bounds) {
          return LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: const [
              Color(0xFFB517FF),
              Color(0xFFFF4FA3),
              Color(0xFFE879F9),
              Color(0xFFB517FF),
            ],
            stops: [
              (_sweep.value - 0.45).clamp(0.0, 1.0),
              (_sweep.value - 0.15).clamp(0.0, 1.0),
              (_sweep.value + 0.15).clamp(0.0, 1.0),
              (_sweep.value + 0.45).clamp(0.0, 1.0),
            ],
          ).createShader(bounds);
        },
        child: widget.child,
      );
    }

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return Stack(
          alignment: Alignment.centerLeft,
          clipBehavior: Clip.none,
          children: [
            Transform.translate(
              offset: const Offset(0, 1.0),
              child: Opacity(
                opacity: 0.42 * _glow.value,
                child: buildGradientText(),
              ),
            ),
            buildGradientText(),
          ],
        );
      },
    );
  }
}

class _LuckyBreakdown {
  final bool isSatMatch;
  final bool isShaMatch;
  final bool noKaki;
  final bool hasMatchingGood;
  final bool isLucky;
  final int multiplier;
  final bool includesKakiBonus;

  const _LuckyBreakdown({
    required this.isSatMatch,
    required this.isShaMatch,
    required this.noKaki,
    required this.hasMatchingGood,
    required this.isLucky,
    required this.multiplier,
    required this.includesKakiBonus,
  });
}

class _LVMonogramPatternPainter extends CustomPainter {
  final Color color;

  _LVMonogramPatternPainter({required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color.withValues(alpha: 0.12)
      ..style = PaintingStyle.fill;

    final strokePaint = Paint()
      ..color = color.withValues(alpha: 0.10)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.0;

    final double stepX = 52.0;
    final double stepY = 52.0;

    for (double x = 20; x < size.width; x += stepX) {
      for (double y = 20; y < size.height; y += stepY) {
        int col = (x / stepX).floor();
        int row = (y / stepY).floor();
        int patternType = (row % 2 == 0)
            ? (col % 2 == 0 ? 0 : 1)
            : (col % 2 == 0 ? 2 : 3);

        if (patternType == 0) {
          final lPath = Path()
            ..moveTo(x - 5, y - 4)
            ..lineTo(x - 5, y + 4)
            ..lineTo(x + 2, y + 4)
            ..lineTo(x + 2, y + 2.3)
            ..lineTo(x - 3.2, y + 2.3)
            ..lineTo(x - 3.2, y - 4)
            ..close();

          final vPath = Path()
            ..moveTo(x - 1, y - 4)
            ..lineTo(x + 2.5, y + 4)
            ..lineTo(x + 6, y - 4)
            ..lineTo(x + 4.2, y - 4)
            ..lineTo(x + 1.6, y + 1.8)
            ..lineTo(x + 0.6, y - 4)
            ..close();

          canvas.drawPath(lPath, paint);
          canvas.drawPath(vPath, paint);
        } else if (patternType == 1) {
          final starPath = Path();
          final double rOuter = 8.0;
          final double rInner = 2.5;
          for (int i = 0; i < 4; i++) {
            double angle1 = i * math.pi / 2;
            double angle2 = angle1 + math.pi / 4;
            double x1 = x + math.cos(angle1) * rOuter;
            double y1 = y + math.sin(angle1) * rOuter;
            double x2 = x + math.cos(angle2) * rInner;
            double y2 = y + math.sin(angle2) * rInner;
            if (i == 0) {
              starPath.moveTo(x1, y1);
            } else {
              starPath.quadraticBezierTo(x, y, x1, y1);
            }
            starPath.quadraticBezierTo(x, y, x2, y2);
          }
          starPath.quadraticBezierTo(x, y, x + rOuter, y);
          starPath.close();
          canvas.drawPath(starPath, paint);
          canvas.drawCircle(Offset(x, y), 1.5, strokePaint);
        } else if (patternType == 2) {
          canvas.drawCircle(Offset(x, y), 2.5, strokePaint);
          final hollowPath = Path();
          for (int i = 0; i < 4; i++) {
            double angle = i * math.pi / 2;
            double px = x + math.cos(angle) * 7.5;
            double py = y + math.sin(angle) * 7.5;
            double plx = x + math.cos(angle - math.pi / 6) * 3.5;
            double ply = y + math.sin(angle - math.pi / 6) * 3.5;
            double prx = x + math.cos(angle + math.pi / 6) * 3.5;
            double pry = y + math.sin(angle + math.pi / 6) * 3.5;
            hollowPath.moveTo(plx, ply);
            hollowPath.quadraticBezierTo(
              x + math.cos(angle) * 5.0,
              y + math.sin(angle) * 5.0,
              px,
              py,
            );
            hollowPath.quadraticBezierTo(
              x + math.cos(angle) * 5.0,
              y + math.sin(angle) * 5.0,
              prx,
              pry,
            );
          }
          canvas.drawPath(hollowPath, strokePaint);
        } else {
          canvas.drawCircle(Offset(x, y), 8, strokePaint);
          final flowerPath = Path();
          for (int i = 0; i < 4; i++) {
            double angle = i * math.pi / 2;
            double cx = x + math.cos(angle) * 4.2;
            double cy = y + math.sin(angle) * 4.2;
            flowerPath.addOval(
              Rect.fromCircle(center: Offset(cx, cy), radius: 2.0),
            );
          }
          flowerPath.addOval(
            Rect.fromCircle(center: Offset(x, y), radius: 1.0),
          );
          canvas.drawPath(flowerPath, paint);
        }
      }
    }
  }

  @override
  bool shouldRepaint(_LVMonogramPatternPainter old) => old.color != color;
}

class _ThaiHighlightPainter extends CustomPainter {
  final List<CharHighlight> highlights;
  final TextStyle baseStyle;
  final bool isGold;

  _ThaiHighlightPainter({
    required this.highlights,
    required this.baseStyle,
    this.isGold = false,
  });

  static final Set<int> _thaiCombiningMarks = {
    0x0E31,
    0x0E34, 0x0E35, 0x0E36, 0x0E37, 0x0E38, 0x0E39, 0x0E3A,
    0x0E47,
    0x0E48,
    0x0E49,
    0x0E4A,
    0x0E4B,
    0x0E4C,
    0x0E4D,
    0x0E4E,
  };

  bool _isCombining(String char) {
    if (char.isEmpty) return false;
    int code = char.runes.first;
    return _thaiCombiningMarks.contains(code);
  }

  void _draw(Canvas canvas, String text, Color color, double x, double y) {
    final tp = TextPainter(
      text: TextSpan(
        text: text,
        style: baseStyle.copyWith(color: color),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(canvas, Offset(x, y));
  }

  @override
  void paint(Canvas canvas, Size size) {
    final red = const Color(0xFFF87171);
    final defaultColor =
        baseStyle.color ?? (isGold ? const Color(0xFFFFD700) : Colors.white);

    final clusters = <List<CharHighlight>>[];
    for (var h in highlights) {
      if (clusters.isEmpty || !_isCombining(h.char)) {
        clusters.add([h]);
      } else {
        clusters.last.add(h);
      }
    }

    double x = 0;
    final sampleTp = TextPainter(
      text: TextSpan(text: "ที่", style: baseStyle),
      textDirection: TextDirection.ltr,
    )..layout();
    final commonBaseline = sampleTp.computeDistanceToActualBaseline(
      TextBaseline.alphabetic,
    );

    final verticalOffset = (size.height - sampleTp.height) / 2;

    for (var cluster in clusters) {
      final clusterText = cluster.map((e) => e.char).join();
      final base = cluster[0];
      final baseColor = base.isKaki ? red : defaultColor;

      final clusterTp = TextPainter(
        text: TextSpan(text: clusterText, style: baseStyle),
        textDirection: TextDirection.ltr,
      )..layout();
      final clusterBaseline = clusterTp.computeDistanceToActualBaseline(
        TextBaseline.alphabetic,
      );

      final targetBaselineY = verticalOffset + commonBaseline;
      bool hasMixedColors = cluster.any((e) => e.isKaki != base.isKaki);

      if (base.isKaki) {
        _draw(canvas, clusterText, red, x, targetBaselineY - clusterBaseline);
      } else if (!hasMixedColors) {
        _draw(
          canvas,
          clusterText,
          baseColor,
          x,
          targetBaselineY - clusterBaseline,
        );
      } else {
        for (int i = cluster.length; i >= 1; i--) {
          final subCluster = cluster.sublist(0, i);
          final subText = subCluster.map((e) => e.char).join();
          final lastChar = subCluster.last;
          final color = lastChar.isKaki ? red : defaultColor;

          final subTp = TextPainter(
            text: TextSpan(text: subText, style: baseStyle),
            textDirection: TextDirection.ltr,
          )..layout();
          final subBaseline = subTp.computeDistanceToActualBaseline(
            TextBaseline.alphabetic,
          );

          _draw(canvas, subText, color, x, targetBaselineY - subBaseline);
        }
      }

      x += clusterTp.width;
    }
  }

  @override
  bool shouldRepaint(covariant _ThaiHighlightPainter oldDelegate) => true;
}
