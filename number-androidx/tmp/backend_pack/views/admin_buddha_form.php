<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>
        <?= $item ? 'แก้ไข' : 'เพิ่ม' ?>พระปาง - NumberNice Admin
    </title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        body {
            background-color: #f8f9fa;
            font-family: 'Sarabun', sans-serif;
        }

        .card {
            border-radius: 15px;
            border: none;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .preview-img {
            max-width: 200px;
            max-height: 300px;
            display: block;
            margin-top: 10px;
            border-radius: 10px;
            border: 2px solid #ddd;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="container mt-4">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="d-flex align-items-center mb-3">
                    <a href="/admin/buddha" class="btn btn-outline-secondary me-3"><i class="fas fa-arrow-left"></i>
                        กลับ</a>
                    <h2>
                        <?= $item ? 'แก้ไขข้อมูลพระปาง' : 'เพิ่มปางใหม่' ?>
                    </h2>
                </div>

                <div class="card p-4">
                    <form action="/admin/buddha/save" method="POST" enctype="multipart/form-data">
                        <input type="hidden" name="id" value="<?= $item->id ?? '' ?>">

                        <div class="mb-3">
                            <label class="form-label">ชื่อปางพระพุทธรูป</label>
                            <input type="text" name="pang_name" class="form-control" required
                                value="<?= htmlspecialchars($item->pang_name ?? '') ?>" placeholder="เช่น ปางรำพึง">
                        </div>

                        <div class="mb-3">
                            <label class="form-label">ประจำวัน (ถ้ามี)</label>
                            <select name="buddha_day" class="form-select">
                                <option value="">ไม่ได้ระบุ</option>
                                <option value="1" <?= ($item->buddha_day ?? '') == 1 ? 'selected' : '' ?>>วันอาทิตย์
                                </option>
                                <option value="2" <?= ($item->buddha_day ?? '') == 2 ? 'selected' : '' ?>>วันจันทร์
                                </option>
                                <option value="3" <?= ($item->buddha_day ?? '') == 3 ? 'selected' : '' ?>>วันอังคาร
                                </option>
                                <option value="4" <?= ($item->buddha_day ?? '') == 4 ? 'selected' : '' ?>>วันพุธ
                                    (กลางวัน)</option>
                                <option value="8" <?= ($item->buddha_day ?? '') == 8 ? 'selected' : '' ?>>วันพุธ
                                    (กลางคืน)</option>
                                <option value="5" <?= ($item->buddha_day ?? '') == 5 ? 'selected' : '' ?>>วันพฤหัสบดี
                                </option>
                                <option value="6" <?= ($item->buddha_day ?? '') == 6 ? 'selected' : '' ?>>วันศุกร์
                                </option>
                                <option value="7" <?= ($item->buddha_day ?? '') == 7 ? 'selected' : '' ?>>วันเสาร์
                                </option>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">รูปภาพ (อัปโหลดใหม่)</label>
                            <input type="file" name="image_file" class="form-control" accept="image/*">
                        </div>

                        <div class="mb-3">
                            <label class="form-label">หรือ ใส่ URL รูปภาพ</label>
                            <input type="text" name="image_url" class="form-control"
                                value="<?= htmlspecialchars($item->image_url ?? '') ?>" placeholder="https://...">
                            <?php if (!empty($item->image_url)): ?>
                                <img src="<?= $item->image_url ?>" class="preview-img" id="imgPreview">
                            <?php endif; ?>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">รายละเอียด/คำอวยพรประจำปี</label>
                            <textarea name="description" class="form-control" rows="6" required
                                placeholder="คำแนะนำในการสรงน้ำพระหรือการเสริมดวง..."><?= htmlspecialchars($item->description ?? '') ?></textarea>
                        </div>

                        <div class="text-end">
                            <button type="submit" class="btn btn-primary px-5 btn-lg">บันทึกข้อมูล</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>

</html>