<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>จัดการวันอัปมงคล - Admin</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            font-family: 'Sarabun', sans-serif;
            background-color: #f8f9fa;
        }

        .main-container {
            max-width: 800px;
            margin: 30px auto;
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .header {
            border-bottom: 2px solid #dc3545;
            padding-bottom: 15px;
            margin-bottom: 25px;
        }

        .form-label {
            font-weight: 600;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="container">
        <div class="main-container">
            <div class="header">
                <h3>👿 จัดการวันอัปมงคล (Inauspicious)</h3>
                <p class="text-muted">กำหนดข้อมูล วัน/ทิศ อัปมงคล ให้กับสมาชิกรายบุคคล</p>
            </div>

            <?php if (isset($_GET['status']) && $_GET['status'] == 'success'): ?>
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    บันทึกข้อมูลและส่งแจ้งเตือนเรียบร้อยแล้ว! (MemberID:
                    <?php echo htmlspecialchars($_GET['memberid'] ?? ''); ?>)
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            <?php endif; ?>

            <form action="/admin/inauspicious/assign" method="POST" enctype="multipart/form-data">
                <div class="mb-3">
                    <label for="memberid" class="form-label">Member ID (รหัสสมาชิก)</label>
                    <input type="text" class="form-control" id="memberid" name="memberid" required
                        value="<?php echo htmlspecialchars($_GET['memberid'] ?? ''); ?>"
                        placeholder="ระบุ Member ID ของสมาชิก">
                </div>

                <div class="mb-3">
                    <label for="type" class="form-label">ประเภทข้อมูล</label>
                    <select class="form-select" id="type" name="type">
                        <option value="year">📅 สำหรับปีนี้ (Year)</option>
                        <option value="life">♾️ ตลอดชีวิต (Lifetime)</option>
                    </select>
                </div>

                <div class="mb-3">
                    <label for="title" class="form-label">หัวข้อ (Title)</label>
                    <input type="text" class="form-control" id="title" name="title" required
                        placeholder="เช่น วันอังคารเป็นวันกาลกิณี">
                </div>

                <div class="mb-3">
                    <label for="description" class="form-label">รายละเอียด (Description)</label>
                    <textarea class="form-control" id="description" name="description" rows="4"
                        placeholder="รายละเอียดคำแนะนำ..."></textarea>
                </div>

                <div class="mb-3">
                    <label for="image_file" class="form-label">รูปภาพ (Image) - *ถ้ามี</label>
                    <input class="form-control" type="file" id="image_file" name="image_file" accept="image/*">
                    <div class="form-text">รองรับไฟล์ JPG, PNG</div>
                </div>

                <div class="d-grid gap-2">
                    <button type="submit" class="btn btn-danger btn-lg">บันทึกและส่งแจ้งเตือน</button>
                    <a href="/web/dashboard" class="btn btn-secondary">กลับหน้า Dashboard</a>
                </div>
            </form>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>

</html>