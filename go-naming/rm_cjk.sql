CREATE OR REPLACE FUNCTION remove_cjk(s TEXT)
RETURNS TEXT AS $$
DECLARE
    result TEXT := ' ';
    ch TEXT;
    code INT;
BEGIN
    IF s IS NULL THEN RETURN NULL; END IF;
    FOR i IN 1..char_length(s) LOOP
        ch := substr(s, i, 1);
        code := ascii(ch);
        IF NOT (code BETWEEN 19968 AND 40959) AND
           NOT (code BETWEEN 13312 AND 16383) AND
           NOT (code BETWEEN 63744 AND 64255) AND
           NOT (code BETWEEN 65280 AND 65519) THEN
            result := result || ch;
        END IF;
    END LOOP;
    RETURN trim(regexp_replace(result, '\s+', ' ', 'g'));
END;
$$ LANGUAGE plpgsql IMMUTABLE;

SELECT '=== PREVIEW ===' AS info;
SELECT name_id, thname, phonetic_summary, remove_cjk(phonetic_summary) AS cleaned
FROM names_miracle
WHERE phonetic_summary ~ '[
