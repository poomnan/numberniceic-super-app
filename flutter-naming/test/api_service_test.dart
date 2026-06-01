import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_naming/src/services/api_service.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
    ApiService.savedNamesCache.clear();
  });

  test('Local saveName and listSavedNames test', () async {
    final apiService = ApiService();

    // Check initial cached and saved names
    expect(ApiService.savedNamesCache, isEmpty);
    final initialList = await apiService.listSavedNames();
    expect(initialList, isEmpty);

    // Save a name
    final payload = {
      "name": "กิตติเดช",
      "sat_sum": 24,
      "sha_sum": 45,
      "is_sat_good": true,
      "is_sha_good": false,
      "root_word": "กิตติ",
      "meaning": "ผู้มีเกียรติ",
      "analysis": "วิเคราะห์เกียรติ",
    };

    final saveSuccess = await apiService.saveName(payload);
    expect(saveSuccess, isTrue);

    // Check saved cache
    expect(ApiService.savedNamesCache.contains("กิตติเดช"), isTrue);

    // List saved names
    final list = await apiService.listSavedNames();
    expect(list.length, equals(1));
    expect(list[0].name, equals("กิตติเดช"));
    expect(list[0].satSum, equals(24));
    expect(list[0].shaSum, equals(45));
    expect(list[0].isSatGood, isTrue);
    expect(list[0].isShaGood, isFalse);
    expect(list[0].rootWord, equals("กิตติ"));
    expect(list[0].meaning, equals("ผู้มีเกียรติ"));
    expect(list[0].analysis, equals("วิเคราะห์เกียรติ"));

    // Delete name
    final deleteSuccess = await apiService.deleteSavedName(list[0].id);
    expect(deleteSuccess, isTrue);

    // Check list is empty now
    final listAfterDelete = await apiService.listSavedNames();
    expect(listAfterDelete, isEmpty);
    expect(ApiService.savedNamesCache.contains("กิตติเดช"), isFalse);
  });

  test(
    'saveName keeps ranking metadata and avoids duplicate local entries',
    () async {
      final apiService = ApiService();

      final firstSave = await apiService.saveName({
        "name": " ภคิน ",
        "sat_sum": 14,
        "sha_sum": 50,
        "is_sat_good": true,
        "is_sha_good": true,
        "meaning": "ผู้มีโชคดี",
        "final_rank_score": 98,
        "final_rank_score_exact": 98.6,
        "rank_position": 2,
      });
      expect(firstSave, isTrue);

      final secondSave = await apiService.saveName({
        "name": "ภคิน",
        "sat_sum": "15",
        "sha_sum": "51",
        "is_sat_good": "true",
        "is_sha_good": "false",
        "meaning": "ข้อมูลใหม่",
        "final_rank_score": "99",
        "final_rank_score_exact": "99.25",
        "rank_position": "1",
      });
      expect(secondSave, isTrue);

      final list = await apiService.listSavedNames();
      expect(list.length, equals(1));
      expect(list.single.name, equals("ภคิน"));
      expect(list.single.satSum, equals(15));
      expect(list.single.shaSum, equals(51));
      expect(list.single.isSatGood, isTrue);
      expect(list.single.isShaGood, isFalse);
      expect(list.single.meaning, equals("ข้อมูลใหม่"));
      expect(list.single.finalRankScore, equals(99));
      expect(list.single.finalRankScoreExact, equals(99.25));
      expect(list.single.rankPosition, equals(1));
      expect(ApiService.savedNamesCache.contains("ภคิน"), isTrue);
    },
  );

  test(
    'saveName recovers when local saved-name storage is malformed',
    () async {
      SharedPreferences.setMockInitialValues({
        'local_saved_names': '{"unexpected":"shape"}',
      });
      ApiService.savedNamesCache.clear();

      final apiService = ApiService();
      final saveSuccess = await apiService.saveName({"name": "กวิน"});

      expect(saveSuccess, isTrue);
      final list = await apiService.listSavedNames();
      expect(list.length, equals(1));
      expect(list.single.name, equals("กวิน"));
    },
  );

  test('saveName migrates legacy string-list local storage', () async {
    SharedPreferences.setMockInitialValues({
      'local_saved_names': <String>[
        '{"name":"ธนิน","sat_sum":24,"created_at":"2026-01-01T00:00:00.000"}',
        'ปภาวิน',
      ],
    });
    ApiService.savedNamesCache.clear();

    final apiService = ApiService();
    final saveSuccess = await apiService.saveName({
      "name": "ภคิน",
      "sat_sum": 14,
      "sha_sum": 50,
    });

    expect(saveSuccess, isTrue);
    final list = await apiService.listSavedNames();
    expect(
      list.map((item) => item.name),
      containsAll(["ธนิน", "ปภาวิน", "ภคิน"]),
    );
    expect(ApiService.savedNamesCache.contains("ภคิน"), isTrue);
  });
}
