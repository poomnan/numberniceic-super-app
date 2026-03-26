-- Fix 623 หิ่งห้อย (Garbled text)
UPDATE dreams SET dream_interpretation = 'ฝันเห็นหิ่งห้อย หรือหิ่งห้อยตัวเดียว ทายว่า สิ่งที่คิดไว้จะสมหวัง การงานจะก้าวหน้า หรือได้รับข่าวดีจากทางไกล' WHERE dream_id = 623;

-- Fix 643 กิ้งก่า (Missing prefix)
UPDATE dreams SET dream_interpretation = 'ฝันเห็นกิ้งก่า เป็นความฝันดี ผู้ฝันจะได้ลาภผลอย่างรวดเร็ว' WHERE dream_id = 643;

-- Fix 645 กษัตริย์ (Missing prefix and typo)
UPDATE dreams SET dream_interpretation = 'ฝันเห็นกษัตริย์ หรือองค์เจ้าแผ่นดินหรือพระราชินี ท่านจะได้เลื่อนตำแหน่งยศทางการ หรือในตำแหน่งหน้าที่การงานทั่วไป หรืออาจมีโชคลาภในการเสี่ยงโชคลาภลอย ได้รับข่าวดีจากญาติผู้ใหญ่' WHERE dream_id = 645;

-- Fix 636 อุจจาระ (Direct copy from 26 ขี้)
UPDATE dreams SET dream_interpretation = 'ฝันเห็นอุจจาระหรือขี้ หรือได้จับขี้ หรือตัวเองถ่ายอุจจาระ ทายว่า ผู้นั้นจะมีเคราะห์ จะมีเรื่องเสื่อมเสียในทางชื่อเสียง หรือถูกคนนินทากล่าวร้ายป้ายสี' WHERE dream_id = 636;

-- Fix 619 เหรียญ (Standard interpetation)
UPDATE dreams SET dream_interpretation = 'ฝันเห็นเหรียญ หรือเงินทอง ทายว่า จะได้รับโชคลาภ หรือมีเรื่องดีในการเสี่ยงโชค' WHERE dream_id = 619;

-- Fix 631 อาวุธ (Standard interpretation)
UPDATE dreams SET dream_interpretation = 'ฝันเห็นอาวุธ ของมีคม ทายว่า จะมีเคราะห์ หรือมีศัตรูปองร้าย แต่จะผ่านพ้นไปได้' WHERE dream_id = 631;

-- Clean up any remaining redirects (Optional, safer to check first)
-- UPDATE dreams SET dream_interpretation = REPLACE(dream_interpretation, '(ดูคำว่า', 'ฝันเห็น') WHERE dream_interpretation LIKE '%(ดูคำว่า%';
