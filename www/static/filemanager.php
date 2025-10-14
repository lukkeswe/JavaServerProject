<?php
require_once(__DIR__ . "/dbmanager.php");
require_once(__DIR__. "/config.php");

if (isset($_COOKIE["javasession"])){
    // Initialize cURL
    $ch = curl_init();
    // Target server
    $url = API_SERVER . "/check-session";
    curl_setopt($ch, CURLOPT_URL, $url);
    // Set timeout
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    // Enable POST
    curl_setopt($ch, CURLOPT_POST, true);
    // Request data
    $data = ["session" => $_COOKIE["javasession"]];
    // Convert to JSON
    $jsonData = json_encode($data);
    // -- Set request options --
    // Return response as a string
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    // Prepare POST
    curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonData);
    // Set the header with apropriate content type and leangth
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Content-Length: ' . strlen($jsonData) 
    ]);
    // Execute POST
    $response = curl_exec($ch);
    // Get the HTTP code
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    // Error check
    if ($response === false || $httpCode != 200) {
        header("Location:logout.php");
        exit();
    } else {
        $msg = "It worked! " . $response;
    }
    // Initialize the DB manager
    if (isset($_COOKIE["email"]) && isset($_COOKIE["password"])){
        $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
        if(!$db->login()){
            header("Location:logout.php");
            exit();
        }
    } else {
        header("Location:logout.php");
        exit();
    }
    

} else {
    header("Location:server.php");
    exit();
}
?>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="css/reset.css">
    <link rel="stylesheet" href="css/style.css">
    <link rel="icon" type="image/x-icon" href="favicon.ico">
    <title>File manager</title>
    <script src="js/upmanager.js"></script>
    <script src="js/docmanager.js"></script>
