<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>
        <?php echo $article->title; ?> - ananya.in.th
    </title>
    <link rel="icon" type="image/png" href="/favicon.png">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link
        href="https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;500;600;700&family=Sarabun:wght@300;400;500;600;700&display=swap"
        rel="stylesheet">
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: 'Sarabun', sans-serif;
            background: #f8fafc;
        }


        .article-container {
            max-width: 800px;
            margin: 2rem auto;
            background: white;
            padding: 2.5rem;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
        }

        .category-badge {
            background-color: #FFD700;
            color: #333;
            padding: 0.25rem 0.75rem;
            border-radius: 20px;
            font-size: 0.9rem;
            font-weight: bold;
            display: inline-block;
            margin-bottom: 1rem;
            font-family: 'Kanit', sans-serif;
        }

        h1 {
            font-family: 'Kanit', sans-serif;
            font-size: 2.2rem;
            line-height: 1.3;
            margin-bottom: 1rem;
            color: #1a202c;
        }

        .meta {
            color: #94a3b8;
            font-size: 0.9rem;
            margin-bottom: 2rem;
        }

        .content {
            line-height: 1.8;
            color: #334155;
            font-size: 1.1rem;
        }

        .content img {
            max-width: 100%;
            height: auto;
            border-radius: 8px;
            margin: 1.5rem auto;
            display: block;
        }

        .back-link {
            display: inline-block;
            margin-top: 3rem;
            color: #667EEA;
            text-decoration: none;
            font-weight: 500;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="article-container">
        <span class="category-badge">
            <?php echo $article->category; ?>
        </span>
        <h1>
            <?php echo $article->title; ?>
        </h1>
        <div class="meta">เผยแพร่เมื่อ:
            <?php echo date('d F Y', strtotime($article->published_at)); ?>
        </div>

        <?php if (!empty($article->image_url)): ?>
            <img src="<?php echo $article->image_url; ?>" alt="<?php echo $article->title; ?>"
                style="width: 100%; border-radius: 12px; margin-bottom: 2rem;">
        <?php endif; ?>

        <div class="content">
            <?php echo $article->content; ?>
        </div>
        <a href="/articles" class="back-link">← กลับไปหน้าบทความ</a>
    </div>
</body>

</html>