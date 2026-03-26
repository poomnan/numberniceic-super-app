<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>API Documentation (News)</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>

<body>
    <?php include 'web_menu.php'; ?>
    <div class="container mt-5 pt-5">
        <h2>News API Documentation</h2>
        <div class="card mt-4">
            <div class="card-body">
                <h5>GET /news/topic24</h5>
                <p>Get top 24 news items.</p>
                <hr>
                <h5>GET /news/api/article/{id}</h5>
                <p>Get article details.</p>
            </div>
        </div>
        <a href="/web/dashboard" class="btn btn-secondary mt-3">Back to Dashboard</a>
    </div>
</body>

</html>