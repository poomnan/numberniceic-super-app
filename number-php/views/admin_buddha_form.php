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
            background-color: #f0f2f5;
            font-family: 'Sarabun', sans-serif;
            color: #333;
        }

        .header-section {
            background: #fff;
            padding: 20px 0;
            margin-bottom: 25px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        }

        .card {
            border-radius: 15px;
            border: none;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
            background: #fff;
        }

        .form-label {
            font-weight: 600;
            color: #444;
            margin-bottom: 8px;
        }

        .form-control,
        .form-select {
            border-radius: 10px;
            padding: 12px 15px;
            border: 1px solid #dee2e6;
            transition: all 0.2s;
        }

        .form-control:focus,
        .form-select:focus {
            border-color: #198754;
            box-shadow: 0 0 0 0.25rem rgba(25, 135, 84, 0.1);
        }

        .preview-img {
            max-width: 100%;
            width: 250px;
            height: auto;
            display: block;
            margin: 15px auto;
            border-radius: 15px;
            border: 3px solid #fff;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
        }

        .btn-save {
            background: linear-gradient(45deg, #198754, #20c997);
            border: none;
            padding: 12px 30px;
            border-radius: 12px;
            font-weight: bold;
            box-shadow: 0 4px 10px rgba(25, 135, 84, 0.2);
        }

        .btn-save:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 15px rgba(25, 135, 84, 0.3);
        }

        @media (max-width: 768px) {
            .header-section h2 {
                font-size: 1.5rem;
            }

            .card {
                padding: 20px !important;
            }

            .btn-save {
                width: 100%;
            }
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="header-section">
        <div class="container">
            <div class="d-flex align-items-center">
                <a href="/web/admin/buddha" class="btn btn-outline-secondary rounded-circle me-3"
                    style="width: 45px; height: 45px; display: flex; align-items: center; justify-content: center;">
                    <i class="fas fa-arrow-left"></i>
                </a>
                <h2 class="mb-0 fw-bold">
                    <?= $item ? '<i class="fas fa-edit text-primary"></i> แก้ไขข้อมูลพระปาง' : '<i class="fas fa-plus-circle text-success"></i> เพิ่มปางใหม่' ?>
                </h2>
            </div>
        </div>
    </div>

    <div class="container mb-5">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card p-4">
                    <form action="/web/admin/buddha/save" method="POST" enctype="multipart/form-data">
                        <input type="hidden" name="id" value="<?= $item->id ?? '' ?>">

                        <div class="row">
                            <div class="col-md-7 mb-4">
                                <label class="form-label">ชื่อปางพระพุทธรูป <span class="text-danger">*</span></label>
                                <input type="text" name="pang_name" class="form-control form-control-lg" required
                                    value="<?= htmlspecialchars($item->pang_name ?? '') ?>"
                                    placeholder="ตัวอย่าง: ปางรำพึง">
                            </div>

                            <div class="col-md-5 mb-4">
                                <label class="form-label">ประจำวัน</label>
                                <select name="buddha_day" class="form-select form-select-lg">
                                    <option value="">-- สมาชิกทั่วไป --</option>
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
                        </div>

                        <div class="mb-4">
                            <label class="form-label">รูปภาพประกอบ</label>
                            <div class="card bg-light border-0 p-3">
                                <div class="mb-3">
                                    <small class="text-muted d-block mb-2">อัปโหลดไฟล์ (JPG/PNG)</small>
                                    <input type="file" name="image_file" class="form-control" accept="image/*">
                                </div>
                                <div class="">
                                    <small class="text-muted d-block mb-2">หรือ ระบุ URL รูปภาพ</small>
                                    <input type="text" name="image_url" class="form-control"
                                        value="<?= htmlspecialchars($item->image_url ?? '') ?>"
                                        placeholder="https://...">
                                </div>
                                <?php if (!empty($item->image_url)): ?>
                                    <img src="<?= $item->image_url ?>" class="preview-img" id="imgPreview">
                                <?php endif; ?>
                            </div>
                        </div>

                        <div class="mb-4">
                            <label class="form-label">รายละเอียด / คำอวยพร / ข้อความสรงน้ำ <span
                                    class="text-danger">*</span></label>
                            <textarea name="description" class="form-control" rows="8" required
                                placeholder="พิมพ์ข้อความที่ต้องการให้สมาชิกเห็น..."><?= htmlspecialchars($item->description ?? '') ?></textarea>
                            <div class="form-text mt-2 text-warning"><i class="fas fa-info-circle"></i>
                                ข้อความนี้จะแสดงในหน้า 'ตั้งพระประจำตัว' บนตัวแอพ</div>
                        </div>

                        <div class="text-center text-md-end pt-3">
                            <button type="submit" class="btn btn-save btn-lg text-white">
                                <i class="fas fa-save me-2"></i> บันทึกข้อมูล
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>

</html>