<?php

namespace App\Managers;

/**
 * Helper for Thai astrological calculations.
 * The lunisolar arithmetic follows the documented Khmer/Thai-family calendar
 * computer method with a Gregorian epoch of 1900-01-01 = เดือน 2 ขึ้น 1 ค่ำ.
 *
 * This keeps backend logic dynamic across years instead of relying on
 * hand-maintained per-year anchor tables.
 */
class ThaiCalendarHelper
{
    private const DEFAULT_RANGE_YEARS_BEFORE = 1;
    private const DEFAULT_RANGE_YEARS_AFTER = 1;
    private const DEFAULT_SONGKRAN_CUTOVER_MONTH_DAY = '04-16';

    private const LUNAR_EPOCH_GREGORIAN_YEAR = 1900;
    private const LUNAR_EPOCH_MONTH = 2;
    private const LUNAR_EPOCH_DITHI = 1;

    private const KALAYOK_TONGCHAI_BY_CS_REMAINDER = [
        1 => 5,
        2 => 1,
        3 => 4,
        4 => 7,
        5 => 3,
        6 => 6,
        7 => 2,
    ];

    private const KALAYOK_ATIPBADEE_BY_CS_REMAINDER = [
        1 => 5,
        2 => 6,
        3 => 7,
        4 => 1,
        5 => 2,
        6 => 3,
        7 => 4,
    ];

    private const KALAYOK_UBATH_BY_CS_REMAINDER = [
        1 => 4,
        2 => 7,
        3 => 3,
        4 => 6,
        5 => 2,
        6 => 5,
        7 => 1,
    ];

    private const KALAYOK_LOKAWINAT_BY_CS_REMAINDER = [
        1 => 7,
        2 => 1,
        3 => 2,
        4 => 3,
        5 => 4,
        6 => 5,
        7 => 6,
    ];

    private const MYHORA_LOY_FU_JOM_RESIDUES = [
        1 => ['loy' => 6, 'fu' => 1, 'jom' => 3],
        2 => ['loy' => 0, 'fu' => 2, 'jom' => 4],
        3 => ['loy' => 5, 'fu' => 0, 'jom' => 2],
        4 => ['loy' => 5, 'fu' => 0, 'jom' => 2],
        5 => ['loy' => 4, 'fu' => 6, 'jom' => 1],
        6 => ['loy' => 6, 'fu' => 1, 'jom' => 3],
        7 => ['loy' => 5, 'fu' => 0, 'jom' => 2],
        8 => ['loy' => 5, 'fu' => 0, 'jom' => 2],
        9 => ['loy' => 2, 'fu' => 4, 'jom' => 6],
        10 => ['loy' => 1, 'fu' => 3, 'jom' => 5],
        11 => ['loy' => 1, 'fu' => 3, 'jom' => 5],
        12 => ['loy' => 0, 'fu' => 2, 'jom' => 4],
    ];

    private const MYHORA_SOLAR_MAHASUN_DAY_BY_ZODIAC = [
        'เมษ' => 6,
        'พฤษภ' => 4,
        'มิถุน' => 8,
        'กรกฎ' => 6,
        'สิงห์' => 10,
        'กันย์' => 8,
        'ตุล' => 12,
        'พิจิก' => 10,
        'ธนู' => 2,
        'มกร' => 12,
        'กุมภ์' => 4,
        'มีน' => 2,
    ];

    private const MYHORA_LUNAR_MAHASUN_DAY_BY_MONTH = [
        6 => 4,
        3 => 4,
        7 => 8,
        10 => 8,
        8 => 6,
        5 => 6,
        11 => 12,
        2 => 12,
        9 => 10,
        12 => 10,
        1 => 2,
        4 => 2,
    ];

    private const MYHORA_AYAKARN_RULES = [
        3 => [4 => 'ปฐม', 5 => 'ทุติยะ', 6 => 'ตติยะ'],
        7 => [4 => 'ปฐม', 5 => 'ทุติยะ', 6 => 'ตติยะ'],
        4 => [1 => 'ปฐม', 2 => 'ทุติยะ', 3 => 'ตติยะ'],
        10 => [1 => 'ปฐม', 2 => 'ทุติยะ', 3 => 'ตติยะ'],
        5 => [13 => 'ปฐม', 14 => 'ทุติยะ', 15 => 'ตติยะ'],
        11 => [13 => 'ปฐม', 14 => 'ทุติยะ', 15 => 'ตติยะ'],
        6 => [10 => 'ปฐม', 11 => 'ทุติยะ', 12 => 'ตติยะ'],
        8 => [6 => 'ปฐม', 7 => 'ทุติยะ', 8 => 'ตติยะ'],
        9 => [3 => 'ปฐม', 4 => 'ทุติยะ', 5 => 'ตติยะ'],
        12 => [2 => 'ปฐม', 3 => 'ทุติยะ', 4 => 'ตติยะ'],
        1 => [9 => 'ปฐม', 10 => 'ทุติยะ', 11 => 'ตติยะ'],
        2 => [7 => 'ปฐม', 8 => 'ทุติยะ', 9 => 'ตติยะ'],
    ];

    private const MYHORA_TRATHUEK_DAY_BY_MONTH = [
        5 => 7,
        6 => 7,
        7 => 7,
        8 => 8,
        9 => 8,
        10 => 8,
        11 => 9,
        12 => 9,
        1 => 9,
        2 => 4,
        3 => 4,
        4 => 4,
    ];

    private const MYHORA_PRIMARY_BAD_PRIORITY = [
        'กาลกรรณี',
        'พิฆาต',
        'ทินสูรย์',
        'กาลทิน',
        'มฤตยู',
        'บอด',
        'วินาศ',
        'โลกาวินาศ',
        'กาลสูร',
        'กาลโชค',
        'ทินศูร',
        'ทินกาล',
        'ทัคธทิน',
        'ยมขันธ์',
        'ทักทิน',
    ];

    private const MYHORA_PRIMARY_BAD_RULES = [
        'ทักทิน' => [7 => [1], 1 => [4], 2 => [5], 3 => [9], 4 => [5], 5 => [3], 6 => [7]],
        'ยมขันธ์' => [7 => [12], 1 => [11], 2 => [7], 3 => [3], 4 => [6], 5 => [8], 6 => [9]],
        'ทัคธทิน' => [7 => [4], 1 => [6], 2 => [1], 3 => [3], 4 => [3], 5 => [9], 6 => [1]],
        'ทินกาล' => [7 => [12], 1 => [10], 2 => [15], 3 => [8], 4 => [5], 5 => [3, 7], 6 => [8]],
        'ทินศูร' => [7 => [4], 1 => [6], 2 => [1], 3 => [3], 4 => [8], 5 => [9], 6 => [10]],
        'กาลโชค' => [7 => [4], 1 => [2], 2 => [7], 3 => [5], 4 => [8], 5 => [3], 6 => [6]],
        'กาลสูร' => [7 => [12], 1 => [11], 2 => [10], 3 => [9], 4 => [8], 5 => [7], 6 => [6]],
        'กาลทัณฑ์' => [7 => [4], 1 => [6], 2 => [10], 3 => [9], 4 => [8], 5 => [9], 6 => [1]],
        'โลกาวินาศ' => [7 => [4], 1 => [5], 2 => [6], 3 => [6], 4 => [8], 5 => [8], 6 => [9]],
        'วินาศ' => [7 => [6], 1 => [10], 2 => [8], 3 => [7], 4 => [12], 5 => [9], 6 => [12]],
        'พิลา' => [7 => [9], 1 => [1], 2 => [10], 3 => [9], 4 => [8], 5 => [7], 6 => [6]],
        'มฤตยู' => [7 => [7], 1 => [8], 2 => [4], 3 => [7], 4 => [1], 5 => [14], 6 => [11]],
        'บอด' => [7 => [5], 1 => [6], 2 => [10], 3 => [8], 4 => [11], 5 => [5], 6 => [7]],
        'กาลทิน' => [7 => [12], 1 => [11], 2 => [7], 3 => [3], 4 => [6], 5 => [8], 6 => [9]],
        'พิฆาต' => [7 => [12], 1 => [11], 2 => [7], 3 => [3], 4 => [6], 5 => [9], 6 => [8]],
        'ทินสูรย์' => [7 => [4], 1 => [6], 2 => [1], 3 => [3], 4 => [3, 7], 5 => [9], 6 => [1]],
        'กาลกรรณี' => [7 => [5], 1 => [6], 2 => [15], 3 => [8], 4 => [1], 5 => [2], 6 => [10]],
    ];

