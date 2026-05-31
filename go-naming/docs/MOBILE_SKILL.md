# Chuedee Naming Mobile Development Skill

## Overview
Chuedee Naming API สำหรับการค้นหาชื่อมงคลด้วย AI พร้อมการวิเคราะห์อักษรกาลกิณีตามวันเกิด

## Base URL
```
https://xn--b3cu8e7ah6h.com
```

## Core API Endpoint

### POST /api/v1/name-search
ค้นหาชื่อมงคลตามความหมายที่ต้องการ พร้อมกรองตามวันเกิดและเงื่อนไขต่างๆ

#### Request Headers
```json
{
  "Content-Type": "application/json"
}
```

#### Request Body
```json
{
  "keyword": "ร่ำรวย",
  "lastname": "ทองแสน",
  "day": "Monday",
  "limit": 20,
  "filter_sat": true,
  "filter_sha": true,
  "filter_kaki": true,
  "similar_mode": false
}
```

#### Parameters
- `keyword` (string, required): คำค้นหาความหมาย (เช่น "ร่ำรวย", "อายุยืน", "กล้าหาญ")
- `lastname` (string, optional): ชื่อที่ต้องการนำมา Matching (เช่น นามสกุล หรือ ชื่อญาติพี่น้อง) เพื่อคำนวณผลรวมใหม่
- `day` (string, optional): วันเกิดสำหรับกรองกาลกิณี
  - `"Sunday"`, `"Monday"`, `"Tuesday"`, `"Wednesday1"`, `"Wednesday2"`, `"Thursday"`, `"Friday"`, `"Saturday"`
- `limit` (number, optional): จำนวนผลลัพธ์สูงสุด 1-100 (default: 50)
- `filter_sat` (boolean, optional): กรองเฉพาะเลขศาสตร์ดี (default: true)
- `filter_sha` (boolean, optional): กรองเฉพาะพลังเงาดี (default: true)
- `filter_kaki` (boolean, optional): กรองอักษรกาลกิณี (default: true)
- `similar_mode` (boolean, optional): รวมชื่อ (Matching) — ใช้ Lastname ช่วยดึงแนวทางความหมายจาก Vector และแสดงผลรวมตัวเลข

#### Ranking Filter Semantics
- เปิดเฉพาะ `filter_sat`: แสดงชื่อที่เลขศาสตร์ดีทั้งหมด รวมถึงชื่อที่พลังเงาดีด้วย
- เปิดเฉพาะ `filter_sha`: แสดงชื่อที่พลังเงาดีทั้งหมด รวมถึงชื่อที่เลขศาสตร์ดีด้วย
- เปิดทั้ง `filter_sat` และ `filter_sha`: แสดงเฉพาะชื่อ Double Lucky ที่ผ่านทั้งสองศาสตร์
- เปิด `filter_kaki` พร้อม `day`: ตัดชื่อที่มีอักษรกาลกิณีของวันนั้นออกจากผลลัพธ์

#### Input Detection Algorithm
- ลำดับสูงสุดคือชื่อที่ตรงฐานข้อมูล: ถ้า input ทั้งคำตรง `names_miracle.thname` ให้เป็น `single_name` ทันที
- สำหรับ input สองคำในวรรคเดียว ให้ตรวจ token แต่ละคำกับฐานข้อมูลก่อน semantic signal ถ้าคำแรกมีในฐานข้อมูลและคำที่สองดูเป็นนามสกุล ให้เป็น `full_name`
- ถ้าคำแรกมีในฐานข้อมูลแต่คำที่สองเป็นคำแนวความหมายชัดเจน เช่น `ร่ำรวย`, `บารมี`, `ความ...` ให้คงคำแรกเป็น `single_name`
- ถ้าคำที่สองมีในฐานข้อมูลแต่ไม่มีสัมผัส/โครงสร้างชื่อสกุล ให้ถือคำที่สองเป็น `single_name` แทนการตีความทั้งวลีเป็น meaning
- ถ้าสองคำไม่มีในฐานข้อมูลแต่มีสัมผัสชื่อไทย เช่น `หอยแครง แสงตะวัน` (`แครง` -> `แสง`) ให้เป็น `full_name` แม้จำนวนอักษรรวมจะยาวพอให้เกิด `long_phrase`
- ถ้าเป็นสองคำที่ไม่มี token ใดอยู่ในฐานข้อมูล ไม่มีสัมผัส และมี meaning keywords ให้เป็น `meaning`
- ถ้าเป็นประโยคยาว 3 คำขึ้นไป หรือมีคำบอกความหมายชัดเจน ให้เป็น `meaning` ยกเว้นมี exact DB match ตามกฎแรก

