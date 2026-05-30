-- Clean existing Chinese/foreign characters from phonetic columns
-- Safe to run multiple times (idempotent)

-- Helper function: keep only Thai + basic ASCII + common punctuation
CREATE OR REPLACE FUNCTION strip_non_thai(s TEXT)
RETURNS TEXT AS $$
DECLARE
    result TEXT := '';
    ch TEXT;
    code INT;
BEGIN
    IF s IS NULL THEN RETURN NULL; END IF;
    FOR i IN 1..char_length(s) LOOP
        ch := substr(s, i, 1);
        code := ascii(ch);
        IF code BETWEEN 3584 AND 3711 THEN
            result := result || ch;
        ELSIF (code >= 65 AND code <= 90) OR
              (code >= 97 AND code <= 122) OR
              (code >= 48 AND code <= 57) THEN
            result := result || ch;
        ELSIF code IN (32, 44, 46, 33, 63, 45, 40, 41, 34, 39, 58) THEN
            result := result || ch;
        ELSIF code IN (10, 13, 9) THEN
            result := result || ' ';
        END IF;
    END LOOP;
    RETURN trim(regexp_replace(result, '\s+', ' ', 'g'));
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 1. Clean phonetic_summary (TEXT)
UPDATE names_miracle
SET phonetic_summary = strip_non_thai(phonetic_summary)
WHERE phonetic_summary IS DISTINCT FROM strip_non_thai(phonetic_summary);

-- 2. Clean phonetic_issues (JSONB array of strings)
UPDATE names_miracle
SET phonetic_issues = (
    SELECT jsonb_agg(strip_non_thai(elem::text) ORDER BY ord)
    FROM jsonb_array_elements(phonetic_issues) WITH ORDINALITY AS t(elem, ord)
    WHERE strip_non_thai(elem::text) <> ''
)
WHERE phonetic_issues IS NOT NULL
  AND phonetic_issues <> '[]'::jsonb;

-- 3. Clean phonetic_style_tone (JSONB array)
UPDATE names_miracle
SET phonetic_style_tone = (
    SELECT jsonb_agg(strip_non_thai(elem::text) ORDER BY ord)
    FROM jsonb_array_elements(phonetic_style_tone) WITH ORDINALITY AS t(elem, ord)
    WHERE strip_non_thai(elem::text) <> ''
)
WHERE phonetic_style_tone IS NOT NULL
  AND phonetic_style_tone <> '[]'::jsonb;

-- 4. Clean phonetic_labels (JSONB array)
UPDATE names_miracle
SET phonetic_labels = (
    SELECT jsonb_agg(strip_non_thai(elem::text) ORDER BY ord)
    FROM jsonb_array_elements(phonetic_labels) WITH ORDINALITY AS t(elem, ord)
    WHERE strip_non_thai(elem::text) <> ''
)
WHERE phonetic_labels IS NOT NULL
  AND phonetic_labels <> '[]'::jsonb;

-- Show summary
SELECT 'phonetic_summary' AS col, COUNT(*) AS cleaned_count FROM names_miracle WHERE phonetic_summary IS DISTINCT FROM strip_non_thai(phonetic_summary);

DROP FUNCTION IF EXISTS strip_non_thai;
