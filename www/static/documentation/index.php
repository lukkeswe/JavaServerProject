<?php
// Handle folder creation
//if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_POST['new_folder'])) {
//    $newFolder = basename(trim($_POST['new_folder'])); // sanitize input
//    if (!is_dir($newFolder)) {
//        mkdir($newFolder);
//        $message = "✅ Folder '$newFolder' created successfully.";
//    } else {
//        $message = "⚠️ Folder '$newFolder' already exists.";
//    }
//}

// Get list of directories
$items = array_filter(glob('*'), function ($path) {
    return is_dir($path) || strtolower(pathinfo($path, PATHINFO_EXTENSION)) === 'pdf';
});

?>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Documentation</title>
<style>
    body {
        font-family: Arial, sans-serif;
        background: #f7f7f7;
        color: #333;
        display: flex;
        flex-direction: column;
        align-items: center;
        margin: 0;
        padding: 40px;
    }
    h1 { margin-bottom: 20px; }
    form {
        margin-bottom: 20px;
        display: flex;
        gap: 10px;
    }
    input[type="text"] {
        padding: 8px 10px;
        border: 1px solid #ccc;
        border-radius: 5px;
    }
    button {
        background-color: #4CAF50;
        color: white;
        border: none;
        padding: 8px 14px;
        border-radius: 5px;
        cursor: pointer;
    }
    button:hover {
        background-color: #45a049;
    }
    ul {
        list-style-type: none;
        padding: 0;
        max-width: 400px;
        width: 100%;
    }
    li {
        background: #fff;
        margin: 5px 0;
        padding: 10px 14px;
        border-radius: 5px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    a {
        text-decoration: none;
        color: #0077cc;
    }
    a:hover { text-decoration: underline; }
    .message {
        margin-bottom: 15px;
        color: #333;
        font-weight: bold;
    }
    @media (max-width: 900px){
        ul {
            font-size: 32px;
        }
    }
</style>
</head>
<body>

<h1>📁 Java HTTP Server Project Documentation</h1>

<!--<form method="post">
    <input type="text" name="new_folder" placeholder="New folder name" required>
    <button type="submit">Create Folder</button>
</form>
-->
<?php if (!empty($message)): ?>
<div class="message"><?= htmlspecialchars($message) ?></div>
<?php endif; ?>

<ul>
<?php foreach ($items as $item): ?>
    <li><a href="<?= urlencode($item) ?>/"><?= str_replace("-", " ", htmlspecialchars($item)) ?></a></li>
<?php endforeach; ?>
</ul>

</body>
</html>