#### Response
```json
{
  "success": true,
  "error": null,
  "results": [
    {
      "name": "สุริยา",
      "meaning": "ผู้มีความเจริญรุ่งเรืองเหมือนดวงอาทิตย์",
      "sat_sum": 15,
      "sha_sum": 8,
      "total_sat": 45,
      "total_sha": 38,
      "distance": 0.25,
      "is_sat_good": true,
      "is_sha_good": true,
      "is_total_sat_good": true,
      "is_total_sha_good": false,
      "kaki_highlight": [
        {
          "char": "ส",
          "is_kaki": false
        },
        {
          "char": "ุ",
          "is_kaki": true
        },
        {
          "char": "ร",
          "is_kaki": false
        },
        {
          "char": "ิ",
          "is_kaki": true
        },
        {
          "char": "ย",
          "is_kaki": false
        },
        {
          "char": "า",
          "is_kaki": true
        }
      ]
    }
  ],
  "total": 1,
  "kaki_info": {
    "day": "Monday",
    "day_th": "จันทร์",
    "description": "สระทั้งหมด + ตัวการันต์",
    "kaki_chars": ["ะ", "ั", "า", "ำ", "ิ", "ี", "ึ", "ื", "ุ", "ู", "เ", "แ", "โ", "ใ", "ไ", "็", "์", "อ", "๊", "้"]
  }
}
```

#### Error Response Example
```json
{
  "success": false,
  "error": {"message": "Invalid JSON body"},
  "results": [],
  "total": 0
}
```

### GET /api/v1/number-meaning
เรียกดูความหมายของตัวเลขตามตำราเลขศาสตร์

#### Parameters
- `number` (string, required): ตัวเลขที่ต้องการค้นหา (1-100)

#### Response
```json
{
  "number": 45,
  "description": "พลังแห่งการรวมกัน",
  "detail": "เลขนี้เป็นเลขดีมาก เป็นเลขที่รวมพลังของดาวที่ส่งผลดี..."
}
```

### GET /api/v1/name-root
วิเคราะห์รากศัพท์และความหมายเชิงลึก

#### Parameters
- `name` (string, required): ชื่อที่ต้องการวิเคราะห์
- `meaning` (string, optional): ความหมายอ้างอิง

#### Response
```json
{
  "name": "สุริยา",
  "root_word": "สุริยะ + อาการ",
  "analysis": "สุริยะ หมายถึง ดวงอาทิตย์..."
}
```

## CORS / Preflight (Mobile)
รองรับ `OPTIONS` สำหรับ preflight และตั้งค่า `Access-Control-Allow-Origin: *` เพื่อให้เรียกจาก mobile app/webview ได้

## Logic & Algorithms สำหรับ Mobile

### 1. การคำนวณผลรวม (Combined Numerology)
เมื่อผู้ใช้กรอก `lastname` ระบบจะคำนวณ `total_sat` (name + lastname) และ `total_sha` ให้อัตโนมัติ โดยอ้างอิงจากเลขศาสตร์ของชื่อที่ค้นพบ

### 2. ผลรวมที่เกิน 100 (3-Digit Logic)
ตามตำรา หากผลรวมหลักเกิน 100 (เช่น 107) จะไม่มีคำทำนายโดยตรง ให้ใช้วิธี **แยกเลขคู่** ดังนี้:
- **คู่หน้า (Front Pair):** ตัวที่ 1 และ 2 (เช่น 10)
- **คู่หลัง (Back Pair):** ตัวที่ 2 และ 3 (เช่น 07)
Mobile app ควรเรียก API `/api/v1/number-meaning` แยกกัน 2 ครั้งเพื่อนำมาแสดงควบคู่กัน

