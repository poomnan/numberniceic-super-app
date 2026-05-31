import 'dart:async';
import 'dart:io' show Platform;
import 'dart:math' as math;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:google_fonts/google_fonts.dart';
import '../models/name_intent_model.dart';
import '../models/name_model.dart';
import '../models/name_root_result.dart';
import '../models/number_meaning_model.dart';
import '../services/api_service.dart';
import '../services/premium_manager.dart';
import '../utils/colors.dart';
import '../utils/numerology_format.dart';
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

enum SpeechButtonVariant { primary, secondary }

enum _TtsStatus { unavailable, noThaiVoice, ready }

class _NamingScreenState extends State<NamingScreen>
    with TickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  final FlutterTts _flutterTts = FlutterTts();
  final TextEditingController _keywordController = TextEditingController();
  String? _selectedNameMeaningName;
  String? _selectedNameMeaning;
  bool _isLoadingSelectedNameMeaning = false;
  NameAnalysisResult? _selectedNameAnalysis;
  bool _hasRankableNameTemplate = false;
  int? _selectedCelebrityIndex; // Track which celebrity avatar is selected
  int? _selectedExampleIndex;
  String? _selectedExampleText;
  String? _selectedDay;
  bool _filterSat = false;
  bool _filterSha = true;
  bool _filterKaki = false;
  List<MobileNameResult> _results = [];
  List<Map<String, dynamic>> _celebrities = [];
  bool _isLoading = false;
  bool _isPivotingIdea = false;
  String? _errorMessage;
  bool _isSearchTimeoutPending = false;
  final GlobalKey _resultsKey = GlobalKey();
  final GlobalKey _step2Key = GlobalKey();
  final GlobalKey _searchFieldKey = GlobalKey();
  bool _showBackToTop = false;
  bool _hasSearched = false;
  bool _isRelaxedSearch =
      false; // New state to track if we show fallback results
  bool _isSatLoading = false;
  bool _isShaLoading = false;
  int _searchRequestId = 0;
  int _suggestionRequestId = 0;
  int _suggestionDebounceRequestId = 0;
  int _suggestionGuardRequestId = 0;
  int _celebsResumeRequestId = 0;
  Timer? _filterDebounce;
  final FocusNode _searchFocusNode = FocusNode();
  late AnimationController _celebsAnimController;
  double _celebsScrollPos = 0.0;
  final ScrollController _scrollController = ScrollController();

  String? _relaxedFiltersNotice; // Labels of filters that were turned off

  NameSuggestionsResponse? _nameSuggestions;
  bool _loadingSuggestions = false;
  NameIntentResult? _nameIntentResult;
  bool _isLoadingNameIntent = false;
  InputClassification? _inputClassification;
  Timer? _classifyTimer;
  bool _ignoreNextSearchInputChange = false;
  bool _hideSelectedMeaningCard = false;
  bool _isSuggestionBoxExpanded = false;
  Map<String, dynamic>? _cachedStats;
  List? _cachedResults;
  bool? _cachedSat, _cachedSha;
  String? _speakingKey;
  _TtsStatus _ttsStatus = _TtsStatus.unavailable;
  bool _suppressNextGlobalUnfocus = false;

  void _invalidateStatsCache() {
    _cachedStats = null;
    _cachedResults = null;
  }

  bool get _hasRankingCriteria => _filterSat || _filterSha;
  bool get _hasCachedSuggestions => _nameSuggestions?.names.isNotEmpty ?? false;

  int _pairTypeRank(String? pairType) {
    final trimmed = pairType?.toUpperCase().trim() ?? '';
    if (trimmed.isEmpty) return 0;
    switch (trimmed) {
      case 'D10':
        return 3;
      case 'D8':
        return 2;
      case 'D5':
        return 1;
      default:
        return 0;
    }
  }

  // ignore: unused_element
  bool _isRedPairType(String? pairType) {
    final normalized = pairType?.toUpperCase().trim() ?? '';
    return normalized == 'R10' || normalized == 'R7' || normalized == 'R5';
  }

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



  List<Map<String, dynamic>> _ideaExamples = [];
  List<Map<String, dynamic>> _pickedExamples = [];
  Timer? _shuffleTimer;

  static IconData _iconNameToIconData(String name) {
    switch (name) {
      case 'trending_up':
        return Icons.trending_up;
      case 'favorite':
        return Icons.favorite;
      case 'shield':
        return Icons.shield;
      case 'psychology':
        return Icons.psychology;
      case 'auto_awesome':
        return Icons.auto_awesome;
      case 'star':
        return Icons.star;
      case 'diamond':
        return Icons.diamond;
      case 'emoji_events':
        return Icons.emoji_events;
      case 'thumb_up':
        return Icons.thumb_up;
      case 'rocket_launch':
        return Icons.rocket_launch;
      case 'local_fire_department':
        return Icons.local_fire_department;
      case 'water_drop':
        return Icons.water_drop;
      case 'park':
        return Icons.park;
      case 'sunny':
        return Icons.sunny;
      case 'nightlight':
        return Icons.nightlight;
      case 'public':
        return Icons.public;
      case 'language':
        return Icons.language;
      case 'spa':
        return Icons.spa;
      case 'pets':
        return Icons.pets;
      case 'music_note':
        return Icons.music_note;
      case 'palette':
        return Icons.palette;
      case 'school':
        return Icons.school;
      case 'work':
        return Icons.work;
      case 'monetization_on':
        return Icons.monetization_on;
      case 'account_balance':
        return Icons.account_balance;
      case 'volunteer_activism':
        return Icons.volunteer_activism;
      case 'self_improvement':
        return Icons.self_improvement;
      case 'hive':
        return Icons.hive;
      case 'bolt':
        return Icons.bolt;
      case 'waving_hand':
        return Icons.waving_hand;
      case 'diversity_3':
        return Icons.diversity_3;
      case 'mood':
        return Icons.mood;
      case 'sparkles':
        return Icons.auto_awesome;
      case 'workspace_premium':
        return Icons.workspace_premium;
      default:
        return Icons.auto_awesome;
    }
  }

  static Color _hexToColor(String hex) {
    hex = hex.replaceFirst('#', '');
    if (hex.length == 6) hex = 'FF$hex';
    return Color(int.parse(hex, radix: 16));
  }

  Future<void> _search({
    bool scrollToResults = false,
    bool showInputSnack = true,
    bool reloadSelectedName = true,
    String? overrideKeyword,
    bool preserveScrollPosition = false,
    bool allowWhileLoading = false,
  }) async {
    final Stopwatch searchWatch = Stopwatch()..start();
    _suggestionDebounceRequestId++;
    final int requestId = ++_searchRequestId;
    debugPrint(
      '[_search] requestId=$requestId, filterSat=$_filterSat, filterSha=$_filterSha',
    );
    if (!mounted) return;
    if (_isLoading && !allowWhileLoading) return;
    if (!preserveScrollPosition) {
      FocusScope.of(context).unfocus();
    }
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
                    "พิมพ์ความหมายที่ต้องการ หรือแตะตัวอย่างก่อนค้นหานะคะ",
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

    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _isSearchTimeoutPending = false;
      _hasSearched = true;
      _isLoadingNameIntent = true;
    });

    String finalKeyword = keyword;
    final String originalInput = keyword;
    NameIntentResult? detectedIntent;

    if (reloadSelectedName || _nameIntentResult == null) {
      try {
        detectedIntent = await _apiService.detectNameIntent(finalKeyword);
      } catch (_) {
        detectedIntent = null;
      }
    } else {
      detectedIntent = _nameIntentResult;
    }

    if (!mounted || requestId != _searchRequestId) return;

    final String? detectedTarget = detectedIntent?.topCandidate;
    final bool shouldPivotToCandidate =
        detectedIntent != null &&
        (detectedIntent.isName || detectedIntent.isHybrid) &&
        detectedTarget != null;

    if (shouldPivotToCandidate) {
      finalKeyword = detectedTarget;
    }

    final bool isNameOrHybridIntent =
        detectedIntent != null &&
        (detectedIntent.isName || detectedIntent.isHybrid);
    final bool looksLikeTypedName = _looksLikeTypedThaiName(originalInput);
    final bool isSemanticIntent = !isNameOrHybridIntent && !looksLikeTypedName;
    debugPrint(
      '[semantic-search] keyword="$originalInput" finalKeyword="$finalKeyword" '
      'isSemanticIntent=$isSemanticIntent',
    );

    setState(() {
      _nameIntentResult = detectedIntent;
      _isLoadingNameIntent = false;
    });

    final bool isSemanticSearch = (_selectedExampleIndex != null) || (isSemanticIntent && !looksLikeTypedName);

    if (isSemanticSearch) {
      if (reloadSelectedName && (!_hasRankableNameTemplate || _selectedNameMeaning != originalInput)) {
        if (mounted) {
          setState(() {
            _isLoadingSelectedNameMeaning = true;
            _hideSelectedMeaningCard = false;
          });
        }
        try {
          final suggRes = await _apiService.getNameSuggestions(originalInput);
          if (suggRes != null && suggRes.names.isNotEmpty) {
            final bestName = suggRes.names.first.name;
            if (mounted) {
              setState(() {
                _selectedNameMeaningName = bestName;
                _selectedNameMeaning = originalInput;
                _selectedNameAnalysis = null;
                _hasRankableNameTemplate = true;
                _isLoadingSelectedNameMeaning = false;
                _isSuggestionBoxExpanded = false;
              });
            }
            unawaited(_loadSeedNameAnalysis(bestName));
            unawaited(fetchNameSuggestions(bestName, meaning: originalInput));
          } else {
            if (mounted) {
              setState(() {
                _selectedNameMeaningName = originalInput;
                _selectedNameMeaning = originalInput;
                _selectedNameAnalysis = null;
                _hasRankableNameTemplate = true;
                _isLoadingSelectedNameMeaning = false;
                _isSuggestionBoxExpanded = false;
              });
            }
          }
        } catch (e) {
          debugPrint('Error loading semantic seed name: $e');
        }
      }
    } else {
      // Always attempt to load numerology/meaning for the search query if it's not a long example phrase
      if (reloadSelectedName && _selectedExampleIndex == null) {
        if (_hasRankableNameTemplate) {
          // Seed name is already resolved from DB suggestion/avatar.
        } else if (isSemanticIntent && !looksLikeTypedName) {
          setState(() {
            _hideSelectedMeaningCard = false;
            _selectedNameMeaningName = originalInput;
            _selectedNameMeaning = originalInput;
            _selectedNameAnalysis = null;
            _hasRankableNameTemplate = true;
            _isLoadingSelectedNameMeaning = false;
          });
        } else {
          await loadSelectedNameMeaning(
            originalInput,
            forceDecode: isNameOrHybridIntent,
          );
        }
      } else if (reloadSelectedName) {
        setState(() {
          _selectedNameAnalysis = null;
          _selectedNameMeaningName = null;
          _selectedNameMeaning = null;
        });
      }
    }

    // --- 1. SET PARAMETERS ---
    String actualTarget = overrideKeyword ?? finalKeyword;
    if (isSemanticSearch && _selectedNameMeaningName != null) {
      actualTarget = _selectedNameMeaningName!;
    }
    const String finalLastname = "";
    final String apiSearchKeyword = actualTarget;
    final bool shouldFetchMeaningSuggestions =
        isSemanticIntent ||
        (_selectedNameAnalysis == null &&
            (_selectedNameMeaning?.trim().isNotEmpty ?? false));
    final String suggestionMeaning =
        (_selectedNameMeaning?.trim().isNotEmpty ?? false)
        ? _selectedNameMeaning!.trim()
        : originalInput;
    final bool shouldUsePgTrgmSuggestions =
        _looksLikeTypedThaiName(originalInput) && !_hasRankableNameTemplate;
    debugPrint(
      '[semantic-search] shouldFetchMeaningSuggestions=$shouldFetchMeaningSuggestions '
      'suggestionMeaning="$suggestionMeaning" selectedMeaning="${_selectedNameMeaning ?? ''}" '
      'pgTrgmFallback=$shouldUsePgTrgmSuggestions',
    );
    final bool shouldFetchRankedResults =
        _hasRankingCriteria && _hasRankableNameTemplate;

    if (!shouldFetchRankedResults) {
      if (!mounted || requestId != _searchRequestId) return;
      setState(() {
        _isLoading = false;
        _errorMessage = null;
        _hasSearched = true;
        _isRelaxedSearch = false;
        _relaxedFiltersNotice = null;
      });
      if (!_hasCachedSuggestions) {
        unawaited(
          fetchNameSuggestions(
            originalInput,
            meaning:
                (!shouldUsePgTrgmSuggestions && shouldFetchMeaningSuggestions)
                ? suggestionMeaning
                : null,
          ),
        );
      }
      if (scrollToResults) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            scrollToBottom();
          }
        });
      }
      return;
    }

    try {
      // --- 2. SUGGESTION PIVOT (For long sentences/ideas) ---
      if (_selectedExampleIndex != null &&
          overrideKeyword == null &&
          !isSemanticIntent) {
        if (mounted) setState(() => _isPivotingIdea = true);
        final suggRes = await _apiService.getNameSuggestions(finalKeyword);
        if (mounted) setState(() => _isPivotingIdea = false);

        if (suggRes != null && suggRes.names.isNotEmpty) {
          final bestName = suggRes.names[0].name;
          if (mounted) {
            await _search(
              scrollToResults: scrollToResults,
              overrideKeyword: bestName,
              allowWhileLoading: true,
            );
          }
          return;
        }
      }

      // --- 3. PRIMARY API CALL ---
      debugPrint('[_search] calling API with filterSat=$_filterSat');
      final int searchLimit = (_filterSat && _filterSha) ? 200 : 100;
      final Stopwatch apiWatch = Stopwatch()..start();
      var response = await _apiService.searchNames(
        keyword: apiSearchKeyword,
        lastname: finalLastname,
        semanticMeaning: _selectedNameMeaning,
        day: _selectedDay,
        filterSat: _filterSat,
        filterSha: _filterSha,
        filterKaki: _filterKaki,
        similarMode: false,
        limit: searchLimit, // Fetch a large pool to ensure we find variety
      );
      debugPrint(
        '[_search] API response received in ${apiWatch.elapsedMilliseconds}ms, count=${response.results.length}',
      );

      // --- 4. DATA PROCESSING & FILTERING ---
      final englishRegex = RegExp(r'[a-zA-Z]');

      List<MobileNameResult> filterAndClean(List<MobileNameResult> list) {
        return list.where((r) {
          final name = r.name.trim();
          if (englishRegex.hasMatch(name)) return false;
          if (name.runes.length <= 1) return false;
          if (!(_filterSat || _filterSha)) return false;

          final bool passesSatFilter = r.isSatGood;
          final bool passesShaFilter = r.isShaGood;
          if (_filterSat && _filterSha) {
            if (!passesSatFilter || !passesShaFilter) return false;
          } else if (_filterSat) {
            if (!passesSatFilter || passesShaFilter) return false;
          } else if (_filterSha) {
            if (passesSatFilter || !passesShaFilter) return false;
          }

          return true;
        }).toList();
      }

      var results = filterAndClean(response.results);

      // Ignore stale responses so old searches cannot override scroll/state.
      if (!mounted || requestId != _searchRequestId) return;

      setState(() {
        _isRelaxedSearch = false; // Reset state
        _isSearchTimeoutPending = false;
        if (_filterSat || _filterSha) {
          const int maxDisplayedResults = 100;
          if (results.length > maxDisplayedResults) {
            results = results.take(maxDisplayedResults).toList();
          }
        }
        _invalidateStatsCache();
        _results = results;

        try {
          results.sort((a, b) {
            if (!(_filterSat || _filterSha)) return 0;

            if (_filterSat && _filterSha) {
              if (a.finalRankScore != b.finalRankScore) {
                return b.finalRankScore.compareTo(a.finalRankScore);
              }
              double score(MobileNameResult item) {
                return item.calculateScore(showMatching: false).toDouble();
              }

              return score(
                b,
              ).compareTo(score(a)); // Sort High Score -> Low Score
            }

            if (_filterSat && !_filterSha) {
              if (a.finalRankScore != b.finalRankScore) {
                return b.finalRankScore.compareTo(a.finalRankScore);
              }
              final double scoreA = a
                  .calculateScore(showMatching: false)
                  .toDouble();
              final double scoreB = b
                  .calculateScore(showMatching: false)
                  .toDouble();
              if (scoreA != scoreB) {
                return scoreB.compareTo(scoreA);
              }

              final int satGoodCompare =
                  (b.isSatGood ? 1 : 0) - (a.isSatGood ? 1 : 0);
              if (satGoodCompare != 0) return satGoodCompare;

              final int pairTypeCompare = _pairTypeRank(
                b.satPairType,
              ).compareTo(_pairTypeRank(a.satPairType));
              if (pairTypeCompare != 0) return pairTypeCompare;

              final int pairPointCompare = b.satPairPoint.compareTo(
                a.satPairPoint,
              );
              if (pairPointCompare != 0) return pairPointCompare;

              return (b.semanticScore).compareTo(a.semanticScore);
            }

            if (_filterSha && !_filterSat) {
              if (a.finalRankScore != b.finalRankScore) {
                return b.finalRankScore.compareTo(a.finalRankScore);
              }
              final double scoreA = a
                  .calculateScore(showMatching: false)
                  .toDouble();
              final double scoreB = b
                  .calculateScore(showMatching: false)
                  .toDouble();
              if (scoreA != scoreB) {
                return scoreB.compareTo(scoreA);
              }

              final int shaGoodCompare =
                  (b.isShaGood ? 1 : 0) - (a.isShaGood ? 1 : 0);
              if (shaGoodCompare != 0) return shaGoodCompare;

              final int pairTypeCompare = _pairTypeRank(
                b.shaPairType,
              ).compareTo(_pairTypeRank(a.shaPairType));
              if (pairTypeCompare != 0) return pairTypeCompare;

              final int pairPointCompare = b.shaPairPoint.compareTo(
                a.shaPairPoint,
              );
              if (pairPointCompare != 0) return pairPointCompare;

              return (b.semanticScore).compareTo(a.semanticScore);
            }

            if (a.finalRankScore != b.finalRankScore) {
              return b.finalRankScore.compareTo(a.finalRankScore);
            }
            double score(MobileNameResult item) {
              return item.calculateScore(showMatching: false).toDouble();
            }

            return score(b).compareTo(score(a)); // Sort High Score -> Low Score
          });
        } catch (e, stack) {
          debugPrint('Error sorting results: $e');
          debugPrint('Stack trace: $stack');
          // Keep results unsorted rather than crash
        }

        _invalidateStatsCache();
        _results = results;
        _isLoading = false;
      });

      if ((shouldFetchMeaningSuggestions || shouldFetchRankedResults) &&
          mounted &&
          requestId == _searchRequestId) {
        final Stopwatch suggestionWatch = Stopwatch()..start();
        if (!_hasCachedSuggestions) {
          unawaited(
            fetchNameSuggestions(originalInput, meaning: suggestionMeaning),
          );
        }
        debugPrint(
          '[_search] fetchNameSuggestions initiated concurrently in ${suggestionWatch.elapsedMilliseconds}ms',
        );
      }

      if (mounted &&
          requestId == _searchRequestId &&
          _shouldRunCelebsAutoScroll) {
        _resetAndRestartCelebsAnimation();
      }

      if (_results.isNotEmpty && scrollToResults) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted || requestId != _searchRequestId) return;
          scrollToResults0();
        });
      }
      debugPrint(
        '[_search] total elapsed ${searchWatch.elapsedMilliseconds}ms (requestId=$requestId)',
      );
    } catch (e) {
      if (!mounted || requestId != _searchRequestId) return;
      final bool isTimeout =
          e is TimeoutException ||
          (e is ApiException && e.message.contains('การเชื่อมต่อล่าช้าเกินไป'));
      if (mounted) {
        setState(() {
          _errorMessage = isTimeout
              ? null
              : e is ApiException
              ? e.message
              : "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง";
          _isSearchTimeoutPending = isTimeout;
          _isLoading = false;
          if (!isTimeout) {
            _invalidateStatsCache();
            _results = []; // Clear old results on error
          }
        });
      }
      debugPrint(
        '[_search] failed after ${searchWatch.elapsedMilliseconds}ms (requestId=$requestId)',
      );
    }
  }

  Map<String, dynamic> computeStatistics() {
    if (_results.isEmpty) {
      final stats = {
        'totalNames': 0,
        'excellentNames': 0,
        'numerologyGood': 0,
        'shadowGood': 0,
        'recommendedDays': null,
      };
      _cachedStats = stats;
      _cachedResults = [];
      _cachedSat = _filterSat;
      _cachedSha = _filterSha;
      return stats;
    }

    if (_cachedStats != null &&
        _cachedResults != null &&
        listEquals(_cachedResults, _results) &&
        _cachedSat == _filterSat &&
        _cachedSha == _filterSha) {
      return _cachedStats!;
    }

    final int totalNames = _results.length;
    int excellentNames = 0;
    int numerologyGood = 0;
    int shadowGood = 0;

    for (final r in _results) {
      final sat = r.isSatGood;
      final sha = r.isShaGood;

      if (sat && sha) excellentNames++;
      if (sat) numerologyGood++;
      if (sha) shadowGood++;
    }

    String? recommendedDays;
    final stats = {
      'totalNames': totalNames,
      'excellentNames': excellentNames,
      'numerologyGood': numerologyGood,
      'shadowGood': shadowGood,
      'recommendedDays': recommendedDays,
    };
    _cachedStats = stats;
    _cachedResults = List.from(_results);
    _cachedSat = _filterSat;
    _cachedSha = _filterSha;
    return stats;
  }

  @override
  void initState() {
    super.initState();
    _celebsAnimController = AnimationController(vsync: this)
      ..addListener(_handleCelebsAnimationTick);
    unawaited(_initTts());
    _keywordController.addListener(onSearchInputChanged);
    _searchFocusNode.addListener(() {
      if (mounted) setState(() {});
    });

    // Prefetch cached saved names so list items know if they are saved automatically
    _apiService.loadSavedNamesCache().then((_) {
      if (mounted) setState(() {});
    });

    loadCelebrities();
    initPremium();
    _loadIdeaExamples();
  }

  Future<void> _loadIdeaExamples() async {
    final raw = await _apiService.getSemanticSearchIdeas();
    if (!mounted) return;
    setState(() {
      final list = raw.map((item) {
        final iconName = item['icon_name'] as String? ?? 'auto_awesome';
        final colorHex = item['icon_color'] as String? ?? '#8B6CD9';
        return {
          'text': item['text'] as String? ?? '',
          'icon': _iconNameToIconData(iconName),
          'iconColor': _hexToColor(colorHex),
        };
      }).toList();

      if (list.isNotEmpty) {
        _ideaExamples = list;
      } else {
        _ideaExamples = [
          {
            "text": "เศรษฐีผู้มั่งคั่ง มีทรัพย์สมบัติและบารมี",
            "icon": Icons.trending_up,
            "iconColor": const Color(0xFFD4A017),
          },
          {
            "text": "หญิงสาวผู้อ่อนหวาน มีเสน่ห์ และเป็นที่รัก",
            "icon": Icons.favorite,
            "iconColor": const Color(0xFFE66A8D),
          },
          {
            "text": "ผู้นำที่กล้าหาญ เจริญรุ่งเรือง ไร้อุปสรรค",
            "icon": Icons.shield,
            "iconColor": const Color(0xFF4F8FE8),
          },
          {
            "text": "ปราชญ์ผู้มีสติปัญญาเฉลียวฉลาด และอายุยืน",
            "icon": Icons.psychology,
            "iconColor": const Color(0xFF8B6CD9),
          },
        ];
      }
    });
    _applyPickedExamples();
  }

  void _applyPickedExamples({String? bringToTop}) {
    final shuffled = List<Map<String, dynamic>>.from(_ideaExamples)
      ..shuffle(math.Random());
    List<Map<String, dynamic>> picked;
    if (bringToTop != null) {
      final target = shuffled.firstWhere(
        (e) => e['text'] == bringToTop,
        orElse: () => shuffled.first,
      );
      shuffled.remove(target);
      picked = [target, ...shuffled.take(5)];
      if (mounted) {
        setState(() {
          _pickedExamples = picked;
        });
      }
      _shuffleTimer?.cancel();
    } else {
      picked = shuffled.take(6).toList();
      if (mounted) {
        setState(() {
          _pickedExamples = picked;
        });
      }
      _startShuffleTimer();
    }
  }

  void _shuffleNext() {
    if (_ideaExamples.isEmpty) return;
    setState(() {
      final textToClear = _selectedExampleText;
      _selectedExampleIndex = null;
      _selectedExampleText = null;
      if (textToClear != null &&
          _keywordController.text.trim() == textToClear.trim()) {
        _keywordController.clear();
      }

      if (_pickedExamples.length >= 2 &&
          _ideaExamples.length > _pickedExamples.length) {
        _pickedExamples.removeAt(0);
        final currentTexts = _pickedExamples.map((e) => e['text'] as String).toSet();
        final available = _ideaExamples.where((e) => !currentTexts.contains(e['text'] as String)).toList();
        if (available.isNotEmpty) {
          final nextItem = available[math.Random().nextInt(available.length)];
          _pickedExamples.add(nextItem);
        } else {
          final first = _pickedExamples.removeAt(0);
          _pickedExamples.add(first);
        }
      } else if (_pickedExamples.length >= 2) {
        final first = _pickedExamples.removeAt(0);
        _pickedExamples.add(first);
      }
    });
    _startShuffleTimer();
  }

  void _startShuffleTimer() {
    // Only run timer if no item is currently selected
    if (_selectedExampleText != null) {
      _shuffleTimer?.cancel();
      return;
    }
    _shuffleTimer?.cancel();
    _shuffleTimer = Timer.periodic(const Duration(seconds: 6), (_) {
      if (!mounted) return;
      // Double check inside tick
      if (_selectedExampleText != null) {
        _shuffleTimer?.cancel();
        return;
      }
      setState(() {
        if (_pickedExamples.length >= 2 &&
            _ideaExamples.length > _pickedExamples.length) {
          // Remove the first item
          _pickedExamples.removeAt(0);

          // Get the texts of the remaining items
          final currentTexts = _pickedExamples
              .map((e) => e['text'] as String)
              .toSet();

          // Find items in _ideaExamples that are not in _pickedExamples
          final available = _ideaExamples
              .where((e) => !currentTexts.contains(e['text'] as String))
              .toList();

          if (available.isNotEmpty) {
            // Pick a random one from available items
            final nextItem = available[math.Random().nextInt(available.length)];
            _pickedExamples.add(nextItem);
          } else {
            // Fallback: rotate the first item back
            final first = _pickedExamples.removeAt(0);
            _pickedExamples.add(first);
          }
        } else if (_pickedExamples.length >= 2) {
          final first = _pickedExamples.removeAt(0);
          _pickedExamples.add(first);
        }
      });
    });
  }

  void onSearchInputChanged() {
    if (_ignoreNextSearchInputChange) {
      _ignoreNextSearchInputChange = false;
      return;
    }

    final text = _keywordController.text.trim();
    debugPrint("Search input changed: '$text'");

    // Reset ranking results and handle active states
    if (mounted) {
      setState(() {
        _hideSelectedMeaningCard = false;
        _isSuggestionBoxExpanded = false;
        _invalidateStatsCache();
        _results = [];
        _hasSearched = false;
        _isRelaxedSearch = false;
        _relaxedFiltersNotice = null;
        _nameIntentResult = null;
        _isLoadingNameIntent = false;
        _inputClassification = null;

        // Improved Logic: Be more forgiving with string comparison (trim both)
        if (_selectedExampleIndex != null) {
          final target = _selectedExampleText ?? '';
          if (text.trim() != target.trim()) {
            _selectedExampleIndex = null;
            _selectedExampleText = null;
            // Restart shuffle timer!
            _startShuffleTimer();
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
        _hideSelectedMeaningCard = false;
        _selectedNameMeaningName = null;
        _selectedNameMeaning = null;
        _selectedNameAnalysis = null;
        _hasRankableNameTemplate = false;
        _isLoadingSelectedNameMeaning = false;
        _nameSuggestions = null;
        _loadingSuggestions = false;
        _nameIntentResult = null;
        _isLoadingNameIntent = false;
        _inputClassification = null;
        _selectedExampleIndex = null;
        _selectedExampleText = null;
        // Restart shuffle timer if it was stopped
        _startShuffleTimer();
      });
      return;
    }

    setState(() {
      _nameSuggestions = null;
      _loadingSuggestions = false;
    });

    // Always fetch suggestions since Backend now handles both pg_trgm & semantic
    // It will also load the meaning/analysis of the currently typed text inside the debouncer
    fetchNameSuggestionsDebounced(text);
    _scheduleClassifyInput(text);

    if (mounted) {
      setState(() {
        _inputClassification = _classifyInputLocally(text);
      });
    }
  }

  InputClassification _classifyInputLocally(String text) {
    final trimmed = text.trim();
    if (trimmed.isEmpty) {
      return const InputClassification(
        type: "meaning",
        confidence: 0.0,
        signals: [],
      );
    }

    // 1. Number detection (digits only)
    if (RegExp(r'^\d+$').hasMatch(trimmed)) {
      return const InputClassification(
        type: "number",
        confidence: 1.0,
        signals: ["digits_only"],
      );
    }

    // 2. Full Name detection (with space)
    if (trimmed.contains(RegExp(r'\s+'))) {
      final parts = trimmed.split(RegExp(r'\s+'));
      return InputClassification(
        type: "full_name",
        confidence: 0.9,
        firstName: parts.first,
        surname: parts.length > 1 ? parts[1] : null,
        signals: ["contains_space"],
      );
    }

    // 3. Single Name vs Meaning detection
    if (RegExp(r'^[ก-๙]+$').hasMatch(trimmed)) {
      // Common prefix meaning keywords in Thai
      final commonMeaningKeywords = [
        'ความ',
        'การ',
        'ผู้',
        'ใจ',
        'รัก',
        'งาม',
        'ดี',
        'มี',
        'สุข',
        'โชค',
      ];
      bool hasMeaningKeyword = false;
      for (final kw in commonMeaningKeywords) {
        if (trimmed.startsWith(kw) && trimmed.length > 3) {
          hasMeaningKeyword = true;
          break;
        }
      }

      // Names are typically short and don't start with meaning prefixes
      if (trimmed.length >= 2 && trimmed.length <= 8 && !hasMeaningKeyword) {
        return InputClassification(
          type: "single_name",
          confidence: 0.85,
          firstName: trimmed,
          signals: ["thai_letters", "name_length_match"],
        );
      } else {
        return const InputClassification(
          type: "meaning",
          confidence: 0.7,
          signals: ["thai_letters", "meaning_pattern"],
        );
      }
    }

    // Default fallback
    return const InputClassification(
      type: "meaning",
      confidence: 0.5,
      signals: ["default_fallback"],
    );
  }

  void _setKeywordWithoutTriggeringListener(String value) {
    _ignoreNextSearchInputChange = true;
    _keywordController.text = value;
  }

  bool _looksLikeTypedThaiName(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty || trimmed.contains(RegExp(r'\s'))) return false;
    final runes = trimmed.runes.length;
    if (runes < 2 || runes > 12) return false;
    return RegExp(r'^[ก-๙]+$').hasMatch(trimmed);
  }

  void _scheduleClassifyInput(String text) {
    _classifyTimer?.cancel();
    if (text.isEmpty) {
      setState(() => _inputClassification = null);
      return;
    }
    _classifyTimer = Timer(const Duration(milliseconds: 600), () async {
      final result = await _apiService.classifyInput(text);
      if (!mounted) return;
      if (_keywordController.text.trim() != text) return;
      setState(() {
        _inputClassification = result;
        if (result != null &&
            (result.type == "single_name" ||
                result.type == "full_name" ||
                result.type == "meaning")) {
          _filterSha = true;
          _filterSat = false;
          _hasRankableNameTemplate = true;
        }
      });
      if (result != null) {
        if (result.type == "full_name" && result.firstName != null) {
          _resolveSeedName(result.firstName!);
        } else if (result.type == "single_name") {
          _resolveSeedName(result.firstName ?? text);
        }
        if (result.type == "single_name" ||
            result.type == "full_name" ||
            result.type == "meaning") {
          // Use allowWhileLoading so this classify-triggered search can
          // override any in-progress search. Without this, a stale search
          // holds _isLoading=true and the new search is silently dropped,
          // leaving the UI in a "ranking active but no results" stuck state.
          _search(
            scrollToResults: false,
            preserveScrollPosition: true,
            allowWhileLoading: true,
          );
        }
      }
    });
  }

  Widget _buildClassificationChip(InputClassification c) {
    final isName = c.type == "single_name";
    final isFullName = c.type == "full_name";
    final Color chipColor = isName
        ? const Color(0xFF059669)
        : isFullName
        ? const Color(0xFF7C3AED)
        : const Color(0xFFD97706);
    final Color bgColor = isName
        ? const Color(0xFFECFDF5)
        : isFullName
        ? const Color(0xFFF5F3FF)
        : const Color(0xFFFFFBEB);
    final IconData icon = isName
        ? Icons.person_rounded
        : isFullName
        ? Icons.people_rounded
        : Icons.auto_awesome_rounded;
    final String label = isFullName
        ? "ชื่อ-สกุล"
        : isName
        ? "ชื่อ"
        : "ค้นความหมาย";
    final String detail = isFullName
        ? "${c.firstName} + ${c.surname}"
        : isName
        ? c.firstName?.isNotEmpty == true
              ? c.firstName!
              : (c.signals.contains("exact_db_match")
                    ? "มีในฐานข้อมูล ✅"
                    : "คาดว่าเป็นชื่อ")
        : "ความหมาย: ${c.signals.where((s) => s != "descriptive_phrase" && s != "long_phrase" && s != "non_name_two_word_phrase").join(", ")}";
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: chipColor.withValues(alpha: 0.3), width: 1),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: chipColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon, size: 14, color: chipColor),
                const SizedBox(width: 4),
                Text(
                  label,
                  style: TextStyle(
                    color: chipColor,
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              detail,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: chipColor.withValues(alpha: 0.8),
                fontSize: 11,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          if (c.confidence > 0)
            Text(
              "${(c.confidence * 100).toInt()}%",
              style: TextStyle(
                color: chipColor.withValues(alpha: 0.5),
                fontSize: 10,
                fontWeight: FontWeight.w600,
              ),
            ),
        ],
      ),
    );
  }

  String _getThaiInputType(String type) {
    if (type == "single_name") return "ชื่อ";
    if (type == "full_name") return "ชื่อ + นามสกุล";
    if (type == "number") return "เลขศาสตร์ / ตัวเลข";
    return "ความหมายของชื่อ";
  }

  Color _getThaiInputTypeColor(String type) {
    if (type == "single_name") return const Color(0xFF059669);
    if (type == "full_name") return const Color(0xFF7C3AED);
    if (type == "number") return const Color(0xFFEA580C);
    return const Color(0xFFD97706);
  }

  void _resolveSeedName(String name, {String? meaningHint, bool expandSuggestions = false}) {
    setState(() {
      _selectedNameMeaningName = name;
      _selectedNameMeaning = meaningHint;
      _selectedNameAnalysis = null;
      _nameSuggestions = null;
      _isSuggestionBoxExpanded = expandSuggestions;
      _hasRankableNameTemplate = true;
      _isLoadingSelectedNameMeaning = false;
    });
    unawaited(_loadSeedNameAnalysis(name));
    unawaited(fetchNameSuggestions(name, meaning: meaningHint));
  }

  Future<void> _loadSeedNameAnalysis(String name) async {
    final trimmed = name.trim();
    if (trimmed.isEmpty) return;
    final analysis = await _apiService.decodeName(trimmed, day: _selectedDay);
    if (!mounted || _selectedNameMeaningName?.trim() != trimmed) return;
    setState(() {
      _selectedNameAnalysis = analysis;
    });
  }

  Future<void> loadSelectedNameMeaning(
    String name, {
    String? meaning,
    bool forceDecode = false,
  }) async {
    final trimmed = name.trim();
    if (trimmed.isEmpty) return;
    setState(() {
      _selectedNameMeaningName = trimmed;
      _selectedNameMeaning = meaning; // Use provided meaning if available
      _selectedNameAnalysis = null;
      _hasRankableNameTemplate = false;
      _isLoadingSelectedNameMeaning = true;
    });

    try {
      final resolved = await _apiService.resolveNameInput(
        trimmed,
        day: _selectedDay,
      );

      final bool useResolve = resolved != null;
      final bool shouldUsePgTrgmSuggestions =
          resolved?.shouldUsePgTrgmSuggestions ??
          (_looksLikeTypedThaiName(trimmed) && meaning == null);
      final bool canRankFromCurrentInput =
          resolved?.canRankFromTemplate == true ||
          resolved?.inputType == "name" ||
          _looksLikeTypedThaiName(trimmed) ||
          (meaning != null && meaning.trim().isNotEmpty);
      final bool isSemanticQuery =
          resolved?.isMeaning ?? !_looksLikeTypedThaiName(trimmed);
      final String? resolvedMeaning = meaning?.trim().isNotEmpty == true
          ? meaning!.trim()
          : (resolved?.dbMeaning?.trim().isNotEmpty == true
                ? resolved!.dbMeaning!.trim()
                : (isSemanticQuery ? trimmed : null));
      NameAnalysisResult? analysis = resolved?.decode;

      if (!useResolve && (forceDecode || _looksLikeTypedThaiName(trimmed))) {
        analysis = await _apiService.decodeName(trimmed);
      }

      if (!mounted) return;
      if (_selectedNameMeaningName != trimmed) return;
      setState(() {
        if (resolved?.intent != null) {
          _nameIntentResult = resolved!.intent;
        }
        _selectedNameMeaning = canRankFromCurrentInput ? resolvedMeaning : null;
        _selectedNameAnalysis = analysis;
        _hasRankableNameTemplate = canRankFromCurrentInput;
        _isLoadingSelectedNameMeaning = false;
      });

      // Names that are not in DB must use q-only pg_trgm suggestions.
      if (shouldUsePgTrgmSuggestions) {
        fetchNameSuggestions(trimmed);
      } else if (canRankFromCurrentInput &&
          resolvedMeaning != null &&
          resolvedMeaning.isNotEmpty &&
          (isSemanticQuery || resolvedMeaning != name)) {
        fetchNameSuggestions(trimmed, meaning: resolvedMeaning);
      } else {
        fetchNameSuggestions(trimmed);
      }
    } catch (e) {
      debugPrint("Error loading selected name meaning: $e");
      if (mounted && _selectedNameMeaningName == trimmed) {
        setState(() {
          _selectedNameMeaning = meaning;
          _selectedNameAnalysis = null;
          _hasRankableNameTemplate =
              meaning != null && meaning.trim().isNotEmpty;
          _isLoadingSelectedNameMeaning = false;
        });
        fetchNameSuggestions(trimmed, meaning: meaning);
      }
    }
  }

  void scrollToTop() {
    if (_scrollController.hasClients) {
      _scrollController.animateTo(
        0.0,
        duration: const Duration(milliseconds: 500),
        curve: Curves.easeInOut,
      );
    }
  }

  Future<void> initPremium() async {
    await PremiumManager().init();
    if (mounted) setState(() {});
  }

  // ANCHOR: Celebrity Avatar (รายชื่อตัวอย่างดารา)
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
        WidgetsBinding.instance.addPostFrameCallback((_) {
          Future.delayed(const Duration(milliseconds: 100), () {
            if (!mounted) return;
            _updateAutoScrollBasedOnFilters();
          });
        });
      } else {
        debugPrint("Naming examples API returned empty list.");
        // Clear celebrities if API fails or returns empty, do not use fallbacks
        setState(() {
          _celebrities = [];
          _celebsScrollPos = 0.0;
        });
        _updateAutoScrollBasedOnFilters();
      }
    } catch (e) {
      debugPrint("Error loading celebs: $e");
      // Clear celebrities on error
      setState(() {
        _celebrities = [];
        _celebsScrollPos = 0.0;
      });
      _updateAutoScrollBasedOnFilters();
    }
  }

  void startCelebsAutoScroll() {
    if (!mounted || !_shouldRunCelebsAutoScroll) return;
    final double seconds = (_celebrities.length * 70) / 25;
    _celebsAnimController
      ..stop()
      ..duration = Duration(
        milliseconds: (seconds * 1000).round().clamp(1000, 60000),
      )
      ..repeat();
  }

  bool get _shouldRunCelebsAutoScroll => _celebrities.isNotEmpty;

  void stopMarqueeTicker() {
    _celebsAnimController.stop();
  }

  void _updateAutoScrollBasedOnFilters() {
    if (!mounted) return;

    if (_shouldRunCelebsAutoScroll) {
      startCelebsAutoScroll();
    } else {
      stopMarqueeTicker();
      if (_celebsScrollPos != 0.0) {
        setState(() => _celebsScrollPos = 0.0);
      }
    }
  }

  void _resetAndRestartCelebsAnimation() {
    if (!mounted) return;
    _celebsAnimController.reset();
    setState(() => _celebsScrollPos = 0.0);
    _updateAutoScrollBasedOnFilters();
  }

  void _handleCelebsAnimationTick() {
    if (!mounted || !_shouldRunCelebsAutoScroll) return;
    final double maxScroll = _celebrities.length * 70.0;
    if (maxScroll <= 0) return;
    setState(() {
      _celebsScrollPos = _celebsAnimController.value * maxScroll;
    });
  }

  void resumeCelebsAutoScrollAfterDelay() {
    final int resumeRequestId = ++_celebsResumeRequestId;
    Future.delayed(const Duration(seconds: 2), () {
      if (!mounted || resumeRequestId != _celebsResumeRequestId) return;
      _updateAutoScrollBasedOnFilters();
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
          duration: const Duration(milliseconds: 700),
          curve: Curves.easeInOutCubic,
          alignment: 0.04, // Keep the whole input card above the keyboard
        );
      }
    });
  }

  void maybeScrollToSearchField() {
    if (!(_filterSat || _filterSha)) return;
    scrollToSearchField();
  }

  void scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final context = _step2Key.currentContext;
      if (context != null) {
        Scrollable.ensureVisible(
          context,
          duration: const Duration(milliseconds: 700),
          curve: Curves.easeInOutCubic,
          alignment: 1.0, // Keep the footer anchor pinned near the bottom
        );
      }
    });
  }

  void commitSearchInput() {
    FocusScope.of(context).unfocus();
    _search(scrollToResults: false);
  }

  void _onFilterToggled() {
    _filterDebounce?.cancel();
    _filterDebounce = Timer(const Duration(milliseconds: 160), () async {
      if (!mounted) return;
      try {
        await _refreshResultsKeepingStep2Anchor();
      } catch (e, stack) {
        debugPrint('Error refreshing premium filters: $e');
        debugPrint('Stack trace: $stack');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('เกิดข้อผิดพลาด: ${e.toString()}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      } finally {
        if (mounted) {
          setState(() {
            _isSatLoading = false;
            _isShaLoading = false;
          });
        }
      }
    });
  }

  bool get _isSubmittingSearch => _isLoading || _isLoadingNameIntent;

  String _inputActionLabel({required bool isBusy}) {
    if (isBusy) {
      if (_nameIntentResult?.isMeaning == true) {
        return "กำลังค้นหาความหมาย";
      }
      return "กำลังวิเคราะห์ชื่อ";
    }

    if (_nameIntentResult?.isMeaning == true) {
      return "ค้นหาความหมาย";
    }

    return "วิเคราะห์ชื่อ";
  }

  @override
  void dispose() {
    _filterDebounce?.cancel();
    _classifyTimer?.cancel();
    _shuffleTimer?.cancel();
    _flutterTts.stop();
    _searchFocusNode.dispose();
    _celebsAnimController.dispose();
    _scrollController.dispose();
    _keywordController.removeListener(onSearchInputChanged);
    _keywordController.dispose();
    super.dispose();
  }

  Future<void> _initTts() async {
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
        if (thaiLanguageOk) {
          thaiVoiceFound = true;
        }
      }
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

    if (mounted) {
      setState(() => _ttsStatus = status);
    }
    debugPrint('TTS initialization completed, status: ${status.name}');

    _flutterTts.setCompletionHandler(() {
      if (!mounted) return;
      setState(() {
        _speakingKey = null;
      });
    });
    _flutterTts.setCancelHandler(() {
      if (!mounted) return;
      setState(() {
        _speakingKey = null;
      });
    });
    _flutterTts.setErrorHandler((_) {
      if (!mounted) return;
      setState(() {
        _speakingKey = null;
      });
    });
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

  // ignore: unused_element
  String _getSpeakTooltip() {
    switch (_ttsStatus) {
      case _TtsStatus.ready:
        return 'อ่านออกเสียงภาษาไทย';
      case _TtsStatus.noThaiVoice:
        return 'ยังไม่มีเสียงไทย — แตะเพื่ออ่านด้วยเสียงที่มี';
      case _TtsStatus.unavailable:
        return 'TTS ไม่พร้อมใช้งาน';
    }
  }

  Future<void> _speakText(String text, {required String speakingKey}) async {
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
      setState(() {
        _speakingKey = null;
      });
      return;
    }

    await _flutterTts.stop();
    if (!mounted) return;
    setState(() {
      _speakingKey = speakingKey;
    });
    await _flutterTts.speak(_prepareSpeakableThaiName(trimmed));
    if (!mounted) return;
    setState(() {
      _speakingKey = null;
    });
  }

  Future<void> _speakNameAndMeaning(SuggestionNameItem item) async {
    final name = _prepareSpeakableThaiName(item.name);
    final meaning = item.meaning.trim();
    if (name.isEmpty) return;

    final speakingKey = 'name+meaning:${item.id}';

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
      await _flutterTts.speak(name);
      if (meaning.isNotEmpty) {
        await Future<void>.delayed(const Duration(milliseconds: 500));
        if (!mounted) return;
        await _flutterTts.speak(_prepareSpeakableThaiName(meaning));
      }
      if (!mounted) return;
      setState(() => _speakingKey = null);
    } catch (_) {
      if (!mounted) return;
      setState(() => _speakingKey = null);
    }
  }

  Future<void> _speakPhonetic(SuggestionNameItem item) async {
    final phonetic = (item.phoneticSummary.trim().isNotEmpty)
        ? item.phoneticSummary.trim()
        : item.name;
    await _speakText(phonetic, speakingKey: 'phonetic:${item.id}');
  }

  Future<void> _speakSelectedMeaning() async {
    final meaning = _selectedNameMeaning?.trim() ?? '';
    final name = _selectedNameMeaningName?.trim() ?? '';
    if (meaning.isEmpty) return;

    final speakingKey = name.isEmpty
        ? 'selected-meaning'
        : 'selected-meaning:$name';
    if (name.isEmpty) {
      await _speakText(meaning, speakingKey: speakingKey);
      return;
    }

    if (_speakingKey == speakingKey) {
      await _flutterTts.stop();
      if (!mounted) return;
      setState(() {
        _speakingKey = null;
      });
      return;
    }

    await _flutterTts.stop();
    if (!mounted) return;
    setState(() {
      _speakingKey = speakingKey;
    });

    try {
      await _flutterTts.speak(_prepareSpeakableThaiName(name));
      await Future<void>.delayed(const Duration(milliseconds: 650));
      if (!mounted) return;
      await _flutterTts.speak(_prepareSpeakableThaiName(meaning));
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _speakingKey = null;
      });
    }
  }

  bool _isSpeakingKey(String key) => _speakingKey == key;

  String _prepareSpeakableThaiName(String name) {
    final normalized = name
        .replaceAll(RegExp(r'\s+'), ' ')
        .replaceAll('-', ' ')
        .replaceAll('_', ' ')
        .replaceAll('/', ' ')
        .trim();

    if (normalized.isEmpty) return name;

    // Read the name itself so the user hears the pronunciation directly,
    // not an explanatory sentence around it.
    return normalized;
  }

  void fetchNameSuggestionsDebounced(String query, {String? meaning}) {
    debugPrint("Debounced suggestion call for: '$query'");
    final int requestId = ++_suggestionDebounceRequestId;
    Future.delayed(const Duration(milliseconds: 500), () async {
      if (!mounted || requestId != _suggestionDebounceRequestId) return;
      await loadSelectedNameMeaning(query, meaning: meaning);
    });
  }

  Future<void> fetchNameSuggestions(String query, {String? meaning}) async {
    if (!mounted) return;
    final int requestId = ++_suggestionRequestId;

    debugPrint("Fetching name suggestions for: '$query' (meaning: $meaning)");
    debugPrint(
      '[semantic-search] requestId=$requestId fetch query="$query" meaning="${meaning ?? ''}"',
    );
    setState(() {
      _loadingSuggestions = true;
      _nameSuggestions = null;
    });
    final int guardRequestId = ++_suggestionGuardRequestId;
    Future.delayed(const Duration(seconds: 12), () {
      if (!mounted || guardRequestId != _suggestionGuardRequestId) return;
      if (requestId != _suggestionRequestId) return;
      setState(() {
        _loadingSuggestions = false;
      });
    });

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

      debugPrint(
        '[semantic-search] requestId=$requestId response names=${suggestions?.names.length ?? 0}',
      );

      if (mounted && requestId == _suggestionRequestId) {
        _suggestionGuardRequestId++;
        setState(() {
          _nameSuggestions = suggestions;
          _loadingSuggestions = false;
        });
      }
    } catch (e) {
      debugPrint("Error fetching name suggestions: $e");
      debugPrint('[semantic-search] requestId=$requestId error=$e');
      if (mounted && requestId == _suggestionRequestId) {
        _suggestionGuardRequestId++;
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
      resizeToAvoidBottomInset: false,
      floatingActionButton: AnimatedSwitcher(
        duration: const Duration(milliseconds: 300),
        transitionBuilder: (Widget child, Animation<double> animation) {
          return ScaleTransition(scale: animation, child: child);
        },
        child: _showBackToTop
            ? GestureDetector(
                key: const ValueKey('back_to_top'),
                onTap: scrollToTop,
                child: Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: const LinearGradient(
                      colors: [Color(0xFFD946EF), Color(0xFFC026D3)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFFD946EF).withValues(alpha: 0.4),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                    ],
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.5),
                      width: 1.5,
                    ),
                  ),
                  child: const Center(
                    child: Icon(
                      Icons.arrow_upward_rounded,
                      color: Colors.white,
                      size: 20,
                    ),
                  ),
                ),
              )
            : const SizedBox.shrink(key: ValueKey('no_back_to_top')),
      ),
      body: Listener(
        onPointerDown: (_) {
          if (_suppressNextGlobalUnfocus) {
            _suppressNextGlobalUnfocus = false;
            return;
          }
        },
        behavior: HitTestBehavior.translucent,
        child: SafeArea(
          child: NotificationListener<ScrollNotification>(
            onNotification: (notification) {
              final bool shouldShow = notification.metrics.pixels > 400;
              if (shouldShow != _showBackToTop) {
                setState(() => _showBackToTop = shouldShow);
              }
              return false;
            },
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
                        buildHeader(),
                        buildSearchForm(),
                        const SizedBox(height: 16),
                        if ((_results.isNotEmpty || _isLoading) &&
                            _hasRankingCriteria)
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
                                      isLoading: _isLoading,
                                      totalNames: _isLoading
                                          ? 0
                                          : stats['totalNames'] as int,
                                      excellentNames: _isLoading
                                          ? '0'
                                          : stats['excellentNames'].toString(),
                                      numerologyGood: _isLoading
                                          ? '0'
                                          : stats['numerologyGood'].toString(),
                                      shadowGood: _isLoading
                                          ? '0'
                                          : stats['shadowGood'].toString(),
                                      isSatActive: _filterSat,
                                      isShaActive: _filterSha,
                                      recommendedDays: _isLoading
                                          ? null
                                          : stats['recommendedDays'] as String?,
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
                if (_isSearchTimeoutPending)
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: buildSearchTimeoutState(),
                    ),
                  ),
                // Show Magic Loading when searching (Global)
                if (_isLoading)
                  SliverToBoxAdapter(
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 40),
                      child: Center(
                        child: MagicLoadingView(
                          message: "กำลังค้นหาและจัดลำดับด้วยมนตรา...",
                          subtitle:
                              "ระบบ AI กำลังวิเคราะห์พลังชื่อและรากศัพท์ที่เหมาะกับคุณ",
                          textColor: AppColors.textLight,
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
                else if (_results.isNotEmpty && _hasRankingCriteria) ...[
                  if (_isRelaxedSearch && _relaxedFiltersNotice != null)
                    SliverToBoxAdapter(
                      child: Container(
                        margin: const EdgeInsets.fromLTRB(20, 0, 20, 12),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 10,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(
                            0xFFF59E0B,
                          ).withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                            color: const Color(
                              0xFFF59E0B,
                            ).withValues(alpha: 0.3),
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

                      final card = NameListItem(
                        key: ValueKey(
                          'matching_${_keywordController.text.hashCode}_${item.name}_${_filterSat}_${_filterSha}',
                        ),
                        rank: index + 1,
                        result: item,
                        comparisonName: "",
                        comparisonAnalysis: null,
                        showMatching: false,
                        isFilterSatActive: _filterSat,
                        isFilterShaActive: _filterSha,
                        isFilterKakiActive: _filterKaki,
                        onTap: () {
                          final name = item.name;
                          setState(() {
                            _setKeywordWithoutTriggeringListener(name);
                            _keywordController.selection =
                                TextSelection.collapsed(offset: name.length);

                            // Once a specific name is selected, the "Idea" highlight has served its discovery purpose.
                            _selectedExampleIndex = null;
                            _selectedCelebrityIndex = null;
                          });

                          // Perform re-analysis by searching for this name
                          _search(
                            scrollToResults: false,
                            showInputSnack: false,
                          );
                          maybeScrollToSearchField();
                        },
                      );

                      return card;
                    }, childCount: _results.length),
                  ),
                ],
                SliverToBoxAdapter(child: buildFooter()),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // ANCHOR: header first (ส่วนของ Header แรก)
  Widget buildHeader() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 0, horizontal: 0),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF3E0), // Light orange background
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            // ANCHOR: header second (ส่วนของ Header ที่สอง)
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 0, horizontal: 12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEDD5),
                borderRadius: BorderRadius.circular(12),
              ),
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
                      color: Color(0xFFFF9800), // Change header color to orange
                      letterSpacing: -0.8,
                      shadows: [
                        Shadow(
                          color: Colors.black.withValues(alpha: 0.1),
                          blurRadius: 4,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 0),
                  Text(
                    "เปลี่ยนชีวิตด้วยแรงดึงดูดจากชื่อดี",
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
          ),
          // ANCHOR: save name button (ปุ่มบันทึกชื่อ)
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: GestureDetector(
              onTap: () async {
                final selectedName = await Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => const SavedNamesScreen(),
                  ),
                );

                if (!mounted) return;
                if (selectedName is Map) {
                  final dynamic nameValue = selectedName['name'];
                  final dynamic modeValue = selectedName['mode'];
                  final dynamic meaningValue = selectedName['meaning'];
                  final name = nameValue is String ? nameValue : null;
                  final mode = modeValue is String ? modeValue : null;
                  final meaning = meaningValue is String ? meaningValue : null;
                  if (name == null) return;

                  if (mode == 'keyword') {
                    setState(() {
                      _hideSelectedMeaningCard = false;
                      _setKeywordWithoutTriggeringListener(name);
                    });
                    _search(scrollToResults: false);
                    return;
                  }

                  if (mode == 'decode') {
                    setState(() {
                      _hideSelectedMeaningCard = false;
                      _setKeywordWithoutTriggeringListener(name);
                    });
                    _search(scrollToResults: false);
                    return;
                  }

                  if (mode == 'semantic') {
                    final semanticQuery = meaning ?? name;
                    setState(() {
                      _hideSelectedMeaningCard = true;
                      _setKeywordWithoutTriggeringListener(semanticQuery);
                      _selectedExampleIndex = null;
                      _selectedCelebrityIndex = null;
                      _selectedNameMeaningName = semanticQuery;
                      _selectedNameMeaning = meaning;
                      _selectedNameAnalysis = null;
                      _isLoadingSelectedNameMeaning = false;
                      _nameSuggestions = null;
                      _loadingSuggestions = true;
                    });
                    fetchNameSuggestions(semanticQuery, meaning: meaning);
                    _search(
                      scrollToResults: false,
                      reloadSelectedName: false,
                      overrideKeyword: semanticQuery,
                    );
                    return;
                  }

                  if (mode == 'analyze') {
                    setState(() {
                      _hideSelectedMeaningCard = false;
                      _setKeywordWithoutTriggeringListener(name);
                      _selectedExampleIndex = null;
                      _selectedCelebrityIndex = null;
                    });
                    _search(scrollToResults: false);
                    return;
                  }

                  setState(() {
                    _hideSelectedMeaningCard = false;
                    _setKeywordWithoutTriggeringListener(name);
                  });
                  _search(scrollToResults: false);
                } else if (selectedName != null && selectedName is String) {
                  setState(() {
                    _hideSelectedMeaningCard = false;
                    _setKeywordWithoutTriggeringListener(selectedName);
                  });
                  _search(scrollToResults: false);
                }
              },
              child: Container(
                padding: const EdgeInsets.fromLTRB(4, 4, 12, 4),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      const Color(0xFF10B981).withValues(alpha: 0.12),
                      const Color(0xFF10B981).withValues(alpha: 0.04),
                    ],
                  ),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: const Color(0xFF10B981).withValues(alpha: 0.25),
                    width: 1,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFF10B981).withValues(alpha: 0.05),
                      blurRadius: 12,
                      spreadRadius: 1,
                    ),
                  ],
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    SparklingGoldHeart(
                      savedCount: ApiService.savedNamesCache.length,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      "ชื่อที่บันทึกไว้",
                      style: GoogleFonts.prompt(
                        color: const Color(0xFF059669),
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget buildErrorState(String message) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF7ED), // Warm Amber background (Avoid Red)
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.orangeAccent.withValues(alpha: 0.3)),
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
              onPressed: _isLoading
                  ? null
                  : () => _search(scrollToResults: false),
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

  Widget buildSearchTimeoutState() {
    final bool isDoubleGoodMode = _filterSat && _filterSha;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEB),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: const Color(0xFFF4C95D).withValues(alpha: 0.45),
        ),
      ),
      child: Column(
        children: [
          const Icon(
            Icons.hourglass_top_rounded,
            color: Color(0xFFD4A017),
            size: 32,
          ),
          const SizedBox(height: 12),
          Text(
            isDoubleGoodMode
                ? "ระบบกำลังประมวลผลชื่อคุณภาพสูง กรุณารอสักครู่..."
                : "กำลังค้นหาชื่อเพิ่มเติม...",
            style: GoogleFonts.sarabun(
              color: AppColors.textLight,
              fontWeight: FontWeight.bold,
              fontSize: 16,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            "หากต้องการให้ระบบลองค้นหาอีกครั้ง สามารถกดปุ่มด้านล่างได้เลย",
            style: GoogleFonts.sarabun(color: AppColors.textGray, fontSize: 14),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: _isLoading
                  ? null
                  : () => _search(scrollToResults: false),
              icon: const Icon(Icons.refresh_rounded),
              label: Text(
                "ค้นหาอีกครั้ง",
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
    final bool needsRankingCriteria = !_hasRankingCriteria;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.inputBackground,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.25)),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: 0.05),
            blurRadius: 16,
            spreadRadius: 2,
          ),
        ],
      ),
      child: Column(
        children: [
          Icon(
            Icons.search_off_rounded,
            color: AppColors.primary.withValues(alpha: 0.6),
            size: 40,
          ),
          const SizedBox(height: 10),
          Text(
            needsRankingCriteria
                ? "เลือกเงื่อนไขจัดลำดับตามตำราก่อน"
                : "เงื่อนไขที่คุณเลือกไม่มีรายชื่อ",
            style: GoogleFonts.sarabun(
              color: AppColors.textLight,
              fontWeight: FontWeight.w700,
              fontSize: 16,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 14),
          Text(
            needsRankingCriteria
                ? "ระบบจะแสดงรายชื่อเมื่อคุณเลือก `เลขศาสตร์ดี` หรือ `พลังเงาดี` เพื่อใช้เป็นเกณฑ์คัดกรองและจัดลำดับ"
                : "ลองปรับเงื่อนไข หรือปิดบางฟิลเตอร์ แล้วค้นหาอีกครั้ง",
            style: GoogleFonts.sarabun(
              color: AppColors.textGray,
              fontSize: 14,
              fontWeight: FontWeight.w500,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }

  Widget buildSelectedNameMeaningUnderKeyword() {
    if (_isPivotingIdea) return const SizedBox.shrink();
    if (_selectedNameMeaningName == null) return const SizedBox.shrink();
    if (_hideSelectedMeaningCard) return const SizedBox.shrink();

    if (_isLoadingSelectedNameMeaning) return const SizedBox.shrink();
    if (_selectedNameMeaning == null && _selectedNameAnalysis == null) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_selectedNameMeaning != null)
            Padding(
              padding: const EdgeInsets.only(
                top: 4,
                bottom: 8,
              ), // เพิ่มระยะห่างเว้นจากช่องค้นหา
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.bgDarker, // Elegant themed background
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: AppColors.accent.withValues(alpha: 0.3),
                    width: 1.5,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.05),
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
                            color: AppColors.primary.withValues(alpha: 0.1),
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
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Expanded(
                                    child: Text.rich(
                                      TextSpan(
                                        text: "ความหมายของชื่อ ",
                                        style: GoogleFonts.sarabun(
                                          color: const Color(
                                            0xFF3D2600,
                                          ).withValues(alpha: 0.6),
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
                                  ),
                                  const SizedBox(width: 10),
                                  _buildSpeechIconButton(
                                    icon:
                                        _isSpeakingKey(
                                          _selectedNameMeaningName
                                                      ?.trim()
                                                      .isNotEmpty ??
                                                  false
                                              ? 'selected-meaning:${_selectedNameMeaningName!.trim()}'
                                              : 'selected-meaning',
                                        )
                                        ? Icons.volume_up_rounded
                                        : Icons.record_voice_over_rounded,
                                    onTap: _speakSelectedMeaning,
                                    tooltip: 'ฟังความหมายของชื่อ',
                                    variant: SpeechButtonVariant.secondary,
                                  ),
                                ],
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
                                color: AppColors.secondary.withValues(
                                  alpha: 0.3,
                                ),
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
                                    //ANCHOR: OriginWord (รากศัพท์)
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
                        color: const Color(0xFF111827).withValues(alpha: 0.18),
                        borderRadius: BorderRadius.circular(8),
                        elevation: 2,
                        shadowColor: Colors.black.withValues(alpha: 0.12),
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
                                  color: Colors.white.withValues(alpha: 0.85),
                                  size: 12,
                                ),
                                const SizedBox(width: 4),
                                Text(
                                  "หมายเหตุ",
                                  style: GoogleFonts.prompt(
                                    color: Colors.white.withValues(alpha: 0.9),
                                    fontSize: 10,
                                    fontWeight: FontWeight.w700,
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
      ),
    );
  }

  // ANCHOR: NO.1 find names user what (NO.1 ค้นหาชื่อตามความหมายที่ต้องการ)
  Widget buildSearchForm() {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(0),
        border: Border.all(
          color: AppColors.primary.withValues(alpha: 0.1),
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_celebrities.isNotEmpty) ...[
            const SizedBox(height: 14),
            buildCelebritiesRow(),
          ],
          Padding(
            padding: EdgeInsets.fromLTRB(
              12,
              _celebrities.isNotEmpty ? 16 : 14,
              12,
              10,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                buildSearchField(),
                AnimatedSize(
                  duration: const Duration(milliseconds: 220),
                  curve: Curves.easeOutCubic,
                  alignment: Alignment.topCenter,
                  child: buildSelectedNameMeaningUnderKeyword(),
                ),
                buildCombinedSuggestionBox(),
                const SizedBox(height: 20),
                buildExamples(),
                const SizedBox(height: 24),

                // ===== STEP 2: ตั้งค่าเพิ่มเติม (SECONDARY) =====
                buildMagicRankingHeader(),
                const SizedBox(height: 18), // Increased from 12

                buildUnifiedDayKakiCard(),
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
    final examples = _pickedExamples.isNotEmpty
        ? _pickedExamples
        : [
            {
              "text": "เศรษฐีผู้มั่งคั่ง มีทรัพย์สมบัติและบารมี",
              "icon": Icons.trending_up,
              "iconColor": const Color(0xFFD4A017),
            },
          ];

    if (examples.isEmpty) return const SizedBox.shrink();
    final item = examples.first;
    final text = item['text'] as String;
    final icon = item['icon'] as IconData;
    final iconColor = item['iconColor'] as Color? ?? AppColors.accent;
    final bool isActive = _selectedExampleText == text;

    return Container(
      decoration: BoxDecoration(
        color: AppColors.inputBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.12)),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: 0.05),
            blurRadius: 16,
            spreadRadius: 1,
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Row(
              children: [
                Container(
                  width: 4,
                  height: 18,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'หาชื่อจากความหมาย (Semantic Search)',
                    style: GoogleFonts.sarabun(
                      color: AppColors.textLight,
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      height: 1.4,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                const SizedBox(width: 8),
                GestureDetector(
                  onTap: _shuffleNext,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      gradient: AppColors.goldGradient,
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0xFFFFD700).withValues(alpha: 0.3),
                          blurRadius: 6,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.play_arrow_rounded,
                          size: 14,
                          color: Color(0xFF4E342E),
                        ),
                        SizedBox(width: 4),
                        Text(
                          'สุ่มต่อ',
                          style: TextStyle(
                            fontFamily: 'Sarabun',
                            color: Color(0xFF4E342E),
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 600),
              switchInCurve: Curves.easeOutBack,
              switchOutCurve: Curves.easeIn,
              transitionBuilder: (Widget child, Animation<double> animation) {
                final slideAnimation = Tween<Offset>(
                  begin: const Offset(0.15, 0.0),
                  end: Offset.zero,
                ).animate(animation);
                return FadeTransition(
                  opacity: animation,
                  child: SlideTransition(
                    position: slideAnimation,
                    child: child,
                  ),
                );
              },
              child: Container(
                key: ValueKey<String>(text),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(20),
                  gradient: isActive ? AppColors.goldGradient : null,
                ),
                padding: isActive ? const EdgeInsets.all(2) : EdgeInsets.zero,
                child: Container(
                  decoration: BoxDecoration(
                    color: isActive
                        ? const Color(0xFFFFFDF5)
                        : Colors.white.withValues(alpha: 0.85),
                    borderRadius: BorderRadius.circular(isActive ? 18 : 20),
                    border: isActive
                        ? null
                        : Border.all(
                            color: Colors.black.withValues(alpha: 0.08),
                            width: 1.0,
                          ),
                    boxShadow: [
                      BoxShadow(
                        color: isActive
                            ? const Color(0xFFFFD700).withValues(alpha: 0.15)
                            : Colors.black.withValues(alpha: 0.03),
                        blurRadius: isActive ? 16 : 8,
                        spreadRadius: isActive ? 1 : 0,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: () {
                        setState(() {
                          if (isActive) {
                            _selectedExampleIndex = null;
                            _selectedExampleText = null;
                            _keywordController.clear();
                            _startShuffleTimer();
                          } else {
                            _setKeywordWithoutTriggeringListener(text);
                            _selectedExampleIndex = 0;
                            _selectedExampleText = text;
                            _selectedCelebrityIndex = null;
                            _filterSat = false;
                            _filterSha = true;
                          }
                        });
                        _search();
                        maybeScrollToSearchField();
                      },
                      borderRadius: BorderRadius.circular(isActive ? 18 : 20),
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 16,
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  colors: [
                                    iconColor.withValues(alpha: 0.18),
                                    iconColor.withValues(alpha: 0.05),
                                  ],
                                  begin: Alignment.topLeft,
                                  end: Alignment.bottomRight,
                                ),
                                shape: BoxShape.circle,
                              ),
                              child: Icon(
                                icon,
                                size: 22,
                                color: iconColor,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Text(
                                text,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: GoogleFonts.sarabun(
                                  color: isActive
                                      ? const Color(0xFF5C3C10)
                                      : AppColors.textLight.withValues(
                                          alpha: 0.85,
                                        ),
                                  fontSize: 14.5,
                                  fontWeight: isActive
                                      ? FontWeight.w800
                                      : FontWeight.w600,
                                  height: 1.3,
                                ),
                              ),
                            ),
                            if (isActive) ...[
                              const SizedBox(width: 10),
                              Container(
                                padding: const EdgeInsets.all(4),
                                decoration: const BoxDecoration(
                                  shape: BoxShape.circle,
                                  gradient: AppColors.goldGradient,
                                ),
                                child: const Icon(
                                  Icons.check_rounded,
                                  color: Color(0xFF5C3C10),
                                  size: 14,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget buildCelebritiesRow() {
    if (_celebrities.isEmpty) return const SizedBox.shrink();

    const double celebItemWidth = 70;
    final double loopWidth = _celebrities.length * celebItemWidth;
    final double visualOffset = loopWidth <= 0
        ? 0.0
        : _celebsScrollPos % loopWidth;
    final double initialScrollOffset = visualOffset;

    Widget buildCelebrityItem(int virtualIndex) {
      final int modIndex = virtualIndex % _celebrities.length;
      final Map<String, dynamic> c = _celebrities[modIndex];
      final String name = c['name'] ?? '';
      final String initial = c['initial'] ?? '';
      final String avatarUrl = c['avatar_url'] ?? '';

      Color bgColor =
          AppColors.avatarBorders[modIndex % AppColors.avatarBorders.length];
      if (c['color'] != null) {
        try {
          String hex = c['color'].replaceAll('#', '');
          if (hex.length == 6) hex = 'FF$hex';
          bgColor = Color(int.parse(hex, radix: 16));
        } catch (_) {}
      }

      final bool isSelected = _selectedCelebrityIndex == modIndex;

      return Padding(
        padding: const EdgeInsets.only(top: 4, bottom: 4),
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTapDown: (_) {
            stopMarqueeTicker();
            _celebsResumeRequestId++;
          },
          onTapCancel: resumeCelebsAutoScrollAfterDelay,
          onTap: () {
            stopMarqueeTicker();
            setState(() {
              _setKeywordWithoutTriggeringListener(name);
              _selectedCelebrityIndex = modIndex;
              _selectedExampleIndex = null;
            });
            _resolveSeedName(name);
            resumeCelebsAutoScrollAfterDelay();

            Future.delayed(const Duration(milliseconds: 600), () {
              if (mounted) {
                _search(scrollToResults: false);
                maybeScrollToSearchField();
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
                    color: bgColor.withValues(alpha: 0.15),
                    border: Border.all(
                      color: isSelected ? AppColors.accent : bgColor,
                      width: isSelected ? 3.5 : 2.5,
                    ),
                    boxShadow: isSelected
                        ? [
                            BoxShadow(
                              color: const Color(
                                0xFFD4AF37,
                              ).withValues(alpha: 0.4),
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
                            color: Colors.black.withValues(alpha: 0.4),
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
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final double viewportWidth = constraints.maxWidth.isFinite
            ? constraints.maxWidth
            : MediaQuery.of(context).size.width;
        final int repeatCount = math.max(
          3,
          (viewportWidth / loopWidth).ceil() + 3,
        );

        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onHorizontalDragStart: (_) {
            stopMarqueeTicker();
            _celebsResumeRequestId++;
          },
          onHorizontalDragUpdate: (details) {
            setState(() {
              final double maxScroll = _celebrities.length * celebItemWidth;
              if (maxScroll > 0) {
                _celebsScrollPos = (_celebsScrollPos - details.delta.dx);
                // Keep it in bounds [0, maxScroll]
                _celebsScrollPos = _celebsScrollPos % maxScroll;
                // Sync animation controller's value to current scroll ratio to prevent jumping
                _celebsAnimController.value = _celebsScrollPos / maxScroll;
              }
            });
          },
          onHorizontalDragEnd: (_) {
            resumeCelebsAutoScrollAfterDelay();
          },
          child: SizedBox(
            height: 98,
            width: double.infinity,
            child: ClipRect(
              child: Stack(
                clipBehavior: Clip.hardEdge,
                children: List.generate(_celebrities.length * repeatCount, (
                  virtualIndex,
                ) {
                  final double left =
                      (virtualIndex * celebItemWidth) - initialScrollOffset;
                  return Positioned(
                    left: left,
                    top: 0,
                    width: celebItemWidth,
                    height: 98,
                    child: buildCelebrityItem(virtualIndex),
                  );
                }),
              ),
            ),
          ),
        );
      },
    );
  }

  // _buildAutocompleteField removed

  Widget buildSearchField() {
    const hint = "พิมพ์ความหมายหรือชื่อที่ต้องการค้นหา";

    return Column(
      key: _searchFieldKey,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ValueListenableBuilder<TextEditingValue>(
          valueListenable: _keywordController,
          builder: (context, value, child) {
            final isFocused = _searchFocusNode.hasFocus;
            final hasText = value.text.trim().isNotEmpty;
            final isSubmitBusy = _isSubmittingSearch;

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
                            : const Color(0xFFD946EF).withValues(alpha: 0.35)),
                  width: hasText ? 3.0 : (isFocused ? 2.5 : 1.5),
                ),
                boxShadow: [
                  // Outer Aura
                  BoxShadow(
                    color: const Color(0xFFD946EF).withValues(
                      alpha: hasText ? 0.42 : (isFocused ? 0.24 : 0.1),
                    ),
                    blurRadius: hasText ? 50 : (isFocused ? 25 : 15),
                    offset: const Offset(0, 10),
                    spreadRadius: hasText ? 8 : (isFocused ? 2 : 0),
                  ),
                  // Inner Glow (only when has text)
                  if (hasText)
                    BoxShadow(
                      color: const Color(0xFFB517FF).withValues(alpha: 0.24),
                      blurRadius: 20,
                      spreadRadius: -1,
                    ),
                  // High-intensity core glow
                  if (hasText)
                    BoxShadow(
                      color: const Color(0xFFFF4FA3).withValues(alpha: 0.28),
                      blurRadius: 10,
                      spreadRadius: -4,
                    ),
                ],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(left: 18, right: 10),
                        child: TweenAnimationBuilder<double>(
                          tween: Tween<double>(
                            begin: 0,
                            end: hasText ? 1.0 : 0.0,
                          ),
                          duration: const Duration(milliseconds: 500),
                          builder: (context, value, child) {
                            return Icon(
                              Icons.auto_awesome_rounded,
                              color: Color.lerp(
                                isFocused
                                    ? const Color(0xFFFF4FA3)
                                    : const Color(
                                        0xFFFF4FA3,
                                      ).withValues(alpha: 0.55),
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
                              color: const Color(
                                0xFF7E22CE,
                              ).withValues(alpha: 0.45),
                              fontSize: 15,
                              fontWeight: FontWeight.w500,
                            ),
                            border: InputBorder.none,
                            contentPadding: const EdgeInsets.symmetric(
                              vertical: 18,
                            ),
                          ),
                          scrollPadding: const EdgeInsets.only(bottom: 260),
                          onTap: maybeScrollToSearchField,
                          onSubmitted: (_) {
                            commitSearchInput();
                          },
                        ),
                      ),
                      if (hasText)
                        IconButton(
                          icon: Icon(
                            Icons.cancel_rounded,
                            color: const Color(
                              0xFF7E22CE,
                            ).withValues(alpha: 0.4),
                            size: 22,
                          ),
                          onPressed: () {
                            _keywordController.clear();
                            setState(() {
                              _selectedExampleIndex = null;
                              _selectedCelebrityIndex = null;
                            });
                          },
                        ),
                      const SizedBox(width: 8),
                    ],
                  ),
                  if (hasText)
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.fromLTRB(16, 0, 12, 12),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  _nameIntentResult?.isMeaning == true
                                      ? "ระบบจะค้นหาชื่อที่มีความหมายใกล้เคียง"
                                      : "ระบบจะวิเคราะห์ชื่อและถอดเลขศาสตร์ให้",
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: GoogleFonts.sarabun(
                                    color: const Color(
                                      0xFF7E22CE,
                                    ).withValues(alpha: 0.55),
                                    fontSize: 12,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                if (_inputClassification != null) ...[
                                  const SizedBox(height: 2),
                                  Text(
                                    "ข้อมูลที่ตรวจพบ: ${_getThaiInputType(_inputClassification!.type)}",
                                    style: GoogleFonts.sarabun(
                                      color: _getThaiInputTypeColor(
                                        _inputClassification!.type,
                                      ),
                                      fontSize: 11,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                          const SizedBox(width: 10),
                          Material(
                            color: Colors.transparent,
                            child: InkWell(
                              onTap: isSubmitBusy ? null : commitSearchInput,
                              borderRadius: BorderRadius.circular(999),
                              child: Ink(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 9,
                                ),
                                decoration: BoxDecoration(
                                  gradient: const LinearGradient(
                                    colors: [
                                      Color(0xFFFF4FA3),
                                      Color(0xFFB517FF),
                                    ],
                                  ),
                                  borderRadius: BorderRadius.circular(999),
                                  boxShadow: [
                                    BoxShadow(
                                      color: const Color(
                                        0xFFB517FF,
                                      ).withValues(alpha: 0.22),
                                      blurRadius: 10,
                                      offset: const Offset(0, 4),
                                    ),
                                  ],
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    if (isSubmitBusy)
                                      const SizedBox(
                                        width: 14,
                                        height: 14,
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2,
                                          valueColor:
                                              AlwaysStoppedAnimation<Color>(
                                                Colors.white,
                                              ),
                                        ),
                                      )
                                    else
                                      const Icon(
                                        Icons.auto_awesome_rounded,
                                        color: Colors.white,
                                        size: 14,
                                      ),
                                    const SizedBox(width: 6),
                                    Text(
                                      _inputActionLabel(isBusy: isSubmitBusy),
                                      style: GoogleFonts.prompt(
                                        color: Colors.white,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w700,
                                        letterSpacing: 0.1,
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
                ],
              ),
            );
          },
        ),
        const SizedBox(height: 12),
        buildBirthdayBadges(),
      ],
    );
  }

  Widget buildBirthdayBadges() {
    final Map<String, Map<String, dynamic>> badgeConfigs = {
      'Sunday': {
        'name': 'อาทิตย์',
        'color': const Color(0xFFFF9500),
        'gradient': const LinearGradient(
          colors: [Color(0xFFFFB300), Color(0xFFFF9500)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFFFF7E6),
        'textColor': const Color(0xFFE65100),
      },
      'Monday': {
        'name': 'จันทร์',
        'color': const Color(0xFFFFCC00),
        'gradient': const LinearGradient(
          colors: [Color(0xFFFFF59D), Color(0xFFFFD54F)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFFFFDE7),
        'textColor': const Color(0xFF8D6E63),
      },
      'Tuesday': {
        'name': 'อังคาร',
        'color': const Color(0xFFD946EF),
        'gradient': const LinearGradient(
          colors: [Color(0xFFF472B6), Color(0xFFD946EF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFFDF4FF),
        'textColor': const Color(0xFF86198F),
      },
      'Wednesday1': {
        'name': 'พุธ (กลางวัน)',
        'color': const Color(0xFF34C759),
        'gradient': const LinearGradient(
          colors: [Color(0xFF81C784), Color(0xFF34C759)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFE8F5E9),
        'textColor': const Color(0xFF2E7D32),
      },
      'Wednesday2': {
        'name': 'พุธ (กลางคืน)',
        'color': const Color(0xFF007A7C),
        'gradient': const LinearGradient(
          colors: [Color(0xFF26A69A), Color(0xFF007A7C)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFE0F2F1),
        'textColor': const Color(0xFF004D40),
      },
      'Thursday': {
        'name': 'พฤหัสฯ',
        'color': const Color(0xFFFF9500),
        'gradient': const LinearGradient(
          colors: [Color(0xFFFFB74D), Color(0xFFFF9500)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFFFF3E0),
        'textColor': const Color(0xFFE65100),
      },
      'Friday': {
        'name': 'ศุกร์',
        'color': const Color(0xFF5AC8FA),
        'gradient': const LinearGradient(
          colors: [Color(0xFF90CAF9), Color(0xFF007AFF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFE3F2FD),
        'textColor': const Color(0xFF0D47A1),
      },
      'Saturday': {
        'name': 'เสาร์',
        'color': const Color(0xFF5856D6),
        'gradient': const LinearGradient(
          colors: [Color(0xFFB39DDB), Color(0xFF5856D6)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'badgeColor': const Color(0xFFF3E5F5),
        'textColor': const Color(0xFF4A148C),
      },
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 6, bottom: 8, top: 4),
          child: Row(
            children: [
              const Icon(
                Icons.auto_awesome_rounded,
                color: Color(0xFFD946EF),
                size: 14,
              ),
              const SizedBox(width: 5),
              Text(
                "วันเกิดของคุณ เพื่อคัดชื่อกาลกิณีออก",
                style: GoogleFonts.prompt(
                  color: const Color(0xFF7E22CE).withValues(alpha: 0.8),
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          physics: const BouncingScrollPhysics(),
          child: Row(
            children: _days.map((day) {
              final isSelected = _selectedDay == day;
              final config = badgeConfigs[day]!;
              final dayColor = config['color'] as Color;
              final dayGradient = config['gradient'] as Gradient;
              final badgeColor = config['badgeColor'] as Color;
              final textColor = config['textColor'] as Color;
              final dayName = config['name'] as String;

              return Padding(
                padding: const EdgeInsets.only(right: 8, bottom: 4),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 250),
                  curve: Curves.easeOutBack,
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: () {
                        setState(() {
                          if (isSelected) {
                            _selectedDay = null;
                            _filterKaki = false;
                          } else {
                            _selectedDay = day;
                            _filterKaki = true;
                          }
                        });
                        unawaited(_refreshResultsKeepingStep2Anchor());
                      },
                      borderRadius: BorderRadius.circular(999),
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 250),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 9,
                        ),
                        decoration: BoxDecoration(
                          gradient: isSelected ? dayGradient : null,
                          color: isSelected ? null : badgeColor.withValues(alpha: 0.7),
                          borderRadius: BorderRadius.circular(999),
                          border: Border.all(
                            color: isSelected
                                ? dayColor.withValues(alpha: 0.8)
                                : dayColor.withValues(alpha: 0.25),
                            width: isSelected ? 2 : 1.2,
                          ),
                          boxShadow: isSelected
                              ? [
                                  BoxShadow(
                                    color: dayColor.withValues(alpha: 0.4),
                                    blurRadius: 10,
                                    offset: const Offset(0, 4),
                                    spreadRadius: 1,
                                  ),
                                ]
                              : [],
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            AnimatedContainer(
                              duration: const Duration(milliseconds: 250),
                              width: isSelected ? 16 : 0,
                              child: isSelected
                                  ? Padding(
                                      padding: const EdgeInsets.only(right: 4),
                                      child: const Icon(
                                        Icons.check_circle_rounded,
                                        color: Colors.white,
                                        size: 13,
                                      ),
                                    )
                                  : const SizedBox.shrink(),
                            ),
                            Text(
                              dayName,
                              style: GoogleFonts.prompt(
                                color: isSelected ? Colors.white : textColor,
                                fontSize: 13,
                                fontWeight: isSelected ? FontWeight.w800 : FontWeight.w700,
                              ),
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
        ),
      ],
    );
  }

  Widget buildCombinedSuggestionBox() {
    return Builder(
      builder: (context) {
        final isTyping = _keywordController.text.isNotEmpty;
        final hasNames = _nameSuggestions?.names.isNotEmpty ?? false;
        final hasIntentCandidates =
            _nameIntentResult?.isHybrid == true &&
            (_nameIntentResult?.candidates.isNotEmpty ?? false);
        final showApiSuggestions =
            isTyping &&
            ((hasNames || _loadingSuggestions) ||
                hasIntentCandidates ||
                _isLoadingNameIntent ||
                _hasSearched);

        if (!showApiSuggestions) {
          return const SizedBox.shrink();
        }

        final int suggestionCount = _nameSuggestions?.names.length ?? 0;
        final isExpanded = _isSuggestionBoxExpanded;

        return AnimatedSize(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          alignment: Alignment.topCenter,
          child: Container(
            margin: EdgeInsets.only(top: hasNames ? 10 : 14),
            width: double.infinity,
            decoration: BoxDecoration(
              color: isExpanded ? AppColors.bgDark : const Color(0xFFE8F5E9),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: isExpanded
                    ? AppColors.secondary.withValues(alpha: 0.25)
                    : AppColors.secondary.withValues(alpha: 0.45),
                width: isExpanded ? 1.5 : 2.0,
              ),
              gradient: isExpanded
                  ? LinearGradient(
                      colors: [
                        AppColors.bgDark,
                        AppColors.bgDarker.withValues(alpha: 0.5),
                      ],
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                    )
                  : const LinearGradient(
                      colors: [
                        Color(0xFFE8F5E9), // Soft green
                        Color(0xFFE0F2F1), // Soft teal
                      ],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
              boxShadow: [
                BoxShadow(
                  color: AppColors.secondary.withValues(alpha: isExpanded ? 0.06 : 0.12),
                  blurRadius: isExpanded ? 16 : 22,
                  spreadRadius: isExpanded ? -2 : 0,
                  offset: isExpanded ? const Offset(0, 6) : const Offset(0, 8),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: () {
                    setState(() {
                      _isSuggestionBoxExpanded = !_isSuggestionBoxExpanded;
                    });
                  },
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 14,
                    ),
                    child: Row(
                      children: [
                        AnimatedContainer(
                          duration: const Duration(milliseconds: 300),
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: isExpanded
                                  ? [
                                      AppColors.secondary.withValues(alpha: 0.8),
                                      AppColors.secondary,
                                    ]
                                  : [
                                      const Color(0xFF00B894),
                                      const Color(0xFF00D1B2),
                                    ],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: AppColors.secondary.withValues(alpha: 0.3),
                                blurRadius: isExpanded ? 4 : 10,
                                offset: const Offset(0, 2),
                              ),
                            ],
                          ),
                          child: Icon(
                            isExpanded
                                ? Icons.auto_awesome_rounded
                                : Icons.auto_awesome_motion_rounded,
                            color: Colors.white,
                            size: 15,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text(
                                isExpanded
                                    ? (suggestionCount > 0
                                        ? "รายชื่อที่มีความหมายใกล้เคียง ($suggestionCount)"
                                        : "รายชื่อที่มีความหมายใกล้เคียง")
                                    : "รายชื่อมงคลความหมายสอดคล้อง",
                                style: GoogleFonts.prompt(
                                  color: const Color(0xFF0F5132),
                                  fontSize: 14,
                                  fontWeight: FontWeight.w800,
                                  letterSpacing: 0.1,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                isExpanded
                                    ? "กำลังแสดงรายชื่อแนะนำที่มีความหมายพิเศษ"
                                    : (suggestionCount > 0
                                        ? "✨ ค้นพบชื่อแนะนำชั้นเลิศ $suggestionCount รายชื่อ แตะเพื่อเปิดดูพิเศษ"
                                        : "✨ ค้นพบรายชื่อแนะนำชั้นเลิศ แตะเพื่อเปิดดูพิเศษ"),
                                style: GoogleFonts.sarabun(
                                  color: isExpanded
                                      ? const Color(0xFF1E7E34).withValues(alpha: 0.75)
                                      : const Color(0xFF0F5132),
                                  fontSize: 11,
                                  fontWeight: isExpanded ? FontWeight.w600 : FontWeight.w800,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        if (_loadingSuggestions) ...[
                          const SizedBox(width: 8),
                          const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              valueColor: AlwaysStoppedAnimation<Color>(
                                AppColors.secondary,
                              ),
                            ),
                          ),
                        ],
                        const SizedBox(width: 8),
                        AnimatedContainer(
                          duration: const Duration(milliseconds: 300),
                          decoration: BoxDecoration(
                            color: AppColors.secondary.withValues(alpha: isExpanded ? 0.08 : 0.15),
                            shape: BoxShape.circle,
                          ),
                          padding: const EdgeInsets.all(5),
                          child: Icon(
                            isExpanded
                                ? Icons.keyboard_arrow_up_rounded
                                : Icons.keyboard_arrow_down_rounded,
                            color: AppColors.secondary,
                            size: 20,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                if (_isSuggestionBoxExpanded) ...[
                  const Divider(height: 1, color: Colors.white12),
                  Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (_isLoadingNameIntent) ...[
                          Row(
                            children: [
                              const Icon(
                                Icons.tune_rounded,
                                color: AppColors.primary,
                                size: 16,
                              ),
                              const SizedBox(width: 6),
                              Text(
                                "กำลังวิเคราะห์คำค้น...",
                                style: GoogleFonts.sarabun(
                                  color: AppColors.primary,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 10),
                        ] else if (hasIntentCandidates) ...[
                          Row(
                            children: [
                              const Icon(
                                Icons.auto_fix_high_rounded,
                                color: AppColors.primary,
                                size: 16,
                              ),
                              const SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  "คุณอาจหมายถึง:",
                                  style: GoogleFonts.sarabun(
                                    color: AppColors.primary,
                                    fontSize: 13,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ),
                              Text(
                                "${((_nameIntentResult?.confidence ?? 0) * 100).round()}%",
                                style: GoogleFonts.sarabun(
                                  color: AppColors.textGray,
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: _nameIntentResult!.candidates.take(5).map(
                              (candidate) {
                                return InkWell(
                                  onTap: () {
                                    _setKeywordWithoutTriggeringListener(
                                      candidate,
                                    );
                                    _keywordController.selection =
                                        TextSelection.collapsed(
                                          offset: candidate.length,
                                        );
                                    FocusScope.of(context).unfocus();
                                    _search();
                                    maybeScrollToSearchField();
                                  },
                                  borderRadius: BorderRadius.circular(999),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 12,
                                      vertical: 8,
                                    ),
                                    decoration: BoxDecoration(
                                      color: AppColors.primary.withValues(
                                        alpha: 0.12,
                                      ),
                                      borderRadius: BorderRadius.circular(999),
                                      border: Border.all(
                                        color: AppColors.primary.withValues(
                                          alpha: 0.28,
                                        ),
                                      ),
                                    ),
                                    child: Text(
                                      candidate,
                                      style: GoogleFonts.sarabun(
                                        color: AppColors.textLight,
                                        fontSize: 14,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ),
                                );
                              },
                            ).toList(),
                          ),
                          if (hasNames || _loadingSuggestions)
                            const SizedBox(height: 14),
                        ],

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
                                style: GoogleFonts.sarabun(
                                  color: AppColors.secondary,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),

                          SizedBox(height: _loadingSuggestions ? 12 : 10),
                          if (_loadingSuggestions)
                            const Center(
                              child: Padding(
                                padding: EdgeInsets.fromLTRB(8, 6, 8, 2),
                                child: MagicLoadingView(
                                  height: 64,
                                  message: "กำลังค้นหาไอเดีย...",
                                  subtitle:
                                      "AI กำลังคัดชื่อที่มีความหมายใกล้เคียงให้คุณ",
                                  textColor: AppColors.textGray,
                                  minimal: true,
                                ),
                              ),
                            )
                          else
                            // ANCHOR: LikeName (ชื่อที่มีความหมายใกล้เคียง)
                            Column(
                              children: [
                                ..._nameSuggestions!.names.map((item) {
                                  return Material(
                                    color: Colors.transparent,
                                    child: Container(
                                      width: double.infinity,
                                      padding: const EdgeInsets.symmetric(
                                        vertical: 12,
                                        horizontal: 10,
                                      ),
                                      margin: const EdgeInsets.only(bottom: 8),
                                      decoration: BoxDecoration(
                                        color: Colors.white.withValues(
                                          alpha: 0.92,
                                        ),
                                        borderRadius: BorderRadius.circular(12),
                                        border: Border.all(
                                          color: AppColors.primary.withValues(
                                            alpha: 0.12,
                                          ),
                                        ),
                                        boxShadow: [
                                          BoxShadow(
                                            color: AppColors.primary.withValues(
                                              alpha: 0.04,
                                            ),
                                            blurRadius: 10,
                                            offset: const Offset(0, 3),
                                          ),
                                        ],
                                      ),
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Row(
                                            crossAxisAlignment:
                                                CrossAxisAlignment.start,
                                            children: [
                                              Expanded(
                                                child: Column(
                                                  crossAxisAlignment:
                                                      CrossAxisAlignment.start,
                                                  children: [
                                                    Row(
                                                      crossAxisAlignment:
                                                          CrossAxisAlignment
                                                              .start,
                                                      children: [
                                                        Expanded(
                                                          child: Text(
                                                            item.name,
                                                            style: GoogleFonts.sarabun(
                                                              color: AppColors
                                                                  .textLight,
                                                              fontSize: 16,
                                                              fontWeight:
                                                                  FontWeight
                                                                      .bold,
                                                            ),
                                                          ),
                                                        ),
                                                        const SizedBox(
                                                          width: 8,
                                                        ),
                                                        _buildSpeechIconButton(
                                                          icon:
                                                              _isSpeakingKey(
                                                                'name+meaning:${item.id}',
                                                              )
                                                              ? Icons
                                                                    .volume_up_rounded
                                                              : Icons
                                                                    .mic_rounded,
                                                          onTap: () =>
                                                              _speakNameAndMeaning(
                                                                item,
                                                              ),
                                                          tooltip:
                                                              'ฟังชื่อและความหมาย',
                                                          variant:
                                                              SpeechButtonVariant
                                                                  .primary,
                                                        ),
                                                      ],
                                                    ),
                                                    const SizedBox(height: 3),
                                                    Text(
                                                      item.meaning,
                                                      style:
                                                          GoogleFonts.sarabun(
                                                            color: AppColors
                                                                .textGray,
                                                            fontSize: 13,
                                                          ),
                                                    ),
                                                  ],
                                                ),
                                              ),
                                              const SizedBox(width: 12),
                                              _buildActionIconButton(
                                                icon:
                                                    Icons.chevron_right_rounded,
                                                onTap: () {
                                                  _setKeywordWithoutTriggeringListener(
                                                    item.name,
                                                  );
                                                  _keywordController.selection =
                                                      TextSelection.collapsed(
                                                        offset:
                                                            item.name.length,
                                                      );
                                                  _resolveSeedName(
                                                    item.name,
                                                    meaningHint: item.meaning,
                                                  );
                                                  FocusScope.of(
                                                    context,
                                                  ).unfocus();
                                                  _search();
                                                  maybeScrollToSearchField();
                                                },
                                                tooltip: 'เลือกชื่อนี้',
                                              ),
                                            ],
                                          ),
                                          const SizedBox(height: 8),
                                          Container(
                                            width: double.infinity,
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: 12,
                                              vertical: 10,
                                            ),
                                            decoration: BoxDecoration(
                                              color: item.phoneticScore != null
                                                  ? AppColors.secondary
                                                        .withValues(alpha: 0.08)
                                                  : AppColors.primary
                                                        .withValues(
                                                          alpha: 0.06,
                                                        ),
                                              borderRadius:
                                                  BorderRadius.circular(10),
                                              border: Border.all(
                                                color:
                                                    item.phoneticScore != null
                                                    ? AppColors.secondary
                                                          .withValues(
                                                            alpha: 0.12,
                                                          )
                                                    : AppColors.primary
                                                          .withValues(
                                                            alpha: 0.12,
                                                          ),
                                              ),
                                            ),
                                            child: Row(
                                              crossAxisAlignment:
                                                  CrossAxisAlignment.start,
                                              children: [
                                                Expanded(
                                                  child: Text(
                                                    item.phoneticScore != null
                                                        ? _buildPhoneticHighlight(
                                                            item,
                                                          )
                                                        : _buildSuggestionTrustLine(
                                                            item,
                                                          ),
                                                    style: GoogleFonts.sarabun(
                                                      color:
                                                          item.phoneticScore !=
                                                              null
                                                          ? const Color(
                                                              0xFF1E4D47,
                                                            )
                                                          : AppColors.textLight,
                                                      fontSize: 12,
                                                      fontWeight:
                                                          FontWeight.w600,
                                                      height: 1.3,
                                                    ),
                                                  ),
                                                ),
                                                const SizedBox(width: 8),
                                                _buildSpeechIconButton(
                                                  icon:
                                                      _isSpeakingKey(
                                                        'phonetic:${item.id}',
                                                      )
                                                      ? Icons.volume_up_rounded
                                                      : Icons
                                                            .record_voice_over_rounded,
                                                  onTap: () =>
                                                      _speakPhonetic(item),
                                                  tooltip: 'ฟังการออกเสียงชื่อ',
                                                  variant: SpeechButtonVariant
                                                      .secondary,
                                                ),
                                              ],
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  );
                                }),
                              ],
                            ),
                        ],
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }

  // ignore: unused_element
  Widget _buildSuggestionMetricChip(String label, int value, {Color? color}) {
    final chipColor = color ?? AppColors.primary;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: chipColor.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: chipColor.withValues(alpha: 0.18)),
      ),
      child: Text(
        '$label $value',
        style: GoogleFonts.sarabun(
          color: chipColor,
          fontSize: 11,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }

  String _buildPhoneticHighlight(SuggestionNameItem item) {
    if (item.phoneticSummary.trim().isNotEmpty) {
      return item.phoneticSummary.trim();
    }

    final score = item.phoneticScore ?? 0;
    final ease = item.pronunciationEase ?? score;
    final euphony = item.euphonyScore ?? score;
    final rhythm = item.rhythmScore ?? score;

    if (score >= 94 && euphony >= 92 && rhythm >= 90) {
      return 'โทนเสียงละมุน นุ่มลึก และจังหวะลงตัว ฟังแล้วติดหูมาก';
    }
    if (ease >= 92 && euphony >= 88) {
      return 'ออกเสียงลื่น ปากเปิดง่าย และน้ำเสียงฟังนุ่มละมุน';
    }
    if (rhythm >= 90 && score >= 88) {
      return 'น้ำหนักเสียงแน่น จังหวะดี เรียกแล้วฟังชัดและมีพลัง';
    }
    if (euphony >= 88) {
      return 'เสียงค่อนข้างหวาน ละมุนหู และฟังราบรื่นต่อเนื่อง';
    }
    if (ease >= 86) {
      return 'ออกเสียงง่าย ฟังลื่น และเรียกใช้ได้สบายในชีวิตประจำวัน';
    }
    if (rhythm >= 84) {
      return 'จังหวะเสียงดี โทนค่อนข้างแน่น เรียกแล้วจำง่าย';
    }
    return 'โทนเสียงค่อนข้างเรียบลื่น ฟังง่าย และใช้งานได้ดี';
  }

  Widget _buildSpeechIconButton({
    required IconData icon,
    required VoidCallback onTap,
    required String tooltip,
    SpeechButtonVariant variant = SpeechButtonVariant.primary,
  }) {
    final bool isPrimary = variant == SpeechButtonVariant.primary;
    final Color backgroundColor = isPrimary
        ? AppColors.secondary.withValues(alpha: 0.1)
        : AppColors.primary.withValues(alpha: 0.05);
    final Color borderColor = isPrimary
        ? AppColors.secondary.withValues(alpha: 0.18)
        : AppColors.primary.withValues(alpha: 0.1);
    final Color iconColor = isPrimary
        ? AppColors.secondary
        : AppColors.textGray.withValues(alpha: 0.9);

    return Tooltip(
      message: tooltip,
      child: Material(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(999),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(999),
          child: Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(999),
              border: Border.all(color: borderColor),
            ),
            child: Icon(icon, size: 18, color: iconColor),
          ),
        ),
      ),
    );
  }

  Widget _buildActionIconButton({
    required IconData icon,
    required VoidCallback onTap,
    required String tooltip,
  }) {
    return Tooltip(
      message: tooltip,
      child: Material(
        color: AppColors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(999),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(999),
          child: Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(
                color: AppColors.primary.withValues(alpha: 0.2),
                width: 1,
              ),
            ),
            child: Icon(
              icon,
              size: 20,
              color: AppColors.primary.withValues(alpha: 0.8),
            ),
          ),
        ),
      ),
    );
  }

  String _buildSuggestionTrustLine(SuggestionNameItem item) {
    if (item.phoneticScore != null) {
      return 'คัดจากความหมายใกล้เคียง พร้อมตรวจการอ่านออกเสียงของชื่อนี้แล้ว';
    }
    return 'คัดจากความหมายใกล้เคียงของชื่อ และเตรียมพร้อมสำหรับการวิเคราะห์ต่อ';
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
            color: Colors.black.withValues(alpha: 0.1),
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
          prefixIcon: Icon(icon, color: Colors.white.withValues(alpha: 0.5)),
          suffixIcon: ValueListenableBuilder<TextEditingValue>(
            valueListenable: controller,
            builder: (context, value, child) {
              if (value.text.isEmpty) return const SizedBox.shrink();
              return IconButton(
                icon: Icon(
                  Icons.close,
                  color: Colors.white.withValues(alpha: 0.5),
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

  // ANCHOR: Unified Day & Kaki Filter Switch Card (การ์ดวันเกิดและตัวกรองกาลกิณีแบบรวมชิ้นดีไซน์พรีเมียม)
  Widget buildUnifiedDayKakiCard() {
    final hasSelectedDay = _selectedDay != null;
    final isActive = hasSelectedDay && _filterKaki;
    
    final Map<String, Map<String, dynamic>> badgeConfigs = {
      'Sunday': {
        'name': 'วันอาทิตย์',
        'color': const Color(0xFFFF9500),
        'gradient': const LinearGradient(
          colors: [Color(0xFFFFF7E6), Color(0xFFFFE0B2)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFFE65100),
      },
      'Monday': {
        'name': 'วันจันทร์',
        'color': const Color(0xFFFFCC00),
        'gradient': const LinearGradient(
          colors: [Color(0xFFFFFDE7), Color(0xFFFFF9C4)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFF795548),
      },
      'Tuesday': {
        'name': 'วันอังคาร',
        'color': const Color(0xFFD946EF),
        'gradient': const LinearGradient(
          colors: [Color(0xFFFDF4FF), Color(0xFFF5D0FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFF86198F),
      },
      'Wednesday1': {
        'name': 'วันพุธ (กลางวัน)',
        'color': const Color(0xFF34C759),
        'gradient': const LinearGradient(
          colors: [Color(0xFFE8F5E9), Color(0xFFC8E6C9)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFF2E7D32),
      },
      'Wednesday2': {
        'name': 'วันพุธ (กลางคืน)',
        'color': const Color(0xFF007A7C),
        'gradient': const LinearGradient(
          colors: [Color(0xFFE0F2F1), Color(0xFFB2DFDB)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFF004D40),
      },
      'Thursday': {
        'name': 'วันพฤหัสบดี',
        'color': const Color(0xFFFF9500),
        'gradient': const LinearGradient(
          colors: [Color(0xFFFFF3E0), Color(0xFFFFE0B2)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFFE65100),
      },
      'Friday': {
        'name': 'วันศุกร์',
        'color': const Color(0xFF5AC8FA),
        'gradient': const LinearGradient(
          colors: [Color(0xFFE3F2FD), Color(0xFFBBDEFB)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFF0D47A1),
      },
      'Saturday': {
        'name': 'วันเสาร์',
        'color': const Color(0xFF5856D6),
        'gradient': const LinearGradient(
          colors: [Color(0xFFF3E5F5), Color(0xFFE1BEE7)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        'textColor': const Color(0xFF4A148C),
      },
    };

    // Reddish gradient configuration when active is false (Toggle is closed)
    const inactiveGradient = LinearGradient(
      colors: [Color(0xFFFFF5F5), Color(0xFFFFE8E8)],
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
    );
    const Color inactiveBorderColor = Color(0xFFFCA5A5);
    const Color inactiveTextColor = Color(0xFFC53030);

    final activeConfig = hasSelectedDay ? badgeConfigs[_selectedDay] : null;
    
    // Background and border colors
    final Gradient cardGradient = isActive
        ? (activeConfig!['gradient'] as Gradient)
        : inactiveGradient;
        
    final Color cardBorderColor = isActive
        ? (activeConfig!['color'] as Color).withValues(alpha: 0.3)
        : inactiveBorderColor.withValues(alpha: 0.55);
        
    final Color titleColor = isActive
        ? (activeConfig!['textColor'] as Color)
        : inactiveTextColor;

    final titleText = isActive
        ? "อันดับชื่อคัดกาลกิณีออกแล้ว"
        : "ยังไม่ได้คัดกาลกิณีออก";
        


    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOutCubic,
      width: double.infinity,
      decoration: BoxDecoration(
        gradient: cardGradient,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: cardBorderColor,
          width: 1.6,
        ),
        boxShadow: [
          BoxShadow(
            color: (isActive ? (activeConfig!['color'] as Color) : const Color(0xFFFF3B30))
                .withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            if (!hasSelectedDay) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  backgroundColor: AppColors.textLight, // Deep brown background
                  behavior: SnackBarBehavior.floating, // Floating for modern premium feel
                  margin: const EdgeInsets.all(16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  duration: const Duration(seconds: 2),
                  content: Row(
                    children: [
                      const Icon(
                        Icons.calendar_month_rounded,
                        color: AppColors.accent, // Gold accent icon
                        size: 20,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          "กรุณาแตะเลือกวันเกิดที่ด้านบนก่อนนะคะ",
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
              return;
            }
            setState(() => _filterKaki = !_filterKaki);
            unawaited(_refreshResultsKeepingStep2Anchor());
          },
          borderRadius: BorderRadius.circular(18),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Row(
              children: [
                AnimatedContainer(
                  duration: const Duration(milliseconds: 300),
                  padding: const EdgeInsets.all(9),
                  decoration: BoxDecoration(
                    color: isActive
                        ? (activeConfig!['color'] as Color).withValues(alpha: 0.12)
                        : const Color(0xFFFFEBEE),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    isActive ? Icons.security_rounded : Icons.warning_rounded,
                    color: isActive ? (activeConfig!['color'] as Color) : const Color(0xFFE53935),
                    size: 18,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        titleText,
                        style: GoogleFonts.prompt(
                          color: titleColor,
                          fontSize: 13,
                          fontWeight: FontWeight.w800,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 2),
                      if (hasSelectedDay)
                        RichText(
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          text: TextSpan(
                            style: GoogleFonts.prompt(
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: isActive
                                  ? titleColor.withValues(alpha: 0.65)
                                  : const Color(0xFFE53935).withValues(alpha: 0.65),
                            ),
                            children: [
                              const TextSpan(text: "คำนวณมงคลตามวันเกิด: "),
                              TextSpan(
                                text: activeConfig!['name'] as String,
                                style: GoogleFonts.prompt(
                                  fontWeight: FontWeight.w900,
                                  color: isActive
                                      ? titleColor
                                      : const Color(0xFFC53030),
                                ),
                              ),
                            ],
                          ),
                        )
                      else
                        Text(
                          "กรุณาเลือกวันเกิดของคุณที่ด้านบนก่อนนะคะ",
                          style: GoogleFonts.prompt(
                            color: const Color(0xFFE53935).withValues(alpha: 0.7),
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                IgnorePointer(
                  child: Transform.scale(
                    scale: 0.75,
                    child: Switch.adaptive(
                      value: isActive,
                      onChanged: null, // Tapped via parent InkWell
                      activeTrackColor: const Color(0xFF38BDF8),
                      activeColor: const Color(0xFF0EA5E9),
                      inactiveThumbColor: Colors.white,
                      inactiveTrackColor: const Color(0xFFEF9A9A),
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




  Widget buildFilterChipsSection() {
    final bool canUseRankingTemplate =
        _keywordController.text.trim().isNotEmpty &&
        (_hasRankableNameTemplate ||
            (_inputClassification != null &&
                (_inputClassification!.type == "single_name" ||
                    _inputClassification!.type == "full_name")));
    final bool needsSeedName =
        _keywordController.text.trim().isNotEmpty && !canUseRankingTemplate;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // --- VIP ZONE CARD ---
        Stack(
          clipBehavior: Clip.none,
          children: [
            Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
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
                  color: const Color(0xFF8B5CF6).withValues(alpha: 0.2),
                  width: 1.5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF8B5CF6).withValues(alpha: 0.05),
                    blurRadius: 18,
                    spreadRadius: 2,
                    offset: const Offset(0, 10),
                  ),
                  BoxShadow(
                    color: const Color(0xFFD4AF37).withValues(alpha: 0.05),
                    blurRadius: 22,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // ANCHOR: 3 book miracle (หาชื่อตามตำราที่ดีที่สุดจาก 3 แสนรายชื่อ)
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          "หาชื่อตามตำราที่ดีที่สุด",
                          style: GoogleFonts.prompt(
                            color: AppColors.textLight,
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  if (_hasRankingCriteria && _hasRankableNameTemplate)
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.auto_awesome_rounded,
                            color: const Color(
                              0xFFD4AF37,
                            ).withValues(alpha: 0.9),
                            size: 14,
                          ),
                          const SizedBox(width: 6),
                          Flexible(
                            child: RichText(
                              text: TextSpan(
                                style: GoogleFonts.prompt(
                                  color: AppColors.textLight.withValues(
                                    alpha: 0.55,
                                  ),
                                  fontSize: 12,
                                  fontWeight: FontWeight.w500,
                                ),
                                children: [
                                  const TextSpan(text: "✨ ใช้ชื่อต้นแบบ "),
                                  TextSpan(
                                    text:
                                        _selectedNameMeaningName ??
                                        _keywordController.text.trim(),
                                    style: GoogleFonts.prompt(
                                      color: const Color(0xFFD4AF37),
                                      fontSize: 13,
                                      fontWeight: FontWeight.w800,
                                      shadows: [
                                        Shadow(
                                          color: const Color(
                                            0xFFD4AF37,
                                          ).withValues(alpha: 0.25),
                                          blurRadius: 8,
                                        ),
                                      ],
                                    ),
                                  ),
                                  const TextSpan(text: " จาก 3 แสนรายชื่อ"),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                  const SizedBox(height: 10),
                  // ANCHOR: 2ButtonPremium (2ปุ่มพรีเมี่ยม)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: _buildCompactFilterTile(
                              title: "เลขศาสตร์ดี",
                              icon: Icons.auto_awesome_rounded,
                              iconColor: const Color(0xFF10B981),
                              isActive:
                                  canUseRankingTemplate &&
                                  _filterSat &&
                                  !_filterSha,
                              isDisabled: !canUseRankingTemplate,
                              activeColor: const Color(0xFF10B981),
                              onTap: () {
                                if (!canUseRankingTemplate) return;
                                final bool currentlyActive =
                                    _filterSat && !_filterSha;
                                _handleFilterOptionTap(
                                  targetSat: !currentlyActive,
                                  targetSha: false,
                                );
                              },
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: _buildCompactFilterTile(
                              title: "พลังเงาดี",
                              icon: Icons.shield_rounded,
                              iconColor: const Color(0xFF7C3AED),
                              isActive:
                                  canUseRankingTemplate &&
                                  !_filterSat &&
                                  _filterSha,
                              isDisabled: !canUseRankingTemplate,
                              activeColor: const Color(0xFF7C3AED),
                              onTap: () {
                                if (!canUseRankingTemplate) return;
                                final bool currentlyActive =
                                    !_filterSat && _filterSha;
                                _handleFilterOptionTap(
                                  targetSat: false,
                                  targetSha: !currentlyActive,
                                );
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      _buildPremiumFilterOption(
                        isPremium: PremiumManager().isPremium,
                        hasFreeDoubleGood:
                            PremiumManager().canUseDoubleGood || kDebugMode,
                        isActive:
                            canUseRankingTemplate && _filterSat && _filterSha,
                        isDisabled: !canUseRankingTemplate,
                        onTap: () {
                          if (!canUseRankingTemplate) return;
                          final bool currentlyActive = _filterSat && _filterSha;
                          _handleFilterOptionTap(
                            targetSat: !currentlyActive,
                            targetSha: !currentlyActive,
                          );
                        },
                      ),
                      if (needsSeedName) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 10,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFFF1F5F9),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: const Color(0xFFE2E8F0)),
                          ),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Icon(
                                Icons.info_outline_rounded,
                                color: Color(0xFF64748B),
                                size: 16,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  "ล็อกอยู่: กรุณาแตะเลือกชื่อที่คุณถูกใจจาก LikeName หรือ Celebrity Avatar ด้านบนก่อน เพื่อเป็นชื่อต้นแบบให้ระบบคัดกรอง",
                                  style: GoogleFonts.sarabun(
                                    color: const Color(0xFF475569),
                                    fontSize: 12,
                                    fontWeight: FontWeight.w600,
                                    height: 1.4,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ],
    );
  }

  void _handleFilterOptionTap({
    required bool targetSat,
    required bool targetSha,
  }) async {
    final bool willBeDoubleGood = targetSat && targetSha;
    if (willBeDoubleGood) {
      if (!PremiumManager().canUseDoubleGood) {
        final purchased = await showPaywallDialog(context);
        if (!mounted) return;
        if (purchased != true) return;
      }
    }
    final bool willHaveCriteria = targetSat || targetSha;
    setState(() {
      _invalidateStatsCache();
      _filterSat = targetSat;
      _filterSha = targetSha;
    });
    if (willBeDoubleGood && !PremiumManager().isPremium) {
      unawaited(PremiumManager().markDoubleGoodUsed());
    }
    _updateAutoScrollBasedOnFilters();
    if (!willHaveCriteria) {
      if (mounted) {
        setState(() {
          _invalidateStatsCache();
          _results = [];
          _hasSearched = true;
          _relaxedFiltersNotice = null;
        });
      }
      return;
    }
    _onFilterToggled();
  }

  Widget _buildCompactFilterTile({
    required String title,
    required IconData icon,
    required Color iconColor,
    required bool isActive,
    required bool isDisabled,
    required Color activeColor,
    required VoidCallback onTap,
  }) {
    final Color bgColor = isDisabled
        ? const Color(0xFFF8FAFC)
        : isActive
        ? activeColor.withValues(alpha: 0.10)
        : Colors.white;
    final Color borderColor = isDisabled
        ? const Color(0xFFE2E8F0)
        : isActive
        ? activeColor
        : const Color(0xFFE2E8F0);
    final Color textColor = isDisabled
        ? const Color(0xFF94A3B8)
        : isActive
        ? activeColor
        : const Color(0xFF475569);
    final Color iconFinalColor = isActive ? activeColor : textColor;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: isDisabled ? null : onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 220),
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 8),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: borderColor, width: 1.5),
          boxShadow: isActive
              ? [
                  BoxShadow(
                    color: activeColor.withValues(alpha: 0.14),
                    blurRadius: 12,
                    spreadRadius: 1,
                    offset: const Offset(0, 4),
                  ),
                ]
              : null,
        ),
        child: Row(
          children: [
            Icon(icon, size: 14, color: iconFinalColor),
            const SizedBox(width: 4),
            Expanded(
              child: Text(
                title,
                style: GoogleFonts.prompt(
                  color: textColor,
                  fontSize: 11,
                  fontWeight: FontWeight.w900,
                  height: 1,
                ),
                maxLines: 1,
                overflow: TextOverflow.visible,
              ),
            ),
            const SizedBox(width: 2),
            Transform.scale(
              scale: 0.7,
              child: Switch.adaptive(
                value: isActive,
                onChanged: isDisabled ? null : (_) => onTap(),
                activeColor: activeColor,
                activeTrackColor: activeColor.withValues(alpha: 0.3),
                inactiveThumbColor: Colors.white,
                inactiveTrackColor: const Color(0xFFCBD5E1),
                materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPremiumFilterOption({
    required bool isPremium,
    required bool hasFreeDoubleGood,
    required bool isActive,
    required bool isDisabled,
    required VoidCallback onTap,
  }) {
    final bool canUse = isPremium || hasFreeDoubleGood;
    final Color activeGold = const Color(0xFFFFD700);

    // Active VIP Theme (Deep Midnight Purple + Emerald Green)
    final BoxDecoration activeDecoration = BoxDecoration(
      gradient: const LinearGradient(
        colors: [
          Color(0xFF1E1B4B),
          Color(0xFF022C22),
        ], // Midnight Indigo to Deep Emerald
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
      ),
      borderRadius: BorderRadius.circular(18),
      border: Border.all(color: activeGold, width: 2.0),
      boxShadow: [
        BoxShadow(
          color: const Color(0xFF7C3AED).withValues(alpha: 0.35),
          blurRadius: 18,
          spreadRadius: 1,
          offset: const Offset(0, 6),
        ),
        BoxShadow(
          color: const Color(0xFFFFD700).withValues(alpha: 0.15),
          blurRadius: 12,
          offset: const Offset(0, 2),
        ),
      ],
    );

    // Inactive VIP Theme (Sophisticated Glassmorphic Off-White)
    final BoxDecoration inactiveDecoration = BoxDecoration(
      color: const Color(0xFFFDFCFE),
      borderRadius: BorderRadius.circular(18),
      border: Border.all(color: const Color(0xFFE2E8F0), width: 1.5),
      boxShadow: [
        BoxShadow(
          color: Colors.black.withValues(alpha: 0.02),
          blurRadius: 8,
          offset: const Offset(0, 3),
        ),
      ],
    );

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: isDisabled ? null : onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: isActive ? activeDecoration : inactiveDecoration,
        child: Row(
          children: [
            // Glowing Premium Icon Badge
            AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  colors: isActive
                      ? [
                          const Color(0xFFFFDF00),
                          const Color(0xFFD4AF37),
                        ] // Pure gold gradient
                      : [
                          const Color(0xFF8B5CF6),
                          const Color(0xFF10B981),
                        ], // Purple to green
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                boxShadow: isActive
                    ? [
                        BoxShadow(
                          color: activeGold.withValues(alpha: 0.5),
                          blurRadius: 10,
                          spreadRadius: 1,
                        ),
                      ]
                    : null,
              ),
              child: Icon(
                isActive
                    ? Icons.stars_rounded
                    : Icons.star_border_purple500_rounded,
                size: 20,
                color: isActive ? const Color(0xFF1E1B4B) : Colors.white,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Wrap(
                    crossAxisAlignment: WrapCrossAlignment.center,
                    spacing: 6,
                    runSpacing: 4,
                    children: [
                      Text(
                        "เลขศาสตร์ x พลังเงาดี",
                        style: GoogleFonts.prompt(
                          color: isActive
                              ? Colors.white
                              : const Color(0xFF1E293B),
                          fontSize: 14,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 0.2,
                        ),
                      ),
                      // Premium Badge tag
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 3,
                        ),
                        decoration: BoxDecoration(
                          color: isActive
                              ? activeGold.withValues(alpha: 0.2)
                              : const Color(0xFFE0F2FE),
                          borderRadius: BorderRadius.circular(999),
                          border: Border.all(
                            color: isActive
                                ? activeGold
                                : const Color(0xFF38BDF8),
                            width: 1,
                          ),
                        ),
                        child: Text(
                          isActive ? "VIP ACTIVE" : "ทดลองใช้ฟรี",
                          style: GoogleFonts.prompt(
                            color: isActive
                                ? activeGold
                                : const Color(0xFF0369A1),
                            fontSize: 9,
                            fontWeight: FontWeight.w900,
                            height: 1,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    "จัดอันดับ Double Lucky x2 ขั้นสูง",
                    style: GoogleFonts.sarabun(
                      color: isActive
                          ? Colors.white.withValues(alpha: 0.7)
                          : const Color(0xFF64748B),
                      fontSize: 11.5,
                      fontWeight: FontWeight.w600,
                      height: 1.2,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            Transform.scale(
              scale: 0.8,
              child: Switch.adaptive(
                value: isActive,
                onChanged: isDisabled ? null : (_) => onTap(),
                activeColor: activeGold,
                activeTrackColor: activeGold.withValues(alpha: 0.3),
                inactiveThumbColor: Colors.white,
                inactiveTrackColor: const Color(0xFFCBD5E1),
                materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            ),
          ],
        ),
      ),
    );
  }

  bool isScoreTrulyGood(int score, bool apiGood) {
    if (score < 100) return apiGood;
    final pairs = toPairList(score);
    if (pairs.length < 2) return apiGood;

    // For split scores, we check if ALL resulting pairs are lucky
    return pairs.every((pair) => isLuckyNumber(int.tryParse(pair) ?? 0));
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
    final pairs = toPairList(score);
    if (pairs.length > 1) {
      return Column(
        children: [
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (int i = 0; i < pairs.length; i++) ...[
                if (i > 0) const SizedBox(width: 4),
                buildAnalysisScoreCircle(
                  pairs[i],
                  isLuckyNumber(int.tryParse(pairs[i]) ?? 0),
                ),
              ],
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
              color: Colors.black.withValues(alpha: 0.1),
              blurRadius: 4,
              offset: const Offset(0, 3),
            ),
          ],
          border: Border.all(
            color: Colors.black.withValues(alpha: 0.05),
            width: 1,
          ),
        ),
        child: Text(
          score,
          style: const TextStyle(
            color: AppColors.textLight,
            fontWeight: FontWeight.bold,
            fontSize: 14,
            fontFamily: 'Prompt',
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
          color: const Color(0xFF10B981).withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(8),
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
          color: const Color(0xFFEF4444).withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(8),
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
        buildAnalysisScoreCircle(zeroPad(score), isGood),
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
              return AlertDialog(
                backgroundColor: AppColors.bgDark,
                content: SizedBox(
                  height: 130, // Reduced from 180
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const CircularProgressIndicator(
                          color: AppColors.primary,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          "กำลังร่ายมนต์...",
                          style: GoogleFonts.prompt(
                            color: AppColors.textLight,
                            fontSize: 14,
                          ),
                        ),
                      ],
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
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: _buildVipDetailParts(
                    data.detail.replaceAll("\\n", "\n"),
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

  List<Widget> _buildVipDetailParts(String detailText) {
    final generalStyle = const TextStyle(
      color: AppColors.textGray,
      height: 1.6,
      fontSize: 14,
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
            style: TextStyle(color: goodColor, height: 1.6, fontSize: 14),
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
            style: TextStyle(color: badColor, height: 1.6, fontSize: 14),
          ),
        );
      }
    }

    if (widgets.isEmpty) {
      widgets.add(Text(detailText, style: generalStyle));
    }

    return widgets;
  }

  Widget buildToggle(
    String label,
    bool value,
    Function(bool) onChanged, {
    Color? activeThumbColor,
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
              activeThumbColor: activeThumbColor ?? AppColors.primary,
              activeTrackColor: (activeThumbColor ?? AppColors.primary)
                  .withValues(alpha: 0.3),
              inactiveThumbColor: Colors.white70,
              inactiveTrackColor: Colors.white.withValues(alpha: 0.1),
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
      width: double.infinity,
      decoration: const BoxDecoration(
        color: Color(0xFFF5EFEB), // Soft, warm light beige-brown background (Luxury Vachetta sand leather)
      ),
      child: Stack(
        children: [
          Positioned.fill(
            child: CustomPaint(
              painter: LouisVuittonMonogramPainter(
                color: const Color(0xFF2D1E15).withValues(alpha: 0.05), // Subtle dark brown monogram shapes
              ),
            ),
          ),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(24, 40, 24, 100), // Integrated bottom space for FAB
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  "ความรู้เรื่องชื่อและเลขศาสตร์",
                  textAlign: TextAlign.center,
                  style: GoogleFonts.prompt(
                    color: const Color(0xFF2D1E15).withValues(alpha: 0.55), // Elegant subtle dark brown text
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 24),
                Wrap(
                  alignment: WrapAlignment.center,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  spacing: 4,
                  runSpacing: 8,
                  children: [
                    buildFooterLink("เลขศาสตร์ & พลังเงา", 0),
                    const Text("•", style: TextStyle(color: Color(0xFF8B6B5C), fontSize: 12)),
                    buildFooterLink("กาลกิณี", 1),
                    const Text("•", style: TextStyle(color: Color(0xFF8B6B5C), fontSize: 12)),
                    buildFooterLink("ระบบอัจฉริยะ (AI)", 2),
                    const Text("•", style: TextStyle(color: Color(0xFF8B6B5C), fontSize: 12)),
                    buildFooterLink("การจัดอันดับชื่อ", 3),
                  ],
                ),
                const SizedBox(height: 24),
                Text(
                  "วิเคราะห์จากชื่อจริง +3 แสนชื่อ",
                  textAlign: TextAlign.center,
                  style: GoogleFonts.prompt(
                    color: const Color(0xFF2D1E15).withValues(alpha: 0.85), // Rich dark brown text
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 40),
                Text(
                  "© 2026 Naming App. All rights reserved.",
                  textAlign: TextAlign.center,
                  style: GoogleFonts.sarabun(
                    color: const Color(0xFF2D1E15).withValues(alpha: 0.25),
                    fontSize: 10,
                  ),
                ),
              ],
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
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 12),
        child: Text(
          label,
          textAlign: TextAlign.center,
          style: GoogleFonts.prompt(
            color: const Color(0xFF8B6B5C), // Beautiful luxury soft warm brown link text
            fontSize: 13,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.3,
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
          color: const Color(0xFF1E293B).withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
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
              "ระบบจะเริ่มจาก semantic retrieval เพื่อหาชื่อที่ตรงความหมายก่อน แล้วจึงจัดอันดับตามเงื่อนไขที่คุณเลือก เช่น เลขศาสตร์ดี พลังเงาดี หรือทั้งสองอย่างร่วมกัน",
              style: GoogleFonts.sarabun(
                color: Colors.white.withValues(alpha: 0.7),
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
                              backgroundColor: AppColors.primary.withValues(
                                alpha: 0.12,
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
                              color: AppColors.textGray.withValues(alpha: 0.1),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }

                return AlertDialog(
                  backgroundColor: AppColors.bgDark,
                  shadowColor: AppColors.primary.withValues(alpha: 0.1),
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
                          color: AppColors.primary.withValues(alpha: 0.1),
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
                          color: AppColors.textGray.withValues(alpha: 0.7),
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
                        color: AppColors.textGray.withValues(alpha: 0.1),
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
                                final navigator = Navigator.of(dialogContext);
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
                                  final success = await ApiService()
                                      .saveNameLocally(payload);
                                  if (success) {
                                    didSave = true;
                                    setDialogState(() {
                                      dialogSaved = true;
                                      dialogSaving = false;
                                    });
                                    Future.delayed(
                                      const Duration(milliseconds: 1000),
                                      () {
                                        if (navigator.canPop()) {
                                          navigator.pop();
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
                          backgroundColor: AppColors.primary.withValues(
                            alpha: 0.15,
                          ),
                          foregroundColor: AppColors.textLight,
                          elevation: 0,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                            side: BorderSide(
                              color: AppColors.primary.withValues(alpha: 0.3),
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

  Future<void> _refreshResultsKeepingStep2Anchor() async {
    final hasQuery = _keywordController.text.trim().isNotEmpty;
    if (hasQuery) {
      await _search(
        scrollToResults: false,
        showInputSnack: false,
        reloadSelectedName: false,
        preserveScrollPosition: true,
        allowWhileLoading: true,
      );
    }
  }

  Widget buildMagicRankingHeader() {
    return Column(
      key: _step2Key,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: const [_MagicSubtitleAnimation()],
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
                    fontSize: 16,
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
          color: const Color(0xFFFFF9E6).withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.accent.withValues(alpha: 0.1)),
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
            border: Border.all(color: color.withValues(alpha: 0.3), width: 1.5),
            gradient: LinearGradient(
              colors: [
                color.withValues(alpha: 0.05 + glow * 0.1),
                AppColors.accent.withValues(alpha: 0.03 + glow * 0.05),
                color.withValues(alpha: 0.02),
              ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            boxShadow: [
              BoxShadow(
                color: color.withValues(alpha: 0.1 + glow * 0.15),
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
                    color: color.withValues(alpha: 0.3 + glow * 0.4),
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
          color.withValues(alpha: 0.05),
          Colors.white.withValues(alpha: 0.1),
          color.withValues(alpha: 0.05),
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
        ..color = color.withValues(alpha: opacity * 0.6)
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
  final int savedCount;

  const SparklingGoldHeart({super.key, required this.savedCount});

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
    const Color heartColor = Color(0xFF10B981);

    return AnimatedBuilder(
      animation: controller,
      builder: (context, child) {
        return SizedBox(
          width: 40,
          height: 40,
          child: Stack(
            alignment: Alignment.center,
            clipBehavior: Clip.none, // Allow sparkles to fly slightly out
            children: [
              // Subtle background pulse
              Container(
                width: 28,
                height: 28,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: heartColor.withValues(
                        alpha:
                            0.15 + 0.25 * math.sin(controller.value * math.pi),
                      ),
                      blurRadius: 10,
                      spreadRadius: 1,
                    ),
                  ],
                ),
              ),
              // Main Heart (Gold)
              const Icon(Icons.favorite_rounded, size: 26, color: heartColor),
              if (widget.savedCount > 0)
                Positioned(
                  right: -2,
                  top: -2,
                  child: Container(
                    constraints: const BoxConstraints(
                      minWidth: 16,
                      minHeight: 16,
                    ),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 4,
                      vertical: 1,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFEA580C),
                      borderRadius: BorderRadius.circular(999),
                      border: Border.all(color: Colors.white, width: 1.5),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(
                            0xFFEA580C,
                          ).withValues(alpha: 0.35),
                          blurRadius: 8,
                          offset: const Offset(0, 3),
                        ),
                      ],
                    ),
                    child: Text(
                      '${widget.savedCount}',
                      textAlign: TextAlign.center,
                      style: GoogleFonts.prompt(
                        color: Colors.white,
                        fontSize: 9,
                        fontWeight: FontWeight.w800,
                        height: 1.0,
                      ),
                    ),
                  ),
                ),
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
                  left: 20 + math.cos(angle) * distance - 5,
                  top: 20 + math.sin(angle) * distance - 5,
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
        return Container(
          width: 4,
          height: 18,
          decoration: BoxDecoration(
            color: AppColors.accent,
            borderRadius: BorderRadius.circular(2),
            boxShadow: [
              BoxShadow(
                color: AppColors.accent.withValues(
                  alpha: 0.6 * controller.value,
                ),
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


class LouisVuittonMonogramPainter extends CustomPainter {
  final Color color;
  LouisVuittonMonogramPainter({required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    const double spacingX = 60.0;
    const double spacingY = 60.0;

    for (double x = 0; x < size.width + spacingX; x += spacingX) {
      int row = 0;
      for (double y = 0; y < size.height + spacingY; y += spacingY) {
        final double offset = (row % 2 == 0) ? 0 : spacingX / 2;
        final double posX = x + offset;
        final double posY = y;

        final int patternType = (row + (x / spacingX).round()) % 4;

        if (patternType == 0) {
          _drawMonogramNA(canvas, posX, posY, paint);
        } else if (patternType == 1) {
          _drawQuatrefoilInCircle(canvas, posX, posY, paint);
        } else if (patternType == 2) {
          _drawFourPointedStar(canvas, posX, posY, paint);
        } else if (patternType == 3) {
          _drawOpenQuatrefoil(canvas, posX, posY, paint);
        }
        row++;
      }
    }
  }

  void _drawMonogramNA(Canvas canvas, double cx, double cy, Paint paint) {
    final textPainter = TextPainter(
      text: TextSpan(
        text: 'N',
        style: TextStyle(
          fontFamily: 'serif',
          fontSize: 14,
          fontWeight: FontWeight.bold,
          color: paint.color,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    textPainter.paint(canvas, Offset(cx - 7, cy - 8));

    final textPainter2 = TextPainter(
      text: TextSpan(
        text: 'A',
        style: TextStyle(
          fontFamily: 'serif',
          fontSize: 11,
          fontWeight: FontWeight.bold,
          color: paint.color,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    textPainter2.paint(canvas, Offset(cx + 1, cy - 1));
  }

  void _drawQuatrefoilInCircle(Canvas canvas, double cx, double cy, Paint paint) {
    final strokePaint = Paint()
      ..color = paint.color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.0;
    canvas.drawCircle(Offset(cx, cy), 9, strokePaint);

    final path = Path();
    for (int i = 0; i < 4; i++) {
      final double angle = i * 3.14159 / 2;
      final double dx = cx + 7.5 * math.cos(angle);
      final double dy = cy + 7.5 * math.sin(angle);
      path.moveTo(cx, cy);
      path.quadraticBezierTo(
        cx + 5.5 * math.cos(angle - 0.45),
        cy + 5.5 * math.sin(angle - 0.45),
        dx,
        dy,
      );
      path.quadraticBezierTo(
        cx + 5.5 * math.cos(angle + 0.45),
        cy + 5.5 * math.sin(angle + 0.45),
        cx,
        cy,
      );
    }
    canvas.drawPath(path, paint);
  }

  void _drawFourPointedStar(Canvas canvas, double cx, double cy, Paint paint) {
    final path = Path();
    path.moveTo(cx, cy - 9);
    path.quadraticBezierTo(cx, cy, cx + 9, cy);
    path.quadraticBezierTo(cx, cy, cx, cy + 9);
    path.quadraticBezierTo(cx, cy, cx - 9, cy);
    path.quadraticBezierTo(cx, cy, cx, cy - 9);
    canvas.drawPath(path, paint);
  }

  void _drawOpenQuatrefoil(Canvas canvas, double cx, double cy, Paint paint) {
    final path = Path();
    for (int i = 0; i < 4; i++) {
      final double angle = i * 3.14159 / 2;
      final double dx = cx + 8.5 * math.cos(angle);
      final double dy = cy + 8.5 * math.sin(angle);
      path.moveTo(cx, cy);
      path.quadraticBezierTo(
        cx + 6.5 * math.cos(angle - 0.5),
        cy + 6.5 * math.sin(angle - 0.5),
        dx,
        dy,
      );
      path.quadraticBezierTo(
        cx + 6.5 * math.cos(angle + 0.5),
        cy + 6.5 * math.sin(angle + 0.5),
        cx,
        cy,
      );
    }
    canvas.drawPath(path, paint);
    canvas.drawCircle(Offset(cx, cy), 1.2, Paint()..color = paint.color);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
