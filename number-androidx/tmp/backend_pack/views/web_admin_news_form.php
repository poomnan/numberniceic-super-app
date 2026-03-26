<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>
        <?php echo isset($newsItem) ? 'Edit News' : 'Create News'; ?> - Ananya Admin
    </title>

    <!-- TinyMCE -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/tinymce/6.8.2/tinymce.min.js" referrerpolicy="origin"></script>
    <script>
        tinymce.init({
            selector: '#news_detail',
            height: 500,
            menubar: true,
            plugins: 'advlist autolink lists link image charmap preview anchor searchreplace visualblocks code fullscreen insertdatetime media table code help wordcount',
            toolbar: 'undo redo | formatselect | bold italic | alignleft aligncenter alignright | bullist numlist | link image | code',
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
            max-width: 900px;
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
        }

        .form-control:focus {
            outline: none;
            border-color: #198754;
        }

        .btn {
            padding: 10px 20px;
            border-radius: 6px;
            text-decoration: none;
            color: white;
            border: none;
            cursor: pointer;
            font-size: 1rem;
        }

        .btn-primary {
            background-color: #0d6efd;
        }

        .btn-secondary {
            background-color: #6c757d;
        }

        .btn-white {
            background-color: white;
            color: #198754;
            font-weight: 600;
        }

        /* Image Preview */
        .img-preview {
            margin-top: 10px;
            width: 100%;
            max-width: 300px;
            height: 150px;
            object-fit: cover;
            border-radius: 6px;
            border: 1px solid #ddd;
            background: #f8f9fa;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <form
            action="<?php echo isset($newsItem) ? '/web/admin/news/update/' . $newsItem->newsid : '/web/admin/news/store'; ?>"
            method="POST">
            <div class="card">
                <div class="card-header">
                    <h2>
                        <?php echo isset($newsItem) ? 'แก้ไขข่าว (Edit News)' : 'สร้างข่าวใหม่ (Create News)'; ?>
                    </h2>
                    <a href="/web/admin/news" class="btn btn-white">กลับ</a>
                </div>
                <div class="card-body">

                    <!-- Title -->
                    <div class="form-group">
                        <label class="form-label">หัวข้อข่าว (Headline)</label>
                        <input type="text" name="news_headline" class="form-control" required
                            value="<?php echo isset($newsItem) ? htmlspecialchars($newsItem->news_headline) : ''; ?>">
                    </div>

                    <!-- Category (NEW!) -->
                    <div class="form-group">
                        <label class="form-label">หมวดหมู่ (Category) - สำคัญสำหรับการแสดงผลใน App</label>
                        <select name="category_name" class="form-control" required>
                            <option value="">-- เลือกหมวดหมู่ --</option>
                            <?php
                            $currentCat = isset($newsItem) ? $newsItem->category_name : '';
                            if (isset($categories)) {
                                foreach ($categories as $cat) {
                                    $selected = ($currentCat == $cat->category_name) ? 'selected' : '';
                                    echo "<option value=\"" . htmlspecialchars($cat->category_name) . "\" $selected>" . htmlspecialchars($cat->category_name) . "</option>";
                                }
                            }
                            ?>
                        </select>
                        <div style="font-size:0.85rem; color:#888; margin-top:5px;">
                            * ระบบจะนำข่าวไปแสดงในหน้า Home ของ App ตามหมวดหมู่นี้
                        </div>
                    </div>

                    <!-- Short Title -->
                    <div class="form-group">
                        <label class="form-label">หัวข้อแบบย่อ (Short Title) - ใช้ในบางจุดของ App</label>
                        <input type="text" name="news_title_short" class="form-control"
                            value="<?php echo isset($newsItem) ? htmlspecialchars($newsItem->news_title_short) : ''; ?>">
                    </div>

                    <!-- Description -->
                    <div class="form-group">
                        <label class="form-label">คำโปรย / รายละเอียดสังเขป (Description)</label>
                        <textarea name="news_desc" class="form-control"
                            rows="3"><?php echo isset($newsItem) ? htmlspecialchars($newsItem->news_desc) : ''; ?></textarea>
                    </div>

                    <!-- Image URL -->
                    <div class="form-group">
                        <label class="form-label">ลิงก์รูปภาพปก (Image URL)</label>
                        <input type="text" id="news_pic_header" name="news_pic_header" class="form-control"
                            placeholder="https://..."
                            value="<?php echo isset($newsItem) ? htmlspecialchars($newsItem->news_pic_header) : ''; ?>">
                        <?php if (isset($newsItem) && !empty($newsItem->news_pic_header)): ?>
                            <img src="<?php echo $newsItem->news_pic_header; ?>" class="img-preview">
                        <?php endif; ?>
                    </div>

                    <!-- Content -->
                    <div class="form-group">
                        <label class="form-label">เนื้อหาข่าว (Detail)</label>
                        <textarea id="news_detail"
                            name="news_detail"><?php echo isset($newsItem) ? htmlspecialchars($newsItem->news_detail) : ''; ?></textarea>
                    </div>

                    <div style="text-align: right; margin-top: 2rem;">
                        <button type="submit" class="btn btn-primary btn-lg">บันทึกข้อมูล (Save)</button>
                    </div>

                </div>
            </div>
        </form>
    </div>
</body>

</html>