    private const MAHAMODO_BAD_RULES = [
        'ทึกทึน' => [7 => [1], 1 => [4], 2 => [6], 3 => [9], 4 => [5], 5 => [3], 6 => [7]],
        'ทรทึก' => [7 => [4], 1 => [6], 2 => [1], 3 => [3], 4 => [8], 5 => [7], 6 => [1]],
        'ยมขันธ์' => [7 => [12], 1 => [11], 2 => [7], 3 => [13], 4 => [6], 5 => [8], 6 => [9]],
        'อัตนิโรจน์' => [7 => [4], 1 => [6], 2 => [1], 3 => [3], 4 => [3], 5 => [9], 6 => [1]],
        'ทินกาล' => [7 => [1], 1 => [2], 2 => [10], 3 => [7], 4 => [1], 5 => [6], 6 => [6]],
        'ทินสูญ' => [7 => [12], 1 => [10], 2 => [15], 3 => [8], 4 => [5], 5 => [7], 6 => [8]],
        'กาฬโชค' => [7 => [4], 1 => [6], 2 => [1], 3 => [3], 4 => [8], 5 => [9], 6 => [10]],
        'กาลสูญ' => [7 => [4], 1 => [2], 2 => [7], 3 => [5], 4 => [8], 5 => [3], 6 => [6]],
        'กาลทัณฑ์' => [7 => [12], 1 => [11], 2 => [10], 3 => [9], 4 => [8], 5 => [7], 6 => [1]],
        'โลกวินาส' => [7 => [4], 1 => [6], 2 => [10], 3 => [9], 4 => [8], 5 => [9], 6 => [1]],
        'วินาสส์' => [7 => [4], 1 => [8], 2 => [6], 3 => [4], 4 => [8], 5 => [8], 6 => [9]],
        'พิลา' => [7 => [6], 1 => [10], 2 => [8], 3 => [7], 4 => [2], 5 => [9], 6 => [12]],
        'มฤตยู' => [7 => [9], 1 => [1], 2 => [10], 3 => [9], 4 => [8], 5 => [7], 6 => [6]],
        'วันบอด' => [7 => [7], 1 => [8], 2 => [4], 3 => [7], 4 => [1], 5 => [14], 6 => [11]],
        'กาลทีน' => [7 => [5], 1 => [6], 2 => [10], 3 => [8], 4 => [11], 5 => [5], 6 => [7]],
    ];

    private const MAHAMODO_DITHI_MONGKOL_5 = [
        2 => [3 => 'ไชยดิถี', 8 => 'ไชยดิถี', 13 => 'ไชยดิถี'],
        3 => [2 => 'ภัทรดีถี', 7 => 'ภัทรดีถี', 12 => 'ภัทรดีถี'],
        4 => [5 => 'ปุณณดีถี', 10 => 'ปุณณดีถี', 15 => 'ปุณณดีถี'],
        5 => [1 => 'นันทดีถี', 6 => 'นันทดีถี', 11 => 'นันทดีถี'],
        6 => [4 => 'มิตตะดีถี', 9 => 'มิตตะดีถี', 14 => 'มิตตะดีถี'],
    ];

    private const TAG_MEANINGS = [
        'วันธงชัย' => 'วันเด่นด้านการเริ่มต้นและเดินหน้ากิจการ มักใช้เป็นวันเปิดงานหรือทำเรื่องสำคัญ',
        'วันอธิบดี' => 'วันแห่งอำนาจการจัดการ เหมาะกับงานที่ต้องการการตัดสินใจและการบริหาร',
        'วันอุบาทว์/อุบาสน' => 'วันแรง ควรระวังการเริ่มเรื่องสำคัญเพราะอาจมีอุปสรรคหรือปัญหาแทรก',
        'วันโลกาวินาศ' => 'วันอ่อนกำลังด้านโชคโดยรวม เหมาะกับการชะลอเรื่องใหญ่และเพิ่มความรอบคอบ',
        'อำฤตโชค' => 'ฤกษ์โชคดีเด่น มักใช้ในงานที่ต้องการผลลัพธ์ราบรื่นและความสำเร็จ',
        'มหาสิทธิโชค' => 'ฤกษ์ดีเข้มแข็ง เน้นความสำเร็จที่ชัดเจนและแรงส่งของงาน',
        'สิทธิโชค' => 'ฤกษ์แห่งผลสัมฤทธิ์ เหมาะกับงานที่ต้องการความก้าวหน้าและได้ผลจริง',
        'ราชาโชค' => 'ฤกษ์เกื้อหนุนชื่อเสียงและสถานะ เหมาะกับงานที่ต้องการภาพลักษณ์ที่ดี',
        'ชัยโชค' => 'ฤกษ์แห่งชัยชนะ เหมาะกับงานแข่งขัน การเจรจา และการตัดสินใจสำคัญ',
        'ดิถีเรียงหมอน' => 'ดิถีที่นิยมใช้กับงานครอบครัว ความสัมพันธ์ และการเริ่มต้นชีวิตคู่',
        'ดิถีพิฆาต' => 'ดิถีที่มีแรงปะทะ ควรระวังงานที่เสี่ยงความขัดแย้งหรือเสียหาย',
        'พิฆาต' => 'ฤกษ์แรงด้านการปะทะ ไม่เหมาะกับการเริ่มเรื่องสำคัญที่ต้องการความราบรื่น',
        'กาลกรรณี' => 'กาลที่มักให้ผลติดขัด ควรหลีกเลี่ยงการเริ่มงานใหญ่หรือเรื่องเสี่ยง',
        'กาลสูร' => 'กาลไม่เกื้อหนุน มีแนวโน้มเกิดแรงเสียดทานและอุปสรรค',
        'กาลโชค' => 'กาลที่มีความผันผวนสูง ควรตรวจความพร้อมก่อนตัดสินใจ',
        'กาลทิน' => 'กาลที่ผลลัพธ์อาจไม่ตรงคาด ควรเน้นงานที่ปรับแก้ได้',
        'กาลทัณฑ์' => 'กาลเคร่งแรง ควรหลีกเลี่ยงงานที่เสี่ยงความเสียหายสูง',
        'ทินสูรย์' => 'ทินที่ต้องใช้ความระวังมากขึ้น โดยเฉพาะงานที่ต้องการความนิ่ง',
        'ทินศูร' => 'ทินที่แรงทางเหตุการณ์ ควรหลีกเลี่ยงการตัดสินใจเร่งด่วน',
        'ทินกาล' => 'ทินที่ไม่คงที่ เหมาะกับการทบทวนมากกว่าการเริ่มต้นใหญ่',
        'ทัคธทิน' => 'ทินที่โบราณมองว่าให้ผลร้อนแรง ควรหลีกเลี่ยงงานเสี่ยง',
        'ยมขันธ์' => 'ทินที่เน้นความระมัดระวัง ไม่เหมาะกับงานที่เดิมพันสูง',
        'ทักทิน' => 'ทินที่มีแรงขัด ควรหลีกเลี่ยงการเปิดเรื่องสำคัญใหม่',
        'มฤตยู' => 'พลังพลิกผันสูง เหมาะกับการระวังและมีแผนสำรอง',
        'บอด' => 'วันอับแสงของงานสำคัญ ควรชะลอหรือเตรียมเงื่อนไขให้พร้อม',
        'วินาศ' => 'แรงเสียทรงของวัน ควรหลีกเลี่ยงการเสี่ยงหรือขยายงานใหญ่',
        'โลกาวินาศ' => 'พลังวันในเชิงลบต่อความสำเร็จโดยรวม ควรเน้นความปลอดภัย',
        'ทรทึก' => 'ดิถีที่เกิดแรงต้านสูง มักใช้เตือนให้เลี่ยงงานสำคัญ',
        'กระทิงวัน' => 'วันที่พลังงานค่อนข้างแข็ง ควรใช้ความระมัดระวังในการเริ่มเรื่องใหญ่',
        'วันลอย' => 'วันเบาและคล่องตัว มักนิยมใช้กับงานที่ต้องการการไหลลื่น',
        'วันฟู' => 'วันหนุนการเติบโต เหมาะกับการเริ่มต้นงานที่ต้องการขยายผล',
        'วันจม' => 'วันชะลอหรือถอยแรง ควรเลี่ยงงานที่คาดหวังความคืบหน้าเร็ว',
        'อมุตโชค' => 'ฤกษ์มงคลสายมหาหมอดู ให้ผลดีเด่น เหมาะกับงานที่ต้องการโชคและความสำเร็จ',
        'ไชยดิถี' => 'ดิถีมงคลสายมหาหมอดู ใช้ประกอบการเลือกวันเริ่มต้นงานที่ต้องการชัยชนะ',
        'ภัทรดีถี' => 'ดิถีมงคลที่เน้นความราบรื่นและผลดี ใช้ประกอบการเลือกวันทำเรื่องสำคัญ',
        'ปุณณดีถี' => 'ดิถีมงคลที่นิยมใช้กับงานที่ต้องการความอุดมพร้อมและผลครบถ้วน',
        'นันทดีถี' => 'ดิถีมงคลที่ให้บรรยากาศชื่นบาน เหมาะกับงานมงคลและการเริ่มต้น',
        'มิตตะดีถี' => 'ดิถีมงคลที่เน้นความสัมพันธ์และการเกื้อหนุน เหมาะกับงานที่ต้องการผู้สนับสนุน',
        'ทึกทึน' => 'ดิถีเตือนสายมหาหมอดูว่ามีแรงต้านและความอึดอัด ควรเลี่ยงงานสำคัญ',
        'อัตนิโรจน์' => 'ดิถีเตือนให้ระวังการเร่งตัดสินใจหรือเปิดงานใหม่ในวันที่แรงไม่เกื้อหนุน',
        'ทินสูญ' => 'ดิถีที่ถือว่าเสียกำลังและผลลัพธ์อาจพร่อง ควรหลีกเลี่ยงงานใหญ่',
        'กาฬโชค' => 'ดิถีที่มีแรงหม่นและเสี่ยงเรื่องติดขัด ใช้เป็นคำเตือนก่อนเริ่มกิจการสำคัญ',
        'กาลสูญ' => 'ดิถีที่พลังตกและผลตอบแทนอาจไม่คุ้ม เหมาะกับการชะลอเรื่องใหญ่',
        'โลกวินาส' => 'ดิถีเชิงลบสายมหาหมอดู เตือนให้ระวังความเสียหายและการเสียทรงของงาน',
        'ปลอด' => 'วันที่ไม่ปรากฏฤกษ์มงคลหรือฤกษ์ลบที่เด่นชัด จัดเป็นวันปลอดภัย เหมาะกับกิจกรรมทั่วไป',
        'วินาสส์' => 'ดิถีแรงทางความเสียหาย ควรหลีกเลี่ยงการเริ่มต้นสิ่งที่มีความเสี่ยงสูง',
        'วันบอด' => 'ดิถีที่พลังอับแสงตามตำรามหาหมอดู ควรลดการคาดหวังผลลัพธ์ใหญ่',
        'กาลทีน' => 'ดิถีเตือนว่าจังหวะวันยังไม่เกื้อหนุน ควรใช้กับงานที่ยืดหยุ่นหรือเลื่อนได้',
    ];

