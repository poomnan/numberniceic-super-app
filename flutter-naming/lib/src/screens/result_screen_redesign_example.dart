import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../models/name_model.dart';

class ResultScreenRedesignExample extends StatelessWidget {
  const ResultScreenRedesignExample({
    super.key,
    required this.query,
    required this.results,
    required this.totalResults,
    required this.filterSummary,
    required this.onSelectName,
  });

  final String query;
  final List<MobileNameResult> results;
  final int totalResults;
  final String filterSummary;
  final ValueChanged<MobileNameResult> onSelectName;

  static const Color ink = Color(0xFF112018);
  static const Color gold = Color(0xFFC7A85A);
  static const Color jade = Color(0xFF587A67);
  static const Color paper = Color(0xFFF8F5EE);
  static const Color card = Color(0xFFFFFCF6);
  static const Color muted = Color(0xFF6E746D);
  static const Color line = Color(0xFFE5DDCB);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: paper,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
          child: ResultPresentationSection(
            query: query,
            results: results,
            totalResults: totalResults,
            filterSummary: filterSummary,
            onSelectName: onSelectName,
          ),
        ),
      ),
    );
  }
}

class ResultPresentationSection extends StatelessWidget {
  const ResultPresentationSection({
    super.key,
    required this.query,
    required this.results,
    required this.totalResults,
    required this.filterSummary,
    required this.onSelectName,
    this.isLocked,
    this.onLockedTap,
  });

  final String query;
  final List<MobileNameResult> results;
  final int totalResults;
  final String filterSummary;
  final ValueChanged<MobileNameResult> onSelectName;
  final bool Function(MobileNameResult result)? isLocked;
  final Future<void> Function(MobileNameResult result)? onLockedTap;

  @override
  Widget build(BuildContext context) {
    final hero = results.isNotEmpty ? results.first : null;
    final secondary = results.length > 1
        ? results.sublist(1)
        : <MobileNameResult>[];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 4),
        if (hero != null)
          _HeroResultCard(
            result: hero,
            locked: isLocked?.call(hero) ?? false,
            onTap: () => onSelectName(hero),
            onLockedTap: () async {
              await onLockedTap?.call(hero);
            },
          ),
        if (hero != null) const SizedBox(height: 18),
        if (secondary.isNotEmpty)
          Text(
            'รายชื่อเด่นลำดับถัดไป',
            style: GoogleFonts.sarabun(
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: ResultScreenRedesignExample.ink,
            ),
          ),
        if (secondary.isNotEmpty) const SizedBox(height: 10),
        for (int i = 0; i < secondary.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _CompactResultCard(
              rank: i + 2,
              result: secondary[i],
              locked: isLocked?.call(secondary[i]) ?? false,
              onTap: () => onSelectName(secondary[i]),
              onLockedTap: () async {
                await onLockedTap?.call(secondary[i]);
              },
            ),
          ),
      ],
    );
  }
}

class _HeroResultCard extends StatelessWidget {
  const _HeroResultCard({
    required this.result,
    required this.onTap,
    this.locked = false,
    this.onLockedTap,
  });

  final MobileNameResult result;
  final VoidCallback onTap;
  final bool locked;
  final Future<void> Function()? onLockedTap;