### 3. Highlighting กาลกิณี
ใน `kaki_highlight` ระบบแยกตัวอักษรให้แล้ว Mobile ควรใช้ `SpannableString` (Android) หรือ `AttributedString` (iOS) ในการใส่สีแดงตาม Flag `is_kaki`

### 4. Algorithm Synchronization (3 Levels Fallback)
เพื่อให้แอปแสดงผลได้อย่างลื่นไหลและไม่ค่อยพบว่า "ไม่พบชื่อ" ระบบ API มีกลยุทธ์ดังนี้:
1. **Level 1 (Strict):** ค้นหาแบบตรงเงื่อนไข 100% (Sat ดี AND Sha ดี AND ไม่มีกาลกิณี)
2. **Level 2 (Relaxed):** หากไม่พบ ระบบจะลดระดับมาหาชื่อที่มีเลขศาสตร์ดีอย่างใดอย่างหนึ่ง (Sat ดี OR Sha ดี) แทนอัตโนมัติ
3. **Level 3 (Desperate):** หากยังไม่พบอีก ระบบจะคืนค่าชื่อที่มีความหมายใกล้เคียงที่สุดตาม Vector โดยละฟิลเตอร์ Sat/Sha ออกทั้งหมด แต่ยังคงฟิลเตอร์กาลกิณี (ถ้าเลือก) ไว้

### 4.1 Indexed Top-Up สำหรับ Mobile Ranking
หลังจากรวมผลลัพธ์จาก semantic/trigram pools แล้ว backend จะกรองซ้ำด้วยกติกาเดียวกับ mobile UI และเติมผลลัพธ์จาก indexed filter path (`sat_sum`, `sha_sum`, `k_{day}`) จนเข้าใกล้ 100 รายการเท่าที่ฐานข้อมูลมีชื่อผ่านเงื่อนไขจริง วิธีนี้ช่วยให้เคสชื่อแม่แบบสั้น เช่น "ใบเตย" ไม่หยุดที่ candidate กลุ่มแรกเพียงไม่กี่รายการ และยังควบคุมต้นทุน query สำหรับผู้ใช้พร้อมกันจำนวนมาก

### 5. Prioritized Sorting (Exact Match)
เมื่อมีการส่ง `lastname` มาเพื่อเป็น Matching Name ระบบจะทำการค้นหาชื่อที่ **สะกดเหมือนกับ Matching Name** ในฐานข้อมูลขึ้นมาแสดงเป็นอันดับแรกสุดเสมอ (ถ้ามี) เพื่อให้ผู้ใช้สามารถวิเคราะห์ชื่อปัจจุบันของตนเองหรือชื่อที่ตั้งเป้าไว้แล้วได้ทันที

### 6. โหมดรวมชื่อ (Matching Mode)
เมื่อเปิด `similar_mode: true` ใน Request:
- **Search Logic:** ระบบจะนำ `lastname` มาต่อท้าย `keyword` เพื่อค้นหา Vector ที่มีทั้งความหมายและ "สไตล์" ของชื่อที่ระบุ (เช่น หาชื่อที่มีสไตล์คล้ายนามสกุล)
- **UI Logic (Checked):** Mobile App **ควรแสดง** ส่วน "ผลรวม Matching" (วงกลมสีแดง) เมื่อผู้ใช้เลือกโหมดนี้
- **UI Logic (Unchecked):** หากผู้ใช้ไม่เลือกโหมดนี้ **ควรซ่อน** ส่วน Matching เพื่อให้ผู้ใช้โฟกัสที่ชื่อเพียงอย่างเดียว
- **Persistence:** เฉพาะเลขศาสตร์เฉพาะตัวชื่อ (Sat/Sha ของชื่อ) **ควรแสดง** ไว้เสมอเพื่อให้ทราบพลังของชื่อนั้นๆ

## Implementation Examples

### Android (Kotlin + Retrofit)

