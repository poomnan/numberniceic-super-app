import 'dart:convert';
import 'dart:math';
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/name_intent_model.dart';
import '../models/name_model.dart';
import '../models/name_root_result.dart';
import '../models/number_meaning_model.dart';

class ApiException implements Exception {
  final String message;
  final int? statusCode;

  ApiException(this.message, {this.statusCode});

  @override
  String toString() => message;
}

class ApiService {
  static const Duration _timeout = Duration(seconds: 60);

  Duration _getTimeout(bool filterSat, bool filterSha) {
    return (filterSat && filterSha)
        ? Duration(seconds: 120)
        : Duration(seconds: 60);
  }

  static String get baseUrl {
    return 'https://xn--b3cu8e7ah6h.com';
  }

  static String? _deviceId;
  Future<String> getDeviceId() async {
    if (_deviceId != null) return _deviceId!;
    final prefs = await SharedPreferences.getInstance();
    _deviceId = prefs.getString('user_device_id');
    if (_deviceId == null) {
      final suffix = 1000 + Random().nextInt(9000);
      _deviceId = 'guest_${DateTime.now().millisecondsSinceEpoch}_$suffix';
      await prefs.setString('user_device_id', _deviceId!);
    }
    return _deviceId!;
  }

  Future<MobileSearchResponse> searchNames({
    required String keyword,
    String? lastname,
    String? day,
    String? semanticMeaning,
    String? meaningIntent,
    String? entryMode,
    bool filterSat = true,
    bool filterSha = true,
    bool filterKaki = false,
    bool similarMode = false,
    int? limit,
  }) async {
    final url = Uri.parse('$baseUrl/api/v1/name-search');
    final body = {
      "keyword": keyword,
      "lastname": lastname ?? "",
      "day": day ?? "",
      "semantic_meaning": semanticMeaning ?? "",
      "meaning_intent": meaningIntent ?? "",
      "entry_mode": entryMode ?? "",
      "filter_sat": filterSat,
      "filter_sha": filterSha,
      "filter_kaki": filterKaki,
      "similar_mode": similarMode,
      "limit": limit ?? 50,
    };

    try {
      final isDoubleGoodMode = filterSat && filterSha;
      final timeout = _getTimeout(filterSat, filterSha);
      final maxRetries = isDoubleGoodMode ? 2 : 1;
      http.Response? response;
      TimeoutException? lastTimeout;

      for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
          response = await _postJsonWithRetry(
            url,
            body,
            retryOnEmptyBody: true,
            timeout: timeout,
          );
          lastTimeout = null;
          break;
        } on TimeoutException catch (e) {
          lastTimeout = e;
          if (attempt == maxRetries) {
            rethrow;
          }
          await Future.delayed(Duration(seconds: attempt * 2));
        }
      }

      if (response == null) {
        throw lastTimeout ??
            TimeoutException('name-search timeout without response');
      }

