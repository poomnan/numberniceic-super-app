<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>บทความทั้งหมด - ananya.in.th</title>
    <link rel="icon" type="image/png" href="/favicon.png">
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


        .container {
            max-width: 1000px;
            margin: 3rem auto;
            padding: 0 1rem;
        }

        h1 {
            font-family: 'Kanit', sans-serif;
            font-size: 2.5rem;
            text-align: center;
            margin-bottom: 3rem;
            color: #1e293b;
        }

        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 2rem;
        }

        .card {
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
            text-decoration: none;
            color: inherit;
            transition: transform 0.2s;
        }

        .card:hover {
            transform: translateY(-5px);
        }

        .card img {
            width: 100%;
            height: 180px;
            object-fit: cover;
        }

        .card-body {
            padding: 1.5rem;
        }

        .cat {
            color: #dc3545;
            font-size: 0.85rem;
            font-weight: 600;
            text-transform: uppercase;
            margin-bottom: 0.5rem;
            display: block;
        }

        .card h3 {
            margin: 0 0 0.75rem 0;
            font-family: 'Kanit', sans-serif;
            font-size: 1.25rem;
            line-height: 1.4;
            color: #1e293b;
        }

        .card p {
            margin: 0;
            color: #64748b;
            font-size: 0.95rem;
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }
    </style>
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container">
        <h1>บทความมงคล</h1>
        <div class="grid">
            <?php foreach ($articles as $a): ?>
                <a href="/articles/<?php echo $a->slug; ?>" class="card">
                    <?php if ($a->image_url): ?>
                        <img src="<?php echo $a->image_url; ?>" alt="">
                    <?php endif; ?>
                    <div class="card-body">
                        <span class="cat">
                            <?php echo $a->category; ?>
                        </span>
                        <h3>
                            <?php echo $a->title; ?>
                        </h3>
                        <p>
                            <?php echo $a->excerpt; ?>
                        </p>
                    </div>
                </a>
            <?php endforeach; ?>
        </div>
    </div>
</body>

</html>