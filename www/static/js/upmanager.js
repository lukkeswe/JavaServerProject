export async function uploadFile(input, path = "") {
    const files = input.files;
    const progress = document.getElementById("uploadProgress");
    const formData = new FormData();
    let htmlFiles = sessionStorage.getItem("htmlList");
    let phpFiles = sessionStorage.getItem("phpList");
    let cssFiles = sessionStorage.getItem("cssList");
    let imgFiles = sessionStorage.getItem("imgList");
    let fileName;
    let isValidFile = false;

    formData.append("path", path);

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
    const content = await blob.text();
    let requestObject = {
        "path"      : path,
        "filename"  : filename,
        "data"      : content
    };
    console.log("Saving blog...");

    const blogs = sessionStorage.getItem("blogList");
    const blogname = filename.replace(".html", ".blog");
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

export function isValidFileName(fileName){
    if (!fileName || fileName.trim() === "") return false;
    const validPattern = /^[a-zA-Z0-9._-]+$/;
    return validPattern.test(fileName);
}

export function isValidPath(path) {
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