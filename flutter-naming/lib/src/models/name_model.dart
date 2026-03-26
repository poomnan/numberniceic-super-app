class MobileNameResult {
  final String name;
  final String meaning;
  final String gender;
  final int satSum;
  final int shaSum;
  final int totalSat;
  final int totalSha;
  final double distance;
  final double rootScore;
  final double semanticScore;
  final double hybridScore;
  final double bonusCalculated;
  final bool isSatGood;
  final bool isShaGood;
  final bool isTotalSatGood;
  final bool isTotalShaGood;
  final String satPairType;
  final String shaPairType;
  final String totalSatPairType;
  final String totalShaPairType;
  final List<CharHighlight> kakiHighlight;
  final int finalRankScore;
  final List<String> rankReasons;

  MobileNameResult({
    required this.name,
    required this.meaning,
    required this.gender,
    required this.satSum,
    required this.shaSum,
    required this.totalSat,
    required this.totalSha,
    required this.distance,
    required this.rootScore,
    required this.semanticScore,
    required this.hybridScore,
    required this.bonusCalculated,
    required this.isSatGood,
    required this.isShaGood,
    required this.isTotalSatGood,
    required this.isTotalShaGood,
    this.satPairType = '',
    this.shaPairType = '',
    this.totalSatPairType = '',
    this.totalShaPairType = '',
    required this.kakiHighlight,
    this.finalRankScore = 0,
    this.rankReasons = const [],
  });

  factory MobileNameResult.fromJson(Map<String, dynamic> json) {
    List<CharHighlight> highlights = [];
    if (json['kaki_highlight'] != null && json['kaki_highlight'] is List) {
      for (var i in json['kaki_highlight']) {
        if (i is Map<String, dynamic>) {
          highlights.add(CharHighlight.fromJson(i));
        } else if (i is Map) {
          highlights.add(CharHighlight.fromJson(Map<String, dynamic>.from(i)));
        }
      }
    }

    return MobileNameResult(
      name: json['name'] ?? '',
      meaning:
          (json['meaning'] as String?)
              ?.replaceAll(RegExp(r'\s*\([^)]*[\u4e00-\u9fa5]+[^)]*\)'), '')
              .replaceAll(RegExp(r'\s*\(含[^\)]+\)'), '') ??
          '',
      gender: json['gender'] ?? '',
      satSum: json['sat_sum'] ?? 0,
      shaSum: json['sha_sum'] ?? 0,
      totalSat: json['total_sat'] ?? 0,
      totalSha: json['total_sha'] ?? 0,
      distance: (json['distance'] ?? 0.0).toDouble(),
      rootScore: (json['root_score'] ?? 0.0).toDouble(),
      semanticScore: (json['semantic_score'] ?? 0.0).toDouble(),
      hybridScore: (json['hybrid_score'] ?? 0.0).toDouble(),
      bonusCalculated: (json['bonus_calculated'] ?? 0.0).toDouble(),
      isSatGood: json['is_sat_good'] ?? false,
      isShaGood: json['is_sha_good'] ?? false,
      isTotalSatGood: json['is_total_sat_good'] ?? false,
      isTotalShaGood: json['is_total_sha_good'] ?? false,
      satPairType: json['sat_pair_type'] ?? '',
      shaPairType: json['sha_pair_type'] ?? '',
      totalSatPairType: json['total_sat_pair_type'] ?? '',
      totalShaPairType: json['total_sha_pair_type'] ?? '',
      kakiHighlight: highlights,
      finalRankScore: json['final_rank_score'] ?? 0,
      rankReasons: List<String>.from(json['rank_reasons'] ?? const []),
    );
  }
  int calculateScore({
    bool showMatching = false,
    bool isFilterSatActive = true,
    bool isFilterShaActive = true,
    bool isFilterKakiActive = true,
  }) {
    if (finalRankScore > 0) return finalRankScore;

    final similarity = (100 * (1 - distance)).clamp(0.0, 100.0);

    final bool satPass = showMatching ? isTotalSatGood : isSatGood;
    final bool shaPass = showMatching ? isTotalShaGood : isShaGood;

    final satBonus = satPass ? 20 : 0;
    final shaBonus = shaPass ? 20 : 0;
    final doubleBonus = (satPass && shaPass) ? 50 : 0;
    final kakiBonus =
        (kakiHighlight.isNotEmpty && !kakiHighlight.any((h) => h.isKaki))
        ? 10
        : 0;

    int lengthBonus = 0;
    int nameLength = name.length;
    if (nameLength <= 4) {
      lengthBonus = 15;
    } else if (nameLength == 5) {
      lengthBonus = 10;
    } else if (nameLength == 6) {
      lengthBonus = 5;
    } else if (nameLength >= 9) {
      lengthBonus = -5;
    }

    // Similarity is the base (0-100)
    // Total raw max = 100 (Sim) + 20 (SAT) + 20 (SHA) + 50 (Double) + 10 (Kaki) + 15 (Len) = 215
    final rawScore =
        similarity +
        satBonus +
        shaBonus +
        doubleBonus +
        kakiBonus +
        lengthBonus;

    // Normalize to 100 pt scale
    return (rawScore * 100 / 215).clamp(0, 100).toInt();
  }
}

class CharHighlight {
  final String char;
  final bool isKaki;

  CharHighlight({required this.char, required this.isKaki});

  factory CharHighlight.fromJson(Map<String, dynamic> json) {
    return CharHighlight(
      char: json['char'] ?? '',
      isKaki: json['is_kaki'] ?? false,
    );
  }
}

