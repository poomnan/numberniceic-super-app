<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>
        <?php echo isset($item) ? 'Edit' : 'Add'; ?> Spell/Warning - Ananya Admin
    </title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
            padding: 0;
        }

        .main-wrapper {
            max-width: 800px;
            margin: 2rem auto;
            padding: 0 1rem;
        }

        .card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
            padding: 2rem;
        }

        .header {
            margin-bottom: 2rem;
            border-bottom: 1px solid #eee;
            padding-bottom: 1rem;
        }

        .header h1 {
            margin: 0;
            font-size: 1.5rem;
            color: #333;
        }

        .form-group {
            margin-bottom: 1.5rem;
        }

        label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 600;
            color: #555;
        }

        input[type="text"],
        select,
        textarea {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 8px;
            font-size: 100%;
            box-sizing: border-box;
        }

        textarea {
            height: 200px;
            resize: vertical;
        }

        .btn {
            padding: 12px 24px;
            border-radius: 8px;
            text-decoration: none;
            color: white;
            display: inline-block;
            font-weight: 500;
            transition: all 0.2s;
            border: none;
            cursor: pointer;
            font-size: 1rem;
        }

        .btn-success {
            background-color: #198754;
        }

        .btn-secondary {
            background-color: #6c757d;
        }

        .btn:hover {
            opacity: 0.9;
        }

        .footer-actions {
            display: flex;
            justify-content: space-between;
            margin-top: 2rem;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <div class="card">
            <div class="header">
                <h1>
                    <?php echo isset($item) ? '📝 แก้ไขรายการ' : '➕ เพิ่มรายการใหม่'; ?>
                </h1>
            </div>

            <form
                action="<?php echo isset($item) ? '/web/admin/spells/update/' . $item->id : '/web/admin/spells/store'; ?>"
                method="POST" enctype="multipart/form-data">
                <div class="form-group">
                    <label>ประเภท (Type)</label>
                    <select name="type" required>
                        <option value="spell" <?php echo (isset($item) && $item->type == 'spell' ? 'selected' : ''); ?>>
                            คาถา (Spell)</option>
                        <option value="warning" <?php echo (isset($item) && $item->type == 'warning' ? 'selected' : ''); ?>>คำเตือนพิเศษ (Warning)</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>หัวข้อ (Title)</label>
                    <input type="text" name="title"
                        value="<?php echo isset($item) ? htmlspecialchars($item->title) : ''; ?>" required
                        placeholder="เช่น คาถาบูชาพระประจำวันเกิด">
                </div>

                <div class="form-group">
                    <label>รูปภาพ (Photo)</label>
                    <?php if (isset($item) && !empty($item->photo)): ?>
                        <div style="margin-bottom: 10px;">
                            <img src="<?php echo htmlspecialchars($item->photo); ?>" alt="Current Photo"
                                style="max-height: 150px; border-radius: 8px; border: 1px solid #ddd;">
                        </div>
                    <?php endif; ?>
                    <input type="file" name="photo" accept="image/*">
                    <small style="color: #888;">รองรับไฟล์ JPG, PNG, GIF</small>
                </div>

                <div class="form-group">
                    <label>เนื้อหา (Content)</label>
                    <textarea name="content" required
                        placeholder="พิมพ์บทคาถาหรือคำเตือนที่นี่..."><?php echo isset($item) ? htmlspecialchars($item->content) : ''; ?></textarea>
                </div>

                <div class="footer-actions">
                    <a href="/web/admin/spells" class="btn btn-secondary">ยกเลิก (Cancel)</a>
                    <button type="submit" class="btn btn-success">
                        <?php echo isset($item) ? 'บันทึกการแก้ไข' : 'บันทึกข้อมูล'; ?>
                    </button>
                </div>
            </form>
        </div>
    </div>
</body>

</html>