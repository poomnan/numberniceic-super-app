<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ข้อปฏิบัติ - ananya.in.th</title>
    <link rel="icon" type="image/png" href="/favicon.png">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link
        href="https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;500;600;700&family=Sarabun:wght@300;400;500;600;700&display=swap"
        rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: 'Sarabun', sans-serif;
            background: #f0f2f5;
        }

        .main-header {
            background: linear-gradient(135deg, #198754 0%, #20c997 100%);
            padding: 2.5rem 1rem;
            text-align: center;
            color: white;
            margin-bottom: 2rem;
        }

        .main-header h1 {
            font-family: 'Kanit', sans-serif;
            font-size: 2.5rem;
            margin: 0;
            color: white;
        }

        .main-header p {
            color: rgba(255, 255, 255, 0.9);
            font-size: 1.1rem;
            margin-top: 0.5rem;
        }

        .services-container {
            max-width: 1000px;
            margin: 0 auto 4rem auto;
            padding: 0 1.5rem;
        }

        .services-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 2rem;
        }

        .service-card {
            background: white;
            border-radius: 20px;
            padding: 2.5rem 1.5rem;
            text-decoration: none;
            color: inherit;
            box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
            transition: all 0.3s ease;
            display: flex;
            flex-direction: column;
            align-items: center;
            border: 1px solid transparent;
        }

        .service-card:hover {
            transform: translateY(-10px);
            border-color: #20c997;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
        }

        .service-icon {
            width: 80px;
            height: 80px;
            background: linear-gradient(135deg, #198754, #20c997);
            color: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2.5rem;
            margin-bottom: 1.5rem;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .service-title {
            font-family: 'Kanit', sans-serif;
            font-size: 1.5rem;
            font-weight: 600;
            color: #2d3748;
            margin-bottom: 0.75rem;
        }

        .service-desc {
            color: #718096;
            font-size: 0.95rem;
            line-height: 1.5;
        }

        @media (max-width: 640px) {
            .services-header h1 {
                font-size: 2rem;
            }

            .services-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>

<body>

    <?php include 'web_menu.php'; ?>

    <header class="main-header">
        <h1>ข้อปฏิบัติ</h1>
        <p>เลือกบริการที่คุณต้องการเพื่อเริ่มต้นความเป็นมงคล</p>
    </header>

    <main class="services-container">

        <div class="services-grid">
            <a href="/changenum/namesur" class="service-card">
                <div class="service-icon">
                    <i class="fa-solid fa-user-edit"></i>
                </div>
                <div class="service-title">วิเคราะห์ชื่อ</div>
                <div class="service-desc">ตรวจสอบความเป็นมงคลของชื่อและนามสกุลตามหลักเลขศาสตร์</div>
            </a>

            <a href="/changenum/phone" class="service-card">
                <div class="service-icon">
                    <i class="fa-solid fa-phone"></i>
                </div>
                <div class="service-title">วิเคราะห์เบอร์</div>
                <div class="service-desc">วิเคราะห์พลังตัวเลขจากเบอร์โทรศัพท์ของคุณ เพื่อเสริมดวงชะตา</div>
            </a>

            <a href="/changenum/home" class="service-card">
                <div class="service-icon">
                    <i class="fa-solid fa-magic"></i>
                </div>
                <div class="service-title">ตั้งชื่อดี</div>
                <div class="service-desc">บริการตั้งชื่อใหม่ที่เป็นมงคลและเหมาะสมกับพื้นดวงชะตา</div>
            </a>
        </div>
    </main>

</body>

</html>