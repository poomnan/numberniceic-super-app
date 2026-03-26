<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <title>
        <?php echo isset($tabian) ? 'แก้ไขป้ายทะเบียน' : 'เพิ่มป้ายทะเบียนใหม่'; ?>
    </title>
    <style>
        body {
            font-family: sans-serif;
            background: #f4f7f6;
            margin: 0;
        }

        .container {
            max-width: 600px;
            margin: 2rem auto;
            padding: 2rem;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }

        h1 {
            color: #333;
            margin-bottom: 2rem;
        }

        .form-group {
            margin-bottom: 1.5rem;
        }

        label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: bold;
            color: #555;
        }

        input,
        select,
        textarea {
            width: 100%;
            padding: 0.8rem;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
            font-size: 1rem;
        }

        .btn-row {
            display: flex;
            gap: 1rem;
            margin-top: 2rem;
        }

        .btn {
            padding: 0.8rem 2rem;
            border-radius: 4px;
            text-decoration: none;
            font-weight: bold;
            cursor: pointer;
            border: none;
            font-size: 1rem;
        }

        .btn-save {
            background: #28a745;
            color: white;
            flex: 1;
        }

        .btn-cancel {
            background: #6c757d;
            color: white;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container">
        <h1>
            <?php echo isset($tabian) ? 'แก้ไขป้ายทะเบียน' : 'เพิ่มป้ายทะเบียนใหม่'; ?>
        </h1>

        <?php
        $actionUrl = isset($tabian)
            ? '/web/admin/tabians/update/' . $tabian->tabian_id
            : '/web/admin/tabians/store';
        ?>
        <form method="POST" action="<?php echo $actionUrl; ?>">
            <div class="form-group">
                <label>เลขทะเบียน (เช่น กข 1234)</label>
                <input type="text" name="tabian_number"
                    value="<?php echo isset($tabian) ? htmlspecialchars($tabian->tabian_number) : ''; ?>" required
                    placeholder="เช่น กข 1234">
            </div>

            <div class="form-group">
                <label>จังหวัด</label>
                <input type="text" name="tabian_province"
                    value="<?php echo isset($tabian) ? htmlspecialchars($tabian->tabian_province) : ''; ?>"
                    placeholder="เช่น กรุงเทพมหานคร">
            </div>

            <div class="form-group">
                <label>หมวดหมู่</label>
                <select name="tabian_category">
                    <option value="ทั่วไป" <?php echo (isset($tabian) && $tabian->tabian_category == 'ทั่วไป') ? 'selected' : ''; ?>>ทั่วไป</option>
                    <option value="ป้ายกราฟิก" <?php echo (isset($tabian) && $tabian->tabian_category == 'ป้ายกราฟิก') ? 'selected' : ''; ?>>ป้ายกราฟิก</option>
                    <option value="เลขสวย" <?php echo (isset($tabian) && $tabian->tabian_category == 'เลขสวย') ? 'selected' : ''; ?>>เลขสวย</option>
                    <option value="เลขมงคล" <?php echo (isset($tabian) && $tabian->tabian_category == 'เลขมงคล') ? 'selected' : ''; ?>>เลขมงคล</option>
                </select>
            </div>

            <div class="form-group">
                <label>ราคา (บาท)</label>
                <input type="number" name="tabian_price"
                    value="<?php echo isset($tabian) ? htmlspecialchars($tabian->tabian_price) : '0'; ?>" required
                    min="0">
            </div>

            <div class="form-group">
                <label>สถานะ</label>
                <select name="tabian_status">
                    <option value="available" <?php echo (isset($tabian) && $tabian->tabian_status == 'available') ? 'selected' : ''; ?>>ว่าง (Available)</option>
                    <option value="sold" <?php echo (isset($tabian) && $tabian->tabian_status == 'sold') ? 'selected' : ''; ?>>ขายแล้ว (Sold)</option>
                </select>
            </div>

            <div class="form-group">
                <label>แท็ก (เช่น VIP, ลดราคา)</label>
                <input type="text" name="tabian_tag"
                    value="<?php echo isset($tabian) ? htmlspecialchars($tabian->tabian_tag ?? '') : ''; ?>"
                    placeholder="เช่น VIP">
            </div>

            <div class="form-group">
                <label>ลำดับการแสดงผล (ตัวเลขน้อยขึ้นก่อน)</label>
                <input type="number" name="order_no"
                    value="<?php echo isset($tabian) ? htmlspecialchars($tabian->order_no ?? '0') : '0'; ?>"
                    placeholder="0">
            </div>

            <div class="btn-row">
                <button type="submit" class="btn btn-save">บันทึกข้อมูล</button>
                <a href="/web/admin/tabians" class="btn btn-cancel">ยกเลิก</a>
            </div>
        </form>
    </div>
</body>

</html>