async function uploadFile() {
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
            const replace = confirm(`The file "${file.name}" already existing. Do you want to replace it?`);
            if (!replace) {continue;}
        } else if (!isValidFileName(file.name)){
            alert(`\"${file.name}\" is not a valid file name.`);
            continue;
        }
        formData.append("files", file);
        fileName = file.name;
        isValidFile = true;
    }
    if (isValidFile) {
        const user = sessionStorage.getItem("username");
        formData.append("user", user);

        console.log("Uploading file...");
        
        await fetch('/upload', {
            method  : 'POST',
            body    : formData
        }).then(res => res.text())
        .then(msg => alert(msg));

        return fileName;
    }
}

function isValidFileName(fileName){
    if (!fileName || fileName.trim() === "") return false;
    const validPattern = /^[a-zA-Z0-9._-]+$/;
    return validPattern.test(fileName);
}