<?php
$is_logged_in = isset($_SESSION['user']);

use App\Managers\ThaiCalendarHelper;

$today_str = date('Y-m-d');
$tomorrow_str = date('Y-m-d', strtotime('+1 day'));

$is_wanpra_today = ThaiCalendarHelper::isWanPra($today_str);
$is_wanpra_tomorrow = ThaiCalendarHelper::isWanPra($tomorrow_str);

$wanpra_msg = "";
if ($is_wanpra_today) {
    $wanpra_msg = "วันนี้วันพระ";
} elseif ($is_wanpra_tomorrow) {
    $wanpra_msg = "พรุ่งนี้วันพระ";
}
?>
<style>
    body {
        margin: 0;
        padding: 0;
        height: auto;
    }

    .navbar {
        background: linear-gradient(135deg, #198754, #20c997);
        padding: 0.75rem 2rem;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        display: flex;
        justify-content: space-between;
        align-items: center;
        color: white;
        position: relative;
        z-index: 1000;
    }

    .navbar-brand {
        font-size: 1.5rem;
        font-weight: bold;
        text-decoration: none;
        color: white;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    /* Desktop Menu */
    .navbar-menu {
        display: flex;
        gap: 1.25rem;
        align-items: center;
    }

    .navbar-link {
        color: white;
        text-decoration: none;
        font-weight: 500;
        transition: all 0.2s;
        display: flex;
        flex-direction: column;
        align-items: center;
        font-size: 0.95rem;
    }

    .navbar-link i {
        font-size: 1.2rem;
        margin-bottom: 2px;
    }

    .navbar-link:hover {
        color: #FFD700;
        transform: translateY(-2px);
    }

    /* Mobile Toggle */
    .navbar-toggle {
        display: none;
        background: none;
        border: none;
        color: white;
        font-size: 1.5rem;
        cursor: pointer;
        padding: 0.5rem;
    }

    /* Responsive Styles */
    @media (max-width: 1024px) {
        .navbar {
            padding: 0.75rem 1.5rem;
        }

        .navbar-toggle {
            display: block;
        }

        .navbar-menu {
            display: none;
            /* Hidden by default on mobile */
            position: absolute;
            top: 100%;
            left: 0;
            width: 100%;
            background: #198754;
            flex-direction: column;
            gap: 0;
            padding: 1rem 0;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .navbar-menu.active {
            display: flex;
        }

        .navbar-link {
            width: 100%;
            padding: 1rem;
            flex-direction: row;
            gap: 1rem;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            box-sizing: border-box;
        }

        .navbar-link:hover {
            background: rgba(255, 255, 255, 0.1);
            transform: none;
        }

        .navbar-link i {
            margin-bottom: 0;
            width: 25px;
            text-align: center;
        }
    }

    .wanpra-bar {
        background-color: #fff3cd;
        color: #856404;
        text-align: center;
        padding: 0.6rem;
        font-weight: 600;
        border-bottom: 1px solid #ffeeba;
        animation: fadeIn 0.5s ease-in-out;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
    }

    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(-10px);
        }

        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
</style>

<!-- FontAwesome for icons -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

<nav class="navbar">
    <a href="/" class="navbar-brand">
        <i class="fa-solid fa-clover"></i> ananya.in.th
    </a>

    <button class="navbar-toggle" id="menuToggle">
        <i class="fa-solid fa-bars"></i>
    </button>

    <div class="navbar-menu" id="navMenu">
        <a href="/" class="navbar-link"><i class="fa-solid fa-house"></i> <span>หน้าแรก</span></a>
        <a href="/articles" class="navbar-link"><i class="fa-solid fa-newspaper"></i> <span>บทความ</span></a>
        <a href="/services" class="navbar-link"><i class="fa-solid fa-list-check"></i> <span>ข้อปฏิบัติ</span></a>

        <a href="/web/auspicious-list" class="navbar-link"><i class="fa-solid fa-calendar-check"></i>
            <span>ปฏิทินวันมงคล</span></a>
        <a href="#" class="navbar-link"><i class="fa-solid fa-info-circle"></i> <span>เกี่ยวกับเรา</span></a>

        <?php if ($is_logged_in): ?>
            <a href="/web/admin/spells" class="navbar-link" style="color: #FFD700;"><i class="fa-solid fa-scroll"></i>
                <span>คาถา/คำเตือน</span></a>
            <a href="/web/dashboard" class="navbar-link" style="color: #FFD700;"><i class="fa-solid fa-user-circle"></i>
                <span>แดชบอร์ด</span></a>
            <a href="/web/logout" class="navbar-link" style="color: #ffcccc;"><i class="fa-solid fa-sign-out-alt"></i>
                <span>ออกจากระบบ</span></a>
        <?php else: ?>
            <a href="/web/login" class="navbar-link"><i class="fa-solid fa-right-to-bracket"></i>
                <span>เข้าสู่ระบบ</span></a>
            <a href="/web/register" class="navbar-link"><i class="fa-solid fa-user-plus"></i> <span>สมัครสมาชิก</span></a>
        <?php endif; ?>
    </div>
</nav>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const toggle = document.getElementById('menuToggle');
        const menu = document.getElementById('navMenu');

        toggle.addEventListener('click', function (e) {
            e.stopPropagation();
            menu.classList.toggle('active');
            const icon = toggle.querySelector('i');
            if (menu.classList.contains('active')) {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-xmark');
            } else {
                icon.classList.remove('fa-xmark');
                icon.classList.add('fa-bars');
            }
        });

        // Close menu when clicking outside
        document.addEventListener('click', function (e) {
            if (!menu.contains(e.target) && !toggle.contains(e.target)) {
                menu.classList.remove('active');
                const icon = toggle.querySelector('i');
                icon.classList.remove('fa-xmark');
                icon.classList.add('fa-bars');
            }
        });

        // Close menu when resizing to desktop
        window.addEventListener('resize', function () {
            if (window.innerWidth > 1024) {
                menu.classList.remove('active');
                const icon = toggle.querySelector('i');
                icon.classList.remove('fa-xmark');
                icon.classList.add('fa-bars');
            }
        });
    });
</script>

<?php if ($wanpra_msg): ?>
    <div class="wanpra-bar">
        <i class="fa-solid fa-bell"></i> <?php echo $wanpra_msg; ?>
    </div>
<?php endif; ?>