    private static $tagMetaCache = null;
    private static $tagMetaDbChecked = false;
    private static $tagMetaCacheAt = 0;
    private static $myHoraVerifiedOverrides = null;
    private static $songkranCutoverConfig = null;

    private static $jan1LunarStateCache = [
        self::LUNAR_EPOCH_GREGORIAN_YEAR => [
            'month' => self::LUNAR_EPOCH_MONTH,
            'dithi' => self::LUNAR_EPOCH_DITHI,
            'lunarYear' => self::LUNAR_EPOCH_GREGORIAN_YEAR,
        ],
    ];

    private static $lunarMonthLengthsCache = [];

    public static function getSupportedDateRange($centerDate = 'now', $yearsBefore = self::DEFAULT_RANGE_YEARS_BEFORE, $yearsAfter = self::DEFAULT_RANGE_YEARS_AFTER)
    {
        $center = self::toImmutableDate($centerDate);
        $year = (int) $center->format('Y');

        return [
            'start' => sprintf('%04d-01-01', $year - max(0, (int) $yearsBefore)),
            'end' => sprintf('%04d-12-31', $year + max(0, (int) $yearsAfter)),
        ];
    }

    public static function getSupportedDisplayYears($centerDate = 'now', $yearsBefore = self::DEFAULT_RANGE_YEARS_BEFORE, $yearsAfter = self::DEFAULT_RANGE_YEARS_AFTER)
    {
        $range = self::getSupportedDateRange($centerDate, $yearsBefore, $yearsAfter);
        $startYear = (int) substr($range['start'], 0, 4);
        $endYear = (int) substr($range['end'], 0, 4);

        return range($startYear, $endYear);
    }

    public static function getMonthRange($year, $month)
    {
        $normalizedYear = (int) $year;
        $normalizedMonth = max(1, min(12, (int) $month));
        $firstDay = new \DateTimeImmutable(sprintf('%04d-%02d-01', $normalizedYear, $normalizedMonth));

        return [
            'start' => $firstDay->format('Y-m-d'),
            'end' => $firstDay->modify('last day of this month')->format('Y-m-d'),
        ];
    }

    private static function toImmutableDate($dateInput)
    {
        if ($dateInput instanceof \DateTimeImmutable) {
            return $dateInput;
        }
        if ($dateInput instanceof \DateTimeInterface) {
            return \DateTimeImmutable::createFromInterface($dateInput);
        }
        return new \DateTimeImmutable((string) $dateInput);
    }

    private static function getGregorianYearDayCount($year)
    {
        $year = (int) $year;
        return (($year % 400 === 0) || ($year % 4 === 0 && $year % 100 !== 0)) ? 366 : 365;
    }

    private static function getBuddhistEraYear($gregorianYear)
    {
        return (int) $gregorianYear + 544;
    }

    private static function getAharkun($gregorianYear)
    {
        $beYear = self::getBuddhistEraYear($gregorianYear);
        return intdiv(($beYear * 292207) + 499, 800) + 4;
    }

    private static function getAvoman($gregorianYear)
    {
        return ((11 * self::getAharkun($gregorianYear)) + 25) % 692;
    }

    private static function getBodithey($gregorianYear)
    {
        $aharkun = self::getAharkun($gregorianYear);
        return (intdiv((11 * $aharkun) + 25, 692) + $aharkun + 29) % 30;
    }

    private static function isSolarLeapYearInLunisolarSystem($gregorianYear)
    {
        $beYear = self::getBuddhistEraYear($gregorianYear);
        return 800 - ((($beYear * 292207) + 499) % 800) <= 207;
    }

    /**
     * 0 = none, 1 = leap month, 2 = leap day, 3 = both (resolved by protetin rule)
     */
    private static function getBoditheyLeapType($gregorianYear)
    {
        $avoman = self::getAvoman($gregorianYear);
        $bodithey = self::getBodithey($gregorianYear);

        $isLeapMonth = ($bodithey >= 25 || $bodithey <= 5);
        if ($bodithey === 25 && self::getBodithey($gregorianYear + 1) === 5) {
            $isLeapMonth = false;
        }
        if ($bodithey === 24 && self::getBodithey($gregorianYear + 1) === 6) {
            $isLeapMonth = true;
        }

        if (self::isSolarLeapYearInLunisolarSystem($gregorianYear)) {
            $isLeapDay = ($avoman <= 126);
        } else {
            $isLeapDay = ($avoman <= 137) && (self::getAvoman($gregorianYear + 1) !== 0);
        }

        if ($isLeapMonth && $isLeapDay) {
            return 3;
        }
        if ($isLeapMonth) {
            return 1;
        }
        if ($isLeapDay) {
            return 2;
        }
        return 0;
    }

    /**
     * 0 = common year (354)
     * 1 = leap month year (384)
     * 2 = leap day year (355)
     */
    private static function getLunarYearType($gregorianYear)
    {
        $boditheyLeapType = self::getBoditheyLeapType($gregorianYear);

        if ($boditheyLeapType === 3) {
            return 1;
        }
        if ($boditheyLeapType === 1 || $boditheyLeapType === 2) {
            return $boditheyLeapType;
        }
        if (self::getBoditheyLeapType($gregorianYear - 1) === 3) {
            return 2;
        }

        return 0;
    }