  @override
  Widget build(BuildContext context) {
    final mood = _humanScore(result);
    final reasons = result.rankReasons.take(3).toList();

    return Container(
      decoration: BoxDecoration(
        color: ResultScreenRedesignExample.card,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(
          color: ResultScreenRedesignExample.gold,
          width: 1.2,
        ),
        boxShadow: const [
          BoxShadow(
            color: Color(0x14112018),
            blurRadius: 28,
            offset: Offset(0, 16),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'อันดับ 1 ที่เราอยากให้คุณเริ่มพิจารณา',
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1.0,
                          color: ResultScreenRedesignExample.jade,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Text(
                        result.name,
                        style: GoogleFonts.cormorantGaramond(
                          fontSize: 42,
                          height: 0.95,
                          fontWeight: FontWeight.w700,
                          color: ResultScreenRedesignExample.ink,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Text(
                        result.meaning,
                        style: GoogleFonts.sarabun(
                          fontSize: 16,
                          height: 1.6,
                          color: ResultScreenRedesignExample.muted,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                _ConfidenceOrb(label: mood.label),
              ],
            ),
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFFF5F0E3),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: _MetricColumn(
                      title: 'ความรู้สึกโดยรวม',
                      value: mood.headline,
                    ),
                  ),
                  Expanded(
                    child: _MetricColumn(
                      title: 'เลขศาสตร์',
                      value: result.isSatGood ? 'ส่งเสริมดี' : 'พอใช้ได้',
                    ),
                  ),
                  Expanded(
                    child: _MetricColumn(
                      title: 'พลังเงา',
                      value: result.isShaGood ? 'สมดุลดี' : 'ยังกลางๆ',
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            Text(
              'เหตุผลที่ชื่อนี้ขึ้นมาเป็นตัวเลือกหลัก',
              style: GoogleFonts.sarabun(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: ResultScreenRedesignExample.ink,
              ),
            ),
            const SizedBox(height: 10),
            ...reasons.isNotEmpty
                ? reasons.map((reason) => _ReasonLine(text: _humanizeReason(reason)))
                : [
                    const _ReasonLine(text: 'ความหมายชัดเจน ฟังดูน่าเชื่อถือ และให้ภาพลักษณ์ดี'),
                    const _ReasonLine(text: 'ผ่านเกณฑ์ที่ระบบให้น้ำหนักสูงในการคัดชื่อสำหรับคุณ'),
                    const _ReasonLine(text: 'เหมาะเป็นชื่อเริ่มต้นสำหรับนำไปพิจารณาต่อเชิงลึก'),
                  ],
            const SizedBox(height: 20),
            DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                gradient: const LinearGradient(
                  colors: [
                    ResultScreenRedesignExample.ink,
                    ResultScreenRedesignExample.jade,
                  ],
                ),
              ),
              child: ElevatedButton(
                onPressed: locked
                    ? () async {
                        await onLockedTap?.call();
                      }
                    : onTap,
                style: ElevatedButton.styleFrom(
                  elevation: 0,
                  backgroundColor: Colors.transparent,
                  shadowColor: Colors.transparent,
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(18),
                  ),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      locked
                          ? 'ปลดล็อกเพื่อใช้ชื่อนี้'
                          : 'เลือกชื่อนี้เพื่อดูรายละเอียดต่อ',
                      style: GoogleFonts.sarabun(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                    const Icon(
                      Icons.arrow_forward_rounded,
                      color: Colors.white,
                    ),
                  ],
                ),
              ),
            ),
            if (locked) ...[
              const SizedBox(height: 10),
              Text(
                'รายการนี้เป็นพรีเมียม แตะเพื่อปลดล็อก',
                style: GoogleFonts.sarabun(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: ResultScreenRedesignExample.jade,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _CompactResultCard extends StatelessWidget {
  const _CompactResultCard({
    required this.rank,
    required this.result,
    required this.onTap,
    this.locked = false,
    this.onLockedTap,
  });

  final int rank;
  final MobileNameResult result;
  final VoidCallback onTap;
  final bool locked;
  final Future<void> Function()? onLockedTap;

  @override
  Widget build(BuildContext context) {
    final mood = _humanScore(result);
    return InkWell(
      onTap: locked
          ? () async {
              await onLockedTap?.call();
            }
          : onTap,
      borderRadius: BorderRadius.circular(22),
      child: Ink(
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          color: ResultScreenRedesignExample.card,
          borderRadius: BorderRadius.circular(22),
          border: Border.all(color: ResultScreenRedesignExample.line),
        ),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: const Color(0xFFF3EEE0),
                borderRadius: BorderRadius.circular(14),
              ),
              alignment: Alignment.center,
              child: Text(
                '$rank',
                style: GoogleFonts.inter(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: ResultScreenRedesignExample.ink,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    result.name,
                    style: GoogleFonts.sarabun(
                      fontSize: 22,
                      fontWeight: FontWeight.w700,
                      color: ResultScreenRedesignExample.ink,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    result.meaning,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: GoogleFonts.sarabun(
                      fontSize: 14,
                      height: 1.5,
                      color: ResultScreenRedesignExample.muted,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  mood.shortLabel,
                  style: GoogleFonts.sarabun(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: ResultScreenRedesignExample.jade,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  locked
                      ? 'ปลดล็อกเพื่อใช้งาน'
                      : (result.isSatGood && result.isShaGood
                            ? 'พร้อมใช้งาน'
                            : 'น่าพิจารณา'),
                  style: GoogleFonts.sarabun(
                    fontSize: 12,
                    color: locked
                        ? ResultScreenRedesignExample.jade
                        : ResultScreenRedesignExample.muted,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ConfidenceOrb extends StatelessWidget {
  const _ConfidenceOrb({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 96,
      height: 96,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          color: ResultScreenRedesignExample.gold,
          width: 1.4,
        ),
        color: const Color(0xFFF5EEDC),
      ),
      alignment: Alignment.center,
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Text(
          label,
          textAlign: TextAlign.center,
          style: GoogleFonts.sarabun(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: ResultScreenRedesignExample.ink,
            height: 1.2,
          ),
        ),
      ),
    );
  }
}

class _MetricColumn extends StatelessWidget {
  const _MetricColumn({
    required this.title,
    required this.value,
  });

  final String title;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: GoogleFonts.sarabun(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: ResultScreenRedesignExample.muted,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: GoogleFonts.sarabun(
            fontSize: 16,
            fontWeight: FontWeight.w700,
            color: ResultScreenRedesignExample.ink,
          ),
        ),
      ],
    );
  }
}

class _ReasonLine extends StatelessWidget {
  const _ReasonLine({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 8,
            height: 8,
            margin: const EdgeInsets.only(top: 7),
            decoration: const BoxDecoration(
              color: ResultScreenRedesignExample.gold,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: GoogleFonts.sarabun(
                fontSize: 14,
                height: 1.55,
                color: ResultScreenRedesignExample.ink,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

_HumanScore _humanScore(MobileNameResult? result) {
  if (result == null) {
    return const _HumanScore(
      label: 'กำลังคัดเลือก',
      shortLabel: 'กำลังคัด',
      headline: 'อยู่ระหว่างประเมิน',
    );
  }

  final score = result.finalRankScore > 0
      ? result.finalRankScore
      : result.calculateScore();

  if (score >= 90) {
    return const _HumanScore(
      label: 'เหมาะมาก',
      shortLabel: 'เหมาะมาก',
      headline: 'ลงตัวที่สุดสำหรับรอบนี้',
    );
  }
  if (score >= 80) {
    return const _HumanScore(
      label: 'น่าเลือกมาก',
      shortLabel: 'เด่นมาก',
      headline: 'สมดุลและน่าใช้จริง',
    );
  }
  if (score >= 70) {
    return const _HumanScore(
      label: 'น่าพิจารณา',
      shortLabel: 'น่าพิจารณา',
      headline: 'มีศักยภาพที่ดี',
    );
  }
  return const _HumanScore(
    label: 'พอใช้ได้',
    shortLabel: 'พอใช้ได้',
    headline: 'อาจเหมาะในบางบริบท',
  );
}

String _humanizeReason(String raw) {
  final text = raw.trim();
  if (text.isEmpty) return 'ระบบให้คะแนนด้านความหมายและความเหมาะสมไว้ในระดับดี';
  return text
      .replaceAll('semantic', 'ความหมาย')
      .replaceAll('ranking', 'การจัดลำดับ')
      .replaceAll('score', 'ความเหมาะสม');
}

class _HumanScore {
  const _HumanScore({
    required this.label,
    required this.shortLabel,
    required this.headline,
  });

  final String label;
  final String shortLabel;
  final String headline;
}
