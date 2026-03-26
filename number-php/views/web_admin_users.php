<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Member Management - Numbernice Admin</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
            padding: 0;
        }

        .main-wrapper {
            max-width: 1200px;
            margin: 2rem auto;
            padding: 0 1rem;
        }

        .header-box {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 2rem;
            background: white;
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
        }

        .header-box h1 {
            margin: 0;
            font-size: 1.5rem;
            color: #333;
        }

        .search-box {
            margin-bottom: 2rem;
            display: flex;
            gap: 10px;
        }

        .search-input {
            flex: 1;
            padding: 10px 15px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 1rem;
        }

        .btn {
            padding: 10px 20px;
            border-radius: 6px;
            text-decoration: none;
            color: white;
            display: inline-block;
            font-weight: 500;
            transition: all 0.2s;
            border: none;
            cursor: pointer;
            font-size: 0.95rem;
        }

        .btn-primary {
            background-color: #0d6efd;
        }

        .btn-danger {
            background-color: #dc3545;
        }

        .btn-secondary {
            background-color: #6c757d;
        }

        .btn:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }

        .card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
            overflow: hidden;
        }

        .admin-table {
            width: 100%;
            border-collapse: collapse;
        }

        .admin-table thead tr {
            background-color: #f8f9fa;
            border-bottom: 1px solid #eee;
        }

        .admin-table th {
            padding: 15px;
            text-align: left;
            font-weight: 600;
            color: #555;
        }

        .admin-table td {
            padding: 15px;
            border-bottom: 1px solid #eee;
            vertical-align: middle;
        }

        .avatar-thumb {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            object-fit: cover;
            background: #eee;
        }

        .vip-badge {
            background: #fff3cd;
            color: #856404;
            padding: 2px 8px;
            border-radius: 4px;
            font-weight: bold;
            font-size: 0.75rem;
        }

        .status-badge {
            padding: 2px 8px;
            border-radius: 10px;
            font-size: 0.75rem;
            color: white;
        }

        .status-active {
            background-color: #198754;
        }

        .status-inactive {
            background-color: #6c757d;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <div class="header-box">
            <h1>👥 จัดการสมาชิก (Member Management)</h1>
            <a href="/web/dashboard" class="btn btn-secondary">กลับแดชบอร์ด</a>
        </div>

        <form method="GET" class="search-box">
            <input type="text" name="search" class="search-input" placeholder="ค้นหาด้วยชื่อ, นามสกุล หรือ Username..."
                value="<?php echo htmlspecialchars($search ?? ''); ?>">
            <button type="submit" class="btn btn-primary">ค้นหา</button>
            <?php if (!empty($search)): ?>
                <a href="/web/admin/users" class="btn btn-secondary">ล้างการค้นหา</a>
            <?php endif; ?>
        </form>

        <div class="card">
            <table class="admin-table">
                <thead>
                    <tr>
                        <th width="60">ID</th>

                        <th>ชื่อ-นามสกุล</th>
                        <th>Username</th>
                        <th>Password</th>
                        <th>Vip Code</th>
                        <th>สถานะ</th>
                        <th width="150" style="text-align:right;">จัดการ</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (isset($members) && count($members) > 0): ?>
                        <?php foreach ($members as $mem): ?>
                            <tr>
                                <td><?php echo $mem->memberid; ?></td>

                                <td>
                                    <div style="font-weight:600;">
                                        <?php echo htmlspecialchars($mem->realname . ' ' . $mem->surname); ?>
                                    </div>
                                </td>
                                <td><?php echo htmlspecialchars($mem->username); ?></td>
                                <td><code><?php echo htmlspecialchars($mem->password); ?></code></td>
                                <td>
                                    <span class="vip-badge"><?php echo htmlspecialchars($mem->vipcode ?? 'normal'); ?></span>
                                </td>
                                <td>
                                    <span
                                        class="status-badge <?php echo (strtolower($mem->status) == 'activie' || strtolower($mem->status) == 'active') ? 'status-active' : 'status-inactive'; ?>">
                                        <?php echo htmlspecialchars($mem->status); ?>
                                    </span>
                                </td>
                                <td style="text-align:right;">
                                    <a href="/web/admin/users/edit/<?php echo $mem->memberid; ?>" 
                                       class="btn btn-primary" style="padding: 5px 10px; font-size: 0.8rem; background-color: #ffc107; color: #000;">แก้ไข</a>
                                    <a href="/web/admin/users/delete/<?php echo $mem->memberid; ?>"
                                        onclick="return confirm('คุณแน่ใจหรือไม่ว่าต้องการลบสมาชิกท่านนี้? ข้อมูลทั้งหมดจะหายไปและกู้คืนไม่ได้');"
                                        class="btn btn-danger" style="padding: 5px 10px; font-size: 0.8rem;">ลบ</a>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php else: ?>
                        <tr>
                            <td colspan="8" style="text-align:center; padding: 3rem; color: #888;">ไม่พบข้อมูลสมาชิก</td>
                        </tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
        <p style="color:#888; font-size:0.9rem; margin-top:1rem;">* แสดงข้อมูลล่าสุด 100 รายการ</p>
    </div>
</body>

</html>