    private static function getLunarMonthLengths($lunarYear)
    {
        $lunarYear = (int) $lunarYear;
        if (isset(self::$lunarMonthLengthsCache[$lunarYear])) {
            return self::$lunarMonthLengthsCache[$lunarYear];
        }

        $lengths = [
            1 => 29,
            2 => 30,
            3 => 29,
            4 => 30,
            5 => 29,
            6 => 30,
            7 => 29,
            8 => 30,
            9 => 29,
            10 => 30,
            11 => 29,
            12 => 30,
        ];

        $yearType = self::getLunarYearType($lunarYear);
        if ($yearType === 1) {
            $lengths = [
                1 => 29,
                2 => 30,
                3 => 29,
                4 => 30,
                5 => 29,
                6 => 30,
                7 => 29,
                8 => 30,
                18 => 30,
                9 => 29,
                10 => 30,
                11 => 29,
                12 => 30,
            ];
        } elseif ($yearType === 2) {
            $lengths[7] = 30;
        }

        self::$lunarMonthLengthsCache[$lunarYear] = $lengths;
        return $lengths;
    }

    private static function getNormalizedThaiMonth($thaiMonth)
    {
        $thaiMonth = (int) $thaiMonth;
        if ($thaiMonth === 18 || $thaiMonth === 13) {
            return 8;
        }
        if ($thaiMonth > 12) {
            return $thaiMonth - 12;
        }
        return $thaiMonth;
    }

    private static function getLunarMonthLength($thaiMonth, $lunarYear)
    {
        $lengths = self::getLunarMonthLengths($lunarYear);
        if (!isset($lengths[$thaiMonth])) {
            throw new \RuntimeException("Unknown Thai lunar month {$thaiMonth} in lunar year {$lunarYear}");
        }
        return $lengths[$thaiMonth];
    }

    private static function moveToNextLunarMonth(array $state)
    {
        $sequence = array_keys(self::getLunarMonthLengths($state['lunarYear']));
        $index = array_search($state['month'], $sequence, true);

        if ($index === false) {
            throw new \RuntimeException("Unable to advance lunar month {$state['month']} in lunar year {$state['lunarYear']}");
        }

        if ($index + 1 < count($sequence)) {
            $state['month'] = $sequence[$index + 1];
            return $state;
        }

        $state['month'] = 1;
        $state['lunarYear']++;
        return $state;
    }

    private static function moveToPreviousLunarMonth(array $state)
    {
        $sequence = array_keys(self::getLunarMonthLengths($state['lunarYear']));
        $index = array_search($state['month'], $sequence, true);

        if ($index === false) {
            throw new \RuntimeException("Unable to rewind lunar month {$state['month']} in lunar year {$state['lunarYear']}");
        }

        if ($index > 0) {
            $state['month'] = $sequence[$index - 1];
            return $state;
        }

        $state['lunarYear']--;
        $prevSequence = array_keys(self::getLunarMonthLengths($state['lunarYear']));
        $state['month'] = $prevSequence[count($prevSequence) - 1];
        return $state;
    }

    private static function advanceLunarStateByDays(array $state, $deltaDays)
    {
        $deltaDays = (int) $deltaDays;

        while ($deltaDays > 0) {
            $currentMonthLength = self::getLunarMonthLength($state['month'], $state['lunarYear']);
            $remainingInMonth = $currentMonthLength - $state['dithi'];

            if ($deltaDays <= $remainingInMonth) {
                $state['dithi'] += $deltaDays;
                return $state;
            }

            $deltaDays -= ($remainingInMonth + 1);
            $state = self::moveToNextLunarMonth($state);
            $state['dithi'] = 1;
        }

        while ($deltaDays < 0) {
            $daysBeforeCurrent = $state['dithi'] - 1;

            if (abs($deltaDays) <= $daysBeforeCurrent) {
                $state['dithi'] += $deltaDays;
                return $state;
            }

            $deltaDays += ($daysBeforeCurrent + 1);
            $state = self::moveToPreviousLunarMonth($state);
            $state['dithi'] = self::getLunarMonthLength($state['month'], $state['lunarYear']);
        }

        return $state;
    }

    private static function getLunarStateAtJan1($gregorianYear)
    {
        $gregorianYear = (int) $gregorianYear;
        if (isset(self::$jan1LunarStateCache[$gregorianYear])) {
            return self::$jan1LunarStateCache[$gregorianYear];
        }

        if ($gregorianYear > self::LUNAR_EPOCH_GREGORIAN_YEAR) {
            $previousState = self::getLunarStateAtJan1($gregorianYear - 1);
            $state = self::advanceLunarStateByDays($previousState, self::getGregorianYearDayCount($gregorianYear - 1));
        } else {
            $nextState = self::getLunarStateAtJan1($gregorianYear + 1);
            $state = self::advanceLunarStateByDays($nextState, -self::getGregorianYearDayCount($gregorianYear));
        }

        self::$jan1LunarStateCache[$gregorianYear] = $state;
        return $state;
    }

    /**
     * Returns dithi (1-30), day (1-15), and Thai month (1-12/18).
     */
    public static function getThaiLunarDate($dateStr)
    {
        $date = self::toImmutableDate($dateStr);
        $year = (int) $date->format('Y');
        $jan1 = new \DateTimeImmutable(sprintf('%04d-01-01', $year));
        $deltaDays = (int) $jan1->diff($date)->format('%r%a');
        $state = self::advanceLunarStateByDays(self::getLunarStateAtJan1($year), $deltaDays);
        $dithi = (int) $state['dithi'];

        return [
            'supported' => true,
            'dithi' => $dithi,
            'day' => ($dithi > 15 ? $dithi - 15 : $dithi),
            'mThai' => (int) $state['month'],
            'lunarYear' => (int) $state['lunarYear'],
        ];
    }

    /**
     * Determines if a date is a Buddhist Holy Day.
     */
    public static function isWanPra($date)
    {
        $lunar = self::getThaiLunarDate($date);
        $day = $lunar['day'];
        $dithi = $lunar['dithi'];

        if ($day === 8 || $day === 15) {
            return true;
        }

        // Waning 14 in a 29-day lunar month.
        return $dithi === 29;
    }

    private static function getChulaSakaratYear(\DateTimeImmutable $date)
    {
        $gregorianYear = (int) $date->format('Y');
        $cutover = self::getSongkranCutoverDate($gregorianYear);
        return ($date >= $cutover) ? ($gregorianYear - 638) : ($gregorianYear - 639);
    }

    private static function getSongkranCutoverConfig()
    {
        if (is_array(self::$songkranCutoverConfig)) {
            return self::$songkranCutoverConfig;
        }

        $path = dirname(__DIR__, 3) . '/shared/rengyam/songkran_cutover_dates.json';
        if (!is_file($path)) {
            self::$songkranCutoverConfig = [
                'default' => self::DEFAULT_SONGKRAN_CUTOVER_MONTH_DAY,
                'years' => [],
            ];
            return self::$songkranCutoverConfig;
        }

        $decoded = json_decode((string) file_get_contents($path), true);
        $default = is_array($decoded) && !empty($decoded['default'])
            ? (string) $decoded['default']
            : self::DEFAULT_SONGKRAN_CUTOVER_MONTH_DAY;
        $years = is_array($decoded['years'] ?? null) ? $decoded['years'] : [];

        self::$songkranCutoverConfig = [
            'default' => $default,
            'years' => $years,
        ];

        return self::$songkranCutoverConfig;
    }

    private static function getSongkranCutoverDate($gregorianYear)
    {
        $config = self::getSongkranCutoverConfig();
        $dateStr = $config['years'][(string) $gregorianYear]
            ?? sprintf('%04d-%s', (int) $gregorianYear, $config['default']);
        return new \DateTimeImmutable($dateStr);
    }

    private static function getMondayBasedWeekdayNumber(\DateTimeImmutable $date)
    {
        $weekday = (int) $date->format('w'); // 0 = Sunday
        return $weekday === 0 ? 7 : $weekday; // 1 = Monday ... 7 = Sunday
    }

    private static function getSundayBasedWeekdayNumber(\DateTimeImmutable $date)
    {
        $mondayBased = self::getMondayBasedWeekdayNumber($date);
        return ($mondayBased % 7) + 1; // 1 = Sunday ... 7 = Saturday
    }

    /**
     * Dynamic Kala-Yoga mapping derived from Chula Sakarat year remainder.
     */
    public static function getKalayok($dateStr)
    {
        $date = self::toImmutableDate($dateStr);
        $weekday = self::getMondayBasedWeekdayNumber($date); // Kala-yoga tables use 1=Monday...7=Sunday
        $csYear = self::getChulaSakaratYear($date);
        $remainder = $csYear % 7;
        if ($remainder === 0) {
            $remainder = 7;
        }

        return [
            'is_tongchai' => $weekday === self::KALAYOK_TONGCHAI_BY_CS_REMAINDER[$remainder],
            'is_atipbadee' => $weekday === self::KALAYOK_ATIPBADEE_BY_CS_REMAINDER[$remainder],
            'is_ubath' => $weekday === self::KALAYOK_UBATH_BY_CS_REMAINDER[$remainder],
            'is_lokawinat' => $weekday === self::KALAYOK_LOKAWINAT_BY_CS_REMAINDER[$remainder],
            'is_kating' => false,
        ];
    }

