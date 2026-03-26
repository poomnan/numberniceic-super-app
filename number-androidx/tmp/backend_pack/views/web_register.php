<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ลงทะเบียนเพื่อสมัครเป็นสมาชิก - Ananya</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
        * {
            box-sizing: border-box;
        }

        body {
            font-family: 'Kanit', sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
        }

        /* Header like Android AppBar */
        .app-bar {
            background-color: #4a8b2c;
            /* Green header color */
            color: white;
            padding: 1rem;
            display: flex;
            align-items: center;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .back-btn {
            color: white;
            text-decoration: none;
            font-size: 1.2rem;
            margin-right: 1rem;
        }

        .app-title {
            font-size: 1.25rem;
            font-weight: 500;
        }

        /* Form Container */
        .form-container {
            background: white;
            margin: 1rem;
            padding: 1.5rem;
            border-radius: 8px;
            /* Slightly rounded */
            border: 1px solid #e0e0e0;
        }

        .input-row {
            display: flex;
            align-items: center;
            /* Center labels vertically */
            margin-bottom: 0.5rem;
            flex-wrap: nowrap;
        }

        .input-label {
            font-weight: 600;
            color: #555;
            width: 100px;
            /* Fixed width for labels */
            text-align: right;
            margin-right: 10px;
            font-size: 1.1rem;
            flex-shrink: 0;
        }

        /* Android-style underlined input */
        .android-input {
            flex-grow: 1;
            border: none;
            border-bottom: 1px solid #999;
            font-family: 'Kanit', sans-serif;
            font-size: 1.1rem;
            padding: 5px 0;
            outline: none;
            color: #888;
            /* Placeholder-like color for text */
        }

        .android-input:focus {
            border-bottom: 2px solid #4a8b2c;
            color: #333;
        }

        .date-select-row {
            display: flex;
            gap: 10px;
            flex-grow: 1;
        }

        .android-select {
            border: none;
            font-family: 'Kanit', sans-serif;
            font-size: 1rem;
            color: #ff6600;
            /* Orange text for date values */
            outline: none;
            background: transparent;
            cursor: pointer;
            appearance: none;
            /* Hide default arrow if possible */
            padding-right: 15px;
            position: relative;
        }

        /* Custom arrow indicator logic would go here, simpler to leave default for web */

        .section-divider {
            border-top: 1px solid #eee;
            margin: 1rem 0;
        }

        .radio-group {
            display: flex;
            gap: 20px;
            align-items: center;
        }

        .radio-item {
            display: flex;
            align-items: center;
            gap: 5px;
            font-size: 1.1rem;
            cursor: pointer;
        }

        /* Custom Green Fields */
        .green-label {
            color: #6da544;
            /* Light green */
            font-weight: 700;
            font-size: 1.2rem;
            margin-right: 10px;
            width: 130px;
            /* Wider for English labels */
            text-align: right;
        }

        .green-input {
            flex-grow: 1;
            border: none;
            border-bottom: 1px solid #999;
            font-size: 1.1rem;
            padding: 5px 0;
            outline: none;
            color: #888;
        }

        .green-input:focus {
            border-bottom: 2px solid #6da544;
        }

        .submit-btn {
            background-color: #4a8b2c;
            color: white;
            width: 100%;
            padding: 12px;
            border: none;
            font-size: 1.1rem;
            border-radius: 4px;
            margin-top: 2rem;
            cursor: pointer;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        }

        .submit-btn:hover {
            background-color: #3e7525;
        }

        /* Error styling */
        .error-text {
            color: red;
            font-size: 0.8rem;
            margin-left: 140px;
            display: none;
        }
    </style>
</head>

<body>

    <!-- App Bar -->
    <div class="app-bar">
        <a href="/web/login" class="back-btn"><i class="fa-solid fa-arrow-left"></i></a>
        <div class="app-title">ลงทะเบียนเพื่อสมัครเป็นสมาชิก</div>
    </div>

    <div class="form-container">

        <?php if (isset($error)): ?>
            <div style="background:#ffebee; color:#c62828; padding:10px; margin-bottom:15px; border-radius:4px;">
                <?php echo htmlspecialchars($error); ?>
            </div>
        <?php endif; ?>

        <form action="/web/register" method="POST" id="regForm">

            <!-- Thai Fields -->
            <div class="input-row">
                <label class="input-label">ชื่อจริง :</label>
                <input type="text" name="realname" class="android-input" placeholder="ชื่อจริง" required>
            </div>

            <div class="input-row">
                <label class="input-label">นามสกุล :</label>
                <input type="text" name="surname" class="android-input" placeholder="นามสกุล" required>
            </div>

            <div class="input-row">
                <label class="input-label">ว/ด/ป เกิด :</label>
                <div class="date-select-row">
                    <select name="birth_day" class="android-select" style="color:#ff6600">
                        <option value="">วัน</option>
                        <?php for ($i = 1; $i <= 31; $i++)
                            echo "<option value='$i'>$i</option>"; ?>
                    </select>
                    <i class="fa-solid fa-caret-down" style="font-size:10px; color:#888; align-self:center;"></i>

                    <select name="birth_month" class="android-select" style="color:#ff6600">
                        <option value="">เดือน</option>
                        <option value="1">มกราคม</option>
                        <option value="2">กุมภาพันธ์</option>
                        <option value="3">มีนาคม</option>
                        <option value="4">เมษายน</option>
                        <option value="5">พฤษภาคม</option>
                        <option value="6">มิถุนายน</option>
                        <option value="7">กรกฎาคม</option>
                        <option value="8">สิงหาคม</option>
                        <option value="9">กันยายน</option>
                        <option value="10">ตุลาคม</option>
                        <option value="11">พฤศจิกายน</option>
                        <option value="12">ธันวาคม</option>
                    </select>
                    <i class="fa-solid fa-caret-down" style="font-size:10px; color:#888; align-self:center;"></i>

                    <select name="birth_year" class="android-select" style="color:#ff6600">
                        <option value="">ปี</option>
                        <?php
                        $curYear = date('Y') + 543;
                        for ($y = $curYear; $y >= $curYear - 100; $y--)
                            echo "<option value='" . ($y - 543) . "'>$y</option>";
                        ?>
                    </select>
                    <i class="fa-solid fa-caret-down" style="font-size:10px; color:#888; align-self:center;"></i>
                </div>
            </div>

            <div class="input-row">
                <label class="input-label">เวลาเกิด :</label>
                <div class="date-select-row">
                    <select name="birth_hour" class="android-select" style="color:#ff6600">
                        <option value="">เวลา</option>
                        <?php for ($h = 0; $h <= 23; $h++)
                            echo "<option value='$h'>" . sprintf('%02d', $h) . "</option>"; ?>
                    </select>
                    <i class="fa-solid fa-caret-down" style="font-size:10px; color:#888; align-self:center;"></i>

                    <select name="birth_minute" class="android-select" style="color:#ff6600">
                        <option value="">นาที</option>
                        <?php for ($m = 0; $m <= 59; $m++)
                            echo "<option value='$m'>" . sprintf('%02d', $m) . "</option>"; ?>
                    </select>
                    <i class="fa-solid fa-caret-down" style="font-size:10px; color:#888; align-self:center;"></i>
                </div>
            </div>

            <div class="input-row">
                <label class="input-label">จังหวัด เกิด :</label>
                <input type="text" name="province" class="android-input" placeholder="จังหวัด" style="color:#ff6600">
                <i class="fa-solid fa-caret-down" style="font-size:10px; color:#888;"></i>
            </div>

            <div class="section-divider"></div>

            <div class="input-row">
                <label class="input-label">เพศ เกิด :</label>
                <div class="radio-group">
                    <label class="radio-item"><input type="radio" name="gender" value="female"> หญิง</label>
                    <label class="radio-item"><input type="radio" name="gender" value="male"> ชาย</label>
                </div>
            </div>

            <div class="section-divider"></div>

            <!-- Login Details -->
            <div class="input-row" style="margin-top:20px;">
                <label class="green-label">User Name :</label>
                <input type="text" name="username" id="username" class="green-input" placeholder="User Name" required>
            </div>
            <div class="error-text" id="user-error">Username is taken</div>
            <div style="color:green; font-size:0.8rem; margin-left:140px; display:none;" id="user-success">Available
            </div>


            <div class="input-row">
                <label class="green-label">Password :</label>
                <input type="password" name="password" id="password" class="green-input" required>
            </div>

            <div class="input-row">
                <label class="green-label">Re-Password :</label>
                <input type="password" name="confirm_password" id="confirm_password" class="green-input" required>
            </div>
            <div class="error-text" id="pass-error">Passwords do not match</div>

            <button type="submit" class="submit-btn" style="background:#4a8b2c">ลงทะเบียนเป็นสมาชิก</button>

        </form>
    </div>

    <script>
        const uInput = document.getElementById('username');
        const pInput = document.getElementById('password');
        const cInput = document.getElementById('confirm_password');
        const uErr = document.getElementById('user-error');
        const uSucc = document.getElementById('user-success');
        const pErr = document.getElementById('pass-error');

        uInput.addEventListener('blur', function(){
            if(!this.value) return;
             fetch('/web/api/check-username?username='+this.value)
                .then(r=>r.json())
                .then(d=>{
                    if(d.exists){
                        uErr.style.display='block';
                        uSucc.style.display='none';
                        uInput.style.borderBottom='2px solid red';
                    }else{
                        uErr.style.display='none';
                        uSucc.style.display='block';
                        uInput.style.borderBottom='2px solid green';
                    }
                });
        });

        document.getElementById('regForm').addEventListener('submit', function(e){
            let valid = true;
            if(pInput.value !== cInput.value){
                pErr.style.display='block';
                valid = false;
            }
            if(!valid) e.preventDefault();
        });
    </script>

</body>

</html>