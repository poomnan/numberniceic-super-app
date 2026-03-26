<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>คลังรูปภาพ - Admin Panel</title>
    <link href="https://fonts.googleapis.com/css2?family=Sarabun:wght@300;400;500;600;700&display=swap"
        rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        :root {
            --primary: #198754;
            --accent: #FFD700;
            --bg: #f8f9fa;
        }

        body {
            font-family: 'Sarabun', sans-serif;
            background-color: var(--bg);
        }

        .container {
            margin-top: 2rem;
            margin-bottom: 4rem;
        }

        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 2rem;
            background: white;
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
        }

        .media-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
            gap: 20px;
        }

        .media-item {
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            transition: all 0.2s;
            position: relative;
        }

        .media-item:hover {
            transform: translateY(-5px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }

        .media-preview {
            width: 100%;
            height: 160px;
            object-fit: cover;
            cursor: pointer;
            background: #eee;
        }

        .media-info {
            padding: 12px;
        }

        .media-name {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            font-size: 0.9rem;
            color: #555;
            margin-bottom: 10px;
        }

        .media-actions {
            display: flex;
            gap: 8px;
        }

        .btn-copy {
            background: #198754;
            color: white;
        }

        .btn-delete {
            background: #dc3545;
            color: white;
        }

        .upload-area {
            background: white;
            border: 2px dashed #ccc;
            border-radius: 12px;
            padding: 40px;
            text-align: center;
            margin-bottom: 2rem;
            cursor: pointer;
            transition: 0.2s;
        }

        .upload-area:hover {
            border-color: var(--primary);
            background: #f1f8f5;
        }

        .upload-area i {
            font-size: 3rem;
            color: #ccc;
            margin-bottom: 1rem;
        }

        #fileInput {
            display: none;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="container">
        <?php $isPicker = isset($_GET['picker']) && $_GET['picker'] == '1'; ?>

        <div class="header">
            <h2 class="mb-0"><i class="fas fa-images text-success"></i> <?php echo $isPicker ? 'เลือกรูปภาพ' : 'คลังรูปภาพ (Gallery)'; ?></h2>
            <?php if (!$isPicker): ?>
                <a href="/web/dashboard" class="btn btn-outline-secondary btn-sm">กลับหน้าหลัก</a>
            <?php else: ?>
                <button type="button" class="btn btn-outline-secondary btn-sm" onclick="window.close()">ปิดหน้าต่าง</button>
            <?php endif; ?>
        </div>

        <div class="upload-area" onclick="document.getElementById('fileInput').click()">
            <i class="fas fa-cloud-upload-alt"></i>
            <h4>คลิกเพื่ออัปโหลดรูปภาพใหม่</h4>
            <p class="text-muted">JPG, PNG, WEBP หรือ GIF (แนะนำขนาดไม่เกิน 5MB)</p>
            <input type="file" id="fileInput" accept="image/*" onchange="uploadFile(this)">
        </div>

        <div id="mediaGrid" class="media-grid">
            <p class="text-center w-100 py-5 text-muted">กำลังโหลดรูปภาพ...</p>
        </div>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', loadMedia);

        async function loadMedia() {
            try {
                const res = await fetch('/api/v1/admin/media/list?t=' + Date.now());
                const data = await res.json();
                const grid = document.getElementById('mediaGrid');
                grid.innerHTML = '';

                if (data.length === 0) {
                    grid.innerHTML = '<p class="text-center w-100 py-5 text-muted">ยังไม่มีรูปภาพในคลัง</p>';
                    return;
                }

                data.forEach(item => {
                    const div = document.createElement('div');
                    div.className = 'media-item';
                    const isPicker = new URLSearchParams(window.location.search).get('picker') === '1';
                    
                    let actionBtn = `<button class="btn btn-sm btn-copy w-100" onclick="copyUrl('${item.url}')"><i class="fas fa-link"></i> คัดลอกลิงก์</button>`;
                    if (isPicker) {
                        actionBtn = `<button class="btn btn-sm btn-primary w-100" onclick="selectForPicker('${item.url}')"><i class="fas fa-check"></i> เลือกรูปนี้</button>`;
                    }

                    div.innerHTML = `
                        <img src="${item.url}" class="media-preview" onclick="window.open('${item.url}', '_blank')">
                        <div class="media-info">
                            <div class="media-name" title="${item.name}">${item.name}</div>
                            <div class="media-actions">
                                ${actionBtn}
                                <button class="btn btn-sm btn-delete" onclick="deleteMedia('${item.name}')"><i class="fas fa-trash"></i></button>
                            </div>
                        </div>
                    `;
                    grid.appendChild(div);
                });
            } catch (e) {
                console.error("Load failed", e);
            }
        }

        function selectForPicker(url) {
            const fullUrl = url.startsWith('http') ? url : window.location.origin + url;
            const params = new URLSearchParams(window.location.search);
            const pickerType = params.get('picker');
            
            if (pickerType === '1' && window.opener && window.opener.tinymceFilePickerCallback) {
                // TinyMCE Editor
                window.opener.tinymceFilePickerCallback(fullUrl);
                window.close();
            } else if (pickerType === '2' && window.opener && window.opener.chooseImageFromGallery) {
                // Cover Image Input
                window.opener.chooseImageFromGallery(fullUrl);
                window.close();
            } else {
                alert("ไม่พบหน้าต่างหลัก (Editor) หรือ Callback ไม่ถูกต้อง");
            }
        }

        async function uploadFile(input) {
            if (!input.files || input.files.length === 0) return;

            const file = input.files[0];
            const formData = new FormData();
            formData.append('file', file);

            try {
                const res = await fetch('/api/v1/admin/media/upload', {
                    method: 'POST',
                    body: formData
                });

                if (res.ok) {
                    loadMedia();
                    input.value = '';
                } else {
                    alert('อัปโหลดไม่สำเร็จ');
                }
            } catch (e) {
                alert('เกิดข้อผิดพลาดในการอัปโหลด');
            }
        }

        async function deleteMedia(name) {
            if (!confirm(`คุณต้องการลบรูป ${name} ใช่หรือไม่?`)) return;

            // Using FormData for simple compatibility if needed, or JSON
            const formData = new FormData();
            formData.append('name', name);

            try {
                const res = await fetch('/api/v1/admin/media/delete', {
                    method: 'POST',
                    body: formData
                });

                if (res.ok) {
                    loadMedia();
                } else {
                    alert('ลบไม่สำเร็จ');
                }
            } catch (e) {
                alert('เกิดข้อผิดพลาดในการลบ');
            }
        }

        function copyUrl(url) {
            const fullUrl = window.location.origin + url;
            navigator.clipboard.writeText(fullUrl).then(() => {
                alert('คัดลอก URL แล้ว: ' + fullUrl);
            });
        }
    </script>
</body>

</html>