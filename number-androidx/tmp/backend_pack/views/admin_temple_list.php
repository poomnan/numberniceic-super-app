<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>จัดการวัดศักดิ์สิทธิ์</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
            padding: 0;
        }

        .main-wrapper {
            max-width: 1000px;
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

        .btn {
            display: inline-block;
            padding: 8px 16px;
            border-radius: 6px;
            text-decoration: none;
            color: white;
            border: none;
            cursor: pointer;
            font-size: 0.9rem;
            font-weight: 500;
            transition: all 0.2s;
        }

        .btn-primary {
            background-color: #0d6efd;
        }

        .btn-warning {
            background-color: #ffc107;
            color: #212529;
        }

        .btn-danger {
            background-color: #dc3545;
        }

        .btn-white {
            background-color: white;
            color: #333;
        }

        .btn-sm {
            padding: 4px 10px;
            font-size: 0.8rem;
        }

        .table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }

        .table th,
        .table td {
            padding: 12px 15px;
            border-bottom: 1px solid #eee;
            text-align: left;
        }

        .table th {
            background-color: #f8f9fa;
            font-weight: 600;
            color: #555;
            text-transform: uppercase;
            font-size: 0.85rem;
        }

        .badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: 600;
            color: white;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="main-wrapper">
        <div class="card">
            <div class="card-header">
                <h2>จัดการวัดศักดิ์สิทธิ์</h2>
                <a href="/web/dashboard" class="btn btn-white">&larr; กลับ Dashboard</a>
            </div>
            <div class="card-body">
                <div style="display:flex; justify-content:space-between; margin-bottom:1rem;">
                    <div><strong>รายการทั้งหมด</strong> (<?= count($items) ?> วัด)</div>
                    <a href="/admin/temple/add" class="btn btn-primary">+ เพิ่มวัดใหม่</a>
                </div>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th width="50">ID</th>
                                <th width="100">รูปภาพ</th>
                                <th>ชื่อวัด</th>
                                <th>ที่อยู่</th>
                                <th width="150">จัดการ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if (empty($items)): ?>
                                <tr>
                                    <td colspan="5" style="text-align:center; padding:3rem; color:#999;">ไม่มีข้อมูล</td>
                                </tr>
                            <?php else: ?>
                                <?php foreach ($items as $item): ?>
                                    <tr>
                                        <td><?= $item->id ?></td>
                                        <td style="text-align:center;">
                                            <?php if (!empty($item->image_url)): ?>
                                                <img src="<?= $item->image_url ?>"
                                                    style="height: 60px; object-fit: cover; border-radius: 4px;">
                                            <?php else: ?>
                                                <span style="color:#ccc;">-</span>
                                            <?php endif; ?>
                                        </td>
                                        <td><?= htmlspecialchars($item->temple_name) ?></td>
                                        <td><?= htmlspecialchars($item->address ?? '-') ?></td>
                                        <td>
                                            <a href="/admin/temple/edit/<?= $item->id ?>"
                                                class="btn btn-warning btn-sm">แก้ไข</a>
                                            <a href="/admin/temple/delete/<?= $item->id ?>" class="btn btn-danger btn-sm"
                                                onclick="return confirm('ยืนยันการลบ?');">ลบ</a>
                                        </td>
                                    </tr>
                                <?php endforeach; ?>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</body>

</html>