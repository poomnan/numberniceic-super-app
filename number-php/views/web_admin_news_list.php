<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage News - Numberniceic Admin</title>
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

        .btn-success {
            background-color: #198754;
        }

        .btn-danger {
            background-color: #dc3545;
        }

        .btn:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }

        .news-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
            overflow: hidden;
        }

        /* Desktop Table Styles */
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

        .img-thumb {
            width: 60px;
            height: 40px;
            object-fit: cover;
            border-radius: 4px;
            background: #eee;
            display: block;
        }

        .fix-badge {
            background: #fff3cd;
            color: #856404;
            padding: 2px 6px;
            border-radius: 4px;
            font-weight: bold;
            border: 1px solid #ffeeba;
            font-size: 0.85rem;
        }

        .cat-badge {
            background: #f0f2f5;
            color: #555;
            padding: 2px 8px;
            border-radius: 10px;
            font-size: 0.75rem;
        }

        /* Mobile Specific Styles */
        @media (max-width: 768px) {
            .header-box {
                flex-direction: column;
                gap: 1rem;
                text-align: center;
                padding: 1rem;
            }

            .header-box h1 {
                font-size: 1.2rem;
            }

            /* Force table to not be a table */
            .admin-table,
            .admin-table thead,
            .admin-table tbody,
            .admin-table th,
            .admin-table td,
            .admin-table tr {
                display: block;
            }

            /* Hide table headers (but not display: none;, for accessibility) */
            .admin-table thead tr {
                position: absolute;
                top: -9999px;
                left: -9999px;
            }

            .admin-table tr {
                border-bottom: 8px solid #f0f2f5;
                padding: 15px;
                position: relative;
            }

            .admin-table td {
                border: none;
                padding: 5px 0;
                position: relative;
                padding-left: 40%;
                text-align: left;
                min-height: 30px;
            }

            /* Labels for mobile */
            .admin-table td:before {
                position: absolute;
                left: 0;
                width: 35%;
                padding-right: 10px;
                white-space: nowrap;
                font-weight: 600;
                color: #888;
                content: attr(data-label);
            }

            /* Custom handling for Image and Headline to make them look better */
            .admin-table td.td-image,
            .admin-table td.td-headline {
                padding-left: 0;
            }

            .admin-table td.td-image:before,
            .admin-table td.td-headline:before {
                display: none;
            }

            .admin-table td.td-image {
                float: left;
                width: 80px;
                min-height: 60px;
            }

            .admin-table td.td-headline {
                margin-left: 90px;
                padding-bottom: 10px;
            }

            .admin-table td.td-actions {
                padding-left: 0;
                text-align: right;
                border-top: 1px solid #eee;
                margin-top: 10px;
                padding-top: 10px;
            }

            .admin-table td.td-actions:before {
                display: none;
            }

            .btn {
                padding: 8px 15px;
                font-size: 0.85rem;
            }
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <div class="header-box">
            <h1>📰 จัดการข่าวและบทความ</h1>
            <a href="/web/admin/news/create" class="btn btn-success">+ เพิ่มข่าวใหม่</a>
        </div>

        <div class="news-card">
            <table class="admin-table">
                <thead>
                    <tr>
                        <th width="60">ID</th>
                        <th width="100">Pos</th>
                        <th width="80">รูป</th>
                        <th>หัวข้อ</th>
                        <th width="120">วันที่</th>
                        <th width="150" style="text-align:right;">จัดการ</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (isset($newsList) && count($newsList) > 0): ?>
                        <?php foreach ($newsList as $news): ?>
                            <tr>
                                <td data-label="ID"><?php echo $news->newsid; ?></td>
                                <td data-label="Position">
                                    <?php if ($news->fix > 0): ?>
                                        <span class="fix-badge" title="ลำดับการปักหมุด/ดันข่าว">#<?php echo $news->fix; ?></span>
                                    <?php else: ?>
                                        <span style="color:#ccc;">-</span>
                                    <?php endif; ?>
                                </td>
                                <td class="td-image">
                                    <?php if (!empty($news->news_pic_header)): ?>
                                        <img src="<?php echo $news->news_pic_header; ?>" class="img-thumb">
                                    <?php else: ?>
                                        <div class="img-thumb"></div>
                                    <?php endif; ?>
                                </td>
                                <td class="td-headline">
                                    <div style="font-weight:600; margin-bottom: 4px;">
                                        <?php echo htmlspecialchars($news->news_headline); ?>
                                    </div>
                                    <span class="cat-badge">
                                        <?php echo htmlspecialchars($news->category_name ?? 'N/A'); ?>
                                    </span>
                                </td>
                                <td data-label="Date" class="hide-mobile">
                                    <?php echo date('d/m/Y', strtotime($news->news_date)); ?>
                                </td>
                                <td class="td-actions">
                                    <div style="display: flex; gap: 8px; justify-content: flex-end;">
                                        <a href="/web/admin/news/edit/<?php echo $news->newsid; ?>"
                                            class="btn btn-primary btn-sm">แก้ไข</a>
                                        <a href="/web/admin/news/delete/<?php echo $news->newsid; ?>"
                                            onclick="return confirm('คุณแน่ใจหรือไม่ว่าต้องการลบข่าวนี้?');"
                                            class="btn btn-danger btn-sm">ลบ</a>
                                    </div>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php else: ?>
                        <tr>
                            <td colspan="6" style="text-align:center; padding: 3rem; color: #888;">ไม่พบข้อมูลข่าว</td>
                        </tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>
</body>

</html>