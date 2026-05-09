import 'name_model.dart';

class NameIntentResult {
  final String mode;
  final double confidence;
  final List<String> candidates;
  final double bestScore;

  const NameIntentResult({
    required this.mode,
    required this.confidence,
    required this.candidates,
    required this.bestScore,
  });

  factory NameIntentResult.fromJson(Map<String, dynamic> json) {
    return NameIntentResult(
      mode: (json['mode'] as String? ?? 'MEANING').trim(),
      confidence: (json['confidence'] ?? 0.0).toDouble(),
      candidates: List<String>.from(json['candidates'] ?? const <String>[]),
      bestScore: (json['best_score'] ?? 0.0).toDouble(),
    );
  }

  bool get isHybrid => mode == 'HYBRID';
  bool get isName => mode == 'NAME';
  bool get isMeaning => mode == 'MEANING';

  String? get topCandidate {
    if (candidates.isEmpty) return null;
    final candidate = candidates.first.trim();
    return candidate.isEmpty ? null : candidate;
  }
}

class NameInputResolveResult {
  final String input;
  final String inputType;
  final bool existsInDatabase;
  final bool canDecode;
  final bool canRankFromTemplate;
  final String suggestionStrategy;
  final String? dbMeaning;
  final NameIntentResult? intent;
  final NameAnalysisResult? decode;

  const NameInputResolveResult({
    required this.input,
    required this.inputType,
    required this.existsInDatabase,
    required this.canDecode,
    required this.canRankFromTemplate,
    required this.suggestionStrategy,
    this.dbMeaning,
    this.intent,
    this.decode,
  });

  factory NameInputResolveResult.fromJson(Map<String, dynamic> json) {
    final intentJson = json['intent'];
    final decodeJson = json['decode'];
    return NameInputResolveResult(
      input: (json['input'] as String? ?? '').trim(),
      inputType: (json['input_type'] as String? ?? 'meaning').trim(),
      existsInDatabase: json['exists_in_database'] == true,
      canDecode: json['can_decode'] == true,
      canRankFromTemplate: json['can_rank_from_template'] == true,
      suggestionStrategy: (json['suggestion_strategy'] as String? ?? 'semantic')
          .trim(),
      dbMeaning: (json['db_meaning'] as String?)?.trim(),
      intent: intentJson is Map<String, dynamic>
          ? NameIntentResult.fromJson(intentJson)
          : intentJson is Map
          ? NameIntentResult.fromJson(Map<String, dynamic>.from(intentJson))
          : null,
      decode: decodeJson is Map<String, dynamic>
          ? NameAnalysisResult.fromJson(decodeJson)
          : decodeJson is Map
          ? NameAnalysisResult.fromJson(Map<String, dynamic>.from(decodeJson))
          : null,
    );
  }

  bool get shouldUsePgTrgmSuggestions =>
      suggestionStrategy == 'pg_trgm' ||
      (inputType == 'name' && !existsInDatabase);

  bool get isMeaning => inputType == 'meaning';
}
