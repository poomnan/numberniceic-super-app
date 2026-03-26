import csv
import difflib

# 1. Read all audit data
data = []
try:
    with open('dreams_audit.csv', 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        try: next(reader)
        except: pass
        for row in reader:
            if len(row) >= 3:
                data.append({'id': row[0], 'kw': row[1].strip(), 'interp': row[2].strip()})
except:
    print("Error reading CSV")
    exit()

suspicious = []

for item in data:
    kw = item['kw']
    interp = item['interp']
    
    # Direct match check
    if kw in interp:
        continue
        
    # Check for near matches in interpretation words
    # Split interpretation into words (crude space split for now, Thai needs specialized tokenizer but let's try sliding window or simple approach)
    # Actually, difflib on the whole string might not work well.
    # Let's just flag it if not found.
    
    # Check if interpretation is short/empty
    if len(interp) < 10:
        suspicious.append((item['id'], kw, "Content Issues", interp))
        continue
        
    # Check for partial overlaps or typos
    # e.g. "ต๊กกะแตน" vs "ตั๊กแตน"
    # Inspect visually or list them.
    suspicious.append((item['id'], kw, "Not found in Interp", interp[:60]))

# Create Report
with open('suspicious_report.csv', 'w', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['ID', 'Keyword', 'Reason', 'Snippet'])
    for s in suspicious:
        writer.writerow(s)
        
print(f"Generated report with {len(suspicious)} items.")