      if (response.statusCode == 200) {
        final data = _decodeJsonBody<Map<String, dynamic>>(
          response,
          fallbackMessage: 'เซิร์ฟเวอร์ตอบกลับไม่สมบูรณ์ กรุณาลองใหม่อีกครั้ง',
        );
        var searchResponse = MobileSearchResponse.fromJson(data);

        if (searchResponse.results.isEmpty && searchResponse.total > 0) {
          final retryResponse = await _postJsonWithRetry(
            url,
            body,
            retryOnEmptyBody: true,
            timeout: timeout,
          );
          if (retryResponse.statusCode == 200) {
            final retryData = _decodeJsonBody<Map<String, dynamic>>(
              retryResponse,
              fallbackMessage:
                  'เซิร์ฟเวอร์ตอบกลับไม่สมบูรณ์ กรุณาลองใหม่อีกครั้ง',
            );
            searchResponse = MobileSearchResponse.fromJson(retryData);
          }
        }

        return searchResponse;
      } else {
        throw ApiException(
          'ระบบขัดข้องชั่วคราว กรุณาลองใหม่อีกครั้ง',
          statusCode: response.statusCode,
        );
      }
    } on TimeoutException {
      throw ApiException(
        'การเชื่อมต่อล่าช้าเกินไป กรุณาตรวจสอบอินเทอร์เน็ตแล้วลองใหม่',
      );
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException(
        'เชื่อมต่อเซิร์ฟเวอร์ไม่ได้ กรุณาตรวจสอบอินเทอร์เน็ตแล้วลองใหม่',
      );
    }
  }

  Future<NameIntentResult?> detectNameIntent(String input) async {
    final trimmed = input.trim();
    if (trimmed.isEmpty) return null;

    final url = Uri.parse('$baseUrl/api/v1/name-intent');
    final body = {"input": trimmed};

    try {
      final response = await _postJsonWithRetry(url, body);

      if (response.statusCode != 200) {
        return null;
      }

      final Map<String, dynamic> data = _decodeJsonBody(
        response,
        fallbackMessage: 'ไม่สามารถวิเคราะห์รูปแบบคำค้นได้ในขณะนี้',
      );
      return NameIntentResult.fromJson(data);
    } catch (e) {
      return null;
    }
  }

  Future<NameInputResolveResult?> resolveNameInput(
    String input, {
    String? day,
  }) async {
    final trimmed = input.trim();
    if (trimmed.isEmpty) return null;

    final queryParams = {'input': trimmed};
    if (day != null && day.isNotEmpty) {
      queryParams['day'] = day;
    }
    final url = Uri.parse(
      '$baseUrl/api/v1/name-input/resolve',
    ).replace(queryParameters: queryParams);

    try {
      final response = await http.get(url).timeout(_timeout);
      if (response.statusCode != 200) return null;
      final Map<String, dynamic> data = _decodeJsonBody(
        response,
        fallbackMessage: 'ไม่สามารถวิเคราะห์คำค้นได้ในขณะนี้',
      );
      return NameInputResolveResult.fromJson(data);
    } catch (e) {
      return null;
    }
  }

  Future<NameSuggestionsResponse?> getNameSuggestions(
    String query, {
    String? meaning,
  }) async {
    if (query.isEmpty) return null;

    final queryParams = {'q': query};
    if (meaning != null && meaning.isNotEmpty) {
      queryParams['meaning'] = meaning;
    }

    final url = Uri.parse(
      '$baseUrl/api/v1/name-suggestions',
    ).replace(queryParameters: queryParams);

    try {
      final response = await http.get(url).timeout(_timeout);

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถอ่านข้อมูลคำแนะนำชื่อได้ในขณะนี้',
        );
        return NameSuggestionsResponse.fromJson(data);
      } else {
        return null;
      }
    } catch (e) {
      return null;
    }
  }

  Future<String?> getNameMeaning(String name) async {
    if (name.isEmpty) return null;

    final url = Uri.parse(
      '$baseUrl/api/v1/name-meaning',
    ).replace(queryParameters: {'name': name});

    try {
      final response = await http.get(url).timeout(_timeout);
      if (response.statusCode == 200) {
        final data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถอ่านความหมายชื่อได้ในขณะนี้',
        );
        if (data is Map<String, dynamic>) {
          final meaning = data['meaning'];
          if (meaning is String) {
            final cleaned = meaning
                .replaceAll(RegExp(r'\s*\([^)]*[\u4e00-\u9fa5]+[^)]*\)'), '')
                .replaceAll(RegExp(r'\s*\(含[^\)]+\)'), '')
                .trim();
            return cleaned.isEmpty ? null : cleaned;
          }
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  Future<NameAnalysisResult?> decodeName(String name, {String? day}) async {
    if (name.isEmpty) return null;

    final params = {'name': name};
    if (day != null) {
      params['day'] = day;
    }

    final url = Uri.parse('$baseUrl/decode').replace(queryParameters: params);

    try {
      final response = await http.get(url).timeout(_timeout);

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถถอดรหัสชื่อได้ในขณะนี้',
        );
        return NameAnalysisResult.fromJson(data);
      } else {
        return null;
      }
    } catch (e) {
      return null;
    }
  }

  Future<NameRootResult?> getNameRoot(String name, {String? meaning}) async {
    if (name.isEmpty) return null;

    final params = {'name': name};
    if (meaning != null && meaning.isNotEmpty) {
      params['meaning'] = meaning;
    }

    final url = Uri.parse(
      '$baseUrl/api/v1/name-root',
    ).replace(queryParameters: params);

    try {
      final response = await http.get(url).timeout(_timeout);

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถวิเคราะห์รากศัพท์ได้ในขณะนี้',
        );
        return NameRootResult.fromJson(data);
      } else {
        return null;
      }
    } catch (e) {
      return null;
    }
  }

  Future<NumberMeaningResult?> getNumberMeaning(String number) async {
    if (number.isEmpty) return null;

    final url = Uri.parse(
      '$baseUrl/api/v1/number-meaning',
    ).replace(queryParameters: {'number': number});

    try {
      final response = await http.get(url).timeout(_timeout);

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถอ่านความหมายตัวเลขได้ในขณะนี้',
        );
        return NumberMeaningResult.fromJson(data);
      } else {
        return null;
      }
    } catch (e) {
      return null;
    }
  }

  Future<List<Map<String, dynamic>>> getNamingExamples() async {
    final url = Uri.parse('$baseUrl/api/v1/naming-examples');

    try {
      final response = await http
          .get(
            url,
            headers: {
              "User-Agent":
                  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
              "Accept":
                  "application/json,text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            },
          )
          .timeout(_timeout);

      if (response.statusCode == 200) {
        final List<dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถอ่านข้อมูลตัวอย่างชื่อได้ในขณะนี้',
        );
        return data.map((e) => Map<String, dynamic>.from(e)).toList();
      } else {
        debugPrint("API Error: Status Code ${response.statusCode} for $url");
        return [];
      }
    } catch (e) {
      debugPrint("API Error: Exception caught for $url: $e");
      return [];
    }
  }

  // Semantic Search Ideas API
  Future<List<Map<String, dynamic>>> getSemanticSearchIdeas() async {
    final url = Uri.parse('$baseUrl/api/v1/semantic-search-ideas');
    try {
      final response = await http.get(url).timeout(_timeout);
      if (response.statusCode == 200) {
        final List<dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถโหลดไอเดียค้นหาได้ในขณะนี้',
        );
        return data.map((e) => Map<String, dynamic>.from(e)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // Input Classification API
  Future<InputClassification?> classifyInput(String text) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return null;

    final url = Uri.parse(
      '$baseUrl/api/v1/input/classify',
    ).replace(queryParameters: {'input': trimmed});

    try {
      final response = await http.get(url).timeout(_timeout);
      if (response.statusCode == 200) {
        final Map<String, dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถจำแนกประเภทข้อมูลได้ในขณะนี้',
        );
        return InputClassification.fromJson(data);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Save name locally (alias for saveName, used in naming screen save dialog)
  Future<bool> saveNameLocally(Map<String, dynamic> savedData) async {
    return saveName(savedData);
  }

  // User Saved Names APIs (Local storage implementation using SharedPreferences)
  Future<bool> saveName(Map<String, dynamic> savedData) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final listJson = prefs.getString('local_saved_names') ?? '[]';
      final List<dynamic> rawList = jsonDecode(listJson);
      
      final int newId = DateTime.now().millisecondsSinceEpoch;
      final Map<String, dynamic> newEntry = {
        "id": newId,
        "name": savedData["name"] ?? "",
        "sat_sum": savedData["sat_sum"] ?? 0,
        "sha_sum": savedData["sha_sum"] ?? 0,
        "is_sat_good": savedData["is_sat_good"] ?? false,
        "is_sha_good": savedData["is_sha_good"] ?? false,
        "root_word": savedData["root_word"] ?? "",
        "meaning": savedData["meaning"] ?? "",
        "analysis": savedData["analysis"] ?? "",
        "created_at": DateTime.now().toIso8601String(),
        "sat_pair_type": savedData["sat_pair_type"] ?? "",
        "sha_pair_type": savedData["sha_pair_type"] ?? "",
        "birth_day": savedData["birth_day"] ?? "",
        "no_kaki": savedData["no_kaki"] ?? false,
        "kaki_chars": savedData["kaki_chars"] ?? "",
        "sat_pair_point": savedData["sat_pair_point"] ?? 0,
        "sha_pair_point": savedData["sha_pair_point"] ?? 0,
        "phonetic_score": savedData["phonetic_score"] ?? 80,
        "phonetic_summary": savedData["phonetic_summary"] ?? "",
        "final_rank_score": savedData["final_rank_score"] ?? 0,
        "final_rank_score_exact": savedData["final_rank_score_exact"] ?? 0.0,
        "rank_position": savedData["rank_position"] ?? 0,
      };

      rawList.add(newEntry);
      await prefs.setString('local_saved_names', jsonEncode(rawList));
      
      savedNamesCache.add(savedData["name"] ?? "");
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<List<UserSavedName>> listSavedNames({
    int? userId,
    String? deviceId,
  }) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final listJson = prefs.getString('local_saved_names') ?? '[]';
      final List<dynamic> rawList = jsonDecode(listJson);
      
      final List<UserSavedName> list = rawList
          .map((e) => UserSavedName.fromJson(Map<String, dynamic>.from(e)))
          .toList();
      
      list.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      return list;
    } catch (e) {
      return [];
    }
  }

  Future<bool> deleteSavedName(int id) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final listJson = prefs.getString('local_saved_names') ?? '[]';
      final List<dynamic> rawList = jsonDecode(listJson);
      
      String? removedName;
      rawList.removeWhere((item) {
        if (item["id"] == id) {
          removedName = item["name"];
          return true;
        }
        return false;
      });

      await prefs.setString('local_saved_names', jsonEncode(rawList));
      if (removedName != null) {
        savedNamesCache.remove(removedName);
      }
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<bool> clearAllSavedNames() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('local_saved_names');
      savedNamesCache.clear();
      return true;
    } catch (e) {
      return false;
    }
  }

  // Local caching to track state without redundant API calls
  static final Set<String> savedNamesCache = {};

  Future<void> loadSavedNamesCache() async {
    final list = await listSavedNames();
    savedNamesCache.clear();
    savedNamesCache.addAll(list.map((e) => e.name));
  }

  static T _decodeJsonBody<T>(
    http.Response response, {
    required String fallbackMessage,
  }) {
    final body = response.body.trim();
    if (body.isEmpty) {
      throw ApiException(fallbackMessage, statusCode: response.statusCode);
    }

    try {
      return jsonDecode(body) as T;
    } on FormatException {
      throw ApiException(fallbackMessage, statusCode: response.statusCode);
    }
  }

  Future<http.Response> _postJsonWithRetry(
    Uri url,
    Map<String, dynamic> body, {
    bool retryOnEmptyBody = false,
    Duration? timeout,
  }) async {
    final requestTimeout = timeout ?? _timeout;
    http.Response response = await http
        .post(
          url,
          headers: {"Content-Type": "application/json"},
          body: jsonEncode(body),
        )
        .timeout(requestTimeout);

    if (retryOnEmptyBody &&
        response.statusCode == 200 &&
        response.body.trim().isEmpty) {
      await Future.delayed(const Duration(milliseconds: 350));
      response = await http
          .post(
            url,
            headers: {"Content-Type": "application/json"},
            body: jsonEncode(body),
          )
          .timeout(requestTimeout);
    }

    return response;
  }
}
