<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>มอบพระพุทธรูปปางต่างๆ - Admin</title>
    <link href="https://fonts.googleapis.com/css2?family=Sarabun:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        body {
            font-family: 'Sarabun', sans-serif;
            background-color: #f4f7f6;
            margin: 0;
            padding: 0;
        }

        .container-fluid {
            max-width: 1200px;
        }

        .card {
            border-radius: 15px;
            border: none;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
            margin-bottom: 2rem;
        }

        .assign-card {
            background: linear-gradient(135deg, #fff 0%, #f9f9f9 100%);
            border-left: 5px solid #FFD700;
        }

        .btn-gold {
            background: linear-gradient(45deg, #FFD700, #FFC107);
            color: #000;
            font-weight: 600;
            border: none;
        }

        .btn-gold:hover {
            background: linear-gradient(45deg, #FFC107, #FFB300);
            transform: translateY(-1px);
        }

        .user-row:hover {
            background-color: #f8f9fa;
        }

        .id-badge {
            background-color: #e9ecef;
            padding: 2px 8px;
            border-radius: 4px;
            font-family: monospace;
            font-size: 0.9rem;
        }

        .history-item {
            font-size: 0.85rem;
            padding: 8px;
            border-bottom: 1px solid #eee;
        }

        .history-item:last-child {
            border-bottom: none;
        }

        .type-badge {
            font-size: 0.7rem;
            text-transform: uppercase;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="container-fluid mt-4">
        <div class="row">
            <!-- Left Side: User Search -->
            <div class="col-md-7">
                <div class="card p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h4 class="mb-0">🔍 ค้นหาสมาชิก</h4>
                        <a href="/admin/buddha" class="btn btn-outline-secondary btn-sm">จัดการรายการพระปาง</a>
                    </div>

                    <form action="/web/admin/buddha/assign" method="GET" class="input-group mb-4">
                        <input type="text" name="search" class="form-control" placeholder="ชื่อ, นามสกุล หรือ ID..."
                            value="<?= htmlspecialchars($search ?? '') ?>">
                        <button class="btn btn-primary" type="submit">ค้นหา</button>
                    </form>

                    <?php if (isset($_SESSION['status'])): ?>
                        <div class="alert alert-<?= $_SESSION['status']['type'] ?> alert-dismissible fade show">
                            <?= $_SESSION['status']['message'] ?>
                            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                        </div>
                        <?php unset($_SESSION['status']); ?>
                    <?php endif; ?>

                    <div class="table-responsive">
                        <table class="table align-middle">
                            <thead class="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th>ชื่อ-นามสกุล</th>
                                    <th>การจัดการ</th>
                                </tr>
                            </thead>
                            <tbody>
                                <?php foreach ($users as $user): ?>
                                    <tr class="user-row">
                                        <td><span class="id-badge">
                                                <?= $user->memberid ?>
                                            </span></td>
                                        <td>
                                            <strong>
                                                <?= htmlspecialchars($user->realname . ' ' . $user->surname) ?>
                                            </strong><br>
                                            <small class="text-muted">@
                                                <?= htmlspecialchars($user->username) ?>
                                            </small>
                                        </td>
                                        <td>
                                            <button class="btn btn-sm btn-gold select-user-btn"
                                                data-id="<?= $user->memberid ?>"
                                                data-name="<?= htmlspecialchars($user->realname . ' ' . $user->surname) ?>">
                                                เลือกมอบพระ
                                            </button>
                                        </td>
                                    </tr>
                                <?php endforeach; ?>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- Right Side: Assign Form -->
            <div class="col-md-5">
                <div class="card p-4 assign-card" id="assignFormContainer">
                    <h4 class="mb-4">☸️ มอบพระพุทธรูปประจำตัว</h4>

                    <div id="noUserSelected" class="text-center py-5 text-muted">
                        <i class="fas fa-user-plus fa-3x mb-3"></i>
                        <p>กรุณาเลือกสมาชิกจากรายการด้านซ้าย</p>
                    </div>

                    <form action="/web/admin/buddha/assign/save" method="POST" id="assignForm" style="display: none;">
                        <input type="hidden" name="memberid" id="formMemberId">
                        <input type="hidden" name="search" value="<?= htmlspecialchars($search ?? '') ?>">

                        <div class="mb-3">
                            <label class="form-label fw-bold">สมาชิกที่เลือก:</label>
                            <div class="p-3 bg-light rounded" id="selectedUserName"
                                style="font-size: 1.1rem; color: #d4a017;"></div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-bold">ประเภทการแนะนำ</label>
                            <div class="btn-group w-100" role="group">
                                <input type="radio" class="btn-check" name="assignment_type" id="typeAnnual"
                                    value="annual" checked>
                                <label class="btn btn-outline-primary" for="typeAnnual">ประจำปี</label>

                                <input type="radio" class="btn-check" name="assignment_type" id="typeLifetime"
                                    value="lifetime">
                                <label class="btn btn-outline-primary" for="typeLifetime">ตลอดชีพ (แนะนำ)</label>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-bold">เลือกพระพุทธรูปปางที่เหมาะสม</label>
                            <select name="buddha_id" class="form-select form-select-lg" required>
                                <option value="">-- เลือกปางพระพุทธรูป --</option>
                                <?php foreach ($pangs as $p):
                                    $dayNames = [1 => 'อาทิตย์', 2 => 'จันทร์', 3 => 'อังคาร', 4 => 'พุธกลางวัน', 5 => 'พฤหัส', 6 => 'ศุกร์', 7 => 'เสาร์', 8 => 'พุธกลางคืน'];
                                    $day = isset($dayNames[$p->buddha_day]) ? "(" . $dayNames[$p->buddha_day] . ") " : "";
                                    ?>
                                    <option value="<?= $p->id ?>">
                                        <?= $day . htmlspecialchars($p->pang_name) ?>
                                    </option>
                                <?php endforeach; ?>
                            </select>
                        </div>

                        <div class="mb-4">
                            <label class="form-label fw-bold">คำแนะนำพิเศษจากคุณนิน (ถ้ามี)</label>
                            <textarea name="custom_description" class="form-control" rows="4"
                                placeholder="เช่น แนะนำให้ถวายพร้อมดอกไม้สีขาว..."></textarea>
                        </div>

                        <button type="submit" class="btn btn-gold btn-lg w-100 py-3 rounded-pill shadow-sm">
                            <i class="fas fa-paper-plane me-2"></i> บันทึกและส่งแจ้งเตือน
                        </button>

                        <hr>
                        <h6 class="text-muted">ประวัติการมอบที่ผ่านมา:</h6>
                        <div id="assignmentHistory" class="mt-2 bg-white rounded p-2 border"
                            style="max-height: 200px; overflow-y: auto;">
                            <!-- Load via JS -->
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.querySelectorAll('.select-user-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const mid = this.dataset.id;
                const name = this.dataset.name;

                document.getElementById('noUserSelected').style.display = 'none';
                document.getElementById('assignForm').style.display = 'block';

                document.getElementById('formMemberId').value = mid;
                document.getElementById('selectedUserName').innerText = name;

                // Load History
                loadHistory(mid);
            });
        });

        async function loadHistory(mid) {
            const historyDiv = document.getElementById('assignmentHistory');
            historyDiv.innerHTML = '<div class="text-center p-2"><div class="spinner-border spinner-border-sm text-primary"></div></div>';

            try {
                const response = await fetch('/web/admin/buddha/assign/history/' + mid);
                const data = await response.json();

                if (data.length === 0) {
                    historyDiv.innerHTML = '<p class="text-center text-muted mb-0 p-2">ไม่มีประวัติการมอบ</p>';
                } else {
                    historyDiv.innerHTML = data.map(item => `
                        <div class="history-item">
                            <div class="d-flex justify-content-between">
                                <strong>${item.pang_name}</strong>
                                <span class="badge ${item.assignment_type === 'lifetime' ? 'bg-danger' : 'bg-primary'} type-badge">${item.assignment_type}</span>
                            </div>
                            <div class="text-muted small">${item.assigned_at}</div>
                            ${item.custom_description ? `<div class="small mt-1 text-info">"${item.custom_description}"</div>` : ''}
                        </div>
                    `).join('');
                }
            } catch (e) {
                historyDiv.innerHTML = '<p class="text-danger text-center p-2">โหลดประวัติไม่สำเร็จ</p>';
            }
        }
    </script>
</body>

</html>