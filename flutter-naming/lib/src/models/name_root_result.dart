class NameRootResult {
  final String name;
  final String rootWord;
  final String analysis;

  NameRootResult({
    required this.name,
    required this.rootWord,
    required this.analysis,
  });

  factory NameRootResult.fromJson(Map<String, dynamic> json) {
    final root = json['root_word'] ?? '';
    return NameRootResult(
      name: json['name'] ?? '',
      rootWord: root
          .replaceAll(RegExp(r'\s*\([^)]*[\u4e00-\u9fa5]+[^)]*\)'), '')
          .replaceAll(RegExp(r'\s*\(含[^\)]+\)'), ''),
      analysis: (json['analysis'] ?? root)
          .toString()
          .replaceAll(RegExp(r'\s*\([^)]*[\u4e00-\u9fa5]+[^)]*\)'), '')
          .replaceAll(
            RegExp(r'\s*\(含[^\)]+\)'),
            '',
          ), // Fallback to rootWord if analysis is missing
    );
  }
}
