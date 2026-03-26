<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= isset($item) ? "แก้ไขวัด" : "เพิ่มวัดใหม่" ?></title>
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

        .form-group label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 600;
            color: #555;
        }

        .form-control {
            width: 100%;
            padding: 0.75rem;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 1rem;
            box-sizing: border-box;
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

        .btn-success {
            background-color: #28a745;
        }

        .btn-secondary {
            background-color: #6c757d;
        }

        .btn-white {
            background-color: white;
            color: #333;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="main-wrapper">
        <div class="card">
            <div class="card-header">
                <h2><?= isset($item) ? "แก้ไขข้อมูลวัด" : "เพิ่มวัดใหม่" ?></h2>
                <a href="/admin/temple" class="btn btn-white">&larr; กลับ</a>
            </div>
            <div class="card-body">
                <form action="/admin/temple/save" method="post" enctype="multipart/form-data">
                    <input type="hidden" name="id" value="<?= $item->id ?? '' ?>">

                    <div class="form-group">
                        <label>ชื่อวัด <span style="color:red">*</span></label>
                        <input type="text" class="form-control" name="temple_name"
                            value="<?= htmlspecialchars($item->temple_name ?? '') ?>" required>
                    </div>

                    <div class="form-group">
                        <label>ที่อยู่ / สถานที่ตั้ง</label>
                        <textarea class="form-control" name="address"
                            rows="3"><?= htmlspecialchars($item->address ?? '') ?></textarea>
                    </div>

                    <div class="form-group">
                        <label>รายละเอียด / ประวัติความเป็นมา</label>
                        <textarea class="form-control" name="description"
                            rows="6"><?= htmlspecialchars($item->description ?? '') ?></textarea>
                    </div>

                    <div class="form-group">
                        <label>รูปภาพ</label>
                        <input type="file" name="image_file" accept="image/*" style="margin-bottom:10px;">
                        <input type="text" class="form-control" name="image_url" placeholder="หรือใส่ URL รูปภาพ"
                            value="<?= htmlspecialchars($item->image_url ?? '') ?>">
                        <?php if (!empty($item->image_url)): ?>
                            <div style="margin-top:10px;">
                                <img src="<?= $item->image_url ?>"
                                    style="max-height: 200px; border-radius: 8px; border:1px solid #ddd;">
                            </div>
                        <?php endif; ?>
                    </div>

                    <div style="margin-top:2rem;">
                        <button type="submit" class="btn btn-success">บันทึกข้อมูล</button>
                        <a href="/admin/temple" class="btn btn-secondary">ยกเลิก</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>

</html>