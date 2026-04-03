# RengYam (ดูฤกษ์ยาม) Calendar — Developer Knowledge Base

> อัปเดตล่าสุด: เมษายน 2569 / April 2026  
> แหล่งที่มา: Session pair-programming กับ AI (Antigravity)

---

## 1. โครงสร้างหลักของระบบ

### ไฟล์สำคัญ

| ไฟล์ | หน้าที่ |
|------|---------|
| `ui/renkyam/RengYamF.kt` | Fragment หลัก: โหลดข้อมูล, คำนวณ tag, แสดง calendar grid |
| `adapters/WanpraAdapter.kt` | RecyclerView adapter: แสดง cell วัน, ป้ายอัปมงคล, Weekday header |
| `data/rengyam/Wanpra.kt` | Data class วันหนึ่งๆ ในปฏิทิน |
| `utils/ThaiAstrologyLib.kt` | Library คำนวณโหราศาสตร์ไทย (วันพระ, ฤกษ์ฯลฯ) |
| `utils/RengYamTagCanonicalizer.kt` | Normalize ชื่อ tag ให้เป็นรูปแบบมาตรฐาน |
| `res/layout/fragment_reng_yam.xml` | Layout หลักของ Fragment (ใช้ Data Binding `<layout>`) |
| `res/layout/item_calendar_day.xml` | Layout ของ cell วันในปฏิทิน (grid) |
| `res/layout/item_calendar_weekday_header.xml` | Layout ของ header วัน (จันทร์-อาทิตย์) ใน RecyclerView |

---

## 2. Logic อัปมงคล (Kalagni) — กฎสำคัญ

### 2.1 ฟิลด์ใน `Wanpra.kt` ที่เกี่ยวข้อง

```kotlin
var isKalagni: Boolean = false          // true ถ้าวันนั้นเป็นอัปมงคลอย่างใดอย่างหนึ่ง
var kalagniBirth: Boolean = false       // true ถ้าตรงกับวันอัปมงคลวันเกิด
var kalagniAge: Boolean = false         // true ถ้าตรงกับวันอัปมงคลอายุย่าง
var kalagniBirthLabel: String? = null   // ชื่อวันที่ match เช่น "อังคาร", "ศุกร์"
var kalagniAgeLabel: String? = null     // ชื่อวันที่ match เช่น "พุธ (กลางคืน)"
```

> **หมายเหตุ**: `kalagniBirthLabel` และ `kalagniAgeLabel` ถูกเพิ่มมาเพื่อ display label แบบ specific เช่น "อัปมงคลอายุย่าง (พุธกลางคืน)" แทนที่จะเป็น generic "อัปมงคลอายุย่าง"

---

### 2.2 กฎการ match วัน: `isSameKalagniDay()`

**ระบบนี้ใช้ 06:00 น. เป็นจุดเปลี่ยนวัน** (ไม่ใช่เที่ยงคืน 00:00)

```
วันพุธ = 06:00 น. พุธ ─────────────── 05:59 น. พฤหัส
         └─── กลางวัน ───┘└─── กลางคืน ───┘
                                   ↑
                   "พุธกลางคืน" อยู่ใน "วันพุธ" ของระบบนี้
```

**กฎการ match:**
- `"พุธ (กลางคืน)"` จาก kalagniAgeDays → match กับ **วันพุธ** (ไม่ใช่พฤหัส)
- strip "กลางคืน", "(" และ ")" ออกก่อนเปรียบเทียบ base day name
- label `"(พุธกลางคืน)"` ยังคงอยู่ใน `kalagniAgeLabel` เพื่อการแสดงผลที่ชัดเจน

```kotlin
// ใน RengYamF.kt
private fun isSameKalagniDay(candidate: String, currentDay: String): Boolean {
    val cRaw = normalizeKalagniDayName(candidate)
    val dRaw = normalizeKalagniDayName(currentDay)
    fun stripNight(s: String) = s.replace("กลางคืน", "").replace("(", "").replace(")", "").trim()
    val cBase = stripNight(cRaw)
    val dBase = stripNight(dRaw)
    return cBase == dBase  // match วันเดียวกัน เพราะ 06:00 boundary
}
```

---

### 2.3 Tag ที่แสดงผล

