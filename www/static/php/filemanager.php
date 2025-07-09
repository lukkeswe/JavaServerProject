<?php
session_start();
require_once(__DIR__ . "/dbmanager.php");
if(!isset($_SESSION["email"]) || !isset($_SESSION["password"])){
    header("Location:server.php");
    exit();
} else {
    $db = new DBmanager($_SESSION["email"], $_SESSION["password"]);
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
    <script src="static/docmanager.js"></script>
</head>
<body>
    <header><h1>ファイル管理</h1></header>
    <div id="container">
        <main id="main-content">
            <div id="upload-container">
                <input type="file" id="fileInput" style="border: solid black 1px;">
                <br>
                <button id="uploadBtn" class="btn">Upload</button>
                <div id="displayContainer"></div>
            </div>
            <div id="filesContainer"></div>
        </main>
    </div>
    
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <script type="text/javascript">
        const dm = new DocumentManager();
        dm.flexContainer();
        dm.fileBurgerMenu();
        dm.getFiles(sessionStorage.getItem("username"));
        document.getElementById("uploadBtn").addEventListener("click", async () => {
            const files = document.getElementById("filesContainer");
            files.innerHTML = "";
            const loadImage = document.createElement("img");
            loadImage.src = "img/muppet-load.gif";
            files.append(loadImage);
            const file = await uploadFile();
            dm.getFiles(sessionStorage.getItem("username"));
            if (
                file.endsWith(".jpg")   ||
                file.endsWith(".jpeg")  ||
                file.endsWith(".png")   || 
                file.endsWith(".JPG")   ||
                file.endsWith(".gif")   ||
                file.endsWith(".webp")
            ){
                dm.showImage(file, "https://" + sessionStorage["domain"] + "/");
            }
        });
    </script>
</body>
</html>