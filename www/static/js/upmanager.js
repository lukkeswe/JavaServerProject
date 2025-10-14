async function uploadFile(user, path = "") {
    const files = document.getElementById('fileInput').files;
    const formData = new FormData();
    let htmlFiles = sessionStorage.getItem("htmlList");
    let phpFiles = sessionStorage.getItem("phpList");
    let cssFiles = sessionStorage.getItem("cssList");
    let imgFiles = sessionStorage.getItem("imgList");
    let fileName;
    let isValidFile = false;

    for (let file of files) {
        if (
            htmlFiles.includes(file.name) ||
            phpFiles.includes(file.name) ||
            cssFiles.includes(file.name) ||
            imgFiles.includes(file.name)
        ){
            const replace = confirm(`「${file.name}」 のファイルが存在しています。上書きますか？`);
            if (!replace) {continue;}
        } else if (!isValidFileName(file.name)){
            alert(`「${file.name}」をファイル名として出来ません。`);
            continue;
        }
        formData.append("files", file);
        fileName = file.name;
        isValidFile = true;
    }
    if (isValidFile) {
        formData.append("user", user);
        formData.append("path", path);
        console.log("Uploading file...");
        
        await fetch('/upload', {
            method  : 'POST',
            body    : formData
        }).then(res => res.text())
        .then(msg => alert(msg));

        return fileName;
    }
}

async function uploadFolder(user, path = ""){
    const input = document.getElementById("folderInput");
    const files = input.files;

    if (files.lenght === 0){
        alert("フォルダが選択していません！")
        return;
    }

    const formData = new FormData();

    formData.append("user", user);
    formData.append("path", path);

    let htmlFiles = sessionStorage.getItem("htmlList");
    let fileName;
    let isValidFile = false;
    let parentFolder;

    for (const file of files){
        if (!isValidPath(file.webkitRelativePath)) {
            alert(`「${file.webkitRelativePath}」は無効なフォルダ/ファイル名です。`);
            isValidFile = false;
            break;
        }

        const parts = file.webkitRelativePath.split("/");
        parentFolder = parts[0] + "/";
        
        formData.append("files[]", file, file.webkitRelativePath);
        fileName = file.name;
        isValidFile = true;
    }
    if (htmlFiles.includes(parentFolder)) {
        const replace = confirm(`「${parentFolder}」 のフォルダが存在しています。上書きますか？`);
        if (!replace) isValidFile = false;
    }
    if (isValidFile){
        const response = await fetch("/uploadFolder", {
            method: "POST",
            body: formData
        });
        alert(await response.text());
    }
    
}

async function saveContentToFile(user, path ="", type, file, content) {
    let requestObject = {
        "user"      : user,
        "path"      : path,
        "type"      : type,
        "filename"  : file,
        "content"   : content
    };
    console.log("Saving file...");
    
    const response = await fetch("/saveFile", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(requestObject)
    });

    let msg = await response.text()

    console.log(msg);

    if (msg == "Success") {
        alert("ファイルを保存しました");
    } else {
        alert("保存エラー")
    }
    
}

async function createFolder(user, path) {
    let requestObject = {
        "user"  : user,
        "path"  : path
    };
    console.log("Creating folder..");
    
    if (!isValidPath(path)){
        console.log(`Invalid path: ${path}`);
        return;
    }

    const response = await fetch("/createFolder", {
        method  : "POST",
        headers : {"Content-Type" : "application/json"},
        body    : JSON.stringify(requestObject)
    });

    let msg = await response.text();
    
    console.log(msg);

    if (msg == "Success") alert("Folder created");
    else alert("Failed to create folder...");
}

function isValidFileName(fileName){
    if (!fileName || fileName.trim() === "") return false;
    const validPattern = /^[a-zA-Z0-9._-]+$/;
    return validPattern.test(fileName);
}

function isValidPath(path) {
    if (!path || path.trim() === "") return false;

    const validPattern = /^[a-zA-Z0-9._-]+$/;

    // Split into parts by "/"
    const parts = path.split("/");

    for (const part of parts) {
        if (!validPattern.test(part)){
            return false;
        }
    }
    return true;
}