    public static function isRiangMon($dithi)
    {
        $targets = [7, 10, 13, 19, 23, 25, 29];
        return in_array((int) $dithi, $targets, true);
    }

    private static function getDayOfFortnight($dithi)
    {
        $day = (int) $dithi;
        return $day > 15 ? $day - 15 : $day;
    }

    private static function matchesMyHoraDayRule(array $rules, $weekday, $day)
    {
        $targets = $rules[(int) $weekday] ?? [];
        foreach ((array) $targets as $target) {
            if ((int) $target === (int) $day) {
                return true;
            }
        }

        return false;
    }

    private static function getMyHoraSolarZodiacName(\DateTimeImmutable $date)
    {
        $month = (int) $date->format('n');
        $day = (int) $date->format('j');

        if (($month === 4 && $day >= 13) || ($month === 5 && $day <= 13)) {
            return 'เมษ';
        }
        if (($month === 5 && $day >= 14) || ($month === 6 && $day <= 13)) {
            return 'พฤษภ';
        }
        if (($month === 6 && $day >= 14) || ($month === 7 && $day <= 14)) {
            return 'มิถุน';
        }
        if (($month === 7 && $day >= 15) || ($month === 8 && $day <= 16)) {
            return 'กรกฎ';
        }
        if (($month === 8 && $day >= 17) || ($month === 9 && $day <= 16)) {
            return 'สิงห์';
        }
        if (($month === 9 && $day >= 17) || ($month === 10 && $day <= 16)) {
            return 'กันย์';
        }
        if (($month === 10 && $day >= 17) || ($month === 11 && $day <= 15)) {
            return 'ตุล';
        }
        if (($month === 11 && $day >= 16) || ($month === 12 && $day <= 15)) {
            return 'พิจิก';
        }
        if (($month === 12 && $day >= 16) || ($month === 1 && $day <= 15)) {
            return 'ธนู';
        }
        if (($month === 1 && $day >= 16) || ($month === 2 && $day <= 12)) {
            return 'มกร';
        }
        if (($month === 2 && $day >= 13) || ($month === 3 && $day <= 13)) {
            return 'กุมภ์';
        }

        return 'มีน';
    }

    private static function getMyHoraMahasunTags(\DateTimeImmutable $date, $mThai, $dithi)
    {
        $tags = [];
        $day = self::getDayOfFortnight($dithi);
        $normalizedMonth = self::getNormalizedThaiMonth($mThai);
        $solarZodiac = self::getMyHoraSolarZodiacName($date);

        if ((self::MYHORA_SOLAR_MAHASUN_DAY_BY_ZODIAC[$solarZodiac] ?? null) === $day) {
            $tags[] = 'มหาสูญ [ก]';
        }

        if ((self::MYHORA_LUNAR_MAHASUN_DAY_BY_MONTH[$normalizedMonth] ?? null) === $day) {
            $tags[] = 'มหาสูญ [ข]';
        }

        return $tags;
    }

    public static function queryMahasun($mThai, $dithiRaw, $dateStr = null)
    {
        $day = self::getDayOfFortnight($dithiRaw);
        $normalizedMonth = self::getNormalizedThaiMonth($mThai);

        if ((self::MYHORA_LUNAR_MAHASUN_DAY_BY_MONTH[$normalizedMonth] ?? null) === $day) {
            return true;
        }

        if ($dateStr !== null) {
            $date = self::toImmutableDate($dateStr);
            $solarZodiac = self::getMyHoraSolarZodiacName($date);
            return (self::MYHORA_SOLAR_MAHASUN_DAY_BY_ZODIAC[$solarZodiac] ?? null) === $day;
        }

        return false;
    }

    private static function getMyHoraAyakarnTag($mThai, $dithi)
    {
        $day = self::getDayOfFortnight($dithi);
        $normalizedMonth = self::getNormalizedThaiMonth($mThai);
        $variant = self::MYHORA_AYAKARN_RULES[$normalizedMonth][$day] ?? null;

        return $variant !== null ? "อายกรรมพลาย{$variant}" : null;
    }

    private static function getMyHoraTrathuekTag($mThai, $dithi)
    {
        $day = self::getDayOfFortnight($dithi);
        $normalizedMonth = self::getNormalizedThaiMonth($mThai);

        return ((self::MYHORA_TRATHUEK_DAY_BY_MONTH[$normalizedMonth] ?? null) === $day) ? 'ทรทึก' : null;
    }

    private static function getMyHoraAgniTag($dithi)
    {
        $day = self::getDayOfFortnight($dithi);

        $targets = [
            1 => 'อัคนิโรธ (-สัตว์)',
            2 => 'อัคนิโรธ (-ป่า)',
            3 => 'อัคนิโรธ (-น้ำ)',
            4 => 'อัคนิโรธ (-ภูเขา)',
            5 => 'อัคนิโรธ (-ที่ดิน)',
            6 => 'อัคนิโรธ (-บ้าน)',
            7 => 'อัคนิโรธ (-วัง)',
            8 => 'อัคนิโรธ (-รถ)',
            9 => 'อัคนิโรธ (-ดิน)',
            10 => 'อัคนิโรธ (-เรือ)',
            11 => 'อัคนิโรธ (-พืช)',
            12 => 'อัคนิโรธ (-สตรี)',
            13 => 'อัคนิโรธ (-บุรุษ)',
            14 => 'อัคนิโรธ (-พัทธสีมา)',
            15 => 'อัคนิโรธ (-เทพ)',
        ];

        return $targets[$day] ?? null;
    }

    private static function getMyHoraLoyFuJomTags($dithiRaw, $mThai)
    {
        $month = self::getNormalizedThaiMonth($mThai);
        $rule = self::MYHORA_LOY_FU_JOM_RESIDUES[$month] ?? self::MYHORA_LOY_FU_JOM_RESIDUES[4];
        $residue = ((int) $dithiRaw % 7 + 7) % 7;
        $tags = [];

        if ($residue === (int) $rule['loy']) {
            $tags[] = 'วันลอย';
        }
        if ($residue === (int) $rule['fu']) {
            $tags[] = 'วันฟู';
        }
        if ($residue === (int) $rule['jom']) {
            $tags[] = 'วันจม';
        }

        return $tags;
    }

    private static function getMyHoraVerifiedTagOverrides()
    {
        if (is_array(self::$myHoraVerifiedOverrides)) {
            return self::$myHoraVerifiedOverrides;
        }

        $path = dirname(__DIR__, 3) . '/shared/rengyam/myhora_verified_tag_overrides.json';
        if (!is_file($path)) {
            self::$myHoraVerifiedOverrides = [];
            return self::$myHoraVerifiedOverrides;
        }

        $decoded = json_decode((string) file_get_contents($path), true);
        self::$myHoraVerifiedOverrides = is_array($decoded) ? $decoded : [];

        return self::$myHoraVerifiedOverrides;
    }

    private static function getMahamodoAgniTag($dithi)
    {
        $day = self::getDayOfFortnight($dithi);

        $targets = [
            1 => 'อัคนิโรธ (-สัตว์)',
            2 => 'อัคนิโรธ (-ป่า)',
            3 => 'อัคนิโรธ (-น้ำ)',
            4 => 'อัคนิโรธ (-ภูเขา)',
            5 => 'อัคนิโรธ (-ที่ดิน)',
            6 => 'อัคนิโรธ (-บ้าน)',
            7 => 'อัคนิโรธ (-วัง)',
            8 => 'อัคนิโรธ (-รถ)',
            9 => 'อัคนิโรธ (-ดิน)',
            10 => 'อัคนิโรธ (-เรือ)',
            11 => 'อัคนิโรธ (-พืช)',
            12 => 'อัคนิโรธ (-สตรี)',
            13 => 'อัคนิโรธ (-บุรุษ)',
            14 => 'อัคนิโรธ (-บวช)',
            15 => 'อัคนิโรธ (-บวงสรวง)',
        ];

        return $targets[$day] ?? null;
    }

    private static function getMahamodoDithiMongkol5Tags($weekday, $dithi)
    {
        $day = self::getDayOfFortnight($dithi);
        return isset(self::MAHAMODO_DITHI_MONGKOL_5[$weekday][$day])
            ? [self::MAHAMODO_DITHI_MONGKOL_5[$weekday][$day]]
            : [];
    }

