-- Super Fast SQL Migration
-- 1. Create a dictionary table for letter values
CREATE TEMP TABLE letter_values (
    letter TEXT PRIMARY KEY,
    sat_val INT,
    sha_val INT
);

INSERT INTO letter_values (letter, sat_val, sha_val) VALUES
('ก', 1, 10), ('ด', 1, 10), ('ถ', 1, 10), ('ท', 1, 10), ('ภ', 1, 10), ('ฤ', 1, 10), ('่', 1, 10), ('ุ', 1, 10), ('า', 1, 10), ('ำ', 1, 10),
('ข', 2, 20), ('ช', 2, 20), ('ง', 2, 15), ('บ', 2, 11), ('ป', 2, 11), ('เ', 2, 0), ('แ', 2, 0), ('้', 2, 11), ('ู', 2, 10),
('ฆ', 3, 30), ('ฑ', 3, 30), ('ฒ', 3, 30), ('ต', 3, 30), ('อ', 6, 6), -- 'อ' corrected
('ค', 4, 40), ('ธ', 4, 40), ('ร', 4, 40), ('ญ', 4, 40), ('ษ', 4, 40), ('โ', 4, 0), ('ะ', 4, 0), ('ั', 4, 0), ('ิ', 4, 0),
('ฉ', 5, 50), ('ฌ', 5, 50), ('ณ', 5, 50), ('น', 5, 10), ('ม', 5, 10), ('ห', 5, 50), ('ฮ', 5, 50), ('ฎ', 5, 50), ('ฬ', 5, 50), ('ึ', 5, 0),
('จ', 6, 60), ('ล', 6, 60), ('ว', 6, 60), ('ใ', 6, 0),
('ซ', 7, 70), ('ศ', 7, 70), ('ส', 7, 21), ('ี', 7, 0), ('ื', 7, 0),
('ย', 8, 30), ('ผ', 8, 80), ('ฝ', 8, 80), ('พ', 8, 80), ('ฟ', 8, 80), ('็', 8, 0),
('ฏ', 9, 90), ('ฐ', 9, 90), ('ไ', 9, 0), ('์', 9, 0)
ON CONFLICT (letter) DO UPDATE SET sat_val = EXCLUDED.sat_val, sha_val = EXCLUDED.sha_val;

-- Correcting specific values based on Go logic (Manual Override if duplicate keys existed in insert above)
-- อ = 6, 6
-- ส = 7, 21
-- น = 5, 10
-- ม = 5, 10

-- 2. Create Calculation Function
CREATE OR REPLACE FUNCTION calc_name_sums(name_text TEXT) 
RETURNS TABLE (calc_sat INT, calc_sha INT) AS $$
DECLARE
    s_sat INT := 0;
    s_sha INT := 0;
    char_text TEXT;
BEGIN
    -- Iterate over each character
    FOR i IN 1..length(name_text) LOOP
        char_text := substr(name_text, i, 1);
        -- Sum up values from temp table
        -- We use COALESCE(..., 0) to skip unknown chars
        SELECT COALESCE(sat_val, 0) + s_sat, COALESCE(sha_val, 0) + s_sha
        INTO s_sat, s_sha
        FROM letter_values 
        WHERE letter = char_text;
    END LOOP;
    
    RETURN QUERY SELECT s_sat, s_sha;
END;
$$ LANGUAGE plpgsql;

-- 3. Execute Mass Update (The Fast Part - Batched)
DO $$
DECLARE 
    rows_updated INT;
BEGIN
    LOOP
        WITH calculated AS (
            SELECT 
                name_id,
                (SELECT SUM(COALESCE(lv.sat_val, 0)) 
                 FROM regexp_split_to_table(nm.thname, '') AS c 
                 JOIN letter_values lv ON lv.letter = c) as new_sat,
                (SELECT SUM(COALESCE(lv.sha_val, 0)) 
                 FROM regexp_split_to_table(nm.thname, '') AS c 
                 JOIN letter_values lv ON lv.letter = c) as new_sha
            FROM names_miracle nm
            WHERE sat_sum = 0 OR sat_sum IS NULL
            LIMIT 10000 -- Process 10k at a time
        )
        UPDATE names_miracle
        SET 
            sat_sum = COALESCE(c.new_sat, 0),
            sha_sum = COALESCE(c.new_sha, 0)
        FROM calculated c
        WHERE names_miracle.name_id = c.name_id;

        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        RAISE NOTICE 'Updated % rows...', rows_updated;

        EXIT WHEN rows_updated = 0;
        
        -- Optional: Sleep if needed to let other queries breathe (not needed here usually)
        -- PERFORM pg_sleep(0.1);
    END LOOP;
END $$;

-- 4. Cleanup
DROP TABLE letter_values;
DROP FUNCTION calc_name_sums;
