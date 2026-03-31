import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/rendering.dart';
import 'package:google_fonts/google_fonts.dart';
import '../models/name_model.dart';
import '../models/name_root_result.dart';
import '../models/number_meaning_model.dart';
import '../services/api_service.dart';
import '../services/premium_manager.dart';
import '../utils/colors.dart';
import '../widgets/gold_effect.dart';
import '../widgets/name_list_item.dart';
import '../widgets/paywall_dialog.dart';
import 'saved_names_screen.dart';
import 'info_screen.dart';
import '../widgets/filter_chip.dart' show FilterChipWidget;
import '../widgets/dashboard_summary.dart' show DashboardSummary;
import '../widgets/magic_loading.dart';

class NamingScreen extends StatefulWidget {
  const NamingScreen({super.key});

  @override
  State<NamingScreen> createState() => _NamingScreenState();
}

enum SearchMode { meaning, name }

class _NamingScreenState extends State<NamingScreen>
    with TickerProviderStateMixin {
  // Release default: keep premium-ranked names locked for non-VIP users.
  static const bool _temporaryShowAllRankedNames = false;

  final ApiService _apiService = ApiService();
  final TextEditingController _keywordController = TextEditingController();
  String? _selectedNameMeaningName;
  String? _selectedNameMeaning;
  bool _isLoadingSelectedNameMeaning = false;
  NameAnalysisResult? _selectedNameAnalysis;
  int? _selectedCelebrityIndex; // Track which celebrity avatar is selected
  int? _selectedExampleIndex; // Track which search idea is active
  String? _selectedDay;
  bool _filterSat = false;
  bool _filterSha = false;
  bool _filterKaki = false;
  bool _similarMode = false;
  List<MobileNameResult> _results = [];
  List<Map<String, dynamic>> _celebrities = [];
  bool _isLoading = false;
  bool _isPivotingIdea = false;
  String? _errorMessage;
  final ScrollController _scrollController = ScrollController();
  final GlobalKey _resultsKey = GlobalKey();
  final GlobalKey _step2Key = GlobalKey();
  final GlobalKey _searchFieldKey = GlobalKey();
  bool _showBackToTop = false;
  bool _hasSearched = false;
  bool _isRelaxedSearch =
      false; // New state to track if we show fallback results
  int _searchRequestId = 0;
  final FocusNode _searchFocusNode = FocusNode();
  final ScrollController _celebsScrollController = ScrollController();
  Timer? _celebsAutoScrollTimer;
  Ticker? _marqueeTicker;
  bool _isCelebsAutoScrolling = true;
  double _lastTickerElapsedMs = 0;

  String? _relaxedFiltersNotice; // Labels of filters that were turned off

  NameSuggestionsResponse? _nameSuggestions;
  bool _loadingSuggestions = false;

  final SearchMode _searchMode = SearchMode.meaning;

  final List<String> _days = [
    'Sunday',
    'Monday',
    'Tuesday',
    'Wednesday1',
    'Wednesday2',
    'Thursday',
    'Friday',
    'Saturday',
  ];

  final Map<String, String> _dayLabels = {
    'Sunday': 'เกิดวันอาทิตย์',
    'Monday': 'เกิดวันจันทร์',
    'Tuesday': 'เกิดวันอังคาร',
    'Wednesday1': 'เกิดวันพุธ (กลางวัน)',
    'Wednesday2': 'เกิดวันพุธ (กลางคืน)',
    'Thursday': 'เกิดวันพฤหัสบดี',
    'Friday': 'เกิดวันศุกร์',
    'Saturday': 'เกิดวันเสาร์',
  };

  static const List<Map<String, dynamic>> _ideaExamples = [
    {
      "text": "เศรษฐีผู้มั่งคั่ง มีทรัพย์สมบัติและบารมี",
      "icon": Icons.trending_up,
      "iconColor": Color(0xFFD4A017),
    },
    {
      "text": "หญิงสาวผู้อ่อนหวาน มีเสน่ห์ และเป็นที่รัก",
      "icon": Icons.favorite,
      "iconColor": Color(0xFFE66A8D),
    },
    {
      "text": "ผู้นำที่กล้าหาญ เจริญรุ่งเรือง ไร้อุปสรรค",
      "icon": Icons.shield,
      "iconColor": Color(0xFF4F8FE8),
    },
    {
      "text": "ปราชญ์ผู้มีสติปัญญาเฉลียวฉลาด และอายุยืน",
      "icon": Icons.psychology,
      "iconColor": Color(0xFF8B6CD9),
    },
  ];

  Future<void> _search({
    bool scrollToResults = false,
    bool showInputSnack = true,
    bool reloadSelectedName = true,
    String? overrideKeyword,
  }) async {
    final int requestId = ++_searchRequestId;
    FocusScope.of(context).unfocus();
    final keyword = (overrideKeyword ?? _keywordController.text).trim();
    if (keyword.isEmpty) {
      if (mounted && showInputSnack) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: AppColors.textLight, // Deep brown background
            behavior:
                SnackBarBehavior.floating, // ลอยขึ้นมาให้ดูทันสมัยและเป็นกันเอง
            margin: const EdgeInsets.all(16),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            content: Row(
              children: [
                const Icon(
                  Icons
                      .auto_awesome_rounded, // ไอคอนวิ้งๆ ให้ดูเป็นกันเองและเข้ากับธีม
                  color: AppColors.accent,
                  size: 20,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    _searchMode == SearchMode.meaning
                        ? "พิมพ์ความหมายที่ต้องการ หรือแตะตัวอย่างก่อนค้นหานะคะ"
                        : "พิมพ์ชื่อที่ต้องการค้นหา หรือแตะตัวอย่างก่อนค้นหานะคะ",
                    style: GoogleFonts.sarabun(
                      color: Colors.white,
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      }
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _hasSearched = true;
    });

    String finalKeyword = keyword;

    // Always attempt to load numerology/meaning for the search query if it's not a long example phrase
    if (reloadSelectedName && _selectedExampleIndex == null) {
      await loadSelectedNameMeaning(finalKeyword);
    } else if (reloadSelectedName) {
      setState(() {
        _selectedNameAnalysis = null;
        _selectedNameMeaningName = null;
        _selectedNameMeaning = null;
      });
    }

    // --- 1. SET PARAMETERS ---
    String actualTarget = overrideKeyword ?? finalKeyword;
    String finalLastname = _similarMode ? actualTarget : "";

    // 🔍 Broad Discovery Logic:
    // If Similar Mode is ON, we want to find ANY name that works with the anchor.
    // If no specific idea phrase is active, we send an EMPTY keyword strings ("")
    // to the backend, which now supports returning a broad pool of candidates
    // for the lastname anchor.
    String apiSearchKeyword =
        (_similarMode &&
            _selectedExampleIndex == null &&
            _selectedCelebrityIndex == null)
        ? ""
        : actualTarget;

    try {
      // --- 2. SUGGESTION PIVOT (For long sentences/ideas) ---
      if (_selectedExampleIndex != null && overrideKeyword == null) {
        setState(() => _isPivotingIdea = true);
        final suggRes = await _apiService.getNameSuggestions(finalKeyword);
        setState(() => _isPivotingIdea = false);

        if (suggRes != null && suggRes.names.isNotEmpty) {
          final bestName = suggRes.names[0].name;
          if (mounted) {
            await _search(
              scrollToResults: scrollToResults,
              overrideKeyword: bestName,
            );
          }
          return;
        }
      }

      // --- 3. PRIMARY API CALL ---
      var response = await _apiService.searchNames(
        keyword: apiSearchKeyword,
        lastname: finalLastname,
        semanticMeaning: _selectedNameMeaning,
        day: _selectedDay,
        filterSat: _filterSat,
        filterSha: _filterSha,
        filterKaki: _filterKaki,
        similarMode: _similarMode,
        limit: 100, // Fetch a large pool to ensure we find variety
      );

      // --- 4. DATA PROCESSING & FILTERING ---
      final bool useTotal = _similarMode && finalLastname.isNotEmpty;
      final englishRegex = RegExp(r'[a-zA-Z]');

      List<MobileNameResult> filterAndClean(List<MobileNameResult> list) {
        return list.where((r) {
          final name = r.name.trim();
          if (englishRegex.hasMatch(name)) return false;
          if (name.runes.length <= 1) return false;

          // In combine mode, users still see the base-name scores on the card.
          // If a "good numerology/shadow" chip is active, require both the
          // combined result and the visible base score to pass so no red score
          // slips into ranked results.
          final bool passesSatFilter = useTotal
              ? (r.isSatGood && r.isTotalSatGood)
              : r.isSatGood;
          final bool passesShaFilter = useTotal
              ? (r.isShaGood && r.isTotalShaGood)
              : r.isShaGood;

          // 🛡️ VIP Exclusive Gate:
          // ป้องกันชื่อที่ "ดีเยี่ยมด้วยตัวเอง" (เขียวคู่แบบเดี่ยว) หลุดมา
          // ถ้า User ไม่ได้เปิดฟิลเตอร์ไว้ทั้ง 2 ตัว
          // หมายเหตุ: ชื่อที่ "รวมแล้วดี" (Total Green) แต่ตัวชื่อเองไม่เขียวคู่ จะยังคงแสดงผลได้
          // ยกเว้นใน "รวมให้เป็นชื่อดี" (_similarMode) ซึ่งเป็นฟีเจอร์พรีเมียมอยู่แล้ว ให้แสดงได้ทั้งหมด
          // Keep excellent names in the result set.
          // Non‑VIP restriction is handled by UI blur/lock overlay, not by skipping rows.

          // 🎯 Standard Inclusive Filters:
          if (_filterSat && !passesSatFilter) return false;
          if (_filterSha && !passesShaFilter) return false;

          return true;
        }).toList();
      }

      var results = filterAndClean(response.results);

      // Ignore stale responses so old searches cannot override scroll/state.
      if (!mounted || requestId != _searchRequestId) return;

      setState(() {
        _isRelaxedSearch = false; // Reset state
        _results = results;

        // Note: Filters (Sat, Sha, Gender) are ALREADY applied at database level by the API.
        // We do NOT need to filter them out again on the client side,
        // especially avoiding 'exclusive' filters that hide excellent names.

        // Custom Ranking Algorithm: Priority to Good Numerology + Short Length
        results.sort((a, b) {
          if (a.finalRankScore != b.finalRankScore) {
            return b.finalRankScore.compareTo(a.finalRankScore);
          }
          double score(MobileNameResult item) {
            return item
                .calculateScore(
                  showMatching:
                      _similarMode && _keywordController.text.isNotEmpty,
                )
                .toDouble();
          }

          return score(b).compareTo(score(a)); // Sort High Score -> Low Score
        });

        _results = results;
        _isLoading = false;
      });

      if (_results.isNotEmpty && scrollToResults) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted || requestId != _searchRequestId) return;
          scrollToResults0();
        });
      }
    } catch (e) {
      if (!mounted || requestId != _searchRequestId) return;
      if (mounted) {
        setState(() {
          _errorMessage = e is ApiException
              ? e.message
              : "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง";
          _isLoading = false;
          _results = []; // Clear old results on error
        });
      }
    }
  }

  Map<String, dynamic> computeStatistics() {
    final bool useTotal = _similarMode && _keywordController.text.isNotEmpty;
    bool satPassesCurrentMode(MobileNameResult r) =>
        useTotal ? (r.isSatGood && r.isTotalSatGood) : r.isSatGood;
    bool shaPassesCurrentMode(MobileNameResult r) =>
        useTotal ? (r.isShaGood && r.isTotalShaGood) : r.isShaGood;
    int totalNames = _results.length;

    // Logic updated: Always count the actual good names in the current results
    int excellentNames = _results.where((r) {
      final satPass = satPassesCurrentMode(r);
      final shaPass = shaPassesCurrentMode(r);
      return satPass && shaPass;
    }).length;

    int numerologyGood = _results.where((r) => satPassesCurrentMode(r)).length;

    int shadowGood = _results.where((r) => shaPassesCurrentMode(r)).length;

    String? recommendedDays;
    return {
      'totalNames': totalNames,
      'excellentNames': excellentNames,
      'numerologyGood': numerologyGood,
      'shadowGood': shadowGood,
      'recommendedDays': recommendedDays,
    };
  }

  @override
  void initState() {
    super.initState();
    _keywordController.addListener(onSearchInputChanged);
    _searchFocusNode.addListener(() {
      if (mounted) setState(() {});
    });
    _scrollController.addListener(() {
      if (_scrollController.offset > 400 && !_showBackToTop) {
        setState(() => _showBackToTop = true);
      } else if (_scrollController.offset <= 400 && _showBackToTop) {
        setState(() => _showBackToTop = false);
      }
    });

    // Prefetch cached saved names so list items know if they are saved automatically
    _apiService.loadSavedNamesCache().then((_) {
      if (mounted) setState(() {});
    });

    loadCelebrities();
    initPremium();
  }

  void onSearchInputChanged() {
    final text = _keywordController.text.trim();
    debugPrint("Search input changed: '$text', mode: $_searchMode");

    // Reset ranking results and handle active states
    if (mounted) {
      setState(() {
        _results = [];
        _hasSearched = false;
        _isRelaxedSearch = false;
        _relaxedFiltersNotice = null;

        // Improved Logic: Be more forgiving with string comparison (trim both)
        if (_selectedExampleIndex != null) {
          final target =
              _ideaExamples[_selectedExampleIndex!]['text'] as String;
          if (text.trim() != target.trim()) {
            _selectedExampleIndex = null;
          }
        }
        if (_selectedCelebrityIndex != null && _celebrities.isNotEmpty) {
          final int modIndex = _selectedCelebrityIndex! % _celebrities.length;
          if (modIndex < _celebrities.length) {
            final celeb = _celebrities[modIndex];
            final targetName = (celeb['name'] ?? '') as String;
            if (text.trim() != targetName.trim()) {
              _selectedCelebrityIndex = null;
            }
          }
        }
      });
    }

    // Handle suggestion based on search mode
    if (text.isEmpty) {
      setState(() {
        _selectedNameMeaningName = null;
        _selectedNameMeaning = null;
        _selectedNameAnalysis = null;
        _isLoadingSelectedNameMeaning = false;
        _nameSuggestions = null;
        _loadingSuggestions = false;
      });
      return;
    }

    // Always fetch suggestions since Backend now handles both pg_trgm & semantic
    // It will also load the meaning/analysis of the currently typed text inside the debouncer
    fetchNameSuggestionsDebounced(text);

    if (mounted) setState(() {});
  }

  Future<void> loadSelectedNameMeaning(String name, {String? meaning}) async {
    final trimmed = name.trim();
    if (trimmed.isEmpty) return;
    setState(() {
      _selectedNameMeaningName = trimmed;
      _selectedNameMeaning = meaning; // Use provided meaning if available
      _selectedNameAnalysis = null;
      _isLoadingSelectedNameMeaning = true;
    });

    try {
      final futures = await Future.wait([
        // Only fetch meaning if it wasn't provided
        meaning == null
            ? _apiService.getNameMeaning(trimmed)
            : Future.value(meaning),
        _apiService.decodeName(trimmed),
      ]);

      final fetchedMeaning = futures[0] as String?;
      final analysis = futures[1] as NameAnalysisResult?;

      if (!mounted) return;
      if (_selectedNameMeaningName != trimmed) return;
      setState(() {
        _selectedNameMeaning =
            (fetchedMeaning != null && fetchedMeaning.trim().isNotEmpty)
            ? fetchedMeaning.trim()
            : (meaning != null && meaning.trim().isNotEmpty
                  ? meaning.trim()
                  : null);
        _selectedNameAnalysis = analysis;
        _isLoadingSelectedNameMeaning = false;
      });

      // After we get the meaning, we can fetch better suggestions based on that meaning
      if (_selectedNameMeaning != null &&
          _selectedNameMeaning!.isNotEmpty &&
          _selectedNameMeaning != name) {
        fetchNameSuggestions(name, meaning: _selectedNameMeaning);
      } else {
        // Fallback to searching ideas/suggestions with just the name
        fetchNameSuggestions(name);
      }
    } catch (e) {
      debugPrint("Error loading selected name meaning: $e");
      if (mounted && _selectedNameMeaningName == trimmed) {
        setState(() {
          _isLoadingSelectedNameMeaning = false;
        });
        fetchNameSuggestions(name);
      }
    }
  }

  void scrollToTop() {
    _scrollController.animateTo(
      0,
      duration: const Duration(milliseconds: 500),
      curve: Curves.easeInOut,
    );
  }

  Future<void> initPremium() async {
    await PremiumManager().init();
    if (mounted) setState(() {});
  }

  Future<void> loadCelebrities() async {
    try {
      debugPrint("Fetching naming examples...");
      final examples = await _apiService.getNamingExamples();
      debugPrint("Naming examples fetched: ${examples.length}");

      if (examples.isNotEmpty) {
        // If API returns data, use it but ensure we have enough items for tablet display
        List<Map<String, dynamic>> finalItems = examples;
        if (finalItems.length < 6) {
          // If too few, pad with empty/initial items to fill width
          finalItems = [...finalItems, ...finalItems];
        }

        setState(() {
          _celebrities = finalItems;
        });
        // Start auto-scroll after data is loaded and rendered
        WidgetsBinding.instance.addPostFrameCallback((_) {
          // Give it a tiny bit more time for the ScrollController to attach and lay out
          Future.delayed(const Duration(milliseconds: 100), () {
            if (mounted && _celebsScrollController.hasClients) {
              final double max =
                  _celebsScrollController.position.maxScrollExtent;
              if (max > 10) {
                _celebsScrollController.jumpTo(max * 0.5);
              }
            }
            startCelebsAutoScroll();
          });
        });
      } else {
        debugPrint("Naming examples API returned empty list.");
        // Clear celebrities if API fails or returns empty, do not use fallbacks
        setState(() {
          _celebrities = [];
        });
      }
    } catch (e) {
      debugPrint("Error loading celebs: $e");
      // Clear celebrities on error
      setState(() {
        _celebrities = [];
      });
    }
  }

  void startCelebsAutoScroll() {
    startMarqueeTicker();
  }

  void startMarqueeTicker() {
    _marqueeTicker ??= createTicker(onTickerTick);
    if (!_marqueeTicker!.isActive) {
      _lastTickerElapsedMs = 0;
      _marqueeTicker!.start();
    }
  }

  void onTickerTick(Duration elapsed) {
    if (!mounted ||
        !_isCelebsAutoScrolling ||
        !_celebsScrollController.hasClients) {
      _lastTickerElapsedMs = elapsed.inMilliseconds.toDouble();
      return;
    }

    final double currentMs = elapsed.inMilliseconds.toDouble();
    if (_lastTickerElapsedMs == 0) {
      _lastTickerElapsedMs = currentMs;
      return;
    }

    final double dt = (currentMs - _lastTickerElapsedMs) / 1000.0;
    _lastTickerElapsedMs = currentMs;

    if (dt <= 0) return;

    final double max = _celebsScrollController.position.maxScrollExtent;
    if (max <= 100) return;

    final int itemCount = _celebrities.length;
    final double baseSpeed = 25.0;
    final double speedMultiplier = itemCount > 20 ? 1.5 : 1.0;
    final double dynamicSpeed = baseSpeed * speedMultiplier;

    final double current = _celebsScrollController.offset;
    double next = current + (dynamicSpeed * dt);

    if (next >= max - 2) {
      next = max * 0.5;
    } else if (next <= 2) {
      next = max * 0.5;
    }

    _celebsScrollController.jumpTo(next);
  }

  void _normalizeCelebsOffsetIfNeeded() {
    if (!mounted || !_celebsScrollController.hasClients) return;
    if (_celebrities.isEmpty) return;

    final double max = _celebsScrollController.position.maxScrollExtent;
    if (max <= 100) return;
    final double current = _celebsScrollController.offset;
    if (current <= 2 || current >= max - 2) {
      _celebsScrollController.jumpTo(max * 0.5);
    }
  }

  void stopMarqueeTicker() {
    _isCelebsAutoScrolling = false;
    _marqueeTicker?.stop();
  }

  void resumeCelebsAutoScrollAfterDelay() {
    _celebsAutoScrollTimer?.cancel();
    _celebsAutoScrollTimer = Timer(const Duration(seconds: 2), () {
      if (mounted) {
        startCelebsAutoScroll();
      }
    });
  }

  void scrollToResults0() {
    if (_resultsKey.currentContext != null) {
      Scrollable.ensureVisible(
        _resultsKey.currentContext!,
        duration: const Duration(milliseconds: 600),
        curve: Curves.easeInOut,
        alignment:
            0.0, // Keep results summary at top for clearer post-filter context
      );
    }
  }

  void scrollToSearchField() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final context = _searchFieldKey.currentContext;
      if (context != null) {
        Scrollable.ensureVisible(
          context,
          duration: const Duration(milliseconds: 600),
          curve: Curves.easeInOutCubic,
          alignment: 0.0, // Scroll until the widget is at the top
        );
      }
    });
  }

  @override
  void dispose() {
    _searchFocusNode.dispose();
    _celebsAutoScrollTimer?.cancel();
    _celebsScrollController.dispose();
    _keywordController.removeListener(onSearchInputChanged);
    _celebsAutoScrollTimer?.cancel();
    _marqueeTicker?.dispose();
    _keywordController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Timer? suggestionsDebounceTimer;
  void fetchNameSuggestionsDebounced(String query) {
    debugPrint("Debounced suggestion call for: '$query'");
    suggestionsDebounceTimer?.cancel();
    suggestionsDebounceTimer = Timer(
      const Duration(milliseconds: 300),
      () async {
        await loadSelectedNameMeaning(query);
      },
    );
  }

  Future<void> fetchNameSuggestions(String query, {String? meaning}) async {
    if (!mounted) return;

    debugPrint("Fetching name suggestions for: '$query' (meaning: $meaning)");
    setState(() => _loadingSuggestions = true);

    try {
      var suggestions = await _apiService.getNameSuggestions(
        query,
        meaning: meaning,
      );

      if (suggestions != null) {
        // Filter out English names and single characters from suggestions
        final englishRegex = RegExp(r'[a-zA-Z]');
        final normalizedQuery = query.trim();
        final seenMeanings = <String>{};
        final filteredNames =
            suggestions.names.where((item) {
                final name = item.name.trim();
                if (englishRegex.hasMatch(name)) return false;
                if (name.runes.length <= 1) return false;
                return true;
              }).toList()
              ..sort((a, b) {
                final aExact = a.name.trim() == normalizedQuery;
                final bExact = b.name.trim() == normalizedQuery;
                if (aExact == bExact) return 0;
                return aExact ? -1 : 1;
              })
              ..retainWhere((item) {
                final isExactMatch = item.name.trim() == normalizedQuery;
                final normalizedMeaning = item.meaning.trim().replaceAll(
                  RegExp(r'\s+'),
                  ' ',
                );
                if (isExactMatch) return true;
                if (normalizedMeaning.isEmpty) return true;
                if (seenMeanings.contains(normalizedMeaning)) return false;
                seenMeanings.add(normalizedMeaning);
                return true;
              });

        // Create a new response object with filtered names since the field is final
        suggestions = NameSuggestionsResponse(
          ideas: suggestions.ideas,
          names: filteredNames,
        );
      }

      if (mounted) {
        // Prevent race condition: ignore results for an old query if the user has moved on to a new name
        if (_selectedNameMeaningName != null &&
            _selectedNameMeaningName != query.trim()) {
          return;
        }

        setState(() {
          _nameSuggestions = suggestions;
          _loadingSuggestions = false;
        });
      }
    } catch (e) {
      debugPrint("Error fetching name suggestions: $e");
      if (mounted) {
        if (_selectedNameMeaningName != null &&
            _selectedNameMeaningName != query.trim()) {
          return;
        }

        setState(() {
          _nameSuggestions = null;
          _loadingSuggestions = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgDark,
      floatingActionButton: AnimatedSwitcher(
        duration: const Duration(milliseconds: 300),
        transitionBuilder: (Widget child, Animation<double> animation) {
          return ScaleTransition(scale: animation, child: child);
        },
        child: _showBackToTop
            ? FloatingActionButton(
                key: const ValueKey('back_to_top'),
                onPressed: scrollToTop,
                mini: true,
                backgroundColor: AppColors.primary.withOpacity(0.9),
                elevation: 4,
                child: const Icon(Icons.arrow_upward, color: Colors.white),
              )
            : const SizedBox.shrink(key: ValueKey('no_back_to_top')),
      ),
      body: Listener(
        onPointerDown: (_) => FocusManager.instance.primaryFocus?.unfocus(),
        behavior: HitTestBehavior.translucent,
        child: SafeArea(
          child: CustomScrollView(
            controller: _scrollController,
            keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
            slivers: [
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 0,
                    vertical: 0,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      const SizedBox(height: 10),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: buildHeader(),
                      ),
                      const SizedBox(height: 24),
                      buildSearchForm(),
                      const SizedBox(height: 16),
                      if (_results.isNotEmpty)
                        Padding(
                          key: _resultsKey,
                          padding: const EdgeInsets.only(
                            left: 20,
                            right: 20,
                            bottom: 16,
                          ),
                          child: Builder(
                            builder: (context) {
                              final stats = computeStatistics();
                              return Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  DashboardSummary(
                                    totalNames: stats['totalNames'] as int,
                                    excellentNames: stats['excellentNames']
                                        .toString(),
                                    numerologyGood: stats['numerologyGood']
                                        .toString(),
                                    shadowGood: stats['shadowGood'].toString(),
                                    isSatActive: _filterSat,
                                    isShaActive: _filterSha,
                                    recommendedDays:
                                        stats['recommendedDays'] as String?,
                                    onInfoTap: () {
                                      Navigator.push(
                                        context,
                                        MaterialPageRoute(
                                          builder: (context) =>
                                              InformationScreen(
                                                initialTabIndex: 2,
                                              ),
                                        ),
                                      );
                                    },
                                  ),
                                ],
                              );
                            },
                          ),
                        ),
                    ],
                  ),
                ),
              ),
              if (_errorMessage != null)
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: buildErrorState(_errorMessage!),
                  ),
                ),
              if (_isLoading && _results.isEmpty)
                const SliverToBoxAdapter(
                  child: Padding(
                    padding: EdgeInsets.symmetric(vertical: 40),
                    child: Center(
                      child: MagicLoadingView(
                        message: "กำลังจัดลำดับชื่อที่ดีที่สุด...",
                        subtitle:
                            "กำลังอ่านความหมาย พลังชื่อ และคัดลำดับที่เหมาะกับคุณมากที่สุด",
                        textColor:
                            AppColors.textLight, // Use high-contrast brown text
                      ),
                    ),
                  ),
                )
              else if (_results.isEmpty &&
                  _hasSearched &&
                  _errorMessage == null)
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 12,
                    ),
                    child: buildResultsEmptyState(),
                  ),
                )
              else ...[
                if (_isRelaxedSearch && _relaxedFiltersNotice != null)
                  SliverToBoxAdapter(
                    child: Container(
                      margin: const EdgeInsets.fromLTRB(20, 0, 20, 12),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF59E0B).withOpacity(0.15),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: const Color(0xFFF59E0B).withOpacity(0.3),
                        ),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.info_outline_rounded,
                            color: Color(0xFFFBBF24),
                            size: 18,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              _relaxedFiltersNotice!,
                              style: GoogleFonts.sarabun(
                                color: const Color(0xFFFBBF24),
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                SliverList(
                  delegate: SliverChildBuilderDelegate((context, index) {
                    final item = _results[index];
                    final bool useTotalForVipGate =
                        _similarMode && _keywordController.text.isNotEmpty;
                    final bool isDoubleGreen = useTotalForVipGate
                        ? (item.isTotalSatGood && item.isTotalShaGood)
                        : (item.isSatGood && item.isShaGood);
                    // Release behavior: lock premium names for non-VIP users.
                    final bool isLockedForNonVip =
                        isDoubleGreen &&
                        !PremiumManager().isPremium &&
                        !_temporaryShowAllRankedNames;

                    final card = NameListItem(
                      key: ValueKey(
                        'matching_${_keywordController.text.hashCode}_${item.name}',
                      ),
                      rank: index + 1,
                      result: item,
                      comparisonName: _similarMode
                          ? _keywordController.text
                          : "",
                      comparisonAnalysis: null,
                      // Show combined section whenever similar mode is ON (backend may return good or bad combined scores)
                      showMatching:
                          _similarMode && _keywordController.text.isNotEmpty,
                      isFilterSatActive: _filterSat,
                      isFilterShaActive: _filterSha,
                      isFilterKakiActive: _filterKaki,
                      onTap: () {
                        final name = item.name;
                        setState(() {
                          _keywordController.text = name;
                          _keywordController.selection =
                              TextSelection.collapsed(offset: name.length);

                          // Once a specific name is selected, the "Idea" highlight has served its discovery purpose.
                          _selectedExampleIndex = null;
                          _selectedCelebrityIndex = null;
                        });

                        // Perform re-analysis by searching for this name
                        loadSelectedNameMeaning(name, meaning: item.meaning);
                        _search(scrollToResults: false, showInputSnack: false);
                        scrollToSearchField();
                      },
                    );

                    if (!isLockedForNonVip) return card;

                    return Stack(
                      children: [
                        IgnorePointer(ignoring: true, child: card),
                        Positioned(
                          left: 20,
                          top: 45,
                          child: Material(
                            color: Colors.transparent,
                            child: InkWell(
                              borderRadius: BorderRadius.circular(999),
                              onTap: () async {
                                final purchased = await showPaywallDialog(
                                  context,
                                );
                                if (purchased && mounted) {
                                  setState(() {});
                                }
                              },
                              child: Container(
                                constraints: const BoxConstraints(
                                  maxWidth: 340,
                                ),
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 14,
                                  vertical: 8,
                                ),
                                decoration: BoxDecoration(
                                  color: Colors.black.withOpacity(0.9),
                                  borderRadius: BorderRadius.circular(999),
                                  boxShadow: [
                                    BoxShadow(
                                      color: Colors.black.withOpacity(0.32),
                                      blurRadius: 10,
                                      offset: const Offset(0, 4),
                                    ),
                                  ],
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Text(
                                      "ชื่อพรีเมียม",
                                      style: GoogleFonts.prompt(
                                        color: Colors.white,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Container(
                                      width: 1,
                                      height: 14,
                                      color: Colors.white24,
                                    ),
                                    const SizedBox(width: 8),
                                    const Icon(
                                      Icons.visibility_off_rounded,
                                      color: Colors.white,
                                      size: 14,
                                    ),
                                    const SizedBox(width: 4),
                                    Text(
                                      "กดปิดป้าย",
                                      style: GoogleFonts.prompt(
                                        color: Colors.white,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    );
                  }, childCount: _results.length),
                ),
              ],
              SliverToBoxAdapter(child: buildFooter()),
              const SliverToBoxAdapter(
                child: SizedBox(height: 60),
              ), // Space for FAB/BottomNav
            ],
          ),
        ),
      ),
    );
  }

  Widget buildHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Premium Title with more prominence
              Text(
                "ชื่อดี",
                textAlign: TextAlign.left,
                style: GoogleFonts.prompt(
                  fontSize: 36, // Larger and more prominent
                  fontWeight: FontWeight.w900, // Thicker
                  color: AppColors.textLight, // Deep brown for high contrast
                  letterSpacing: -0.8,
                  shadows: [
                    Shadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 2),
              Text(
                "ค้นหาความหมายเพื่อเลือกชื่อที่เหมาะกับคุณ",
                style: GoogleFonts.sarabun(
                  color: AppColors.textGray,
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  height: 1.2,
                ),
              ),
            ],
          ),
        ),
        GestureDetector(
          onTap: () async {
            final selectedName = await Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => const SavedNamesScreen()),
            );

            if (selectedName is Map) {
              final dynamic nameValue = selectedName['name'];
              final dynamic modeValue = selectedName['mode'];
              final name = nameValue is String ? nameValue : null;
              final mode = modeValue is String ? modeValue : null;
              if (name == null) return;

              if (mode == 'keyword') {
                setState(() {
                  _keywordController.text = name;
                  _similarMode = false;
                });
                loadSelectedNameMeaning(name);
                fetchNameSuggestionsDebounced(name);
                _search(scrollToResults: true);
                return;
              }

              if (mode == 'matching') {
                setState(() {
                  _keywordController.text = name;
                  _similarMode = true;
                });
                fetchNameSuggestionsDebounced(name);
                _search(scrollToResults: true);
                return;
              }

              setState(() {
                _keywordController.text = name;
                _similarMode = true;
              });
              fetchNameSuggestionsDebounced(name);
              _search(scrollToResults: true);
            } else if (selectedName != null && selectedName is String) {
              setState(() {
                _keywordController.text = selectedName;
                _similarMode = true;
              });
              fetchNameSuggestionsDebounced(selectedName);
              _search(scrollToResults: true);
            }
          },
          child: Container(
            padding: const EdgeInsets.all(
              8,
            ), // Perfectly sized for the sparkling icon
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  const Color(0xFFD946EF).withOpacity(0.12),
                  const Color(0xFFD946EF).withOpacity(0.04),
                ],
              ),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: const Color(0xFFD946EF).withOpacity(0.25),
                width: 1,
              ),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFFD946EF).withOpacity(0.05),
                  blurRadius: 12,
                  spreadRadius: 1,
                ),
              ],
            ),
            child: const SparklingGoldHeart(),
          ),
        ),
      ],
    );
  }

  Widget buildErrorState(String message) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF7ED), // Warm Amber background (Avoid Red)
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.orangeAccent.withOpacity(0.3)),
      ),
      child: Column(
        children: [
          const Icon(
            Icons.wifi_off_rounded,
            color: Colors.orangeAccent,
            size: 32,
          ),
          const SizedBox(height: 12),
          Text(
            "ขออภัยค่ะ ตอนนี้ระบบยังเชื่อมต่อไม่สำเร็จ",
            style: GoogleFonts.sarabun(
              color: AppColors.textLight,
              fontWeight: FontWeight.bold,
              fontSize: 16,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            message,
            style: GoogleFonts.sarabun(color: AppColors.textGray, fontSize: 14),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => _search(scrollToResults: false),
              icon: const Icon(Icons.refresh),
              label: Text(
                "ลองเชื่อมต่ออีกครั้ง",
                style: GoogleFonts.sarabun(fontWeight: FontWeight.bold),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget buildResultsEmptyState() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.inputBackground,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.primary.withOpacity(0.25)),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withOpacity(0.05),
            blurRadius: 16,
            spreadRadius: 2,
          ),
        ],
      ),
      child: Column(
        children: [
          Icon(
            Icons.search_off_rounded,
            color: AppColors.primary.withOpacity(0.6),
            size: 40,
          ),
          const SizedBox(height: 10),
          Text(
            "เงื่อนไขที่คุณเลือกไม่มีรายชื่อ",
            style: GoogleFonts.sarabun(
              color: AppColors.textLight,
              fontWeight: FontWeight.w700,
              fontSize: 16,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () {
                setState(() {
                  _filterSat = false;
                  _filterSha = false;
                  _filterKaki = false;
                });
                _search(scrollToResults: false);
              },
              icon: Icon(Icons.tune_rounded, color: AppColors.secondary),
              label: Text(
                "ลองแบบไม่กรองก่อน",
                style: GoogleFonts.sarabun(
                  color: AppColors.secondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              style: OutlinedButton.styleFrom(
                side: BorderSide(color: AppColors.primary.withOpacity(0.4)),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                padding: const EdgeInsets.symmetric(vertical: 12),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget buildSelectedNameMeaningUnderKeyword() {
    if (_isPivotingIdea) return const SizedBox.shrink();
    if (_selectedNameMeaningName == null) return const SizedBox.shrink();

    if (_isLoadingSelectedNameMeaning) return const SizedBox.shrink();
    if (_selectedNameMeaning == null && _selectedNameAnalysis == null) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (_selectedNameMeaning != null)
          Padding(
            padding: const EdgeInsets.only(
              top: 24,
              bottom: 8,
            ), // เพิ่มระยะห่างเว้นจากช่องค้นหา
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.bgDarker, // Elegant themed background
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: AppColors.accent.withOpacity(0.3),
                  width: 1.5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 15,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: AppColors.primary.withOpacity(0.1),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.menu_book_rounded,
                          color: AppColors.secondary,
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text.rich(
                              TextSpan(
                                text: "ความหมายของชื่อ ",
                                style: GoogleFonts.sarabun(
                                  color: const Color(
                                    0xFF3D2600,
                                  ).withOpacity(0.6),
                                  fontSize: 13,
                                  fontWeight: FontWeight.w500,
                                ),
                                children: [
                                  TextSpan(
                                    text: " $_selectedNameMeaningName",
                                    style: GoogleFonts.prompt(
                                      color: const Color(0xFF3D2600),
                                      fontSize: 18,
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 12),
                            Padding(
                              padding: const EdgeInsets.only(right: 120),
                              child: Text(
                                _selectedNameMeaning!,
                                style: GoogleFonts.prompt(
                                  color: AppColors.textGray,
                                  fontSize: 16,
                                  height: 1.6,
                                  fontWeight: FontWeight.w500,
                                  letterSpacing: 0.2,
                                ),
                              ),
                            ),
                            const SizedBox(height: 14),
                          ],
                        ),
                      ),
                    ],
                  ),

                  // Absolute Positioned Root Word Button (Bottom Right)
                  if (_selectedNameAnalysis != null)
                    Positioned(
                      bottom: -12,
                      right: -8,
                      child: Container(
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [AppColors.secondary, AppColors.accent],
                          ),
                          borderRadius: BorderRadius.circular(20),
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.secondary.withOpacity(0.3),
                              blurRadius: 12,
                              offset: const Offset(0, 4),
                            ),
                          ],
                          border: Border.all(
                            color: Colors.white.withOpacity(0.5),
                            width: 1.5,
                          ),
                        ),
                        child: Material(
                          color: Colors.transparent,
                          child: InkWell(
                            onTap: () => showRootWordDialog(
                              name: _selectedNameAnalysis!.name,
                              satSum: _selectedNameAnalysis!.satSum,
                              shaSum: _selectedNameAnalysis!.shaSum,
                              isSatGood: _selectedNameAnalysis!.isSatGood,
                              isShaGood: _selectedNameAnalysis!.isShaGood,
                              meaning: _selectedNameMeaning,
                            ),
                            borderRadius: BorderRadius.circular(20),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 14,
                                vertical: 8,
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Icon(
                                    Icons.auto_stories_rounded,
                                    size: 16,
                                    color: Colors.white,
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    "รากศัพท์",
                                    style: GoogleFonts.prompt(
                                      color: Colors.white,
                                      fontSize: 13,
                                      fontWeight: FontWeight.bold,
                                      letterSpacing: 0.5,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        if (_selectedNameAnalysis != null) ...[
          const SizedBox(height: 10),
          _MagicSummaryWrapper(
            isActive:
                _selectedNameAnalysis!.isSatGood &&
                _selectedNameAnalysis!.isShaGood,
            color: const Color(0xFFFFD700), // Gold color
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    buildAnalysisScoreWithSmart(
                      "เลขศาสตร์:",
                      _selectedNameAnalysis!.satSum,
                      _selectedNameAnalysis!.isSatGood,
                    ),
                    buildAnalysisScoreWithSmart(
                      "พลังเงา:",
                      _selectedNameAnalysis!.shaSum,
                      _selectedNameAnalysis!.isShaGood,
                    ),
                  ],
                ),

                // Absolute Positioned Remark Button (Top Right)
                Positioned(
                  top: -8,
                  right: -8,
                  child: Tooltip(
                    message: "อ่านคำอธิบายเลขศาสตร์และพลังเงา",
                    child: Material(
                      color: Colors.white.withOpacity(0.05),
                      borderRadius: BorderRadius.circular(8),
                      child: InkWell(
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) =>
                                  const InformationScreen(initialTabIndex: 0),
                            ),
                          );
                        },
                        borderRadius: BorderRadius.circular(8),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.info_outline_rounded,
                                color: Colors.white.withOpacity(0.4),
                                size: 12,
                              ),
                              const SizedBox(width: 4),
                              Text(
                                "หมายเหตุ",
                                style: GoogleFonts.prompt(
                                  color: Colors.white.withOpacity(0.4),
                                  fontSize: 10,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget buildSearchForm() {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.primary.withOpacity(
          0.05,
        ), // Light vibrant primary tint
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: AppColors.primary.withOpacity(0.1), width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 24, 12, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // ===== STEP 1: ค้นหาชื่อจากความหมาย (PRIMARY) =====
                Row(
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFF0F9D7A), Color(0xFF13B38A)],
                        ),
                        borderRadius: BorderRadius.circular(10),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFF0F9D7A).withOpacity(0.3),
                            blurRadius: 10,
                            offset: const Offset(0, 2),
                          ),
                        ],
                      ),
                      child: const Text(
                        '1',
                        style: TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        "ค้นหาชื่อตามความหมายที่ต้องการ...",
                        style: GoogleFonts.prompt(
                          color: AppColors.textLight,
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                if (_celebrities.isNotEmpty)
                  Row(
                    children: [
                      Container(
                        width: 4,
                        height: 18,
                        decoration: BoxDecoration(
                          color: AppColors.accent,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                      const SizedBox(width: 8),
                      const Padding(
                        padding: EdgeInsets.only(top: 15, bottom: 0),
                        child: Text(
                          'สแกนชื่อคนที่ประสบความสำเร็จ',
                          style: TextStyle(
                            fontFamily: 'Prompt',
                            color: AppColors.textLight,
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            height: 1.5,
                          ),
                          overflow: TextOverflow.visible,
                          maxLines: 1,
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ),
          if (_celebrities.isNotEmpty) ...[
            const SizedBox(height: 10),
            MediaQuery.removePadding(
              context: context,
              removeLeft: true,
              removeRight: true,
              child: buildCelebritiesRow(),
            ),
            const SizedBox(height: 6),
          ],
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 10),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                buildExamples(),
                const SizedBox(height: 20),
                buildSearchField(),
                const SizedBox(height: 24),

                // ===== STEP 2: ตั้งค่าเพิ่มเติม (SECONDARY) =====
                buildMagicRankingHeader(),
                const SizedBox(height: 18), // Increased from 12

                Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(child: buildDropdown()),
                    const SizedBox(width: 8),
                    FilterChipWidget(
                      label: "กาลกิณี (คัดออก)",
                      icon: Icons.warning,
                      isActive: _selectedDay != null && _filterKaki,
                      onTap: () {
                        if (_selectedDay == null) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text(
                                "ระบุวันเกิด เพื่อคัดอักษรกาลกิณีตามตำราโบราณนะคะ",
                              ),
                              backgroundColor: Colors.orange,
                              duration: Duration(seconds: 2),
                            ),
                          );
                          return;
                        }
                        setState(() => _filterKaki = !_filterKaki);
                        _refreshResultsKeepingStep2Anchor();
                      },
                    ),
                  ],
                ),
                const SizedBox(
                  height: 24,
                ), // Increased from 16 to give VIP Badge space
                buildFilterChipsSection(),
                const SizedBox(height: 6),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget buildExamples() {
    final examples = _ideaExamples;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 18,
                decoration: BoxDecoration(
                  color: AppColors.accent,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 8),
              const Padding(
                padding: EdgeInsets.only(top: 15, bottom: 0),
                child: Text(
                  'ไอเดียค้นหาจากประโยคตัวอย่าง',
                  style: TextStyle(
                    fontFamily: 'Prompt',
                    color: AppColors.textLight,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    height: 1.5,
                    letterSpacing: 0.5,
                  ),
                  overflow: TextOverflow.visible,
                  maxLines: 1,
                ),
              ),
            ],
          ),
        ),
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: examples.asMap().entries.map((entry) {
            final int index = entry.key;
            final item = entry.value;
            final text = item['text'] as String;
            final icon = item['icon'] as IconData;
            final iconColor = item['iconColor'] as Color? ?? AppColors.accent;
            final bool isActive = _selectedExampleIndex == index;

            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Container(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: isActive
                      ? [
                          BoxShadow(
                            color: AppColors.accent.withOpacity(0.2),
                            blurRadius: 15,
                            spreadRadius: 2,
                          ),
                        ]
                      : null,
                ),
                child: Material(
                  color: Colors.transparent,
                  child: InkWell(
                    onTap: () {
                      setState(() {
                        _keywordController.text = text;
                        _selectedExampleIndex = index;
                        _selectedCelebrityIndex = null;
                        _similarMode = false;
                        _filterSat = false;
                        _filterSha = false;
                      });
                      _search();
                      scrollToSearchField();
                    },
                    borderRadius: BorderRadius.circular(16),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 12,
                      ),
                      decoration: BoxDecoration(
                        color: isActive
                            ? const Color(0xFFFDF4FF)
                            : Colors.white.withOpacity(0.9),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: isActive
                              ? AppColors.accent
                              : Colors.white.withOpacity(0.15),
                          width: isActive ? 2.0 : 1.0,
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            icon,
                            size: 20,
                            color: isActive
                                ? iconColor
                                : iconColor.withOpacity(0.75),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              text,
                              style: GoogleFonts.prompt(
                                color: isActive
                                    ? const Color(0xFF5C3C10)
                                    : AppColors.textLight.withOpacity(0.8),
                                fontSize: 15,
                                fontWeight: isActive
                                    ? FontWeight.w700
                                    : FontWeight.w600,
                              ),
                            ),
                          ),
                          if (isActive)
                            const Icon(
                              Icons.check_circle_rounded,
                              size: 20,
                              color: AppColors.accent,
                            ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget buildCelebritiesRow() {
    if (_celebrities.isEmpty) return const SizedBox.shrink();

    const double celebItemWidth = 70;
    const double celebStride = celebItemWidth;
    final int totalCount = _celebrities.length * 400;

    return NotificationListener<ScrollNotification>(
      onNotification: (notification) {
        if (notification is UserScrollNotification) {
          if (notification.direction != ScrollDirection.idle) {
            _isCelebsAutoScrolling = false;
          } else {
            _normalizeCelebsOffsetIfNeeded();
            resumeCelebsAutoScrollAfterDelay();
          }
        } else if (notification is ScrollEndNotification) {
          _normalizeCelebsOffsetIfNeeded();
        }
        return false;
      },
      child: SizedBox(
        height: 98,
        child: ListView.builder(
          controller: _celebsScrollController,
          scrollDirection: Axis.horizontal,
          padding: EdgeInsets.zero,
          primary: false,
          physics: const ClampingScrollPhysics(),
          itemCount: totalCount,
          itemBuilder: (context, virtualIndex) {
            final int modIndex = virtualIndex % _celebrities.length;
            final Map<String, dynamic> c = _celebrities[modIndex];
            final String name = c['name'] ?? '';
            final String initial = c['initial'] ?? '';
            final String avatarUrl = c['avatar_url'] ?? '';

            Color bgColor = AppColors
                .avatarBorders[modIndex % AppColors.avatarBorders.length];
            if (c['color'] != null) {
              try {
                String hex = c['color'].replaceAll('#', '');
                if (hex.length == 6) hex = 'FF$hex';
                bgColor = Color(int.parse(hex, radix: 16));
              } catch (_) {}
            }

            final bool isSelected = _selectedCelebrityIndex == virtualIndex;

            return Padding(
              padding: const EdgeInsets.only(top: 4, bottom: 4),
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTapDown: (_) {
                  // Stop marquee immediately on touch down
                  stopMarqueeTicker();
                  _celebsAutoScrollTimer?.cancel();

                  if (_celebsScrollController.hasClients) {
                    _celebsScrollController.jumpTo(
                      _celebsScrollController.offset,
                    );
                  }
                },
                onTapCancel: () {
                  // Resume auto-scroll if it was a drag gesture
                  resumeCelebsAutoScrollAfterDelay();
                },
                onTap: () {
                  // Execute selection logic ONLY on actual tap
                  setState(() {
                    _keywordController.text = name;
                    _similarMode = false;
                    _selectedCelebrityIndex = virtualIndex;
                    _selectedExampleIndex = null;
                  });

                  // Trigger Analysis immediately
                  loadSelectedNameMeaning(name);
                  fetchNameSuggestionsDebounced(name);

                  // Centering with animation
                  if (context.mounted) {
                    final double screenWidth = MediaQuery.of(
                      context,
                    ).size.width;
                    final double avatarCenterInList =
                        (virtualIndex * celebStride) + (celebItemWidth / 2.0);
                    final double targetOffset =
                        avatarCenterInList - (screenWidth / 2.0);

                    _celebsScrollController
                        .animateTo(
                          targetOffset.clamp(
                            0.0,
                            _celebsScrollController.position.maxScrollExtent,
                          ),
                          duration: const Duration(milliseconds: 800),
                          curve: Curves.easeOutCubic,
                        )
                        .then((_) {
                          _celebsAutoScrollTimer?.cancel();
                          _celebsAutoScrollTimer = Timer(
                            const Duration(seconds: 7),
                            () {
                              if (mounted) {
                                startCelebsAutoScroll();
                              }
                            },
                          );
                        });
                  }

                  // Execute search results
                  Future.delayed(const Duration(milliseconds: 600), () {
                    if (mounted) {
                      _search(scrollToResults: false);
                      scrollToSearchField();
                    }
                  });
                },
                child: SizedBox(
                  width: celebItemWidth,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 58,
                        height: 58,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: bgColor.withOpacity(0.15),
                          border: Border.all(
                            color: isSelected ? AppColors.accent : bgColor,
                            width: isSelected ? 3.5 : 2.5,
                          ),
                          boxShadow: isSelected
                              ? [
                                  BoxShadow(
                                    color: const Color(
                                      0xFFD4AF37,
                                    ).withOpacity(0.4),
                                    blurRadius: 15,
                                    spreadRadius: 2,
                                  ),
                                ]
                              : null,
                        ),
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            if (avatarUrl.isEmpty)
                              Text(
                                initial,
                                style: GoogleFonts.prompt(
                                  fontWeight: FontWeight.bold,
                                  color: AppColors.textLight,
                                  fontSize: 14,
                                ),
                              )
                            else
                              ClipOval(
                                child: Image.network(
                                  avatarUrl.startsWith('http')
                                      ? avatarUrl
                                      : "${ApiService.baseUrl}$avatarUrl",
                                  fit: BoxFit.cover,
                                  width: 58,
                                  height: 58,
                                  headers: const {
                                    "User-Agent":
                                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                                    "Accept":
                                        "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
                                  },
                                  errorBuilder: (context, error, stackTrace) {
                                    debugPrint(
                                      "Failed to load tablet image: $error for URL: $avatarUrl",
                                    );
                                    return Container(
                                      color: bgColor,
                                      alignment: Alignment.center,
                                      child: Text(
                                        initial,
                                        style: GoogleFonts.prompt(
                                          fontWeight: FontWeight.bold,
                                          color: Colors.white,
                                          fontSize: 24,
                                        ),
                                      ),
                                    );
                                  },
                                ),
                              ),
                            if (_isLoadingSelectedNameMeaning && isSelected)
                              Container(
                                width: 58,
                                height: 58,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: Colors.black.withOpacity(0.4),
                                ),
                                child: const Center(
                                  child: SizedBox(
                                    width: 24,
                                    height: 24,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 3,
                                      valueColor: AlwaysStoppedAnimation<Color>(
                                        AppColors.accent,
                                      ),
                                      backgroundColor: Colors.white30,
                                    ),
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 6),
                      SizedBox(
                        width: celebItemWidth,
                        child: Text(
                          name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.center,
                          style: GoogleFonts.sarabun(
                            color: const Color(0xFF4F8FE8),
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            height: 1.15,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  // _buildAutocompleteField removed

  Widget buildSearchField() {
    const hint = "ค้นหาชื่อ หรือ ความหมายที่ต้องการ...";

    return Column(
      key: _searchFieldKey,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ValueListenableBuilder<TextEditingValue>(
          valueListenable: _keywordController,
          builder: (context, value, child) {
            final isFocused = _searchFocusNode.hasFocus;
            final hasText = value.text.isNotEmpty;

            return AnimatedContainer(
              duration: const Duration(milliseconds: 400),
              curve: Curves.easeOutCubic,
              decoration: BoxDecoration(
                color: const Color(0xFFFFF8FF),
                borderRadius: BorderRadius.circular(22),
                border: Border.all(
                  color: hasText
                      ? const Color(0xFFE879F9)
                      : (isFocused
                            ? const Color(0xFFD946EF)
                            : const Color(0xFFD946EF).withOpacity(0.35)),
                  width: hasText ? 3.0 : (isFocused ? 2.5 : 1.5),
                ),
                boxShadow: [
                  // Outer Aura
                  BoxShadow(
                    color: const Color(
                      0xFFD946EF,
                    ).withOpacity(hasText ? 0.42 : (isFocused ? 0.24 : 0.1)),
                    blurRadius: hasText ? 50 : (isFocused ? 25 : 15),
                    offset: const Offset(0, 10),
                    spreadRadius: hasText ? 8 : (isFocused ? 2 : 0),
                  ),
                  // Inner Glow (only when has text)
                  if (hasText)
                    BoxShadow(
                      color: const Color(0xFFB517FF).withOpacity(0.24),
                      blurRadius: 20,
                      spreadRadius: -1,
                    ),
                  // High-intensity core glow
                  if (hasText)
                    BoxShadow(
                      color: const Color(0xFFFF4FA3).withOpacity(0.28),
                      blurRadius: 10,
                      spreadRadius: -4,
                    ),
                ],
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(left: 18, right: 10),
                    child: TweenAnimationBuilder<double>(
                      tween: Tween<double>(begin: 0, end: hasText ? 1.0 : 0.0),
                      duration: const Duration(milliseconds: 500),
                      builder: (context, value, child) {
                        return Icon(
                          Icons.auto_awesome_rounded,
                          color: Color.lerp(
                            isFocused
                                ? const Color(0xFFFF4FA3)
                                : const Color(0xFFFF4FA3).withOpacity(0.55),
                            const Color(0xFFB517FF),
                            value,
                          ),
                          size: 18 + (value * 4),
                        );
                      },
                    ),
                  ),
                  Expanded(
                    child: TextField(
                      focusNode: _searchFocusNode,
                      controller: _keywordController,
                      minLines: 1,
                      maxLines: 2,
                      style: GoogleFonts.prompt(
                        color: const Color(0xFF7E22CE),
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0.1,
                      ),
                      textInputAction: TextInputAction.search,
                      decoration: InputDecoration(
                        hintText: hint,
                        hintStyle: GoogleFonts.sarabun(
                          color: const Color(0xFF7E22CE).withOpacity(0.45),
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                        ),
                        border: InputBorder.none,
                        contentPadding: const EdgeInsets.symmetric(
                          vertical: 18,
                        ),
                      ),
                      onSubmitted: (_) {
                        FocusScope.of(context).unfocus();
                        _search(scrollToResults: false);
                      },
                    ),
                  ),
                  // Clear button
                  ValueListenableBuilder<TextEditingValue>(
                    valueListenable: _keywordController,
                    builder: (context, value, child) {
                      if (value.text.isEmpty) return const SizedBox.shrink();
                      return IconButton(
                        icon: Icon(
                          Icons.cancel_rounded,
                          color: const Color(0xFF7E22CE).withOpacity(0.4),
                          size: 22,
                        ),
                        onPressed: () {
                          _keywordController.clear();
                          setState(() {
                            _selectedExampleIndex = null;
                            _selectedCelebrityIndex = null;
                          });
                        },
                      );
                    },
                  ),
                  const SizedBox(width: 8),
                ],
              ),
            );
          },
        ),

        AnimatedSize(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          alignment: Alignment.topCenter,
          child: buildSelectedNameMeaningUnderKeyword(),
        ),

        // Combined Suggestion Box
        Builder(
          builder: (context) {
            final isTyping = _keywordController.text.isNotEmpty;
            final hasNames = _nameSuggestions?.names.isNotEmpty ?? false;
            final showApiSuggestions =
                isTyping && (hasNames || _loadingSuggestions);

            if (!showApiSuggestions) {
              return const SizedBox.shrink();
            }

            return AnimatedSize(
              duration: const Duration(milliseconds: 220),
              curve: Curves.easeOutCubic,
              alignment: Alignment.topCenter,
              child: Container(
                margin: EdgeInsets.only(top: hasNames ? 10 : 14),
                width: double.infinity,
                padding: EdgeInsets.fromLTRB(
                  12,
                  _loadingSuggestions ? 14 : 12,
                  12,
                  _loadingSuggestions ? 14 : 12,
                ),
                decoration: BoxDecoration(
                  color: AppColors.bgDark,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.primary.withOpacity(0.3)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.05),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // --- SECTION 1: API SUGGESTIONS (NAMES) ---
                    if (_loadingSuggestions || hasNames) ...[
                      Row(
                        children: [
                          const Icon(
                            Icons.auto_awesome_rounded,
                            color: AppColors.secondary,
                            size: 16,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            "รายชื่อที่ความหมายใกล้เคียง:",
                            style: GoogleFonts.prompt(
                              color: AppColors.secondary,
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 8,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.secondary.withOpacity(0.06),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(
                            color: AppColors.secondary.withOpacity(0.14),
                          ),
                        ),
                        child: Text(
                          "เรียงจากชื่อที่มีความหมายใกล้กับชื่อที่คุณพิมพ์มากที่สุดอยู่ด้านบน แล้วค่อยลดหลั่นลงมา โดยใช้ AI semantic search เทียบจากความหมายเป็นหลัก",
                          style: GoogleFonts.sarabun(
                            color: AppColors.textGray,
                            fontSize: 12,
                            height: 1.45,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      SizedBox(height: _loadingSuggestions ? 12 : 10),
                      if (_loadingSuggestions)
                        const Center(
                          child: Padding(
                            padding: EdgeInsets.fromLTRB(8, 6, 8, 2),
                            child: MagicLoadingView(
                              height: 64,
                              message: "กำลังค้นหาไอเดีย...",
                              textColor: AppColors.textGray,
                            ),
                          ),
                        )
                      else
                        Column(
                          children: _nameSuggestions!.names.map((item) {
                            return Material(
                              color: Colors.transparent,
                              child: InkWell(
                                onTap: () {
                                  _keywordController.text = item.name;
                                  _keywordController.selection =
                                      TextSelection.collapsed(
                                        offset: item.name.length,
                                      );
                                  // Close keyboard first
                                  FocusScope.of(context).unfocus();

                                  loadSelectedNameMeaning(
                                    item.name,
                                    meaning: item.meaning,
                                  );
                                  _search();
                                  // Scroll to show the search field and result
                                  scrollToSearchField();
                                },
                                borderRadius: BorderRadius.circular(8),
                                child: Container(
                                  width: double.infinity,
                                  padding: const EdgeInsets.symmetric(
                                    vertical: 10,
                                    horizontal: 8,
                                  ),
                                  margin: const EdgeInsets.only(bottom: 4),
                                  decoration: BoxDecoration(
                                    border: Border(
                                      bottom: BorderSide(
                                        color: AppColors.textGray.withOpacity(
                                          0.1,
                                        ),
                                      ),
                                    ),
                                  ),
                                  child: Row(
                                    children: [
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              item.name,
                                              style: GoogleFonts.prompt(
                                                color: AppColors.textLight,
                                                fontSize: 16,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                            const SizedBox(height: 2),
                                            Text(
                                              item.meaning,
                                              style: GoogleFonts.sarabun(
                                                color: AppColors.textGray,
                                                fontSize: 13,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(width: 12),
                                      // Professional Action Indicator
                                      Container(
                                        padding: const EdgeInsets.all(6),
                                        decoration: BoxDecoration(
                                          color: AppColors.primary.withOpacity(
                                            0.1,
                                          ),
                                          shape: BoxShape.circle,
                                          border: Border.all(
                                            color: AppColors.primary
                                                .withOpacity(0.2),
                                            width: 1,
                                          ),
                                        ),
                                        child: Icon(
                                          Icons.chevron_right_rounded,
                                          size: 20,
                                          color: AppColors.primary.withOpacity(
                                            0.8,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            );
                          }).toList(),
                        ),
                    ],
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }

  Widget buildTextField({
    required TextEditingController controller,
    required String hint,
    required IconData icon,
    TextInputAction textInputAction = TextInputAction.done,
    ValueChanged<String>? onSubmitted,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.bgDarker, // ใช้สีพื้นหลังครีมทองที่เข้มขึ้น
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: AppColors.glassBorder, // ใช้สี border จากธีมใหม่
          width: 1.5,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 6,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: TextField(
        controller: controller,
        style: const TextStyle(
          color: Color(0xFFFFD700), // สีเหลืองทองสว่าง
          fontSize: 16,
          fontWeight: FontWeight.w600, // หนาขึ้น
        ),
        textInputAction: textInputAction,
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: TextStyle(
            color: Color(0xFFFFE992), // สีเหลืองทองอ่อนสำหรับ hint text
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
          prefixIcon: Icon(icon, color: Colors.white.withOpacity(0.5)),
          suffixIcon: ValueListenableBuilder<TextEditingValue>(
            valueListenable: controller,
            builder: (context, value, child) {
              if (value.text.isEmpty) return const SizedBox.shrink();
              return IconButton(
                icon: Icon(
                  Icons.close,
                  color: Colors.white.withOpacity(0.5),
                  size: 20,
                ),
                onPressed: () => controller.clear(),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
                splashRadius: 20,
              );
            },
          ),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 14,
          ),
        ),
        onSubmitted: onSubmitted,
      ),
    );
  }

  Widget buildDropdown() {
    final bool hasSelectedDay = _selectedDay != null;

    return Container(
      padding: const EdgeInsets.fromLTRB(14, 8, 14, 8),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: hasSelectedDay
              ? [const Color(0xFFFFF8DF), Colors.white, const Color(0xFFFDF2FF)]
              : [Colors.white, const Color(0xFFFFFCF2)],
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: hasSelectedDay
              ? const Color(0xFFD4AF37)
              : AppColors.secondary.withOpacity(0.18),
          width: hasSelectedDay ? 1.8 : 1.4,
        ),
        boxShadow: [
          BoxShadow(
            color: hasSelectedDay
                ? const Color(0xFFD4AF37).withOpacity(0.14)
                : Colors.black.withOpacity(0.04),
            blurRadius: hasSelectedDay ? 16 : 10,
            offset: const Offset(0, 6),
          ),
          if (hasSelectedDay)
            BoxShadow(
              color: const Color(0xFF8B5CF6).withOpacity(0.08),
              blurRadius: 18,
              spreadRadius: 1,
              offset: const Offset(0, 8),
            ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 30,
            height: 30,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: LinearGradient(
                colors: hasSelectedDay
                    ? [const Color(0xFFF7D046), const Color(0xFFD4AF37)]
                    : [const Color(0xFFE6FBF7), const Color(0xFFD7F4EE)],
              ),
              boxShadow: [
                BoxShadow(
                  color:
                      (hasSelectedDay
                              ? const Color(0xFFD4AF37)
                              : AppColors.primary)
                          .withOpacity(0.18),
                  blurRadius: 10,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: Icon(
              hasSelectedDay
                  ? Icons.auto_awesome_rounded
                  : Icons.calendar_month_rounded,
              color: hasSelectedDay
                  ? const Color(0xFF6B4E16)
                  : AppColors.primary,
              size: 16,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: DropdownButtonHideUnderline(
              child: DropdownButton<String>(
                value: _selectedDay,
                menuWidth: MediaQuery.of(context).size.width - 48,
                hint: Text(
                  "เลือกวันเกิด...",
                  style: GoogleFonts.prompt(
                    color: AppColors.textGray.withOpacity(0.62),
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                style: GoogleFonts.prompt(
                  color: AppColors.textLight,
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                ),
                dropdownColor: Colors.white,
                isExpanded: true,
                icon: Container(
                  width: 26,
                  height: 26,
                  decoration: BoxDecoration(
                    color: hasSelectedDay
                        ? const Color(0xFFFFF4CC)
                        : AppColors.secondary.withOpacity(0.08),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Icon(
                    Icons.keyboard_arrow_down_rounded,
                    color: hasSelectedDay
                        ? const Color(0xFFC58B00)
                        : AppColors.secondary,
                    size: 20,
                  ),
                ),
                selectedItemBuilder: (context) {
                  return _days.map((day) {
                    return Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        _dayLabels[day]!,
                        style: GoogleFonts.prompt(
                          color: AppColors.textLight,
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    );
                  }).toList();
                },
                items: _days.map((String day) {
                  return DropdownMenuItem<String>(
                    value: day,
                    child: Text(
                      _dayLabels[day]!,
                      style: GoogleFonts.prompt(
                        color: AppColors.textLight,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  );
                }).toList(),
                onChanged: (String? newValue) {
                  setState(() {
                    _selectedDay = newValue;
                    if (newValue == null) {
                      _filterKaki = false;
                    } else {
                      _filterKaki = true;
                    }
                  });
                  _refreshResultsKeepingStep2Anchor();
                },
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget buildFilterChipsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // --- VIP ZONE CARD ---
        Stack(
          clipBehavior: Clip.none,
          children: [
            Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(
                16,
                36,
                16,
                16,
              ), // Increased top padding from 28 to 36
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    const Color(0xFFF8F3FF),
                    const Color(0xFFF4EEFF),
                    const Color(0xFFFFF9EC),
                  ],
                ),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(
                  color: const Color(0xFF8B5CF6).withOpacity(0.2),
                  width: 1.5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF8B5CF6).withOpacity(0.05),
                    blurRadius: 18,
                    spreadRadius: 2,
                    offset: const Offset(0, 10),
                  ),
                  BoxShadow(
                    color: const Color(0xFFD4AF37).withOpacity(0.05),
                    blurRadius: 22,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 10,
                    runSpacing: 14, // Increased from 10
                    children: [
                      FilterChipWidget(
                        label: "เลขศาสตร์ดี",
                        icon: Icons.auto_awesome_rounded,
                        isActive: _filterSat,
                        activeColor: const Color(0xFF8B5CF6),
                        activeTextColor: Colors.white,
                        onTap: () async {
                          setState(() => _filterSat = !_filterSat);
                          _refreshResultsKeepingStep2Anchor();
                        },
                      ),
                      FilterChipWidget(
                        label: "พลังเงาดี",
                        icon: Icons.shield_rounded,
                        isActive: _filterSha,
                        activeColor: const Color(0xFF8B5CF6),
                        activeTextColor: Colors.white,
                        onTap: () async {
                          setState(() => _filterSha = !_filterSha);
                          _refreshResultsKeepingStep2Anchor();
                        },
                      ),
                      FilterChipWidget(
                        label: "รวมให้เป็น \"ชื่อดี\"",
                        icon: Icons.people_rounded,
                        isActive: _similarMode,
                        activeColor: const Color(0xFF8B5CF6),
                        activeTextColor: Colors.white,
                        onTap: () async {
                          if (!_similarMode &&
                              _keywordController.text.trim().isEmpty) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(
                                  "โปรดระบุชื่อที่ต้องการใช้รวมชื่อก่อนครับ",
                                  style: GoogleFonts.prompt(),
                                ),
                                backgroundColor: Colors.amber[800],
                                behavior: SnackBarBehavior.floating,
                              ),
                            );
                            return;
                          }

                          setState(() => _similarMode = !_similarMode);
                          _refreshResultsKeepingStep2Anchor();
                        },
                      ),
                    ],
                  ),
                ],
              ),
            ),
            // VIP Badge
            Positioned(
              top:
                  -10, // Slightly more offset from -8 to -10 for "floating" feel
              left: 16,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF2E7D5A), Color(0xFF1F5F45)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFF1F5F45).withOpacity(0.28),
                      blurRadius: 10,
                      offset: const Offset(0, 3),
                    ),
                  ],
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.stars_rounded,
                      size: 14,
                      color: Colors.white,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      "หาชื่อตามตำราที่ดีที่สุดจาก 3 แสนรายชื่อ",
                      style: GoogleFonts.prompt(
                        color: Colors.white,
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  bool isScoreTrulyGood(int score, bool apiGood) {
    if (score < 100) return apiGood;
    String s = score.toString();
    if (s.length < 3) return apiGood;

    // For split scores, we check if ALL resulting pairs are lucky
    int n1 = int.tryParse(s.substring(0, 2)) ?? 0;
    int n2 = int.tryParse(s.substring(1, 3)) ?? 0;
    return isLuckyNumber(n1) && isLuckyNumber(n2);
  }

  bool isLuckyNumber(int n) {
    const lucky = {
      2,
      4,
      5,
      6,
      9,
      14,
      15,
      19,
      23,
      24,
      32,
      36,
      40,
      41,
      42,
      44,
      45,
      46,
      50,
      51,
      54,
      55,
      56,
      59,
      63,
      64,
      65,
      69,
      79,
      89,
      90,
      91,
      92,
      93,
      94,
      95,
      96,
      97,
      98,
      99,
    };
    return lucky.contains(n);
  }

  Widget buildAnalysisScoreWithSmart(String label, int score, bool isGood) {
    if (score >= 100) {
      String s = score.toString();
      if (s.length >= 3) {
        String p1 = s.substring(0, 2);
        String p2 = s.substring(1, 3);

        int n1 = int.tryParse(p1) ?? 0;
        int n2 = int.tryParse(p2) ?? 0;

        return Column(
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                buildAnalysisScoreCircle(p1, isLuckyNumber(n1)),
                const SizedBox(width: 4),
                buildAnalysisScoreCircle(p2, isLuckyNumber(n2)),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              label,
              style: GoogleFonts.prompt(
                color: const Color(0xFF3D2600),
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        );
      }
    }
    return buildAnalysisScore(label, score, isGood);
  }

  Widget buildAnalysisScoreCircle(String score, bool isGood) {
    Color lightColor = isGood
        ? const Color(0xFF4ADE80)
        : const Color(0xFFF87171);
    Color darkColor = isGood
        ? const Color(0xFF16A34A)
        : const Color(0xFFDC2626);

    return InkWell(
      onTap: () {
        showNumberMeaningDialog(context, score, isGood);
      },
      borderRadius: BorderRadius.circular(50),
      child: Container(
        width: 36,
        height: 36,
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
              color: Colors.black.withOpacity(0.1),
              blurRadius: 4,
              offset: const Offset(0, 3),
            ),
          ],
          border: Border.all(color: Colors.black.withOpacity(0.05), width: 1),
        ),
        child: Text(
          score,
          style: const TextStyle(
            color: AppColors.textLight,
            fontWeight: FontWeight.bold,
            fontSize: 14,
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
    );
  }

  Widget buildHighlightedName(NameAnalysisResult result) {
    final bool isGold = result.isSatGood && result.isShaGood;
    final bool hasKaki = result.characters.any((c) => c.isKaki);

    // Check if we need to show the Kaki warning text below the name
    // If the name has Kaki, we show the name with red highlights.
    // The user might also want a small text saying "Contains Misfortune"?
    // But for now, let's just show the name.

    final textStyle = GoogleFonts.sarabun(
      fontSize: 22,
      fontWeight: FontWeight.bold,
      color: isGold ? const Color(0xFFFFD700) : AppColors.textLight,
      height: 1.3,
    );

    Widget nameWidget;
    if (!hasKaki) {
      nameWidget = Text(result.name, style: textStyle);
    } else {
      final tp = TextPainter(
        text: TextSpan(text: result.name, style: textStyle),
        textDirection: TextDirection.ltr,
      )..layout();

      nameWidget = CustomPaint(
        size: Size(tp.width, 30), // Adjust height to fit font
        painter: _ThaiHighlightPainter(
          highlights: result.characters,
          baseStyle: textStyle,
          isGold: isGold,
        ),
      );
    }

    if (isGold && !hasKaki) {
      return ShimmeringGoldText(child: nameWidget);
    }
    return nameWidget;
  }

  Widget buildKakiBadgeForCard(List<CharHighlight> chars) {
    if (chars.isEmpty) return const SizedBox.shrink();

    final kakiChars = chars
        .where((c) => c.isKaki)
        .map((c) => c.char)
        .toSet()
        .join(", ");
    final hasKaki = kakiChars.isNotEmpty;

    if (!hasKaki) {
      return Container(
        // margin: const EdgeInsets.only(top: 6, bottom: 2),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFF10B981).withOpacity(0.15),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: const Color(0xFF10B981).withOpacity(0.4)),
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
                fontSize: 11,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      );
    } else {
      return Container(
        // margin: const EdgeInsets.only(top: 6, bottom: 2),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFFEF4444).withOpacity(0.15),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: const Color(0xFFEF4444).withOpacity(0.4)),
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
              "กาลกิณี: $kakiChars",
              style: GoogleFonts.prompt(
                color: const Color(0xFFF87171),
                fontSize: 11,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      );
    }
  }

  Widget buildAnalysisScore(String label, int score, bool isGood) {
    return Column(
      children: [
        buildAnalysisScoreCircle(score.toString(), isGood),
        const SizedBox(height: 6),
        Text(
          label,
          style: GoogleFonts.prompt(
            color: const Color(0xFF3D2600),
            fontSize: 13,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }

  void showNumberMeaningDialog(
    BuildContext context,
    String number,
    bool isGood,
  ) {
    showDialog(
      context: context,
      builder: (context) {
        return FutureBuilder<NumberMeaningResult?>(
          future: _apiService.getNumberMeaning(
            number,
          ), // Use _apiService instance
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const AlertDialog(
                backgroundColor: AppColors.bgDark,
                content: SizedBox(
                  height: 180,
                  child: Center(
                    child: MagicLoadingView(
                      height: 110,
                      message: "กำลังร่ายมนต์ดึงข้อมูล...",
                    ),
                  ),
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
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
                side: const BorderSide(color: AppColors.primary, width: 2),
              ),
              title: Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: isGood
                          ? AppColors.success
                          : const Color(0xFFEF4444),
                      shape: BoxShape.circle,
                    ),
                    child: Text(
                      number,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      data.description,
                      style: const TextStyle(
                        color: AppColors.textLight,
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              content: SingleChildScrollView(
                child: Text(
                  data.detail.replaceAll("\\n", "\n"),
                  style: const TextStyle(
                    color: AppColors.textGray,
                    height: 1.6,
                    fontSize: 14,
                  ),
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
      },
    );
  }

  Widget buildToggle(
    String label,
    bool value,
    Function(bool) onChanged, {
    Color? activeColor,
  }) {
    return Container(
      padding: const EdgeInsets.only(right: 12),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Transform.scale(
            scale:
                0.75, // Standard switch is a bit large, 0.75-0.8 is better for UI labels
            child: Switch.adaptive(
              value: value,
              onChanged: onChanged,
              activeColor: activeColor ?? AppColors.primary,
              activeTrackColor: (activeColor ?? AppColors.primary).withOpacity(
                0.3,
              ),
              inactiveThumbColor: Colors.white70,
              inactiveTrackColor: Colors.white.withOpacity(0.1),
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
          ),
          Text(
            label,
            style: GoogleFonts.prompt(
              color: value ? AppColors.textLight : AppColors.textGray,
              fontSize: 13,
              fontWeight: value ? FontWeight.w600 : FontWeight.normal,
            ),
          ),
        ],
      ),
    );
  }

  Widget buildFooter() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 40, horizontal: 24),
      decoration: const BoxDecoration(
        color: AppColors.bgDark, // เปลี่ยนมาใช้สีเดียวกับพื้นหลังแอป
      ),
      child: Column(
        children: [
          Text(
            "ความรู้เรื่องชื่อและเลขศาสตร์",
            style: GoogleFonts.prompt(
              color: AppColors.textGray.withOpacity(0.6),
              fontSize: 14,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 20),
          Wrap(
            alignment: WrapAlignment.center,
            spacing: 12,
            runSpacing: 12,
            children: [
              buildFooterLink("เลขศาสตร์ & พลังเงา", 0),
              buildFooterLink("กาลกิณี", 1),
              buildFooterLink("ระบบอัจฉริยะ (AI)", 2),
              buildFooterLink("การจัดอันดับชื่อ", 3),
            ],
          ),
          const SizedBox(height: 20),
          Text(
            "วิเคราะห์จากชื่อจริง +3 แสนชื่อ",
            textAlign: TextAlign.center,
            style: GoogleFonts.prompt(
              color: AppColors.textLight.withOpacity(0.9),
              fontSize: 13,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 40),
          Text(
            "© 2026 Naming App. All rights reserved.",
            style: GoogleFonts.sarabun(
              color: AppColors.textGray.withOpacity(0.3),
              fontSize: 10,
            ),
          ),
        ],
      ),
    );
  }

  Widget buildFooterLink(String label, int index) {
    return InkWell(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => InformationScreen(initialTabIndex: index),
          ),
        );
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
        decoration: BoxDecoration(
          color: AppColors.bgDarker,
          border: Border.all(color: AppColors.secondary.withOpacity(0.2)),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Text(
          label,
          style: GoogleFonts.prompt(
            color: AppColors.secondary,
            fontSize: 12,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }

  Widget buildRankingExplanation() {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => InformationScreen(initialTabIndex: 2),
          ),
        );
      },
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFF1E293B).withOpacity(0.5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.white.withOpacity(0.1)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(
                  Icons.auto_graph_rounded,
                  color: AppColors.accent,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Text(
                  "การจัดอันดับชื่อ",
                  style: GoogleFonts.prompt(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              "ระบบใช้ปัญญาประดิษฐ์ระดับแนวหน้า ทำงานผ่าน Semantic Search และ Embedding Vectors เพื่อวิเคราะห์ความหมายที่ลึกซึ้ง พร้อมผสานหลักความเป็นมงคล (เลขศาสตร์และพลังเงา) และตำราโบราณ (คัดกาลกิณีตามวันเกิด) เพื่อมอบรายชื่อที่ตรงใจและเป็นมงคลที่สุดสำหรับคุณ",
              style: GoogleFonts.sarabun(
                color: Colors.white.withOpacity(0.7),
                fontSize: 12,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Text(
                  "อ่านเพิ่มเติม",
                  style: GoogleFonts.prompt(
                    color: AppColors.accent,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(width: 4),
                const Icon(
                  Icons.arrow_forward_rounded,
                  size: 12,
                  color: AppColors.accent,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void showRootWordDialog({
    required String name,
    required int satSum,
    required int shaSum,
    required bool isSatGood,
    required bool isShaGood,
    String? meaning,
  }) {
    final rootFuture = ApiService().getNameRoot(name, meaning: meaning);
    // Always fetch meaning independently so we can save it reliably
    final meaningFuture = (meaning != null && meaning.trim().isNotEmpty)
        ? Future.value(meaning)
        : ApiService().getNameMeaning(name);
    String? resolvedMeaning = meaning; // Will be updated when future completes
    meaningFuture.then((val) {
      resolvedMeaning = val;
    });
    bool didSave = false;

    showDialog(
      context: context,
      builder: (dialogContext) {
        bool dialogSaving = false;
        bool dialogSaved =
            false; // We don't easily know if it's saved from here without a global state or check

        return StatefulBuilder(
          builder: (dialogContext, setDialogState) {
            return FutureBuilder<NameRootResult?>(
              future: rootFuture,
              builder: (context, snapshot) {
                Widget content;
                NameRootResult? rootData;

                if (snapshot.connectionState == ConnectionState.waiting) {
                  content = SizedBox(
                    height: 120,
                    child: Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          SizedBox(
                            width: 28,
                            height: 28,
                            child: CircularProgressIndicator(
                              strokeWidth: 3,
                              valueColor: AlwaysStoppedAnimation<Color>(
                                AppColors.primary,
                              ),
                              backgroundColor: AppColors.primary.withOpacity(
                                0.12,
                              ),
                            ),
                          ),
                          const SizedBox(height: 14),
                          Text(
                            "กำลังวิเคราะห์รากศัพท์มงคล...",
                            style: GoogleFonts.sarabun(
                              color: AppColors.textGray,
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
                  );
                } else if (snapshot.hasError || snapshot.data == null) {
                  content = const Text(
                    "ไม่สามารถดึงข้อมูลรากศัพท์ได้",
                    style: TextStyle(color: AppColors.textGray),
                  );
                } else {
                  rootData = snapshot.data!;
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
                              color: AppColors.textGray,
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
                              color: AppColors.textGray.withOpacity(0.1),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }

                return AlertDialog(
                  backgroundColor: AppColors.bgDark,
                  shadowColor: AppColors.primary.withOpacity(0.1),
                  surfaceTintColor: Colors.transparent,
                  elevation: 20,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24),
                    side: const BorderSide(color: AppColors.primary, width: 2),
                  ),
                  title: Column(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.primary.withOpacity(0.1),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.notes_rounded,
                          color: AppColors.primary,
                          size: 32,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        "การวิเคราะห์รากศัพท์",
                        style: GoogleFonts.sarabun(
                          color: AppColors.textGray.withOpacity(0.7),
                          fontSize: 12,
                          letterSpacing: 2.0,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        name,
                        style: GoogleFonts.prompt(
                          color: AppColors.textLight,
                          fontWeight: FontWeight.w800,
                          fontSize: 32,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Divider(
                        color: AppColors.textGray.withOpacity(0.1),
                        height: 1,
                        thickness: 1,
                      ),
                    ],
                  ),
                  content: content,
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(dialogContext),
                      child: Text(
                        "ปิด",
                        style: GoogleFonts.prompt(
                          color: AppColors.textGray,
                          fontWeight: FontWeight.w600,
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
                                    "name": name,
                                    "sat_sum": satSum,
                                    "sha_sum": shaSum,
                                    "is_sat_good": isSatGood,
                                    "is_sha_good": isShaGood,
                                    "root_word": rootData!.rootWord,
                                    "meaning": resolvedMeaning ?? "",
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
                                    Future.delayed(
                                      const Duration(milliseconds: 1000),
                                      () {
                                        if (Navigator.canPop(dialogContext)) {
                                          Navigator.pop(dialogContext);
                                        }
                                      },
                                    );
                                    return;
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
                                  color: AppColors.textLight,
                                ),
                              )
                            : const Icon(
                                Icons.favorite_rounded,
                                size: 18,
                                color: Color(0xFFD946EF),
                              ),
                        label: const Text(
                          "บันทึกชื่อนี้",
                          style: TextStyle(color: Color(0xFF6B21A8)),
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.primary.withOpacity(0.15),
                          foregroundColor: AppColors.textLight,
                          elevation: 0,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                            side: BorderSide(
                              color: AppColors.primary.withOpacity(0.3),
                            ),
                          ),
                        ),
                      ),
                    if (dialogSaved)
                      ElevatedButton.icon(
                        onPressed: null,
                        icon: const Icon(
                          Icons.check_circle,
                          size: 18,
                          color: AppColors.textLight,
                        ),
                        label: const Text(
                          "บันทึกสำเร็จ",
                          style: TextStyle(color: AppColors.textLight),
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.success,
                          disabledBackgroundColor: AppColors.success,
                          disabledForegroundColor: AppColors.textLight,
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
      if (didSave && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("บันทึกชื่อ $name แล้ว"),
            backgroundColor: AppColors.success,
          ),
        );
      }
    });
  }

  void scrollToStep2() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final context = _step2Key.currentContext;
      if (context != null) {
        Scrollable.ensureVisible(
          context,
          duration: const Duration(milliseconds: 600),
          curve: Curves.easeInOutCubic,
          alignment: 0.0, // Scroll until the widget is at the top
        );
      }
    });
  }

  void _refreshResultsKeepingStep2Anchor() {
    final hasQuery = _keywordController.text.trim().isNotEmpty;
    if (hasQuery) {
      final double? lockedOffset = _scrollController.hasClients
          ? _scrollController.offset
          : null;

      _search(
        scrollToResults: false,
        showInputSnack: false,
        reloadSelectedName: false,
      ).then((_) {
        if (!mounted || lockedOffset == null || !_scrollController.hasClients) {
          return;
        }
        final maxScroll = _scrollController.position.maxScrollExtent;
        final target = lockedOffset.clamp(0.0, maxScroll);
        if ((_scrollController.offset - target).abs() > 1) {
          _scrollController.jumpTo(target);
        }
      });
    }
  }

  Widget buildMagicRankingHeader() {
    return Column(
      key: _step2Key,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              width: 32,
              height: 32,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF0F9D7A), Color(0xFF13B38A)],
                ),
                borderRadius: BorderRadius.circular(10),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF0F9D7A).withOpacity(0.3),
                    blurRadius: 10,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: const Text(
                '2',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Text(
              "จัดอันดับตามเงื่อนไขตำราตั้งชื่อ",
              style: GoogleFonts.prompt(
                color: AppColors.textLight,
                fontSize: 18,
                fontWeight: FontWeight.w800,
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        const _MagicSubtitleAnimation(),
      ],
    );
  }
}

class _MagicSubtitleAnimation extends StatefulWidget {
  const _MagicSubtitleAnimation();

  @override
  State<_MagicSubtitleAnimation> createState() =>
      _MagicSubtitleAnimationState();
}

class _MagicSubtitleAnimationState extends State<_MagicSubtitleAnimation>
    with SingleTickerProviderStateMixin {
  late AnimationController controller;
  late Animation<double> shimmer;

  @override
  void initState() {
    super.initState();
    controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2500),
    )..repeat();
    shimmer = Tween<double>(begin: -1.0, end: 2.0).animate(controller);
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: shimmer,
      builder: (context, child) {
        return ShaderMask(
          shaderCallback: (bounds) {
            return LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                const Color(0xFF0B8F70),
                const Color(0xFFD97706),
                const Color(0xFF0B8F70),
              ],
              stops: [
                (shimmer.value - 0.3).clamp(0.0, 1.0),
                shimmer.value.clamp(0.0, 1.0),
                (shimmer.value + 0.3).clamp(0.0, 1.0),
              ],
            ).createShader(bounds);
          },
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: Row(
              children: [
                const SizedBox(width: 38),
                const Icon(Icons.auto_awesome, size: 12, color: Colors.white),
                const SizedBox(width: 4),
                Text(
                  "ความมหัศจรรย์เกิดขึ้นที่นี่",
                  style: GoogleFonts.sarabun(
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.5,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
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
  final Set<int> thaiCombiningMarks = {
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

  bool isCombining(String char) {
    if (char.isEmpty) return false;
    int code = char.runes.first;
    return thaiCombiningMarks.contains(code);
  }

  void draw(Canvas canvas, String text, Color color, double x, double y) {
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
        baseStyle.color ??
        (isGold ? const Color(0xFFFFD700) : AppColors.textLight);

    // 1. Group into Thai Grapheme Clusters to avoid shaping breaks (dotted circles)
    final clusters = <List<CharHighlight>>[];
    for (var h in highlights) {
      if (clusters.isEmpty || !isCombining(h.char)) {
        clusters.add([h]);
      } else {
        clusters.last.add(h);
      }
    }

    double x = 0;
    // Calculate a common baseline for the entire text to ensure vertical alignment
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

      // Measure the full cluster
      final clusterTp = TextPainter(
        text: TextSpan(text: clusterText, style: baseStyle),
        textDirection: TextDirection.ltr,
      )..layout();
      final clusterBaseline = clusterTp.computeDistanceToActualBaseline(
        TextBaseline.alphabetic,
      );

      final targetBaselineY = verticalOffset + commonBaseline;

      // 2. Logic to paint parts of cluster with different colors
      bool hasMixedColors = cluster.any((e) => e.isKaki != base.isKaki);

      if (base.isKaki) {
        draw(canvas, clusterText, red, x, targetBaselineY - clusterBaseline);
      } else if (!hasMixedColors) {
        draw(
          canvas,
          clusterText,
          baseColor,
          x,
          targetBaselineY - clusterBaseline,
        );
      } else {
        // Mixed Case (e.g. White Base + Red Vowel)
        // Draw layers from full cluster down to base to ensure proper stacking
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

          draw(canvas, subText, color, x, targetBaselineY - subBaseline);
        }
      }

      x += clusterTp.width;
    }
  }

  @override
  bool shouldRepaint(_ThaiHighlightPainter oldDelegate) => true;
}

// ─────────────────────────────────────────────────────────────────────────────
// _MagicSummaryWrapper: แอนิเมชันสำหรับกรอบสรุปคะแนนเมื่อได้คู่ "ดีเยี่ยม"
// ─────────────────────────────────────────────────────────────────────────────
class _MagicSummaryWrapper extends StatefulWidget {
  final Widget child;
  final bool isActive;
  final Color color;

  const _MagicSummaryWrapper({
    required this.child,
    required this.isActive,
    required this.color,
  });

  @override
  State<_MagicSummaryWrapper> createState() => _MagicSummaryWrapperState();
}

class _MagicSummaryWrapperState extends State<_MagicSummaryWrapper>
    with TickerProviderStateMixin {
  late AnimationController glowController;
  late AnimationController shimmerController;
  late AnimationController particleController;
  late Animation<double> glowAnim;
  late Animation<double> shimmerAnim;
  late List<_MagicSummaryParticle> particles0;

  @override
  void initState() {
    super.initState();
    glowController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    );
    shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2200),
    );
    particleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3000),
    );

    glowAnim = CurvedAnimation(parent: glowController, curve: Curves.easeInOut);
    shimmerAnim = shimmerController;

    if (widget.isActive) {
      startAnimations();
    }

    final rng = math.Random(42);
    particles0 = List.generate(
      10,
      (i) => _MagicSummaryParticle(
        x: rng.nextDouble(),
        y: rng.nextDouble(),
        size: 1.5 + rng.nextDouble() * 2.5,
        speed: 0.2 + rng.nextDouble() * 0.6,
        phase: rng.nextDouble(),
      ),
    );
  }

  void startAnimations() {
    glowController.repeat(reverse: true);
    shimmerController.repeat();
    particleController.repeat();
  }

  void stopAnimations() {
    glowController.stop();
    shimmerController.stop();
    particleController.stop();
  }

  @override
  void didUpdateWidget(_MagicSummaryWrapper oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isActive != oldWidget.isActive) {
      if (widget.isActive) {
        startAnimations();
      } else {
        stopAnimations();
      }
    }
  }

  @override
  void dispose() {
    glowController.dispose();
    shimmerController.dispose();
    particleController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.isActive) {
      return Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
        decoration: BoxDecoration(
          color: const Color(0xFFFFF9E6).withOpacity(0.5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.accent.withOpacity(0.1)),
        ),
        child: widget.child,
      );
    }

    final color = widget.color;
    return AnimatedBuilder(
      animation: Listenable.merge([glowAnim, shimmerAnim, particleController]),
      builder: (context, _) {
        final glow = glowAnim.value;
        return Container(
          decoration: BoxDecoration(
            color: AppColors.bgDarker, // Elegant themed background Base
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: color.withOpacity(0.3), width: 1.5),
            gradient: LinearGradient(
              colors: [
                color.withOpacity(0.05 + glow * 0.1),
                AppColors.accent.withOpacity(0.03 + glow * 0.05),
                color.withOpacity(0.02),
              ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            boxShadow: [
              BoxShadow(
                color: color.withOpacity(0.1 + glow * 0.15),
                blurRadius: 10 + glow * 10,
                spreadRadius: glow * 1,
              ),
            ],
          ),
          child: Stack(
            children: [
              // Shimmer overlay
              Positioned.fill(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: CustomPaint(
                    painter: _MagicSummaryShimmerPainter(
                      progress: shimmerAnim.value,
                      color: color,
                    ),
                  ),
                ),
              ),
              // Particle overlay
              Positioned.fill(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: CustomPaint(
                    painter: _MagicSummaryParticlePainter(
                      particles: particles0,
                      progress: particleController.value,
                      color: color,
                    ),
                  ),
                ),
              ),
              // Border & Content
              Container(
                padding: const EdgeInsets.symmetric(
                  vertical: 12,
                  horizontal: 16,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: color.withOpacity(0.3 + glow * 0.4),
                    width: 1.5,
                  ),
                ),
                child: widget.child,
              ),
            ],
          ),
        );
      },
    );
  }
}

class _MagicSummaryParticle {
  final double x, y, size, speed, phase;
  _MagicSummaryParticle({
    required this.x,
    required this.y,
    required this.size,
    required this.speed,
    required this.phase,
  });
}

class _MagicSummaryShimmerPainter extends CustomPainter {
  final double progress;
  final Color color;
  _MagicSummaryShimmerPainter({required this.progress, required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final sweepX = -size.width + progress * size.width * 2.5;
    final paint = Paint()
      ..shader = LinearGradient(
        colors: [
          Colors.transparent,
          color.withOpacity(0.05),
          Colors.white.withOpacity(0.1),
          color.withOpacity(0.05),
          Colors.transparent,
        ],
        stops: const [0.0, 0.3, 0.5, 0.7, 1.0],
        begin: Alignment.centerLeft,
        end: Alignment.centerRight,
        transform: GradientRotation(math.pi / 6),
      ).createShader(Rect.fromLTWH(sweepX, 0, size.width * 0.8, size.height));
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height), paint);
  }

  @override
  bool shouldRepaint(_MagicSummaryShimmerPainter old) =>
      old.progress != progress;
}

class _MagicSummaryParticlePainter extends CustomPainter {
  final List<_MagicSummaryParticle> particles;
  final double progress;
  final Color color;
  _MagicSummaryParticlePainter({
    required this.particles,
    required this.progress,
    required this.color,
  });

  @override
  void paint(Canvas canvas, Size size) {
    for (final p in particles) {
      final t = (progress * p.speed + p.phase) % 1.0;
      final px = p.x * size.width + math.sin(t * math.pi * 2 + p.phase * 5) * 8;
      final py = size.height - (t * (size.height + 20));
      final opacity = math.sin(t * math.pi).clamp(0.0, 1.0);
      final paint = Paint()
        ..color = color.withOpacity(opacity * 0.6)
        ..style = PaintingStyle.fill;

      final path = Path();
      final center = Offset(px, py);
      final s = p.size * opacity;
      for (int i = 0; i < 4; i++) {
        final angle = i * math.pi / 2;
        final tip = Offset(
          center.dx + math.cos(angle) * s * 2,
          center.dy + math.sin(angle) * s * 2,
        );
        final side1 = Offset(
          center.dx + math.cos(angle + math.pi / 2) * s * 0.4,
          center.dy + math.sin(angle + math.pi / 2) * s * 0.4,
        );
        final side2 = Offset(
          center.dx + math.cos(angle - math.pi / 2) * s * 0.4,
          center.dy + math.sin(angle - math.pi / 2) * s * 0.4,
        );
        if (i == 0) path.moveTo(side1.dx, side1.dy);
        path.lineTo(tip.dx, tip.dy);
        path.lineTo(side2.dx, side2.dy);
        path.lineTo(center.dx, center.dy);
      }
      path.close();
      canvas.drawPath(path, paint);
    }
  }

  @override
  bool shouldRepaint(_MagicSummaryParticlePainter old) =>
      old.progress != progress;
}

/// A premium, sparkling gold heart icon for the saved names button
class SparklingGoldHeart extends StatefulWidget {
  const SparklingGoldHeart({super.key});

  @override
  State<SparklingGoldHeart> createState() => _SparklingGoldHeartState();
}

class _SparklingGoldHeartState extends State<SparklingGoldHeart>
    with SingleTickerProviderStateMixin {
  late AnimationController controller;

  @override
  void initState() {
    super.initState();
    controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2000),
    )..repeat();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const Color heartColor = Color(0xFFD946EF);

    return AnimatedBuilder(
      animation: controller,
      builder: (context, child) {
        return SizedBox(
          width: 32,
          height: 32,
          child: Stack(
            alignment: Alignment.center,
            clipBehavior: Clip.none, // Allow sparkles to fly slightly out
            children: [
              // Subtle background pulse
              Container(
                width: 24,
                height: 24,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: heartColor.withOpacity(
                        0.15 + 0.25 * math.sin(controller.value * math.pi),
                      ),
                      blurRadius: 10,
                      spreadRadius: 1,
                    ),
                  ],
                ),
              ),
              // Main Heart (Gold)
              const Icon(Icons.favorite_rounded, size: 24, color: heartColor),
              // Sparkles - improved visibility and movement
              ...List.generate(3, (index) {
                final progress = (controller.value + (index / 3)) % 1.0;
                final opacity = math.sin(progress * math.pi);
                final scale = 0.4 + (0.8 * opacity);
                // Rotate a full 360 degrees (2 * pi) for a perfect loop without jumping
                final angle =
                    (index * (2 * math.pi / 3)) +
                    (controller.value * 2 * math.pi);
                final distance = 8.0 + (6.0 * progress);

                return Positioned(
                  left: 16 + math.cos(angle) * distance - 5,
                  top: 16 + math.sin(angle) * distance - 5,
                  child: Transform.scale(
                    scale: scale,
                    child: Opacity(
                      opacity: opacity,
                      child: const Icon(
                        Icons.auto_awesome,
                        size: 10, // Increased size
                        color: Colors.white,
                      ),
                    ),
                  ),
                );
              }),
            ],
          ),
        );
      },
    );
  }
}

class _PulsingBar extends StatefulWidget {
  const _PulsingBar();

  @override
  State<_PulsingBar> createState() => _PulsingBarState();
}

class _PulsingBarState extends State<_PulsingBar>
    with SingleTickerProviderStateMixin {
  late AnimationController controller;

  @override
  void initState() {
    super.initState();
    controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 6),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, child) {
        final double pulse = 0.8 + (controller.value * 0.4);
        return Container(
          width: 4,
          height: 18,
          decoration: BoxDecoration(
            color: AppColors.accent,
            borderRadius: BorderRadius.circular(2),
            boxShadow: [
              BoxShadow(
                color: AppColors.accent.withOpacity(0.6 * controller.value),
                blurRadius: 10 * controller.value,
                spreadRadius: 2 * controller.value,
              ),
            ],
          ),
        );
      },
    );
  }
}
