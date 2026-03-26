<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ananya - Number Lucky | เลขศาสตร์ พลังเงา</title>
    <link rel="icon" type="image/png" href="/favicon.png">

    <!-- Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link
        href="https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;500;600;700&family=Outfit:wght@400;500;700&display=swap"
        rel="stylesheet">

    <!-- Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
        :root {
            --primary: #8b5cf6;
            --primary-dark: #7c3aed;
            --secondary: #ec4899;
            --accent: #f59e0b;
            --bg-dark: #0f172a;
            --bg-darker: #020617;
            --text-light: #f8fafc;
            --text-gray: #94a3b8;
            --glass: rgba(255, 255, 255, 0.05);
            --glass-border: rgba(255, 255, 255, 0.1);
        }

        body {
            margin: 0;
            padding: 0;
            font-family: 'Kanit', sans-serif;
            background-color: var(--bg-dark);
            color: var(--text-light);
            overflow-x: hidden;
        }

        /* Override Navbar for Homepage */
        .navbar {
            background: rgba(15, 23, 42, 0.8) !important;
            backdrop-filter: blur(10px);
            border-bottom: 1px solid var(--glass-border);
            position: fixed !important;
            width: 100%;
            top: 0;
            box-sizing: border-box;
        }

        /* Section Spacing */
        main {
            padding-top: 80px;
            /* Offset fixed navbar */
        }

        /* Hero Section */
        .hero {
            position: relative;
            min-height: 90vh;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            padding: 2rem;
        }

        .hero-bg {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: radial-gradient(circle at 20% 20%, #2e1065 0%, #0f172a 50%);
            z-index: -1;
        }

        .hero-blob {
            position: absolute;
            filter: blur(80px);
            opacity: 0.6;
            animation: float 10s infinite ease-in-out;
            border-radius: 50%;
        }

        .blob-1 {
            top: -10%;
            right: -10%;
            width: 500px;
            height: 500px;
            background: var(--primary);
        }

        .blob-2 {
            bottom: -10%;
            left: -10%;
            width: 400px;
            height: 400px;
            background: var(--secondary);
            animation-delay: -5s;
        }

        .hero-content {
            max-width: 1200px;
            width: 100%;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 4rem;
            align-items: center;
            z-index: 1;
        }

        .hero-text h1 {
            font-size: 4rem;
            line-height: 1.1;
            margin-bottom: 1.5rem;
            background: linear-gradient(to right, #fff, #cbd5e1);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            font-weight: 700;
        }

        .hero-text span {
            background: linear-gradient(135deg, var(--primary), var(--secondary));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .hero-text p {
            font-size: 1.25rem;
            color: var(--text-gray);
            margin-bottom: 2.5rem;
            line-height: 1.6;
        }

        .cta-group {
            display: flex;
            gap: 1rem;
        }

        .btn {
            padding: 1rem 2rem;
            border-radius: 50px;
            font-weight: 600;
            font-size: 1.1rem;
            text-decoration: none;
            transition: all 0.3s ease;
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
        }

        .btn-primary {
            background: linear-gradient(90deg, var(--primary), var(--secondary));
            color: white;
            box-shadow: 0 10px 25px -5px rgba(139, 92, 246, 0.5);
        }

        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 15px 30px -5px rgba(139, 92, 246, 0.6);
        }

        .btn-glass {
            background: rgba(255, 255, 255, 0.1);
            color: white;
            border: 1px solid rgba(255, 255, 255, 0.2);
            backdrop-filter: blur(10px);
        }

        .btn-glass:hover {
            background: rgba(255, 255, 255, 0.2);
            transform: translateY(-2px);
        }

        .hero-image {
            position: relative;
            display: flex;
            justify-content: center;
        }

        .hero-card {
            background: var(--glass);
            border: 1px solid var(--glass-border);
            border-radius: 24px;
            padding: 2rem;
            backdrop-filter: blur(20px);
            width: 100%;
            max-width: 400px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            transform: perspective(1000px) rotateY(-5deg);
            transition: transform 0.3s ease;
        }

        .hero-card:hover {
            transform: perspective(1000px) rotateY(0deg) scale(1.02);
        }

        /* Stats Section */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 1.5rem;
            margin-top: 2rem;
        }

        .stat-box {
            background: rgba(0, 0, 0, 0.2);
            padding: 1rem;
            border-radius: 16px;
            text-align: center;
        }

        .stat-number {
            font-size: 1.5rem;
            font-weight: 700;
            color: var(--accent);
        }

        .stat-label {
            font-size: 0.9rem;
            color: var(--text-gray);
        }

        /* Features Section */
        .section {
            padding: 5rem 2rem;
            max-width: 1200px;
            margin: 0 auto;
        }

        .section-header {
            text-align: center;
            margin-bottom: 4rem;
        }

        .section-title {
            font-size: 2.5rem;
            font-weight: 700;
            margin-bottom: 1rem;
        }

        .section-subtitle {
            color: var(--text-gray);
            font-size: 1.1rem;
        }

        .features-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 2rem;
        }

        .feature-card {
            background: var(--glass);
            border: 1px solid var(--glass-border);
            border-radius: 20px;
            padding: 2rem;
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
            text-decoration: none;
            color: inherit;
            display: block;
        }

        .feature-card:hover {
            transform: translateY(-5px);
            background: rgba(255, 255, 255, 0.08);
            border-color: rgba(255, 255, 255, 0.3);
        }

        .feature-icon {
            width: 60px;
            height: 60px;
            background: linear-gradient(135deg, var(--primary), var(--secondary));
            border-radius: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.5rem;
            margin-bottom: 1.5rem;
            color: white;
        }

        .feature-title {
            font-size: 1.5rem;
            font-weight: 600;
            margin-bottom: 0.5rem;
        }

        .feature-desc {
            color: var(--text-gray);
            line-height: 1.5;
        }

        /* Articles Section */
        .articles-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 2rem;
        }

        .article-card {
            background: #1e293b;
            border-radius: 16px;
            overflow: hidden;
            border: 1px solid rgba(255, 255, 255, 0.05);
            transition: all 0.3s ease;
            text-decoration: none;
            color: inherit;
        }

        .article-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
        }

        .article-image {
            width: 100%;
            height: 200px;
            object-fit: cover;
        }

        .article-content {
            padding: 1.5rem;
        }

        .article-category {
            font-size: 0.8rem;
            color: var(--accent);
            text-transform: uppercase;
            letter-spacing: 0.05em;
            font-weight: 600;
            margin-bottom: 0.5rem;
        }

        .article-title {
            font-size: 1.2rem;
            font-weight: 600;
            margin-bottom: 0.5rem;
            line-height: 1.4;
            color: white;
        }

        .article-excerpt {
            font-size: 0.9rem;
            color: var(--text-gray);
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }

        /* Footer */
        .footer {
            border-top: 1px solid var(--glass-border);
            padding: 4rem 2rem 2rem;
            background: var(--bg-darker);
            text-align: center;
            color: var(--text-gray);
        }

        @keyframes float {
            0% {
                transform: translate(0, 0) scale(1);
            }

            50% {
                transform: translate(20px, -20px) scale(1.05);
            }

            100% {
                transform: translate(0, 0) scale(1);
            }
        }

        @media (max-width: 968px) {
            .hero-content {
                grid-template-columns: 1fr;
                text-align: center;
                gap: 3rem;
            }

            .hero-text h1 {
                font-size: 3rem;
            }

            .cta-group {
                justify-content: center;
            }

            .hero-card {
                margin: 0 auto;
                transform: none;
            }

            .hero-card:hover {
                transform: scale(1.02);
            }
        }
    </style>
