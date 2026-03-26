<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bag Colors Management - Admin</title>
    <link href="https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;500&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Kanit', sans-serif;
            background-color: #f3f4f6;
            margin: 0;
            padding: 0;
        }

        .container {
            max-width: 60rem;
            margin: 0 auto;
            padding: 2rem 1rem;
        }

        .card {
            background-color: white;
            border-radius: 0.5rem;
            box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
            padding: 1.5rem;
            margin-bottom: 1.5rem;
        }

        .search-box {
            display: flex;
            gap: 1rem;
            margin-bottom: 2rem;
        }

        input[type="text"] {
            flex: 1;
            padding: 0.5rem 1rem;
            border: 1px solid #d1d5db;
            border-radius: 0.25rem;
        }

        .btn {
            padding: 0.5rem 1rem;
            border-radius: 0.25rem;
            font-weight: 500;
            cursor: pointer;
            border: none;
            text-decoration: none;
            display: inline-block;
        }

        .btn-primary {
            background-color: #3b82f6;
            color: white;
        }

        .btn-success {
            background-color: #10b981;
            color: white;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th {
            text-align: left;
            background-color: #f9fafb;
            padding: 0.75rem 1rem;
            border-bottom: 1px solid #e5e7eb;
        }

        td {
            padding: 1rem;
            border-bottom: 1px solid #e5e7eb;
        }

        .badge {
            background-color: #e5e7eb;
            padding: 0.25rem 0.5rem;
            border-radius: 9999px;
            font-size: 0.75rem;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container">
        <div class="card">
            <h1>จัดการสีกระเป๋า (Manage Bag Colors)</h1>
            <form action="/web/admin/bag-colors" method="GET" class="search-box">
                <input type="text" name="search" placeholder="ค้นหาชื่อ, นามสกุล, หรือ ID..."
                    value="<?= htmlspecialchars($search ?? '') ?>">
                <button type="submit" class="btn btn-primary">ค้นหา</button>
            </form>

            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>ชื่อ-นามสกุล</th>
                        <th>วันเกิด</th>
                        <th>FCM Token</th>
                        <th>จัดการ</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (!empty($users)): ?>
                        <?php foreach ($users as $user): ?>
                            <tr>
                                <td><span class="badge"><?= $user->memberid ?></span></td>
                                <td>
                                    <strong><?= htmlspecialchars($user->realname . ' ' . $user->surname) ?></strong>
                                    <div style="font-size: 0.8rem; color: #6b7280;"><?= htmlspecialchars($user->username) ?>
                                    </div>
                                </td>
                                <td><?= htmlspecialchars($user->birthday ?: '-') ?></td>
                                <td>
                                    <?php if (!empty($user->fcm_token)): ?>
                                        <span class="badge" style="background-color: #d1fae5; color: #065f46;">Token OK</span>
                                    <?php else: ?>
                                        <span class="badge" style="background-color: #fee2e2; color: #991b1b;">No Token</span>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <a href="/web/admin/bag-colors/edit/<?= $user->memberid ?>" class="btn btn-success">
                                        จัดการสี
                                    </a>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php else: ?>
                        <tr>
                            <td colspan="4" style="text-align: center; color: #6b7280;">ไม่พบข้อมูลสมาชิก</td>
                        </tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>
</body>

</html>