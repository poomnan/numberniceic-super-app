<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Bag Colors - Admin</title>
    <link href="https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;500&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Kanit', sans-serif;
            background-color: #f3f4f6;
            margin: 0;
            padding: 0;
        }

        /* Navbar */
        .navbar {
            background-color: white;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
            padding: 1rem;
        }

        .nav-container {
            max-width: 80rem;
            margin: 0 auto;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .nav-brand {
            display: flex;
            align-items: center;
            text-decoration: none;
            color: #6b7280;
            font-size: 1.125rem;
            font-weight: 600;
        }

        .nav-brand img {
            height: 2rem;
            width: 2rem;
            margin-right: 0.5rem;
        }

        .nav-link {
            color: #10b981;
            text-decoration: none;
            font-weight: 500;
        }

        /* Layout */
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

        .flex-between {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .flex-center {
            display: flex;
            align-items: center;
        }

        .mb-4 {
            margin-bottom: 1rem;
        }

        .mr-3 {
            margin-right: 0.75rem;
        }

        /* Typography */
        h1 {
            font-size: 1.5rem;
            font-weight: 700;
            margin: 0;
        }

        h2 {
            font-size: 1.25rem;
            font-weight: 700;
            margin-bottom: 1rem;
        }

        .text-gray {
            color: #6b7280;
        }

        .text-green {
            color: #16a34a;
        }

        .text-blue {
            color: #2563eb;
        }

        .text-sm {
            font-size: 0.875rem;
        }

        .text-xs {
            font-size: 0.75rem;
        }

        .font-bold {
            font-weight: 700;
        }

        /* Buttons */
        .btn {
            padding: 0.5rem 1rem;
            border-radius: 0.25rem;
            font-weight: 700;
            cursor: pointer;
            border: none;
            transition: background-color 0.2s;
            color: white;
            display: inline-block;
            text-decoration: none;
        }

        .btn-sm {
            padding: 0.25rem 0.75rem;
            font-size: 0.875rem;
        }

        .btn-purple {
            background-color: #9333ea;
        }

        .btn-purple:hover {
            background-color: #7e22ce;
        }

        .btn-blue {
            background-color: #3b82f6;
        }

        .btn-blue:hover {
            background-color: #2563eb;
        }

        .btn-green {
            background-color: #22c55e;
        }

        .btn-green:hover {
            background-color: #16a34a;
        }

        .btn-edit {
            background-color: #dbeafe;
            color: #2563eb;
        }

        .btn-edit:hover {
            text-decoration: underline;
        }

        .w-full {
            width: 100%;
        }

        /* Forms */
        .form-grid {
            display: grid;
            grid-template-columns: repeat(1, 1fr);
            gap: 1rem;
            align-items: end;
        }

        @media (min-width: 768px) {
            .form-grid {
                grid-template-columns: repeat(2, 1fr);
            }
        }

        @media (min-width: 1024px) {
            .form-grid {
                grid-template-columns: repeat(6, 1fr);
            }
        }

        label {
            display: block;
            color: #374151;
            font-size: 0.875rem;
            font-weight: 700;
            margin-bottom: 0.5rem;
        }

        input[type="number"],
        input[type="text"] {
            width: 100%;
            padding: 0.5rem 0.75rem;
            border: 1px solid #d1d5db;
            border-radius: 0.25rem;
            color: #374151;
            box-sizing: border-box;
        }

        input[type="number"]:focus,
        input[type="text"]:focus {
            outline: 2px solid #3b82f6;
            border-color: transparent;
        }

        .color-input-group {
            display: flex;
        }

        input[type="color"] {
            height: 2.5rem;
            width: 3rem;
            border: none;
            background: transparent;
            cursor: pointer;
            padding: 0;
        }

        .color-text {
            margin-left: 0.5rem;
            font-size: 0.75rem;
        }

        /* Recommendation Box */
        .recommend-box {
            background-color: #fefce8;
            color: #854d0e;
            padding: 1rem;
            border: 1px solid #fef08a;
            border-radius: 0.25rem;
            margin-bottom: 1.5rem;
        }

        .rec-grid {
            display: grid;
            grid-template-columns: 1fr;
            gap: 1rem;
        }

        @media (min-width: 768px) {
            .rec-grid {
                grid-template-columns: 1fr 1fr;
            }
        }

        .gap-2 {
            gap: 0.5rem;
        }

        /* Table */
        .table-container {
            overflow-x: auto;
            background: white;
            border-radius: 0.25rem;
            box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
        }

        table {
            min-width: 100%;
            border-collapse: collapse;
        }

        th {
            background-color: #e5e7eb;
            text-align: left;
            padding: 0.75rem 1rem;
            font-weight: 600;
        }

        td {
            padding: 1rem;
            border-bottom: 1px solid #e5e7eb;
        }

        .color-circles {
            display: flex;
            gap: 0.5rem;
        }

        .circle {
            width: 2rem;
            height: 2rem;
            border-radius: 50%;
            border: 1px solid #e5e7eb;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
        }
    </style>
</head>

<body>

    <!-- Navbar -->
    <nav class="navbar">
        <div class="nav-container">
            <div class="flex-center">
                <a href="/web/dashboard" class="nav-brand">
                    <img src="/loggo.gif" alt="Logo">
                    <span>Admin Panel</span>
                </a>
            </div>
            <div>
                <a href="/web/admin/bag-colors" class="nav-link">Back to Search</a>
            </div>
        </div>
    </nav>

    <div class="container">
        <div class="card">
            <div class="flex-between mb-4">
                <div>
                    <h1 style="display:inline-block;" class="mr-3">จัดการสีกระเป๋า:
                        <?= htmlspecialchars($targetUser->realname) ?> (ID: <?= $targetUser->memberid ?>)
                    </h1>
                    <button onclick="sendNotification()" class="btn btn-sm btn-purple">
                        🔔 Send Notification
                    </button>
                </div>
                <span class="text-gray">เกิด: <?= $targetUser->birthday ?></span>
            </div>

            <!-- User Info / Age Helper -->
            <?php
            $currentAge = 0;
            if (!empty($targetUser->birthday)) {
                try {
                    $dob = new DateTime($targetUser->birthday);
                    $now = new DateTime();
                    $diff = $now->diff($dob);
                    $currentAge = $diff->y;
                } catch (Exception $e) {
                }
            }
            $ageNext = $currentAge + 1;
            ?>
            <div class="recommend-box">
                <h3 class="font-bold" style="font-size: 1.125rem; margin-top:0; margin-bottom: 0.5rem;">💡 คำแนะนำ
                    (Recommendation)</h3>
                <div class="rec-grid">
                    <div>
                        <p>วันเกิด: <strong><?= htmlspecialchars($targetUser->birthday) ?></strong></p>
                        <p>อายุปัจจุบัน: <strong class="text-blue" style="font-size: 1.125rem;"><?= $currentAge ?>
                                (<?= $currentAge ?> ย่าง <?= $currentAge + 1 ?>)</strong></p>
                        <p>ปีถัดไป: <strong class="text-green" style="font-size: 1.125rem;"><?= $ageNext ?>
                                (<?= $ageNext ?> ย่าง <?= $ageNext + 1 ?>)</strong> <span
                                class="text-xs text-gray">(ใช้สำหรับปีหน้า)</span></p>
                    </div>
                    <div class="flex-center gap-2">
                        <span>เลือกอายุที่จะบันทึก: </span>
                        <button type="button" onclick="setAge(<?= $currentAge ?>)" class="btn btn-sm btn-blue">
                            Age <?= $currentAge ?> (<?= $currentAge ?>-><?= $currentAge + 1 ?>)
                        </button>
                        <button type="button" onclick="setAge(<?= $ageNext ?>)" class="btn btn-sm btn-green">
                            Age <?= $ageNext ?> (<?= $ageNext ?>-><?= $ageNext + 1 ?>)
                        </button>
                    </div>
                </div>
            </div>

            <!-- Form -->
            <form action="/web/admin/bag-colors/save" method="POST"
                style="background-color: #f9fafb; border: 1px solid #e5e7eb; padding: 1rem; border-radius: 0.5rem;">
                <input type="hidden" name="memberid" value="<?= $targetUser->memberid ?>">

                <div class="form-grid">
                    <div>
                        <label>Age (อายุ)</label>
                        <input type="number" id="input_age" name="age" required placeholder="เช่น <?= $ageNext ?>"
                            value="<?= $ageNext ?>">
                    </div>

                    <?php for ($i = 1; $i <= 6; $i++): ?>
                        <div>
                            <label>Color <?= $i ?></label>
                            <div class="color-input-group">
                                <input type="color" id="input_c<?= $i ?>" name="c<?= $i ?>" value="#FFFFFF">
                                <input type="text" id="text_c<?= $i ?>"
                                    onchange="document.getElementById('input_c<?= $i ?>').value = this.value"
                                    class="color-text" value="#FFFFFF">
                            </div>
                        </div>
                    <?php endfor; ?>

                    <div>
                        <button type="submit" class="btn btn-green w-full">
                            Save Colors
                        </button>
                    </div>
                </div>

                <!-- Script sync color inputs -->
                <script>
                    ['c1', 'c2', 'c3', 'c4', 'c5', 'c6'].forEach(id => {
                        document.getElementById('input_' + id).addEventListener('input', function (e) {
                            document.getElementById('text_' + id).value = e.target.value.toUpperCase();
                        });
                    });
                </script>
            </form>
        </div>

        <!-- History Table -->
        <h2>ประวัติการตั้งค่าสี (History)</h2>
        <?php if (!empty($bagColors)): ?>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Age</th>
                            <th>Colors (1-6)</th>
                            <th>บันทึกล่าสุด</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($bagColors as $bag): ?>
                            <tr>
                                <td style="font-weight: bold; font-size: 1.125rem;">
                                    <?= $bag->age ?>
                                    <span class="text-sm font-normal text-gray" style="display:block;">(<?= $bag->age ?> ย่าง
                                        <?= $bag->age + 1 ?>)</span>
                                </td>
                                <td>
                                    <div class="color-circles">
                                        <div class="circle" style="background-color: <?= $bag->bag_color1 ?>"
                                            title="1: <?= $bag->bag_color1 ?>"></div>
                                        <div class="circle" style="background-color: <?= $bag->bag_color2 ?>"
                                            title="2: <?= $bag->bag_color2 ?>"></div>
                                        <div class="circle" style="background-color: <?= $bag->bag_color3 ?>"
                                            title="3: <?= $bag->bag_color3 ?>"></div>
                                        <div class="circle" style="background-color: <?= $bag->bag_color4 ?>"
                                            title="4: <?= $bag->bag_color4 ?>"></div>
                                        <div class="circle"
                                            style="background-color: <?= !empty($bag->bag_color5) ? $bag->bag_color5 : '#FFFFFF' ?>"
                                            title="5: <?= $bag->bag_color5 ?>"></div>
                                        <div class="circle"
                                            style="background-color: <?= !empty($bag->bag_color6) ? $bag->bag_color6 : '#FFFFFF' ?>"
                                            title="6: <?= $bag->bag_color6 ?>"></div>
                                    </div>
                                </td>
                                <td>
                                    <span
                                        class="text-sm <?= empty($bag->date_color_updated) ? 'text-gray italic' : 'font-bold text-blue' ?>">
                                        <?= !empty($bag->date_color_updated) ? htmlspecialchars($bag->date_color_updated) : 'ยังไม่ update' ?>
                                    </span>
                                </td>
                                <td>
                                    <button
                                        onclick="editColors('<?= $bag->age ?>', '<?= $bag->bag_color1 ?>', '<?= $bag->bag_color2 ?>', '<?= $bag->bag_color3 ?>', '<?= $bag->bag_color4 ?>', '<?= !empty($bag->bag_color5) ? $bag->bag_color5 : '#FFFFFF' ?>', '<?= !empty($bag->bag_color6) ? $bag->bag_color6 : '#FFFFFF' ?>')"
                                        class="btn-edit btn-sm"
                                        style="border:none; cursor:pointer; background: #e0f2fe; padding: 4px 12px; border-radius: 4px;">
                                        Edit / Load
                                    </button>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        <?php else: ?>
            <p class="text-gray">ยังไม่มีข้อมูลสีกระเป๋า</p>
        <?php endif; ?>
    </div>

    <script>
        function setAge(age) {
            document.getElementById('input_age').value = age;
        }

        function editColors(age, c1, c2, c3, c4, c5, c6) {
            document.getElementById('input_age').value = age;

            document.getElementById('input_c1').value = c1;
            document.getElementById('text_c1').value = c1;

            document.getElementById('input_c2').value = c2;
            document.getElementById('text_c2').value = c2;

            document.getElementById('input_c3').value = c3;
            document.getElementById('text_c3').value = c3;

            document.getElementById('input_c4').value = c4;
            document.getElementById('text_c4').value = c4;

            document.getElementById('input_c5').value = c5;
            document.getElementById('text_c5').value = c5;

            document.getElementById('input_c6').value = c6;
            document.getElementById('text_c6').value = c6;

            window.scrollTo({ top: 0, behavior: 'smooth' });
        }

        function sendNotification() {
            if (!confirm('ยืนยันการส่ง Notification หาสมาชิกรายนี้?')) return;

            const btn = event.target;
            const originalText = btn.innerText;
            btn.innerText = 'Sending...';
            btn.disabled = true;

            fetch('/web/admin/notifications/send-bag-colors?memberid=<?= $targetUser->memberid ?>')
                .then(response => response.json())
                .then(data => {
                    if (data.status === 'completed') {
                        if (data.sent_count > 0) {
                            alert(`ส่งเรียบร้อย! (Sent: ${data.sent_count})`);
                        } else {
                            let msg = 'ไม่สามารถส่ง Notification ได้:';
                            if (data.details && data.details[0]) {
                                const d = data.details[0];
                                if (d.status === 'no_bag_color_found') msg += '\n- ไม่พบข้อมูลสีกระเป๋าสำหรับอายุปัจจุบัน';
                                else if (d.status === 'failed') msg += '\n- Token หมดอายุหรือผิดพลาด (FCM: UNREGISTERED)';
                                else msg += '\n- ' + d.status;
                            } else {
                                msg += '\n- ไม่พบ Token ของสมาชิกรายนี้';
                            }
                            alert(msg);
                        }
                    } else {
                        alert('เกิดข้อผิดพลาด: ' + (data.message || 'Unknown error'));
                    }
                })
                .catch(err => {
                    alert('Error sending notification');
                    console.error(err);
                })
                .finally(() => {
                    btn.innerText = originalText;
                    btn.disabled = false;
                });
        }
    </script>
</body>

</html>