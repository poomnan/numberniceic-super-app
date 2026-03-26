<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Member - Numbernice Admin</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
            padding: 0;
        }

        .main-wrapper {
            max-width: 800px;
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

        .form-group {
            margin-bottom: 1.5rem;
        }

        .form-label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 600;
            color: #444;
        }

        .form-control {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 1rem;
            box-sizing: border-box;
        }

        .form-control:focus {
            outline: none;
            border-color: #198754;
        }

        .btn {
            padding: 10px 20px;
            border-radius: 6px;
            text-decoration: none;
            color: white;
            border: none;
            cursor: pointer;
            font-size: 1rem;
            display: inline-block;
        }

        .btn-primary {
            background-color: #0d6efd;
        }

        .btn-white {
            background-color: white;
            color: #198754;
            font-weight: 600;
        }

        .grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
        }

        @media (max-width: 600px) {
            .grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <form action="/web/admin/users/update/<?php echo $member->memberid; ?>" method="POST">
            <div class="card">
                <div class="card-header">
                    <h2>แก้ไขข้อมูลสมาชิก (Edit Member)</h2>
                    <a href="/web/admin/users" class="btn btn-white">กลับ</a>
                </div>
                <div class="card-body">

                    <div class="grid">
                        <div class="form-group">
                            <label class="form-label">ชื่อจริง (Real Name)</label>
                            <input type="text" name="realname" class="form-control" required
                                value="<?php echo htmlspecialchars($member->realname); ?>">
                        </div>
                        <div class="form-group">
                            <label class="form-label">นามสกุล (Surname)</label>
                            <input type="text" name="surname" class="form-control" required
                                value="<?php echo htmlspecialchars($member->surname); ?>">
                        </div>
                    </div>

                    <div class="grid">
                        <div class="form-group">
                            <label class="form-label">Username</label>
                            <input type="text" name="username" class="form-control" required
                                value="<?php echo htmlspecialchars($member->username); ?>">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Password</label>
                            <input type="text" name="password" class="form-control" required
                                value="<?php echo htmlspecialchars($member->password); ?>">
                        </div>
                    </div>

                    <div class="grid">
                        <div class="form-group">
                            <label class="form-label">Vip Code</label>
                            <select name="vipcode" class="form-control" required>
                                <option value="normal" <?php echo ($member->vipcode == 'normal') ? 'selected' : ''; ?>>Normal</option>
                                <option value="vip" <?php echo ($member->vipcode == 'vip') ? 'selected' : ''; ?>>VIP
                                </option>
                                <option value="vvip" <?php echo ($member->vipcode == 'vvip') ? 'selected' : ''; ?>>VVIP
                                </option>
                                <option value="admin" <?php echo ($member->vipcode == 'admin') ? 'selected' : ''; ?>>Admin</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">สถานะ (Status)</label>
                            <select name="status" class="form-control" required>
                                <option value="activie" <?php echo (strtolower($member->status) == 'activie' || strtolower($member->status) == 'active') ? 'selected' : ''; ?>>Active (ใช้งานปกติ)
                                </option>
                                <option value="inactive" <?php echo (strtolower($member->status) == 'inactive') ? 'selected' : ''; ?>>Inactive (ปิดการใช้งาน)</option>
                            </select>
                        </div>
                    </div>

                    <div style="text-align: right; margin-top: 2rem;">
                        <button type="submit" class="btn btn-primary"
                            style="font-size: 1.1rem; padding: 12px 30px;">บันทึกการแก้ไข (Update)</button>
                    </div>
                </div>
            </div>
        </form>
    </div>
</body>

</html>