<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Spells & Warnings - Ananya Admin</title>
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

        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 2rem;
            background: white;
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
        }

        .header h1 {
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

        .card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
            overflow: hidden;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th,
        td {
            padding: 15px;
            text-align: left;
            border-bottom: 1px solid #eee;
        }

        th {
            background-color: #f8f9fa;
            font-weight: 600;
            color: #555;
        }

        tr:hover {
            background-color: #f8f9fa;
        }

        .badge {
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 0.8rem;
        }

        .badge-spell {
            background-color: #e3f2fd;
            color: #0d47a1;
        }

        .badge-warning {
            background-color: #fff3e0;
            color: #e65100;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <div class="header">
            <h1>📜 คาถาและคำเตือนพิเศษ (Spells & Warnings)</h1>
            <a href="/web/admin/spells/create" class="btn btn-success">+ เพิ่มรายการใหม่</a>
        </div>

        <div class="card">
            <table>
                <thead>
                    <tr>
                        <th width="50">ID</th>
                        <th width="80">Photo</th>
                        <th width="120">Type</th>
                        <th width="200">Title</th>
                        <th>Content Preview</th>
                        <th width="120">Admin Note</th>
                        <th width="130" style="text-align:right;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (isset($items) && count($items) > 0): ?>
                        <?php foreach ($items as $item): ?>
                            <tr>
                                <td><?php echo $item->id; ?></td>
                                <td>
                                    <?php if (!empty($item->photo)): ?>
                                        <img src="<?php echo htmlspecialchars($item->photo); ?>" alt="img"
                                            style="width: 50px; height: 50px; object-fit: cover; border-radius: 4px;">
                                    <?php else: ?>
                                        <span style="color:#ccc;">-</span>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <span
                                        class="badge <?php echo ($item->type == 'spell' ? 'badge-spell' : 'badge-warning'); ?>">
                                        <?php echo ($item->type == 'spell' ? 'คาถา' : 'คำเตือน'); ?>
                                    </span>
                                </td>
                                <td><strong><?php echo htmlspecialchars($item->title); ?></strong></td>
                                <td style="color: #666; font-size: 0.9rem;">
                                    <?php echo htmlspecialchars(mb_strimwidth(strip_tags($item->content), 0, 80, '...')); ?>
                                </td>
                                <td style="color: #f57c00; font-size: 0.85rem; font-style: italic;">
                                    <span id="note-text-<?php echo $item->id; ?>">
                                        <?php echo htmlspecialchars(mb_strimwidth($item->note ?? '', 0, 50, '...')); ?>
                                    </span>
                                    <button class="btn btn-primary"
                                        style="padding: 2px 5px; font-size: 0.7rem; margin-left: 5px; background-color: #ff9800;"
                                        onclick="editNote(<?php echo $item->id; ?>, '<?php echo addslashes($item->note ?? ''); ?>')">
                                        Edit
                                    </button>
                                </td>
                                <td style="text-align:right;">
                                    <a href="/web/admin/spells/edit/<?php echo $item->id; ?>" class="btn btn-primary"
                                        style="padding: 5px 10px; font-size:0.8rem;">Edit</a>
                                    <a href="/web/admin/spells/delete/<?php echo $item->id; ?>"
                                        onclick="return confirm('Are you sure?');" class="btn btn-danger"
                                        style="padding: 5px 10px; font-size:0.8rem;">Del</a>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php else: ?>
                        <tr>
                            <td colspan="5" style="text-align:center; padding: 3rem; color: #999;">ไม่พบข้อมูลคาถาและคำเตือน
                            </td>
                        </tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>

    <script>
        function editNote(id, currentNote) {
            const newNote = prompt("แก้ไข คำเตือนพิเศษ:", currentNote);
            if (newNote !== null) {
                updateNote(id, newNote);
            }
        }

        function updateNote(id, note) {
            fetch('/admin/spell/update-note', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id, note: note })
            })
                .then(response => response.json())
                .then(data => {
                    if (data.status === 'success') {
                        const noteSpan = document.getElementById(`note-text-${id}`);
                        if (noteSpan) {
                            noteSpan.innerText = note.length > 50 ? note.substring(0, 50) + '...' : note;
                        }
                        alert('บันทึกสำเร็จ');
                    } else {
                        alert('เกิดข้อผิดพลาด: ' + (data.message || 'Unknown error'));
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('เกิดข้อผิดพลาดในการเชื่อมต่อ');
                });
        }
    </script>
</body>

</html>