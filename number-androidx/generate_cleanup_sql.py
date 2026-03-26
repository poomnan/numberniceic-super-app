import csv
import re

# We need the full list to check for duplicates
# First, let's allow the user to provide the list file.
# We'll assume dreams_dump.csv exists (we created it).

def clean_keyword(k):
    # Remove text in parentheses
    k = re.sub(r'\(.*?\)', '', k)
    # Remove text after comma or slash
    if ',' in k:
        k = k.split(',')[0]
    if '/' in k:
        k = k.split('/')[0]
    return k.strip()

existing_keywords = {}
rows_to_process = []

try:
    with open('dreams_dump.csv', 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        try:
            next(reader)
        except StopIteration:
            pass
        for row in reader:
            if len(row) < 2: continue
            id = row[0]
            kw = row[1].strip()
            # interpretation = row[2] # needed?
            rows_to_process.append((id, kw))
            existing_keywords[kw] = id

    with open('cleanup_dreams.sql', 'w', encoding='utf-8') as out:
        out.write("BEGIN;\n")
        
        # Specific Typo Fixes (Manual)
        # Check if target exists first? No, likely not.
        manual_fixes = {
            'ซฎา': 'ชฎา',
            'ฟิงท้อย': 'หิ่งห้อย',
            'ใส่ใจ': 'ไล่ออก' # Based on interpretation found earlier
        }
        
        for bad, good in manual_fixes.items():
            if bad in existing_keywords:
                # If good keyword ALREADY exists, we delete the bad row to avoid unique violation
                if good in existing_keywords and existing_keywords[good] != existing_keywords[bad]:
                    out.write(f"-- Merging duplicate (typo fix) {bad} -> {good}\n")
                    bad_id = existing_keywords[bad]
                    good_id = existing_keywords[good]
                    out.write(f"DELETE FROM dreams WHERE dream_id = {bad_id};\n")
                    del existing_keywords[bad]
                else:
                    out.write(f"UPDATE dreams SET dream_keyword = '{good}' WHERE dream_keyword = '{bad}';\n")
                    # Update map
                    if good not in existing_keywords:
                        existing_keywords[good] = existing_keywords[bad]
                    del existing_keywords[bad]

        # General Cleanup
        for id, kw in rows_to_process:
            if kw not in existing_keywords: continue # Already handled
            
            cleaned = clean_keyword(kw)
            
            if cleaned != kw and cleaned:
                # Check for collision
                if cleaned in existing_keywords and existing_keywords[cleaned] != id:
                    # Collision! 'แตน, ต่อ' -> 'แตน' (which exists)
                    out.write(f"-- Collision: '{kw}' cleaning to '{cleaned}' which exists. Deleting {kw} (ID {id}).\n")
                    out.write(f"DELETE FROM dreams WHERE dream_id = {id};\n")
                    del existing_keywords[kw]
                else:
                    # Safe to update
                    out.write(f"UPDATE dreams SET dream_keyword = '{cleaned}' WHERE dream_id = {id};\n")
                    existing_keywords[cleaned] = id
                    del existing_keywords[kw]

        out.write("COMMIT;\n")
        
except FileNotFoundError:
    print("dreams_dump.csv not found")