</head>

<body>

    <?php include 'web_menu.php'; ?>

    <main>
        <!-- Hero Section -->
        <section class="hero">
            <div class="hero-bg"></div>
            <div class="hero-blob blob-1"></div>
            <div class="hero-blob blob-2"></div>

            <div class="hero-content">
                <div class="hero-text">
                    <h1>ค้นพบพลังแห่ง<br><span>ตัวเลขมงคล</span></h1>
                    <p>
                        Ananya Number Miracle<br>
                        เปลี่ยนเบอร์ เปลี่ยนชีวิต ด้วยศาสตร์แห่งตัวเลข<br>
                        วิเคราะห์ดวง วางเบอร์มงคล ทะเบียนรถ และชื่อมงคล
                    </p>
                    <div class="cta-group">
                        <a href="https://play.google.com/store/apps/details?id=com.numberniceic" target="_blank"
                            class="btn btn-primary">
                            <i class="fa-brands fa-google-play"></i> โหลดแอปเลย
                        </a>
                        <a href="/lucky/number" class="btn btn-glass">
                            <i class="fa-solid fa-star"></i> เลขนำโชค
                        </a>
                    </div>
                </div>

                <div class="hero-image">
                    <div class="hero-card">
                        <div style="display:flex; align-items:center; gap:1rem; margin-bottom:1.5rem;">
                            <img src="https://play-lh.googleusercontent.com/1zRGPJdAu_e7XrS-vC0cPEdrrEF5xhZ96Wm2VTzSUAgFihw36gGpwvt_dENOHY7A6DkDSDpFB2gpuKiNKg94=s128-rw"
                                alt="Icon" style="width:64px; height:64px; border-radius:16px;">
                            <div>
                                <h3 style="margin:0; font-size:1.2rem;">Ananya Number</h3>
                                <p style="margin:0; color:var(--text-gray); font-size:0.9rem;">Lifestyle</p>
                            </div>
                        </div>

                        <div class="stats-grid">
                            <div class="stat-box">
                                <div class="stat-number"><?php echo $appStats['downloads'] ?? '10K+'; ?></div>
                                <div class="stat-label">Downloads</div>
                            </div>
                            <div class="stat-box">
                                <div class="stat-number"><?php echo $appStats['rating'] ?? '4.8'; ?> <i
                                        class="fa-solid fa-star text-yellow-400" style="font-size:0.8em"></i></div>
                                <div class="stat-label">Rating</div>
                            </div>
                        </div>

                        <div
                            style="margin-top:1.5rem; padding:1rem; background:rgba(255,255,255,0.05); border-radius:12px;">
                            <p style="margin:0; font-size:0.9rem; color:#cbd5e1;">
                                <i class="fa-solid fa-quote-left" style="color:var(--primary); margin-right:8px;"></i>
                                แอปดีมากครับ แม่นยำเรื่องตัวเลข ช่วยตัดสินใจได้เยอะเลย
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- Features / Services -->
        <section class="section" id="features">
            <div class="section-header">
                <h2 class="section-title">บริการของเรา</h2>
                <p class="section-subtitle">ครบเครื่องเรื่องตัวเลขและมงคลชีวิต</p>
            </div>

            <div class="features-grid">
                <!-- Service 1 -->
                <a href="/shopsell/main" class="feature-card">
                    <div class="feature-icon">
                        <i class="fa-solid fa-sim-card"></i>
                    </div>
                    <h3 class="feature-title">เบอร์มงคล</h3>
                    <p class="feature-desc">ค้นหาเบอร์โทรศัพท์ที่เหมาะสมกับพื้นดวงของคุณ เสริมดวงการงาน การเงิน
                        และความรัก</p>
                </a>

                <!-- Service 2 -->
                <a href="/changenum/tabian" class="feature-card">
                    <div class="feature-icon">
                        <i class="fa-solid fa-car"></i>
                    </div>
                    <h3 class="feature-title">ทะเบียนรถมงคล</h3>
                    <p class="feature-desc">วิเคราะห์และจัดหาทะเบียนรถสวย เลขผลรวมดี ขับขี่ปลอดภัย แคล้วคลาด</p>
                </a>

                <!-- Service 3 -->
                <a href="/changenum/namenick" class="feature-card">
                    <div class="feature-icon">
                        <i class="fa-solid fa-signature"></i>
                    </div>
                    <h3 class="feature-title">วิเคราะห์ชื่อ</h3>
                    <p class="feature-desc">ตรวจสอบชื่อ-นามสกุล ว่าเป็นมงคลหรือไม่
                        พร้อมบริการตั้งชื่อใหม่ตามหลักเลขศาสตร์</p>
                </a>

                <!-- Service 4 -->
                <a href="/web/auspicious-list" class="feature-card">
                    <div class="feature-icon">
                        <i class="fa-solid fa-calendar-days"></i>
                    </div>
                    <h3 class="feature-title">ฤกษ์มงคล</h3>
                    <p class="feature-desc">ปฏิทินวันธงชัย วันพระ และฤกษ์ดีสำหรับการเริ่มต้นสิ่งใหม่ๆ ในชีวิต</p>
                </a>
            </div>
        </section>



        <footer class="footer">
            <!-- New Footer Section: Color Logic Explanation -->
            <div
                style="max-width: 1200px; margin: 0 auto; text-align: left; padding: 2rem 0; border-bottom: 1px solid var(--glass-border);">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 2rem;">

                    <!-- About Colors -->
                    <div>
                        <h4 style="color: var(--text-light); font-size: 1.2rem; margin-bottom: 1rem;">
                            <i class="fa-solid fa-palette" style="color: var(--secondary); margin-right: 8px;"></i>
                            หลักการแสดงผลสีมงคล 8 ช่อง
                        </h4>
                        <p style="color: var(--text-gray); font-size: 0.95rem; line-height: 1.6;">
                            ระบบของเราไม่ได้แสดงแค่สีเดียวต่อวัน แต่ใช้หลักการ <strong>"แตกเฉดสี" (Color
                                Expansion)</strong> เพื่อกระจายให้ครบ 8 ช่องอัตโนมัติ
                            ช่วยให้ท่านมีทางเลือกในการแต่งกายที่หลากหลายและครบถ้วนตามหลักทักษา
                        </p>
                        <ul
                            style="color: var(--text-gray); font-size: 0.9rem; margin-top: 1rem; padding-left: 1.5rem; line-height: 1.6;">
                            <li><strong>แถวบน (สีมงคล):</strong> แสดงเฉดสีเสริมดวง หากวันมงคลมีน้อย
                                ระบบจะดึงเฉดสีสำรองมาแสดงเพิ่มให้ครบ 8 ช่องเพื่อความสมบูรณ์</li>
                            <li><strong>แถวล่าง (สีกาลกิณี):</strong> เน้นย้ำสีที่ควรเลี่ยง
                                โดยดึงเฉดสีของวันกาลกิณีหลักมาแสดงซ้ำหลายเฉดให้ท่านสังเกตได้ง่ายชัดเจน</li>
                        </ul>
                    </div>

                    <!-- App Links (Existing context or new) -->
                    <div>
                        <h4 style="color: var(--text-light); font-size: 1.2rem; margin-bottom: 1rem;">
                            ดาวน์โหลดแอปพลิเคชัน</h4>
                        <a href="https://play.google.com/store/apps/details?id=com.numberniceic" target="_blank"
                            style="text-decoration: none; color: var(--text-gray); display: inline-flex; align-items: center; gap: 0.5rem; background: rgba(255,255,255,0.05); padding: 0.5rem 1rem; border-radius: 8px; transition: 0.3s;">
                            <i class="fa-brands fa-google-play" style="font-size: 1.2rem;"></i>
                            <span>Google Play Store</span>
                        </a>
                    </div>

                </div>
            </div>

            <p style="margin-top: 2rem;">&copy; <?php echo date('Y'); ?> Number เลขศาสตร์ พลังเงา (Ananya). All rights
                reserved.</p>
        </footer>
    </main>

</body>

</html>