class MobileSearchResponse {
  final bool success;
  final List<MobileNameResult> results;
  final int total;
  final KakiInfo? kakiInfo;

  MobileSearchResponse({
    required this.success,
    required this.results,
    required this.total,
    this.kakiInfo,
  });

  factory MobileSearchResponse.fromJson(Map<String, dynamic> json) {
    List<MobileNameResult> resultsList = [];
    if (json['results'] != null && json['results'] is List) {
      for (var i in json['results']) {
        if (i is Map<String, dynamic>) {
          resultsList.add(MobileNameResult.fromJson(i));
        } else if (i is Map) {
          resultsList.add(
            MobileNameResult.fromJson(Map<String, dynamic>.from(i)),
          );
        }
      }
    }

    return MobileSearchResponse(
      success: json['success'] ?? false,
      results: resultsList,
      total: json['total'] ?? 0,
      kakiInfo: json['kaki_info'] != null
          ? KakiInfo.fromJson(json['kaki_info'])
          : null,
    );
  }
}

class KakiInfo {
  final String day;
  final String dayThai;
  final String description;

  KakiInfo({
    required this.day,
    required this.dayThai,
    required this.description,
  });

  factory KakiInfo.fromJson(Map<String, dynamic> json) {
    return KakiInfo(
      day: json['day'] ?? '',
      dayThai: json['day_th'] ?? '',
      description: json['description'] ?? '',
    );
  }
}

class NameAnalysisResult {
  final String name;
  final int satSum;
  final int shaSum;
  final bool isSatGood;
  final bool isShaGood;
  final List<CharHighlight> characters;
  final String satPairType;
  final String shaPairType;

  NameAnalysisResult({
    required this.name,
    required this.satSum,
    required this.shaSum,
    required this.isSatGood,
    required this.isShaGood,
    required this.characters,
    this.satPairType = '',
    this.shaPairType = '',
  });

  factory NameAnalysisResult.fromJson(Map<String, dynamic> json) {
    // Helper to check if details are all good
    bool checkGood(dynamic details) {
      if (details == null || details is! List || details.isEmpty) return false;
      for (var item in details) {
        if (item != null && item is Map && item['is_good'] != true) {
          return false;
        }
      }
      return true;
    }

    List<CharHighlight> chars = [];
    if (json['characters'] != null && json['characters'] is List) {
      for (var i in json['characters']) {
        if (i is Map<String, dynamic>) {
          chars.add(CharHighlight.fromJson(i));
        } else if (i is Map) {
          chars.add(CharHighlight.fromJson(Map<String, dynamic>.from(i)));
        }
      }
    }

    return NameAnalysisResult(
      name: json['name'] ?? '',
      satSum: json['total_sat'] ?? 0,
      shaSum: json['total_sha'] ?? 0,
      isSatGood: checkGood(json['sat_details']),
      isShaGood: checkGood(json['sha_details']),
      characters: chars,
      satPairType: json['sat_pair_type'] ?? '',
      shaPairType: json['sha_pair_type'] ?? '',
    );
  }
}

class UserSavedName {
  final int id;
  final String name;
  final int satSum;
  final int shaSum;
  final bool isSatGood;
  final bool isShaGood;
  final String rootWord;
  final String meaning;
  final String analysis;
  final DateTime createdAt;
  final String satPairType;
  final String shaPairType;

  UserSavedName({
    required this.id,
    required this.name,
    required this.satSum,
    required this.shaSum,
    required this.isSatGood,
    required this.isShaGood,
    required this.rootWord,
    required this.meaning,
    required this.analysis,
    required this.createdAt,
    this.satPairType = '',
    this.shaPairType = '',
  });

  factory UserSavedName.fromJson(Map<String, dynamic> json) {
    final root = json['root_word'] ?? '';
    final anal = json['analysis'] ?? '';
    final mn = json['meaning'] ?? '';
    return UserSavedName(
      id: json['id'] ?? 0,
      name: json['name'] ?? '',
      satSum: json['sat_sum'] ?? 0,
      shaSum: json['sha_sum'] ?? 0,
      isSatGood: json['is_sat_good'] ?? false,
      isShaGood: json['is_sha_good'] ?? false,
      rootWord: root,
      meaning: mn,
      analysis: (anal.isEmpty ? root : anal)
          .replaceAll(RegExp(r'\s*\([^)]*[\u4e00-\u9fa5]+[^)]*\)'), '')
          .replaceAll(RegExp(r'\s*\(含[^\)]+\)'), ''),
      createdAt: DateTime.parse(
        json['created_at'] ?? DateTime.now().toIso8601String(),
      ),
      satPairType: json['sat_pair_type'] ?? '',
      shaPairType: json['sha_pair_type'] ?? '',
    );
  }
}

class NameSuggestionsResponse {
  final List<String> ideas;
  final List<SuggestionNameItem> names;

  NameSuggestionsResponse({required this.ideas, required this.names});

  factory NameSuggestionsResponse.fromJson(Map<String, dynamic> json) {
    return NameSuggestionsResponse(
      ideas: List<String>.from(json['ideas'] ?? []),
      names: (json['names'] as List? ?? [])
          .map((e) => SuggestionNameItem.fromJson(e))
          .toList(),
    );
  }
}

class SuggestionNameItem {
  final int id;
  final String name;
  final String meaning;

  SuggestionNameItem({
    required this.id,
    required this.name,
    required this.meaning,
  });

  factory SuggestionNameItem.fromJson(Map<String, dynamic> json) {
    return SuggestionNameItem(
      id: json['id'] ?? 0,
      name: json['name'] ?? '',
      meaning: json['meaning'] ?? '',
    );
  }
}