ฟังก์ชัน `applyPersonalizedKalagniTags(wp)` สร้าง tag โดยรวม label เฉพาะเจาะจง:

| สถานการณ์ | Tag ที่แสดง |
|-----------|------------|
| kalagniAge เท่านั้น (ตรงกับ "ศุกร์") | `อัปมงคลอายุย่าง (ศุกร์)` |
| kalagniAge เท่านั้น (ตรงกับ "พุธ (กลางคืน)") | `อัปมงคลอายุย่าง (พุธกลางคืน)` |
| kalagniBirth เท่านั้น | `อัปมงคลวันเกิด (อังคาร)` |
| ทั้ง birth และ age | `อัปมงคลอายุย่าง (xxx)` + `อัปมงคลวันเกิด (yyy)` |

> **สำคัญ**: Filter tag ด้วย `startsWith("อัปมงคล")` ก่อนเพิ่ม tag ใหม่ เพื่อกันการซ้ำซ้อน

---

## 3. Frozen Weekday Header (ตรึงหัวคอลัมน์วัน)

### 3.1 Layout

ใน `fragment_reng_yam.xml` มี `LinearLayout` id = `layout_weekday_frozen` วางซ้อนอยู่บน RecyclerView:

```xml
<!-- fragment_reng_yam.xml — อยู่ภายใน <layout> tag (Data Binding) -->
<LinearLayout
    android:id="@+id/layout_weekday_frozen"
    android:visibility="gone"
    android:elevation="8dp"
    app:layout_constraintTop_toTopOf="parent" ...>

    <!-- แถวเดือน+ปี + ปุ่ม Prev/Next -->
    <LinearLayout ...>
        <TextView android:id="@+id/frozen_btn_month_dropdown" ... />
        <ImageView android:id="@+id/frozen_btn_prev_month" ... />
        <TextView android:id="@+id/frozen_txt_month" ... />
        <TextView android:id="@+id/frozen_txt_year" ... />
        <ImageView android:id="@+id/frozen_btn_next_month" ... />
        <TextView android:id="@+id/frozen_btn_year_dropdown" ... />
    </LinearLayout>

    <!-- แถวหัววัน จันทร์-อาทิตย์ -->
    <LinearLayout ...> ... </LinearLayout>
</LinearLayout>
```

> **กฎสำคัญ**: frozen header **ต้องอยู่ภายใน `<layout>` tag** ของ Data Binding file  
> ถ้าใช้ `<include>` ที่ชี้ไปยัง layout ที่ไม่มี `<layout>` tag จะ compile ไม่ผ่าน (Unresolved reference)

### 3.2 Scroll Listener ใน `setupAdapterList()`

```kotlin
rengYamBinding.recyclerviewWanpra.clearOnScrollListeners()
rengYamBinding.recyclerviewWanpra.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
        val lm = rv.layoutManager as? GridLayoutManager ?: return
        val firstVisible = lm.findFirstVisibleItemPosition()
        val viewAtPos1 = lm.findViewByPosition(1)
        val shouldShow = if (viewAtPos1 != null) viewAtPos1.top <= 0 else firstVisible > 0
        rengYamBinding.layoutWeekdayFrozen.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }
})
```

> scroll listener ต้องอยู่ **นอก** `if (adapter == null)` block เพื่อให้ทำงานได้ทุกครั้ง

### 3.3 Sync เดือน/ปี ให้ frozen header

```kotlin
// เรียกทุกครั้งที่ headerData เปลี่ยน
private fun syncFrozenHeader() {
    if (!isAdded) return
    rengYamBinding.layoutWeekdayFrozen
        .findViewById<TextView>(R.id.frozen_txt_month)?.text = headerData.monthStr
    rengYamBinding.layoutWeekdayFrozen
        .findViewById<TextView>(R.id.frozen_txt_year)?.text = toThaiNum(headerData.yearStr)
}
```

Call chain: `initHeaderData()` → `updateRecyclerHeader()` → `syncFrozenHeader()`

---

## 4. Badge Deduplication (กฎกันป้ายซ้ำ)

### 4.1 ใน `WanpraAdapter.kt` (CalendarDayHolder / WanparHolder)