    private static function getMahamodoBadTags($weekday, $dithi)
    {
        $day = self::getDayOfFortnight($dithi);
        $tags = [];
        foreach (self::MAHAMODO_BAD_RULES as $tag => $rules) {
            if (in_array($day, $rules[$weekday] ?? [], true)) {
                $tags[] = $tag;
            }
        }
        return $tags;
    }

    private static function buildGenericMahamodoDisplayTags($dateStr, array $status)
    {
        $date = self::toImmutableDate($dateStr);
        $weekday = self::getMondayBasedWeekdayNumber($date);
        $lunar = self::getThaiLunarDate($dateStr);
        $dithi = (int) ($lunar['dithi'] ?? 0);
        $mThai = (int) ($lunar['mThai'] ?? 0);

        $tags = [];

        if (!empty($status['is_tongchai'])) {
            $tags[] = 'วันธงชัย';
        }
        if (!empty($status['is_atipbadee'])) {
            $tags[] = 'วันอธิบดี';
        }
        if (!empty($status['is_ubath'])) {
            $tags[] = 'วันอุบาทว์/อุบาสน';
        }
        if (!empty($status['is_lokawinat'])) {
            $tags[] = 'วันโลกาวินาศ';
        }

        foreach (['is_ammarit' => 'อมุตโชค', 'is_mahasittichok' => 'มหาสิทธิโชค', 'is_sittichok' => 'สิทธิโชค', 'is_rachachok' => 'ราชาโชค', 'is_chaichok' => 'ชัยโชค'] as $flag => $tag) {
            if (!empty($status[$flag])) {
                $tags[] = $tag;
            }
        }

        if (!empty($status['is_riangmon'])) {
            $tags[] = 'ดิถีเรียงหมอน';
        }
        if (!empty($status['is_kating'])) {
            $tags[] = 'กระทิงวัน';
        }

        $tags = array_merge(
            $tags,
            self::getMahamodoDithiMongkol5Tags($weekday, $dithi),
            self::getMahamodoBadTags($weekday, $dithi)
        );

        if (self::queryMahasun($mThai, $dithi, $dateStr)) {
            $tags[] = 'มหาสูญ';
        }

        $agniTag = self::getMahamodoAgniTag($dithi);
        if ($agniTag !== null) {
            $tags[] = $agniTag;
        }

        return array_values(array_unique($tags));
    }

    private static function getMyHoraGenericPrimaryBadTags($weekday, $dithiRaw)
    {
        $day = self::getDayOfFortnight($dithiRaw);
        $weekday = (int) $weekday;
        $matches = [];

        foreach (self::MYHORA_PRIMARY_BAD_RULES as $label => $rule) {
            if (self::matchesMyHoraDayRule($rule, $weekday, $day)) {
                $matches[] = $label;
            }
        }

        if (in_array('พิฆาต', $matches, true)) {
            return ['พิฆาต', 'ดิถีพิฆาต'];
        }

        foreach (self::MYHORA_PRIMARY_BAD_PRIORITY as $label) {
            if (in_array($label, $matches, true)) {
                return [$label];
            }
        }

        return [];
    }

    private static function buildGenericMyHoraDisplayTags($dateStr, array $status)
    {
        $date = self::toImmutableDate($dateStr);
        $weekday = self::getMondayBasedWeekdayNumber($date);
        $lunar = self::getThaiLunarDate($dateStr);
        $dithi = (int) $lunar['dithi'];
        $mThai = (int) $lunar['mThai'];

        $tags = [];

        if (!empty($status['is_mahasittichok'])) {
            $tags[] = 'มหาสิทธิโชค';
        }
        if (!empty($status['is_sittichok'])) {
            $tags[] = 'สิทธิโชค';
        }
        if (!empty($status['is_ammarit'])) {
            $tags[] = 'อำฤตโชค';
        }
        if (!empty($status['is_rachachok'])) {
            $tags[] = 'ราชาโชค';
        }
        if (!empty($status['is_chaichok'])) {
            $tags[] = 'ชัยโชค';
        }

        $tags = array_merge($tags, self::getMyHoraGenericPrimaryBadTags($weekday, $dithi));

        $tags = array_merge($tags, self::getMyHoraMahasunTags($date, $mThai, $dithi));

        $ayakarnTag = self::getMyHoraAyakarnTag($mThai, $dithi);
        if ($ayakarnTag !== null) {
            $tags[] = $ayakarnTag;
        }

        if (!empty($status['is_kating'])) {
            $tags[] = 'กระทิงวัน';
        }

        $trathuekTag = self::getMyHoraTrathuekTag($mThai, $dithi);
        if ($trathuekTag !== null) {
            $tags[] = $trathuekTag;
        }

        if (!empty($status['is_tongchai'])) {
            $tags[] = 'วันธงชัย';
        }
        if (!empty($status['is_atipbadee'])) {
            $tags[] = 'วันอธิบดี';
        }
        if (!empty($status['is_ubath'])) {
            $tags[] = 'วันอุบาทว์/อุบาสน';
        }
        if (!empty($status['is_lokawinat'])) {
            $tags[] = 'วันโลกาวินาศ';
        }

        $tags = array_merge($tags, self::getMyHoraLoyFuJomTags($dithi, $mThai));

        if (!empty($status['is_riangmon'])) {
            $tags[] = 'ดิถีเรียงหมอน';
        }

        $agniTag = self::getMyHoraAgniTag($dithi);
        if ($agniTag !== null) {
            $tags[] = $agniTag;
        }

        return array_values(array_unique($tags));
    }

    private static function isGoodDithiTag($tag)
    {
        return in_array($tag, [
            'มหาสิทธิโชค', 'สิทธิโชค', 'อำฤตโชค', 'อมุตโชค', 'ราชาโชค', 'ชัยโชค',
            'ไชยดิถี', 'ภัทรดีถี', 'ปุณณดีถี', 'นันทดีถี', 'มิตตะดีถี',
        ], true);
    }

    private static function isPrimaryDithiTag($tag)
    {
        if (self::isGoodDithiTag($tag)) {
            return true;
        }

        if (in_array($tag, [
            'พิฆาต', 'ดิถีพิฆาต', 'ดิถีเรียงหมอน', 'กระทิงวัน', 'ทรทึก',
            'ทึกทึน', 'อัตนิโรจน์', 'ทินสูญ', 'กาฬโชค', 'กาลสูญ', 'โลกวินาส', 'วินาสส์', 'วันบอด', 'กาลทีน',
        ], true)) {
            return true;
        }

        if (strpos($tag, 'มหาสูญ') === 0 || strpos($tag, 'อายกรรมพลาย') === 0) {
            return true;
        }

        return in_array($tag, self::MYHORA_PRIMARY_BAD_PRIORITY, true);
    }

    private static function classifyCalendarTag($tag)
    {
        if (in_array($tag, ['วันธงชัย', 'วันอธิบดี', 'วันอุบาทว์/อุบาสน', 'วันโลกาวินาศ'], true)) {
            return 'kal';
        }
        if (in_array($tag, ['วันลอย', 'วันฟู', 'วันจม'], true)) {
            return 'day_type';
        }
        if (strpos($tag, 'อัคนิโรธ') === 0) {
            return 'warning';
        }
        if (self::isPrimaryDithiTag($tag)) {
            return 'dithi';
        }

        return 'other';
    }

    private static function isPositiveCalendarTag($tag)
    {
        if (in_array($tag, ['วันธงชัย', 'วันอธิบดี', 'วันลอย', 'วันฟู', 'ดิถีเรียงหมอน', 'กระทิงวัน'], true)) {
            return true;
        }

        if (self::isGoodDithiTag($tag)) {
            return true;
        }

        return false;
    }

    private static function isNegativeCalendarTag($tag)
    {
        if (in_array($tag, ['วันอุบาทว์/อุบาสน', 'วันโลกาวินาศ', 'วันจม'], true)) {
            return true;
        }

        if (strpos($tag, 'อัคนิโรธ') === 0) {
            return true;
        }

        if (self::isPrimaryDithiTag($tag) && !self::isGoodDithiTag($tag) && $tag !== 'ดิถีเรียงหมอน' && $tag !== 'กระทิงวัน') {
            return true;
        }

        return false;
    }

