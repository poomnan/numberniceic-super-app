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
        final data = _decodeJsonBody(
          response,
          fallbackMessage: 'เซิร์ฟเวอร์ตอบกลับไม่สมบูรณ์ กรุณาลองใหม่อีกครั้ง',
        );
        return MobileSearchResponse.fromJson(data);
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
            return meaning
                .replaceAll(RegExp(r'\s*\([^)]*[\u4e00-\u9fa5]+[^)]*\)'), '')
                .replaceAll(RegExp(r'\s*\(含[^\)]+\)'), '');
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

  // User Saved Names APIs
  Future<bool> saveName(Map<String, dynamic> savedData) async {
    final url = Uri.parse('$baseUrl/api/v1/saved-names/save');
    try {
      final response = await http
          .post(
            url,
            headers: {"Content-Type": "application/json"},
            body: jsonEncode(savedData),
          )
          .timeout(_timeout);
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  Future<List<UserSavedName>> listSavedNames({
    int? userId,
    String? deviceId,
  }) async {
    String query = "";
    if (userId != null) {
      query = "user_id=$userId";
    } else if (deviceId != null) {
      query = "device_id=$deviceId";
    }

    final url = Uri.parse('$baseUrl/api/v1/saved-names/list?$query');
    try {
      final response = await http.get(url).timeout(_timeout);
      if (response.statusCode == 200) {
        final List<dynamic> data = _decodeJsonBody(
          response,
          fallbackMessage: 'ไม่สามารถอ่านรายชื่อที่บันทึกไว้ได้ในขณะนี้',
        );
        return data.map((e) => UserSavedName.fromJson(e)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  Future<bool> deleteSavedName(int id) async {
    final url = Uri.parse('$baseUrl/api/v1/saved-names/delete');
    try {
      final response = await http
          .post(
            url,
            headers: {"Content-Type": "application/json"},
            body: jsonEncode({"id": id}),
          )
          .timeout(_timeout);
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  // Local caching to track state without redundant API calls
  static final Set<String> savedNamesCache = {};

  Future<void> loadSavedNamesCache() async {
    final did = await getDeviceId();
    final list = await listSavedNames(deviceId: did);
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
