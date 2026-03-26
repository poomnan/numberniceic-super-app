<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>แก้ไขข้อมูล:
        <?php echo htmlspecialchars($merit->title); ?> - Admin Panel
    </title>

    <!-- Include TinyMCE via CDNJS to avoid API Key nagging -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/tinymce/6.8.3/tinymce.min.js" referrerpolicy="origin"></script>
    <script>
        tinymce.init({
            selector: '#content',
            plugins: 'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template',
            toolbar: 'undo redo | blocks | bold italic strikethrough | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | forecolor backcolor | link image | preview code fullscreen',
            height: 600,
            promotion: false, // Hide "Upgrade" button and nagging
            branding: false,  // Hide TinyMCE branding
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
            border-bottom: 2px solid #ff9800;
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
            border-color: #ff9800;
            outline: none;
            box-shadow: 0 0 0 3px rgba(255, 152, 0, 0.15);
        }

        .code-display {
            display: inline-block;
            background: #e9ecef;
            color: #495057;
            padding: 0.4rem 0.8rem;
            border-radius: 4px;
            font-family: monospace;
            font-size: 1.1em;
            user-select: text;
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
            background: #ff9800;
            color: white;
            min-width: 120px;
            box-shadow: 0 4px 6px rgba(255, 152, 0, 0.2);
        }

        .btn-save:hover {
            background: #e68a00;
            transform: translateY(-2px);
            box-shadow: 0 6px 12px rgba(255, 152, 0, 0.3);
        }

        .btn-cancel {
            background: #f8f9fa;
            color: #495057;
            border: 1px solid #ced4da;
            min-width: 100px;
        }

        .btn-cancel:hover {
            background: #e2e6ea;
            border-color: #dae0e5;
        }

        @media (max-width: 768px) {
            .container {
                margin: 1rem;
                padding: 1.5rem;
            }

            h1 {
                font-size: 1.5rem;
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
            <?php echo htmlspecialchars($merit->title); ?>
        </h1>

        <form action="/web/admin/merits/update/<?php echo urlencode($merit->file_name); ?>" method="POST">
            <div class="form-group"
                style="background:#f8f9fa; padding:1.5rem; border-radius:8px; border:1px solid #eee;">
                <label>รหัสไฟล์อ้างอิง (System Key)</label>
                <div class="code-display">
                    <?php echo htmlspecialchars($merit->file_name); ?>
                </div>
                <small style="display:block; margin-top:0.5rem; color:#6c757d;">ระบบจะใช้ค่านี้เพื่อดึงข้อมูลไปแสดงในแอป
                    (ไม่สามารถเปลี่ยนชื่อสคริปต์กลางได้)</small>
            </div>

            <div class="form-group">
                <label for="title">หัวข้อเรื่อง (Title)</label>
                <input type="text" id="title" name="title" value="<?php echo htmlspecialchars($merit->title); ?>"
                    required placeholder="เช่น ปางวันจันทร์, ปางวันพุธ ฯลฯ">
            </div>

            <div class="form-group">
                <label for="content">เนื้อหาบทความ (HTML Content)</label>
                <textarea id="content" name="content"><?php echo htmlspecialchars($merit->content); ?></textarea>
            </div>

            <div class="actions">
                <a href="/web/admin/merits" class="btn btn-cancel">ยกเลิก</a>
                <button type="submit" class="btn btn-save">💾 บันทึกข้อมูล</button>
            </div>
        </form>
    </div>
</body>

</html>