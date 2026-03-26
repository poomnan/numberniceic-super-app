import psycopg2
import re

# ==========================================
# 1. ตั้งค่าการเชื่อมต่อ PostgreSQL
# ==========================================
DB_HOST = "43.228.85.200"        # IP ของ Server 
DB_NAME = "tayap"                # ชื่อ Database
DB_USER = "tayap"                # Username
DB_PASS = "IntelliP24.X"         # Password
DB_PORT = "5432"                 # Port มาตรฐานของ PostgreSQL

def parse_dream_file(filepath):
    """
    ฟังก์ชันสำหรับอ่านไฟล์และสกัดข้อมูล (Keyword, คำทำนาย, เลขเด็ด)
    """
    with open(filepath, 'r', encoding='utf-8') as f:
        # ตัดบรรทัดว่างทิ้ง เพื่อให้อ่านข้อมูลได้ต่อเนื่อง
        lines = [line.strip() for line in f if line.strip()]
    
    dreams = []
    current_dream = {}
    
    for line in lines:
        # เงื่อนไขที่ 1: ตรวจสอบว่าเป็นบรรทัดตัวเลขหรือไม่ (มีแค่ตัวเลข, ลูกน้ำ และช่องว่าง)
        if re.match(r'^[\d,\s]+$', line):
            current_dream['lucky_numbers'] = line
            dreams.append(current_dream)
            current_dream = {} # เคลียร์ค่าเพื่อเริ่มชุดใหม่
            
        # เงื่อนไขที่ 2: ตรวจสอบว่าเป็นคำทำนายหรือไม่ (มักจะเริ่มด้วยคำว่า ฝัน/หญิง/ชาย หรือมีความยาวมาก)
        elif line.startswith('ฝัน') or line.startswith('หญิง') or line.startswith('ชาย') or len(line) > 30:
            # ใช้ get() เผื่อกรณีที่คำทำนายมีหลายบรรทัดต่อกัน
            current_dream['dream_interpretation'] = current_dream.get('dream_interpretation', '') + ' ' + line
            
        # เงื่อนไขที่ 3: ถ้าไม่ใช่ตัวเลขและไม่ใช่คำทำนาย ให้ถือว่าเป็น Keyword (ชื่อความฝัน)
        else:
            # ถ้ารอบก่อนหน้ามี Keyword และคำทำนายแล้ว แต่ไม่มีเลขเด็ดตามมา ให้บันทึกชุดเก่าก่อนเป็น NULL
            if 'dream_keyword' in current_dream and 'dream_interpretation' in current_dream:
                current_dream['lucky_numbers'] = None
                dreams.append(current_dream)
                current_dream = {}
            
            # เก็บ Keyword ชุดใหม่
            current_dream['dream_keyword'] = line

    # จัดการข้อมูลชุดสุดท้ายที่อาจค้างอยู่ในระบบ (กรณีไฟล์จบบรรทัดที่ไม่มีเลขเด็ด)
    if 'dream_keyword' in current_dream and 'dream_interpretation' in current_dream:
        if 'lucky_numbers' not in current_dream:
            current_dream['lucky_numbers'] = None
        dreams.append(current_dream)
        
    return dreams

def insert_to_postgres(dreams_data):
    """
    ฟังก์ชันสำหรับนำเข้าข้อมูลที่สกัดได้ลงตาราง dreams ใน PostgreSQL
    """
    try:
        # เชื่อมต่อฐานข้อมูล
        conn = psycopg2.connect(
            host=DB_HOST,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASS,
            port=DB_PORT
        )
        cur = conn.cursor()

        # สร้างตารางหากยังไม่มี (ปรับให้ตรงกับ schema จริงที่มี dream_id)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS dreams (
                dream_id SERIAL PRIMARY KEY,
                dream_keyword TEXT NOT NULL UNIQUE,
                dream_interpretation TEXT NOT NULL,
                lucky_numbers TEXT
            );
        """)

        # คำสั่ง INSERT พร้อมจัดการความซ้ำซ้อน (ON CONFLICT)
        insert_query = """
            INSERT INTO dreams (dream_keyword, dream_interpretation, lucky_numbers)
            VALUES (%s, %s, %s)
            ON CONFLICT (dream_keyword) DO UPDATE SET
                dream_interpretation = EXCLUDED.dream_interpretation,
                lucky_numbers = EXCLUDED.lucky_numbers;
        """

        # วนลูป Insert ข้อมูล
        success_count = 0
        for dream in dreams_data:
            keyword = dream.get('dream_keyword')
            interpretation = dream.get('dream_interpretation')
            
            if not keyword or not interpretation:
                continue
                
            cur.execute(insert_query, (
                keyword.strip(),
                interpretation.strip(),
                dream.get('lucky_numbers')
            ))
            success_count += 1

        # บันทึกข้อมูลและปิดการเชื่อมต่อ
        conn.commit()
        cur.close()
        conn.close()

        print(f"✅ บันทึกข้อมูลลงฐานข้อมูลสำเร็จทั้งหมด {success_count} รายการ (รวมรายการที่ Update)")

    except Exception as e:
        print(f"❌ เกิดข้อผิดพลาดในการเชื่อมต่อหรือ Insert ข้อมูล: {e}")

if __name__ == "__main__":
    file_path = "dream.txt"
    print("กำลังอ่านไฟล์และสกัดข้อมูลจัดหมวดหมู่...")
    parsed_data = parse_dream_file(file_path)
    
    print(f"พบข้อมูลพร้อม Insert ทั้งหมด {len(parsed_data)} ชุดความฝัน")
    print("กำลังประมวลผลเข้าสู่ PostgreSQL...")
    insert_to_postgres(parsed_data)
