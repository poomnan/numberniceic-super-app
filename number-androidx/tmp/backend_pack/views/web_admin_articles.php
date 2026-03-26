<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Article Management - Ananya</title>
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

        .btn-secondary {
            background-color: #6c757d;
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

        .btn:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }

        .table-responsive {
            overflow-x: auto;
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

        .table tr:hover {
            background-color: #fcfcfc;
        }

        .badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: 600;
            color: white;
        }

        .badge-success {
            background-color: #28a745;
        }

        .badge-secondary {
            background-color: #6c757d;
        }

        .action-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <div class="card">
            <div class="card-header">
                <h2>จัดการบทความ (Articles)</h2>
                <a href="/web/dashboard" class="btn btn-white">
                    &larr; กลับ Dashboard
                </a>
            </div>
            <div class="card-body">
                <?php include 'web_admin_toolbar.php'; ?>
                <div class="action-bar">
                    <div><strong>รายการบทความทั้งหมด</strong> (<?php echo count($articles); ?>)</div>
                    <a href="/web/admin/articles/create" class="btn btn-primary">+ เขียนบทความใหม่</a>
                </div>

                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th width="50">ID</th>
                                <th>ชื่อบทความ / Slug</th>
                                <th>หมวดหมู่</th>
                                <th width="100">สถานะ</th>
                                <th width="100">วันที่</th>
                                <th width="150">จัดการ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php foreach ($articles as $art): ?>
                                <tr>
                                    <td><?php echo $art->art_id; ?></td>
                                    <td>
                                        <div style="font-weight: 600; color: #333; margin-bottom: 2px;">
                                            <?php echo htmlspecialchars($art->title); ?>
                                        </div>
                                        <div style="font-size: 0.8rem; color: #888;">
                                            /<?php echo htmlspecialchars($art->slug); ?>
                                        </div>
                                    </td>
                                    <td>
                                        <span
                                            style="background:#eef; color:#448; padding:2px 6px; border-radius:4px; font-size:0.8rem;">
                                            <?php echo htmlspecialchars($art->category); ?>
                                        </span>
                                    </td>
                                    <td>
                                        <?php if ($art->is_published): ?>
                                            <span class="badge badge-success">เผยแพร่</span>
                                        <?php else: ?>
                                            <span class="badge badge-secondary">ฉบับร่าง</span>
                                        <?php endif; ?>
                                    </td>
                                    <td style="font-size:0.85rem; color:#666;">
                                        <?php echo date('d/m/Y', strtotime($art->published_at)); ?>
                                    </td>
                                    <td>
                                        <a href="/web/admin/articles/<?php echo $art->art_id; ?>"
                                            class="btn btn-warning btn-sm">แก้ไข</a>
                                        <a href="/web/admin/confirm-delete/<?php echo $art->art_id; ?>"
                                            class="btn btn-danger btn-sm">
                                            ลบ
                                        </a>
                                    </td>
                                </tr>
                            <?php endforeach; ?>

                            <?php if (empty($articles)): ?>
                                <tr>
                                    <td colspan="6" style="text-align: center; padding: 3rem; color: #999;">
                                        ยังไม่มีบทความในระบบ
                                    </td>
                                </tr>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</body>

</html>