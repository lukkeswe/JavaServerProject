<?php
require_once(__DIR__ . "/../dbmanager.php");
require_once(__DIR__. "/../config.php");

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
        header("Location:/logout/");
        exit();
    } else {
        $msg = "It worked! " . $response;
    }
    // Initialize the DB manager
    if (isset($_COOKIE["email"]) && isset($_COOKIE["password"])){
        $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
        if(!$db->login()){
            header("Location:/logout/");
            exit();
        }
    } else {
        header("Location:/logout/");
        exit();
    }
    

} else if(isset($_POST["email"]) && isset($_POST["password"])) {
    $db = new DBmanager($_POST["email"], $_POST["password"]);
    if(!$db->login()){
        header("Location:/login/");
        exit();
    } else {
        // Initialize cURL
        $ch = curl_init();
        // Target server
        $url = API_SERVER . "/create-session";
        curl_setopt($ch, CURLOPT_URL, $url);
        // Set timeout
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
        // Enable POST  method
        curl_setopt($ch, CURLOPT_POST, true);
        // Request data
        $data = [
            "email" => $_POST["email"]
        ];
        // Convert to JSON
        $jsonData = json_encode($data);
        // -- Set the request options --
        // Return response as a string
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        // Prepare POST
        curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonData);
        // Set the header with apropriate content type and leangth
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json',
            'Content-Length: ' . strlen($jsonData)
        ]);
        // Execute POST request
        $response = curl_exec($ch);
        // Error check
        if ($response === false) {
            header("Location:/login/");
            exit();
        } else {
            // Set the cookies
            setcookie("javasession", $response, 0, "/");
            setcookie("email", $_POST["email"], 0, "/", "", true, true);
            setcookie("password", $_POST["password"], 0, "/", "", true, true);
        }
        curl_close($ch);
    }
} else {
    header("Location:/login/");
    exit();
}
?>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="/css/reset.css">
    <link rel="stylesheet" href="/css/style.css">
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
    <title>File manager</title>
    <script type="module" src="/js/upmanager.js"></script>
    <script type="module" src="/js/docmanager.js"></script>
