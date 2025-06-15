function uploadFile() {
    const files = document.getElementById('fileInput').files;
    const formData = new FormData();
    for (let file of files) {
        formData.append("files", file);
    }

    const user = sessionStorage.getItem("username");
    formData.append("user", user);

    console.log("Uploading file...");
    
    fetch('/upload', {
        method  : 'POST',
        body    : formData
    }).then(res => res.text())
      .then(msg => alert(msg));
}