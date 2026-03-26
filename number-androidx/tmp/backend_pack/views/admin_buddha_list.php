<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>จัดการพระปางต่างๆ - NumberNice Admin</title>
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
            margin-bottom: 20px;
        }

        .btn-gold {
            background: linear-gradient(45deg, #FFD700, #FFC107);
            color: #000;
            font-weight: bold;
            border: none;
        }

        .buddha-img {
            width: 80px;
            height: 120px;
            object-fit: cover;
            border-radius: 8px;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h2>☸️ จัดการพระพุทธรูปปางต่างๆ</h2>
                <p class="text-muted">จัดการข้อมูลพระปางต่างๆ ชื่อปาง และรายละเอียดประจำวัน</p>
            </div>
            <a href="/admin/buddha/add" class="btn btn-gold btn-lg"><i class="fas fa-plus"></i> เพิ่มปางใหม่</a>
        </div>

        <?php if (isset($status)): ?>
            <div class="alert alert-<?= $status['type'] == 'success' ? 'success' : 'danger' ?> alert-dismissible fade show"
                role="alert">
                <?= htmlspecialchars($status['message']) ?>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        <?php endif; ?>

        <div class="card overflow-hidden">
            <table class="table table-hover mb-0">
                <thead class="bg-light text-center">
                    <tr>
                        <th style="width: 100px;">รูปภาพ</th>
                        <th>ปาง</th>
                        <th>ประจำวัน</th>
                        <th>รายละเอียดเบื้องต้น</th>
                        <th style="width: 150px;">การจัดการ</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($items)): ?>
                        <tr>
                            <td colspan="5" class="text-center py-5 text-muted">ไม่พบข้อมูลพระปางต่างๆ</td>
                        </tr>
                    <?php else: ?>
                        <?php foreach ($items as $item):
                            $days = [
                                1 => 'อาทิตย์',
                                2 => 'จันทร์',
                                3 => 'อังคาร',
                                4 => 'พุธ (กลางวัน)',
                                5 => 'พฤหัสบดี',
                                6 => 'ศุกร์',
                                7 => 'เสาร์',
                                8 => 'พุธ (กลางคืน)'
                            ];
                            $dayName = $days[$item->buddha_day] ?? 'ทั่วไป';
                            ?>
                            <tr class="align-middle">
                                <td class="text-center">
                                    <?php if ($item->image_url): ?>
                                        <img src="<?= htmlspecialchars($item->image_url) ?>" class="buddha-img shadow-sm" alt="">
                                    <?php else: ?>
                                        <div class="bg-light d-flex align-items-center justify-content-center buddha-img">
                                            <i class="fas fa-image text-muted"></i>
                                        </div>
                                    <?php endif; ?>
                                </td>
                                <td><strong>
                                        <?= htmlspecialchars($item->pang_name) ?>
                                    </strong></td>
                                <td class="text-center">
                                    <span class="badge rounded-pill bg-info text-dark">
                                        <?= $dayName ?>
                                    </span>
                                </td>
                                <td>
                                    <div class="text-truncate" style="max-width: 400px;">
                                        <?= htmlspecialchars(mb_substr($item->description, 0, 100)) ?>...
                                    </div>
                                </td>
                                <td class="text-center">
                                    <a href="/admin/buddha/edit/<?= $item->id ?>" class="btn btn-sm btn-outline-primary"><i
                                            class="fas fa-edit"></i></a>
                                    <a href="/admin/buddha/delete/<?= $item->id ?>" class="btn btn-sm btn-outline-danger"
                                        onclick="return confirm('ยืนยันการลบ?')"><i class="fas fa-trash"></i></a>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>

</html>