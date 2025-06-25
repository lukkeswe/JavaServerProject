<?php
require_once(__DIR__ . "/dbmanager.php");
if(!isset($_COOKIE["email"]) || !isset($_COOKIE["password"])){
    header("Location:server.php");
    exit();
} else {
    $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
    if (!$db->login()){
        header("Location:server.php");
        exit();
    }
}
?>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>File manager</title>
    <script src="static/upmanager.js"></script>
</head>
<body>
    <input type="file" id="fileInput" style="border: solid black 1px;">
    <button onclick="uploadFile()">Upload</button>
</body>
</html>