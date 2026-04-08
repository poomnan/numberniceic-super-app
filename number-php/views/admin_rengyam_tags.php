<!DOCTYPE html>
<html lang="th">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>จัดการความหมายฤกษ์ยาม - Dashboard</title>
    <style>
        :root {
            --primary: #198754;
            --secondary: #6c757d;
            --danger: #dc3545;
            --light: #f8f9fa;
        }
        body { font-family: 'Sarabun', sans-serif; background: #f0f2f5; margin: 0; padding: 20px; color: #333; }
        .container { max-width: 1000px; margin: auto; background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
        h1 { color: var(--primary); margin-bottom: 20px; display: flex; align-items: center; gap: 10px; font-size: 1.5rem; }
        .back-btn { display: inline-block; margin-bottom: 15px; color: var(--secondary); text-decoration: none; font-size: 0.9rem; }
        
        /* Card-based list styling */
        .tag-list { margin-top: 20px; display: grid; grid-template-columns: 1fr; gap: 15px; }
        .tag-card { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 15px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .tag-card-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
        .tag-card-header strong { font-size: 1.1rem; color: #111; }
        .expand-icon { color: var(--primary); font-size: 0.9rem; font-weight: 600; }
        .source-label-text { font-size: 0.75rem; background: #eef7f3; color: var(--primary); padding: 3px 8px; border-radius: 20px; font-weight: 600; }
        
        .tag-card-editor { border-top: 1px solid #f0f0f0; margin-top: 15px; padding-top: 15px; }
        .tag-card-body { margin-bottom: 20px; }
        .tag-card-actions { display: flex; gap: 10px; align-items: center; }
        .inline-feedback { margin-top: 12px; font-size: 0.95rem; font-weight: 600; display: none; }
        .inline-feedback.success { color: var(--primary); display: block; }
        .inline-feedback.error { color: var(--danger); display: block; }
        
        .edit-input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-family: inherit; box-sizing: border-box; font-size: 1rem; }
        .btn { padding: 12px 20px; border-radius: 8px; cursor: pointer; border: none; font-weight: 600; transition: 0.2s; font-size: 1rem; text-align: center; }
        .btn-save { background: var(--primary); color: white; flex: 2; }
        .btn-save:hover { background: #157347; }
        .btn-delete { background: white; color: var(--danger); border: 1px solid var(--danger); flex: 1; font-size: 0.9rem; }
        .btn-delete:hover { background: #fff5f5; }
        
        .form-add { margin-bottom: 30px; padding: 25px; background: #fff; border-radius: 12px; border: 2px solid var(--primary); }
        .form-grid { display: grid; grid-template-columns: 1fr; gap: 15px; }
        label { display: block; margin-bottom: 8px; font-size: 0.95rem; font-weight: 600; color: #444; }

        @media (min-width: 768px) {
            .container { padding: 40px; }
            h1 { font-size: 2.2rem; }
            .tag-list { grid-template-columns: repeat(auto-fill, minmax(450px, 1fr)); }
            .form-grid { grid-template-columns: 1fr 1fr 2fr auto; align-items: end; }
        }
    </style>
    <script>
        function toggleEdit(id) {
            const editor = document.getElementById('editor-' + id);
            const isHidden = editor.style.display === 'none';
            // Optional: Close others
            // document.querySelectorAll('.tag-card-editor').forEach(el => el.style.display = 'none');
            editor.style.display = isHidden ? 'block' : 'none';
        }
    </script>
    <link href="https://fonts.googleapis.com/css2?family=Sarabun:wght@300;400;600&display=swap" rel="stylesheet">
</head>
<body>

<div class="container">
    <a href="/web/dashboard" class="back-btn">← กลับไปยัง Dashboard หลัก</a>
    <h1>🏷️ จัดการคำอธิบายฤกษ์ยาม (RengYam Tags)</h1>

    <div class="form-add">
        <h3>เพิ่มรายการใหม่</h3>
        <div class="form-grid">
            <div>
                <label>ชื่อ Tag (เช่น วันธงชัย)</label>
                <input type="text" id="new-tag" class="edit-input" placeholder="ป้อนชื่อ Tag">
            </div>
            <div>
                <label>ประเภท (Label)</label>
                <input type="text" id="new-source" class="edit-input" placeholder="เช่น กาลโยค">
            </div>
            <div>
                <label>คำอธิบายความหมาย</label>
                <input type="text" id="new-desc" class="edit-input" placeholder="ใส่ความหมายที่ถูกต้องที่นี่">
            </div>
            <button class="btn btn-save" onclick="saveTag('', true)">เพิ่ม</button>
        </div>
    </div>

    <div class="tag-list">
        <?php foreach ($tags as $tag): ?>
        <div class="tag-card" id="row-<?php echo md5($tag['tag_name']); ?>">
            <div class="tag-card-header" onclick="toggleEdit('<?php echo md5($tag['tag_name']); ?>')">
                <div style="display: flex; align-items: center; gap: 10px;">
                    <strong style="color: #333;"><?php echo htmlspecialchars($tag['tag_name']); ?></strong>
                    <span class="source-label-text"><?php echo htmlspecialchars($tag['source_label']); ?></span>
                </div>
                <span class="expand-icon">แก้ไข ▾</span>
            </div>
            
            <div class="tag-card-editor" id="editor-<?php echo md5($tag['tag_name']); ?>" style="display: none;">
                <div class="tag-card-body">
                    <div class="form-group">
                        <label>ประเภท (Label)</label>
                        <input type="text" class="edit-input source-field" value="<?php echo htmlspecialchars($tag['source_label']); ?>">
                    </div>
                    <div class="form-group" style="margin-top: 15px;">
                        <label>คำอธิบายความหมาย</label>
                        <textarea class="edit-input desc-field" rows="4"><?php echo htmlspecialchars($tag['short_description']); ?></textarea>
                    </div>
                </div>
                
                <div class="tag-card-actions">
                    <button class="btn btn-save" type="button" onclick="saveTag(this, '<?php echo addslashes($tag['tag_name']); ?>')">บันทึกที่แก้ไข</button>
                    <button class="btn btn-delete" type="button" onclick="deleteTag('<?php echo addslashes($tag['tag_name']); ?>', '<?php echo addslashes($tag['source_label']); ?>')">ลบ</button>
                </div>
                <div class="inline-feedback" aria-live="polite"></div>
            </div>
        </div>
        <?php endforeach; ?>
    </div>
</div>

<script>
function setInlineFeedback(container, message, type) {
    if (!container) return;
    container.textContent = message;
    container.className = 'inline-feedback ' + type;
}

async function saveTag(buttonOrName, name = '', isNew = false) {
    const button = isNew ? null : buttonOrName;
    const tagName = isNew ? document.getElementById('new-tag').value.trim() : name;
    const editor = isNew ? null : button.closest('.tag-card-editor');
    const feedback = isNew ? null : editor.querySelector('.inline-feedback');
    const sourceLabel = isNew
        ? document.getElementById('new-source').value.trim()
        : editor.querySelector('.source-field').value.trim();
    const description = isNew
        ? document.getElementById('new-desc').value.trim()
        : editor.querySelector('.desc-field').value.trim();

    if (!tagName) return alert('กรุณาระบุชื่อ Tag');
    if (!sourceLabel) return alert('กรุณาระบุประเภท (Label)');
    if (!description) return alert('กรุณาระบุคำอธิบายความหมาย');

    try {
        if (button) button.disabled = true;
        if (feedback) setInlineFeedback(feedback, 'กำลังบันทึก...', 'success');

        const res = await fetch('/web/admin/rengyam-tags/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                'tag_name': tagName,
                'source_label': sourceLabel,
                'short_description': description,
                'is_active': 1
            })
        });
        const result = await res.json();
        if (result.status === 'success') {
            if (isNew) {
                alert('บันทึกสำเร็จ');
                location.reload();
                return;
            }
            setInlineFeedback(feedback, 'บันทึกที่แก้ไขเรียบร้อยแล้ว', 'success');
        } else {
            if (isNew) {
                alert(result.message || 'เกิดข้อผิดพลาด');
            } else {
                setInlineFeedback(feedback, result.message || 'เกิดข้อผิดพลาด', 'error');
            }
        }
    } catch (e) {
        if (isNew) {
            alert('Error communicating with server');
        } else {
            setInlineFeedback(feedback, 'เชื่อมต่อเซิร์ฟเวอร์ไม่สำเร็จ', 'error');
        }
    } finally {
        if (button) button.disabled = false;
    }
}

async function deleteTag(name, sourceLabel) {
    const message = `ยืนยันการลบ "${name}"${sourceLabel ? ' (' + sourceLabel + ')' : ''} ?\n\nการลบนี้ไม่สามารถย้อนกลับได้`;
    if (!confirm(message)) return;

    try {
        const res = await fetch('/web/admin/rengyam-tags/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ 'tag_name': name })
        });
        const result = await res.json();
        if (result.status === 'success') {
            location.reload();
        } else {
            alert('ลบไม่สำเร็จ');
        }
    } catch (e) {
        alert('Error');
    }
}
</script>

</body>
</html>
