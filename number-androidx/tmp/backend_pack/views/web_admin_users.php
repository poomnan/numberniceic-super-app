<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Management - Admin</title>
    <link href="https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;500&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Kanit', sans-serif;
            background-color: #f3f4f6;
            margin: 0;
            padding: 0;
        }

        .container {
            max-width: 80rem;
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

        .btn {
            padding: 0.5rem 1rem;
            border-radius: 0.25rem;
            font-weight: 500;
            cursor: pointer;
            border: none;
            text-decoration: none;
            display: inline-block;
        }

        .btn-success {
            background-color: #10b981;
            color: white;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }

        th {
            background-color: #f9fafb;
            padding: 0.75rem 1rem;
            text-align: left;
            border-bottom: 2px solid #e5e7eb;
        }

        td {
            padding: 1rem;
            border-bottom: 1px solid #e5e7eb;
        }

        .badge {
            background-color: #d1fae5;
            color: #065f46;
            padding: 0.25rem 0.6rem;
            border-radius: 9999px;
            font-size: 0.75rem;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container">
        <div class="card">
            <h1>จัดการสมาชิก (User Management)</h1>
            <p>กรุณาใช้งานหน้า <a href="/web/admin/bag-colors">จัดการสีกระเป๋า</a> โดยตรงสำหรับจัดการข้อมูลสีกระเป๋า</p>

            <div class="alert alert-info"
                style="background:#e0f2fe; padding:1rem; border-radius:8px; border:1px solid #bae6fd; color:#0369a1;">
                หน้าจอนี้ยังอยู่ระหว่างการพัฒนา หากท่านต้องการค้นหาสมาชิกและจัดการสีกระเป๋า ให้ใช้เมนู
                <strong>"จัดการสีกระเป๋า"</strong> จากหน้าแดชบอร์ด
            </div>

            <a href="/web/dashboard" class="btn"
                style="background:#6b7280; color:white; margin-top:1rem;">กลับหน้าแดชบอร์ด</a>
        </div>
    </div>
</body>

</html>