```kotlin
val existingTags = buildString { addAll(wp.calendarDisplayTags.orEmpty()); ... }

if (wp.kalagniBirth && wp.kalagniAge) {
    // แสดงป้ายรวม ไม่แสดงแยก
    if (!existingTags.contains("อัปมงคลอายุย่าง และวันเกิด")) {
        addBadge("อัปมงคลอายุย่าง และวันเกิด", forbiddenColor)
    }
} else {
    if (wp.kalagniBirth) addBadge(...)
    if (wp.kalagniAge)   addBadge(...)
    // wp.isKalagni && !birth && !age → "วันอัปมงคล" ทั่วไป
}
```

> **กับดัก**: `wp.isKalagni` เป็น `true` เสมอถ้า `kalagniBirth` หรือ `kalagniAge` เป็น true  
> อย่าใช้ `wp.isKalagni` คู่กับ `wp.kalagniAge` ในเงื่อนไขเดียวกัน จะทำให้ป้ายซ้ำ

### 4.2 ใน `applyPersonalizedKalagniTags()` (RengYamF.kt)

- filter ด้วย `startsWith("อัปมงคล")` ก่อนเพิ่ม tag ใหม่ทุกครั้ง
- เพิ่ม tag ใหม่พร้อม label เฉพาะ เช่น `"อัปมงคลอายุย่าง (พุธกลางคืน)"`

---

## 5. ลำดับการ match และ inject tag

```
updateRecyclerWithData()
  ├── reset: isKalagni/kalagniBirth/kalagniAge = false
  ├── match: firstOrNull { isSameKalagniDay(...) } → populate kalagniAgeLabel/kalagniBirthLabel
  ├── applyPersonalizedKalagniTags(wp) → สร้าง tag พร้อม label
  ├── sanitizeAgniknirodTags(wp, dt)
  └── RengYamTagCanonicalizer.normalize(wp)
```

---

## 6. Grid Layout (7 คอลัมน์)

```kotlin
GridLayoutManager(context, 7).apply {
    spanSizeLookup = object : SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (adapter.getItemViewType(position)) {
                TYPE_CALENDAR_DAY, TYPE_WEEKDAY_HEADER, TYPE_EMPTY_DAY -> 1  // 1/7 width
                else -> 7  // full width (header, legend, list)
            }
        }
    }
}
```

- TopHeader (เดือน/ปี, ฤกษ์หมวดหมู่) → span 7
- WeekdayHeader (จันทร์-อาทิตย์) → span 1 แต่มี 7 ตัว
- CalendarDay → span 1
- EmptyDay → span 1 (วันเดือนก่อน/หลัง, สีซีด)

---

## 7. สีป้ายอ้างอิง

| ป้าย | สี |
|------|-----|
| อัปมงคลวันเกิด / อายุย่าง | `#D32F2F` (แดงเข้ม) |
| วันอาทิตย์ (header) | `#FF9800` (ส้ม) |
| วันอื่น (header) | `#E0D8B0` (เบจ) |
| วันที่เลือก (ฤกษ์ดี) | `#E8F5E9` (เขียวอ่อน) |
| Best Day (Marquee) | `bg_calendar_cell_best` |

---

## 8. ข้อควรระวังพิเศษ (Gotchas)

1. **`<include>` vs inline** — ถ้าจะใช้ Data Binding กับ view ที่ include มา layout นั้นต้องมี `<layout>` ห่อด้วย ไม่งั้น binding property จะไม่ถูกสร้าง
2. **Parcelable** — ถ้าเพิ่ม field ใหม่ใน `Wanpra.kt` ต้องเพิ่มทั้ง `constructor(source: Parcel)` และ `writeToParcel()` ให้ครบ
3. **`isKalagni` trap** — `isKalagni = kalagniBirth || kalagniAge` ดังนั้นการเช็ค `isKalagni` จะ true ถ้ามีอย่างใดอย่างหนึ่ง อย่าใช้แทน `kalagniAge` โดยตรง
4. **clearOnScrollListeners()** — ต้องเรียก clear ก่อน addOnScrollListener ทุกครั้งที่ `setupAdapterList()` ถูก call เพราะ method นี้อาจถูก call หลายครั้ง (เมื่อ refresh หรือเปลี่ยน permission)
