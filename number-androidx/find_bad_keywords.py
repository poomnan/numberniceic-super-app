import csv
import sys

try:
    with open('dreams_dump.csv', 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        try:
            next(reader) # skip header
        except StopIteration:
            pass
        
        for row in reader:
            if len(row) < 3: continue
            keyword = row[1].strip()
            interpretation = row[2].strip()
            
            # Basic check: is keyword in interpretation?
            if keyword not in interpretation:
                print(f"{row[0]},{keyword},{interpretation[:70]}")
except FileNotFoundError:
    print("File not found")
