import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/colors.dart';

class InformationScreen extends StatelessWidget {
  final int initialTabIndex;

  const InformationScreen({super.key, this.initialTabIndex = 0});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 4,
      initialIndex: initialTabIndex,
      child: Scaffold(
        backgroundColor: const Color(0xFF0F172A), // Premium Midnight Black
        appBar: AppBar(
          backgroundColor: const Color(0xFF0F172A),
          elevation: 0,
          title: Text(
            "ความรู้เรื่องชื่อมงคล",
            style: GoogleFonts.prompt(
              fontWeight: FontWeight.bold,
              fontSize: 18,
              color: Colors.white,
            ),
          ),
          iconTheme: const IconThemeData(color: Colors.white),
          bottom: TabBar(
            isScrollable: true,
            labelColor: AppColors.accent, // Gold
            unselectedLabelColor: Colors.white.withValues(alpha: 0.4),
            indicatorColor: AppColors.accent,
            indicatorWeight: 3,
            labelStyle: GoogleFonts.prompt(
              fontWeight: FontWeight.w800,
              fontSize: 13,
            ),
            unselectedLabelStyle: GoogleFonts.prompt(fontSize: 13),
            tabs: const [
              Tab(text: "เลขศาสตร์ & พลังเงา"),
              Tab(text: "กาลกิณี"),
              Tab(text: "ระบบอัจฉริยะ (AI)"),
              Tab(text: "การจัดอันดับชื่อ"),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _buildNumerologyPage(context),
            _buildKakiPage(context),
            _buildAIPage(context),
            _buildRankingPage(context),
          ],
        ),
      ),
    );
  }

  // ─── Tab 0: เลขศาสตร์ & พลังเงา ─────────────────────────────────────────
  Widget _buildNumerologyPage(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 40),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeroSection(
            icon: Icons.auto_awesome_motion_rounded,
            title: "เลขศาสตร์ & พลังเงา",
            subtitle: "วิศวกรรมตัวเลขพรีเมียม (Advanced SAT & SHA)",
            colors: [const Color(0xFFDBB632), AppColors.secondary],
          ),
          const SizedBox(height: 28),
          _buildFeatureCard(
            number: "SAT",
            title: "เลขศาสตร์ (Numerology)",
            description:
                "การถอดรหัสชื่อตามอิทธิพลความถี่ของตัวเลข (1-100) ผ่านพยัญชนะ สระ และวรรณยุกต์ เพื่อหา 'เลขกำลังดาว' ที่ส่งเสริมดวงชะตา",
            bullets: [
              "วิเคราะห์ SAT จากชื่อและนามสกุลเพื่อหาความสมดุล",
              "คัดเฉพาะคู่เลขมงคล (Auspicious Numbers) ที่ส่งเสริมด้านโชคลาภ",
            ],
            icon: Icons.calculate_rounded,
            color: const Color(0xFF4ADE80),
          ),
          const SizedBox(height: 16),
          _buildFeatureCard(
            number: "SHA",
            title: "พลังเงา (Shadow Power)",
            description:
                "พลังแฝงที่ซ่อนเร้นอยู่เบื้องหลังชื่อ เปรียบเสมือน 'รากฐาน' ของชีวิต บ่งบอกถึงความมั่นคงและบารมีของผู้ครอบครอง",
            bullets: [
              "วิเคราะห์แรงดึงดูดจากดวงดาวที่ส่งอิทธิพลต่อชื่อ",
              "ปรับสมดุลพยัญชนะเพื่อให้พลัง SHA หนุนนำชีวิตให้ยั่งยืน",
            ],
            icon: Icons.blur_on_rounded,
            color: const Color(0xFF34D399),
          ),
          const SizedBox(height: 24),
          _buildTip(
            "ชื่อชั้นเลิศต้องผ่านทั้ง SAT และ SHA พร้อมกัน เพื่อสถานะ 'Double Lucky' ที่สมบูรณ์แบบ",
          ),
        ],
      ),
    );
  }

  // ─── Tab 1: กาลกิณี ──────────────────────────────────────────────────────
  Widget _buildKakiPage(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 40),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeroSection(
            icon: Icons.shield_rounded,
            title: "อักษรกาลกิณี",
            subtitle: "การคัดออกอักขระอริต่อดวงชะตา",
            colors: [const Color(0xFFEF4444), const Color(0xFFB91C1C)],
          ),
          const SizedBox(height: 28),
          _buildFeatureCard(
            number: "📜",
            title: "คัมภีร์มหาทักษา",
            description:
                "อ้างอิงตามตำราโบราณที่ระบุว่าผู้เกิดในแต่ละวันจะมีตัวอักขระที่เป็น 'กาลกิณี' ซึ่งเปรียบเสมือนอุปสรรคขัดขวางความเจริญ",
            bullets: [
              "ระบบจะทำการสแกนพยัญชนะและสระทุกตัวตามวันเกิด",
              "หากพบอักษรต้องห้าม จะแสดงแถบสีแดงเป็นจุดสังเกตทันที",
              "ชื่อมงคลที่ดีที่สุดควรหลีกเลี่ยงกาลกิณีโดยสิ้นเชิง",
            ],
            icon: Icons.warning_amber_rounded,
            color: const Color(0xFFF87171),
          ),
          const SizedBox(height: 24),
          _buildTip(
            "การปลอดกาลกิณีคือจุดเริ่มต้นของการเสริมดวงชะตาที่เรียบง่ายแต่ทรงพลัง",
          ),
        ],
      ),
    );
  }

  // ─── Tab 2: ระบบ AI ──────────────────────────────────────────────────────
  Widget _buildAIPage(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 40),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeroSection(
            icon: Icons.psychology_rounded,
            title: "ระบบอัจฉริยะ (AI)",
            subtitle: "Semantic Intelligence Search",
            colors: [AppColors.primary, AppColors.secondary],
          ),
          const SizedBox(height: 28),
          _buildStepCard(
            step: 1,
            title: "ค้นหาด้วย Semantic Search",
            subtitle: "มากกว่าแค่การจับคู่คำ",
            description:
                "ระบบใช้ปัญญาประดิษฐ์วิเคราะห์ 'เจตนารมณ์' และ 'บริบท' จากคำที่คุณพิมพ์ เพื่อค้นหาชื่อที่มีความหมายลึกซึ้งใกล้เคียงที่สุด",
            icon: Icons.search_rounded,
            color: const Color(0xFF60A5FA),
          ),
          const SizedBox(height: 12),
          _buildStepCard(
            step: 2,
            title: "Embedding Vector Analysis",
            subtitle: "การประมวลผลผ่านความหมาย",
            description:
                "ชื่อทุกชื่อถูกแปลงเป็นเวกเตอร์อัจฉริยะในพื้นที่อาณาจักร 1,536 มิติ เพื่อวัดค่าความแม่นยำของความหมายให้ตรงใจคุณ",
            icon: Icons.auto_awesome,
            color: const Color(0xFFDBB632),
            magic: true,
          ),
          const SizedBox(height: 12),
          _buildStepCard(
            step: 3,
            title: "การทำ Root Word Analysis",
            subtitle: "การถอดรากศัพท์พรีเมียม",
            description:
                "ระบบจะแจกแจงที่มาของชื่อให้เห็นชัดเจน ว่าถูกประกอบขึ้นจากคำมงคลบาลี-สันสกฤตคำไหนบ้าง",
            icon: Icons.history_edu_rounded,
            color: Colors.white,
          ),
          const SizedBox(height: 24),
          _buildTip(
            "วิเคราะห์จากฐานข้อมูลชื่อจริงคุณภาพสูงกว่า 300,000 รายชื่อ",
          ),
        ],
      ),
    );
  }

  // ─── Tab 3: การจัดอันดับ ─────────────────────────────────────────────────
  Widget _buildRankingPage(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 40),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeroSection(
            icon: Icons.emoji_events_rounded,
            title: "การจัดอันดับชื่อ",
            subtitle: "Ranking by selected criteria",
            colors: [const Color(0xFFDBB632), const Color(0xFFB45309)],
          ),
          const SizedBox(height: 28),
          _buildFeatureCard(
            number: "1",
            title: "ต้องมีชื่ออ้างอิงและเลือกเงื่อนไขก่อนจัดอันดับ",
            description:
                "ระบบจะจัดอันดับเมื่อมีชื่อหรือความหมายที่แปลงเป็นแม่แบบชื่อได้ และคุณเปิดอย่างน้อยหนึ่งตัวคัด คือ 'คัดเลขศาสตร์ดี' หรือ 'คัดพลังเงาดี' เพื่อให้รายชื่อที่แสดงผ่านเกณฑ์เดียวกับที่ใช้เรียงอันดับ",
            bullets: [
              "ยังไม่มีแม่แบบชื่อหรือยังไม่เลือกตัวคัด: ระบบแสดงไอเดียชื่อ ยังไม่แสดงอันดับ",
              "เลือกคัดเลขศาสตร์ดี: แสดงเฉพาะชื่อที่ผ่าน SAT แล้วเรียงตามคะแนนอันดับ",
              "เลือกคัดพลังเงาดี: แสดงเฉพาะชื่อที่ผ่าน SHA แล้วเรียงตามคะแนนอันดับ",
              "เลือกทั้งสองตัวคัด: ชื่อต้องผ่านทั้ง SAT และ SHA ก่อนจึงเข้า ranking list",
            ],
            icon: Icons.rule_rounded,
            color: const Color(0xFFDBB632),
          ),
          const SizedBox(height: 16),
          _buildFeatureCard(
            number: "2",
            title: "ความหมายเป็นด่านแรก คะแนนอันดับเป็นแกนหลัก",
            description:
                "ระบบเริ่มจาก semantic retrieval เพื่อหาชื่อที่มีความหมายใกล้เคียงก่อน จากนั้นใช้คะแนนจัดอันดับจาก backend เป็นหลัก หากไม่มีคะแนนนี้จึงคำนวณจากคะแนนชื่อในแอปเป็น fallback",
            bullets: [
              "ใช้ final_rank_score ก่อนเสมอเมื่อ backend ส่งกลับมา",
              "หากคะแนนอันดับเท่ากัน ระบบดูความแข็งแรงของคู่เลข D10/D8/D5 และ pairpoint",
              "semantic score ใช้ช่วยตัดสินลำดับท้าย ๆ เมื่อคุณภาพชื่อใกล้กันมาก",
            ],
            icon: Icons.account_tree_rounded,
            color: const Color(0xFF60A5FA),
          ),
          const SizedBox(height: 20),

          _buildBonusCard(
            label: "ความแม่นยำความหมาย",
            sublabel: "Semantic Score",
            points: "Base",
            description:
                "ใช้เพื่อหากลุ่มชื่อที่ตรงความหมายก่อน แล้วค่อยให้เงื่อนไขที่คุณเลือกเป็นตัวจัดอันดับ",
            icon: Icons.search_rounded,
            color: const Color(0xFF60A5FA),
            highlight: false,
          ),
          const SizedBox(height: 10),
          _buildBonusCard(
            label: "โบนัสเลขศาสตร์",
            sublabel: "SAT Bonus",
            points: "+20pt",
            description:
                "ใช้เมื่อชื่อผ่านเลขศาสตร์ดี และจะเป็นเงื่อนไขคัดกรองเมื่อเปิด 'คัดเลขศาสตร์ดี'",
            icon: Icons.calculate_rounded,
            color: const Color(0xFF4ADE80),
            highlight: false,
          ),
          const SizedBox(height: 10),
          _buildBonusCard(
            label: "โบนัสพลังเงา",
            sublabel: "SHA Bonus",
            points: "+20pt",
            description:
                "ใช้เมื่อชื่อผ่านพลังเงาดี และจะเป็นเงื่อนไขคัดกรองเมื่อเปิด 'คัดพลังเงาดี'",
            icon: Icons.grid_view_rounded,
            color: const Color(0xFF34D399),
            highlight: false,
          ),
          const SizedBox(height: 10),
          _buildBonusCard(
            label: "Double Lucky ⭐",
            sublabel: "Elite Grand Bonus",
            points: "+50pt",
            description:
                "ชื่อที่ผ่านทั้งเลขศาสตร์ดีและพลังเงาดีจะได้แรงส่งพิเศษ โดยเฉพาะเมื่อเปิดทั้งสองตัวคัดพร้อมกัน",
            icon: Icons.auto_awesome,
            color: const Color(0xFFDBB632),
            highlight: true,
          ),
          const SizedBox(height: 10),
          _buildBonusCard(
            label: "คุณภาพคู่เลข",
            sublabel: "PairType / Pairpoint",
            points: "Tie-break",
            description:
                "เมื่อคะแนนอันดับใกล้กัน ระบบให้คู่เลขที่แข็งแรงกว่า เช่น D10, D8, D5 และ pairpoint สูงกว่า ขึ้นก่อนตามแกนที่คุณเลือกคัด",
            icon: Icons.linear_scale_rounded,
            color: const Color(0xFF38BDF8),
            highlight: false,
          ),
          const SizedBox(height: 10),
          _buildBonusCard(
            label: "โบนัสปลอดกาลกิณี",
            sublabel: "Crystal Clean Bonus",
            points: "+10pt",
            description:
                "ชื่อที่ไม่มีอักษรกาลกิณีตามวันเกิดของคุณเลยแม้แต่ตัวเดียว",
            icon: Icons.shield_rounded,
            color: const Color(0xFFA78BFA),
            highlight: false,
          ),
          const SizedBox(height: 10),
          _buildBonusCard(
            label: "ความทันสมัย-กระชับ",
            sublabel: "Aesthetic Bonus",
            points: "+10pt",
            description: "ชื่อที่มีความยาวเหมาะสมและเลือกใช้พยัญชนะที่สละสลวย",
            icon: Icons.short_text_rounded,
            color: const Color(0xFFFBBF24),
            highlight: false,
          ),
          const SizedBox(height: 28),

          Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            decoration: BoxDecoration(
              color: const Color(0xFF1E293B),
              borderRadius: BorderRadius.circular(24),
              border: Border.all(
                color: const Color(0xFFDBB632).withValues(alpha: 0.3),
                width: 1.5,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.3),
                  blurRadius: 20,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFFDBB632).withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Icon(
                    Icons.emoji_events_rounded,
                    color: Color(0xFFDBB632),
                    size: 32,
                  ),
                ),
                const SizedBox(width: 20),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "เป้าหมายคะแนนสูงสุด",
                        style: GoogleFonts.prompt(
                          color: Colors.white.withValues(alpha: 0.6),
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        "ขึ้นกับเงื่อนไข",
                        style: GoogleFonts.prompt(
                          color: const Color(0xFFDBB632),
                          fontSize: 26,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ─── Shared Widgets ───────────────────────────────────────────────────────

  Widget _buildHeroSection({
    required IconData icon,
    required String title,
    required String subtitle,
    required List<Color> colors,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: colors[0].withValues(alpha: 0.2), width: 1.5),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.2),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: colors[0].withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Icon(icon, size: 36, color: colors[0]),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.prompt(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  subtitle,
                  style: GoogleFonts.sarabun(
                    color: colors[0],
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFeatureCard({
    required String number,
    required String title,
    required String description,
    required List<String> bullets,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withValues(alpha: 0.15), width: 1.2),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  number,
                  style: GoogleFonts.prompt(
                    color: color,
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  title,
                  style: GoogleFonts.prompt(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Icon(icon, color: color.withValues(alpha: 0.4), size: 20),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            description,
            style: GoogleFonts.sarabun(
              color: Colors.white.withValues(alpha: 0.7),
              fontSize: 14,
              height: 1.6,
            ),
          ),
          const SizedBox(height: 12),
          ...bullets.map(
            (b) => Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.check_circle_rounded, size: 14, color: color),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      b,
                      style: GoogleFonts.sarabun(
                        color: Colors.white.withValues(alpha: 0.5),
                        fontSize: 13,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStepCard({
    required int step,
    required String title,
    required String subtitle,
    required String description,
    required IconData icon,
    required Color color,
    bool magic = false,
  }) {
    if (magic) {
      return _MagicStepCard(
        step: step,
        title: title,
        subtitle: subtitle,
        description: description,
        icon: icon,
        color: color,
      );
    }
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: color.withValues(alpha: 0.15), width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Step number
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Center(
              child: Text(
                '$step',
                style: GoogleFonts.prompt(
                  color: color,
                  fontSize: 16,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.prompt(
                    color: Colors.white,
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  subtitle,
                  style: GoogleFonts.sarabun(
                    color: color,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  description,
                  style: GoogleFonts.sarabun(
                    color: Colors.white.withValues(alpha: 0.6),
                    fontSize: 13,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
          Icon(icon, color: color.withValues(alpha: 0.4), size: 20),
        ],
      ),
    );
  }

  Widget _buildBonusCard({
    required String label,
    required String sublabel,
    required String points,
    required String description,
    required IconData icon,
    required Color color,
    required bool highlight,
  }) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: highlight
              ? color.withValues(alpha: 0.6)
              : Colors.white.withValues(alpha: 0.1),
          width: highlight ? 2.0 : 1.0,
        ),
        boxShadow: highlight
            ? [
                BoxShadow(
                  color: color.withValues(alpha: 0.12),
                  blurRadius: 12,
                  offset: const Offset(0, 6),
                ),
              ]
            : [],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(icon, color: color, size: 22),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                      child: Text(
                        label,
                        style: GoogleFonts.prompt(
                          color: Colors.white,
                          fontSize: 15,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                    if (highlight) ...[
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 1,
                        ),
                        decoration: BoxDecoration(
                          color: color.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          "Best",
                          style: GoogleFonts.prompt(
                            color: color,
                            fontSize: 10,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  description,
                  style: GoogleFonts.sarabun(
                    color: Colors.white.withValues(alpha: 0.5),
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: color.withValues(alpha: 0.2)),
            ),
            child: Text(
              points,
              style: GoogleFonts.prompt(
                color: color,
                fontSize: 13,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTip(String text) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B).withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.3)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.lightbulb_rounded,
            color: AppColors.primary,
            size: 18,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: GoogleFonts.sarabun(
                color: Colors.white.withValues(alpha: 0.7),
                fontSize: 13,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Magic Step Card ──────────────────────────────────────────────────────────
class _MagicStepCard extends StatefulWidget {
  final int step;
  final String title;
  final String subtitle;
  final String description;
  final IconData icon;
  final Color color;

  const _MagicStepCard({
    required this.step,
    required this.title,
    required this.subtitle,
    required this.description,
    required this.icon,
    required this.color,
  });

  @override
  State<_MagicStepCard> createState() => _MagicStepCardState();
}

class _MagicStepCardState extends State<_MagicStepCard>
    with TickerProviderStateMixin {
  late AnimationController _glowController;
  late AnimationController _shimmerController;
  late AnimationController _particleController;
  late Animation<double> _glowAnim;
  late Animation<double> _shimmerAnim;
  late List<_Particle> _particles;

  @override
  void initState() {
    super.initState();

    // Pulsing glow on border
    _glowController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    )..repeat(reverse: true);
    _glowAnim = CurvedAnimation(
      parent: _glowController,
      curve: Curves.easeInOut,
    );

    // Shimmer sweep
    _shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2200),
    )..repeat();
    _shimmerAnim = _shimmerController;

    // Floating particles
    _particleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3000),
    )..repeat();

    final rng = math.Random(42);
    _particles = List.generate(
      8,
      (i) => _Particle(
        x: rng.nextDouble(),
        y: rng.nextDouble(),
        size: 2.0 + rng.nextDouble() * 3,
        speed: 0.3 + rng.nextDouble() * 0.7,
        phase: rng.nextDouble(),
      ),
    );
  }

  @override
  void dispose() {
    _glowController.dispose();
    _shimmerController.dispose();
    _particleController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final color = widget.color;
    return AnimatedBuilder(
      animation: Listenable.merge([
        _glowAnim,
        _shimmerAnim,
        _particleController,
      ]),
      builder: (context, _) {
        final glow = _glowAnim.value;
        return Container(
          decoration: BoxDecoration(
            color: const Color(0xFF1E293B),
            borderRadius: BorderRadius.circular(18),
            boxShadow: [
              BoxShadow(
                color: color.withValues(alpha: 0.12 + glow * 0.15),
                blurRadius: 15 + glow * 10,
                spreadRadius: glow * 2,
              ),
              BoxShadow(
                color: const Color(
                  0xFF8B5CF6,
                ).withValues(alpha: 0.08 + glow * 0.1),
                blurRadius: 25 + glow * 15,
                spreadRadius: glow * 1,
              ),
            ],
            border: Border.all(
              color: color.withValues(alpha: 0.2 + glow * 0.3),
              width: 1.5,
            ),
          ),
          child: Stack(
            children: [
              // Shimmer sweep overlay
              Positioned.fill(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(18),
                  child: CustomPaint(
                    painter: _ShimmerPainter(
                      progress: _shimmerAnim.value,
                      color: color,
                    ),
                  ),
                ),
              ),
              // Floating sparkle particles
              Positioned.fill(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(18),
                  child: CustomPaint(
                    painter: _ParticlePainter(
                      particles: _particles,
                      progress: _particleController.value,
                      color: color,
                    ),
                  ),
                ),
              ),
              // Content container
              Container(
                padding: const EdgeInsets.all(18),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Animated step badge
                    Container(
                      width: 36,
                      height: 36,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [
                            color,
                            Color.lerp(
                              color,
                              const Color(0xFF8B5CF6),
                              0.5 + glow * 0.5,
                            )!,
                          ],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: BorderRadius.circular(10),
                        boxShadow: [
                          BoxShadow(
                            color: color.withValues(alpha: 0.4 + glow * 0.4),
                            blurRadius: 8 + glow * 8,
                            spreadRadius: glow * 2,
                          ),
                        ],
                      ),
                      child: Center(
                        child: Text(
                          '${widget.step}',
                          style: GoogleFonts.prompt(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            widget.title,
                            style: GoogleFonts.prompt(
                              color: Colors.white,
                              fontSize: 16,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          // Animated subtitle with shimmer text effect
                          ShaderMask(
                            shaderCallback: (bounds) => LinearGradient(
                              colors: [
                                color,
                                const Color(0xFF8B5CF6),
                                AppColors.primary,
                                color,
                              ],
                              stops: [
                                0.0,
                                (_shimmerAnim.value - 0.1).clamp(0.0, 1.0),
                                _shimmerAnim.value.clamp(0.0, 1.0),
                                1.0,
                              ],
                            ).createShader(bounds),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    Icons.auto_awesome,
                                    size: 10,
                                    color: Colors.white.withValues(alpha: 0.8),
                                  ),
                                  const SizedBox(width: 4),
                                  Text(
                                    widget.subtitle,
                                    style: GoogleFonts.sarabun(
                                      color: Colors.white,
                                      fontSize: 12,
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            widget.description,
                            style: GoogleFonts.sarabun(
                              color: Colors.white.withValues(alpha: 0.6),
                              fontSize: 13,
                              height: 1.5,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Icon(
                      widget.icon,
                      color: color.withValues(alpha: 0.7),
                      size: 24,
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _Particle {
  final double x, y, size, speed, phase;
  _Particle({
    required this.x,
    required this.y,
    required this.size,
    required this.speed,
    required this.phase,
  });
}

class _ShimmerPainter extends CustomPainter {
  final double progress;
  final Color color;
  _ShimmerPainter({required this.progress, required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final sweepX = -size.width + progress * size.width * 2.8;
    final paint = Paint()
      ..shader = LinearGradient(
        colors: [
          Colors.transparent,
          color.withValues(alpha: 0.12),
          AppColors.primary.withValues(alpha: 0.2),
          color.withValues(alpha: 0.12),
          Colors.transparent,
        ],
        stops: const [0.0, 0.3, 0.5, 0.7, 1.0],
        begin: Alignment.centerLeft,
        end: Alignment.centerRight,
        transform: GradientRotation(math.pi / 6),
      ).createShader(Rect.fromLTWH(sweepX, 0, size.width * 0.8, size.height));
    canvas.drawRect(
      Rect.fromLTWH(sweepX, 0, size.width * 0.8, size.height),
      paint,
    );
  }

  @override
  bool shouldRepaint(_ShimmerPainter old) => old.progress != progress;
}

class _ParticlePainter extends CustomPainter {
  final List<_Particle> particles;
  final double progress;
  final Color color;
  _ParticlePainter({
    required this.particles,
    required this.progress,
    required this.color,
  });

  @override
  void paint(Canvas canvas, Size size) {
    for (final p in particles) {
      final t = (progress * p.speed + p.phase) % 1.0;
      // Float upward
      final px =
          p.x * size.width + math.sin(t * math.pi * 2 + p.phase * 10) * 8;
      final py = size.height - (t * (size.height + 20));
      final opacity = math.sin(t * math.pi).clamp(0.0, 1.0);
      final paint = Paint()
        ..color = color.withValues(alpha: opacity * 0.9)
        ..style = PaintingStyle.fill;
      // Draw sparkle as 4-point star
      _drawSparkle(
        canvas,
        Offset(px, py),
        p.size * opacity,
        paint,
        color.withValues(alpha: opacity * 0.6),
      );
    }
  }

  void _drawSparkle(
    Canvas canvas,
    Offset center,
    double size,
    Paint paint,
    Color dimColor,
  ) {
    final path = Path();
    for (int i = 0; i < 4; i++) {
      final angle = i * math.pi / 2;
      final tip = Offset(
        center.dx + math.cos(angle) * size * 2,
        center.dy + math.sin(angle) * size * 2,
      );
      final left = Offset(
        center.dx + math.cos(angle + math.pi / 2) * size * 0.4,
        center.dy + math.sin(angle + math.pi / 2) * size * 0.4,
      );
      final right = Offset(
        center.dx + math.cos(angle - math.pi / 2) * size * 0.4,
        center.dy + math.sin(angle - math.pi / 2) * size * 0.4,
      );
      if (i == 0) path.moveTo(left.dx, left.dy);
      path.lineTo(tip.dx, tip.dy);
      path.lineTo(right.dx, right.dy);
      path.lineTo(center.dx, center.dy);
    }
    path.close();
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(_ParticlePainter old) => old.progress != progress;
}
