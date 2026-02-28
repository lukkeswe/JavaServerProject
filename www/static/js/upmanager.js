export async function uploadFile(input, path = "") {
    const files = input.files;
    const progress = document.getElementById("uploadProgress");
    const formData = new FormData();
    let folderList = sessionStorage.getItem("folderList");
    let htmlFiles = sessionStorage.getItem("htmlList");
    let phpFiles = sessionStorage.getItem("phpList");
    let cssFiles = sessionStorage.getItem("cssList");
    let imgFiles = sessionStorage.getItem("imgList");
    let videoFiles = sessionStorage.getItem("videoList");
    let fileName;
    let isValidFile = false;

    formData.append("path", path);

    for (let file of files) {
        if (!isValidFileName(file.name) || !isValidFileFormat(file.name)){
            alert(`「${file.name}」をファイル名として出来ません。`);
            continue;
        }
        if (file.relativePath != null) {
            let newFolder = file.relativePath.split("/")[0];
            if (folderList.includes(newFolder + "/")){
                const replace = confirm(`\"${newFolder}/\" already exsist. Do you want to overwrite it's contents?\n(If there are any files with the same name in the current folder they will be replaced!)`);
                if (!replace) continue;
            }
            let relativePath = file.relativePath.split("/");
            let isValid = true;
            for (let dir of relativePath) {
                if (!isValidFileName(dir)) {
                    isValid = false;
                    alert(`\"${file.relativePath}\" has an invalid folder name.`);
                    break;
                }
            }
            if (!isValid) continue;
        }
        if (
            htmlFiles.includes(file.name) ||
            phpFiles.includes(file.name) ||
            cssFiles.includes(file.name) ||
            imgFiles.includes(file.name) ||
            videoFiles.includes(file.name)
        ){
            const replace = confirm(`「${file.name}」 のファイルが存在しています。上書きますか？`);
            if (!replace) {continue;}
        }
        
        formData.append("files", file, file.relativePath);
        fileName = file.name;
        isValidFile = true;
    }
    if (isValidFile) {
        console.log("Uploading file...");

        progress.style.display = "block";
        progress.value = 0;

        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open("POST", "/upload");

            xhr.upload.addEventListener("progress", (e) => {
                if (e.lengthComputable) {
                    const percent = (e.loaded / e.total) * 100;
                    progress.value = percent;
                }
            });
            
            xhr.onload = () => {
                progress.style.display = "none";
                alert(xhr.responseText);
                resolve(fileName);
            };

            xhr.onerror = () => {
                progress.style.display = "none";
                reject(new Error("Upload failed"));
            };

            xhr.send(formData);
        });
    }
}

export async function uploadTempFile(input) {
    const files = input.files;
    const progress = document.getElementById("uploadProgress");
    const formData = new FormData();
    let fileName;
    let isValidFile = false;

    formData.append("path", "temp/");

    for (let file of files) {
        if (!isValidFileName(file.name)){
            alert(`「${file.name}」をファイル名として出来ません。`);
            continue;
        }
        formData.append("files", file, file.relativePath);
        fileName = file.name;
        isValidFile = true;
    }
    if (isValidFile) {
        console.log("Uploading file...");

        progress.style.display = "block";
        progress.value = 0;

        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open("POST", "/upload");

            xhr.upload.addEventListener("progress", (e) => {
                if (e.lengthComputable) {
                    const percent = (e.loaded / e.total) * 100;
                    progress.value = percent;
                }
            });
            
            xhr.onload = () => {
                progress.style.display = "none";
                alert(xhr.responseText);
                resolve(fileName);
            };

            xhr.onerror = () => {
                progress.style.display = "none";
                reject(new Error("Upload failed"));
            };

            xhr.send(formData);
        });
    }
}

export async function saveBlog(blob, path = "", filename){
    if (!isValidFileName(filename)){
        alert("Invalid filename!");
        return;
    }
    if (!isValidPath(path) && path != ""){
        alert("Invalid path!");
        return;
    }
    const content = await blob.text();
    let requestObject = {
        "path"      : path,
        "filename"  : filename,
        "data"      : content
    };
    console.log("Saving blog...");

    const blogs = sessionStorage.getItem("blogList");
    const blogname = filename.replace(".html", ".blog");
    if (!isValidFileName(blogname)){
        alert(`\"${blogname}\" is not a valid filename!`);
        return;
    }
    if (blogs.includes(blogname)){
        const replace = confirm(`「${blogname}」 のファイルが存在しています。上書きますか？`);
        if (!replace) return;
    }

    const response = await fetch("/saveBlog", {
        method:"POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(requestObject)
    });

    let msg = await response.text();
    console.log("Response:", msg);
}

export async function deleteBlog(path = "", filename) {
    if (!isValidFileName(filename)){
        alert("Invalid filename!");
        return;
    }
    if (!isValidPath(path) && path != ""){
        alert("Invalid path!");
        return;
    }
    let requestObject = {
        "path"      : path,
        "filename"  : filename
    };
    console.log("Deleting blog...");
    
    const response = await fetch("/deleteBlog", {
        method  : "POST",
        headers : {"Content-Type" : "application/json"},
        body    : JSON.stringify(requestObject)
    });

    if (!response.ok) throw new Error(`Server responded with ${response.status}`);

    const msg = await response.text();
    console.log(msg);
}

