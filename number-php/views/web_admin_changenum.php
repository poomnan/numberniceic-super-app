<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>จัดการขั้นตอนการเปลี่ยนแปลง - Admin Panel</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f0f2f5;
            margin: 0;
            padding: 0;
        }

        .container {
            max-width: 1000px;
            margin: 2rem auto;
            padding: 2rem;
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
        }

        h1 {
            color: #333;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 2px solid #20c997;
            padding-bottom: 1rem;
            margin-top: 0;
        }

        .btn {
            display: inline-block;
            padding: 0.6rem 1.2rem;
            border-radius: 6px;
            text-decoration: none;
            font-weight: 600;
            cursor: pointer;
            border: none;
            transition: all 0.2s;
        }

        .btn-edit {
            background: #20c997;
            color: white;
            font-size: 0.85rem;
            box-shadow: 0 2px 4px rgba(32, 201, 151, 0.2);
        }

        .btn-edit:hover {
            background: #1ba87e;
            transform: translateY(-1px);
        }

        .btn-back {
            background: #6c757d;
            color: white;
            display: inline-flex;
            align-items: center;
            margin-top: 1.5rem;
        }

        .btn-back:hover {
            background: #5a6268;
        }

        table {
            width: 100%;
            border-collapse: separate;
            border-spacing: 0;
            margin-top: 1.5rem;
            border-radius: 8px;
            overflow: hidden;
            border: 1px solid #eaeaea;
        }

        th,
        td {
            padding: 1.2rem 1rem;
            text-align: left;
            border-bottom: 1px solid #eaeaea;
        }

        th {
            background: #f8f9fa;
            color: #555;
            font-weight: 600;
            text-transform: uppercase;
            font-size: 0.85rem;
            letter-spacing: 0.5px;
        }

        tr:last-child td {
            border-bottom: none;
        }

        tr:hover {
            background: #fdfdfd;
        }

        .code-name {
            background: #f0f2f5;
            padding: 4px 8px;
            border-radius: 4px;
            font-family: monospace;
            color: #0d6efd;
            font-size: 0.9em;
        }

        .title-text {
            font-weight: 600;
            color: #2c3e50;
            font-size: 1.05em;
        }

        @media (max-width: 768px) {
            .container {
                margin: 1rem;
                padding: 1rem;
            }

            th,
            td {
                padding: 0.8rem 0.5rem;
            }

            h1 {
                font-size: 1.5rem;
                flex-direction: column;
                align-items: flex-start;
                gap: 1rem;
            }
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container">
        <h1>🏗️ จัดการขั้นตอนการเปลี่ยนแปลง</h1>

        <table>
            <thead>
                <tr>
                    <th width="5%">#</th>
                    <th width="20%">รหัสไฟล์ (Key)</th>
                    <th width="50%">หัวข้อแสดงผล (Title)</th>
                    <th width="15%">อัปเดตล่าสุด</th>
                    <th width="10%" style="text-align: center;">จัดการ</th>
                </tr>
            </thead>
            <tbody>
                <?php $i = 1;
                foreach ($items as $item): ?>
                    <tr>
                        <td>
                            <?php echo $i++; ?>
                        </td>
                        <td><span class="code-name">
                                <?php echo htmlspecialchars($item->file_name); ?>
                            </span></td>
                        <td><span class="title-text">
                                <?php echo htmlspecialchars($item->title); ?>
                            </span></td>
                        <td style="color: #888; font-size: 0.85em;">
                            <?php echo date('d/m/Y H:i', strtotime($item->updated_at)); ?>
                        </td>
                        <td style="text-align: center;">
                            <a href="/web/admin/changenum/edit/<?php echo urlencode($item->file_name); ?>"
                                class="btn btn-edit">📝 แก้ไข</a>
                        </td>
                    </tr>
                <?php endforeach; ?>
                <?php if (empty($items)): ?>
                    <tr>
                        <td colspan="5" style="text-align:center; padding: 3rem; color: #888;">ไม่พบข้อมูลในตาราง
                            changenum_contents</td>
                    </tr>
                <?php endif; ?>
            </tbody>
        </table>

        <a href="/web/dashboard" class="btn btn-back">⬅ กลับไปหน้า Dashboard</a>
    </div>
</body>

</html>