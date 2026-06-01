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
  static const String _localSavedNamesKey = 'local_saved_names';

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
      final savedName = _normalizeSavedName(savedData["name"]);
      if (savedName.isEmpty) return false;

      final prefs = await SharedPreferences.getInstance();
      final rawList = _readLocalSavedNames(prefs);

      final now = DateTime.now();
      final int newId = DateTime.now().millisecondsSinceEpoch;
      final Map<String, dynamic> newEntry = {
        "id": newId,
        "name": savedName,
        "sat_sum": _toInt(savedData["sat_sum"]) ?? 0,
        "sha_sum": _toInt(savedData["sha_sum"]) ?? 0,
        "is_sat_good": _toBool(savedData["is_sat_good"]),
        "is_sha_good": _toBool(savedData["is_sha_good"]),
        "root_word": (savedData["root_word"] ?? "").toString(),
        "meaning": (savedData["meaning"] ?? "").toString(),
        "analysis": (savedData["analysis"] ?? "").toString(),
        "created_at": now.toIso8601String(),
        "sat_pair_type": (savedData["sat_pair_type"] ?? "").toString(),
        "sha_pair_type": (savedData["sha_pair_type"] ?? "").toString(),
        "birth_day": (savedData["birth_day"] ?? "").toString(),
        "no_kaki": _toBool(savedData["no_kaki"]),
        "kaki_chars": (savedData["kaki_chars"] ?? "").toString(),
        "sat_pair_point": _toInt(savedData["sat_pair_point"]) ?? 0,
        "sha_pair_point": _toInt(savedData["sha_pair_point"]) ?? 0,
        "phonetic_score": _toInt(savedData["phonetic_score"]) ?? 80,
        "phonetic_summary": (savedData["phonetic_summary"] ?? "").toString(),
        "final_rank_score": _toInt(savedData["final_rank_score"]) ?? 0,
        "final_rank_score_exact":
            _toDouble(savedData["final_rank_score_exact"]) ?? 0.0,
        "rank_position": _toInt(savedData["rank_position"]) ?? 0,
      };

      final existingIndex = rawList.indexWhere((item) {
        if (item is! Map) return false;
        return _normalizeSavedName(item["name"]) == savedName;
      });

      if (existingIndex >= 0) {
        final existing = Map<String, dynamic>.from(
          rawList[existingIndex] as Map,
        );
        rawList[existingIndex] = {
          ...existing,
          ...newEntry,
          "id": _toInt(existing["id"]) ?? newId,
          "created_at": (existing["created_at"] ?? newEntry["created_at"])
              .toString(),
        };
      } else {
        rawList.add(newEntry);
      }

      await _writeLocalSavedNames(prefs, rawList);

      savedNamesCache.add(savedName);
      return true;
    } catch (e) {
      debugPrint("Failed to save name locally: $e");
      return false;
    }
  }

  Future<List<UserSavedName>> listSavedNames({
    int? userId,
    String? deviceId,
  }) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final rawList = _readLocalSavedNames(prefs);

      final List<UserSavedName> list = [];
      for (var item in rawList) {
        try {
          if (item is Map) {
            list.add(UserSavedName.fromJson(Map<String, dynamic>.from(item)));
          }
        } catch (e) {
          debugPrint("Error parsing UserSavedName item: $e");
        }
      }

      list.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      return list;
    } catch (e) {
      debugPrint("Failed to list saved names locally: $e");
      return [];
    }
  }

  Future<bool> deleteSavedName(int id) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final rawList = _readLocalSavedNames(prefs);

      String? removedName;
      rawList.removeWhere((item) {
        if (item is Map && _toInt(item["id"]) == id) {
          removedName = _normalizeSavedName(item["name"]);
          return true;
        }
        return false;
      });

      await _writeLocalSavedNames(prefs, rawList);
      if (removedName != null && removedName!.isNotEmpty) {
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
      await prefs.remove(_localSavedNamesKey);
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
    savedNamesCache.addAll(
      list
          .map((e) => _normalizeSavedName(e.name))
          .where((name) => name.isNotEmpty),
    );
  }

  List<dynamic> _readLocalSavedNames(SharedPreferences prefs) {
    Object? storedValue;
    try {
      storedValue = prefs.get(_localSavedNamesKey);
    } catch (e) {
      debugPrint("Error reading local saved names: $e");
      return [];
    }

    if (storedValue == null) return [];
    if (storedValue is String) {
      return _decodeLocalSavedNames(storedValue);
    }
    if (storedValue is List<String>) {
      return storedValue
          .map(_decodeLegacySavedNameString)
          .whereType<Map<String, dynamic>>()
          .toList();
    }

    debugPrint(
      "Unsupported local saved names type: ${storedValue.runtimeType}",
    );
    return [];
  }

  Future<void> _writeLocalSavedNames(
    SharedPreferences prefs,
    List<dynamic> rawList,
  ) async {
    await prefs.remove(_localSavedNamesKey);
    await prefs.setString(_localSavedNamesKey, jsonEncode(rawList));
  }

  Map<String, dynamic>? _decodeLegacySavedNameString(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) return null;

    try {
      final decoded = jsonDecode(trimmed);
      if (decoded is Map) {
        return Map<String, dynamic>.from(decoded);
      }
    } catch (_) {
      // Legacy string-list entries may be plain names, not JSON.
    }

    return {
      "id": DateTime.now().microsecondsSinceEpoch,
      "name": trimmed,
      "created_at": DateTime.now().toIso8601String(),
    };
  }

  List<dynamic> _decodeLocalSavedNames(String listJson) {
    try {
      final decoded = jsonDecode(listJson);
      if (decoded is List) {
        return List<dynamic>.from(decoded);
      }
      debugPrint("Local saved names data is not a list");
    } catch (e) {
      debugPrint("Error decoding local saved names: $e");
    }
    return [];
  }

  static String _normalizeSavedName(dynamic value) {
    return (value ?? '').toString().trim();
  }

  static int? _toInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.round();
    return int.tryParse(value.toString());
  }

  static double? _toDouble(dynamic value) {
    if (value == null) return null;
    if (value is double) return value;
    if (value is num) return value.toDouble();
    return double.tryParse(value.toString());
  }

  static bool _toBool(dynamic value) {
    if (value is bool) return value;
    if (value is num) return value != 0;
    final normalized = value?.toString().trim().toLowerCase();
    return normalized == 'true' || normalized == '1' || normalized == 'yes';
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
