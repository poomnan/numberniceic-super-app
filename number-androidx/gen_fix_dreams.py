import csv
import re

# Load data
dreams = {}
redirects = []

try:
    with open('dreams_audit.csv', 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        try: next(reader)
        except: pass
        for row in reader:
            if len(row) >= 3:
                did = row[0]
                kw = row[1].strip()
                interp = row[2].strip()
                dreams[kw] = {'id': did, 'interp': interp}
                
                # Check for redirect pattern: (ดูคำว่า 'KEY' ...)
                match = re.search(r"ดูคำว่า\s*['\"](.*?)['\"]", interp)
                if match:
                    target_kw = match.group(1)
                    redirects.append({'id': did, 'kw': kw, 'target': target_kw})
                elif "ดูคำว่า" in interp:
                     # Loose match
                     match_loose = re.search(r"ดูคำว่า\s*([ก-๙]+)", interp)
                     if match_loose:
                         redirects.append({'id': did, 'kw': kw, 'target': match_loose.group(1)})

    with open('fix_dreams_entries.sql', 'w', encoding='utf-8') as out:
        out.write("BEGIN;\n")
        
        # 1. Fix Redirects
        for r in redirects:
            target = r['target']
            if target in dreams:
                target_interp = dreams[target]['interp']
                # Clean up target interp to include the current keyword if needed
                # e.g. "ฝันเห็นเงินทอง..." -> "ฝันเห็นเหรียญ หรือเงินทอง..."
                new_interp = target_interp.replace(f"ฝันเห็น{target}", f"ฝันเห็น{r['kw']} หรือ{target}")
                
                # Generic fallback if pattern doesn't match
                if new_interp == target_interp:
                     new_interp = f"ฝันเห็น{r['kw']} หรือ{target} {target_interp}"
                
                out.write(f"-- Redirect Fix: {r['kw']} -> {target}\n")
                out.write(f"UPDATE dreams SET dream_interpretation = '{new_interp}' WHERE dream_id = {r['id']};\n")
            else:
                out.write(f"-- Target '{target}' not found for redirect '{r['kw']}' (ID {r['id']})\n")

        # 2. Manual Fixes (Data Quality)
        
        # 623 หิ่งห้อย
        out.write("-- Fix 623 หิ่งห้อย (Garbled text)\n")
        out.write("UPDATE dreams SET dream_interpretation = 'ฝันเห็นหิ่งห้อย หรือหิ่งห้อยตัวเดียว ทายว่า สิ่งที่คิดไว้จะสมหวัง การงานจะก้าวหน้า หรือได้รับข่าวดีจากทางไกล' WHERE dream_id = 623;\n")
        
        # 643 กิ้งก่า (Missing context)
        if 'กิ้งก่า' in dreams:
            curr = dreams['กิ้งก่า']['interp']
            if "ฝันเห็น" not in curr:
                 out.write("-- Fix 643 กิ้งก่า (Missing prefix)\n")
                 out.write(f"UPDATE dreams SET dream_interpretation = 'ฝันเห็นกิ้งก่า {curr}' WHERE dream_id = 643;\n")

        # 645 กษัตริย์
        if 'กษัตริย์' in dreams:
            curr = dreams['กษัตริย์']['interp']
            if "ฝันเห็น" in curr and "กษัตริย์" not in curr:
                 # Insert keyword after "ฝันเห็น"
                 new_i = curr.replace("ฝันเห็น", "ฝันเห็นกษัตริย์ หรือ")
                 out.write("-- Fix 645 กษัตริย์\n")
                 out.write(f"UPDATE dreams SET dream_interpretation = '{new_i}' WHERE dream_id = 645;\n")

        out.write("COMMIT;\n")

except Exception as e:
    print(e)
