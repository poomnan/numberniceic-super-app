<!DOCTYPE html>
<html lang="th">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($item->title); ?></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Sarabun:wght@300;400;500;600&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Sarabun', sans-serif;
            background-color: #f8f9fa;
            color: #333;
            padding: 20px;
        }

        .container {
            max-width: 600px;
            margin: 0 auto;
            background: white;
            border-radius: 15px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
            padding: 25px;
            overflow: hidden;
        }

        .merit-image {
            width: 100%;
            border-radius: 10px;
            margin-bottom: 20px;
            object-fit: cover;
            max-height: 300px;
        }

        .merit-title {
            font-size: 1.4rem;
            font-weight: 600;
            color: #2c3e50;
            margin-bottom: 15px;
            border-bottom: 2px solid #f0f0f0;
            padding-bottom: 10px;
        }

        .merit-content {
            font-size: 1rem;
            line-height: 1.6;
            color: #555;
            white-space: pre-line;
        }
    </style>
</head>

<body>
    <div class="container">
        <?php if (!empty($item->image)): ?>
            <img src="<?php echo $item->image; ?>" alt="<?php echo htmlspecialchars($item->title); ?>" class="merit-image">
        <?php endif; ?>

        <h1 class="merit-title"><?php echo htmlspecialchars($item->title); ?></h1>
        <div class="merit-content"><?php echo $item->content; ?></div>
    </div>
</body>

</html>