export async function fetchBlogContent(file, path = ""){
    let requestObject = {
        "filename"  : file,
        "path"      : path
    };

    console.log(`Fetching ${path + file}'s content...`);
    let response = await fetch("/getBlogContent", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(requestObject)
    });

    if(!response.ok) {
        throw new Error(`Server responded with ${response.status}`);
    }

    const content = await response.text();
    return content;
}

export async function getFilesFromItems(items){
    let files = [];
    for (const item in items) {
        if (item.kind === "file") {
            const file = item.getAsFile();
            if (file) files.push(file);
        }
    }
    return files;
}

function readEntry(entry){
    return new Promise(resolve => {
        if (entry.isFile){
            entry.file(file => resolve([file]));
        } else if(entry.isDirectory) {
            const dirReader = entry.createReader();
            dirReader.readEntries(async entries => {
                let results = [];
                for (const e of entries) {
                    const r = await readEntry(e);
                    results.push(...r);
                }
                resolve(results);
            });
        }
    });
}

export async function saveContentToFile(path ="", type, file, content) {
    if (!isValidFileName(file)) {
        alert("Invalid filename!");
        return;
    }
    if (!isValidFileFormat(file)) {
        alert("Invalid file format!");
        return;
    }
    let requestObject = {
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

    let msg = await response.text();

    console.log(msg);

    if (msg == "Success") {
        alert("ファイルを保存しました");
    } else {
        alert("保存エラー")
    }
    
}

export async function createFolder(path) {
    let requestObject = {"path"  : path};
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

    if (msg == "Success") return;
    else alert("Failed to create folder...");
}

export async function deleteFolder(path){
    if (!isValidPath(path)){
        alert("Invalid path!");
        return;
    }
    let requestObject = {"path"  : path};
    console.log("Deleting folder...");

    const response = await fetch("/deleteFolder", {
        method  : "POST",
        headers : {"Content-Type" : "application/json"},
        body    : JSON.stringify(requestObject)
    });

    let msg = await response.text();

    console.log(msg);

    if (msg == "Success") alert("Folder deleted");
    else alert("Failed to delete folder...");
}

export async function moveIt(source, target){
    if (!isValidPath(source) || !isValidPath(target)){
        alert("Invallid path!");
        return;
    }
    let requestObject = {
        "source" : source,
        "target" : target
    };
    console.log("Moving it...");

    const response = await fetch("/moveIt", {
        method  : "POST",
        headers : {"Content-Type" : "application/json"},
        body    : JSON.stringify(requestObject)
    });

    let msg = await response.text();
    console.log(msg);
}

export async function renameBlog(path = "", filename, newname) {
    if (!isValidFileName(filename) || !isValidFileName(newname)){
        alert("Invalid filename!");
        return;
    }
    if (!isValidPath(path) && path != "") {
        alert("Invalid path!");
        return;
    }
    let requestObject = {
        "path"      : path,
        "filename"  : filename,
        "newname"   : newname
    };
    if (!isValidFileName(filename)) {
        alert(`\"${filename}\" is not a valid filename!`);
        return;
    }
    console.log("Renaming blog...");
    let response = await fetch('/renameBlog', {
        method : "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(requestObject)
    });
    let msg = await response.text();
}

export function isValidFileName(fileName, nodots){
    if (!fileName) return false;
    if (fileName === "." || fileName === "..") return false;
    let validPattern = /^[a-zA-Z0-9._-]+$/;
    if (nodots == true) validPattern = /^[a-zA-Z0-9_-]+$/;
    return validPattern.test(fileName);
}

export function isValidPath(path) {
    if (!path) return false;

    const validPattern = /^[a-zA-Z0-9._-]+$/;

    // Split into parts by "/"
    const parts = path.split("/");

    for (const part of parts) {
        if (!part) return false;
        if (part === "." || part === "..") return false;
        if (!validPattern.test(part))return false;
    }
    return true;
}

const TEXT_EXTENSIONS = [".html", ".php", ".css", ".js", ".txt", ".blog"];
const IMAGE_EXTENSIONS = [".gif", ".jpg", ".jpeg", ".JPG", ".png", ".webp", ".svg", ".bmp", ".ico", ".avif", ".heic", ".tiff"];
const VIDEO_EXTENSIONS = [".mp4", ".mov", ".avi"];


function isValidFileFormat(filename) {
    const lastDot = filename.lastIndexOf(".");
    if (lastDot === -1) return false;

    const extension = filename.substring(lastDot).toLowerCase();

    const ALL_EXTENSIONS = [
        ...TEXT_EXTENSIONS,
        ...IMAGE_EXTENSIONS,
        ...VIDEO_EXTENSIONS
    ].map(e => e.toLowerCase());

    return ALL_EXTENSIONS.includes(extension);
}