    private static function normalizeMyHoraDisplayTags(array $tags)
    {
        $seen = [];
        $normalized = [];
        $chosenDayType = null;

        foreach ($tags as $tag) {
            $tag = trim((string) $tag);
            if ($tag === '' || isset($seen[$tag])) {
                continue;
            }

            if (in_array($tag, ['วันลอย', 'วันฟู', 'วันจม'], true)) {
                if ($chosenDayType !== null) {
                    continue;
                }
                $chosenDayType = $tag;
            }

            $normalized[] = $tag;
            $seen[$tag] = true;
        }

        return $normalized;
    }

    private static function buildMyHoraTagBundle(array $displayTags)
    {
        $bundle = [
            'kal_tags' => [],
            'dithi_tags' => [],
            'day_type_tags' => [],
            'warning_tags' => [],
            'other_tags' => [],
        ];

        foreach ($displayTags as $tag) {
            switch (self::classifyCalendarTag($tag)) {
                case 'kal':
                    $bundle['kal_tags'][] = $tag;
                    break;
                case 'dithi':
                    $bundle['dithi_tags'][] = $tag;
                    break;
                case 'day_type':
                    $bundle['day_type_tags'][] = $tag;
                    break;
                case 'warning':
                    $bundle['warning_tags'][] = $tag;
                    break;
                default:
                    $bundle['other_tags'][] = $tag;
                    break;
            }
        }

        return $bundle;
    }

    private static function getTagSourceLabel($tag)
    {
        switch (self::classifyCalendarTag($tag)) {
            case 'kal':
                return 'กาลโยค';
            case 'dithi':
                return 'ดิถี/ฤกษ์';
            case 'day_type':
                return 'ชนิดวัน';
            case 'warning':
                return 'ข้อห้าม';
            default:
                return 'อื่นๆ';
        }
    }

    private static function explainTagFallback($tag)
    {
        $cleanTag = trim((string) $tag);
        if ($cleanTag === '') {
            return 'คำอธิบายไม่พร้อมใช้งาน';
        }

        if (isset(self::TAG_MEANINGS[$cleanTag])) {
            return self::TAG_MEANINGS[$cleanTag];
        }

        if (strpos($cleanTag, 'มหาสูญ') === 0) {
            return 'ดิถีที่โบราณจัดเป็นวันพลังลบ ควรหลีกเลี่ยงการเริ่มงานสำคัญ';
        }
        if (strpos($cleanTag, 'อายกรรมพลาย') === 0) {
            return 'ดิถีเตือนเรื่องผลกระทบและความเสียหาย ควรพิจารณาให้รอบคอบก่อนเริ่มงาน';
        }
        if (strpos($cleanTag, 'อัคนิโรธ') === 0) {
            $topic = trim(str_replace(['อัคนิโรธ', '(', ')', '-'], '', $cleanTag));
            if ($topic !== '') {
                return sprintf('ข้อห้ามเฉพาะกิจกรรมเกี่ยวกับ "%s" ในวันนี้ ควรหลีกเลี่ยงหรือลดความเสี่ยง', $topic);
            }
            return 'ข้อห้ามเฉพาะวันตามตำราโหร ควรหลีกเลี่ยงกิจกรรมที่มีความเสี่ยง';
        }

        return 'ฤกษ์นี้เป็นตัวชี้วัดเชิงตำรา ใช้ประกอบการตัดสินใจร่วมกับบริบทจริงของงาน';
    }

    private static function getTagMetaFromDb()
    {
        // Cache with TTL so admin edits propagate without having to restart PHP-FPM.
        // This also avoids repeated DB hits within a short window.
        $ttlSeconds = 60;
        if (self::$tagMetaDbChecked && is_int(self::$tagMetaCacheAt) && (time() - self::$tagMetaCacheAt) < $ttlSeconds) {
            return self::$tagMetaCache;
        }

        self::$tagMetaDbChecked = true;
        self::$tagMetaCache = null;
        self::$tagMetaCacheAt = time();

        try {
            $configPath = dirname(__DIR__, 2) . '/configs/config.php';
            if (!file_exists($configPath)) {
                return null;
            }

            $config = include $configPath;
            if (!is_array($config) || empty($config['db'])) {
                return null;
            }
            $db = $config['db'];
            $dsn = sprintf(
                'mysql:host=%s;dbname=%s;charset=utf8mb4',
                $db['host'] ?? 'localhost',
                $db['dbname'] ?? ''
            );
            $pdo = new \PDO($dsn, $db['user'] ?? '', $db['pass'] ?? '');
            $pdo->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
            $pdo->setAttribute(\PDO::ATTR_DEFAULT_FETCH_MODE, \PDO::FETCH_ASSOC);

            $stmt = $pdo->query("SHOW TABLES LIKE 'rengyam_tag_meanings'");
            if (!$stmt || !$stmt->fetch()) {
                return null;
            }

            $stmt = $pdo->query("SELECT tag_name, source_label, short_description FROM rengyam_tag_meanings WHERE is_active = 1");
            $rows = $stmt ? $stmt->fetchAll() : [];
            if (!$rows) {
                return null;
            }

            $meta = [];
            foreach ($rows as $row) {
                $tag = trim((string) ($row['tag_name'] ?? ''));
                if ($tag === '') {
                    continue;
                }
                $meta[$tag] = [
                    'source' => trim((string) ($row['source_label'] ?? '')),
                    'description' => trim((string) ($row['short_description'] ?? '')),
                ];
            }

            self::$tagMetaCache = !empty($meta) ? $meta : null;
        } catch (\Throwable $e) {
            self::$tagMetaCache = null;
        }

        return self::$tagMetaCache;
    }

    private static function explainTag($tag)
    {
        $cleanTag = trim((string) $tag);
        $meta = self::getTagMetaFromDb();
        if ($cleanTag !== '' && is_array($meta) && isset($meta[$cleanTag])) {
            $dbDescription = trim((string) ($meta[$cleanTag]['description'] ?? ''));
            if ($dbDescription !== '') {
                return $dbDescription;
            }
        }

        return self::explainTagFallback($cleanTag);
    }

    private static function buildTagDetails(array $displayTagsPrioritized, $school = null)
    {
        $meta = self::getTagMetaFromDb();
        $details = [];
        foreach ($displayTagsPrioritized as $tag) {
            $cleanTag = trim((string) $tag);
            $source = self::getTagSourceLabel($cleanTag);
            if ($cleanTag !== '' && is_array($meta) && isset($meta[$cleanTag])) {
                $dbSource = trim((string) ($meta[$cleanTag]['source'] ?? ''));
                if ($dbSource !== '') {
                    $source = $dbSource;
                }
            }
            $details[] = [
                'tag' => $cleanTag,
                'source' => $source,
                'description' => self::explainTag($cleanTag),
            ];
            if ($school !== null && $school !== '') {
                $details[count($details) - 1]['school'] = $school;
            }
        }
        return $details;
    }

    private static function mergeSourceTagDetails(array $myhoraDetails, array $mahamodoDetails)
    {
        $all = array_merge($myhoraDetails, $mahamodoDetails);
        $merged = [];
        $seen = [];
        foreach ($all as $detail) {
            $tag = trim((string) ($detail['tag'] ?? ''));
            if ($tag === '' || isset($seen[$tag])) {
                continue;
            }
            $detail['display_tag'] = $tag;
            $merged[] = $detail;
            $seen[$tag] = true;
        }

        return $merged;
    }

