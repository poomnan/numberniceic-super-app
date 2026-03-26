-- SQL Migration Script for Application Privileges Data
-- This script migrates hardcoded privilege texts from Android app to database

-- First, check if the table exists and create if necessary
CREATE TABLE IF NOT EXISTS application_privileges (
    id INT PRIMARY KEY AUTO_INCREMENT,
    privilege_key VARCHAR(50) NOT NULL UNIQUE,
    privilege_name_th VARCHAR(255) NOT NULL,
    privilege_detail_th TEXT,
    privilege_benefits_th TEXT NOT NULL,
    upgrade_price_th VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert/Update privilege data for NORMAL member
INSERT INTO application_privileges (privilege_key, privilege_name_th, privilege_detail_th, privilege_benefits_th, upgrade_price_th)
VALUES ('NORMAL', 'สมาชิกปกติ', 'สิทธิ์พื้นฐานสำหรับสมาชิกทั่วไป', 
    'จุดประสงค์ในการลงทะเบียนสมาชิก เป็นการระบุตัวตนของคุณ เพื่อให้สามารถใช้งานแอพลิเคชั่นในส่วนของการทำนาย ชื่อเล่น ชื่อจริงนามสกุล บ้านเลขที่ และเบอร์โทรศัพท์ได้ ไม่จำกัดจำนวน และในอนาคตหากคุณต้องการใช้งานในระดับที่สูงขึ้น (VIP) ทั้งการเลือกดูรายการชื่อเล่น ชื่อจริง ที่ดีทั้งเลขศาสตร์พลังเงา ซึ่งเราได้เก็บรวบรวมไว้ให้เลือกใช้นับพันรายการ พร้อมใช้ยืนยันเพื่อรับสิทธิประโยชน์ที่เรามีให้ในอนาคต โดยระบบจะไม่มีการเก็บข้อมูลอื่นใดอีก และไม่จำเป็นต้องลงทะเบียนสมัครสมาชิกใหม่อีกครั้ง',
    'ฟรี'
)
ON DUPLICATE KEY UPDATE 
    privilege_name_th = VALUES(privilege_name_th),
    privilege_detail_th = VALUES(privilege_detail_th),
    privilege_benefits_th = VALUES(privilege_benefits_th),
    upgrade_price_th = VALUES(upgrade_price_th),
    updated_at = CURRENT_TIMESTAMP;

-- Insert/Update privilege data for SILVER member (SPECIALP)
INSERT INTO application_privileges (privilege_key, privilege_name_th, privilege_detail_th, privilege_benefits_th, upgrade_price_th)
VALUES ('SPECIALP', 'สมาชิก Silver', 'สิทธิ์พิเศษสำหรับสมาชิก Silver', 
    'มีสิทธิ์เหมือนสมาชิกปรกติ แต่มีสิทธิพิเศษเพิ่มขึ้นดังนี้',
    'ราคาพิเศษ'
)
ON DUPLICATE KEY UPDATE 
    privilege_name_th = VALUES(privilege_name_th),
    privilege_detail_th = VALUES(privilege_detail_th),
    privilege_benefits_th = VALUES(privilege_benefits_th),
    upgrade_price_th = VALUES(upgrade_price_th),
    updated_at = CURRENT_TIMESTAMP;

-- Insert/Update privilege data for GOLD member
INSERT INTO application_privileges (privilege_key, privilege_name_th, privilege_detail_th, privilege_benefits_th, upgrade_price_th)
VALUES ('GOLD', 'สมาชิก Gold', 'สิทธิ์พิเศษสำหรับสมาชิก Gold', 
    'VIP Gold สามารถเลือกดูรายการ ชื่อเล่น และชื่อจริง ที่ได้เลขดีทั้งเลขศาสตร์พลังเงา เพื่อนำไปใช้ในการตั้งชื่อให้ตัวเองและครอบครัวได้ตลอดชีวิต โดยเราจะเปิดดวงเจาะเลขเพื่อหาอักษรนำหน้าชื่อเล่นชื่อสกุลที่ดีที่สุดเพื่อให้คุณนำไปเลือกตั้งชื่อให้ฟรี 1 ครั้ง คือ เจ้าของสิทธิ์นี้เท่านั้น',
    '24,599 บาท'
)
ON DUPLICATE KEY UPDATE 
    privilege_name_th = VALUES(privilege_name_th),
    privilege_detail_th = VALUES(privilege_detail_th),
    privilege_benefits_th = VALUES(privilege_benefits_th),
    upgrade_price_th = VALUES(upgrade_price_th),
    updated_at = CURRENT_TIMESTAMP;

-- Insert/Update privilege data for DIAMOND member  
INSERT INTO application_privileges (privilege_key, privilege_name_th, privilege_detail_th, privilege_benefits_th, upgrade_price_th)
VALUES ('DIAMOND', 'สมาชิก Diamond', 'สิทธิ์พิเศษสูงสุดสำหรับสมาชิก Diamond', 
    'VIP Diamond สามารถดูรายการ ชื่อเล่น และชื่อจริงที่ดีทั้งเลขศาสตร์และพลังเงานับพันรายการ เพื่อนำไปใช้ในการตั้งชื่อของตัวเองและครอบครัว โดยเราจะเปิดดวงเจาะเลขเพื่อหาอักษรนำหน้าชื่อเล่นชื่อสกุลที่ดีที่สุดเพื่อให้คุณนำไปเลือกตั้งชื่อให้ฟรี 2 ครั้ง คือ เจ้าของสิทธิ์นี้และคนที่เจ้าของสิทธิ์เลือกมาเท่านั้นและ ***คุณยังสามารถใช้แอพพลิเคชั่นในการทำนายทะเบียนรถได้ไม่จำกัดจำนวนตลอดชีวิต',
    '26,599 บาท'
)
ON DUPLICATE KEY UPDATE 
    privilege_name_th = VALUES(privilege_name_th),
    privilege_detail_th = VALUES(privilege_detail_th),
    privilege_benefits_th = VALUES(privilege_benefits_th),
    upgrade_price_th = VALUES(upgrade_price_th),
    updated_at = CURRENT_TIMESTAMP;

-- Add additional privilege types if needed
INSERT INTO application_privileges (privilege_key, privilege_name_th, privilege_detail_th, privilege_benefits_th, upgrade_price_th)
VALUES 
('SPECIAL', 'สิทธิ์พิเศษ', 'สิทธิ์พิเศษเพิ่มเติม', 'มีสิทธิ์สามารถดูได้ และทำได้ทุกอย่างเหมือน SpecialP แต่มีสิทธิพิเศษเพิ่มขึ้นดังนี้', 'ราคาตามแพ็คเกจ'),
('VIP', 'สมาชิก VIP', 'สิทธิ์สมาชิก VIP', 'สามารถทำได้ทุกอย่างเหมือนสมาชิกปรกติ แต่มีสิทธิพิเศษเพิ่มขึ้นดังนี้', 'เริ่มต้น 20,000 บาท')
ON DUPLICATE KEY UPDATE 
    privilege_name_th = VALUES(privilege_name_th),
    privilege_detail_th = VALUES(privilege_detail_th),
    privilege_benefits_th = VALUES(privilege_benefits_th),
    upgrade_price_th = VALUES(upgrade_price_th),
    updated_at = CURRENT_TIMESTAMP;

-- Display the migrated data
SELECT * FROM application_privileges ORDER BY id ASC;