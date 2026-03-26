<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ปฏิทินวันมงคล - ananya.in.th</title>
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
            background: #f8f9fa;
        }

        .main-header {
            background: linear-gradient(135deg, #FFD700 0%, #FFC107 100%);
            padding: 2.5rem 1rem;
            text-align: center;
            color: #333;
            margin-bottom: 2rem;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .main-header h1 {
            font-family: 'Kanit', sans-serif;
            font-size: 2.5rem;
            margin: 0;
            color: #2c3e50;
            text-shadow: 1px 1px 0px rgba(255, 255, 255, 0.5);
        }

        .main-header p {
            color: #333;
            font-size: 1.1rem;
            margin-top: 0.5rem;
            opacity: 0.9;
        }

        .container {
            max-width: 900px;
            margin: 0 auto 4rem auto;
            padding: 0 1.5rem;
        }

        /* Card Style */
        .day-card {
            background: white;
            border-radius: 12px;
            padding: 1.5rem;
            margin-bottom: 1.5rem;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
            display: flex;
            flex-direction: row;
            gap: 1.5rem;
            transition: transform 0.2s, box-shadow 0.2s;
            border-left: 5px solid transparent;
        }

        .day-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
        }

        .day-card.wanpra {
            border-left-color: #ffc107;
        }

        /* Yellow for Buddhism */
        .day-card.tongchai {
            border-left-color: #198754;
        }

        /* Green for Success */
        .day-card.atipbadee {
            border-left-color: #0d6efd;
        }

        /* Blue for Authority */

        .date-box {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-width: 100px;
            text-align: center;
            padding-right: 1.5rem;
            border-right: 1px solid #eee;
        }

        .date-day {
            font-size: 2.5rem;
            font-weight: 700;
            color: #2c3e50;
            font-family: 'Kanit', sans-serif;
            line-height: 1;
        }

        .date-month {
            font-size: 1rem;
            color: #6c757d;
            margin-top: 0.5rem;
        }

        .date-year {
            font-size: 0.9rem;
            color: #adb5bd;
        }

        .details-box {
            flex: 1;
        }

        .badges {
            display: flex;
            gap: 0.5rem;
            margin-bottom: 0.75rem;
            flex-wrap: wrap;
        }

        .badge {
            padding: 0.35rem 0.8rem;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 0.4rem;
        }

        .badge-wanpra {
            background-color: #fff3cd;
            color: #856404;
            border: 1px solid #ffeeba;
        }

        .badge-tongchai {
            background-color: #d1e7dd;
            color: #0f5132;
            border: 1px solid #badbcc;
        }

        .badge-atipbadee {
            background-color: #cfe2ff;
            color: #084298;
            border: 1px solid #b6d4fe;
        }

        .description {
            color: #495057;
            line-height: 1.6;
            margin-top: 0.5rem;
            white-space: pre-line;
            /* Handle newline */
        }

        /* Mobile Responsive */
        @media (max-width: 600px) {
            .day-card {
                flex-direction: column;
                gap: 1rem;
            }

            .date-box {
                border-right: none;
                border-bottom: 1px solid #eee;
                padding-right: 0;
                padding-bottom: 1rem;
                flex-direction: row;
                gap: 0.5rem;
                align-items: baseline;
            }

            .date-day {
                font-size: 2rem;
            }

            .date-year {
                display: none;
            }

            /* Hide year on mobile to save space */
        }
    </style>
</head>

<body>

    <?php include 'web_menu.php'; ?>

    <header class="main-header">
        <h1>ปฏิทินวันมงคล</h1>
        <p>วันพระ วันธงชัย และวันอธิบดี พร้อมคำแนะนำในการปฏิบัติตน</p>
    </header>

    <main class="container">
        <?php if (!empty($auspiciousDays)): ?>
            <?php
            $thaiMonths = [
                1 => 'มกราคม',
                2 => 'กุมภาพันธ์',
                3 => 'มีนาคม',
                4 => 'เมษายน',
                5 => 'พฤษภาคม',
                6 => 'มิถุนายน',
                7 => 'กรกฎาคม',
                8 => 'สิงหาคม',
                9 => 'กันยายน',
                10 => 'ตุลาคม',
                11 => 'พฤศจิกายน',
                12 => 'ธันวาคม'
            ];
            ?>

            <?php foreach ($auspiciousDays as $day): ?>
                <?php
                $dateObj = new DateTime($day->date);
                $d = $dateObj->format('j');
                $m = (int) $dateObj->format('n');
                $y = (int) $dateObj->format('Y') + 543;

                // Determine dominant class for border color
                $cardClass = '';
                if ($day->is_wanpra)
                    $cardClass = 'wanpra';
                else if ($day->is_tongchai)
                    $cardClass = 'tongchai';
                else if ($day->is_atipbadee)
                    $cardClass = 'atipbadee';
                ?>
                <div class="day-card <?php echo $cardClass; ?>">
                    <div class="date-box">
                        <span class="date-day">
                            <?php echo $d; ?>
                        </span>
                        <div style="display:flex; flex-direction:column; align-items:center;">
                            <span class="date-month">
                                <?php echo $thaiMonths[$m]; ?>
                            </span>
                            <span class="date-year">
                                <?php echo $y; ?>
                            </span>
                        </div>
                    </div>
                    <div class="details-box">
                        <div class="badges">
                            <?php if ($day->is_wanpra): ?>
                                <span class="badge badge-wanpra"><i class="fa-solid fa-hands-praying"></i> วันพระ</span>
                            <?php endif; ?>
                            <?php if ($day->is_tongchai): ?>
                                <span class="badge badge-tongchai"><i class="fa-solid fa-flag"></i> วันธงชัย</span>
                            <?php endif; ?>
                            <?php if ($day->is_atipbadee): ?>
                                <span class="badge badge-atipbadee"><i class="fa-solid fa-star"></i> วันอธิบดี</span>
                            <?php endif; ?>
                        </div>
                        <div class="description">
                            <?php echo nl2br(htmlspecialchars($day->description)); ?>
                        </div>
                    </div>
                </div>
            <?php endforeach; ?>
        <?php else: ?>
            <div style="text-align: center; color: #999; padding: 3rem;">
                <i class="fa-regular fa-calendar-xmark" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                <p>ไม่พบข้อมูลวันมงคลในช่วงนี้</p>
            </div>
        <?php endif; ?>
    </main>

</body>

</html>