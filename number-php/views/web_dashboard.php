<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Ananya</title>
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
            transition: transform 0.2s;
        }

        .card-header {
            background: linear-gradient(135deg, #198754, #20c997);
            color: white;
            padding: 2rem;
            position: relative;
        }

        .card-header h2 {
            margin: 0;
            font-size: 1.8rem;
            font-weight: 600;
        }

        .card-body {
            padding: 2.5rem;
        }

        .profile-info {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 2rem;
            margin-top: 1.5rem;
        }

        .info-group {
            display: flex;
            flex-direction: column;
        }

        .info-label {
            font-weight: 600;
            color: #888;
            font-size: 0.85rem;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 0.5rem;
        }

        .info-value {
            color: #333;
            font-size: 1.2rem;
            font-weight: 500;
        }

        .badge {
            display: inline-block;
            padding: 0.35rem 0.75rem;
            border-radius: 50px;
            font-size: 0.9rem;
            font-weight: 600;
            color: white;
            text-transform: capitalize;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .badge-success {
            background-color: #28a745;
        }

        .badge-info {
            background-color: #17a2b8;
        }

        .badge-warning {
            background-color: #ffc107;
            color: #212529;
        }

        .badge-danger {
            background-color: #dc3545;
        }

        .badge-secondary {
            background-color: #6c757d;
        }

        .welcome-msg {
            margin-bottom: 2rem;
            color: #666;
            font-size: 1.05rem;
            border-bottom: 1px solid #eee;
            padding-bottom: 1rem;
        }

        .user-id-badge {
            background: rgba(255, 255, 255, 0.2);
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 0.8rem;
            margin-left: 10px;
            vertical-align: middle;
        }

        /* Admin Menu Styles */
        .admin-section {
            border-top: 2px dashed #eee;
            margin-top: 2.5rem;
            padding-top: 1.5rem;
            animation: fadeIn 0.5s ease-in-out;
        }

        .admin-title {
            font-size: 1.2rem;
            font-weight: 600;
            color: #dc3545;
            /* Red to indicate admin area */
            margin-bottom: 1.5rem;
            display: flex;
            align-items: center;
        }

        .admin-menu-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
            gap: 1rem;
        }

        .admin-btn {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            background: #fff;
            border: 2px solid #eee;
            color: #555;
            padding: 1.5rem 1rem;
            border-radius: 12px;
            text-decoration: none;
            font-weight: 600;
            transition: all 0.2s;
            text-align: center;
            cursor: pointer;
        }

        .admin-btn:hover {
            border-color: #3CA7E6;
            color: #3CA7E6;
            background: #f0f7fb;
            transform: translateY(-3px);
            box-shadow: 0 4px 8px rgba(60, 167, 230, 0.15);
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(10px);
            }

            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <?php if ($user): ?>
            <div class="card">
                <div class="card-header">
                    <h2>
                        ยินดีต้อนรับ, <?php echo htmlspecialchars($user->realname); ?>!
                        <span class="user-id-badge">ID: <?php echo htmlspecialchars($user->memberid); ?></span>
                    </h2>
                </div>
                <div class="card-body">
                    <div class="welcome-msg">
                        ข้อมูลส่วนตัวของคุณ
                    </div>

                    <div class="profile-info">
                        <div class="info-group">
                            <div class="info-label">ชื่อ-นามสกุล</div>
                            <div class="info-value">
                                <?php echo htmlspecialchars($user->realname . ' ' . $user->surname); ?>
                            </div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">ชื่อผู้ใช้</div>
                            <div class="info-value">
                                <?php echo htmlspecialchars($user->username); ?>
                            </div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">สถานะบัญชี</div>
                            <div class="info-value">
                                <?php
                                $statusClass = 'badge-secondary';
                                $s = strtolower($user->status);
                                $statusText = $s;
                                if ($s === 'active' || $s === 'activie') {
                                    $statusClass = 'badge-success';
                                    $statusText = 'ใช้งานปกติ';
                                }
                                if ($s === 'banned') {
                                    $statusClass = 'badge-danger';
                                    $statusText = 'ถูกระงับ';
                                }
                                if ($s === 'pending') {
                                    $statusClass = 'badge-warning';
                                    $statusText = 'รออนุมัติ';
                                }
                                ?>
                                <span class="badge <?php echo $statusClass; ?>">
                                    <?php echo $statusText; ?>
                                </span>
                            </div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">ประเภทสมาชิก</div>
                            <div class="info-value">
                                <?php
                                $vipClass = 'badge-info';
                                $vipText = 'สมาชิกทั่วไป';
                                $v = strtolower($user->vipcode);
                                $isAdmin = ($v === 'admin' || $v === 'administrator');

                                if ($v === 'normal' || empty($v)) {
                                    $vipClass = 'badge-secondary';
                                    $vipText = 'สมาชิกทั่วไป';
                                } elseif ($isAdmin) {
                                    $vipClass = 'badge-danger';
                                    $vipText = 'ผู้ดูแลระบบ';
                                } else {
                                    $vipClass = 'badge-warning';
                                    $vipText = 'สมาชิก VIP (' . strtoupper($v) . ')';
                                }
                                ?>
                                <span class="badge <?php echo $vipClass; ?>">
                                    <?php echo htmlspecialchars($vipText); ?>
                                </span>
                            </div>
                        </div>

                        <?php if ($user->birthday): ?>
                            <div class="info-group">
                                <div class="info-label">วันเกิด</div>
                                <div class="info-value">
                                    <?php echo htmlspecialchars($user->birthday); ?>
                                </div>
                            </div>
                        <?php endif; ?>
                    </div>

                    <!-- ADMIN SECTION -->
                    <?php if ($isAdmin): ?>
                        <div class="admin-section">
                            <div class="admin-title">
                                🔧 ผู้ดูแลระบบ (Admin Panel)
                            </div>
                            <div class="admin-menu-grid">
                                <a href="/web/admin/users" class="admin-btn">
                                    จัดการผู้ใช้ระบบ
                                </a>
                                <a href="/web/admin/news" class="admin-btn">
                                    จัดการบทความ (News)
                                </a>
                                <a href="/web/admin/images" class="admin-btn">
                                    คลังรูปภาพ 📷
                                </a>

                                <a href="/admin/notifications/custom" class="admin-btn">
                                    ส่งการแจ้งเตือน 📢
                                </a>
                                <a href="#" class="admin-btn">
                                    จัดการเบอร์
                                </a>
                                <a href="/web/admin/tabians" class="admin-btn">
                                    จัดการป้ายทะเบียน
                                </a>
                                <a href="/web/admin/buddha" class="admin-btn">
                                    จัดการพระปางต่างๆ ☸️
                                </a>
                                <a href="/web/admin/temple" class="admin-btn">
                                    วัดเก่าวัดศักดิ์สิทธิ์ 🛕
                                </a>
                                <a href="/web/admin/spells" class="admin-btn">
                                    คาถาและคำเตือนพิเศษ ✨
                                </a>
                                <a href="/web/admin/merits" class="admin-btn" style="border-color:#ff9800; color:#ff9800;">
                                    วิธีทำบุญ (ปางพระ) 🙏
                                </a>
                                <a href="/web/admin/changenum" class="admin-btn" style="border-color:#20c997; color:#20c997;">
                                    ขั้นตอนการเปลี่ยน 🏗️
                                </a>

                                <a href="/web/admin/privileges" class="admin-btn"
                                    style="border-color:#6610f2; color:#6610f2; background: #f3f0ff;">
                                    จัดการสิทธิ์การใช้งาน 💎
                                </a>
                                <a href="/web/admin/rengyam-tags" class="admin-btn"
                                    style="border-color:#198754; color:#198754; background: #eef7f3;">
                                    จัดการความหมายฤกษ์ยาม 🏷️
                                </a>
                            </div>
                        </div>
                    <?php endif; ?>

                </div>
            </div>
        <?php else: ?>
            <div class="card">
                <div class="card-body">
                    <p>ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบอีกครั้ง</p>
                </div>
            </div>
        <?php endif; ?>
    </div>
</body>

</html>