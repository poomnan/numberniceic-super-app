<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 Not Found - Ananya</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;600&display=swap');

        :root {
            --bg-color: #0f172a;
            --card-bg: rgba(255, 255, 255, 0.05);
            --text-color: #e2e8f0;
            --accent-color: #8b5cf6;
            --accent-secondary: #ec4899;
        }

        body {
            margin: 0;
            padding: 0;
            font-family: 'Kanit', sans-serif;
            background-color: var(--bg-color);
            color: var(--text-color);
            height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            background: radial-gradient(circle at top left, #1e1b4b, #0f172a);
        }

        .background-blobs {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: -1;
            overflow: hidden;
        }

        .blob {
            position: absolute;
            filter: blur(80px);
            opacity: 0.6;
            animation: float 10s infinite ease-in-out;
        }

        .blob-1 {
            top: -10%;
            left: -10%;
            width: 500px;
            height: 500px;
            background: var(--accent-color);
            animation-delay: 0s;
        }

        .blob-2 {
            bottom: -10%;
            right: -10%;
            width: 400px;
            height: 400px;
            background: var(--accent-secondary);
            animation-delay: -5s;
        }

        @keyframes float {
            0% {
                transform: translate(0, 0) scale(1);
            }

            50% {
                transform: translate(30px, -50px) scale(1.1);
            }

            100% {
                transform: translate(0, 0) scale(1);
            }
        }

        .glass-card {
            background: var(--card-bg);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 24px;
            padding: 3rem;
            text-align: center;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            max-width: 90%;
            width: 500px;
            position: relative;
            overflow: hidden;
        }

        .glass-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: -50%;
            width: 200%;
            height: 100%;
            background: linear-gradient(to right,
                    transparent,
                    rgba(255, 255, 255, 0.05),
                    transparent);
            transform: skewX(-25deg);
            animation: shine 8s infinite;
        }

        @keyframes shine {
            0% {
                transform: translateX(-100%) skewX(-25deg);
            }

            20% {
                transform: translateX(100%) skewX(-25deg);
            }

            100% {
                transform: translateX(100%) skewX(-25deg);
            }
        }

        h1 {
            font-size: 8rem;
            margin: 0;
            line-height: 1;
            background: linear-gradient(135deg, var(--accent-color), var(--accent-secondary));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            font-weight: 700;
            letter-spacing: -5px;
        }

        h2 {
            font-size: 2rem;
            margin: 1rem 0;
            font-weight: 600;
        }

        p {
            font-size: 1.1rem;
            color: #94a3b8;
            margin-bottom: 2.5rem;
        }


        .buttons {
            display: flex;
            gap: 1rem;
            justify-content: center;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            padding: 12px 32px;
            background: linear-gradient(90deg, var(--accent-color), var(--accent-secondary));
            color: white;
            text-decoration: none;
            border-radius: 50px;
            font-weight: 600;
            font-size: 1.1rem;
            transition: all 0.3s ease;
            box-shadow: 0 10px 20px -5px rgba(139, 92, 246, 0.5);
            border: none;
            cursor: pointer;
        }

        .btn-secondary {
            background: transparent;
            border: 2px solid rgba(255, 255, 255, 0.2);
            box-shadow: none;
        }

        .btn-secondary:hover {
            background: rgba(255, 255, 255, 0.1);
            border-color: rgba(255, 255, 255, 0.4);
            transform: translateY(-2px);
            box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.2);
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 15px 25px -5px rgba(139, 92, 246, 0.6);
            filter: brightness(1.1);
        }

        .icon-container {
            font-size: 4rem;
            margin-bottom: 1rem;
            animation: bounce 2s infinite;
        }

        @keyframes bounce {

            0%,
            20%,
            50%,
            80%,
            100% {
                transform: translateY(0);
            }

            40% {
                transform: translateY(-20px);
            }

            60% {
                transform: translateY(-10px);
            }
        }
    </style>
</head>

<body>
    <div class="background-blobs">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>
    </div>

    <div class="glass-card">
        <div class="icon-container">
            👻
        </div>
        <h1>404</h1>
        <h2>Oops! Page Not Found</h2>
        <p>
            ขออภัย เราหาหน้าที่คุณต้องการไม่เจอ<br>
            อาจมีการย้ายหน้า หรือคุณอาจพิมพ์ลิงก์ผิด
        </p>
        <div class="buttons">
            <a href="javascript:history.back()" class="btn btn-secondary">
                ย้อนกลับ
            </a>
            <a href="/" class="btn">
                กลับหน้าหลัก
            </a>
        </div>
    </div>
</body>

</html>