import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../models/name_model.dart';
import '../models/number_meaning_model.dart';
import '../services/api_service.dart';
import '../utils/colors.dart';

class SavedNamesScreen extends StatefulWidget {
  const SavedNamesScreen({super.key});

  @override
  State<SavedNamesScreen> createState() => _SavedNamesScreenState();
}

class _SavedNamesScreenState extends State<SavedNamesScreen> {
  final ApiService _apiService = ApiService();
  List<UserSavedName> _savedNames = [];
  bool _isLoading = true;
  final Map<String, String> _meanings = {}; // Cache for fetched meanings

  @override
  void initState() {
    super.initState();
    _loadSavedNames();
  }

  Future<void> _loadSavedNames() async {
    setState(() => _isLoading = true);
    try {
      final deviceId = await _apiService.getDeviceId();
      final names = await _apiService.listSavedNames(deviceId: deviceId);
      setState(() => _savedNames = names);
      // Fetch meanings for items that don't have one yet (legacy data)
      _fetchMissingMeanings();
    } catch (e) {
      // Error
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _fetchMissingMeanings() async {
    for (final item in _savedNames) {
      if (item.meaning.isEmpty && !_meanings.containsKey(item.name)) {
        final meaning = await _apiService.getNameMeaning(item.name);
        if (meaning != null && meaning.trim().isNotEmpty && mounted) {
          setState(() {
            _meanings[item.name] = meaning.trim();
          });
        }
      }
    }
  }

  /// Get the display meaning for a saved name item
  String _getDisplayMeaning(UserSavedName item) {
    if (item.meaning.isNotEmpty) return item.meaning;
    if (_meanings.containsKey(item.name)) return _meanings[item.name]!;
    return ''; // Will show loading or fallback
  }

  Future<void> _deleteName(int id) async {
    final success = await _apiService.deleteSavedName(id);
    if (success) {
      setState(() {
        _savedNames.removeWhere((n) {
          if (n.id == id) {
            ApiService.savedNamesCache.remove(n.name);
            return true;
          }
          return false;
        });
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("ลบออกจากคลังชื่อแล้วค่ะ")),
        );
      }
    }
  }

  Future<void> _confirmDelete(UserSavedName item) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: AppColors.primary, width: 2),
          ),
          title: Text(
            "ลบชื่อออกจากคลัง?",
            style: GoogleFonts.sarabun(
              fontWeight: FontWeight.bold,
              color: AppColors.textLight,
            ),
          ),
          content: Text(
            "แน่ใจนะคะว่าจะลบ “${item.name}” ออกจากคลังชื่อ",
            style: GoogleFonts.sarabun(color: AppColors.textGray),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text(
                "ยกเลิก",
                style: TextStyle(color: AppColors.textGray),
              ),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text(
                "ลบ",
                style: TextStyle(color: Colors.redAccent),
              ),
            ),
          ],
        );
      },
    );

    if (ok == true) {
      await _deleteName(item.id);
    }
  }

  Future<void> _chooseUseMode(UserSavedName item) async {
    await showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF0B1220),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        item.name,
                        style: GoogleFonts.sarabun(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.pop(context),
                      icon: const Icon(Icons.close, color: Colors.white54),
                    ),
                  ],
                ),
              ),
              ListTile(
                leading: const Icon(
                  Icons.search_rounded,
                  color: Colors.white70,
                ),
                title: Text(
                  "ใช้เป็นความหมายที่อยากได้",
                  style: GoogleFonts.sarabun(color: Colors.white),
                ),
                subtitle: Text(
                  "เช่น “อ่อนโยน”, “ฉลาด”, “แข็งแรง”",
                  style: GoogleFonts.sarabun(
                    color: Colors.white54,
                    fontSize: 12,
                  ),
                ),
                onTap: () => Navigator.pop(context, {
                  "mode": "keyword",
                  "name": item.name,
                }),
              ),
              ListTile(
                leading: const Icon(
                  Icons.compare_arrows_rounded,
                  color: Colors.white70,
                ),
                title: Text(
                  "ใช้เป็นชื่อคู่หรือนามสกุล",
                  style: GoogleFonts.sarabun(color: Colors.white),
                ),
                subtitle: Text(
                  "เพื่อดูคะแนนรวมเมื่อประกบกับชื่อที่กำลังหา",
                  style: GoogleFonts.sarabun(
                    color: Colors.white54,
                    fontSize: 12,
                  ),
                ),
                onTap: () => Navigator.pop(context, {
                  "mode": "matching",
                  "name": item.name,
                }),
              ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    ).then((value) {
      if (value != null) {
        Navigator.pop(context, value);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgDark, // Premium Cream
      appBar: AppBar(
        title: Text(
          "รายชื่อที่บันทึก",
          style: GoogleFonts.sarabun(
            fontWeight: FontWeight.bold,
            color: const Color(0xFF3D2600),
          ),
        ),
        iconTheme: const IconThemeData(color: Color(0xFF3D2600)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _savedNames.isEmpty
          ? _buildEmptyState()
          : RefreshIndicator(
              onRefresh: _loadSavedNames,
              child: ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: _savedNames.length,
                itemBuilder: (context, index) {
                  return _buildSavedNameCard(_savedNames[index]);
                },
              ),
            ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.collections_bookmark_rounded,
            size: 80,
            color: AppColors.textLight.withOpacity(0.1),
          ),
          const SizedBox(height: 16),
          Text(
            "ยังไม่มีชื่อในคลังเลยค่ะ",
            style: GoogleFonts.sarabun(
              color: AppColors.textLight.withOpacity(0.4),
              fontSize: 18,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back_rounded, size: 18),
            label: Text(
              "กลับไปค้นหาชื่อ",
              style: GoogleFonts.prompt(fontWeight: FontWeight.bold),
            ),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSavedNameCard(UserSavedName item) {
    final bool isGold = item.isSatGood && item.isShaGood;

    // Determine lucky status
    String luckText;
    List<Color> gradientColors;
    if (isGold) {
      luckText = 'Double Lucky x2';
      gradientColors = [const Color(0xFFDBB632), const Color(0xFFFF8C00)];
    } else {
      luckText = 'น่าเสียดาย';
      gradientColors = [const Color(0xFF71717A), const Color(0xFF3F3F46)];
    }

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF9E6), // Elegant Champagne
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: AppColors.accent.withOpacity(0.3), width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 15,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () => _chooseUseMode(item),
            child: Stack(
              children: [
                // Main content
                Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 44, 20, 16),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Left: Name + badges + meaning
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // Name
                                isGold
                                    ? PremiumNameTextEffect(
                                        child: Text(
                                          item.name,
                                          style: GoogleFonts.sarabun(
                                            fontSize: 24,
                                            fontWeight: FontWeight.bold,
                                            color: const Color(0xFF3D2600),
                                          ),
                                        ),
                                      )
                                    : Text(
                                        item.name,
                                        style: GoogleFonts.sarabun(
                                          fontSize: 24,
                                          fontWeight: FontWeight.bold,
                                          color: const Color(0xFF3D2600),
                                        ),
                                      ),
                                const SizedBox(height: 2),
                                // Meaning / Analysis preview
                                Text(
                                  _getDisplayMeaning(item).isNotEmpty
                                      ? _getDisplayMeaning(item)
                                      : (item.analysis.isNotEmpty
                                            ? item.analysis.split('\n').first
                                            : item.rootWord),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    color: AppColors.textGray,
                                    fontSize: 14,
                                    height: 1.5,
                                    fontFamily: 'Sarabun',
                                  ),
                                ),
                                const SizedBox(height: 8),
                                // Date
                                Text(
                                  "บันทึกเมื่อ ${item.createdAt.day}/${item.createdAt.month}/${item.createdAt.year}",
                                  style: TextStyle(
                                    color: AppColors.textGray, // Medium Brown
                                    fontSize: 10,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ],
                            ),
                          ),

                          const SizedBox(width: 8),

                          // Right: Score circles without labels
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              _buildScoreWithLabel(
                                item.satSum,
                                item.isSatGood,
                                "",
                                item.satPairType,
                              ),
                              const SizedBox(height: 12),
                              _buildScoreWithLabel(
                                item.shaSum,
                                item.isShaGood,
                                "",
                                item.shaPairType,
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    // Action row at bottom spanning full width
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          // Delete button
                          InkWell(
                            onTap: () => _confirmDelete(item),
                            borderRadius: BorderRadius.circular(12),
                            child: Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: Colors.red.withOpacity(0.05),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: Colors.red.withOpacity(0.15),
                                ),
                              ),
                              child: const Icon(
                                Icons.delete_outline_rounded,
                                size: 20,
                                color: Colors.red,
                              ),
                            ),
                          ),
                          // Root word button (Match image 1 - Gold)
                          InkWell(
                            onTap: () => _showSavedAnalysisDialog(item),
                            borderRadius: BorderRadius.circular(12),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 8,
                              ),
                              decoration: BoxDecoration(
                                gradient: AppColors.goldGradient,
                                borderRadius: BorderRadius.circular(12),
                                boxShadow: [
                                  BoxShadow(
                                    color: AppColors.accent.withOpacity(0.3),
                                    blurRadius: 8,
                                    offset: const Offset(0, 2),
                                  ),
                                ],
                              ),
                              child: const Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    Icons.auto_stories_rounded,
                                    color: Color(0xFF3D2600),
                                    size: 14,
                                  ),
                                  SizedBox(width: 6),
                                  Text(
                                    "ดูรากศัพท์",
                                    style: TextStyle(
                                      color: Color(0xFF3D2600),
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                // Lucky badge at top-right
                Positioned(
                  top: 0,
                  right: 0,
                  child: GestureDetector(
                    onTap: () => _showLuckExplanationDialog(item),
                    child: Container(
                      padding: const EdgeInsets.fromLTRB(16, 8, 12, 8),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: gradientColors,
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: const BorderRadius.only(
                          bottomLeft: Radius.circular(24),
                          topRight: Radius.circular(24),
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.2),
                            blurRadius: 8,
                            offset: const Offset(-2, 2),
                          ),
                        ],
                        border: Border.all(
                          color: Colors.white.withOpacity(0.3),
                          width: 1,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            isGold
                                ? Icons.auto_awesome
                                : Icons.info_outline_rounded,
                            color: Colors.white,
                            size: 14,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            luckText,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              letterSpacing: 0.5,
                              shadows: [
                                Shadow(
                                  color: Colors.black26,
                                  offset: Offset(0, 1),
                                  blurRadius: 2,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBadge(IconData icon, String text, Color bgColor, Color fgColor) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: bgColor.withOpacity(0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: bgColor.withOpacity(0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: fgColor, size: 12),
          const SizedBox(width: 4),
          Text(
            text,
            style: GoogleFonts.prompt(
              color: fgColor,
              fontSize: 10,
              fontWeight: FontWeight.bold,
              height: 1.2,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScoreWithLabel(
    int score,
    bool isGood,
    String label,
    String pairType,
  ) {
    Widget circle;
    const double size = 44;

    if (score >= 100) {
      String s = score.toString();
      if (s.length >= 3) {
        String p1 = s.substring(0, 2);
        String p2 = s.substring(1, 3);
        circle = Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildGradientCircle(p1, isGood, size, pairType),
            const SizedBox(width: 4),
            _buildGradientCircle(p2, isGood, size, pairType),
          ],
        );
      } else {
        circle = _buildGradientCircle(score.toString(), isGood, size, pairType);
      }
    } else {
      circle = _buildGradientCircle(score.toString(), isGood, size, pairType);
    }

    return circle;
  }

  Widget _buildGradientCircle(
    String score,
    bool isGood,
    double size,
    String pairType,
  ) {
    Color lightColor;
    Color darkColor;

    if (isGood) {
      lightColor = const Color(0xFF4ADE80); // Green-400
      darkColor = const Color(0xFF16A34A); // Green-600
    } else {
      lightColor = const Color(0xFFF87171); // Red-400
      darkColor = const Color(0xFFDC2626); // Red-600
    }

    return InkWell(
      onTap: () => _showNumberMeaningDialog(score, isGood),
      borderRadius: BorderRadius.circular(50),
      child: Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [lightColor, darkColor],
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.3),
              blurRadius: 6,
              offset: const Offset(0, 4),
            ),
            BoxShadow(
              color: Colors.white.withOpacity(0.2),
              blurRadius: 0,
              offset: const Offset(-1, -1),
            ),
          ],
          border: Border.all(color: Colors.white.withOpacity(0.15), width: 1),
        ),
        child: Text(
          score,
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: size * 18 / 44,
            shadows: const [
              Shadow(
                color: Colors.black26,
                offset: Offset(0, 1),
                blurRadius: 2,
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showSavedAnalysisDialog(UserSavedName item) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: AppColors.primary, width: 2),
          ),
          title: Column(
            children: [
              const Icon(
                Icons.article_rounded,
                color: AppColors.textGray,
                size: 40,
              ),
              const SizedBox(height: 12),
              Text(
                item.name,
                style: GoogleFonts.sarabun(
                  color: AppColors.textLight,
                  fontWeight: FontWeight.bold,
                  fontSize: 28,
                ),
              ),
              Divider(
                color: AppColors.textGray.withOpacity(0.1),
                height: 32,
                thickness: 1,
              ),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                if (_getDisplayMeaning(item).isNotEmpty) ...[
                  Text(
                    _getDisplayMeaning(item),
                    style: GoogleFonts.sarabun(
                      color: AppColors.textLight,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                if (item.rootWord.isNotEmpty) ...[
                  RichText(
                    text: TextSpan(
                      style: GoogleFonts.sarabun(
                        color: AppColors.textGray,
                        fontSize: 16,
                      ),
                      children: [
                        const TextSpan(
                          text: "ที่มาและรากศัพท์: ",
                          style: TextStyle(color: AppColors.textGray),
                        ),
                        TextSpan(
                          text: item.rootWord,
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            color: AppColors.textLight,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                Text(
                  item.analysis,
                  style: GoogleFonts.sarabun(
                    color: AppColors.textGray,
                    height: 1.8,
                    fontSize: 16,
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text(
                "ปิด",
                style: TextStyle(color: AppColors.textGray),
              ),
            ),
          ],
        );
      },
    );
  }

  void _showNumberMeaningDialog(String number, bool isGood) {
    showDialog(
      context: context,
      builder: (context) {
        return FutureBuilder<NumberMeaningResult?>(
          future: _apiService.getNumberMeaning(number),
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const AlertDialog(
                backgroundColor: AppColors.bgDark,
                content: SizedBox(
                  height: 100,
                  child: Center(child: CircularProgressIndicator()),
                ),
              );
            }

            if (snapshot.hasError || snapshot.data == null) {
              return AlertDialog(
                backgroundColor: AppColors.bgDark,
                title: const Text(
                  "ข้อผิดพลาด",
                  style: TextStyle(color: AppColors.textLight),
                ),
                content: const Text(
                  "ไม่สามารถดึงข้อมูลคำทำนายได้",
                  style: TextStyle(color: AppColors.textGray),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text(
                      "ปิด",
                      style: TextStyle(color: AppColors.textGray),
                    ),
                  ),
                ],
              );
            }

            final data = snapshot.data!;
            return AlertDialog(
              backgroundColor: AppColors.bgDark,
              shadowColor: AppColors.primary.withOpacity(0.1),
              surfaceTintColor: Colors.transparent,
              elevation: 20,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
                side: const BorderSide(color: AppColors.primary, width: 2),
              ),
              title: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: isGood
                          ? AppColors.success
                          : const Color(0xFFEF4444),
                      shape: BoxShape.circle,
                      boxShadow: [
                        BoxShadow(
                          color:
                              (isGood
                                      ? AppColors.success
                                      : const Color(0xFFEF4444))
                                  .withOpacity(0.3),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Text(
                      number,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 20,
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          "เลขศาสตร์ $number",
                          style: GoogleFonts.prompt(
                            color: AppColors.textGray.withOpacity(0.7),
                            fontSize: 12,
                          ),
                        ),
                        Text(
                          data.description,
                          style: GoogleFonts.prompt(
                            color: AppColors.textLight,
                            fontWeight: FontWeight.bold,
                            fontSize: 18,
                            height: 1.2,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              content: ConstrainedBox(
                constraints: BoxConstraints(
                  maxHeight: MediaQuery.of(context).size.height * 0.5,
                ),
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      Divider(
                        color: AppColors.textGray.withOpacity(0.1),
                        height: 24,
                      ),
                      Text(
                        data.detail.replaceAll("\\n", "\n"),
                        style: GoogleFonts.sarabun(
                          color: AppColors.textGray,
                          height: 1.7,
                          fontSize: 15,
                          letterSpacing: 0.1,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text(
                    "ปิด",
                    style: TextStyle(
                      color: AppColors.textGray,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }

  void _showLuckExplanationDialog(UserSavedName item) {
    final bool isLucky = item.isSatGood && item.isShaGood;

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: const Color(
            0xFF2A1F0A,
          ), // Consistent with flip back-side color
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
            side: BorderSide(
              color: isLucky
                  ? const Color(0xFFDBB632).withOpacity(0.4)
                  : Colors.white24,
              width: 1.5,
            ),
          ),
          contentPadding: const EdgeInsets.all(24),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Icon(
                        isLucky
                            ? Icons.auto_awesome_rounded
                            : Icons.info_outline_rounded,
                        color: isLucky
                            ? const Color(0xFFDBB632)
                            : const Color(0xFF9CA3AF),
                        size: 20,
                      ),
                      const SizedBox(width: 10),
                      Text(
                        isLucky ? 'ที่มาของโชค' : 'ทำไมถึงน่าเสียดาย?',
                        style: GoogleFonts.prompt(
                          color: isLucky
                              ? const Color(0xFFDBB632)
                              : const Color(0xFF9CA3AF),
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  GestureDetector(
                    onTap: () => Navigator.pop(context),
                    child: const Icon(
                      Icons.close_rounded,
                      color: Colors.white38,
                      size: 20,
                    ),
                  ),
                ],
              ),
              const Divider(color: Colors.white12, height: 24),
              Text(
                isLucky
                    ? 'Double Lucky x2 ได้มาจาก:'
                    : 'วิเคราะห์ข้อบกพร่องของชื่อนี้:',
                style: GoogleFonts.sarabun(
                  color: isLucky ? Colors.white70 : const Color(0xFFFCA5A5),
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 20),
              _buildCriteriaRowInDialog(
                Icons.looks_one_rounded,
                'เลขศาสตร์ (ตัวเลขมงคล)',
                item.isSatGood,
                item.isSatGood
                    ? 'เลขรวม ${item.satSum} — ดีมาก ✓'
                    : 'เลขรวม ${item.satSum} — ไม่ผ่าน',
              ),
              const SizedBox(height: 16),
              _buildCriteriaRowInDialog(
                Icons.blur_on_rounded,
                'พลังเงา (เลขเงา)',
                item.isShaGood,
                item.isShaGood
                    ? 'เลขเงา ${item.shaSum} — ดีมาก ✓'
                    : 'เลขเงา ${item.shaSum} — ไม่ผ่าน',
              ),
              const SizedBox(height: 24),
              Center(
                child: Text(
                  'แตะด้านนอกหรือกดปิดเพื่อกลับ',
                  style: GoogleFonts.sarabun(
                    color: Colors.white24,
                    fontSize: 11,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildCriteriaRowInDialog(
    IconData icon,
    String label,
    bool passed,
    String detail,
  ) {
    final color = passed ? const Color(0xFF4ADE80) : const Color(0xFFF87171);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          passed ? Icons.check_circle_rounded : Icons.cancel_rounded,
          color: color,
          size: 18,
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: GoogleFonts.sarabun(
                  color: color,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                detail,
                style: GoogleFonts.sarabun(color: Colors.white38, fontSize: 12),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class PremiumNameTextEffect extends StatefulWidget {
  final Widget child;

  const PremiumNameTextEffect({super.key, required this.child});

  @override
  State<PremiumNameTextEffect> createState() => _PremiumNameTextEffectState();
}

class _PremiumNameTextEffectState extends State<PremiumNameTextEffect>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _sweep;
  late final Animation<double> _glow;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3800),
    )..repeat();
    _sweep = Tween<double>(
      begin: -1.0,
      end: 2.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    _glow = Tween<double>(begin: 0.35, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOutSine),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ShaderMask buildGradientText() {
      return ShaderMask(
        blendMode: BlendMode.srcIn,
        shaderCallback: (bounds) {
          return LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: const [
              Color(0xFFB517FF),
              Color(0xFFFF4FA3),
              Color(0xFFE879F9),
              Color(0xFFB517FF),
            ],
            stops: [
              (_sweep.value - 0.45).clamp(0.0, 1.0),
              (_sweep.value - 0.15).clamp(0.0, 1.0),
              (_sweep.value + 0.15).clamp(0.0, 1.0),
              (_sweep.value + 0.45).clamp(0.0, 1.0),
            ],
          ).createShader(bounds);
        },
        child: widget.child,
      );
    }

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return Stack(
          alignment: Alignment.centerLeft,
          clipBehavior: Clip.none,
          children: [
            Transform.translate(
              offset: const Offset(0, 1.0),
              child: Opacity(
                opacity: 0.42 * _glow.value,
                child: buildGradientText(),
              ),
            ),
            buildGradientText(),
          ],
        );
      },
    );
  }
}
