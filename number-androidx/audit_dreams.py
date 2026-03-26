import csv
import re

def is_thai(text):
    return any("\u0E00" <= char <= "\u0E7F" for char in text)

suspicious_records = []

try:
    with open('dreams_audit.csv', 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        try:
            next(reader) # skip header
        except StopIteration:
            pass
            
        for row in reader:
            if len(row) < 3: continue
            
            dream_id = row[0]
            keyword = row[1].strip()
            interpretation = row[2].strip()
            
            # Check 1: Is keyword in interpretation?
            # Many correct entries might fail this simple check (synonyms), but it's a good flag.
            keyword_found = keyword in interpretation
            
            # Check 2: Does interpretation start with "ฝันเห็น" followed by something else?
            first_sentence = interpretation.split(' ')[0] # crude
            
            # Check 3: Uncommon trailing characters in keyword?
            # Like the 'ย' in 'เสื้อย'.
            # Or ending with vowels responsible for tone marks only but misplaced?
            uncommon_ending = False
            # Check specifically for "yy" or repeated trailing chars? No specific pattern known yet.
            
            # Check 4: Keyword looks like a fragment?
            # e.g. "เสื้อย" vs "เสื้อ"
            
            if not keyword_found:
                 suspicious_records.append({
                    "id": dream_id,
                    "keyword": keyword,
                    "reason": "Keyword not in interpretation",
                    "interpretation_snippet": interpretation[:60]
                })

    # Print results
    print(f"Found {len(suspicious_records)} potentially suspicious records based on missing keyword match.")
    for rec in suspicious_records:
        print(f"ID: {rec['id']}, Keyword: '{rec['keyword']}', Reason: {rec['reason']}")
        print(f"   Interpretation: {rec['interpretation_snippet']}...")
        print("-" * 40)

except FileNotFoundError:
    print("dreams_audit.csv not found")
