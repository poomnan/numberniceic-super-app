<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <title>Manage License Plates - Admin Panel</title>
    <style>
        body {
            font-family: sans-serif;
            background: #f4f7f6;
            margin: 0;
        }

        .container {
            max-width: 1000px;
            margin: 2rem auto;
            padding: 2rem;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }

        h1 {
            color: #333;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .btn {
            padding: 0.6rem 1.2rem;
            border-radius: 4px;
            text-decoration: none;
            font-weight: bold;
            cursor: pointer;
            border: none;
        }

        .btn-add {
            background: #28a745;
            color: white;
        }

        .btn-edit {
            background: #ffc107;
            color: #212529;
            font-size: 0.8rem;
        }

        .btn-delete {
            background: #dc3545;
            color: white;
            font-size: 0.8rem;
        }

        .btn-back {
            background: #6c757d;
            color: white;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1.5rem;
        }

        th,
        td {
            padding: 1rem;
            text-align: left;
            border-bottom: 1px solid #eee;
        }

        th {
            background: #f8f9fa;
            color: #666;
            text-transform: uppercase;
            font-size: 0.85rem;
        }

        tr:hover {
            background: #fafafa;
        }

        .price {
            font-weight: bold;
            color: #28a745;
        }

        .status {
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            font-size: 0.8rem;
        }

        .status-available {
            background: #d4edda;
            color: #155724;
        }

        .status-sold {
            background: #f8d7da;
            color: #721c24;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container">
        <h1>
            จัดการป้ายทะเบียน
            <a href="/web/admin/tabians/create" class="btn btn-add">+ เพิ่มป้ายทะเบียน</a>
        </h1>

        <table id="tabianTable">
            <thead>
                <tr>
                    <th>ลำดับ</th>
                    <th>Order</th>
                    <th>เลขทะเบียน</th>
                    <th>จังหวัด</th>
                    <th>หมวดหมู่</th>
                    <th>ราคา</th>
                    <th>สถานะ</th>
                    <th>จัดการ</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($tabians as $index => $item): ?>
                    <tr>
                        <td>
                            <?php echo $index + 1; ?>
                        </td>
                        <td>
                            <span style="background:#eee; padding:2px 6px; border-radius:4px; font-size:0.8rem;">
                                <?php echo $item->order_no ?? 0; ?>
                            </span>
                        </td>
                        <td><strong>
                                <?php echo htmlspecialchars($item->tabian_number); ?>
                            </strong></td>
                        <td>
                            <?php echo htmlspecialchars($item->tabian_province); ?>
                        </td>
                        <td>
                            <?php echo htmlspecialchars($item->tabian_category); ?>
                        </td>
                        <td class="price">
                            <?php echo number_format($item->tabian_price); ?> ฿
                        </td>
                        <td>
                            <span class="status status-<?php echo strtolower($item->tabian_status); ?>">
                                <?php echo $item->tabian_status == 'available' ? 'ว่าง' : 'ขายแล้ว'; ?>
                            </span>
                        </td>
                        <td>
                            <a href="/web/admin/tabians/edit/<?php echo $item->tabian_id; ?>" class="btn btn-edit">แก้ไข</a>
                            <a href="/web/admin/tabians/delete/<?php echo $item->tabian_id; ?>" class="btn btn-delete"
                                onclick="return confirm('ยืนยันการลบ?')">ลบ</a>
                        </td>
                    </tr>
                <?php endforeach; ?>
                <?php if (empty($tabians)): ?>
                    <tr>
                        <td colspan="7" style="text-align:center; padding: 3rem; color: #888;">ไม่พบข้อมูลป้ายทะเบียน</td>
                    </tr>
                <?php endif; ?>
            </tbody>
        </table>

        <div style="margin-top: 2rem;">
            <a href="/web/dashboard" class="btn btn-back">กลับไปหน้า Dashboard</a>
        </div>
    </div>
</body>

</html>