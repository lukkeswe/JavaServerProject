async function uploadFile(user) {
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