</head>
<body>
    <div id="grayScreen">
        <div id="miniExplorer" style="display:none;">
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
        <div id="uploadContainer" class="grayWindow">
            <div id="dropzone">
            Drag files or folders here
            </div>
            <div id="droppedFiles"></div>
            <input type="file" id="uploadInputFile" style="display:none;">
            <input type="file" id="bufferInput" style="display: none;">
            <progress id="uploadProgress" value="0" max="100" style="width: 100%; display: none;"></progress>
            <div class="spaceBetween">
                <button id="uploadBtn" class="btn">✅</button>
                <button class="btn" id="cancelUpload">❌</button>
            </div>
        </div>
        <div id="textInputContainer" style="display: none;">
            <input id="textInput" type="text">
        </div>
    </div>
    <div id="loadImage" style="width: 50%;margin:0 auto;display:none;">
      <img src="../img/muppet-load.gif" alt="muppet load image">
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
                <div id="newFileOptions" class="newOptions" style="display: none;">
                    <input type="text" id="newFilename" class="textInput" value="filename">
                    <select name="extention" id="extentionSelect" class="btn">
                        <option value="html" selected>HTML</option>
                        <option value="css">CSS</option>
                        <option value="js">JavaScript</option>
                        <option value="php">PHP</option>
                    </select><br>
                    <button id="create" class="btn">✅</button>
                    <button id="cancelFile" class="btn">❌</button>
                </div>
                <div id="newFolderOptions" class="newOptions" style="display: none;">
                    <input id="folderName" class="textInput" type="text" style="width: 200px;">
                    <button id="createNewFolder" class="btn">✅</button>
                </div>
                <div id="optionsContainer"></div>
                <div id="displayContainer"></div>
            </div>
            <div id="explorer">
                <div id="pathContainer"><span><?php echo $db->domain; ?>/</span><p id="path"></p></div>
                <div id="filesContainer"></div>
                <div id="editor"></div>
                <div id="exLoad"> <img src="../img/load.gif" alt="load image"></div>
            </div>
        </main>
    </div>
    
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <!-- Ace Editor JS -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.32.9/ace.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.32.6/ext-language_tools.min.js"></script>
    <script type="module">
        import DM from '../js/docmanager.js';
        import * as UpManager from "../js/upmanager.js";

        sessionStorage.setItem("domain", "<?php echo $db->domain;?>");
        const dm = new DM();

        const uploadInputFile = document.getElementById("uploadInputFile");
        const bufferInput = document.getElementById("bufferInput");

        const grayScreen = document.getElementById('grayScreen');
        const miniExplorer = document.getElementById('miniExplorer');
        const loadImage = document.getElementById("loadImage");

        const uploadContainer = document.getElementById("uploadContainer");
        const droppedFiles = document.getElementById("droppedFiles");
        const droppZone = document.getElementById("dropzone");

        const createFile = document.getElementById("createFileBtn");
        
        dm.flexContainer();
        dm.fileBurgerMenu();
        dm.getFiles();
        document.getElementById("uploadSomeBtn").addEventListener("click", () => {
            grayScreen.style.display = "block";
            uploadContainer.style.display = "block";
        });
        document.getElementById("cancelUpload").addEventListener("click", () => {
            droppedFiles.innerHTML = "";
            uploadInputFile.value = null;
            uploadContainer.style.display = "none";
            grayScreen.style.display = "none";
        });
        document.getElementById("uploadBtn").addEventListener("click", async () => {
            const files = document.getElementById("filesContainer");
            files.innerHTML = "";

            const currentPath = document.getElementById("path");
            let path;
            if (currentPath) path = currentPath.textContent;
            else path = "";

            const folder = await UpManager.uploadFile(uploadInputFile, path);
            if (currentPath) {
                dm.getFiles(currentPath.textContent);
                console.log("Sending: ", currentPath.textContent);
            }
            else dm.getFiles();
            droppedFiles.innerHTML = "";
            uploadInputFile.value = null;
            uploadContainer.style.display = "none";
            grayScreen.style.display = "none";
        });
        droppZone.addEventListener("click", () =>{
            document.getElementById("bufferInput").click();
        });
        bufferInput.addEventListener("change", () => {
            const combinedFiles = Array.from(
                uploadInputFile.files
            ).concat(
                Array.from(bufferInput.files)
            );
            const final = new DataTransfer();
            for (const file of combinedFiles) final.items.add(file);
            uploadInputFile.files = final.files;
            console.log("Current files: ", uploadInputFile.files);
            droppedFiles.innerHTML = "";
            for (let file of combinedFiles) {
                const p = document.createElement("p");
                p.innerHTML = file.name;
                droppedFiles.append(p);
            }
        });
        // Put this in place of your current drop handler
        droppZone.addEventListener('drop', async (e) => {
            e.preventDefault();

            // 1) Prefer FileList (most direct, same as input.files)
            const dt = e.dataTransfer;
            let files = [];

            if (dt && dt.items && dt.items.length) {
                // Snapshot items immediately (do not use the live list later)
                const items = Array.from(dt.items);

                for (const item of items) {
                    // 2) Universal: try getAsFile() first (works in most cases)
                    if (typeof item.webkitGetAsEntry === 'function') {
                        const entry = item.webkitGetAsEntry();
                        if (entry){
                            if (entry.isDirectory) {
                                // It's a folder - read it recursively
                                const entryFiles = await readEntriesRecursively(entry);
                                files.push(...entryFiles);
                            } else if (entry.isFile) {
                                // It's a file - wrap in Promise
                                const file = await new Promise((resolve, reject) => {
                                    entry.file(resolve, reject);
                                });
                                files.push(file);
                            }
                            continue;
                        }
                    }
                    if (typeof item.getAsFile === 'function') {
                        const f = item.getAsFile();
                        if (f) {
                            files.push(f);
                        }
                    }
                }
            } else if (dt && dt.files && dt.files.length) {
                files = Array.from(dt.files).filter(f => f.size > 0 || f.type);
            }

            console.log('Dropped files:', files); // should be File[] like input.files
            

            // 4) Turn files[] into a FileList and assign to your hidden file input
            if (files.length) {
                let existingFiles = Array.from(uploadInputFile.files);
                let combinedFiles = existingFiles.concat(files);

                const final = new DataTransfer();
                for (const f of combinedFiles) final.items.add(f);

                uploadInputFile.files = final.files;

                console.log("Current files: ", uploadInputFile.files);

                droppedFiles.innerHTML = "";
            for (let file of uploadInputFile.files) {
                const p = document.createElement("p");
                p.innerHTML = file.name;
                droppedFiles.append(p);
            }

                // Call your existing upload function that expects an input element:
                //uploadFiles(document.getElementById('uploadInputFolder').files);
            } else {
                console.log('No files found in drop (source might be URL/text).');
            }
        });

        // Helper: recursively read DirectoryEntry/FileEntry (Chrome)
        function readEntriesRecursively(entry) {
            return new Promise((resolve, reject) => {
                if (entry.isFile) {
                entry.file(file => {
                    // set relativePath if you want folder structure (entry.fullPath)
                    file.relativePath = (entry.fullPath || entry.name).replace(/^\//,'');
                    resolve([file]);
                }, reject);
                } else if (entry.isDirectory) {
                const reader = entry.createReader();
                const files = [];
                const readBatch = () => {
                    reader.readEntries(async (entries) => {
                    if (!entries || entries.length === 0) {
                        resolve(files);
                        return;
                    }
                    // process entries sequentially to preserve memory
                    for (const ent of entries) {
                        // await recursive call to preserve order
                        // but we wrap in Promise since readEntries uses callback
                        // use synchronous push via await
                        // this code uses recursion; adapt if you want parallelism
                        // NB: entry may be FileEntry or DirectoryEntry
                        const subFiles = await readEntriesRecursively(ent);
                        files.push(...subFiles);
                    }
                    readBatch();
                    }, reject);
                };
                readBatch();
                } else {
                resolve([]);
                }
            });
        }
        droppZone.addEventListener('dragover', e => e.preventDefault());
        // !!!
        document.getElementById("cancel").addEventListener("click", ()=> {
            document.getElementById("uploadBtnMini").remove();
            grayScreen.style.display = "none";
        });
        createFile.addEventListener("click", ()=>{
            if (document.getElementById("newFileOptions").style.display == "block") {
                document.getElementById("newFileOptions").style.display = "none";
            } else {
                document.getElementById("newFileOptions").style.display = "block";
                createFileFunc();
            }
        });
        document.getElementById("cancelFile").addEventListener("click", ()=> {
            document.getElementById("newFileOptions").style.display = "none";
        });
        document.getElementById("createFolderBtn").addEventListener("click", ()=> {
            //const options = document.getElementById("newFolderOptions");
            //if (options.style.display == "none") options.style.display = "block";
            //else options.style.display = "none";
            grayScreen.style.display = "block";
            dm.showCreateFolderContainer();
        });
        document.getElementById("createNewFolder").addEventListener("click", async ()=> {
            const folderName = document.getElementById("folderName");
            const path = document.getElementById("path");
            if (path) {
                await UpManager.createFolder(path.textContent + folderName.value);
                await dm.getFiles(path.textContent);
            }
            else {
                await UpManager.createFolder(folderName.value);
                await dm.getFiles("");
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
                await UpManager.createFolder(path.textContent + folderName.value);
                await dm.getFilesMini(path.textContent);
            }
            else {
                await UpManager.createFolder(folderName.value);
                await dm.getFilesMini("");
            }
            document.getElementById("newFolderOptionsMini").style.display = "none";
        });
        
        function createFileFunc() {
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
