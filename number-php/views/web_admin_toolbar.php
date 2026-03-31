<div
    style="background: #fff; padding: 15px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); display: flex; gap: 10px; flex-wrap: wrap; align-items: center; justify-content: flex-end;">
    <span style="margin-right: auto; font-weight: bold; color: #555;">🛠️ Admin Tools:</span>

    <a href="/web/admin/users" class="btn"
        style="background: #6c757d; color: white; text-decoration: none; padding: 8px 15px; border-radius: 4px;">
        <i class="fa-solid fa-users"></i> ผู้ใช้ทั้งหมด
    </a>

    <a href="/admin/notifications/custom" class="btn"
        style="background: #28a745; color: white; text-decoration: none; padding: 8px 15px; border-radius: 4px;">
        <i class="fa-solid fa-bullhorn"></i> ส่งข้อความพิเศษ
    </a>

    <a href="/admin/notifications/send-bag-colors" class="btn"
        style="background: #17a2b8; color: white; text-decoration: none; padding: 8px 15px; border-radius: 4px;">
        <i class="fa-solid fa-palette"></i> จัดการสีกระเป๋า
    </a>

    <a href="/web/admin/articles" class="btn"
        style="background: #007bff; color: white; text-decoration: none; padding: 8px 15px; border-radius: 4px;">
        <i class="fa-solid fa-newspaper"></i> บทความ
    </a>

    <a href="/web/admin/rengyam-tags" class="btn"
        style="background: #0ea5e9; color: white; text-decoration: none; padding: 8px 15px; border-radius: 4px;">
        <i class="fa-solid fa-book-open"></i> คำอธิบายฤกษ์ยาม
    </a>

    <a href="/cron/wanpra?force=test" target="_blank" class="btn"
        style="background: #fd7e14; color: white; text-decoration: none; padding: 8px 15px; border-radius: 4px;"
        onclick="return confirm('⚠️ ยืนยันทดสอบส่งแจ้งเตือนวันพระ? \n(ข้อความจะถูกส่งหาทุกคนที่มี App)');">
        <i class="fa-solid fa-bell"></i> ทดสอบวันพระ
    </a>
</div>
