<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage News - Ananya Admin</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f0f2f5; margin: 0; padding: 0; }
        .main-wrapper { max-width: 1200px; margin: 2rem auto; padding: 0 1rem; }
        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; background: white; padding: 1.5rem; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
        .header h1 { margin: 0; font-size: 1.5rem; color: #333; }
        .btn { padding: 10px 20px; border-radius: 6px; text-decoration: none; color: white; display: inline-block; font-weight: 500; transition: all 0.2s; border: none; cursor: pointer; }
        .btn-primary { background-color: #0d6efd; }
        .btn-success { background-color: #198754; }
        .btn-danger { background-color: #dc3545; }
        .btn-warning { background-color: #ffc107; color: #000; }
        .btn:hover { opacity: 0.9; transform: translateY(-1px); }
        
        .card { background: white; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); overflow: hidden; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 15px; text-align: left; border-bottom: 1px solid #eee; }
        th { background-color: #f8f9fa; font-weight: 600; color: #555; }
        tr:hover { background-color: #f8f9fa; }
        .badge { padding: 5px 10px; border-radius: 20px; font-size: 0.8rem; background: #eee; color: #333; }
        .img-thumb { width: 60px; height: 40px; object-fit: cover; border-radius: 4px; background: #eee; }
        
        /* Mobile responsive */
        @media (max-width: 768px) {
            .hide-mobile { display: none; }
            th, td { padding: 10px; font-size: 0.9rem; }
            .img-thumb { width: 40px; height: 30px; }
        }
    </style>
</head>
<body>
    <?php include 'web_menu.php'; ?>

    <div class="main-wrapper">
        <div class="header">
            <h1>📰 จัดการข่าวและบทความ (News Table)</h1>
            <a href="/web/admin/news/create" class="btn btn-success">+ เพิ่มข่าวใหม่</a>
        </div>

        <div class="card">
            <table>
                <thead>
                    <tr>
                        <th width="50">ID</th>
                        <th width="80">Use</th>
                        <th width="80">Image</th>
                        <th>Headline</th>
                        <th class="hide-mobile">Category</th>
                        <th class="hide-mobile">Last Updated</th>
                        <th width="150" style="text-align:right;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (isset($newsList) && count($newsList) > 0): ?>
                        <?php foreach ($newsList as $news): ?>
                            <tr>
                                <td><?php echo $news->newsid; ?></td>
                                <td>
                                    <?php 
                                        // Show which legacy group it falls into just for info
                                        if($news->fix == 1) echo '<span style="color:gold;">★ Hot</span>';
                                        elseif($news->hashtag1) echo 'Feedback';
                                        elseif($news->hashtag2) echo 'Phone';
                                        elseif($news->hashtag3) echo 'Name';
                                        elseif($news->hashtag4) echo 'Tabian';
                                        elseif($news->hashtag5) echo 'Home';
                                        elseif($news->hashtag6) echo 'Concept';
                                        else echo '-';
                                    ?>
                                </td>
                                <td>
                                    <?php if(!empty($news->news_pic_header)): ?>
                                        <img src="<?php echo $news->news_pic_header; ?>" class="img-thumb">
                                    <?php else: ?>
                                        <div class="img-thumb"></div>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <div style="font-weight:600;"><?php echo htmlspecialchars($news->news_headline); ?></div>
                                    <div style="font-size:0.85rem; color:#888;"><?php echo htmlspecialchars(mb_strimwidth($news->news_desc, 0, 50, '...')); ?></div>
                                </td>
                                <td class="hide-mobile">
                                    <span class="badge" style="background-color:#e3f2fd; color:#0d47a1;">
                                        <?php echo htmlspecialchars($news->category_name ?? 'N/A'); ?>
                                    </span>
                                </td>
                                <td class="hide-mobile" style="font-size:0.85rem; color:#666;">
                                    <?php echo $news->news_date; ?>
                                </td>
                                <td style="text-align:right;">
                                    <a href="/web/admin/news/edit/<?php echo $news->newsid; ?>" class="btn btn-primary" style="padding: 5px 10px; font-size:0.8rem;">Edit</a>
                                    <a href="/web/admin/news/delete/<?php echo $news->newsid; ?>" onclick="return confirm('Are you sure you want to delete this news?');" class="btn btn-danger" style="padding: 5px 10px; font-size:0.8rem;">Del</a>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php else: ?>
                        <tr><td colspan="6" style="text-align:center; padding: 2rem;">ไม่พบข้อมูลข่าว</td></tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
