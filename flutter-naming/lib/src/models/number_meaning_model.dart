class NumberMeaningResult {
  final String number;
  final String description;
  final String detail;
  final String pairType; // e.g. D5, D8, D10, R5, R7, R10

  NumberMeaningResult({
    required this.number,
    required this.description,
    required this.detail,
    this.pairType = '',
  });

  factory NumberMeaningResult.fromJson(Map<String, dynamic> json) {
    return NumberMeaningResult(
      number: json['number'] ?? '',
      description: json['description'] ?? '',
      detail: json['detail'] ?? '',
      pairType: json['pair_type'] ?? '',
    );
  }
}
