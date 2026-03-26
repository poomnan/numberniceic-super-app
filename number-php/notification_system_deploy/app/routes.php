<?php
use Slim\Factory\AppFactory;
use Slim\Routing\RouteCollectorProxy;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

// Load Routes
$app->get('/test', function ($request, $response) {
    $response->getBody()->write("Test Route Success");
    return $response;
});

$app->get('/migrate-spells-v2', function ($request, $response) use ($container) {
    try {
        $db = $container->get('db');

        // 1. Create the new member_spell_notes table
        $sql1 = "CREATE TABLE IF NOT EXISTS member_spell_notes (
            id INT AUTO_INCREMENT PRIMARY KEY,
            memberid VARCHAR(50) NOT NULL,
            spell_id INT NOT NULL,
            note TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY member_spell (memberid, spell_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        $db->exec($sql1);

        // 2. Remove the note column from spells_warnings if it exists
        $stmt = $db->prepare("SHOW COLUMNS FROM spells_warnings LIKE 'note'");
        $stmt->execute();
        if ($stmt->fetch()) {
            $sql2 = "ALTER TABLE spells_warnings DROP COLUMN note;";
            $db->exec($sql2);
            $msg = "Success: Table created and column dropped.";
        } else {
            $msg = "Success: Table created (column already dropped).";
        }

        $response->getBody()->write($msg);
    } catch (\Exception $e) {
        $response->getBody()->write("Error: " . $e->getMessage());
    }
    return $response;
});

// TEST SPELL ROUTE WITH CLOSURE
$app->get('/admin/spell/test-list', function ($request, $response) use ($container) {
    $db = $container->get('db');
    $stmt = $db->query("SELECT * FROM spells_warnings ORDER BY id DESC");
    $items = $stmt->fetchAll(\PDO::FETCH_ASSOC);

    $domain = "https://ananya.in.th";
    foreach ($items as &$item) {
        if (!empty($item['photo'])) {
            $item['photo_url'] = $domain . $item['photo'];
        } else {
            $item['photo_url'] = "";
        }
        $item['note'] = $item['note'] ?? "";
    }

    $response->getBody()->write(json_encode(['status' => 'success', 'source' => 'closure', 'data' => $items]));
    return $response->withHeader('Content-Type', 'application/json');
});

$app->get('/debug/bagcolor', function ($request, $response) use ($container) {
    $db = $container->get('db');
    $res = ["status" => "running", "tables" => []];
    try {
        $stmt = $db->query("SHOW TABLES LIKE 'bagcolortb'");
        $res["tables"]["bagcolortb"] = ($stmt->rowCount() > 0);
        if ($res["tables"]["bagcolortb"]) {
            $stmt = $db->query("SELECT * FROM bagcolortb LIMIT 5");
            $res["data"]["bagcolortb"] = $stmt->fetchAll();
        }

        $stmt = $db->query("SHOW TABLES LIKE 'colortb'");
        $res["tables"]["colortb"] = ($stmt->rowCount() > 0);
        if ($res["tables"]["colortb"]) {
            $stmt = $db->query("SELECT * FROM colortb LIMIT 5");
            $res["data"]["colortb"] = $stmt->fetchAll();
        }
    } catch (\Exception $e) {
        $res["error"] = $e->getMessage();
    }
    $response->getBody()->write(json_encode($res, JSON_PRETTY_PRINT));
    return $response->withHeader('Content-Type', 'application/json');
});
$app->get('/debug/structure', function ($request, $response) use ($container) {
    try {
        $db = $container->get('db');
        $res = ["status" => "running", "schema" => []];
        $tables = ['bagcolortb', 'colortb', 'membertb'];
        foreach ($tables as $table) {
            $stmt = $db->query("DESCRIBE $table");
            $res["schema"][$table] = $stmt->fetchAll();
        }
        $response->getBody()->write(json_encode($res, JSON_PRETTY_PRINT));
    } catch (\Exception $e) {
        $response->getBody()->write(json_encode(["error" => $e->getMessage()]));
    }
    return $response->withHeader('Content-Type', 'application/json');
});

$app->get('/debug/requests', function ($request, $response) {
    $logFile = __DIR__ . '/../cache/requests.log';
    if (file_exists($logFile)) {
        $lines = array_slice(explode("\n", file_get_contents($logFile)), -50); // Last 50 lines
        $response->getBody()->write("<pre>" . htmlspecialchars(implode("\n", $lines)) . "</pre>");
    } else {
        $response->getBody()->write("No request logs found.");
    }
    return $response;
});

$app->get('/debug/logs', function ($request, $response) {
    $logFile = __DIR__ . '/../cache/bagcolor_save.log';
    if (file_exists($logFile)) {
        $response->getBody()->write("<pre>" . htmlspecialchars(file_get_contents($logFile)) . "</pre>");
    } else {
        $response->getBody()->write("No logs found at " . $logFile);
    }
    return $response;
});


$app->get('/debug/birthday', function ($request, $response) {
    $file = __DIR__ . '/../public/debug_birthday.txt';
    $publicDir = __DIR__ . '/../public/';
    $isWritable = is_writable($publicDir);

    $out = "--- Birthday Debug System (Version 5.0) ---\n";
    $out .= "Server Time: " . date("Y-m-d H:i:s") . "\n";
    $out .= "Directory: $publicDir\n";
    $out .= "Directory Writable: " . ($isWritable ? "YES" : "NO") . "\n";
    $out .= "------------------------------------------\n\n";

    if (file_exists($file)) {
        $out .= file_get_contents($file);
    } else {
        $out .= "Log Status: NO DATA YET\n";
        $out .= "Action: Please perform a registration or update in the app now.";
    }

    $response->getBody()->write("<pre>$out</pre>");
    return $response;
});

$app->get('/debug/controller', function ($request, $response) {
    $out = "--- Controller Inspection ---\n";
    if (class_exists(\App\Managers\UserController::class)) {
        $reflector = new \ReflectionClass(\App\Managers\UserController::class);
        $path = $reflector->getFileName();
        $code = file_get_contents($path);

        $hasLog = (strpos($code, 'debug_birthday.txt') !== false);
        $hasPlusOne = (strpos($code, '$smonth + 1') !== false);
        $hasSprintfFix = (strpos($code, 'sprintf("%04d-%02d-%02d"') !== false);

        $out .= "Loaded Path: $path\n";
        $out .= "Has Logging Code: " . ($hasLog ? "YES" : "NO") . "\n";
        $out .= "Has +1 Bug: " . ($hasPlusOne ? "YES" : "NO") . "\n";
        $out .= "Has Sprintf Fix: " . ($hasSprintfFix ? "YES" : "NO") . "\n";
    } else {
        $out .= "ERROR: UserController class not found in autoloader!\n";
    }
    $response->getBody()->write("<pre>$out</pre>");
    return $response;
});

$app->get('/check-controller', function ($request, $response) use ($app) {
    $exists = class_exists(\App\Managers\AdminController::class);
    $data = [
        'AdminController_exists' => $exists,
        'AdminController_class' => \App\Managers\AdminController::class,
        'basePath' => $app->getBasePath(),
        'uriPath' => $request->getUri()->getPath()
    ];
    $response->getBody()->write(json_encode($data));
    return $response->withHeader('Content-Type', 'application/json');
});

$app->get('/debug/db-test', function ($request, $response) use ($container) {
    try {
        $db = $container->get('db');
        $stmt = $db->query("SELECT COUNT(*) FROM bagcolortb");
        $count = $stmt->fetchColumn();
        $response->getBody()->write(json_encode(["status" => "ok", "count" => $count]));
    } catch (\Exception $e) {
        $response->getBody()->write(json_encode(["status" => "error", "message" => $e->getMessage()]));
    }
    return $response->withHeader('Content-Type', 'application/json');
});

$app->get('/', \App\Managers\HomePageManager::class . ':index');

// --- Member & Auth ---
$app->post('/member/login', \App\Managers\UserController::class . ':userLogin');
$app->post('/member/updateToken', \App\Managers\UserController::class . ':updateFcmToken');
$app->get('/member/currenttime', \App\Managers\UserController::class . ':currentTime');
$app->post('/member/register', \App\Managers\UserController::class . ':userRegister');
$app->post('/member/register/v2', \App\Managers\UserController::class . ':userRegisterV2');
$app->post('/member/register/successcf', \App\Managers\UserController::class . ':successInsertConfirm');
$app->post('/member/update', \App\Managers\UserController::class . ':memberUpdate');
$app->post('/member/vipcode', \App\Managers\UserController::class . ':userAddVipCode');
$app->get('/vipcode/{vipcode}', \App\Managers\UserController::class . ':vipCode');

// --- Bag Color & Dress Color & Fortune (Person Section) ---
$app->get('/member/bagcolor/{memberid}/{age1}/{age2}', \App\Managers\UserController::class . ':bagColor');
$app->get('/member/dresscolor/{days}', \App\Managers\UserController::class . ':dressColor');
$app->get('/member/wanpra/{wandate}', \App\Managers\UserController::class . ':wanPra');
$app->get('/member/wanspecial/{wandate}', \App\Managers\UserController::class . ':wanSpecial');
$app->get('/member/miradoV2/{activity}/{birthday}/{today}/{currentday}', \App\Managers\UserController::class . ':miraDoV2');
$app->get('/member/mirado/{activity}/{birthday}/{today}', \App\Managers\UserController::class . ':miraDo');
$app->get('/member/lengyam', \App\Managers\UserController::class . ':lengyamList');

// --- News ---
$app->get('/news/topic24', \App\Managers\NewsController::class . ':newsTop24');
$app->get('/news/topicall/{newsidtype}', \App\Managers\NewsController::class . ':newsTypeAll');
$app->get('/news/topic/{newsidtype}', \App\Managers\NewsController::class . ':newsTypeAll');
$app->get('/news/api/article/{number}', \App\Managers\NewsController::class . ':newsNumberViewDetail');

// --- Fortune & Tools ---
$app->get('/home/main/{homeid}', \App\Managers\HomeController::class . ':main');
$app->get('/tabian/main/{carid}', \App\Managers\TabianController::class . ':main');
$app->get('/phone/{phoneNumber}', \App\Managers\Telephone\PairController::class . ':mainPhone');
$app->get('/shopsell/main', \App\Managers\Telephone\PhoneNumberSellManager::class . ':mainPhoneSell');
$app->get('/tabian/sell/all', \App\Managers\TabianController::class . ':listTabianSell');
$app->get('/nickname/main/{nickname}/{birthday}', \App\Managers\NickNameController::class . ':main');
$app->get('/name/main/{name}/{surname}/{birthday}', \App\Managers\NameController::class . ':main');
$app->get('/nickname/list/{usetable}/{day}/{charx}/{prefix}', \App\Managers\NickNameVipController::class . ':listNickNameByAll');
$app->get('/nickname/list/{usetable}/{day}/{charx}/{prefix}/{lastid}', \App\Managers\NickNameVipController::class . ':listNameVip');

// --- Admin & Assignments ---
$app->get('/admin/finduser/bagcolor/{username}', \App\Managers\AdminController::class . ':findUserBagColor');
$app->get('/admin/finduser/bagcolor', \App\Managers\AdminController::class . ':findUserBagColor');
$app->get('/admin/finduser/bagcolor/', \App\Managers\AdminController::class . ':findUserBagColor');
$app->put('/admin/bagcolor', function ($request, $response) use ($container) {
    $controller = new \App\Managers\AdminController($container);
    return $controller->updateBagColorById($request, $response);
});
$app->put('/admin/bagcolor/', function ($request, $response) use ($container) {
    $controller = new \App\Managers\AdminController($container);
    return $controller->updateBagColorById($request, $response);
});
$app->post('/admin/bagcolor', function ($request, $response) use ($container) {
    $controller = new \App\Managers\AdminController($container);
    return $controller->insertBagColorByUserId($request, $response);
});
$app->post('/admin/bagcolor/', function ($request, $response) use ($container) {
    $controller = new \App\Managers\AdminController($container);
    return $controller->insertBagColorByUserId($request, $response);
});
$app->get('/admin/bagcolor/{userid}', \App\Managers\AdminController::class . ':colorBagByUserId');
$app->get('/admin/bagcolor/{userid}/', \App\Managers\AdminController::class . ':colorBagByUserId');
$app->get('/admin/buddha/pangs', \App\Managers\BuddhaPangController::class . ':getBuddhaPangsApi');
$app->get('/admin/spell/api', \App\Managers\SpellAPIController::class . ':getAll');
$app->get('/admin/spell/api/', \App\Managers\SpellAPIController::class . ':getAll');
$app->get('/admin/spell/api/{id}', \App\Managers\SpellAPIController::class . ':getById');
$app->get('/admin/temple/api', \App\Managers\SacredTempleController::class . ':getTemplesApi');
$app->post('/admin/merit/assign', \App\Managers\SacredTempleController::class . ':assignMerit');
$app->post('/admin/buddha/assign', \App\Managers\BuddhaPangController::class . ':assignToUser');
$app->post('/admin/temple/assign', \App\Managers\SacredTempleController::class . ':assignToUser');

$app->get('/member/buddha/assigned/{memberid}', \App\Managers\BuddhaPangController::class . ':getAssigned');
$app->get('/admin/temple/assigned/{memberid}', \App\Managers\SacredTempleController::class . ':getAssigned');
// --- Admin & Name Management ---
$app->post('/admin/nickname', \App\Managers\AdminController::class . ':addNickName');
$app->post('/admin/realname', \App\Managers\AdminController::class . ':addRealName');
$app->post('/admin/addnickname', \App\Managers\AdminController::class . ':addNickName');
$app->post('/admin/addrealname', \App\Managers\AdminController::class . ':addRealName');
$app->post('/admin/secretcode/list', \App\Managers\AdminController::class . ':addVipcode');

// --- Admin Web Pages (Missing) ---
$app->get('/admin/buddha', \App\Managers\BuddhaPangController::class . ':viewList');
$app->get('/admin/buddha/add', \App\Managers\BuddhaPangController::class . ':viewAdd');
$app->get('/admin/buddha/edit/{id}', \App\Managers\BuddhaPangController::class . ':viewEdit');
$app->post('/admin/buddha/save', \App\Managers\BuddhaPangController::class . ':save');
$app->get('/admin/buddha/delete/{id}', \App\Managers\BuddhaPangController::class . ':delete');

$app->get('/admin/temple', \App\Managers\SacredTempleController::class . ':viewList');
$app->get('/admin/temple/add', \App\Managers\SacredTempleController::class . ':viewAdd');
$app->get('/admin/temple/edit/{id}', \App\Managers\SacredTempleController::class . ':viewEdit');
$app->post('/admin/temple/save', \App\Managers\SacredTempleController::class . ':save');
$app->get('/admin/temple/delete/{id}', \App\Managers\SacredTempleController::class . ':delete');

$app->get('/web/admin/news', \App\Managers\AdminNewsController::class . ':index');
$app->get('/web/admin/news/create', \App\Managers\AdminNewsController::class . ':create');
$app->post('/web/admin/news/store', \App\Managers\AdminNewsController::class . ':store');
$app->get('/web/admin/news/edit/{id}', \App\Managers\AdminNewsController::class . ':edit');
$app->post('/web/admin/news/update/{id}', \App\Managers\AdminNewsController::class . ':update');
$app->get('/web/admin/news/delete/{id}', \App\Managers\AdminNewsController::class . ':delete');

// --- Admin Spells & Special Warnings ---
$app->get('/web/admin/spells', \App\Managers\AdminSpellController::class . ':index');
$app->get('/web/admin/spells/create', \App\Managers\AdminSpellController::class . ':create');
$app->post('/web/admin/spells/store', \App\Managers\AdminSpellController::class . ':store');
$app->get('/web/admin/spells/edit/{id}', \App\Managers\AdminSpellController::class . ':edit');
$app->post('/web/admin/spells/update/{id}', \App\Managers\AdminSpellController::class . ':update');
$app->get('/web/admin/spells/delete/{id}', \App\Managers\AdminSpellController::class . ':delete');

// --- Additional Admin Pages (Fix 404s) ---
$app->get('/web/admin/users', function ($request, $response) use ($container) {
    return $container->get('view')->render($response, 'web_admin_users.php');
});

$app->get('/web/admin/images', function ($request, $response) use ($container) {
    return $container->get('view')->render($response, 'web_admin_images.php');
});

$app->get('/web/admin/bag-colors', function ($request, $response) use ($container) {
    return $container->get('view')->render($response, 'web_admin_bag_colors.php');
});

$app->get('/admin/notifications/custom', \App\Managers\NotificationController::class . ':viewCustomNotify');
$app->post('/admin/notifications/custom/send', \App\Managers\NotificationController::class . ':sendCustomNotify');
$app->get('/admin/notifications/send-bag-colors', \App\Managers\NotificationController::class . ':sendBagColors');

$app->get('/web/admin/api-doc/news', function ($request, $response) use ($container) {
    return $container->get('view')->render($response, 'web_api_doc_news.php');
});

// --- Merit View (App WebView) ---
// $app->get('/merit/view', \App\Managers\MeritController::class . ':view');

// --- Admin Tabian Management (Restored) ---
// API for Android App
$app->post('/admin/tabian/save', \App\Managers\AdminTabianController::class . ':saveTabian');
$app->post('/admin/tabian/delete', \App\Managers\AdminTabianController::class . ':deleteTabian');

$app->get('/web/admin/tabians', function ($request, $response) use ($container) {
    if (session_status() == PHP_SESSION_NONE) {
        session_start();
    }
    $user = $_SESSION['user'] ?? null;
    if (
        !$user || !isset($user->vipcode) ||
        ($user->vipcode !== 'admin' && $user->vipcode !== 'administrator')
    ) {
        return $response->withHeader('Location', '/web/login')->withStatus(302);
    }

    $db = $container->get('db');
    // Order by order_no ASC (Manual), then Newest first
    $stmt = $db->prepare("SELECT * FROM tabian_sell ORDER BY order_no ASC, tabian_id DESC");
    $stmt->execute();
    $tabians = $stmt->fetchAll(\PDO::FETCH_OBJ);

    return $container->get('view')->render($response, 'web_admin_tabians.php', [
        'user' => $user,
        'tabians' => $tabians
    ]);
});

// Create Form
$app->get('/web/admin/tabians/create', function ($request, $response) use ($container) {
    if (session_status() == PHP_SESSION_NONE) {
        session_start();
    }
    $user = $_SESSION['user'] ?? null;
    if (!$user || !isset($user->vipcode) || ($user->vipcode !== 'admin' && $user->vipcode !== 'administrator')) {
        return $response->withHeader('Location', '/web/login')->withStatus(302);
    }
    return $container->get('view')->render($response, 'web_admin_tabian_form.php', ['user' => $user, 'tabian' => null]);
});

// Edit Form
$app->get('/web/admin/tabians/edit/{id}', function ($request, $response, $args) use ($container) {
    if (session_status() == PHP_SESSION_NONE) {
        session_start();
    }
    $user = $_SESSION['user'] ?? null;
    if (!$user || !isset($user->vipcode) || ($user->vipcode !== 'admin' && $user->vipcode !== 'administrator')) {
        return $response->withHeader('Location', '/web/login')->withStatus(302);
    }

    $id = $args['id'];
    $db = $container->get('db');
    $stmt = $db->prepare("SELECT * FROM tabian_sell WHERE tabian_id = ?");
    $stmt->execute([$id]);
    $tabian = $stmt->fetch(\PDO::FETCH_OBJ);

    if (!$tabian) {
        return $response->withHeader('Location', '/web/admin/tabians')->withStatus(302);
    }

    return $container->get('view')->render($response, 'web_admin_tabian_form.php', ['user' => $user, 'tabian' => $tabian]);
});

// Store (Insert)
$app->post('/web/admin/tabians/store', function ($request, $response) use ($container) {
    $data = $request->getParsedBody();
    $db = $container->get('db');

    $sql = "INSERT INTO tabian_sell (tabian_number, tabian_province, tabian_category, tabian_price, tabian_status, order_no) 
            VALUES (:no, :prov, :cat, :price, :status, :ord)";
    $stmt = $db->prepare($sql);
    $stmt->execute([
        ':no' => $data['tabian_number'],
        ':prov' => $data['tabian_province'] ?? '',
        ':cat' => $data['tabian_category'] ?? '',
        ':price' => $data['tabian_price'] ?? 0,
        ':status' => $data['tabian_status'] ?? 'available',
        ':ord' => $data['order_no'] ?? 0
    ]);

    return $response->withHeader('Location', '/web/admin/tabians')->withStatus(302);
});

// Update
$app->post('/web/admin/tabians/update/{id}', function ($request, $response, $args) use ($container) {
    $id = $args['id'];
    $data = $request->getParsedBody();
    $db = $container->get('db');

    $sql = "UPDATE tabian_sell SET 
            tabian_number = :no, 
            tabian_province = :prov, 
            tabian_category = :cat, 
            tabian_price = :price, 
            tabian_status = :status,
            order_no = :ord
            WHERE tabian_id = :id";

    $stmt = $db->prepare($sql);
    $stmt->execute([
        ':no' => $data['tabian_number'],
        ':prov' => $data['tabian_province'] ?? '',
        ':cat' => $data['tabian_category'] ?? '',
        ':price' => $data['tabian_price'] ?? 0,
        ':status' => $data['tabian_status'] ?? 'available',
        ':ord' => $data['order_no'] ?? 0,
        ':id' => $id
    ]);

    return $response->withHeader('Location', '/web/admin/tabians')->withStatus(302);
});

// Delete
$app->get('/web/admin/tabians/delete/{id}', function ($request, $response, $args) use ($container) {
    if (session_status() == PHP_SESSION_NONE) {
        session_start();
    }
    $user = $_SESSION['user'] ?? null;
    if (!$user || !isset($user->vipcode) || ($user->vipcode !== 'admin' && $user->vipcode !== 'administrator')) {
        return $response->withHeader('Location', '/web/login')->withStatus(302);
    }

    $id = $args['id'];
    $db = $container->get('db');
    $stmt = $db->prepare("DELETE FROM tabian_sell WHERE tabian_id = ?");
    $stmt->execute([$id]);

    return $response->withHeader('Location', '/web/admin/tabians')->withStatus(302);
});

// --- Lucky Number ---
$app->get('/lucky/number', \App\Managers\NewsController::class . ':getLuckyNumber');
$app->get('/lucky/number/', \App\Managers\NewsController::class . ':getLuckyNumber');
$app->post('/lucky/number', \App\Managers\NewsController::class . ':postLuckyNumber');
$app->post('/lucky/number/', \App\Managers\NewsController::class . ':postLuckyNumber');
$app->post('/lucky/number/v2', \App\Managers\NewsController::class . ':postLuckyNumberV2');
$app->post('/lucky/number/v2/', \App\Managers\NewsController::class . ':postLuckyNumberV2');

// --- Spells API ---
$app->get('/api/spell/latest', \App\Managers\SpellAPIController::class . ':latest');
$app->post('/admin/spell/assign', \App\Managers\SpellAPIController::class . ':assign');
$app->post('/admin/spell/assign/', \App\Managers\SpellAPIController::class . ':assign');
$app->post('/admin/spell/update-note', \App\Managers\SpellAPIController::class . ':updateNote');
$app->post('/admin/spell/update-note/', \App\Managers\SpellAPIController::class . ':updateNote');

// --- Bag Color Update API ---
$app->post('/api/bag-color/update', function ($request, $response) use ($container) {
    // Prevent any previous output from breaking the JSON response
    if (ob_get_level() > 0)
        ob_clean();

    try {
        $body = $request->getParsedBody();

        // Handle both object and array just in case
        $memberid = isset($body['memberid']) ? (string) $body['memberid'] : '';
        $age = isset($body['age']) ? (string) $body['age'] : '';

        // Color A (Current Year)
        $c1a = $body['color0'] ?? '';
        $c2a = $body['color1'] ?? '';
        $c3a = $body['color2'] ?? '';
        $c4a = $body['color3'] ?? '';
        $c5a = $body['color4'] ?? '';
        $c6a = $body['color5'] ?? '';

        // Color B (Next Year)
        $c1b = $body['colorb0'] ?? '';
        $c2b = $body['colorb1'] ?? '';
        $c3b = $body['colorb2'] ?? '';
        $c4b = $body['colorb3'] ?? '';
        $c5b = $body['colorb4'] ?? '';
        $c6b = $body['colorb5'] ?? '';

        $successA = false;
        $successB = false;

        $age1 = (string) $age;
        $age2 = (string) ((int) $age + 1);

        // --- PROCESS COLOR A ---
        $db = $container->get('db');
        $stmtA = $db->prepare("SELECT bag_id FROM bagcolortb WHERE memberid = :mid AND age = :age ORDER BY bag_id ASC LIMIT 1");
        $stmtA->execute([':mid' => $memberid, ':age' => $age1]);
        $rowA = $stmtA->fetch(\PDO::FETCH_OBJ);

        if ($rowA) {
            // Update existing
            $sqlUpA = "UPDATE bagcolortb SET age = :age, bag_color1 = :c1, bag_color2 = :c2, bag_color3 = :c3, bag_color4 = :c4, bag_color5 = :c5, bag_color6 = :c6, date_color_updated = NOW() WHERE bag_id = :id";
            $successA = $db->prepare($sqlUpA)->execute([
                ':age' => $age1,
                ':c1' => $c1a,
                ':c2' => $c2a,
                ':c3' => $c3a,
                ':c4' => $c4a,
                ':c5' => $c5a,
                ':c6' => $c6a,
                ':id' => $rowA->bag_id
            ]);
        } else {
            // Insert new if not found
            $sqlInsA = "INSERT INTO bagcolortb (memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated) VALUES (:mid, :age, :c1, :c2, :c3, :c4, :c5, :c6, NOW())";
            $successA = $db->prepare($sqlInsA)->execute([
                ':mid' => $memberid,
                ':age' => $age1,
                ':c1' => $c1a,
                ':c2' => $c2a,
                ':c3' => $c3a,
                ':c4' => $c4a,
                ':c5' => $c5a,
                ':c6' => $c6a
            ]);
        }

        // --- PROCESS COLOR B ---
        $stmtB = $db->prepare("SELECT bag_id FROM bagcolortb WHERE memberid = :mid AND age = :age ORDER BY bag_id ASC LIMIT 1");
        $stmtB->execute([':mid' => $memberid, ':age' => $age2]);
        $rowB = $stmtB->fetch(\PDO::FETCH_OBJ);

        if ($rowB) {
            $sqlUpB = "UPDATE bagcolortb SET age = :age, bag_color1 = :c1, bag_color2 = :c2, bag_color3 = :c3, bag_color4 = :c4, bag_color5 = :c5, bag_color6 = :c6, date_color_updated = NOW() WHERE bag_id = :id";
            $successB = $db->prepare($sqlUpB)->execute([
                ':age' => $age2,
                ':c1' => $c1b,
                ':c2' => $c2b,
                ':c3' => $c3b,
                ':c4' => $c4b,
                ':c5' => $c5b,
                ':c6' => $c6b,
                ':id' => $rowB->bag_id
            ]);
        } else {
            $sqlInsB = "INSERT INTO bagcolortb (memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated) VALUES (:mid, :age, :c1, :c2, :c3, :c4, :c5, :c6, NOW())";
            $successB = $db->prepare($sqlInsB)->execute([
                ':mid' => $memberid,
                ':age' => $age2,
                ':c1' => $c1b,
                ':c2' => $c2b,
                ':c3' => $c3b,
                ':c4' => $c4b,
                ':c5' => $c5b,
                ':c6' => $c6b
            ]);
        }

        $response->getBody()->write(json_encode([
            'success_update_a' => $successA ? "true" : "false",
            'success_update_b' => $successB ? "true" : "false"
        ]));
        return $response->withHeader('Content-Type', 'application/json');

    } catch (\Throwable $e) {
        $response->getBody()->write(json_encode([
            'success_update_a' => "false",
            'success_update_b' => "false",
            'error' => $e->getMessage()
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }
});

// Merit View Route (Calling original .phtml files)
$app->get('/merit/view', function ($request, $response) use ($container) {
    $params = $request->getQueryParams();
    $id = $params['id'] ?? '1';

    $map = [
        '1' => 'sunday',
        '2' => 'monday',
        '3' => 'tuesday',
        '4' => 'wednesday',
        '5' => 'thursday',
        '6' => 'friday',
        '7' => 'saturday',
        '8' => 'rahuu',
        '9' => 'love',
        '10' => 'chataa',
        'love' => 'love',
        'chataa' => 'chataa'
    ];

    $viewFile = $map[$id] ?? 'sunday';
    $fullPath = "tambon/" . $viewFile . ".phtml";
    $path = "http://" . $_SERVER['HTTP_HOST'] . "/";

    return $container->get('view')->render($response, $fullPath, ['PATH' => $path]);
});

// Change Number Route
$app->get('/changenum/view', function ($request, $response) use ($container) {
    $params = $request->getQueryParams();
    $id = $params['id'] ?? 'phone';

    $map = [
        'phone' => 'phone',
        'namenick' => 'namenick',
        'namesur' => 'namesur',
        'tabian' => 'tabian',
        'home' => 'home'
    ];

    $viewFile = $map[$id] ?? 'phone';
    $fullPath = "changenum/" . $viewFile . ".phtml";
    $path = "http://" . $_SERVER['HTTP_HOST'] . "/";

    return $container->get('view')->render($response, $fullPath, ['PATH' => $path]);
});

// Web Routes
$app->get('/web/login', function ($request, $response) use ($container) {
    return $container->get('view')->render($response, 'web_login.php', []);
});

$app->post('/web/login', function ($request, $response) use ($container) {
    $data = $request->getParsedBody();
    $username = $data['username'] ?? '';
    $password = $data['password'] ?? '';

    $db = $container->get('db');
    $sql = "SELECT * FROM membertb WHERE username = :username AND password = :password";
    $stmt = $db->prepare($sql);
    $stmt->execute([':username' => $username, ':password' => $password]);
    $user = $stmt->fetch(\PDO::FETCH_OBJ);

    if ($user) {
        $_SESSION['user'] = $user;
        return $response->withHeader('Location', '/web/dashboard')->withStatus(302);
    } else {
        $view = $container->get('view');
        return $view->render($response, 'web_login.php', ['error' => 'ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง']);
    }
});

$app->get('/web/register', function ($request, $response) use ($container) {
    return $container->get('view')->render($response, 'web_register.php', []);
});

$app->get('/web/api/check-username', function ($request, $response) use ($container) {
    try {
        $username = $request->getQueryParams()['username'] ?? '';
        $db = $container->get('db');
        $stmt = $db->prepare("SELECT count(*) as c FROM membertb WHERE username = ?");
        $stmt->execute([$username]);
        $row = $stmt->fetch();
        $exists = ($row['c'] > 0);

        $response->getBody()->write(json_encode(['exists' => $exists]));
        return $response->withHeader('Content-Type', 'application/json');
    } catch (\Exception $e) {
        $response->getBody()->write(json_encode(['exists' => false, 'error' => $e->getMessage()]));
        return $response->withHeader('Content-Type', 'application/json');
    }
});

$app->get('/web/dashboard', function ($request, $response) use ($container) {
    if (session_status() == PHP_SESSION_NONE) {
        session_start();
    }
    $user = $_SESSION['user'] ?? null;
    return $container->get('view')->render($response, 'web_dashboard.php', ['user' => $user]);
});

$app->get('/web/logout', function ($request, $response) use ($container) {
    if (session_status() == PHP_SESSION_NONE) {
        session_start();
    }
    session_destroy();
    return $response->withHeader('Location', '/')->withStatus(302);
});

// --- Notification History API (Old) ---
$app->post('/api/notifications/save', \App\Managers\NotificationHistoryManager::class . ':saveNotification');
$app->get('/api/notifications/list/{userId}', \App\Managers\NotificationHistoryManager::class . ':getHistory');
$app->post('/api/notifications/delete', \App\Managers\NotificationHistoryManager::class . ':deleteNotification');

// --- Notification API (New - Database-backed) ---
$app->get('/api/v2/notifications', \App\Managers\NotificationAPIController::class . ':getNotifications');
$app->get('/api/v2/notifications/by-type', \App\Managers\NotificationAPIController::class . ':getNotificationsByType');
$app->post('/api/v2/notifications/mark-read', \App\Managers\NotificationAPIController::class . ':markAsRead');
$app->get('/api/v2/notifications/unread-count', \App\Managers\NotificationAPIController::class . ':getUnreadCount');
$app->get('/api/v2/notifications/statistics', \App\Managers\NotificationAPIController::class . ':getStatistics');
$app->post('/api/v2/notifications/save', \App\Managers\NotificationAPIController::class . ':saveNotification');

