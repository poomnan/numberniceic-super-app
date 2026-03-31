<?php
/** @var object $user */
/** @var array $rows */
/** @var string $q */
?>
<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>จัดการคำอธิบายฤกษ์ยาม - Admin</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        body {
            font-family: Arial, sans-serif;
            background: #f6f8fb;
            margin: 0;
            padding: 20px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
        }

        .card {
            background: #fff;
            border-radius: 10px;
            padding: 18px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        }

        .header {
            display: flex;
            gap: 12px;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            margin-bottom: 12px;
        }

        .title {
            font-size: 20px;
            font-weight: 700;
            color: #1f2937;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .subtitle {
            color: #6b7280;
            font-size: 13px;
            margin-top: 6px;
        }

        .actions {
            display: flex;
            gap: 10px;
            align-items: center;
            flex-wrap: wrap;
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
            font-weight: 700;
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

        .search {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .search input {
            padding: 10px 12px;
            border: 1px solid #d1d5db;
            border-radius: 8px;
            min-width: 260px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
        }

        th,
        td {
            border-bottom: 1px solid #eef2f7;
            padding: 12px;
            text-align: left;
            vertical-align: top;
        }

        th {
            background: #f8fafc;
            color: #334155;
            font-size: 13px;
            position: sticky;
            top: 0;
            z-index: 1;
        }

        td {
            font-size: 14px;
            color: #111827;
        }

        .badge {
            display: inline-block;
            padding: 4px 10px;
            border-radius: 999px;
            font-size: 12px;
            font-weight: 700;
        }

        .badge-on {
            background: #dcfce7;
            color: #166534;
        }

        .badge-off {
            background: #fee2e2;
            color: #991b1b;
        }

        .muted {
            color: #6b7280;
            font-size: 12px;
        }

        .desc {
            color: #374151;
            line-height: 1.4;
        }

        .row-actions {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
        }
    </style>
</head>

<body>
    <div class="container">
        <?php include 'web_admin_toolbar.php'; ?>

        <div class="card">
            <div class="header">
                <div>
                    <div class="title">
                        <i class="fa-solid fa-book-open"></i>
                        จัดการคำอธิบายฤกษ์ยาม (Rengyam Tag Meanings)
                    </div>
                    <div class="subtitle">
                        แก้ชื่อแท็ก/สำนัก/คำอธิบายผ่านตาราง <code>rengyam_tag_meanings</code> เพื่อให้แอปแสดงผลอัปเดตทันที (ไม่ต้องแก้โค้ด)
                    </div>
                </div>

                <div class="actions">
                    <form class="search" method="GET" action="/web/admin/rengyam-tags">
                        <input type="text" name="q" placeholder="ค้นหาแท็ก/สำนัก/คำอธิบาย" value="<?php echo htmlspecialchars($q ?? '', ENT_QUOTES); ?>">
                        <button class="btn btn-secondary" type="submit"><i class="fa-solid fa-magnifying-glass"></i> ค้นหา</button>
                        <?php if (!empty($q)): ?>
                            <a class="btn btn-secondary" href="/web/admin/rengyam-tags"><i class="fa-solid fa-rotate-left"></i> ล้าง</a>
                        <?php endif; ?>
                    </form>
                    <a class="btn btn-primary" href="/web/admin/rengyam-tags/create"><i class="fa-solid fa-plus"></i> เพิ่มแท็ก</a>
                    <a class="btn btn-secondary" href="/web/dashboard"><i class="fa-solid fa-gauge-high"></i> กลับแดชบอร์ด</a>
                </div>
            </div>

            <div class="muted">รายการทั้งหมด: <?php echo is_array($rows) ? count($rows) : 0; ?></div>

            <div style="overflow:auto; margin-top:10px;">
                <table>
                    <thead>
                        <tr>
                            <th style="min-width:70px;">ID</th>
                            <th style="min-width:220px;">ชื่อแท็ก</th>
                            <th style="min-width:140px;">สำนัก/หมวด</th>
                            <th>คำอธิบาย</th>
                            <th style="min-width:110px;">สถานะ</th>
                            <th style="min-width:210px;">จัดการ</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (!is_array($rows) || empty($rows)): ?>
                            <tr>
                                <td colspan="6" class="muted">ไม่พบข้อมูล</td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($rows as $r): ?>
                                <tr>
                                    <td class="muted"><?php echo (int) ($r->id ?? 0); ?></td>
                                    <td><strong><?php echo htmlspecialchars((string) ($r->tag_name ?? ''), ENT_QUOTES); ?></strong></td>
                                    <td><?php echo htmlspecialchars((string) ($r->source_label ?? ''), ENT_QUOTES); ?></td>
                                    <td class="desc"><?php echo htmlspecialchars((string) ($r->short_description ?? ''), ENT_QUOTES); ?></td>
                                    <td>
                                        <?php if ((int) ($r->is_active ?? 0) === 1): ?>
                                            <span class="badge badge-on">ใช้งาน</span>
                                        <?php else: ?>
                                            <span class="badge badge-off">ปิด</span>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        <div class="row-actions">
                                            <a class="btn btn-secondary" href="/web/admin/rengyam-tags/edit/<?php echo (int) ($r->id ?? 0); ?>"><i class="fa-solid fa-pen-to-square"></i> แก้ไข</a>
                                            <a class="btn btn-secondary" href="/web/admin/rengyam-tags/toggle/<?php echo (int) ($r->id ?? 0); ?>"
                                                onclick="return confirm('ยืนยันสลับสถานะการใช้งาน?');">
                                                <i class="fa-solid fa-toggle-on"></i> สลับสถานะ
                                            </a>
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
</body>

</html>

