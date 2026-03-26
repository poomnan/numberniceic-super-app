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
            background-color: #f0f2f5;
            font-family: 'Sarabun', sans-serif;
            color: #333;
        }

        .header-section {
            background: #fff;
            padding: 20px 0;
            margin-bottom: 25px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }

        .card {
            border-radius: 12px;
            border: none;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
            margin-bottom: 20px;
            transition: transform 0.2s;
        }

        .btn-gold {
            background: linear-gradient(45deg, #FFD700, #FFC107);
            color: #000;
            font-weight: bold;
            border: none;
            box-shadow: 0 3px 6px rgba(255, 215, 0, 0.3);
        }

        .btn-gold:hover {
            background: linear-gradient(45deg, #FFC107, #FFD700);
            transform: translateY(-2px);
        }

        .buddha-img {
            width: 70px;
            height: 100px;
            object-fit: cover;
            border-radius: 10px;
            border: 2px solid #fff;
        }

        /* Responsive Table to Cards */
        @media (max-width: 768px) {
            .table-responsive-stack thead {
                display: none;
            }
            .table-responsive-stack tr {
                display: block;
                background: white;
                margin-bottom: 15px;
                border-radius: 12px;
                padding: 15px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            }
            .table-responsive-stack td {
                display: flex;
                flex-direction: column;
                padding: 8px 0 !important;
                border: none;
                text-align: left !important;
            }
            .table-responsive-stack td::before {
                content: attr(data-label);
                font-weight: bold;
                color: #666;
                font-size: 0.8rem;
                text-transform: uppercase;
                margin-bottom: 4px;
            }
            .header-buttons {
                display: flex;
                flex-direction: column;
                gap: 10px;
                width: 100%;
            }
            .header-buttons a {
                width: 100%;
                margin: 0 !important;
            }
            .buddha-img {
                width: 100%;
                height: 250px;
                max-width: 200px;
                margin: 0 auto;
            }
            .action-btns {
                flex-direction: row !important;
                justify-content: center;
                gap: 20px;
                margin-top: 10px;
            }
        }

        .badge-day {
            padding: 8px 15px;
            font-size: 0.9rem;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="header-section">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-7 mb-3 mb-md-0 text-center text-md-start">
                    <h2 class="fw-bold"><i class="fas fa-dharmachakra text-success me-2"></i> จัดการพระพุทธรูปปางต่างๆ</h2>
                    <p class="text-muted mb-0">แสดงชื่อปาง ประจำวัน และรายละเอียดสำหรับสมาชิก</p>
                </div>
                <div class="col-md-5">
                    <div class="header-buttons">
                        <a href="/web/admin/buddha/assign" class="btn btn-outline-primary btn-lg"><i class="fas fa-user-check me-2"></i> มอบสิทธิ์</a>
                        <a href="/web/admin/buddha/add" class="btn btn-gold btn-lg"><i class="fas fa-plus-circle me-2"></i> เพิ่มปางใหม่</a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="container">
        <?php if (isset($status)): ?>
            <div class="alert alert-<?= $status['type'] == 'success' ? 'success' : 'danger' ?> alert-dismissible fade show shadow-sm rounded-3" role="alert">
                <i class="fas <?= $status['type'] == 'success' ? 'fa-check-circle' : 'fa-exclamation-circle' ?> me-2"></i>
                <?= htmlspecialchars($status['message']) ?>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        <?php endif; ?>

        <div class="card overflow-hidden">
            <div class="table-responsive">
                <table class="table table-hover mb-0 table-responsive-stack">
                    <thead class="table-light">
                        <tr class="text-center">
                            <th style="width: 100px;">รูปภาพ</th>
                            <th>ข้อมูลปาง</th>
                            <th>ประจำวัน</th>
                            <th>คำอธิบาย</th>
                            <th style="width: 130px;">จัดการ</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($items)): ?>
                            <tr>
                                <td colspan="5" class="text-center py-5">
                                    <div class="text-muted mb-3"><i class="fas fa-box-open fa-3x"></i></div>
                                    <h5 class="text-muted">ยังไม่มีข้อมูลพระพุทธรูปในระบบ</h5>
                                </td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($items as $item):
                                $days = [
                                    1 => ['name' => 'อาทิตย์', 'color' => '#dc3545'],
                                    2 => ['name' => 'จันทร์', 'color' => '#ffc107'],
                                    3 => ['name' => 'อังคาร', 'color' => '#e83e8c'],
                                    4 => ['name' => 'พุธ (กลางวัน)', 'color' => '#28a745'],
                                    5 => ['name' => 'พฤหัสบดี', 'color' => '#fd7e14'],
                                    6 => ['name' => 'ศุกร์', 'color' => '#007bff'],
                                    7 => ['name' => 'เสาร์', 'color' => '#6f42c1'],
                                    8 => ['name' => 'พุธ (กลางคืน)', 'color' => '#343a40']
                                ];
                                $dayData = $days[$item->buddha_day] ?? ['name' => 'ทั่วไป', 'color' => '#6c757d'];
                                ?>
                                <tr class="align-middle">
                                    <td class="text-center" data-label="รูปภาพ">
                                        <?php if ($item->image_url): ?>
                                            <img src="<?= htmlspecialchars($item->image_url) ?>" class="buddha-img shadow-sm mx-auto" alt="">
                                        <?php else: ?>
                                            <div class="bg-light d-flex align-items-center justify-content-center buddha-img mx-auto">
                                                <i class="fas fa-image text-muted fa-2x"></i>
                                            </div>
                                        <?php endif; ?>
                                    </td>
                                    <td data-label="ปาง">
                                        <div class="fw-bold text-primary fs-5"><?= htmlspecialchars($item->pang_name) ?></div>
                                    </td>
                                    <td class="text-center" data-label="ประจำวัน">
                                        <span class="badge badge-day" style="background-color: <?= $dayData['color'] ?>; color: #fff;">
                                            <i class="far fa-calendar-alt me-1"></i> <?= $dayData['name'] ?>
                                        </span>
                                    </td>
                                    <td data-label="คำอธิบาย">
                                        <div class="text-muted" style="max-height: 100px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;">
                                            <?= htmlspecialchars($item->description) ?>
                                        </div>
                                    </td>
                                    <td class="text-center" data-label="จัดการ">
                                        <div class="action-btns d-flex justify-content-center gap-2">
                                            <a href="/web/admin/buddha/edit/<?= $item->id ?>" class="btn btn-outline-primary shadow-sm"><i class="fas fa-edit"></i></a>
                                            <a href="/web/admin/buddha/delete/<?= $item->id ?>" class="btn btn-outline-danger shadow-sm" onclick="return confirm('คุณต้องการลบข้อมูลนี้ใช่หรือไม่?')"><i class="fas fa-trash"></i></a>
                                        </div>
                                    </td>
                                </tr>
                            <?php endforeach; ?>
                        <?php endif; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>

</html>