#### 1. Data Models (Updated)
```kotlin
data class NameSearchRequest(
    val keyword: String,
    val lastname: String? = null,
    val day: String? = null,
    val limit: Int = 50,
    val filter_sat: Boolean = true,
    val filter_sha: Boolean = true,
    val filter_kaki: Boolean = true,
    val similar_mode: Boolean = false
)

data class CharHighlight(
    val char: String,
    val is_kaki: Boolean
)

data class NameResult(
    val name: String,
    val meaning: String,
    val sat_sum: Int,
    val sha_sum: Int,
    val total_sat: Int,
    val total_sha: Int,
    val distance: Double,
    val is_sat_good: Boolean,
    val is_sha_good: Boolean,
    val is_total_sat_good: Boolean,
    val is_total_sha_good: Boolean,
    val kaki_highlight: List<CharHighlight>
)

## Numerical Values Reference (Real-time Calculation)
เพื่อให้ Mobile App สามารถคำนวณผลรวมได้รวดเร็วแบบ Real-time (ขณะพิมพ์) Developer ควร implement logic ฝั่ง Client โดยอ้างอิงตารางค่าตัวเลขดังนี้:

### 1. เลขศาสตร์ (SatValues)
```json
{
  "A": 1, "B": 2, "C": 3, "D": 4, "E": 5, "F": 8, "G": 3, "H": 5, "I": 1, "J": 1, "K": 2, "L": 3, "M": 4,
  "N": 5, "O": 7, "P": 8, "Q": 1, "R": 2, "S": 3, "T": 4, "U": 6, "V": 6, "W": 6, "X": 5, "Y": 1, "Z": 7,
  "a": 1, "b": 2, "c": 3, "d": 4, "e": 5, "f": 8, "g": 3, "h": 5, "i": 1, "j": 1, "k": 2, "l": 3, "m": 4,
  "n": 5, "o": 7, "p": 8, "q": 1, "r": 2, "s": 3, "t": 4, "u": 6, "v": 6, "w": 6, "x": 5, "y": 1, "z": 7,
  "ก": 1, "ข": 2, "ฃ": 3, "ค": 4, "ฅ": 5, "ฆ": 3, "ง": 2, "จ": 6, "ฉ": 5, "ช": 2, "ซ": 7, "ฌ": 5, "ญ": 4, "ฎ": 5, "ฏ": 9,
  "ฐ": 9, "ฑ": 3, "ฒ": 3, "ณ": 5, "ด": 1, "ต": 3, "ถ": 1, "ท": 1, "ธ": 4, "น": 5, "บ": 2, "ป": 2, "ผ": 8, "ฝ": 8, "พ": 8,
  "ฟ": 8, "ภ": 1, "ม": 5, "ย": 8, "ร": 4, "ล": 6, "ว": 6, "ศ": 7, "ษ": 4, "ส": 7, "ห": 5, "ฬ": 5, "อ": 6, "ฮ": 5,
  "ะ": 4, "ั": 4, "า": 1, "ำ": 1, "ิ": 4, "ี": 7, "ึ": 5, "ื": 7, "ุ": 1, "ู": 2, "เ": 2, "แ": 2, "โ": 4, "ใ": 6, "ไ": 9,
  "็": 8, "่": 1, "้": 2, "๊": 7, "๋": 3, "์": 9, "ฤ": 1, "ฦ": 1
}
```

### 2. พลังเงา (ShaValues)
```json
{
  "ก": 15, "ข": 15, "ฃ": 15, "ค": 15, "ฅ": 15, "ฆ": 15, "ง": 15,
  "จ": 8, "ฉ": 8, "ช": 8, "ซ": 8, "ฌ": 8, "ญ": 8,
  "ฎ": 17, "ฏ": 17, "ฐ": 17, "ฑ": 17, "ฒ": 17, "ณ": 17,
  "ด": 10, "ต": 10, "ถ": 10, "ท": 10, "ธ": 10, "น": 10,
  "บ": 19, "ป": 19, "ผ": 19, "ฝ": 19, "พ": 19, "ฟ": 19, "ภ": 19, "ม": 19,
  "ย": 12, "ร": 12, "ล": 12, "ว": 12,
  "ศ": 21, "ษ": 21, "ส": 21, "ห": 21, "ฬ": 21, "อ": 6, "ฮ": 21,
  "ะ": 6, "า": 6, "ิ": 6, "ี": 6, "ุ": 6, "ู": 6, "เ": 6, "โ": 6
}
```
data class KakiInfo(
    val day: String,
    val day_th: String,
    val description: String,
    val kaki_chars: List<String>
)

data class NameSearchResponse(
    val success: Boolean,
    val results: List<NameResult>,
    val kaki_info: KakiInfo
)
```

