import 'dart:async';
import 'dart:io' show File, Platform;
import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import '../models/name_model.dart';
import '../models/name_root_result.dart';
import '../models/number_meaning_model.dart';
import '../services/api_service.dart';
import '../utils/colors.dart';
import '../utils/numerology_format.dart';
import 'gold_effect.dart';

class NameListItem extends StatefulWidget {
  final MobileNameResult result;
  final String? comparisonName; // Name used for matching
  final NameAnalysisResult?
  comparisonAnalysis; // Analysis for the matching name (for Kaki highlight)
  final bool showMatching;
  final VoidCallback? onTap;
  final int rank; // Ranking position (1-based)
  final bool isFilterSatActive;
  final bool isFilterShaActive;
  final bool isFilterKakiActive;

  const NameListItem({
    super.key,
    required this.result,
    this.comparisonName,
    this.comparisonAnalysis,
    this.showMatching = false,
    this.onTap,
    this.rank = 0,
    this.isFilterSatActive = false,
    this.isFilterShaActive = false,
    this.isFilterKakiActive = false,
  });

  @override
  State<NameListItem> createState() => _NameListItemState();
}

enum _TtsStatus { unavailable, noThaiVoice, ready }

class _NameListItemState extends State<NameListItem>
    with TickerProviderStateMixin {
  bool _isSaving = false;
  bool _isSharing = false;
  bool _isSaved = false;
  bool _isFlipped = false;
  String _flipType = 'score'; // 'score' or 'luck'
  NameRootResult? _rootData;
  final bool _isLoadingRoot = false;
  late AnimationController _controller;
  late AnimationController _flipController;
  late Animation<double> _flipAnimation;
  static FlutterTts? _sharedTts;
  static Completer<void>? _ttsInitCompleter;
  static _TtsStatus _globalTtsStatus = _TtsStatus.unavailable;
  FlutterTts get _flutterTts => _sharedTts ??= FlutterTts();
  final GlobalKey _sharePosterKey = GlobalKey();
  NumberMeaningResult? _shareSatMeaning;
  NumberMeaningResult? _shareShaMeaning;
  _TtsStatus _ttsStatus = _TtsStatus.unavailable;
  String? _speakingKey;
  bool _isChainedSpeech = false;

  @override
  void initState() {
    super.initState();
    // Check if name is already saved
    _isSaved = ApiService.savedNamesCache.contains(widget.result.name);

    // Entry animation controller
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );
    _controller.forward();

    // Flip animation controller — value goes 0 (front) to 1 (back)
    _flipController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _flipAnimation = CurvedAnimation(
      parent: _flipController,
      curve: Curves.easeInOut,
    );
    unawaited(_initTts());
  }

  @override
  void dispose() {
    _flutterTts.stop();
    _controller.dispose();
    _flipController.dispose();
    super.dispose();
  }

  Future<void> _initTts() async {
    // Use a static completer to ensure TTS is initialized only once
    if (_ttsInitCompleter != null) {
      await _ttsInitCompleter!.future;
      if (mounted) {
        setState(() => _ttsStatus = _globalTtsStatus);
      }
      return;
    }

    _ttsInitCompleter = Completer<void>();

    bool basicSetupOk = false;
    bool thaiLanguageOk = false;
    bool thaiVoiceFound = false;

    try {
      await _flutterTts.setLanguage('th-TH');
      debugPrint('Set TTS language to: th-TH');
      await _flutterTts.setSpeechRate(0.35);
      await _flutterTts.setPitch(1.0);
      await _flutterTts.setVolume(1.0);
      await _flutterTts.awaitSpeakCompletion(true);
      basicSetupOk = true;
      thaiLanguageOk = true;
    } catch (e) {
      debugPrint('TTS th-TH setup error: $e');
    }

    if (!basicSetupOk) {
      try {
        await _flutterTts.setLanguage('en-US');
        debugPrint('Fallback TTS to en-US');
        await _flutterTts.setSpeechRate(0.35);
        await _flutterTts.setPitch(1.0);
        await _flutterTts.setVolume(1.0);
        await _flutterTts.awaitSpeakCompletion(true);
        basicSetupOk = true;
      } catch (e) {
        debugPrint('TTS en-US fallback error: $e');
      }
    }

    if (basicSetupOk) {
      try {
        final engines = await _flutterTts.getEngines;
        debugPrint('TTS engines available: $engines');
        if (engines is List && engines.isNotEmpty) {
          final dynamic preferredEngine = engines.cast<dynamic>().firstWhere(
            (engine) => '$engine'.toLowerCase().contains('google'),
            orElse: () => engines.first,
          );
          debugPrint('Selected TTS engine: $preferredEngine');
          await _flutterTts.setEngine('$preferredEngine');
        }
      } catch (_) {
        debugPrint('getEngines/setEngine not supported on this platform');
      }

      try {
        final voices = await _flutterTts.getVoices;
        debugPrint('Available voices: $voices');
        if (voices is List) {
          final dynamic thaiVoice = voices.cast<dynamic>().firstWhere(
            (voice) =>
                voice is Map && '${voice['locale'] ?? ''}'.startsWith('th'),
            orElse: () => null,
          );
          if (thaiVoice is Map) {
            debugPrint('Selected Thai voice: $thaiVoice');
            await _flutterTts.setVoice(
              Map<String, String>.from(
                thaiVoice.map((key, value) => MapEntry('$key', '$value')),
              ),
            );
            thaiVoiceFound = true;
          } else {
            debugPrint('No Thai voice found in voice list');
          }
        }
      } catch (_) {
        debugPrint('getVoices/setVoice not supported on this platform');
        // On platforms without getVoices (e.g. some Android), if th-TH setLanguage worked,
        // assume the system will use the default Thai voice
        if (thaiLanguageOk) {
          thaiVoiceFound = true;
        }
      }

      _flutterTts.setCompletionHandler(() {
        if (!mounted) return;
        if (_isChainedSpeech) return;
        setState(() => _speakingKey = null);
      });
      _flutterTts.setCancelHandler(() {
        if (!mounted) return;
        if (_isChainedSpeech) return;
        setState(() => _speakingKey = null);
      });
      _flutterTts.setErrorHandler((_) {
        if (!mounted) return;
        if (_isChainedSpeech) return;
        setState(() => _speakingKey = null);
      });
    }

    final _TtsStatus status;
    if (!basicSetupOk) {
      status = _TtsStatus.unavailable;
    } else if (!thaiLanguageOk && !thaiVoiceFound) {
      status = _TtsStatus.noThaiVoice;
    } else if (thaiLanguageOk && !thaiVoiceFound) {
      status = _TtsStatus.noThaiVoice;
    } else {
      status = _TtsStatus.ready;
    }

    _globalTtsStatus = status;
    if (mounted) {
      setState(() => _ttsStatus = status);
    }
    debugPrint('TTS initialization completed, status: ${status.name}');
    _ttsInitCompleter!.complete();
  }

  void _showTtsHelpSnackBar() {
    final isIOS = Platform.isIOS;

    String message;
    if (_ttsStatus == _TtsStatus.unavailable) {
      message = 'อุปกรณ์นี้ไม่รองรับการอ่านออกเสียง';
    } else if (isIOS) {
      message =
          'กรุณาติดตั้งเสียงภาษาไทย:\nSettings > Accessibility > Spoken Content > Voices > Thai';
    } else {
      message =
          'กรุณาติดตั้งเสียงภาษาไทย:\nSettings > Language & Input > Text-to-Speech > ติดตั้งข้อมูลเสียง > Thai';
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: const TextStyle(fontSize: 13)),
        backgroundColor: _ttsStatus == _TtsStatus.unavailable
            ? Colors.red.shade700
            : Colors.deepOrange,
        duration: const Duration(seconds: 5),
        action: SnackBarAction(
          label: 'ตกลง',
          textColor: Colors.white,
          onPressed: () {},
        ),
      ),
    );
  }

  String _getSpeakTooltip() {
    switch (_ttsStatus) {
      case _TtsStatus.ready:
        return 'อ่านออกเสียงภาษาไทย';
      case _TtsStatus.noThaiVoice:
        return Platform.isIOS
            ? 'ยังไม่มีเสียงไทย — แตะเพื่ออ่านด้วยเสียงที่มี'
            : 'ยังไม่มีเสียงไทย — แตะเพื่ออ่านด้วยเสียงที่มี';
      case _TtsStatus.unavailable:
        return 'TTS ไม่พร้อมใช้งาน';
    }
  }

  String _prepareSpeakableThaiText(String text) {
    final normalized = text
        .replaceAll(RegExp(r'\s+'), ' ')
        .replaceAll('-', ' ')
        .replaceAll('_', ' ')
        .replaceAll('/', ' ')
        .trim();
    return normalized.isEmpty ? text : normalized;
  }

  Future<void> _speakSectionText(String text, String speakingKey) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return;

    if (_ttsStatus != _TtsStatus.ready &&
        _ttsStatus != _TtsStatus.noThaiVoice) {
      if (!mounted) return;
      _showTtsHelpSnackBar();
      return;
    }

    if (_speakingKey == speakingKey) {
      await _flutterTts.stop();
      if (!mounted) return;
      setState(() => _speakingKey = null);
      return;
    }

    await _flutterTts.stop();
    if (!mounted) return;
    setState(() => _speakingKey = speakingKey);

    try {
      await _flutterTts.speak(trimmed);
    } catch (_) {
      if (!mounted) return;
      setState(() => _speakingKey = null);
    }
  }

  Future<void> _speakNameOnly() async {
    await _speakNameAndMeaning();
  }

  Future<void> _speakMeaningOnly() async {
    await _speakSectionText(
      _prepareSpeakableThaiText(widget.result.meaning),
      'meaning:${widget.result.name}',
    );
  }

  Future<void> _speakPhoneticOnly() async {
    await _speakSectionText(
      _prepareSpeakableThaiText(_buildPhoneticInsightText()),
      _phoneticSpeakingKey(),
    );
  }

  Future<void> _speakNameAndMeaning() async {
    if (_ttsStatus != _TtsStatus.ready &&
        _ttsStatus != _TtsStatus.noThaiVoice) {
      if (!mounted) return;
      _showTtsHelpSnackBar();
      return;
    }

    final speakingKey = _nameMeaningSpeakingKey();

    if (_speakingKey == speakingKey) {
      await _flutterTts.stop();
      if (!mounted) return;
      setState(() => _speakingKey = null);
      return;
    }

    final name = _prepareSpeakableThaiText(widget.result.name);
    final meaning = _prepareSpeakableThaiText(widget.result.meaning);
    if (name.isEmpty && meaning.isEmpty) return;

    await _flutterTts.stop();
    if (!mounted) return;
    setState(() => _speakingKey = speakingKey);
    _isChainedSpeech = true;

    try {
      if (name.isNotEmpty) {
        await _flutterTts.speak(name);
      }
      await Future<void>.delayed(const Duration(milliseconds: 300));
      if (meaning.isNotEmpty) {
        await _flutterTts.speak(meaning);
      }
    } catch (_) {
      if (!mounted) return;
      setState(() => _speakingKey = null);
    } finally {
      _isChainedSpeech = false;
      if (mounted) {
        setState(() => _speakingKey = null);
      }
    }
  }

  bool _isSpeaking(String speakingKey) => _speakingKey == speakingKey;

  String _nameMeaningSpeakingKey() => 'name-meaning:${widget.result.name}';

  String _phoneticSpeakingKey() => 'phonetic:${widget.result.name}';

  Future<void> _saveName() async {
    if (_isSaved) return;

    setState(() => _isSaving = true);

    try {
      final deviceId = await ApiService().getDeviceId();
      final rootData = await ApiService().getNameRoot(widget.result.name);

      final payload = {
        "name": widget.result.name,
        "sat_sum": widget.result.satSum,
        "sha_sum": widget.result.shaSum,
        "is_sat_good": widget.result.isSatGood,
        "is_sha_good": widget.result.isShaGood,
        "root_word": rootData?.rootWord ?? "",
        "analysis": rootData?.analysis ?? widget.result.meaning,
        "device_id": deviceId,
      };

      final success = await ApiService().saveName(payload);
      if (success) {
        setState(() => _isSaved = true);
        ApiService.savedNamesCache.add(widget.result.name);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text("บันทึกชื่อ ${widget.result.name} แล้ว"),
              backgroundColor: AppColors.success,
            ),
          );
        }
      }
    } catch (e) {
      // Error
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  double _scale = 1.0;

  void _onTapDown(TapDownDetails details) {
    setState(() => _scale = 0.98);
  }

  void _onTapCancel() {
    setState(() => _scale = 1.0);
  }

  void _toggleFlip([String type = 'score']) {
    if (!_isFlipped) {
      setState(() {
        _isFlipped = true;
        _flipType = type;
      });
    } else {
      setState(() => _isFlipped = false);
    }
  }

  bool get _canShareRank => widget.rank >= 1 && widget.rank <= 13;

  Future<void> _shareRankingCard() async {
    if (!_canShareRank || _isSharing) return;

    final messenger = ScaffoldMessenger.of(context);

    try {
      await _ensureShareMeaningLoaded();
      final imageBytes = await _captureSharePoster();
      if (!mounted) return;
      await _showSharePreview(imageBytes);
    } catch (error, stackTrace) {
      debugPrint('Share ranking failed: $error');
      debugPrintStack(stackTrace: stackTrace);
      await Clipboard.setData(ClipboardData(text: _buildShareCaption()));
      if (!mounted) return;
      messenger.showSnackBar(
        const SnackBar(
          content: Text('แชร์รูปภาพไม่สำเร็จ ระบบคัดลอกข้อความไว้ให้แล้ว'),
          backgroundColor: Colors.deepOrange,
        ),
      );
    }
  }

  Future<void> _ensureShareMeaningLoaded() async {
    final futures = <Future<void>>[];

    if (_shareSatMeaning == null) {
      futures.add(
        ApiService().getNumberMeaning(zeroPad(widget.result.satSum)).then((
          value,
        ) {
          _shareSatMeaning = value;
        }),
      );
    }

    if (_shareShaMeaning == null) {
      futures.add(
        ApiService().getNumberMeaning(zeroPad(widget.result.shaSum)).then((
          value,
        ) {
          _shareShaMeaning = value;
        }),
      );
    }

    if (futures.isNotEmpty) {
      await Future.wait(futures);
    }
  }

  Future<void> _showSharePreview(Uint8List imageBytes) async {
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
                        'ภาพนี้คือ card อันดับจริงที่จะถูกแชร์ไปยัง social',
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
                                await _sharePreviewImage(imageBytes);
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
                                  ClipboardData(text: _buildShareCaption()),
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

  Future<void> _sharePreviewImage(Uint8List imageBytes) async {
    final messenger = ScaffoldMessenger.of(context);
    final shareBox = context.findRenderObject() as RenderBox?;
    setState(() => _isSharing = true);

    try {
      final fileName = _sharePosterFileName;
      final tempDir = await getTemporaryDirectory();
      final file = File('${tempDir.path}/$fileName');
      await file.writeAsBytes(imageBytes, flush: true);

      await Share.shareXFiles(
        [XFile(file.path, mimeType: 'image/png')],
        subject: 'อันดับชื่อมงคล #${widget.rank} ${widget.result.name}',
        sharePositionOrigin: shareBox == null
            ? null
            : shareBox.localToGlobal(Offset.zero) & shareBox.size,
      );
    } catch (error, stackTrace) {
      debugPrint('Share preview image failed: $error');
      debugPrintStack(stackTrace: stackTrace);
      await Clipboard.setData(ClipboardData(text: _buildShareCaption()));
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

  String get _sharePosterFileName {
    final safeName = widget.result.name
        .replaceAll(RegExp(r'[^\wก-๙]+'), '_')
        .replaceAll(RegExp(r'_+'), '_');
    return 'ranking_${widget.rank}_$safeName.png';
  }

  Future<Uint8List> _captureSharePoster() async {
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
                child: RepaintBoundary(
                  key: _sharePosterKey,
                  child: _buildSharePoster(),
                ),
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

  String _buildShareCaption() {
    return 'อันดับ #${widget.rank} "${widget.result.name}"\n'
        '${widget.result.meaning}\n'
        'เลขศาสตร์ ${widget.result.satSum} คือสัญลักษณ์ที่สะท้อนพลังตัวเลขของชื่อ\n'
        'พลังเงา ${widget.result.shaSum} คือสัญลักษณ์ที่บอกแรงดึงดูดและอิทธิพลของชื่อ\n'
        'เปลี่ยนชีวิตด้วยแรงดึงดูด --ชื่อดี.com';
  }

  Widget _buildSharePoster() {
    final bool hasMatching =
        widget.showMatching && (widget.result.totalSat != widget.result.satSum);

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
                color: AppColors.primary.withValues(alpha: 0.12),
              ),
            ),
            child: Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    gradient: AppColors.primaryGradient,
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
                          color: AppColors.textLight,
                          fontSize: 20,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      Text(
                        'เปลี่ยนชีวิตด้วยแรงดึงดูดจากชื่อดี',
                        style: GoogleFonts.sarabun(
                          color: AppColors.textGray,
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
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(26),
              boxShadow: [
                BoxShadow(
                  color: AppColors.primary.withValues(alpha: 0.10),
                  blurRadius: 18,
                  offset: const Offset(0, 8),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(22),
              child: _buildFrontCardContent(
                hasMatching: hasMatching,
                showShareAction: false,
                margin: EdgeInsets.zero,
                isSharePreview: true,
                satMeaning: _shareSatMeaning,
                shaMeaning: _shareShaMeaning,
              ),
            ),
          ),
        ],
      ),
    );
  }

  _LuckyBreakdown _computeLuckyBreakdown() {
    final bool isSatMatch = widget.result.isSatGood;
    final bool isShaMatch = widget.result.isShaGood;
    final bool noKaki =
        widget.result.kakiHighlight.isNotEmpty &&
        !widget.result.kakiHighlight.any((h) => h.isKaki);
    final bool hasMatchingGood =
        widget.showMatching &&
        (widget.result.totalSat != widget.result.satSum) &&
        widget.result.isTotalSatGood &&
        widget.result.isTotalShaGood;

    final bool satSelected = widget.isFilterSatActive;
    final bool shaSelected = widget.isFilterShaActive;

    final bool satisfiesFilters =
        (!satSelected || isSatMatch) &&
        (!shaSelected || isShaMatch) &&
        (!widget.isFilterKakiActive || noKaki);

    final bool passesSelectedCriteria = satSelected && shaSelected
        ? (isSatMatch && isShaMatch)
        : satSelected
        ? isSatMatch
        : shaSelected
        ? isShaMatch
        : (isSatMatch && isShaMatch);

    final bool hasAnyRedSignal = !isSatMatch || !isShaMatch;
    final bool isLucky =
        satisfiesFilters && passesSelectedCriteria && !hasAnyRedSignal;

    int multiplier = 0;
    if (satSelected && isSatMatch) multiplier++;
    if (shaSelected && isShaMatch) multiplier++;
    if (!satSelected && !shaSelected) {
      if (isSatMatch) multiplier++;
      if (isShaMatch) multiplier++;
    }
    if (widget.result.kakiHighlight.isNotEmpty && noKaki) multiplier++;
    if (hasMatchingGood) multiplier++;

    return _LuckyBreakdown(
      isSatMatch: isSatMatch,
      isShaMatch: isShaMatch,
      noKaki: noKaki,
      hasMatchingGood: hasMatchingGood,
      isLucky: isLucky,
      multiplier: multiplier,
      includesKakiBonus: widget.result.kakiHighlight.isNotEmpty && noKaki,
    );
  }

  @override
  Widget build(BuildContext context) {
    // Sync local state with cache to handle delayed loading or deletions
    if (ApiService.savedNamesCache.contains(widget.result.name)) {
      _isSaved = true;
    } else {
      _isSaved = false;
    }

    bool hasMatching =
        widget.showMatching && (widget.result.totalSat != widget.result.satSum);

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 350),
      transitionBuilder: (child, animation) {
        return FadeTransition(
          opacity: animation,
          child: ScaleTransition(
            scale: Tween<double>(begin: 0.92, end: 1.0).animate(
              CurvedAnimation(parent: animation, curve: Curves.easeOut),
            ),
            child: child,
          ),
        );
      },
      child: _isFlipped ? _buildBackSide() : _buildFrontSide(hasMatching),
    );
  }

  Widget _buildFrontSide(bool hasMatching) {
    return GestureDetector(
      onTapDown: _onTapDown,
      onTapUp: (_) => _onTapCancel(),
      onTapCancel: _onTapCancel,
      child: AnimatedScale(
        scale: _scale,
        duration: const Duration(milliseconds: 100),
        curve: Curves.easeOut,
        child: _buildFrontCardContent(
          hasMatching: hasMatching,
          showShareAction: true,
          margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
        ),
      ),
    );
  }

  Widget _buildFrontCardContent({
    required bool hasMatching,
    required bool showShareAction,
    required EdgeInsets margin,
    bool isSharePreview = false,
    NumberMeaningResult? satMeaning,
    NumberMeaningResult? shaMeaning,
  }) {
    final bool isEvenRow = widget.rank > 0 ? widget.rank.isEven : false;
    final Color rowBackground = isEvenRow
        ? const Color(0xFFFFFCF4)
        : const Color(0xFFFFFFFF);
    const bool showSatScore = true;
    const bool showShaScore = true;

    return ClipRRect(
      borderRadius: BorderRadius.circular(18),
      child: Container(
        margin: margin,
        decoration: BoxDecoration(
          color: rowBackground,
          borderRadius: BorderRadius.circular(18),
          border: Border(
            bottom: BorderSide(
              color: AppColors.textGray.withValues(alpha: 0.12),
              width: 0.8,
            ),
          ),
          boxShadow: [
            BoxShadow(
              color: AppColors.primary.withValues(alpha: isEvenRow ? 0.06 : 0.03),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Stack(
          children: [
            // Thai Kanok pattern overlay
            Positioned.fill(
              child: CustomPaint(
                painter: _ThaiKanokPatternPainter(
                  color: widget.rank <= 3
                      ? AppColors.accent.withValues(alpha: 0.06)
                      : AppColors.primary.withValues(alpha: 0.04),
                  rank: widget.rank,
                ),
              ),
            ),
            Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(8, 12, 8, 0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                if (widget.rank > 0)
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      _buildRankBadge(
                        widget.rank,
                        showShareAction: showShareAction,
                        isSharePreview: isSharePreview,
                      ),
                      if (!isSharePreview) ...[
                        const SizedBox(width: 10),
                        _buildBookmarkButton(compact: true),
                      ],
                    ],
                  ),
                const SizedBox(width: 8),
                Flexible(
                  child: Align(
                    alignment: Alignment.centerRight,
                    child: FittedBox(
                      fit: BoxFit.scaleDown,
                      alignment: Alignment.centerRight,
                      child: Builder(
                        builder: (context) {
                          final lucky = _computeLuckyBreakdown();
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

                          return _MagicLuckyBadge(
                            isLucky: lucky.isLucky,
                            text: luckText,
                            gradientColors: gradientColors,
                            onTap: showShareAction
                                ? () => _toggleFlip('luck')
                                : () {},
                          );
                        },
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(8, 8, 8, 16),
            child: Column(
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: [
                              Expanded(
                                child: FittedBox(
                                  fit: BoxFit.scaleDown,
                                  alignment: Alignment.centerLeft,
                                  child: GestureDetector(
                                    behavior: HitTestBehavior.opaque,
                                    onTap: () {
                                      if (showShareAction &&
                                          widget.onTap != null) {
                                        widget.onTap!();
                                      }
                                    },
                                    child: _buildNameText(context),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              _buildSectionSpeakButton(
                                compact: true,
                                isSpeaking: _isSpeaking(
                                  _nameMeaningSpeakingKey(),
                                ),
                                onTap: _speakNameAndMeaning,
                                tooltip: _getSpeakTooltip(),
                                icon: _isSpeaking(_nameMeaningSpeakingKey())
                                    ? Icons.volume_up_rounded
                                    : Icons.mic_rounded,
                              ),
                            ],
                          ),
                          const SizedBox(height: 2),
                          Wrap(
                            spacing: 6,
                            runSpacing: 4,
                            children: [
                              if (widget.result.kakiHighlight.isNotEmpty &&
                                  !widget.result.kakiHighlight.any(
                                    (h) => h.isKaki,
                                  ))
                                _buildNoKakiBadge(),
                              if (widget.result.kakiHighlight.any(
                                (h) => h.isKaki,
                              ))
                                _buildKakiWarningBadge(),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: Text(
                                  widget.result.meaning,
                                  style: const TextStyle(
                                    color: AppColors.textGray,
                                    fontSize: 15,
                                    height: 1.5,
                                    fontFamily: 'Sarabun',
                                  ),
                                ),
                              ),
                            ],
                          ),
                          if (isSharePreview &&
                              ((satMeaning?.description.isNotEmpty ?? false) ||
                                  (shaMeaning?.description.isNotEmpty ??
                                      false))) ...[
                            const SizedBox(height: 12),
                            _buildShareMiracleDetails(
                              satMeaning: satMeaning,
                              shaMeaning: shaMeaning,
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        if (showSatScore)
                          _buildSmartScoreDisplay(
                            widget.result.satSum,
                            widget.result.isSatGood,
                            showShareAction ? "" : "เลขศาสตร์",
                            description: showShareAction ? "" : "",
                            isActive: widget.isFilterSatActive,
                            labelOnLeft: showShareAction,
                            size: isSharePreview
                                ? 38
                                : (showShareAction ? 29.1 : 44),
                            pairType: widget.result.satPairType,
                          ),
                        if (showSatScore && showShaScore)
                          SizedBox(height: isSharePreview ? 10 : 12),
                        if (showShaScore)
                          _buildSmartScoreDisplay(
                            widget.result.shaSum,
                            widget.result.isShaGood,
                            showShareAction ? "" : "พลังเงา",
                            description: showShareAction ? "" : "",
                            isActive: widget.isFilterShaActive,
                            labelOnLeft: showShareAction,
                            size: isSharePreview
                                ? 38
                                : (showShareAction ? 29.1 : 44),
                            pairType: widget.result.shaPairType,
                          ),
                      ],
                    ),
                  ],
                ),
                if (_shouldShowPhoneticInsight()) ...[
                  const SizedBox(height: 12),
                  _buildPhoneticInsightCard(),
                ],
                if (showShareAction) ...[
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Flexible(
                        child: Align(
                          alignment: Alignment.centerLeft,
                          child: FittedBox(
                            fit: BoxFit.scaleDown,
                            child: _buildRootWordButton(context, compact: true),
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Flexible(
                        child: Align(
                          alignment: Alignment.centerRight,
                          child: _buildShareCornerButton(compact: true),
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
          if (hasMatching)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(16, 8, 0, 10),
              decoration: BoxDecoration(
                color: AppColors.secondary.withValues(alpha: 0.05),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.subdirectory_arrow_right_rounded,
                    color: Color(0xFF8B6B04),
                    size: 24,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [_buildMatchingNameText(context)],
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      if (showSatScore)
                        _buildSmartScoreDisplay(
                          widget.result.totalSat,
                          widget.result.isTotalSatGood,
                          "",
                          isActive: widget.isFilterSatActive,
                          size: 34,
                          labelOnLeft: true,
                          pairType: widget.result.totalSatPairType,
                        ),
                      if (showSatScore && showShaScore)
                        const SizedBox(height: 4),
                      if (showShaScore)
                        _buildSmartScoreDisplay(
                          widget.result.totalSha,
                          widget.result.isTotalShaGood,
                          "",
                          isActive: widget.isFilterShaActive,
                          size: 34,
                          labelOnLeft: true,
                          pairType: widget.result.totalShaPairType,
                        ),
                    ],
                  ),
                ],
              ),
            ),
        ],
      ),
          ],  // Stack children
        ),   // Stack
      ),     // Container
    );       // ClipRRect
  }

  Widget _buildBackSide() {
    return GestureDetector(
      key: const ValueKey('back_side'),
      onTap: () => _toggleFlip(),
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 6, horizontal: 4),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: _flipType == 'luck'
                ? [const Color(0xFF2A1F0A), const Color(0xFF3D2E0F)]
                : [const Color(0xFF0A1628), const Color(0xFF0D1F3C)],
          ),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: _flipType == 'luck'
                ? const Color(0xFFDBB632).withValues(alpha: 0.4)
                : AppColors.accent.withValues(alpha: 0.3),
            width: 1.5,
          ),
          boxShadow: [
            BoxShadow(
              color:
                  (_flipType == 'luck'
                          ? const Color(0xFFDBB632)
                          : AppColors.accent)
                      .withValues(alpha: 0.15),
              blurRadius: 20,
              spreadRadius: 2,
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Builder(
                      builder: (context) {
                        final lucky = _computeLuckyBreakdown();

                        String title;
                        IconData icon;
                        Color color;

                        if (_flipType == 'luck') {
                          if (lucky.isLucky) {
                            title = 'ที่มาของโชค';
                            icon = Icons.auto_awesome;
                            color = const Color(0xFFDBB632);
                          } else {
                            title = 'ทำไมถึงน่าเสียดาย?';
                            icon = Icons.info_outline_rounded;
                            color = const Color(0xFF9CA3AF); // Gray
                          }
                        } else {
                          title = 'ที่มาของอันดับ';
                          icon = Icons.leaderboard_rounded;
                          color = AppColors.accent;
                        }

                        return Row(
                          children: [
                            Icon(icon, color: color, size: 18),
                            const SizedBox(width: 8),
                            Text(
                              title,
                              style: GoogleFonts.prompt(
                                color: color,
                                fontSize: 14,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                  ],
                ),
                GestureDetector(
                  onTap: () => _toggleFlip(),
                  child: const Icon(
                    Icons.close_rounded,
                    color: Colors.white38,
                    size: 18,
                  ),
                ),
              ],
            ),
            const Divider(color: Colors.white12, height: 16),

            // Content based on flip type
            if (_flipType == 'luck')
              _buildLuckExplanation()
            else
              _buildScoreBreakdownContent(),

            const SizedBox(height: 12),
            Center(
              child: Text(
                'แตะเพื่อปิด',
                style: GoogleFonts.sarabun(color: Colors.white24, fontSize: 10),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLuckExplanation() {
    final lucky = _computeLuckyBreakdown();

    final label = lucky.multiplier >= 4
        ? 'Super'
        : lucky.multiplier == 3
        ? 'Triple'
        : 'Double';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          lucky.isLucky
              ? '$label Lucky x${lucky.multiplier} ได้มาจาก:'
              : 'วิเคราะห์ข้อบกพร่องของชื่อนี้:',
          style: GoogleFonts.sarabun(
            color: lucky.isLucky ? Colors.white70 : const Color(0xFFFCA5A5),
            fontSize: 12,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 10),
        _buildCriteriaRow(
          Icons.looks_one_rounded,
          'เลขศาสตร์ (ตัวเลขมงคล)',
          lucky.isSatMatch,
          lucky.isSatMatch
              ? 'เลขรวม ${widget.result.satSum}${widget.result.satPairType.isNotEmpty ? " (${widget.result.satPairType}/${widget.result.satPairPoint})" : ""} — ดีมาก ✓'
              : 'เลขรวม ${widget.result.satSum} — ไม่ผ่าน',
        ),
        const SizedBox(height: 8),
        _buildCriteriaRow(
          Icons.blur_on_rounded,
          'พลังเงา (เลขเงา)',
          lucky.isShaMatch,
          lucky.isShaMatch
              ? 'เลขเงา ${widget.result.shaSum}${widget.result.shaPairType.isNotEmpty ? " (${widget.result.shaPairType}/${widget.result.shaPairPoint})" : ""} — ดีมาก ✓'
              : 'เลขเงา ${widget.result.shaSum} — ไม่ผ่าน',
        ),
        if (lucky.includesKakiBonus || widget.isFilterKakiActive) ...[
          const SizedBox(height: 8),
          _buildCriteriaRow(
            Icons.shield_rounded,
            'ปลอดกาลกิณี',
            lucky.noKaki,
            lucky.noKaki ? 'ไม่มีอักษรกาลกิณี ✓' : 'มีอักษรกาลกิณี',
          ),
        ],
        if (widget.showMatching) ...[
          const SizedBox(height: 8),
          _buildCriteriaRow(
            Icons.people_rounded,
            'เลขศาสตร์รวมกับนามสกุล',
            lucky.hasMatchingGood,
            lucky.hasMatchingGood
                ? 'รวม ${widget.result.totalSat}/${widget.result.totalSha} — ดีทั้งคู่ ✓'
                : 'รวมแล้วยังไม่สมบูรณ์',
          ),
        ],
      ],
    );
  }

  Widget _buildCriteriaRow(
    IconData icon,
    String label,
    bool passed,
    String detail,
  ) {
    final color = passed
        ? const Color(0xFF4ADE80)
        : const Color(0xFFF87171); // Changed non-passed to red
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          passed ? Icons.check_circle_rounded : Icons.cancel_rounded,
          color: color,
          size: 16,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: GoogleFonts.sarabun(
                  color: color,
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                ),
              ),
              Text(
                detail,
                style: GoogleFonts.sarabun(color: Colors.white38, fontSize: 11),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildScoreBreakdownContent() {
    final similarity = (100 * (1 - widget.result.distance))
        .clamp(0.0, 100.0)
        .toDouble();
    final score = widget.result.calculateScore(
      showMatching: widget.showMatching,
    );
    final reasons = widget.result.rankReasons;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'คะแนนอันดับ #${widget.rank} ($score คะแนน) จาก:',
          style: GoogleFonts.sarabun(
            color: Colors.white70,
            fontSize: 12,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 10),
        _buildScoreRow(
          'ความคล้าย',
          similarity.toStringAsFixed(0),
          const Color(0xFF60A5FA),
        ),
        if (reasons.isNotEmpty) ...[
          const SizedBox(height: 8),
          ...reasons
              .take(5)
              .map(
                (reason) => Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Text(
                    '• $reason',
                    style: GoogleFonts.sarabun(
                      color: Colors.white70,
                      fontSize: 12,
                    ),
                  ),
                ),
              ),
        ],
        const Divider(color: Colors.white12, height: 14),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'คะแนนรวม',
              style: GoogleFonts.sarabun(
                color: Colors.white,
                fontSize: 13,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              '$score คะแนน',
              style: GoogleFonts.prompt(
                color: AppColors.accent,
                fontSize: 16,
                fontWeight: FontWeight.w900,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildScoreRow(String label, String value, Color color) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: GoogleFonts.sarabun(color: Colors.white54, fontSize: 12),
          ),
          Text(
            value,
            style: GoogleFonts.prompt(
              color: color,
              fontSize: 12,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  bool _shouldShowPhoneticInsight() {
    return widget.result.phoneticSummary.trim().isNotEmpty ||
        widget.result.phoneticScore != null;
  }

  String _buildPhoneticInsightText() {
    if (widget.result.phoneticSummary.trim().isNotEmpty) {
      return widget.result.phoneticSummary.trim();
    }

    final score = widget.result.phoneticScore ?? 0;
    final ease = widget.result.pronunciationEase ?? score;
    final euphony = widget.result.euphonyScore ?? score;
    final rhythm = widget.result.rhythmScore ?? score;

    if (score >= 94 && euphony >= 92 && rhythm >= 90) {
      return "โทนเสียงละมุน นุ่มลึก และจังหวะลงตัว ฟังแล้วติดหูมาก";
    }
    if (ease >= 92 && euphony >= 88) {
      return "ออกเสียงลื่น ปากเปิดง่าย และน้ำเสียงฟังนุ่มละมุน";
    }
    if (rhythm >= 90 && score >= 88) {
      return "น้ำหนักเสียงแน่น จังหวะดี เรียกแล้วฟังชัดและมีพลัง";
    }
    if (euphony >= 88) {
      return "เสียงค่อนข้างหวาน ละมุนหู และฟังราบรื่นต่อเนื่อง";
    }
    if (ease >= 86) {
      return "ออกเสียงง่าย ฟังลื่น และเรียกใช้ได้สบายในชีวิตประจำวัน";
    }
    if (rhythm >= 84) {
      return "จังหวะเสียงดี โทนค่อนข้างแน่น เรียกแล้วจำง่าย";
    }
    return "โทนเสียงค่อนข้างเรียบลื่น ฟังง่าย และใช้งานได้ดี";
  }

  Widget _buildPhoneticInsightCard() {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          decoration: BoxDecoration(
            color: const Color(0xFFEFF9F8),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFBDE8E3), width: 1.2),
          ),
          child: Padding(
            padding: const EdgeInsets.only(right: 54),
            child: Text(
              _buildPhoneticInsightText(),
              style: GoogleFonts.sarabun(
                color: const Color(0xFF245A57),
                fontSize: 14,
                height: 1.35,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
        Positioned(
          top: 0,
          bottom: 0,
          right: 10,
          child: Align(
            alignment: Alignment.centerRight,
            child: _buildPhoneticSpeakButton(),
          ),
        ),
      ],
    );
  }

  Widget _buildPhoneticSpeakButton() {
    final isSpeaking = _isSpeaking(_phoneticSpeakingKey());
    return Tooltip(
      message: _getSpeakTooltip(),
      child: GestureDetector(
        onTap: _speakPhoneticOnly,
        child: Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFFEFF6FF), Color(0xFFBFDBFE)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            shape: BoxShape.circle,
            border: Border.all(
              color: isSpeaking
                  ? const Color(0xFF16A34A)
                  : const Color(0xFF93C5FD),
              width: 1.2,
            ),
            boxShadow: [
              BoxShadow(
                color:
                    (isSpeaking
                            ? const Color(0xFF22C55E)
                            : const Color(0xFF60A5FA))
                        .withValues(alpha: 0.15),
                blurRadius: 6,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Icon(
            Icons.record_voice_over_rounded,
            color: isSpeaking ? Colors.white : const Color(0xFF1D4ED8),
            size: 16,
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

  Widget _buildRankBadge(
    int rank, {
    bool showShareAction = true,
    bool isSharePreview = false,
  }) {
    final bool useTotal =
        widget.showMatching && (widget.result.totalSat != widget.result.satSum);
    final score = widget.result.calculateScore(showMatching: useTotal);

    // Top 3 get special colors
    Color textColor;
    IconData? icon;

    if (rank == 1) {
      textColor = const Color(0xFFB8860B); // Gold
      icon = Icons.emoji_events;
    } else if (rank == 2) {
      textColor = const Color(0xFF64748B); // Silver
      icon = Icons.emoji_events;
    } else if (rank == 3) {
      textColor = const Color(0xFFCD7F32); // Bronze
      icon = Icons.emoji_events;
    } else {
      textColor = const Color(0xFF7C3AED); // Modern Purple
      icon = null;
    }

    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: const Duration(milliseconds: 500),
      curve: Curves.elasticOut,
      builder: (context, value, child) {
        return Transform.scale(
          scale: value,
          alignment: Alignment.topLeft,
          child: GestureDetector(
            onLongPress: () => _showScoreBreakdown(context, rank, score),
            onTap: () => _toggleFlip('score'),
            child: Container(
              padding: EdgeInsets.fromLTRB(
                0,
                isSharePreview ? 12 : 12,
                isSharePreview ? 16 : 16,
                0,
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (icon != null && rank <= 3) ...[
                    Icon(
                      icon,
                      size: isSharePreview ? 18 : 16,
                      color: textColor,
                    ),
                    SizedBox(width: isSharePreview ? 7 : 6),
                  ],
                  //ANCHOR: RanKingStartNO1 (อันดับ)
                  Text(
                    'อันดับ #$rank | ${(widget.result.finalRankScoreExact > 0 ? widget.result.finalRankScoreExact : (widget.result.finalRankScore > 0 ? widget.result.finalRankScore.toDouble() : score.toDouble())).toStringAsFixed(2)} คะแนน',
                    style: TextStyle(
                      color: textColor,
                      fontSize: isSharePreview ? 13 : 11.5,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 0.5,
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildShareCornerButton({bool compact = false}) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: _shareRankingCard,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: EdgeInsets.symmetric(
          horizontal: compact ? 10 : 12,
          vertical: compact ? 7 : 8,
        ),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFFBAE6FD), Color(0xFF7DD3FC)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(15),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF7DD3FC).withValues(alpha: 0.22),
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
              size: compact ? 14 : 15,
              color: const Color(
                0xFF0F4C81,
              ).withValues(alpha: _isSharing ? 0.75 : 1),
            ),
            SizedBox(width: compact ? 5 : 6),
            Text(
              'แชร์',
              style: GoogleFonts.sarabun(
                color: const Color(
                  0xFF0F4C81,
                ).withValues(alpha: _isSharing ? 0.75 : 1),
                fontSize: compact ? 12 : 13,
                fontWeight: FontWeight.w800,
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showScoreBreakdown(BuildContext context, int rank, int totalScore) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: const Color(0xFF1E293B),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          title: Column(
            children: [
              if (rank <= 3)
                Icon(
                  Icons.emoji_events,
                  size: 40,
                  color: rank == 1
                      ? const Color(0xFFFFD700)
                      : rank == 2
                      ? const Color(0xFFC0C0C0)
                      : const Color(0xFFCD7F32),
                ),
              const SizedBox(height: 8),
              Text(
                widget.result.name,
                style: GoogleFonts.sarabun(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 24,
                ),
              ),
              Text(
                'อันดับ #$rank • $totalScore คะแนน',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.6),
                  fontSize: 14,
                ),
              ),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Divider(color: Colors.white12),
              const SizedBox(height: 8),

              const SizedBox(height: 16),
              Text(
                'คะแนนรวมคำนวณจากความหมาย, เลขศาสตร์, พลังเงา และความยาวของชื่อ เพื่อให้ได้ชื่อที่ดีที่สุดสำหรับคุณ',
                style: GoogleFonts.sarabun(
                  color: Colors.white.withValues(alpha: 0.6),
                  fontSize: 13,
                  height: 1.5,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              const Divider(color: Colors.white12),
              const SizedBox(height: 8),
              Text(
                'ยิ่งคะแนนสูง ยิ่งเหมาะสมกับความต้องการ',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.4),
                  fontSize: 11,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text(
                'ปิด',
                style: TextStyle(color: AppColors.primary),
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _scoreRow(String label, String value, bool isPositive) {
    return Row(
      children: [
        Expanded(
          child: Text(
            label,
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.8),
              fontSize: 13,
            ),
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
          decoration: BoxDecoration(
            color: isPositive
                ? const Color(0xFF10B981).withValues(alpha: 0.2)
                : Colors.white.withValues(alpha: 0.05),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: isPositive
                  ? const Color(0xFF10B981).withValues(alpha: 0.4)
                  : Colors.white.withValues(alpha: 0.1),
            ),
          ),
          child: Text(
            value,
            style: TextStyle(
              color: isPositive
                  ? const Color(0xFF34D399)
                  : Colors.white.withValues(alpha: 0.4),
              fontSize: 13,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSmartScoreDisplay(
    dynamic score,
    bool isGood,
    String label, {
    String description = '',
    bool isActive = true,
    double size = 44,
    bool labelOnLeft = false,
    String pairType = '',
  }) {
    int sVal = 0;
    if (score is int) {
      sVal = score;
    } else if (score is String) {
      sVal = int.tryParse(score) ?? 0;
    }

    final circle = (sVal >= 100)
        ? _buildTripleDigitScores(
            sVal,
            isGood,
            size,
            isActive: isActive,
            renderNeutral: false,
            pairType: pairType,
          )
        : _buildScoreCircle(
            sVal,
            isGood,
            "",
            size: size,
            isActive: isActive,
            renderNeutral: false,
            pairType: pairType,
          );

    if (labelOnLeft && label.isNotEmpty) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label,
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.7),
              fontSize: size * 11 / 44,
              fontWeight: FontWeight.w600,
              fontFamily: 'Prompt',
            ),
          ),
          const SizedBox(width: 8),
          circle,
        ],
      );
    }

    if (label.isNotEmpty) {
      return Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          circle,
          const SizedBox(height: 6),
          Text(
            label,
            style: TextStyle(
              color: AppColors.textGray,
              fontSize: 10,
              fontWeight: FontWeight.w500,
            ),
          ),
          if (description.isNotEmpty) ...[
            const SizedBox(height: 2),
            SizedBox(
              width: size + 18,
              child: Text(
                description,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: AppColors.textGray.withValues(alpha: 0.8),
                  fontSize: 8,
                  height: 1.2,
                  fontWeight: FontWeight.w400,
                ),
              ),
            ),
          ],
        ],
      );
    }

    return circle;
  }

  Widget _buildShareMiracleDetails({
    NumberMeaningResult? satMeaning,
    NumberMeaningResult? shaMeaning,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (satMeaning?.description.isNotEmpty ?? false)
          Expanded(
            child: _buildShareMiracleChip(
              title: 'เลขศาสตร์ ${widget.result.satSum}',
              detail: satMeaning!.description,
              accent: const Color(0xFF16A34A),
            ),
          ),
        if ((satMeaning?.description.isNotEmpty ?? false) &&
            (shaMeaning?.description.isNotEmpty ?? false))
          const SizedBox(width: 8),
        if (shaMeaning?.description.isNotEmpty ?? false)
          Expanded(
            child: _buildShareMiracleChip(
              title: 'พลังเงา ${widget.result.shaSum}',
              detail: shaMeaning!.description,
              accent: const Color(0xFF0EA5E9),
            ),
          ),
      ],
    );
  }

  Widget _buildShareMiracleChip({
    required String title,
    required String detail,
    required Color accent,
  }) {
    return Container(
      padding: const EdgeInsets.fromLTRB(10, 8, 10, 9),
      decoration: BoxDecoration(
        color: accent.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: accent.withValues(alpha: 0.14)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: GoogleFonts.prompt(
              color: accent,
              fontSize: 10,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            detail.replaceAll("\\n", " "),
            maxLines: 4,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.sarabun(
              color: AppColors.textGray,
              fontSize: 10.5,
              height: 1.25,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTripleDigitScores(
    int score,
    bool isGood,
    double size, {
    bool isActive = true,
    bool renderNeutral = false,
    String pairType = '',
  }) {
    final pairs = toPairList(score);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (int i = 0; i < pairs.length; i++) ...[
          if (i > 0) const SizedBox(width: 4),
          _buildScoreCircle(
            pairs[i],
            isGood,
            "",
            size: size,
            isActive: isActive,
            renderNeutral: renderNeutral,
            pairType: pairType,
          ),
        ],
      ],
    );
  }

  Widget _buildScoreCircle(
    dynamic score,
    bool isGood,
    String label, {
    bool isActive = true,
    bool renderNeutral = false,
    double size = 44,
    String pairType = '',
  }) {
    Color lightColor;
    Color darkColor;

    if (renderNeutral) {
      lightColor = const Color(0xFFF8FAFC);
      darkColor = const Color(0xFFE5E7EB);
    } else if (pairType.isNotEmpty) {
      Color base = _pairTypeColor(pairType);
      String p = pairType.toUpperCase();

      if (p.startsWith('D')) {
        if (p.contains('10')) {
          // Vibrant Emerald Green (D10)
          lightColor = const Color(0xFF10B981);
          darkColor = const Color(0xFF059669);
        } else if (p.contains('8')) {
          // Medium Good Green (D8)
          lightColor = const Color(0xFF34D399);
          darkColor = const Color(0xFF10B981);
        } else {
          // Light Green
          lightColor = const Color(0xFFBBF7D0);
          darkColor = const Color(0xFF4ADE80);
        }
      } else if (p.startsWith('R')) {
        if (p.contains('10')) {
          // Very Dark Red
          lightColor = const Color(0xFF991B1B);
          darkColor = const Color(0xFF7F1D1D);
        } else if (p.contains('7')) {
          // Mid Red
          lightColor = const Color(0xFFEF4444);
          darkColor = const Color(0xFFB91C1C);
        } else {
          // Light Red
          lightColor = const Color(0xFFFECACA);
          darkColor = const Color(0xFFF87171);
        }
      } else {
        lightColor = base.withValues(alpha: 0.8);
        darkColor = base;
      }
    } else {
      lightColor = isGood ? const Color(0xFF4ADE80) : const Color(0xFFF87171);
      darkColor = isGood ? const Color(0xFF16A34A) : const Color(0xFFDC2626);
    }

    return Builder(
      builder: (context) => InkWell(
        onTap: () {
          final displayScore = score is int ? zeroPad(score) : score.toString();
          _showNumberMeaningDialog(context, displayScore, isGood);
        },
        borderRadius: BorderRadius.circular(50),
        child: Column(
          children: [
            Container(
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
                "$score",
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
            if (label.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(
                label,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 10,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildRootWordButton(BuildContext context, {bool compact = false}) {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppColors.secondary, AppColors.accent],
        ),
        borderRadius: BorderRadius.circular(compact ? 16 : 20),
        boxShadow: [
          BoxShadow(
            color: AppColors.secondary.withValues(alpha: 0.3),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
        border: Border.all(
          color: Colors.white.withValues(alpha: 0.5),
          width: 1.5,
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            _showRootWordDialog(context);
          },
          borderRadius: BorderRadius.circular(compact ? 16 : 20),
          child: Padding(
            padding: EdgeInsets.symmetric(
              horizontal: compact ? 10 : 14,
              vertical: compact ? 7 : 8,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.auto_stories_rounded,
                  size: compact ? 14 : 16,
                  color: Colors.white,
                ),
                SizedBox(width: compact ? 6 : 8),
                // ANCHOR: WordOriginItem (รากศัพท์รายการ)
                Text(
                  "รากศัพท์",
                  style: GoogleFonts.prompt(
                    color: Colors.white,
                    fontSize: compact ? 12 : 13,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 0.5,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBookmarkButton({bool compact = false}) {
    final Color savedColor = const Color(
      0xFFD946EF,
    ); // Purple-Pink (ม่วงอมชมพู)

    return GestureDetector(
      onTap: _isSaved ? null : _saveName,
      child: Container(
        padding: compact ? const EdgeInsets.all(6) : const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: _isSaved
              ? savedColor.withValues(alpha: 0.08)
              : Colors.black.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(compact ? 10 : 12),
          border: Border.all(
            color: _isSaved
                ? savedColor.withValues(alpha: 0.3)
                : Colors.black.withValues(alpha: 0.1),
          ),
        ),
        child: _isSaving
            ? const SizedBox(
                width: 14,
                height: 14,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppColors.primary,
                ),
              )
            : Icon(
                _isSaved ? Icons.favorite : Icons.favorite_border,
                size: compact ? 18 : 20,
                color: _isSaved
                    ? savedColor
                    : Colors.black.withValues(alpha: 0.3),
              ),
      ),
    );
  }

  Widget _buildVoiceButton() {
    return const SizedBox.shrink();
  }

  Widget _buildNoKakiBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(
          0xFF10B981,
        ).withValues(alpha: 0.15), // Emerald-500 light bg
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
              color: const Color(0xFF34D399), // Emerald-400
              fontSize: 10,
              fontWeight: FontWeight.bold,
              height: 1.2,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNumerologyBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFF3B82F6).withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFF3B82F6).withValues(alpha: 0.4),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.calculate, color: Color(0xFF60A5FA), size: 12),
          const SizedBox(width: 4),
          Text(
            "เลขศาสตร์ดี",
            style: GoogleFonts.prompt(
              color: const Color(0xFF60A5FA),
              fontSize: 10,
              fontWeight: FontWeight.bold,
              height: 1.2,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildShadowBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFF8B5CF6).withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFF8B5CF6).withValues(alpha: 0.4),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.dark_mode, color: Color(0xFFA78BFA), size: 12),
          const SizedBox(width: 4),
          Text(
            "พลังเงาดี",
            style: GoogleFonts.prompt(
              color: const Color(0xFFA78BFA),
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
              color: const Color(0xFF94A3B8), // Muted Slate Gray (Avoid Red)
              fontSize: 10,
              fontWeight: FontWeight.bold,
              height: 1.2,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMatchingNameText(BuildContext context) {
    final bool isGold =
        widget.result.isTotalSatGood && widget.result.isTotalShaGood;
    final bool hasKaki =
        widget.comparisonAnalysis != null &&
        widget.comparisonAnalysis!.characters.any((c) => c.isKaki);
    final combinedName = widget.result.name.trim();
    final combinedPhrase = (widget.comparisonName ?? '').trim();

    final textStyle = TextStyle(
      color: isGold ? AppColors.accent : AppColors.inputText,
      fontSize: 16,
      fontWeight: FontWeight.bold,
    );

    Widget nameWidget;

    if (widget.comparisonAnalysis != null && hasKaki) {
      final tp = TextPainter(
        text: TextSpan(text: combinedName, style: textStyle),
        textDirection: TextDirection.ltr,
      )..layout();

      nameWidget = CustomPaint(
        size: Size(tp.width, 32),
        painter: _ThaiHighlightPainter(
          highlights: widget.comparisonAnalysis!.characters,
          baseStyle: textStyle,
          isGold: isGold,
        ),
      );
    } else {
      nameWidget = Text(combinedName, style: textStyle);
    }

    final nameSection = isGold && !hasKaki
        ? ShimmeringGoldText(child: nameWidget)
        : nameWidget;

    if (combinedPhrase.isEmpty) {
      return nameSection;
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        RichText(
          text: TextSpan(
            style: GoogleFonts.sarabun(
              fontSize: 15,
              height: 1.45,
              color: AppColors.textLight,
            ),
            children: [
              TextSpan(
                text: '"',
                style: textStyle.copyWith(
                  fontSize: 17,
                  color: isGold ? AppColors.accent : AppColors.textLight,
                ),
              ),
              WidgetSpan(
                alignment: PlaceholderAlignment.middle,
                child: nameSection,
              ),
              TextSpan(
                text: '" $combinedPhrase',
                style: GoogleFonts.sarabun(
                  fontSize: 15,
                  height: 1.45,
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFF9A7B00),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildNameText(BuildContext context) {
    final bool isGold = widget.result.isSatGood && widget.result.isShaGood;
    final bool hasKaki = widget.result.kakiHighlight.any((h) => h.isKaki);

    final textStyle = TextStyle(
      fontSize: 22,
      fontWeight: FontWeight.w900,
      fontFamily: 'Sarabun',
      color: AppColors.textLight, // Themed text color
    );

    Widget nameWidget;
    if (widget.result.kakiHighlight.isEmpty) {
      nameWidget = Text(widget.result.name, style: textStyle);
    } else {
      // Calculate actual width to prevent fixed-size overflow
      final tp = TextPainter(
        text: TextSpan(text: widget.result.name, style: textStyle),
        textDirection: TextDirection.ltr,
      )..layout();

      nameWidget = CustomPaint(
        size: Size(tp.width, 36),
        painter: _ThaiHighlightPainter(
          highlights: widget.result.kakiHighlight,
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

  NameRootResult? _cachedRootData;

  void _showRootWordDialog(BuildContext context) {
    // Create future once, outside the builder — prevents recreation on rebuild
    final rootFuture = _cachedRootData != null
        ? Future.value(_cachedRootData)
        : ApiService().getNameRoot(widget.result.name);
    bool didSave = false;

    showDialog(
      context: context,
      builder: (dialogContext) {
        bool dialogSaving = false;
        bool dialogSaved = _isSaved;

        return StatefulBuilder(
          builder: (dialogContext, setDialogState) {
            return FutureBuilder<NameRootResult?>(
              future: rootFuture,
              builder: (context, snapshot) {
                Widget content;
                NameRootResult? rootData;

                if (snapshot.connectionState == ConnectionState.waiting) {
                  content = const SizedBox(
                    height: 100,
                    child: Center(
                      child: CircularProgressIndicator(color: Colors.blueGrey),
                    ),
                  );
                } else if (snapshot.hasError || snapshot.data == null) {
                  content = const Text(
                    "ไม่สามารถดึงข้อมูลรากศัพท์ได้",
                    style: TextStyle(color: Colors.black54),
                  );
                } else {
                  rootData = snapshot.data!;
                  _cachedRootData = rootData; // Cache the result
                  content = ConstrainedBox(
                    constraints: BoxConstraints(
                      maxHeight: MediaQuery.of(context).size.height * 0.6,
                    ),
                    child: SingleChildScrollView(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            rootData.analysis,
                            style: GoogleFonts.sarabun(
                              color: const Color(0xFF334155),
                              height: 1.8,
                              fontSize: 16,
                              letterSpacing: 0.1,
                            ),
                            textAlign: TextAlign.left,
                          ),
                          const SizedBox(height: 24),
                          Center(
                            child: Container(
                              width: 40,
                              height: 2,
                              color: Colors.black12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }

                return AlertDialog(
                  backgroundColor: const Color(0xFFF1F5F9),
                  shadowColor: Colors.black.withValues(alpha: 0.3),
                  surfaceTintColor: Colors.transparent,
                  elevation: 10,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                    side: BorderSide(
                      color: Colors.black.withValues(alpha: 0.05),
                    ),
                  ),
                  title: Column(
                    children: [
                      const Icon(
                        Icons.article_rounded,
                        color: Color(0xFF1E3A8A),
                        size: 40,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        "การวิเคราะห์รากศัพท์",
                        style: GoogleFonts.sarabun(
                          color: Colors.black45,
                          fontSize: 12,
                          letterSpacing: 1.2,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        widget.result.name,
                        style: GoogleFonts.sarabun(
                          color: Colors.black,
                          fontWeight: FontWeight.bold,
                          fontSize: 28,
                        ),
                      ),
                      const Divider(
                        color: Colors.black12,
                        height: 32,
                        thickness: 1,
                      ),
                    ],
                  ),
                  content: content,
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(dialogContext),
                      child: const Text(
                        "ปิด",
                        style: TextStyle(
                          color: Colors.black45,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    if (rootData != null && !dialogSaved)
                      ElevatedButton.icon(
                        onPressed: dialogSaving
                            ? null
                            : () async {
                                setDialogState(() => dialogSaving = true);
                                try {
                                  final deviceId = await ApiService()
                                      .getDeviceId();
                                  final payload = {
                                    "name": widget.result.name,
                                    "sat_sum": widget.result.satSum,
                                    "sha_sum": widget.result.shaSum,
                                    "is_sat_good": widget.result.isSatGood,
                                    "is_sha_good": widget.result.isShaGood,
                                    "root_word": rootData!.rootWord,
                                    "analysis": rootData.analysis,
                                    "device_id": deviceId,
                                  };
                                  final success = await ApiService().saveName(
                                    payload,
                                  );
                                  if (success) {
                                    didSave = true;
                                    setDialogState(() {
                                      dialogSaved = true;
                                      dialogSaving = false;
                                    });
                                    // Auto-close dialog after 1 second
                                    Future.delayed(
                                      const Duration(milliseconds: 1000),
                                      () {
                                        if (Navigator.canPop(dialogContext)) {
                                          Navigator.pop(dialogContext);
                                        }
                                      },
                                    );
                                    return; // skip finally
                                  }
                                } catch (e) {
                                  // Error
                                }
                                setDialogState(() => dialogSaving = false);
                              },
                        icon: dialogSaving
                            ? const SizedBox(
                                width: 16,
                                height: 16,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Icon(Icons.favorite_rounded, size: 18),
                        label: const Text("บันทึกชื่อนี้"),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF1E3A8A),
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                      ),
                    if (dialogSaved)
                      ElevatedButton.icon(
                        onPressed: null,
                        icon: const Icon(
                          Icons.check_circle,
                          size: 18,
                          color: Colors.white,
                        ),
                        label: const Text(
                          "บันทึกสำเร็จ",
                          style: TextStyle(color: Colors.white),
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.success,
                          disabledBackgroundColor: AppColors.success,
                          disabledForegroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                      ),
                  ],
                );
              },
            );
          },
        );
      },
    ).then((_) {
      // Update parent state and show toast only after dialog is closed
      if (didSave && mounted) {
        setState(() => _isSaved = true);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("บันทึกชื่อ ${widget.result.name} แล้ว"),
            backgroundColor: AppColors.success,
          ),
        );
      }
    });
  }

  // สีตามระดับ pairType
  static Color _pairTypeColor(String pairType) {
    switch (pairType.toUpperCase().trim()) {
      // ดี (เขียวอ่อน → เขียวเข้ม)
      case 'D5':
        return const Color(0xFF86EFAC); // เขียวอ่อน — ดี
      case 'D8':
        return const Color(0xFF10B981); // Emerald 500 — ดีมาก
      case 'D10':
        return const Color(
          0xFF059669,
        ); // Emerald 600 — ดีเยี่ยม (เขียวเข้มแต่สดใส)
      // ร้าย (แดงอ่อน → แดงเข้ม)
      case 'R5':
        return const Color(0xFFFCA5A5); // แดงอ่อน — ร้าย
      case 'R7':
        return const Color(0xFFEF4444); // แดงกลาง — ร้ายมาก
      case 'R10':
        return const Color(0xFF991B1B); // แดงเข้ม — ร้ายมากๆ
      default:
        return const Color(0xFF64748B); // สีเทาเมื่อไม่รู้ประเภท
    }
  }

  static String _pairTypeLabel(String pairType) {
    switch (pairType.toUpperCase().trim()) {
      case 'D5':
        return 'ดี';
      case 'D8':
        return 'ดีมาก';
      case 'D10':
        return 'ดีเยี่ยม';
      case 'R5':
        return 'ร้าย';
      case 'R7':
        return 'ร้ายมาก';
      case 'R10':
        return 'ร้ายมากๆ';
      default:
        return '';
    }
  }

  void _showNumberMeaningDialog(
    BuildContext context,
    String number,
    bool isGood,
  ) {
    showDialog(
      context: context,
      builder: (context) {
        return FutureBuilder<NumberMeaningResult?>(
          future: ApiService().getNumberMeaning(number),
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const AlertDialog(
                backgroundColor: Color(0xFF1E293B),
                content: SizedBox(
                  height: 100,
                  child: Center(child: CircularProgressIndicator()),
                ),
              );
            }

            if (snapshot.hasError || snapshot.data == null) {
              return AlertDialog(
                backgroundColor: const Color(0xFF1E293B),
                title: const Text(
                  "ข้อผิดพลาด",
                  style: TextStyle(color: Colors.white),
                ),
                content: const Text(
                  "ไม่สามารถดึงข้อมูลคำทำนายได้",
                  style: TextStyle(color: Colors.white70),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text(
                      "ปิด",
                      style: TextStyle(color: Colors.white54),
                    ),
                  ),
                ],
              );
            }

            final data = snapshot.data!;
            final pairType = data.pairType;
            final color = _pairTypeColor(pairType);
            final label = _pairTypeLabel(pairType);

            return AlertDialog(
              backgroundColor: const Color(0xFF1E293B),
              shadowColor: Colors.black.withValues(alpha: 0.5),
              surfaceTintColor: Colors.transparent,
              elevation: 20,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
                side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
              ),
              title: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: color,
                      shape: BoxShape.circle,
                      boxShadow: [
                        BoxShadow(
                          color: color.withValues(alpha: 0.4),
                          blurRadius: 12,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Text(
                      number,
                      style: TextStyle(
                        color: isGood ? const Color(0xFF052E16) : Colors.white,
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
                        Row(
                          children: [
                            Text(
                              "เลขศาสตร์ $number",
                              style: GoogleFonts.prompt(
                                color: Colors.white.withValues(alpha: 0.5),
                                fontSize: 12,
                              ),
                            ),
                            if (label.isNotEmpty) ...[
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 8,
                                  vertical: 2,
                                ),
                                decoration: BoxDecoration(
                                  color: color.withValues(alpha: 0.2),
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(
                                    color: color.withValues(alpha: 0.5),
                                    width: 1,
                                  ),
                                ),
                                child: Text(
                                  label,
                                  style: GoogleFonts.prompt(
                                    color: color,
                                    fontSize: 10,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                        Text(
                          data.description,
                          style: GoogleFonts.prompt(
                            color: Colors.white,
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
                      const Divider(color: Colors.white12, height: 24),
                      ..._buildVipDetailParts(
                        data.detail.replaceAll("\\n", "\n"),
                        isDark: true,
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
                    style: TextStyle(color: Colors.white54),
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }

  List<Widget> _buildVipDetailParts(String detailText, {bool isDark = false}) {
    final generalStyle = GoogleFonts.sarabun(
      color: (isDark ? Colors.white : AppColors.textGray).withValues(
        alpha: isDark ? 0.8 : 1.0,
      ),
      height: 1.7,
      fontSize: 15,
      letterSpacing: 0.1,
    );
    final goodHeaderColor = isDark
        ? const Color(0xFF4ADE80)
        : const Color(0xFF16A34A);
    final badHeaderColor = isDark
        ? const Color(0xFFF87171)
        : const Color(0xFFDC2626);
    final goodBodyColor = isDark
        ? const Color(0xFF4ADE80).withValues(alpha: 0.85)
        : const Color(0xFF16A34A);
    final badBodyColor = isDark
        ? const Color(0xFFF87171).withValues(alpha: 0.85)
        : const Color(0xFFDC2626);

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
              color: goodHeaderColor,
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
              color: goodBodyColor,
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
              color: badHeaderColor,
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
              color: badBodyColor,
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

class _ThaiHighlightPainter extends CustomPainter {
  final List<CharHighlight> highlights;
  final TextStyle baseStyle;
  final bool isGold;

  _ThaiHighlightPainter({
    required this.highlights,
    required this.baseStyle,
    this.isGold = false,
  });

  // Thai Combining Marks (0-width) that trigger dotted circles if painted alone.
  static final Set<int> _thaiCombiningMarks = {
    0x0E31, // Mai Han-Akat
    0x0E34, 0x0E35, 0x0E36, 0x0E37, 0x0E38, 0x0E39, 0x0E3A, // Vowels up/down
    0x0E47,
    0x0E48,
    0x0E49,
    0x0E4A,
    0x0E4B,
    0x0E4C,
    0x0E4D,
    0x0E4E, // Tones/Marks
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

    // 1. Group into Thai Grapheme Clusters to avoid shaping breaks (dotted circles)
    final clusters = <List<CharHighlight>>[];
    for (var h in highlights) {
      if (clusters.isEmpty || !_isCombining(h.char)) {
        clusters.add([h]);
      } else {
        clusters.last.add(h);
      }
    }

    double x = 0;
    // Calculate a common baseline for the entire text to ensure vertical alignment
    // We use a sample "tall" character to determine the line height/baseline
    final sampleTp = TextPainter(
      text: TextSpan(text: "ที่", style: baseStyle),
      textDirection: TextDirection.ltr,
    )..layout();
    final commonBaseline = sampleTp.computeDistanceToActualBaseline(
      TextBaseline.alphabetic,
    );

    // Center vertically within the available height
    final verticalOffset = (size.height - sampleTp.height) / 2;

    for (var cluster in clusters) {
      final clusterText = cluster.map((e) => e.char).join();
      final base = cluster[0];
      final baseColor = base.isKaki ? red : defaultColor;

      // Measure the full cluster to find its specific baseline
      final clusterTp = TextPainter(
        text: TextSpan(text: clusterText, style: baseStyle),
        textDirection: TextDirection.ltr,
      )..layout();
      final clusterBaseline = clusterTp.computeDistanceToActualBaseline(
        TextBaseline.alphabetic,
      );

      // We align this cluster's baseline to the common baseline (or just paint relative to its own)
      // Actually, standard text rendering aligns baselines.
      // If we simply want to align "layers", we should measure the LARGEST layer in this cluster
      // and align smaller layers to IT.

      // Let's rely on aligning to the cluster's own baseline.
      // We paint the cluster such that its baseline is at `verticalOffset + commonBaseline`.
      // y = (targetBaseline) - (thisTextBaseline)

      final targetBaselineY = verticalOffset + commonBaseline;

      // 2. Logic to paint parts of cluster with different colors
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
        // Mixed Case (e.g. White Base + Red Vowel)
        // Draw layers from full cluster down to base to ensure proper stacking
        // The "Full Cluster" (i=length) determines the baseline for this position.

        // We use the full cluster's metrics for the "Base" of alignment.
        final fullClusterBaseline = clusterBaseline;

        for (int i = cluster.length; i >= 1; i--) {
          final subCluster = cluster.sublist(0, i);
          final subText = subCluster.map((e) => e.char).join();
          final lastChar = subCluster.last;
          final color = lastChar.isKaki ? red : defaultColor;

          // Measure subText
          final subTp = TextPainter(
            text: TextSpan(text: subText, style: baseStyle),
            textDirection: TextDirection.ltr,
          )..layout();
          final subBaseline = subTp.computeDistanceToActualBaseline(
            TextBaseline.alphabetic,
          );

          // Align subText's baseline to targetBaselineY
          _draw(canvas, subText, color, x, targetBaselineY - subBaseline);
        }
      }

      x += clusterTp.width;
    }
  }

  @override
  bool shouldRepaint(covariant _ThaiHighlightPainter oldDelegate) => true;
}

// ─────────────────────────────────────────────────────────────────────────────
// _MagicLuckyBadge: ป้าย Double Lucky พร้อมลูกเล่น Animation ดาว และ Shimmer
// ─────────────────────────────────────────────────────────────────────────────
class _MagicLuckyBadge extends StatefulWidget {
  final String text;
  final List<Color> gradientColors;
  final bool isLucky;
  final VoidCallback onTap;

  const _MagicLuckyBadge({
    required this.text,
    required this.gradientColors,
    required this.isLucky,
    required this.onTap,
  });

  @override
  State<_MagicLuckyBadge> createState() => _MagicLuckyBadgeState();
}

class _MagicLuckyBadgeState extends State<_MagicLuckyBadge>
    with TickerProviderStateMixin {
  late AnimationController _glowController;
  late AnimationController _shimmerController;
  late AnimationController _particleController;
  late Animation<double> _glowAnim;
  late Animation<double> _shimmerAnim;
  late List<_LuckyParticle> _particles;

  @override
  void initState() {
    super.initState();

    // 1. Pulsing glow on border
    _glowController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    )..repeat(reverse: true);
    _glowAnim = CurvedAnimation(
      parent: _glowController,
      curve: Curves.easeInOut,
    );

    // 2. Shimmer sweep
    _shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2200),
    )..repeat();
    _shimmerAnim = _shimmerController;

    // 3. Floating particles (Stars)
    _particleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3000),
    )..repeat();

    final rng = math.Random();
    _particles = List.generate(
      6,
      (i) => _LuckyParticle(
        x: rng.nextDouble(),
        y: rng.nextDouble(),
        size: 1.5 + rng.nextDouble() * 2.0,
        speed: 0.2 + rng.nextDouble() * 0.5,
        phase: rng.nextDouble(),
      ),
    );
  }

  @override
  void dispose() {
    _glowController.dispose();
    _shimmerController.dispose();
    _particleController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.isLucky) {
      // Return simple non-lucky version
      return GestureDetector(
        onTap: widget.onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 10),
          decoration: BoxDecoration(
            color: widget.gradientColors.last.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: widget.gradientColors.last.withValues(alpha: 0.2),
              width: 1,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.info_outline_rounded,
                color: widget.gradientColors.last.withValues(alpha: 0.7),
                size: 14,
              ),
              const SizedBox(width: 6),
              Text(
                widget.text,
                style: TextStyle(
                  color: widget.gradientColors.last,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
      );
    }

    final color =
        widget.gradientColors.last; // Use the most vibrant color for effects

    return AnimatedBuilder(
      animation: Listenable.merge([
        _glowAnim,
        _shimmerAnim,
        _particleController,
      ]),
      builder: (context, _) {
        final glow = _glowAnim.value;
        return GestureDetector(
          onTap: widget.onTap,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: widget.gradientColors,
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: const BorderRadius.only(
                bottomLeft: Radius.circular(8),
                topRight: Radius.circular(24),
                topLeft: Radius.circular(24),
                bottomRight: Radius.circular(24),
              ),
              boxShadow: [
                BoxShadow(
                  color: color.withValues(alpha: 0.2 + glow * 0.2),
                  blurRadius: 10 + glow * 12,
                  spreadRadius: glow * 2,
                ),
                BoxShadow(
                  color: const Color(
                    0xFF8B5CF6,
                  ).withValues(alpha: 0.1 + glow * 0.1),
                  blurRadius: 15 + glow * 10,
                  spreadRadius: glow * 1,
                ),
              ],
            ),
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                // Shimmer sweep overlay
                Positioned.fill(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(24),
                    child: CustomPaint(
                      painter: _LuckyShimmerPainter(
                        progress: _shimmerAnim.value,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
                // Floating sparkle particles
                Positioned.fill(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(24),
                    child: CustomPaint(
                      painter: _LuckyParticlePainter(
                        particles: _particles,
                        progress: _particleController.value,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
                // Icon and text
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Shimmering Icon
                    ShaderMask(
                      shaderCallback: (bounds) => LinearGradient(
                        colors: [
                          Colors.white,
                          const Color(0xFF8B5CF6).withValues(alpha: 0.5),
                          Colors.white,
                        ],
                        stops: [
                          (_shimmerAnim.value - 0.2).clamp(0.0, 1.0),
                          _shimmerAnim.value.clamp(0.0, 1.0),
                          (_shimmerAnim.value + 0.2).clamp(0.0, 1.0),
                        ],
                      ).createShader(bounds),
                      child: const Icon(
                        Icons.auto_awesome,
                        color: Colors.white,
                        size: 14,
                      ),
                    ),
                    const SizedBox(width: 6),
                    // Animated text with shimmer sweep
                    ShaderMask(
                      shaderCallback: (bounds) => LinearGradient(
                        colors: [
                          Colors.white,
                          const Color(0xFFFFFFFF).withValues(alpha: 0.7),
                          Colors.white,
                        ],
                        stops: [
                          (_shimmerAnim.value - 0.1).clamp(0.0, 1.0),
                          _shimmerAnim.value.clamp(0.0, 1.0),
                          (_shimmerAnim.value + 0.1).clamp(0.0, 1.0),
                        ],
                      ).createShader(bounds),
                      child: Text(
                        widget.text,
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
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _LuckyParticle {
  final double x, y, size, speed, phase;
  _LuckyParticle({
    required this.x,
    required this.y,
    required this.size,
    required this.speed,
    required this.phase,
  });
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
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
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

class _LuckyShimmerPainter extends CustomPainter {
  final double progress;
  final Color color;
  _LuckyShimmerPainter({required this.progress, required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final sweepX = -size.width + progress * size.width * 2.5;
    final paint = Paint()
      ..shader = LinearGradient(
        colors: [
          Colors.transparent,
          color.withValues(alpha: 0.05),
          Colors.white.withValues(alpha: 0.12),
          color.withValues(alpha: 0.05),
          Colors.transparent,
        ],
        stops: const [0.0, 0.3, 0.5, 0.7, 1.0],
        begin: Alignment.centerLeft,
        end: Alignment.centerRight,
        transform: const GradientRotation(math.pi / 4),
      ).createShader(Rect.fromLTWH(sweepX, 0, size.width * 0.6, size.height));
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height), paint);
  }

  @override
  bool shouldRepaint(_LuckyShimmerPainter old) => old.progress != progress;
}

class _LuckyParticlePainter extends CustomPainter {
  final List<_LuckyParticle> particles;
  final double progress;
  final Color color;
  _LuckyParticlePainter({
    required this.particles,
    required this.progress,
    required this.color,
  });

  @override
  void paint(Canvas canvas, Size size) {
    for (final p in particles) {
      final t = (progress * p.speed + p.phase) % 1.0;
      final px =
          (p.x * size.width + math.sin(t * math.pi * 2 + p.phase * 5) * 6) %
          size.width;
      final py = (size.height - (t * (size.height + 10))) % size.height;
      final opacity = math.sin(t * math.pi).clamp(0.0, 1.0);

      final paint = Paint()
        ..color = color.withValues(alpha: opacity * 0.8)
        ..style = PaintingStyle.fill;

      _drawStar(canvas, Offset(px, py), p.size * opacity, paint);
    }
  }

  void _drawStar(Canvas canvas, Offset center, double size, Paint paint) {
    final path = Path();
    for (int i = 0; i < 4; i++) {
      final angle = i * math.pi / 2;
      final tip = Offset(
        center.dx + math.cos(angle) * size * 2.2,
        center.dy + math.sin(angle) * size * 2.2,
      );
      final left = Offset(
        center.dx + math.cos(angle + math.pi / 2) * size * 0.4,
        center.dy + math.sin(angle + math.pi / 2) * size * 0.4,
      );
      final right = Offset(
        center.dx + math.cos(angle - math.pi / 2) * size * 0.4,
        center.dy + math.sin(angle - math.pi / 2) * size * 0.4,
      );
      if (i == 0) path.moveTo(left.dx, left.dy);
      path.lineTo(tip.dx, tip.dy);
      path.lineTo(right.dx, right.dy);
      path.lineTo(center.dx, center.dy);
    }
    path.close();
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(_LuckyParticlePainter old) => old.progress != progress;
}

/// Subtle Thai Kanok (กนก) pattern painter for card backgrounds.
/// Draws stylized lotus/petal motifs that evoke traditional Thai artistry.
class _ThaiKanokPatternPainter extends CustomPainter {
  final Color color;
  final int rank;

  _ThaiKanokPatternPainter({required this.color, required this.rank});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.8
      ..strokeCap = StrokeCap.round;

    final fillPaint = Paint()
      ..color = color.withValues(alpha: (color.a * 0.3))
      ..style = PaintingStyle.fill;

    // Draw corner kanok motifs
    _drawCornerKanok(canvas, size, paint, fillPaint, topRight: true);
    _drawCornerKanok(canvas, size, paint, fillPaint, topRight: false);

    // Subtle center petal for top-3
    if (rank > 0 && rank <= 3) {
      _drawCenterLotus(canvas, size, paint, fillPaint);
    }

    // Small dot accents scattered
    _drawDotAccents(canvas, size, paint);
  }

  void _drawCornerKanok(
    Canvas canvas,
    Size size,
    Paint strokePaint,
    Paint fillPaint, {
    required bool topRight,
  }) {
    final dx = topRight ? size.width : 0.0;
    final flipX = topRight ? -1.0 : 1.0;

    canvas.save();
    canvas.translate(dx, 0);

    // Kanok petal curve (top corner)
    final petal = Path();
    final petalSize = size.width * 0.12;
    petal.moveTo(0, 0);
    petal.cubicTo(
      flipX * petalSize * 0.6, petalSize * 0.3,
      flipX * petalSize * 0.5, petalSize * 0.8,
      flipX * petalSize * 0.15, petalSize * 1.1,
    );
    petal.cubicTo(
      flipX * petalSize * 0.4, petalSize * 0.7,
      flipX * petalSize * 0.8, petalSize * 0.4,
      flipX * petalSize * 1.0, petalSize * 0.1,
    );
    petal.cubicTo(
      flipX * petalSize * 0.7, petalSize * 0.05,
      flipX * petalSize * 0.3, -petalSize * 0.05,
      0, 0,
    );

    canvas.drawPath(petal, fillPaint);
    canvas.drawPath(petal, strokePaint);

    // Inner swirl detail
    final swirl = Path();
    swirl.moveTo(flipX * petalSize * 0.15, petalSize * 0.25);
    swirl.quadraticBezierTo(
      flipX * petalSize * 0.4, petalSize * 0.45,
      flipX * petalSize * 0.3, petalSize * 0.65,
    );
    canvas.drawPath(swirl, strokePaint);

    canvas.restore();
  }

  void _drawCenterLotus(
    Canvas canvas,
    Size size,
    Paint strokePaint,
    Paint fillPaint,
  ) {
    final cx = size.width * 0.92;
    final cy = size.height * 0.55;
    final petalLen = size.height * 0.10;

    for (int i = 0; i < 5; i++) {
      final angle = (i * math.pi * 2 / 5) - math.pi / 2;
      final path = Path();
      path.moveTo(cx, cy);
      path.quadraticBezierTo(
        cx + math.cos(angle + 0.25) * petalLen * 0.6,
        cy + math.sin(angle + 0.25) * petalLen * 0.6,
        cx + math.cos(angle) * petalLen,
        cy + math.sin(angle) * petalLen,
      );
      path.quadraticBezierTo(
        cx + math.cos(angle - 0.25) * petalLen * 0.6,
        cy + math.sin(angle - 0.25) * petalLen * 0.6,
        cx,
        cy,
      );
      canvas.drawPath(path, fillPaint);
      canvas.drawPath(path, strokePaint);
    }
  }

  void _drawDotAccents(Canvas canvas, Size size, Paint paint) {
    final dotPaint = Paint()
      ..color = color.withValues(alpha: (color.a * 0.5))
      ..style = PaintingStyle.fill;

    // Fixed positions for subtle dots (not random, so repaint is stable)
    final dots = [
      Offset(size.width * 0.08, size.height * 0.85),
      Offset(size.width * 0.15, size.height * 0.92),
      Offset(size.width * 0.85, size.height * 0.88),
    ];

    for (final dot in dots) {
      canvas.drawCircle(dot, 1.2, dotPaint);
    }
  }

  @override
  bool shouldRepaint(_ThaiKanokPatternPainter old) =>
      old.color != color || old.rank != rank;
}
