-- Quick cleanup: remove Chinese/foreign chars from phonetic columns
-- Only affects rows that actually have foreign characters

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
        ELSIF (code >= 65 AND code <= 90) OR (code >= 97 AND code <= 122) OR (code >= 48 AND code <= 57) THEN
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

UPDATE names_miracle SET phonetic_summary = strip_non_thai(phonetic_summary) WHERE phonetic_summary IS DISTINCT FROM strip_non_thai(phonetic_summary);
UPDATE names_miracle SET phonetic_issues = (SELECT jsonb_agg(strip_non_thai(elem::text)) FROM jsonb_array_elements(phonetic_issues) AS elem WHERE strip_non_thai(elem::text) <> '') WHERE phonetic_issues IS NOT NULL AND phonetic_issues <> '[]'::jsonb;
UPDATE names_miracle SET phonetic_style_tone = (SELECT jsonb_agg(strip_non_thai(elem::text)) FROM jsonb_array_elements(phonetic_style_tone) AS elem WHERE strip_non_thai(elem::text) <> '') WHERE phonetic_style_tone IS NOT NULL AND phonetic_style_tone <> '[]'::jsonb;
UPDATE names_miracle SET phonetic_labels = (SELECT jsonb_agg(strip_non_thai(elem::text)) FROM jsonb_array_elements(phonetic_labels) AS elem WHERE strip_non_thai(elem::text) <> '') WHERE phonetic_labels IS NOT NULL AND phonetic_labels <> '[]'::jsonb;

DROP FUNCTION IF EXISTS strip_non_thai;
SELECT 'DONE' AS status;
