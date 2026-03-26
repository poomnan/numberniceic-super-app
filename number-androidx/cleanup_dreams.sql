BEGIN;
UPDATE dreams SET dream_keyword = 'ชฎา' WHERE dream_keyword = 'ซฎา';
UPDATE dreams SET dream_keyword = 'หิ่งห้อย' WHERE dream_keyword = 'ฟิงท้อย';
UPDATE dreams SET dream_keyword = 'ไล่ออก' WHERE dream_keyword = 'ใส่ใจ';
UPDATE dreams SET dream_keyword = 'กฑา' WHERE dream_id = 3;
UPDATE dreams SET dream_keyword = 'โกมุท' WHERE dream_id = 8;
UPDATE dreams SET dream_keyword = 'เกือก' WHERE dream_id = 7;
UPDATE dreams SET dream_keyword = 'กริช' WHERE dream_id = 17;
UPDATE dreams SET dream_keyword = 'โกนุท' WHERE dream_id = 20;
UPDATE dreams SET dream_keyword = 'แซ่' WHERE dream_id = 130;
UPDATE dreams SET dream_keyword = 'อาวุธ' WHERE dream_id = 631;
-- Collision: 'กางเขน (นก)' cleaning to 'กางเขน' which exists. Deleting กางเขน (นก) (ID 2).
DELETE FROM dreams WHERE dream_id = 2;
UPDATE dreams SET dream_keyword = 'กาสาวพัสตร์' WHERE dream_id = 9;
UPDATE dreams SET dream_keyword = 'น้อยหน่า' WHERE dream_id = 286;
-- Collision: 'ราชา (ดูคำว่า 'ราชีวงศ์' อีกบ้าง ก.)' cleaning to 'ราชา' which exists. Deleting ราชา (ดูคำว่า 'ราชีวงศ์' อีกบ้าง ก.) (ID 487).
DELETE FROM dreams WHERE dream_id = 487;
-- Collision: 'ลิงพาย (ดูคำว่า พาย อีกรา พ.)' cleaning to 'ลิงพาย' which exists. Deleting ลิงพาย (ดูคำว่า พาย อีกรา พ.) (ID 505).
DELETE FROM dreams WHERE dream_id = 505;
UPDATE dreams SET dream_keyword = 'วัด' WHERE dream_id = 535;
UPDATE dreams SET dream_keyword = 'ว่าย' WHERE dream_id = 541;
-- Collision: 'ศพ (ดูคำว่า 'ลงศพ' ในอักษร ล.)' cleaning to 'ศพ' which exists. Deleting ศพ (ดูคำว่า 'ลงศพ' ในอักษร ล.) (ID 543).
DELETE FROM dreams WHERE dream_id = 543;
UPDATE dreams SET dream_keyword = 'ศาลพระภูมิ' WHERE dream_id = 545;
UPDATE dreams SET dream_keyword = 'อุจจาระ' WHERE dream_id = 636;
UPDATE dreams SET dream_keyword = 'ฮูก' WHERE dream_id = 640;
UPDATE dreams SET dream_keyword = 'ฮ่องเต้' WHERE dream_id = 641;
UPDATE dreams SET dream_keyword = 'แตน' WHERE dream_id = 201;
COMMIT;
