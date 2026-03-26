<?php
require_once __DIR__ . '/configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "Connected to DB\n";

    // Disable strict mode to allow 0000-00-00 dates
    $pdo->exec("SET sql_mode = ''");

    // Drop tables if exist to be clean (ORDER MATTERS due to FKs if any)
    $tables = [
        'articles',
        'auspicious_days',
        'bagcolortb',
        'colortb',
        'dayspecialtb',
        'frontname',
        'luckynumber',
        'luckynumber_v2',
        'membertb',
        'memberuse',
        'miracledo',
        'miracledo_desc',
        'news',
        'news_categories',
        'nickname',
        'numbers',
        'phonenumber_sell',
        'realname',
        'secretcode',
        'tabian_number',
        'tabian_sell',
        'topictb',
        'vipcode',
        'wanpra'
    ];
    foreach ($tables as $table) {
        $pdo->exec("DROP TABLE IF EXISTS `$table`");
    }

    // Read SQL file
    $sql = file_get_contents(__DIR__ . '/restore_news.sql');

    // Split into statements if needed, but PDO might handle it if not too complex
    // Or we can just exec() it.

    // Drop table if exists to be clean
    $pdo->exec("DROP TABLE IF EXISTS news");

    // Execute the SQL from file
    $pdo->exec($sql);
    echo "News table restored successfully.\n";

    // ADD PRIMARY KEY manually because the dump file might be missing it
    try {
        $pdo->exec("ALTER TABLE news ADD PRIMARY KEY (newsid)");
        $pdo->exec("ALTER TABLE news MODIFY newsid int(11) NOT NULL AUTO_INCREMENT");
        echo "Added Primary Key to news table.\n";
    } catch (Exception $e) {
        echo "Warning: Could not add PK (maybe already exists or duplicates): " . $e->getMessage() . "\n";
    }

    echo "News table restored successfully.\n";

    // --- AUTO-FIX: Populate hashtags for demo/testing ---
    $stmt = $pdo->query("SELECT newsid FROM news");
    $ids = $stmt->fetchAll(PDO::FETCH_COLUMN);

    foreach ($ids as $index => $id) {
        // Distribute evenly rather than purely random to ensure coverage
        $tag = ($index % 6) + 1; // 1 to 6
        $col = "hashtag" . $tag;
        $pdo->exec("UPDATE news SET $col = 1 WHERE newsid = $id");
    }
    echo "Populated hashtags 1-6 for testing.\n";
    // ----------------------------------------------------

    // Verify count
    $stmt = $pdo->query("SELECT COUNT(*) as c FROM news");
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "Rows in news: " . $row['c'] . "\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