</head>
<body>
    <div id="grayScreen">
        <div id="miniExplorer">
            <button class="createBtn" id="createFolderBtnMini">📁</button>
            <input id="filename" type="text" value="index" style="width: 150px;"><span id="extention"></span><br>
            <input id="newFolder" type="hidden" style="width: 150px;">
            <div id="pathContainerMini"><p id="pathMini"></p></div>
            <div id="optionsContainerMini"></div>
            <div id="newFolderOptionsMini">
                <input id="folderNameMini" type="text" style="width: 200px;">
                <button id="createNewFolderMini" class="btn">✅</button>
            </div>
            <div id="displayContainerMini" style="display: none;"></div>
            <div id="filesContainerMini"></div>
            <div class="cancelContainer">
                <div id="uploadBtnContainerMini"></div>
                <button class="btn" id="cancel">❌</button>
            </div>
        </div>
    </div>
    <header><h1>ファイル管理</h1></header>
    <div id="container">
        <main id="main-content">
            <div id="upload-container">
                <div id="createButtons">
                    <button class="createBtn" id="createFileBtn">📄</button>
                    <button class="createBtn" id="createFolderBtn">📁</button>
                    <button class="createBtn" id="uploadSomeBtn">📤</button>
                </div>
                <button id="uploadBackBtn" class="btn" style="display: none;">❌</button>
                <div id="fileUploadContainer" style="display: none;">
                    <input type="file" id="fileInput" style="border: solid black 1px;">
                    <button id="uploadBtn" class="btn">✅</button>
                </div>
                <div id="folderUploadContainer" style="display: none;">
                    <input type="file" id="folderInput" webkitdirectory multiple style="border: solid black 1px;">
                    <button id="uploadFolderBtn" class="btn">✅</button>
                </div>
                <div id="uploadBtnsContainer" style="display: none;">
                    <button id="fileBtn" class="btn">📎</button>
                    <button id="folderBtn" class="btn">📂</button>
                    <button id="cancelUpload" class="btn">❌</button>
                </div>
                <div id="newFileOptions" style="display: none;">
                    <input type="text" id="newFilename" value="filename">
                    <select name="extention" id="extentionSelect" class="btn">
                        <option value="html" selected>HTML</option>
                        <option value="css">CSS</option>
                        <option value="js">JavaScript</option>
                        <option value="php">PHP</option>
                    </select><br>
                    <button id="create" class="btn">✅</button>
                    <button id="cancelFile" class="btn">❌</button>
                </div>
                <div id="newFolderOptions" style="display: none;">
                    <input id="folderName" type="text" style="width: 200px;">
                    <button id="createNewFolder" class="btn">✅</button>
                </div>
                <div id="optionsContainer"></div>
                <div id="displayContainer"></div>
            </div>
            <div id="explorer">
                <div id="pathContainer"><span><?php echo $db->domain; ?>/</span><p id="path"></p></div>
                <div id="filesContainer"></div>
                <div id="editor"></div>
            </div>
        </main>
    </div>
    
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <!-- Ace Editor JS -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.32.9/ace.js"></script>
    <script type="text/javascript">
        sessionStorage.setItem("user", "<?php echo $db->username; ?>");
        sessionStorage.setItem("domain", "<?php echo $db->domain;?>");
        const dm = new DocumentManager("<?php echo $db->username; ?>");
        console.log("dm.user = " + dm.user);
        
        dm.flexContainer();
        dm.fileBurgerMenu();
        dm.getFiles("<?php echo $db->username; ?>");
        document.getElementById("uploadSomeBtn").addEventListener("click", () => {
            dm.toggleShowUploadBtn();
        });
        document.getElementById("cancelUpload").addEventListener("click", () => {
            dm.toggleShowUploadBtn();
        });
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
            if (currentPath) path = currentPath.textContent;
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
            if (currentPath) path = currentPath.textContent;
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
        document.getElementById("cancel").addEventListener("click", ()=> {
            document.getElementById("uploadBtnMini").remove();
            document.getElementById("grayScreen").style.display = "none";
        });
        document.getElementById("createFileBtn").addEventListener("click", ()=>{
            createFileFunc();
        });
        document.getElementById("cancelFile").addEventListener("click", ()=> {
            document.getElementById("newFileOptions").style.display = "none";
        });
        document.getElementById("createFolderBtn").addEventListener("click", ()=> {
            const options = document.getElementById("newFolderOptions");
            if (options.style.display == "none") options.style.display = "block";
            else options.style.display = "none";
        });
        document.getElementById("createNewFolder").addEventListener("click", async ()=> {
            const folderName = document.getElementById("folderName");
            const path = document.getElementById("path");
            if (path) {
                await createFolder("<?php echo $db->username; ?>", path.textContent + folderName.value);
                await dm.getFiles("<?php echo $db->username; ?>", path.textContent);
            }
            else {
                await createFolder("<?php echo $db->username; ?>", folderName.value);
                await dm.getFiles("<?php echo $db->username; ?>", "");
            }
            document.getElementById("newFolderOptions").style.display = "none";
        });
        document.getElementById("createFolderBtnMini").addEventListener("click", ()=> {
            const options = document.getElementById("newFolderOptionsMini");
            if (options.style.display == "none") options.style.display = "block";
            else options.style.display = "none";
        });
        document.getElementById("createNewFolderMini").addEventListener("click", async ()=> {
            const folderName = document.getElementById("folderNameMini");
            const path = document.getElementById("pathMini");
            console.log("new path: " + path.textContent + folderName.value);
            
            if (path) {
                await createFolder("<?php echo $db->username; ?>", path.textContent + folderName.value);
                await dm.getFilesMini("<?php echo $db->username; ?>", path.textContent);
            }
            else {
                await createFolder("<?php echo $db->username; ?>", folderName.value);
                await dm.getFilesMini("<?php echo $db->username; ?>", "");
            }
            document.getElementById("newFolderOptionsMini").style.display = "none";
        });
        
        function createFileFunc() {
            document.getElementById("newFileOptions").style.display = "block";
            document.getElementById("create").addEventListener("click", ()=> {
                const filename = document.createElement("h2");
                const name = document.getElementById("newFilename").value;
                // Empty the display container
                document.getElementById("displayContainer").innerHTML = "";
                // Append the file name to the display container
                document.getElementById("displayContainer").append(filename);
                const backBtn = dm.editorBackButton();
                const extention = document.getElementById("extentionSelect").value;
                filename.innerHTML = name + "." + extention;
                document.getElementById("newFileOptions").style.display = "none";
                dm.createFile(name + "." + extention, extention);
                document.getElementById("optionsContainer").append(backBtn);
            });
        }
    </script>
</body>
</html>