#### 2. API Service
```kotlin
interface ChuedeeApiService {
    @POST("/api/v1/name-search")
    suspend fun searchNames(
        @Body request: NameSearchRequest
    ): Response<NameSearchResponse>
    
    companion object {
        private const val BASE_URL = "https://xn--b3cu8e7ah6h.com/"
        
        fun create(): ChuedeeApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ChuedeeApiService::class.java)
        }
    }
}
```

#### 3. Repository
```kotlin
class NameRepository {
    private val apiService = ChuedeeApiService.create()
    
    suspend fun searchNames(
        keyword: String,
        day: String? = null,
        filters: NameFilters = NameFilters()
    ): Result<List<NameResult>> {
        return try {
            val request = NameSearchRequest(
                keyword = keyword,
                day = day,
                limit = 20,
                filter_sat = filters.satGood,
                filter_sha = filters.shaGood,
                filter_kaki = filters.filterKaki
            )
            
            val response = apiService.searchNames(request)
            if (response.isSuccessful) {
                Result.success(response.body()?.results ?: emptyList())
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class NameFilters(
    val satGood: Boolean = true,
    val shaGood: Boolean = true,
    val filterKaki: Boolean = true
)
```

#### 4. ViewModel
```kotlin
class NameSearchViewModel : ViewModel() {
    private val repository = NameRepository()
    private val _searchResults = MutableLiveData<List<NameResult>>()
    val searchResults: LiveData<List<NameResult>> = _searchResults
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    fun searchNames(keyword: String, day: String?, filters: NameFilters) {
        if (keyword.isBlank()) {
            _errorMessage.value = "กรุณากรอกคำค้นหา"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.searchNames(keyword, day, filters)
                result.fold(
                    onSuccess = { names ->
                        _searchResults.value = names
                        _errorMessage.value = if (names.isEmpty()) "ไม่พบชื่อที่ตรงเงื่อนไข" else null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "เกิดข้อผิดพลาด: ${error.message}"
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

#### 5. UI Implementation with Highlighting
```kotlin
class NameAdapter : ListAdapter<NameResult, NameViewHolder>(NameDiffCallback()) {
    
    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        val name = getItem(position)
        holder.bind(name)
    }
    
    class NameViewHolder(private val binding: ItemNameBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(name: NameResult) {
            binding.nameText.text = createHighlightedText(name.name, name.kaki_highlight)
            binding.meaningText.text = name.meaning
            
            // Display numerical analysis
            binding.satSumText.text = "เลขศาสตร์: ${name.sat_sum}"
            binding.shaSumText.text = "พลังเงา: ${name.sha_sum}"
            
            // Set colors based on goodness
            binding.satSumText.setTextColor(
                ContextCompat.getColor(binding.root.context, 
                    if (name.is_sat_good) R.color.good_color else R.color.neutral_color)
            )
            binding.shaSumText.setTextColor(
                ContextCompat.getColor(binding.root.context,
                    if (name.is_sha_good) R.color.good_color else R.color.neutral_color)
            )
        }
        
        private fun createHighlightedText(name: String, highlights: List<CharHighlight>): SpannableStringBuilder {
            val spannable = SpannableStringBuilder(name)
            
            highlights.forEach { highlight ->
                if (highlight.is_kaki) {
                    val startIndex = name.indexOf(highlight.char)
                    if (startIndex >= 0) {
                        val endIndex = startIndex + highlight.char.length
                        spannable.setSpan(
                            ForegroundColorSpan(Color.RED),
                            startIndex,
                            endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
            
            return spannable
        }
    }
}
```

### iOS (Swift + URLSession)

#### 1. Data Models
```swift
struct NameSearchRequest: Codable {
    let keyword: String
    let day: String?
    let limit: Int
    let filter_sat: Bool
    let filter_sha: Bool
    let filter_kaki: Bool
}

struct CharHighlight: Codable {
    let char: String
    let is_kaki: Bool
}

struct NameResult: Codable {
    let name: String
    let meaning: String
    let sat_sum: Int
    let sha_sum: Int
    let distance: Double
    let is_sat_good: Bool
    let is_sha_good: Bool
    let kaki_highlight: [CharHighlight]
}

struct KakiInfo: Codable {
    let day: String
    let day_th: String
    let description: String
    let kaki_chars: [String]
}

struct NameSearchResponse: Codable {
    let success: Bool
    let results: [NameResult]
    let kaki_info: KakiInfo
}
```

#### 2. API Service
```swift
class ChuedeeAPIService {
    static let shared = ChuedeeAPIService()
    private let baseURL = "https://xn--b3cu8e7ah6h.com"
    
    private init() {}
    
    func searchNames(
        keyword: String,
        day: String? = nil,
        limit: Int = 20,
        filterSat: Bool = true,
        filterSha: Bool = true,
        filterKaki: Bool = true
    ) async throws -> NameSearchResponse {
        
        let request = NameSearchRequest(
            keyword: keyword,
            day: day,
            limit: limit,
            filter_sat: filterSat,
            filter_sha: filterSha,
            filter_kaki: filterKaki
        )
        
        guard let url = URL(string: "\(baseURL)/api/v1/name-search") else {
            throw APIError.invalidURL
        }
        
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        urlRequest.httpBody = try JSONEncoder().encode(request)
        
        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw APIError.serverError
        }
        
        return try JSONDecoder().decode(NameSearchResponse.self, from: data)
    }
    
    enum APIError: Error {
        case invalidURL
        case serverError
        case decodingError
    }
}
```

#### 3. ViewModel
```swift
@MainActor
class NameSearchViewModel: ObservableObject {
    @Published var searchResults: [NameResult] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var kakiInfo: KakiInfo?
    
    private let apiService = ChuedeeAPIService.shared
    
    func searchNames(
        keyword: String,
        day: String? = nil,
        filters: NameFilters
    ) async {
        guard !keyword.isEmpty else {
            errorMessage = "กรุณากรอกคำค้นหา"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            let response = try await apiService.searchNames(
                keyword: keyword,
                day: day,
                filterSat: filters.satGood,
                filterSha: filters.shaGood,
                filterKaki: filters.filterKaki
            )
            
            searchResults = response.results
            kakiInfo = response.kaki_info
            
            if searchResults.isEmpty {
                errorMessage = "ไม่พบชื่อที่ตรงเงื่อนไข"
            }
        } catch {
            errorMessage = "เกิดข้อผิดพลาด: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
}

struct NameFilters {
    var satGood = true
    var shaGood = true
    var filterKaki = true
}
```

#### 4. UI Implementation with Highlighting
```swift
struct NameRowView: View {
    let nameResult: NameResult
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Name with kaki highlighting
            Text(highlightedName(nameResult.name, highlights: nameResult.kaki_highlight))
                .font(.title2)
                .fontWeight(.bold)
            
            Text(nameResult.meaning)
                .font(.body)
                .foregroundColor(.secondary)
            
            HStack {
                Label("เลขศาสตร์: \(nameResult.sat_sum)", systemImage: "calculator")
                    .foregroundColor(nameResult.is_sat_good ? .green : .gray)
                
                Spacer()
                
                Label("พลังเงา: \(nameResult.sha_sum)", systemImage: "moon.fill")
                    .foregroundColor(nameResult.is_sha_good ? .blue : .gray)
            }
            .font(.caption)
        }
        .padding()
        .background(Color.gray.opacity(0.1))
        .cornerRadius(8)
    }
    
    private func highlightedName(_ name: String, highlights: [CharHighlight]) -> Text {
        var result = Text("")
        
        for highlight in highlights {
            let color: Color = highlight.is_kaki ? .red : .primary
            result = result + Text(highlight.char).foregroundColor(color)
        }
        
        return result
    }
}
```

### Flutter (Dart)

#### 1. Data Models
```dart
class NameSearchRequest {
  final String keyword;
  final String? day;
  final int limit;
  final bool filterSat;
  final bool filterSha;
  final bool filterKaki;

  NameSearchRequest({
    required this.keyword,
    this.day,
    this.limit = 20,
    this.filterSat = true,
    this.filterSha = true,
    this.filterKaki = true,
  });

  Map<String, dynamic> toJson() {
    return {
      'keyword': keyword,
      'day': day,
      'limit': limit,
      'filter_sat': filterSat,
      'filter_sha': filterSha,
      'filter_kaki': filterKaki,
    };
  }
}

class CharHighlight {
  final String char;
  final bool isKaki;

  CharHighlight({required this.char, required this.isKaki});

  factory CharHighlight.fromJson(Map<String, dynamic> json) {
    return CharHighlight(
      char: json['char'],
      isKaki: json['is_kaki'],
    );
  }
}

class NameResult {
  final String name;
  final String meaning;
  final int satSum;
  final int shaSum;
  final double distance;
  final bool isSatGood;
  final bool isShaGood;
  final List<CharHighlight> kakiHighlight;

  NameResult({
    required this.name,
    required this.meaning,
    required this.satSum,
    required this.shaSum,
    required this.distance,
    required this.isSatGood,
    required this.isShaGood,
    required this.kakiHighlight,
  });

  factory NameResult.fromJson(Map<String, dynamic> json) {
    return NameResult(
      name: json['name'],
      meaning: json['meaning'],
      satSum: json['sat_sum'],
      shaSum: json['sha_sum'],
      distance: json['distance'].toDouble(),
      isSatGood: json['is_sat_good'],
      isShaGood: json['is_sha_good'],
      kakiHighlight: (json['kaki_highlight'] as List)
          .map((i) => CharHighlight.fromJson(i))
          .toList(),
    );
  }
}

class NameSearchResponse {
  final bool success;
  final List<NameResult> results;
  final KakiInfo kakiInfo;

  NameSearchResponse({
    required this.success,
    required this.results,
    required this.kakiInfo,
  });

  factory NameSearchResponse.fromJson(Map<String, dynamic> json) {
    return NameSearchResponse(
      success: json['success'],
      results: (json['results'] as List)
          .map((i) => NameResult.fromJson(i))
          .toList(),
      kakiInfo: KakiInfo.fromJson(json['kaki_info']),
    );
  }
}

class KakiInfo {
  final String day;
  final String dayTh;
  final String description;
  final List<String> kakiChars;

  KakiInfo({
    required this.day,
    required this.dayTh,
    required this.description,
    required this.kakiChars,
  });

  factory KakiInfo.fromJson(Map<String, dynamic> json) {
    return KakiInfo(
      day: json['day'],
      dayTh: json['day_th'],
      description: json['description'],
      kakiChars: List<String>.from(json['kaki_chars']),
    );
  }
}
```

#### 2. API Service
```dart
class ChuedeeApiService {
  static const String _baseUrl = 'https://xn--b3cu8e7ah6h.com';
  final http.Client _client = http.Client();

  Future<NameSearchResponse> searchNames(NameSearchRequest request) async {
    final response = await _client.post(
      Uri.parse('$_baseUrl/api/v1/name-search'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode(request.toJson()),
    );

    if (response.statusCode == 200) {
      return NameSearchResponse.fromJson(json.decode(response.body));
    } else {
      throw Exception('Failed to search names: ${response.statusCode}');
    }
  }
}
```

#### 3. Provider/Bloc
```dart
class NameSearchProvider extends ChangeNotifier {
  final ChuedeeApiService _apiService = ChuedeeApiService();
  
  List<NameResult> _results = [];
  List<NameResult> get results => _results;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  KakiInfo? _kakiInfo;
  KakiInfo? get kakiInfo => _kakiInfo;

  Future<void> searchNames({
    required String keyword,
    String? day,
    bool filterSat = true,
    bool filterSha = true,
    bool filterKaki = true,
  }) async {
    if (keyword.trim().isEmpty) {
      _errorMessage = 'กรุณากรอกคำค้นหา';
      notifyListeners();
      return;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final request = NameSearchRequest(
        keyword: keyword,
        day: day,
        filterSat: filterSat,
        filterSha: filterSha,
        filterKaki: filterKaki,
      );

      final response = await _apiService.searchNames(request);
      
      _results = response.results;
      _kakiInfo = response.kakiInfo;

      if (_results.isEmpty) {
        _errorMessage = 'ไม่พบชื่อที่ตรงเงื่อนไข';
      }
    } catch (e) {
      _errorMessage = 'เกิดข้อผิดพลาด: $e';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
```

#### 4. UI Implementation with Highlighting
```dart
class NameCard extends StatelessWidget {
  final NameResult nameResult;

  const NameCard({Key? key, required this.nameResult}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Name with kaki highlighting
            _buildHighlightedName(),
            const SizedBox(height: 8),
            
            // Meaning
            Text(
              nameResult.meaning,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Colors.grey[600],
              ),
            ),
            const SizedBox(height: 12),
            
            // Numerical analysis
            Row(
              children: [
                _buildAnalysisChip(
                  'เลขศาสตร์: ${nameResult.satSum}',
                  nameResult.isSatGood ? Colors.green : Colors.grey,
                ),
                const SizedBox(width: 8),
                _buildAnalysisChip(
                  'พลังเงา: ${nameResult.shaSum}',
                  nameResult.isShaGood ? Colors.blue : Colors.grey,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHighlightedName() {
    return RichText(
      text: TextSpan(
        children: nameResult.kakiHighlight.map((highlight) {
          return TextSpan(
            text: highlight.char,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: highlight.isKaki ? Colors.red : Colors.black,
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildAnalysisChip(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 12,
          color: color,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}
```

## Key Implementation Notes

### 1. Kaki Highlighting
- อักษรกาลกิณีจะถูก标记 ด้วย `is_kaki: true` ใน `kaki_highlight` array
- แสดงสีแดงใน UI เพื่อให้ผู้ใช้เห็นตัวอักษรที่ไม่พึงประสงค์
- แต่ละตัวอักษรถูกแยกเพื่อให้สามารถ highlight ได้แม่นยำ

### 2. Day Selection
- ใช้ค่าภาษาอังกฤษสำหรับ API: `"Monday"`, `"Tuesday"`, etc.
- สำหรับวันพุธมี 2 ค่า: `"Wednesday1"` (พุธกลางวัน) และ `"Wednesday2"` (พุธกลางคืน)

### 3. Error Handling
- ตรวจสอบสถานะ HTTP response (200 = success)
- จัดการกรณีไม่มีข้อมูล (empty results)
- แสดงข้อความผิดพลาดที่เป็นประโยชน์แก่ผู้ใช้

### 4. Performance
- Implement pagination สำหรับผลลัพธ์จำนวนมาก
- Cache ผลลัพธ์สำหรับการค้นหาซ้ำ
- Debounce การค้นหาแบบ real-time ถ้าจะ implement auto-search

### 5. UI/UX Best Practices
- แสดง loading indicator ระหว่างค้นหา
- ใช้ color coding สำหรับค่าต่างๆ (เขียว=ดี, แดง=กาลกิณี)
- จัดกลุ่มผลลัพธ์ตามความเกี่ยวข้อง (distance)
- ให้สามารถ copy ชื่อที่สนใจได้

## Testing

### Example Request/Response
```bash
curl -X POST https://xn--b3cu8e7ah6h.com/api/v1/name-search \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "ร่ำรวย",
    "day": "Monday",
    "limit": 5,
    "filter_sat": true,
    "filter_sha": true,
    "filter_kaki": true
  }'
```

## Support
สำหรับข้อมูลเพิ่มเติม ดูที่: https://xn--b3cu8e7ah6h.com/api-mobile-doc
