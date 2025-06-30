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
    <link rel="stylesheet" href="css/reset.css">
    <link rel="stylesheet" href="css/style.css">
    <title>File manager</title>
    <script src="static/upmanager.js"></script>
    <script src="static/docmaanager.js"></script>
</head>
<body>
    <header><h1>ファイル管理</h1></header>
    <div id="container">
        <main id="main-content">
            <div id="upload-container">
                <input type="file" id="fileInput" style="border: solid black 1px;">
                <br>
                <button onclick="uploadFile()">Upload</button>
            </div>
        </main>
    </div>
    
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <script type="text/javasccript">
        const dm = new DocumentManager();
        dm.flexContainer();
    </script>
</body>
</html>