    /**
     * Combined logic for various auspicious days.
     */
    public static function queryMahaChok($dateStr)
    {
        $date = self::toImmutableDate($dateStr);
        $wDay = self::getMondayBasedWeekdayNumber($date);
        $sunBasedWeekday = self::getSundayBasedWeekdayNumber($date);

        $lunar = self::getThaiLunarDate($dateStr);
        $d = $lunar['day'];
        $dithi = $lunar['dithi'];
        $normalizedMonth = self::getNormalizedThaiMonth($lunar['mThai']);

        $res = [
            'is_ammarit' => false,
            'is_mahasittichok' => false,
            'is_sittichok' => false,
            'is_rachachok' => false,
            'is_chaichok' => false,
            'is_tongchai' => false,
            'is_atipbadee' => false,
            'is_lokawinat' => false,
            'is_ubath' => false,
            'is_kating' => ($sunBasedWeekday === $d)
                || ($normalizedMonth === $d),
            'is_riangmon' => self::isRiangMon($dithi),
        ];

        $kalayok = self::getKalayok($dateStr);
        $res['is_tongchai'] = $kalayok['is_tongchai'];
        $res['is_atipbadee'] = $kalayok['is_atipbadee'];
        $res['is_lokawinat'] = $kalayok['is_lokawinat'];
        $res['is_ubath'] = $kalayok['is_ubath'];

        switch ($wDay) {
            case 7: // Sunday
                if ($d === 8) {
                    $res['is_ammarit'] = true;
                    $res['is_chaichok'] = true;
                }
                if ($d === 14) {
                    $res['is_mahasittichok'] = true;
                }
                if ($d === 11) {
                    $res['is_sittichok'] = true;
                }
                if ($d === 6) {
                    $res['is_rachachok'] = true;
                }
                break;
            case 1: // Monday
                if ($d === 3) {
                    $res['is_ammarit'] = true;
                    $res['is_rachachok'] = true;
                    $res['is_chaichok'] = true;
                }
                if ($d === 12) {
                    $res['is_mahasittichok'] = true;
                }
                if ($d === 5) {
                    $res['is_sittichok'] = true;
                }
                break;
            case 2: // Tuesday
                if ($d === 9) {
                    $res['is_ammarit'] = true;
                    $res['is_rachachok'] = true;
                }
                if ($d === 13) {
                    $res['is_mahasittichok'] = true;
                }
                if ($d === 14) {
                    $res['is_sittichok'] = true;
                }
                if ($d === 11) {
                    $res['is_chaichok'] = true;
                }
                break;
            case 3: // Wednesday
                if ($d === 2) {
                    $res['is_ammarit'] = true;
                }
                if ($d === 4) {
                    $res['is_mahasittichok'] = true;
                }
                if ($d === 10) {
                    $res['is_sittichok'] = true;
                    $res['is_chaichok'] = true;
                }
                if ($d === 6) {
                    $res['is_rachachok'] = true;
                }
                break;
            case 4: // Thursday
                if ($d === 4) {
                    $res['is_ammarit'] = true;
                    $res['is_chaichok'] = true;
                }
                if ($d === 3 || $d === 7) {
                    $res['is_mahasittichok'] = true;
                }
                if ($d === 9) {
                    $res['is_sittichok'] = true;
                }
                if ($d === 10) {
                    $res['is_rachachok'] = true;
                }
                break;
            case 5: // Friday
                if ($d === 1) {
                    $res['is_ammarit'] = true;
                    $res['is_rachachok'] = true;
                    $res['is_chaichok'] = true;
                }
                if ($d === 10) {
                    $res['is_mahasittichok'] = true;
                }
                if ($d === 11) {
                    $res['is_sittichok'] = true;
                }
                break;
            case 6: // Saturday
                if ($d === 5) {
                    $res['is_ammarit'] = true;
                    $res['is_rachachok'] = true;
                }
                if ($d === 15) {
                    $res['is_mahasittichok'] = true;
                }
                if ($d === 4) {
                    $res['is_sittichok'] = true;
                }
                if ($d === 11) {
                    $res['is_chaichok'] = true;
                }
                break;
        }

        return $res;
    }

    /**
     * Backward-compatible wrapper for callers that need the daily live status.
     */
    public static function getAuspiciousStatus($dateStr)
    {
        $status = self::queryMahaChok($dateStr);
        $status['is_wanpra'] = self::isWanPra($dateStr);

        $verifiedOverrides = self::getMyHoraVerifiedTagOverrides();
        $myhoraDisplayTags = $verifiedOverrides[$dateStr] ?? self::buildGenericMyHoraDisplayTags($dateStr, $status);
        $myhoraDisplayTags = self::normalizeMyHoraDisplayTags($myhoraDisplayTags);
        $myhoraBundle = self::buildMyHoraTagBundle($myhoraDisplayTags);
        $myhoraPrioritized = array_merge(
            $myhoraBundle['kal_tags'],
            $myhoraBundle['dithi_tags'],
            $myhoraBundle['day_type_tags'],
            $myhoraBundle['warning_tags'],
            $myhoraBundle['other_tags']
        );
        $myhoraTagDetails = self::buildTagDetails($myhoraPrioritized, 'myhora');

        $mahamodoDisplayTags = self::buildGenericMahamodoDisplayTags($dateStr, $status);
        $mahamodoDisplayTags = self::normalizeMyHoraDisplayTags($mahamodoDisplayTags);
        $mahamodoBundle = self::buildMyHoraTagBundle($mahamodoDisplayTags);
        $mahamodoPrioritized = array_merge(
            $mahamodoBundle['kal_tags'],
            $mahamodoBundle['dithi_tags'],
            $mahamodoBundle['day_type_tags'],
            $mahamodoBundle['warning_tags'],
            $mahamodoBundle['other_tags']
        );
        $mahamodoTagDetails = self::buildTagDetails($mahamodoPrioritized, 'mahamodo');

        $mergedRawTags = array_values(array_unique(array_merge($myhoraPrioritized, $mahamodoPrioritized)));
        $mergedTagDetails = self::mergeSourceTagDetails($myhoraTagDetails, $mahamodoTagDetails);

        $status = array_merge($status, $myhoraBundle);
        $finalTags = $mergedRawTags;
        $finalTagDetails = $mergedTagDetails;

        $initialHasPositive = count(array_filter($mergedRawTags, [self::class, 'isPositiveCalendarTag'])) > 0;
        $initialHasNegative = count(array_filter($mergedRawTags, [self::class, 'isNegativeCalendarTag'])) > 0;
        $needsCleanTag = !$initialHasPositive && !$initialHasNegative;
        if ($needsCleanTag && !in_array('ปลอด', $finalTags, true)) {
            $finalTags[] = 'ปลอด';
            $finalTagDetails[] = [
                'tag' => 'ปลอด',
                'source' => self::getTagSourceLabel('ปลอด'),
                'description' => self::explainTag('ปลอด'),
            ];
        }

        $finalTags = array_values(array_unique($finalTags));

        $status['display_tags'] = $finalTags;
        $status['display_tags_prioritized'] = $finalTags;
        $status['tag_details'] = $finalTagDetails;
        $status['calendar_display_tags'] = $finalTags;
        $status['myhora_display_tags'] = $myhoraDisplayTags;
        $status['myhora_display_tags_prioritized'] = $myhoraPrioritized;
        $status['myhora_tag_details'] = $myhoraTagDetails;
        $status['mahamodo_display_tags'] = $mahamodoDisplayTags;
        $status['mahamodo_display_tags_prioritized'] = $mahamodoPrioritized;
        $status['mahamodo_tag_details'] = $mahamodoTagDetails;
        $status['is_ammarit'] = in_array('อำฤตโชค', $finalTags, true);
        $status['is_mahasittichok'] = in_array('มหาสิทธิโชค', $finalTags, true);
        $status['is_sittichok'] = in_array('สิทธิโชค', $finalTags, true);
        $status['is_rachachok'] = in_array('ราชาโชค', $finalTags, true);
        $status['is_chaichok'] = in_array('ชัยโชค', $finalTags, true);
        $status['is_loy'] = in_array('วันลอย', $finalTags, true);
        $status['is_fu'] = in_array('วันฟู', $finalTags, true);
        $status['is_jom'] = in_array('วันจม', $finalTags, true);
        $status['is_kating'] = in_array('กระทิงวัน', $finalTags, true);
        $status['is_riangmon'] = in_array('ดิถีเรียงหมอน', $finalTags, true);
        $status['is_ubath'] = in_array('วันอุบาทว์/อุบาสน', $finalTags, true);
        $status['is_lokawinat'] = in_array('วันโลกาวินาศ', $finalTags, true);
        $status['is_tongchai'] = in_array('วันธงชัย', $finalTags, true);
        $status['is_atipbadee'] = in_array('วันอธิบดี', $finalTags, true);
        $status['has_positive_tags'] = count(array_filter($finalTags, [self::class, 'isPositiveCalendarTag'])) > 0;
        $status['has_negative_tags'] = count(array_filter($finalTags, [self::class, 'isNegativeCalendarTag'])) > 0;
        $status['is_conflict_day'] = $status['has_positive_tags'] && $status['has_negative_tags'];

        return $status;
    }

    public static function getUpcomingWanPras($months = 4)
    {
        $wanpras = [];
        $start = new \DateTime();
        $end = (new \DateTime())->modify("+$months months");
        $current = clone $start;
        $current->modify("-1 day");

        while ($current <= $end) {
            $dateStr = $current->format('Y-m-d');
            if (self::isWanPra($dateStr)) {
                $wanpras[] = [
                    'wanpra_date' => $dateStr,
                ];
            }
            $current->modify("+1 day");
        }

        return $wanpras;
    }
}
