# Outfit UI - Source of Truth

การแสดงผลสีมงคล/อัปมงคลของแอป Android ต้องอ้างอิง logic จาก:

- `/Users/tayap/project-naming/outfit-miracle/main.go`
- `/Users/tayap/project-naming/outfit-miracle/SKILL.md`

## Endpoint ที่ใช้

- `POST https://ชื่อดี.com/api/v1/outfit-miracle/color-sets`

request body แนะนำ:

```json
{
  "current_day_name": "ศุกร์",
  "birth_day_name": "อาทิตย์",
  "age_years": 44
}
```

## จุดเชื่อมในแอป

- `app/src/main/java/com/numberniceic/ui/apersonnews/PersonNewsViewModel.kt`
- `app/src/main/java/com/numberniceic/ui/apersonnews/Clothcolor3df.kt`
- `app/src/main/java/com/numberniceic/data/persons/OutfitMiracleColorSetsResponse.kt`

UI ต้องแสดงค่า `reference_skill_doc` ที่ backend ส่งกลับ เพื่อให้ทีมตรวจสอบแหล่งอ้างอิงได้ทันที
