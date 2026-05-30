SELECT '=== CJK count in phonetic_summary ===' AS info;
SELECT COUNT(*) AS cjk_rows FROM names_miracle 
WHERE phonetic_summary ~ '[' || chr(19968) || chr(45) || chr(40959) || ']';

SELECT '=== Sample CJK rows ===' AS info;
SELECT name_id, thname, substring(phonetic_summary, 1, 80) AS summary_snippet
FROM names_miracle 
WHERE phonetic_summary ~ '[' || chr(19968) || chr(45) || chr(40959) || ']'
LIMIT 5;
