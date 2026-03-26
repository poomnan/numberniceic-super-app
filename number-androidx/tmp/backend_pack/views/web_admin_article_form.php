<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>
        <?php echo isset($article) ? 'Edit Article' : 'Create Article'; ?> - Ananya
    </title>
    <!-- Use TinyMCE from CDN (Free version) -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/tinymce/6.8.2/tinymce.min.js" referrerpolicy="origin"></script>
    <script>
        tinymce.init({
            selector: '#content',
            height: 600,
            menubar: true,
            plugins: 'advlist autolink lists link image charmap preview anchor searchreplace visualblocks code fullscreen insertdatetime media table code help wordcount',
            toolbar: 'undo redo | formatselect | ' +
                'bold italic backcolor | alignleft aligncenter ' +
                'alignright alignjustify | bullist numlist outdent indent | ' +
                'removeformat | help',
            content_style: 'body { font-family:Helvetica,Arial,sans-serif; font-size:14px }'
        });
    </script>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
            padding: 0;
        }

        .main-wrapper {
            max-width: 1000px;
            margin: 2rem auto;
            padding: 0 1rem;
            padding-bottom: 4rem;
        }

        .card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
            overflow: hidden;
        }

        .card-header {
            background: linear-gradient(135deg, #198754, #20c997);
            color: white;
            padding: 1.5rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .card-header h2 {
            margin: 0;
            font-size: 1.5rem;
        }

        .card-body {
            padding: 2rem;
        }

        .btn {
            display: inline-block;
            padding: 10px 20px;
            border-radius: 6px;
            text-decoration: none;
            color: white;
            border: none;
            cursor: pointer;
            font-size: 1rem;
            font-weight: 500;
            transition: all 0.2s;
        }

        .btn-secondary {
            background-color: #6c757d;
        }

        .btn-primary {
            background-color: #0d6efd;
        }

        .btn-white {
            background-color: rgba(255, 255, 255, 0.9);
            color: #198754;
            font-weight: 600;
        }

        .btn:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }

        .form-group {
            margin-bottom: 1.5rem;
        }

        .form-label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 600;
            color: #444;
        }

        .form-control {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 1rem;
            box-sizing: border-box;
            font-family: inherit;
        }

        .form-control:focus {
            outline: none;
            border-color: #198754;
            box-shadow: 0 0 0 3px rgba(25, 135, 84, 0.1);
        }

        .row {
            display: flex;
            gap: 1.5rem;
        }

        .col {
            flex: 1;
        }

        .help-text {
            font-size: 0.85rem;
            color: #888;
            margin-top: 4px;
        }

        .checkbox-wrapper {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px;
            background: #f8f9fa;
            border-radius: 6px;
            border: 1px solid #eee;
        }

        input[type="checkbox"] {
            width: 18px;
            height: 18px;
        }

        /* Image Picker Styles */
        .input-group {
            display: flex;
            gap: 10px;
        }

        .btn-outline {
            background-color: white;
            border: 1px solid #0d6efd;
            color: #0d6efd;
            padding: 10px 15px;
        }

        .btn-outline:hover {
            background-color: #0d6efd;
            color: white;
        }

        /* Modal Styles */
        .modal-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            z-index: 9999;
            align-items: center;
            justify-content: center;
        }

        .modal-content {
            background: white;
            width: 80%;
            max-width: 800px;
            max-height: 80vh;
            border-radius: 12px;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
        }

        .modal-header {
            padding: 15px 20px;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .modal-body {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            background: #f8f9fa;
        }

        .image-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
            gap: 15px;
        }

        .image-item {
            background: white;
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 5px;
            cursor: pointer;
            transition: all 0.2s;
            text-align: center;
        }

        .image-item:hover {
            border-color: #0d6efd;
            transform: scale(1.05);
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
        }

        .image-item img {
            width: 100%;
            height: 100px;
            object-fit: cover;
            border-radius: 4px;
            background: #eee;
        }

        .image-name {
            font-size: 0.75rem;
            margin-top: 5px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            color: #555;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <form action="" method="POST">
            <div class="card">
                <div class="card-header">
                    <h2>
                        <?php echo isset($article) ? 'แก้ไขบทความ' : 'เขียนบทความใหม่'; ?>
                    </h2>
                    <a href="/web/admin/articles" class="btn btn-white">ยกเลิก</a>
                </div>
                <div class="card-body">

                    <div class="form-group">
                        <label class="form-label">หัวข้อบทความ (Title)</label>
                        <input type="text" name="title" class="form-control" require
                            value="<?php echo isset($article) ? htmlspecialchars($article->title) : ''; ?>">
                    </div>

                    <div class="row">
                        <div class="col">
                            <div class="form-group">
                                <label class="form-label">URL Slug (ภาษาอังกฤษ, ห้ามเว้นวรรค)</label>
                                <input type="text" name="slug" class="form-control" placeholder="example-article-slug"
                                    value="<?php echo isset($article) ? htmlspecialchars($article->slug) : ''; ?>">
                                <div class="help-text">หากเว้นว่าง ระบบจะสร้างให้เองอัตโนมัติ</div>
                            </div>
                        </div>
                        <div class="col">
                            <div class="form-group">
                                <label class="form-label">หมวดหมู่ (Category)</label>
                                <select name="category" class="form-control" required>
                                    <option value="">-- เลือกหมวดหมู่ --</option>
                                    <?php
                                    // Fetch categories directly in view for simplicity (or pass from controller ideally)
                                    // Ideally, $categories should be passed from Controller. If not, fallback fetch.
                                    if (!isset($categories)) {
                                        // Fallback for direct view rendering without controller passing it
                                        global $container;
                                        if (isset($container)) {
                                            $stmt = $container->get('db')->query("SELECT * FROM news_categories ORDER BY sort_order ASC, category_name ASC");
                                            $categories = $stmt->fetchAll(PDO::FETCH_OBJ);
                                        }
                                    }

                                    $currentCat = isset($article) ? $article->category : '';
                                    if (isset($categories) && is_array($categories)) {
                                        foreach ($categories as $cat) {
                                            $selected = ($currentCat == $cat->category_name) ? 'selected' : '';
                                            echo "<option value=\"" . htmlspecialchars($cat->category_name) . "\" $selected>" . htmlspecialchars($cat->category_name) . "</option>";
                                        }
                                    } else {
                                        // Fallback options if DB fetch fails
                                        $defaults = ['ข่าวและบทความที่น่าสนใจ', 'Reviews และ Feedback จากประสบการณ์ลูกค้า', 'ความรู้และบทความเกี่ยวกับเบอร์โทรศัพท์', 'ชื่อและนามสกุลเสริมดวงชะตา', 'ศาสตร์ตัวเลขและทะเบียนรถมงคล', 'ทำนายดวงชะตาจากบ้านเลขที่', 'หลักการใช้และการเลือกเลขมงคลที่ถูกต้อง'];
                                        foreach ($defaults as $d) {
                                            $selected = ($currentCat == $d) ? 'selected' : '';
                                            echo "<option value=\"$d\" $selected>$d</option>";
                                        }
                                    }
                                    ?>
                                </select>
                            </div>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">คำโปรยสั้นๆ (Excerpt) - แสดงหน้ารายการ</label>
                        <textarea name="excerpt" class="form-control"
                            rows="3"><?php echo isset($article) ? htmlspecialchars($article->excerpt) : ''; ?></textarea>
                    </div>

                    <div class="form-group">
                        <label class="form-label">ลิงก์รูปภาพปก (Image URL)</label>
                        <div class="input-group">
                            <input type="text" id="image_url" name="image_url" class="form-control"
                                placeholder="/uploads/..."
                                value="<?php echo isset($article) ? htmlspecialchars($article->image_url) : ''; ?>">
                            <button type="button" class="btn btn-outline" onclick="openImageSelector()">
                                📷 เลือกรูป
                            </button>
                        </div>
                        <?php if (isset($article) && !empty($article->image_url)): ?>
                            <div style="margin-top:10px;">
                                <img src="<?php echo $article->image_url; ?>"
                                    style="height:100px; border-radius:4px; border:1px solid #ddd;">
                                <span style="font-size:0.8rem; color:#888; vertical-align:top;">(รูปปัจจุบัน)</span>
                            </div>
                        <?php endif; ?>
                    </div>

                    <div class="form-group">
                        <label class="form-label">เนื้อหาบทความ (Content)</label>
                        <textarea id="content"
                            name="content"><?php echo isset($article) ? htmlspecialchars($article->content) : ''; ?></textarea>
                    </div>

                    <div class="row">
                        <div class="col">
                            <div class="form-group">
                                <label class="form-label">หัวข้อแบบย่อ (Title Short)</label>
                                <input type="text" name="title_short" class="form-control"
                                    value="<?php echo isset($article) ? htmlspecialchars($article->title_short) : ''; ?>">
                            </div>
                        </div>
                        <div class="col">
                            <div class="form-group">
                                <label class="form-label">สถานะการเผยแพร่</label>
                                <div class="checkbox-wrapper">
                                    <input type="checkbox" name="is_published" id="is_published" value="1" <?php echo (isset($article) && $article->is_published) ? 'checked' : ''; ?>>
                                    <label for="is_published" style="cursor:pointer; font-weight:500;">เผยแพร่ทันที
                                        (Publish)</label>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="form-group" style="margin-top: 2rem; text-align: right;">
                        <button type="submit" class="btn btn-primary">
                            <?php echo isset($article) ? 'บันทึกการแก้ไข' : 'สร้างบทความ'; ?>
                        </button>
                    </div>

                </div>
            </div>
        </form>
    </div>

    <!-- Image Modal -->
    <div id="imageModal" class="modal-overlay">
        <div class="modal-content">
            <div class="modal-header">
                <h3 style="margin:0;">เลือกรูปภาพ (Select Image)</h3>
                <button type="button" class="btn btn-sm btn-secondary" onclick="closeImageSelector()">X</button>
            </div>
            <div class="modal-body">
                <div id="loadingImages" style="text-align:center; padding:20px;">Loading images...</div>
                <div id="imageGrid" class="image-grid"></div>
            </div>
        </div>
    </div>

    <script>
        function openImageSelector() {
            document.getElementById('imageModal').style.display = 'flex';
            fetchImages();
        }

        function closeImageSelector() {
            document.getElementById('imageModal').style.display = 'none';
        }

        function fetchImages() {
            const grid = document.getElementById('imageGrid');
            const loading = document.getElementById('loadingImages');
            grid.innerHTML = '';
            loading.style.display = 'block';

            fetch('/web/admin/api/images')
                .then(res => res.json())
                .then(files => {
                    loading.style.display = 'none';
                    if (!files || files.length === 0) {
                        grid.innerHTML = '<p style="text-align:center; width:100%; color:#888;">No images found in public/uploads</p>';
                        return;
                    }
                    files.forEach(file => {
                        const item = document.createElement('div');
                        item.className = 'image-item';
                        item.onclick = () => selectImage(file);

                        // Assuming running from root, path is /uploads/
                        const imgPath = '/uploads/' + file;

                        item.innerHTML = `<img src="${imgPath}" loading="lazy"><div class="image-name">${file}</div>`;
                        grid.appendChild(item);
                    });
                })
                .catch(err => {
                    loading.style.display = 'none';
                    grid.innerHTML = '<p style="color:red; text-align:center;">Error loading images.</p>';
                    console.error(err);
                });
        }

        function selectImage(filename) {
            // Update the input field
            const fullPath = '/uploads/' + filename;
            document.getElementById('image_url').value = fullPath;
            closeImageSelector();
        }

        // Close modal if clicked outside
        document.getElementById('imageModal').onclick = function (e) {
            if (e.target === this) {
                closeImageSelector();
            }
        }
    </script>
</body>

</html>