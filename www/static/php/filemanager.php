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
    <script src="static/js/upmanager.js"></script>
    <script src="static/js/docmanager.js"></script>
</head>
<body>
    <header><h1>ファイル管理</h1></header>
    <div id="container">
        <main id="main-content">
            <div id="upload-container">
                <div id="pathContainer"></div>
                <button id="uploadBackBtn" style="display: none;">back</button>
                <div id="fileUploadContainer" style="display: none;">
                    <input type="file" id="fileInput" style="border: solid black 1px;">
                    <button id="uploadBtn" class="btn">アップロード</button>
                </div>
                <div id="folderUploadContainer" style="display: none;">
                    <input type="file" id="folderInput" webkitdirectory multiple style="border: solid black 1px;">
                    <button id="uploadFolderBtn" class="btn">アップロード</button>
                </div>
                <div id="uploadBtnsContainer" style="display: flex;">
                    <button id="fileBtn" class="btn">file</button>
                    <button id="folderBtn" class="btn">folder</button>
                </div>
                <div id="optionsContainer"></div>
                <div id="displayContainer"></div>
            </div>
            <div id="filesContainer"></div>
            <div id="editor"></div>
        </main>
    </div>
    
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <!-- Ace Editor JS -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.32.9/ace.js"></script>
    <script type="text/javascript">
        sessionStorage.setItem("user", "<?php echo $db->username; ?>")
        const dm = new DocumentManager("<?php echo $db->username; ?>");
        console.log("dm.user = " + dm.user);
        
        dm.flexContainer();
        dm.fileBurgerMenu();
        dm.getFiles("<?php echo $db->username; ?>");
        document.getElementById("fileBtn").addEventListener("click", () => {
            dm.showFileUpload();
        });
        document.getElementById("folderBtn").addEventListener("click", () => {
            dm.showFolderUpload();
        });
        document.getElementById("uploadBackBtn").addEventListener("click", () => {
            dm.uploadBack();
        });
        document.getElementById("uploadBtn").addEventListener("click", async () => {
            const files = document.getElementById("filesContainer");
            files.innerHTML = "";

            const currentPath = document.getElementById("path");
            let path;
            if (currentPath) path = "html/" + currentPath.textContent;
            else path = "";

            const loadImage = document.createElement("img");
            loadImage.src = "img/muppet-load.gif";
            files.append(loadImage);
            const file = await uploadFile("<?php echo $db->username; ?>", path);
            if (currentPath) {
                dm.getFiles("<?php echo $db->username; ?>", currentPath.textContent);
                console.log("Sending: ", currentPath.textContent);
            }
            else dm.getFiles("<?php echo $db->username; ?>");
            dm.emptyDisplayContainer();
            dm.showInfo(file);
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
        document.getElementById("uploadFolderBtn").addEventListener("click", async () => {
            const files = document.getElementById("filesContainer");
            files.innerHTML = "";

            const currentPath = document.getElementById("path");
            let path;
            if (currentPath) path = "html/" + currentPath.textContent;
            else path = "";

            const loadImage = document.createElement("img");
            loadImage.src = "img/muppet-load.gif";
            files.append(loadImage);
            const folder = await uploadFolder("<?php echo $db->username; ?>", path);
            if (currentPath) {
                dm.getFiles("<?php echo $db->username; ?>", currentPath.textContent);
                console.log("Sending: ", currentPath.textContent);
            }
            else dm.getFiles("<?php echo $db->username; ?>");
            dm.emptyDisplayContainer();
            dm.showInfo();
        });

    </script>
</body>
</html>
