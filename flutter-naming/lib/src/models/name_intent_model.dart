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
