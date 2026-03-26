<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Privileges - Admin</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f8f9fa;
            margin: 0;
            padding: 0;
        }

        .main-wrapper {
            max-width: 1000px;
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
            background: linear-gradient(135deg, #6610f2, #6f42c1);
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

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }

        th {
            text-align: left;
            background: #f1f3f5;
            padding: 1rem;
            border-bottom: 2px solid #dee2e6;
            color: #495057;
            text-transform: uppercase;
            font-size: 0.85rem;
            letter-spacing: 0.5px;
        }

        td {
            padding: 1rem;
            border-bottom: 1px solid #eee;
            vertical-align: top;
        }

        .badge {
            display: inline-block;
            padding: 0.25rem 0.5rem;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: bold;
            text-transform: uppercase;
            margin-bottom: 0.5rem;
        }

        .badge-primary {
            background: #e7f5ff;
            color: #228be6;
        }

        .badge-warning {
            background: #fff9db;
            color: #f08c00;
        }

        .btn {
            display: inline-block;
            padding: 0.5rem 1rem;
            border-radius: 6px;
            text-decoration: none;
            font-weight: 500;
            font-size: 0.9rem;
            border: none;
            cursor: pointer;
            transition: all 0.2s;
        }

        .btn-edit {
            background-color: #f0f7fb;
            color: #3CA7E6;
            border: 1px solid #3CA7E6;
        }

        .btn-edit:hover {
            background-color: #3CA7E6;
            color: white;
        }

        .btn-save {
            background-color: #28a745;
            color: white;
            margin-top: 1rem;
        }

        .btn-save:hover {
            background-color: #218838;
        }

        .btn-cancel {
            background-color: #6c757d;
            color: white;
            margin-top: 1rem;
        }

        .btn-cancel:hover {
            background-color: #5a6268;
        }

        textarea {
            width: 100%;
            min-height: 80px;
            padding: 0.5rem;
            border: 1px solid #ced4da;
            border-radius: 4px;
            font-family: inherit;
            font-size: 0.9rem;
            box-sizing: border-box;
        }

        input[type="text"] {
            width: 100%;
            padding: 0.5rem;
            border: 1px solid #ced4da;
            border-radius: 4px;
            box-sizing: border-box;
            font-size: 0.9rem;
        }

        .form-row {
            margin-bottom: 1.5rem;
        }

        .form-label {
            display: block;
            font-weight: 600;
            margin-bottom: 0.5rem;
            color: #495057;
            font-size: 0.95rem;
        }

        .modal {
            display: none;
            position: fixed;
            z-index: 1001;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            overflow-y: auto;
        }

        .modal-content {
            background: white;
            margin: 3rem auto;
            padding: 2rem;
            border-radius: 12px;
            width: 90%;
            max-width: 600px;
            position: relative;
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
            border-bottom: 1px solid #eee;
            padding-bottom: 1rem;
        }

        .modal-header h3 {
            margin: 0;
        }

        .btn-close {
            background: none;
            border: none;
            font-size: 1.5rem;
            cursor: pointer;
            color: #888;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <div class="card">
            <div class="card-header">
                <h2>💎 จัดการสิทธิ์การใช้งาน (Privileges)</h2>
                <a href="/web/dashboard" class="btn" style="color: white; border:1px solid white;">← กลับไปหลังบ้าน</a>
            </div>
            <div class="card-body">
                <p style="color: #666; margin-bottom: 2rem;">แก้ไขรายละเอียดและสิทธิประโยชน์ที่จะแสดงในแอป Android
                    ตามระดับสมาชิกต่างๆ</p>

                <table>
                    <thead>
                        <tr>
                            <th width="15%">KEY / ระดับ</th>
                            <th width="25%">ชื่อที่แสดง (Name)</th>
                            <th width="40%">รายละเอียดสิทธิ์ Benefits</th>
                            <th width="10%">ราคา</th>
                            <th width="10%">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($privileges as $p): ?>
                            <tr>
                                <td>
                                    <span class="badge badge-primary">
                                        <?php echo htmlspecialchars($p['privilege_key']); ?>
                                    </span>
                                </td>
                                <td>
                                    <strong>
                                        <?php echo htmlspecialchars($p['privilege_name_th']); ?>
                                    </strong>
                                </td>
                                <td>
                                    <div style="font-size: 0.85rem; color: #555; white-space: pre-wrap;">
                                        <?php echo htmlspecialchars($p['privilege_benefits_th']); ?>
                                    </div>
                                </td>
                                <td>
                                    <span style="font-weight: 600; color: #28a745;">
                                        <?php echo htmlspecialchars($p['upgrade_price_th']); ?>
                                    </span>
                                </td>
                                <td>
                                    <button class="btn btn-edit"
                                        onclick="openEdit(<?php echo htmlspecialchars(json_encode($p)); ?>)">แก้ไข</button>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- Edit Modal -->
    <div id="editModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3>แก้ไขรายละเอียดสิทธิ์: <span id="modalTitleKey"></span></h3>
                <button class="btn-close" onclick="closeModal()">&times;</button>
            </div>
            <form action="/web/admin/privileges/update" method="POST">
                <input type="hidden" name="id" id="edit_id">

                <div class="form-row">
                    <label class="form-label">ชื่อระดับสมาชิก (แสดงในแอป)</label>
                    <input type="text" name="name_th" id="edit_name" required>
                </div>

                <div class="form-row">
                    <label class="form-label">รายละเอียดเบื้องต้น (Detail)</label>
                    <textarea name="detail_th" id="edit_detail"></textarea>
                </div>

                <div class="form-row">
                    <label class="form-label">สิทธิประโยชน์ที่จะได้รับ (Benefits - รองรับการขึ้นบรรทัดใหม่)</label>
                    <textarea name="benefits_th" id="edit_benefits" style="height: 150px;"></textarea>
                </div>

                <div class="form-row">
                    <label class="form-label">ราคาอัปเกรด (Thai Baht)</label>
                    <input type="text" name="price_th" id="edit_price">
                </div>

                <div
                    style="display: flex; gap: 1rem; justify-content: flex-end; border-top: 1px solid #eee; padding-top: 1rem;">
                    <button type="button" class="btn btn-cancel" onclick="closeModal()">ยกเลิก</button>
                    <button type="submit" class="btn btn-save">บันทึกข้อมูล ✅</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        function openEdit(p) {
            document.getElementById('modalTitleKey').textContent = p.privilege_key;
            document.getElementById('edit_id').value = p.id;
            document.getElementById('edit_name').value = p.privilege_name_th;
            document.getElementById('edit_detail').value = p.privilege_detail_th;
            document.getElementById('edit_benefits').value = p.privilege_benefits_th;
            document.getElementById('edit_price').value = p.upgrade_price_th;
            document.getElementById('editModal').style.display = 'block';
        }

        function closeModal() {
            document.getElementById('editModal').style.display = 'none';
        }

        window.onclick = function (event) {
            if (event.target == document.getElementById('editModal')) {
                closeModal();
            }
        }
    </script>
</body>

</html>