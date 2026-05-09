String zeroPad(int n) {
  if (n < 10) return '0$n';
  return n.toString();
}

List<String> toPairList(int n) {
  final s = n.toString();
  if (n < 10) return ['0$n'];
  if (n < 100) return [s];

  final pairs = <String>[];
  for (int i = 0; i < s.length - 1; i++) {
    pairs.add(s.substring(i, i + 2));
  }
  return pairs;
}
