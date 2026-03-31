<?php
/** @var object $user */
/** @var string $mode */
/** @var object|null $row */
/** @var string|null $error */
$isEdit = ($mode ?? '') === 'edit';
$id = $isEdit ? (int) ($row->id ?? 0) : 0;
?>
<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title><?php echo $isEdit ? 'แก้ไขแท็กฤกษ์ยาม' : 'เพิ่มแท็กฤกษ์ยาม'; ?> - Admin</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        body {
            font-family: Arial, sans-serif;
            background: #f6f8fb;
            margin: 0;
            padding: 20px;
        }

        .container {
            max-width: 900px;
            margin: 0 auto;
        }

        .card {
            background: #fff;
            border-radius: 10px;
            padding: 18px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        }

        .title {
            font-size: 20px;
            font-weight: 800;
            color: #1f2937;
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 12px;
        }

        .muted {
            color: #6b7280;
            font-size: 13px;
        }

        .grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
            margin-top: 16px;
        }

        @media (max-width: 720px) {
            .grid {
                grid-template-columns: 1fr;
            }
        }

        label {
            display: block;
            font-weight: 700;
            font-size: 13px;
            color: #334155;
            margin-bottom: 6px;
        }

        input,
        textarea {
            width: 100%;
            box-sizing: border-box;
            padding: 10px 12px;
            border: 1px solid #d1d5db;
            border-radius: 8px;
            font-size: 14px;
        }

        textarea {
            min-height: 120px;
            resize: vertical;
        }

        .row {
            margin-bottom: 12px;
        }

        .actions {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            margin-top: 16px;
            align-items: center;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            border: 0;
            padding: 10px 14px;
            border-radius: 8px;
            text-decoration: none;
            cursor: pointer;
            font-weight: 800;
            font-size: 14px;
        }

        .btn-primary {
            background: #0ea5e9;
            color: #fff;
        }

        .btn-secondary {
            background: #6c757d;
            color: #fff;
        }

        .error {
            background: #fee2e2;
            border: 1px solid #fecaca;
            color: #991b1b;
            padding: 10px 12px;
            border-radius: 8px;
            margin-top: 12px;
            font-weight: 700;
        }

        .checkbox {
            display: inline-flex;
            gap: 10px;
            align-items: center;
            padding: 10px 12px;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
            background: #f8fafc;
        }
    </style>
</head>

<body>
    <div class="container">
        <?php include 'web_admin_toolbar.php'; ?>

        <div class="card">
            <div class="title">
                <i class="fa-solid fa-pen-nib"></i>
                <?php echo $isEdit ? 'แก้ไขแท็กฤกษ์ยาม' : 'เพิ่มแท็กฤกษ์ยาม'; ?>
            </div>
            <div class="muted">
                ตาราง: <code>rengyam_tag_meanings</code> (ชื่อแท็ก/สำนัก/คำอธิบาย) ใช้เพื่อให้แอปแสดงคำอธิบายจากฐานข้อมูลแทน hardcode
            </div>

            <?php if (!empty($error)): ?>
                <div class="error"><i class="fa-solid fa-triangle-exclamation"></i> <?php echo htmlspecialchars((string) $error, ENT_QUOTES); ?></div>
            <?php endif; ?>

            <form method="POST"
                action="<?php echo $isEdit ? '/web/admin/rengyam-tags/update/' . $id : '/web/admin/rengyam-tags/store'; ?>">

                <div class="grid">
                    <div class="row">
                        <label>ชื่อแท็ก (tag_name)</label>
                        <input type="text" name="tag_name" value="<?php echo htmlspecialchars((string) ($row->tag_name ?? ''), ENT_QUOTES); ?>" required>
                        <div class="muted" style="margin-top:6px;">ตัวอย่าง: วันฟู, วันจม, อำฤตโชค, มหาสิทธิโชค</div>
                    </div>
                    <div class="row">
                        <label>สำนัก/หมวด (source_label)</label>
                        <input type="text" name="source_label" value="<?php echo htmlspecialchars((string) ($row->source_label ?? ''), ENT_QUOTES); ?>" required>
                        <div class="muted" style="margin-top:6px;">ตัวอย่าง: กาลโยค, ดิถี/ฤกษ์, ชนิดวัน</div>
                    </div>
                </div>

                <div class="row">
                    <label>คำอธิบายสั้น (short_description)</label>
                    <textarea name="short_description" required><?php echo htmlspecialchars((string) ($row->short_description ?? ''), ENT_QUOTES); ?></textarea>
                    <div class="muted" style="margin-top:6px;">แนะนำไม่เกิน 1-2 บรรทัด เพื่อแสดงใน bottom sheet ได้สวย</div>
                </div>

                <div class="row">
                    <label>สถานะ</label>
                    <label class="checkbox">
                        <input type="checkbox" name="is_active" value="1" <?php echo ((int) ($row->is_active ?? 1) === 1) ? 'checked' : ''; ?>>
                        <span>เปิดใช้งาน (is_active = 1)</span>
                    </label>
                </div>

                <div class="actions">
                    <button class="btn btn-primary" type="submit"><i class="fa-solid fa-floppy-disk"></i> บันทึก</button>
                    <a class="btn btn-secondary" href="/web/admin/rengyam-tags"><i class="fa-solid fa-arrow-left"></i> กลับรายการ</a>
                </div>
            </form>
        </div>
    </div>
</body>

</html>

