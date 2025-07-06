async function uploadFile() {
    const files = document.getElementById('fileInput').files;
    const formData = new FormData();
    let htmlFiles = sessionStorage.getItem("htmlList");
    console.log("html: ", htmlFiles);
    let cssFiles = sessionStorage.getItem("cssList");
    console.log("css: ", cssFiles);
    let imgFiles = sessionStorage.getItem("imgList");
    console.log("img: ", imgFiles);
    for (let file of files) {
        if (
            htmlFiles.includes(file.name) ||
            cssFiles.includes(file.name) ||
            imgFiles.includes(file.name)
        ){
            const replace = confirm(`The file "${file.name}" already existing. Do you want to replace it?`);
            if (!replace) {continue;}
        }
        formData.append("files", file);
    }

    const user = sessionStorage.getItem("username");
    formData.append("user", user);

    console.log("Uploading file...");
    
    await fetch('/upload', {
        method  : 'POST',
        body    : formData
    }).then(res => res.text())
      .then(msg => alert(msg));
}