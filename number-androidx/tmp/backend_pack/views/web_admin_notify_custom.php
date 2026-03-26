<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Send Custom Notification - NumberNice Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        body { background-color: #f8f9fa; font-family: 'Sarabun', sans-serif; }
        .card { border-radius: 15px; border: none; box-shadow: 0 4px 6px rgba(0,0,0,0.1); margin-bottom: 20px; }
        .btn-custom { background: linear-gradient(45deg, #FFD700, #FFC107); color: #000; font-weight: bold; border: none; }
    </style>
</head>
<body>
    <?php include 'web_menu.php'; ?>

    <div class="container mt-4">
        <div class="row mb-4">
            <div class="col">
                <h2>📢 ส่งข้อความแจ้งเตือน (Custom Notification)</h2>
                <p class="text-muted">ค้นหาผู้ใช้แล้วส่งข้อความแจ้งเตือนความพิเศษได้ที่นี่</p>
                <?php include 'web_admin_toolbar.php'; ?>
            </div>
        </div>

        <div class="card p-4">
            <form action="" method="GET" class="row g-3">
                <div class="col-md-10">
                    <input type="text" name="search" class="form-control" placeholder="ค้นหาด้วย ชื่อจริง, Username หรือ ID..." value="<?= htmlspecialchars($search) ?>">
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-primary w-100"><i class="fas fa-search"></i> ค้นหา</button>
                </div>
            </form>
        </div>

        <div class="card p-0 overflow-hidden">
            <table class="table table-hover mb-0">
                <thead class="bg-light">
                    <tr>
                        <th>ID</th>
                        <th>Username</th>
                        <th>ชื่อ-นามสกุล</th>
                        <th>STATUS</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($users)): ?>
                        <tr><td colspan="5" class="text-center py-4">ไม่พบข้อมูลผู้ใช้</td></tr>
                    <?php else: ?>
                        <?php foreach ($users as $u): ?>
                            <tr>
                                <td><?= $u->memberid ?></td>
                                <td><?= htmlspecialchars($u->username) ?></td>
                                <td><?= htmlspecialchars($u->realname . ' ' . $u->surname) ?></td>
                                <td>
                                    <?php if (!empty($u->fcm_token)): ?>
                                        <span class="badge bg-success">มี Token ✅</span>
                                    <?php else: ?>
                                        <span class="badge bg-secondary">ไม่มี Token ❌</span>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <button class="btn btn-sm btn-custom send-btn" 
                                            data-id="<?= $u->memberid ?>" 
                                            data-name="<?= htmlspecialchars($u->username) ?>"
                                            <?= empty($u->fcm_token) ? 'disabled' : '' ?>>
                                        <i class="fas fa-paper-plane"></i> ส่งข้อความ
                                    </button>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Send Modal -->
    <div class="modal fade" id="sendModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="/admin/notifications/custom/send" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title">ส่งข้อความถึง: <span id="modalUserName"></span></h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" name="memberid" id="modalMemberId">
                        <div class="mb-3">
                            <label class="form-label"><b>ประเภทการแจ้งเตือน (Category)</b></label>
                            <select name="type" class="form-select" required>
                                <option value="custom">ทั่วไป (Custom)</option>
                                <option value="webview_merit">วิธีทำบุญ (Merit)</option>
                                <option value="webview_changenum">การเปลี่ยนเบอร์ (Change Steps)</option>
                                <option value="webview_spell">คาถาและคำเตือนพิเศษ (Spells)</option>
                            </select>
                            <div class="form-text">เลือกหมวดหมู่เพื่อให้รายการไปแสดงถูกที่ในแอป</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">หัวข้อ (Title)</label>
                            <input type="text" name="title" class="form-control" required placeholder="เช่น โปรโมชั่นพิเศษ">
                        </div>
                        <div class="mb-3">
                            <label class="form-label">เนื้อหา (Body)</label>
                            <textarea name="body" class="form-control" rows="3" required placeholder="เนื้อหาแจ้งเตือน..."></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">ลิงก์ URL หรือ <b>ID คาถา</b> (กรณีเลือกหมวดคาถา)</label>
                            <input type="text" name="url" class="form-control" placeholder="เช่น 12 หรือ https://...">
                            <div class="form-text">หากเลือกหมวดคาถา ให้ใส่ตัวเลข ID ของคาถาที่ต้องการให้แสดง</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">หมายเหตุเพิ่มเติม (Note)</label>
                            <input type="text" name="note" class="form-control" placeholder="หมายเหตุสั้นๆ">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">ยกเลิก</button>
                        <button type="submit" class="btn btn-success">ยืนยันส่ง</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.querySelectorAll('.send-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const id = this.dataset.id;
                const name = this.dataset.name;
                document.getElementById('modalMemberId').value = id;
                document.getElementById('modalUserName').innerText = name;
                new bootstrap.Modal(document.getElementById('sendModal')).show();
            });
        });
    </script>
</body>
</html>