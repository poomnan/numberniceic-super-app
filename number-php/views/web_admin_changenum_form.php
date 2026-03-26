<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>แก้ไขข้อมูล:
        <?php echo htmlspecialchars($item->title); ?> - Admin Panel
    </title>

    <!-- Include TinyMCE via CDNJS -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/tinymce/6.8.3/tinymce.min.js" referrerpolicy="origin"></script>
    <script>
        tinymce.init({
            selector: '#content',
            plugins: 'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template',
            toolbar: 'undo redo | blocks | bold italic strikethrough | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | forecolor backcolor | link image | preview code fullscreen',
            height: 600,
            promotion: false,
            branding: false,
            contextmenu: false,
            content_style: 'body { font-family: "Segoe UI", Helvetica, Arial, sans-serif; font-size:16px; line-height: 1.6; }'
        });
    </script>

    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f0f2f5;
            margin: 0;
            padding: 0;
        }

        .container {
            max-width: 1000px;
            margin: 2rem auto;
            padding: 2.5rem;
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
        }

        h1 {
            color: #333;
            border-bottom: 2px solid #20c997;
            padding-bottom: 1rem;
            margin-top: 0;
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 1.8rem;
        }

        .form-group {
            margin-bottom: 1.5rem;
        }

        label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 600;
            color: #495057;
            font-size: 0.95rem;
        }

        input[type="text"] {
            width: 100%;
            padding: 0.8rem 1rem;
            border: 1px solid #ced4da;
            border-radius: 6px;
            box-sizing: border-box;
            font-size: 1rem;
            color: #333;
            transition: border-color 0.2s;
        }

        input[type="text"]:focus {
            border-color: #20c997;
            outline: none;
            box-shadow: 0 0 0 3px rgba(32, 201, 151, 0.15);
        }

        .code-display {
            display: inline-block;
            background: #e9ecef;
            color: #495057;
            padding: 0.4rem 0.8rem;
            border-radius: 4px;
            font-family: monospace;
            font-size: 1.1em;
            border: 1px solid #dee2e6;
        }

        .actions {
            margin-top: 2rem;
            display: flex;
            gap: 1rem;
            justify-content: flex-end;
            padding-top: 1.5rem;
            border-top: 1px solid #eee;
        }

        .btn {
            padding: 0.75rem 1.5rem;
            border-radius: 6px;
            text-decoration: none;
            font-weight: 600;
            cursor: pointer;
            border: none;
            font-size: 1rem;
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
        }

        .btn-save {
            background: #20c997;
            color: white;
            min-width: 120px;
            box-shadow: 0 4px 6px rgba(32, 201, 151, 0.2);
        }

        .btn-save:hover {
            background: #1ba87e;
            transform: translateY(-2px);
            box-shadow: 0 6px 12px rgba(32, 201, 151, 0.3);
        }

        .btn-cancel {
            background: #f8f9fa;
            color: #495057;
            border: 1px solid #ced4da;
            min-width: 100px;
        }

        @media (max-width: 768px) {
            .container {
                margin: 1rem;
                padding: 1.5rem;
            }

            .actions {
                flex-direction: column-reverse;
            }

            .btn {
                width: 100%;
            }
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container">
        <h1>
            <span>📝</span>
            แก้ไข:
            <?php echo htmlspecialchars($item->title); ?>
        </h1>

        <form action="/web/admin/changenum/update/<?php echo urlencode($item->file_name); ?>" method="POST">
            <div class="form-group"
                style="background:#f8f9fa; padding:1.5rem; border-radius:8px; border:1px solid #eee;">
                <label>รหัสไฟล์อ้างอิง (System Key)</label>
                <div class="code-display">
                    <?php echo htmlspecialchars($item->file_name); ?>
                </div>
            </div>

            <div class="form-group">
                <label for="title">หัวข้อเรื่อง (Title)</label>
                <input type="text" id="title" name="title" value="<?php echo htmlspecialchars($item->title); ?>"
                    required>
            </div>

            <div class="form-group">
                <label for="content">เนื้อหาบทความ (HTML Content)</label>
                <textarea id="content" name="content"><?php echo htmlspecialchars($item->content); ?></textarea>
            </div>

            <div class="actions">
                <a href="/web/admin/changenum" class="btn btn-cancel">ยกเลิก</a>
                <button type="submit" class="btn btn-save">💾 บันทึกข้อมูล</button>
            </div>
        </form>
    </div>
</body>

</html>