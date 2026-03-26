<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit User - Admin</title>
    <style>
        body {
            font-family: 'Segoe UI', sans-serif;
            background-color: #f0f2f5;
            margin: 0;
        }

        .main-wrapper {
            max-width: 600px;
            margin: 2rem auto;
            padding: 0 1rem;
        }

        .card {
            background: white;
            border-radius: 12px;
            padding: 2rem;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
        }

        h2 {
            margin-top: 0;
            color: #333;
            border-bottom: 1px solid #eee;
            padding-bottom: 1rem;
        }

        .form-group {
            margin-bottom: 1.5rem;
        }

        label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 600;
            color: #555;
        }

        input[type="text"],
        input[type="date"],
        select {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 1rem;
        }

        .btn-group {
            display: flex;
            gap: 1rem;
            margin-top: 2rem;
        }

        .btn {
            padding: 10px 20px;
            text-decoration: none;
            border-radius: 6px;
            font-weight: 600;
            border: none;
            cursor: pointer;
            font-size: 1rem;
        }

        .btn-save {
            background-color: #28a745;
            color: white;
        }

        .btn-cancel {
            background-color: #6c757d;
            color: white;
        }

        .readonly-field {
            background-color: #f8f9fa;
            color: #666;
            cursor: not-allowed;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="main-wrapper">
        <div class="card">
            <h2>แก้ไขผู้ใช้: <?php echo htmlspecialchars($editUser->username); ?></h2>
            <form method="post">
                <div class="form-group">
                    <label>ชื่อ-นามสกุล (แก้ไขไม่ได้)</label>
                    <input type="text" class="readonly-field"
                        value="<?php echo htmlspecialchars($editUser->realname . ' ' . $editUser->surname); ?>"
                        readonly>
                </div>

                <div class="form-group">
                    <label for="status">สถานะ</label>
                    <select name="status" id="status">
                        <option value="active" <?php if (strtolower($editUser->status) == 'active' || strtolower($editUser->status) == 'activie')
                            echo 'selected'; ?>>ใช้งานปกติ</option>
                        <option value="banned" <?php if (strtolower($editUser->status) == 'banned')
                            echo 'selected'; ?>>
                            ระงับการใช้งาน</option>
                        <option value="pending" <?php if (strtolower($editUser->status) == 'pending')
                            echo 'selected'; ?>>
                            รออนุมัติ</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="vipcode">ระดับสมาชิก (VIP Level)</label>
                    <select name="vipcode" id="vipcode">
                        <option value="normal" <?php if ($editUser->vipcode == 'normal')
                            echo 'selected'; ?>>สมาชิกทั่วไป
                            (Normal)</option>
                        <option value="vip" <?php if ($editUser->vipcode == 'vip')
                            echo 'selected'; ?>>VIP</option>
                        <option value="super" <?php if ($editUser->vipcode == 'super')
                            echo 'selected'; ?>>Super VIP
                        </option>
                        <option value="admin" <?php if ($editUser->vipcode == 'admin')
                            echo 'selected'; ?>>ผู้ดูแลระบบ
                            (Admin)</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="birthday">วันเกิด</label>
                    <input type="date" name="birthday" id="birthday"
                        value="<?php echo htmlspecialchars($editUser->birthday); ?>">
                </div>

                <div class="btn-group">
                    <button type="submit" class="btn btn-save">บันทึก</button>
                    <a href="/web/admin/users" class="btn btn-cancel">ยกเลิก</a>
                </div>
            </form>
        </div>
    </div>
</body>

</html>