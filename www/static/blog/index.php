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
        header("Location:https://norlund-johan-lukas.com/logout.php");
        exit();
    } else {
        $msg = "It worked! " . $response;
    }
    // Initialize the DB manager
    if (isset($_COOKIE["email"]) && isset($_COOKIE["password"])){
        $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
        if(!$db->login()){
            header("Location:https://norlund-johan-lukas.com/logout.php");
            exit();
        }
    } else {
        header("Location:https://norlund-johan-lukas.com/logout.php");
        exit();
    }
    

} else {
    header("Location:https://norlund-johan-lukas.com/server.php");
    exit();
}
?>
<!doctype html>
<html lang="ja">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>Blog creater tool</title>
  <link rel="stylesheet" href="css/style.css">
  <script src="../js/upmanager.js"></script>
  <script src="../js/docmanager.js"></script>
</head>
<body>
  <div id="grayScreen">
    <div id="miniExplorer">
      <input id="filename" type="text" value="index" style="width: 150px;"><span>.html</span>
      <div id="pathContainer"></div>
      <div id="optionsContainer"></div>
      <div id="displayContainer" style="display: none;"></div>
      <div id="filesContainer"></div>
      <div id="uploadBtnContainer"></div>
      <button id="cancel">Cancel</button>
    </div>
  </div>
  <header>
    <div class="toolbar">
      <div class="title">Blog creater tool — ブログ作成ツール</div>
      <div class="muted" style="margin-left:12px">簡単にブログを作ってHTMLをダウンロード</div>
      <div class="controls">
        <button id="addText">＋ 文章を追加</button>
        <button id="addImage">＋ 画像を追加</button>
        <label><input type="checkbox" id="toggleBase64" checked> Base64画像</label>
        <button id="download">↓ HTMLをダウンロード</button>
        <button id="save">↑ HTMLを保存</button>
        <input id="hiddenFile" type="file" accept="image/*" style="display:none" />
      </div>
    </div>
  </header>

  <main>
    <div class="canvas" id="canvas" aria-label="ブログキャンバス"></div>
    <div class="muted" style="margin-top:8px">各ブロックにマウスを合わせると、ブロックの上に編集ボタン（オーバーレイ）が表示されます。</div>
  </main>

  <footer>Background: dark UI • ブロック上のボタンでフォントスタイルやサイズを変更できます</footer>

  <script src="js/blogcreator.js"></script>
  <script>
    const canvas = document.getElementById('canvas');
    const addTextBtn = document.getElementById('addText');
    const addImageBtn = document.getElementById('addImage');
    const hiddenFile = document.getElementById('hiddenFile');
    const downloadBtn = document.getElementById('download');
    const toggleBase64 = document.getElementById('toggleBase64');
    const saveBtn = document.getElementById('save');

    const dm = new DocumentManager("<?php echo $db->username; ?>");
    sessionStorage.setItem("user", "<?php echo $db->username; ?>")

    addTextBtn.addEventListener('click', ()=>{
      const b = createBlock('text');
      canvas.appendChild(b);
      setTimeout(()=>{ b.querySelector('[contenteditable]').focus(); },100);
    });

    addImageBtn.addEventListener('click', ()=>{
      hiddenFile.onchange = async e=>{
        const f = e.target.files[0]; if(!f) return;
        if(toggleBase64.checked){
          const r = new FileReader(); r.onload = ev=>{
            const b = createBlock('image', ev.target.result);
            canvas.appendChild(b);
          }; r.readAsDataURL(f);
        } else {
          const path = f.name;
          const uploadedPath = await uploadFile("<?php echo $db->username; ?>", path);
          const b = createBlock('image', uploadedPath);
          canvas.appendChild(b);
        }
        hiddenFile.value = '';
      };
      hiddenFile.click();
    });

    downloadBtn.addEventListener('click', ()=>{
      const blocks = Array.from(canvas.children);
      let bodyInner = '';
      blocks.forEach(b=>{
        if(b.querySelector('.text-content')){
          const t = b.querySelector('.text-content');
          let html = t.innerHTML.replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '');
          const style = [];
          if(t.style.fontFamily) style.push('font-family:' + t.style.fontFamily);
          if(t.style.fontSize) style.push('font-size:' + t.style.fontSize);
          if(t.style.fontWeight) style.push('font-weight:' + t.style.fontWeight);
          if(t.style.fontStyle) style.push('font-style:' + t.style.fontStyle);
          bodyInner += `\t<div style="${style.join(';')};margin:18px 0;">\n\t\t${html}\n\t</div>\n`;
        } else if(b.querySelector('img')){
          const img = b.querySelector('img');
          bodyInner += `\t<div style=\"margin:18px 0;text-align:center;\">\n\t\t<img src=\"${img.src}\" alt=\"image\" style=\"max-width:100%;height:auto;border-radius:6px;\"/>\n\t</div>`;
        }
      });

      const template = `<!doctype html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n<title>My Blog</title>\n<style>body{background:#111;color:#fff;font-family:system-ui,Arial;line-height:1.6;padding:28px;max-width:750px;margin:0 auto}</style>\n</head>\n<body>\n${bodyInner}\n</body>\n</html>`;

      const blob = new Blob([template], {type:'text/html'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = 'blog.html'; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
    });

    saveBtn.addEventListener("click", async ()=> {
      const blocks = Array.from(canvas.children);
      let bodyInner = '';
      blocks.forEach(b=>{
        if(b.querySelector('.text-content')){
          const t = b.querySelector('.text-content');
          let html = t.innerHTML.replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '');
          const style = [];
          if(t.style.fontFamily) style.push('font-family:' + t.style.fontFamily);
          if(t.style.fontSize) style.push('font-size:' + t.style.fontSize);
          if(t.style.fontWeight) style.push('font-weight:' + t.style.fontWeight);
          if(t.style.fontStyle) style.push('font-style:' + t.style.fontStyle);
          bodyInner += `\t<div style="${style.join(';')};margin:18px 0;">\n\t\t${html}\n\t</div>\n`;
        } else if(b.querySelector('img')){
          const img = b.querySelector('img');
          bodyInner += `\t<div style=\"margin:18px 0;text-align:center;\">\n\t\t<img src=\"${img.src}\" alt=\"image\" style=\"max-width:100%;height:auto;border-radius:6px;\"/>\n\t</div>`;
        }
      });

      const template = `<!doctype html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n<title>My Blog</title>\n<style>body{background:#111;color:#fff;font-family:system-ui,Arial;line-height:1.6;padding:28px;max-width:750px;margin:0 auto}</style>\n</head>\n<body>\n${bodyInner}\n</body>\n</html>`;

      const blob = new Blob([template], {type:'text/html'});

      const user = "<?php echo $db->username; ?>";
      let path = "test/";

      document.getElementById("grayScreen").style.display = "block";

      let currentPath = document.getElementById("path");
      console.log("currentPath: " + !currentPath);
      
      if (currentPath) dm.getFilesMini("<?php echo $db->username; ?>", currentPath.textContent);
      else dm.getFilesMini("<?php echo $db->username; ?>", "");

      const uploadBtnContainer = document.getElementById("uploadBtnContainer");
      const uploadBtn = document.createElement("button");
      uploadBtn.id = "uploadBtn";
      uploadBtn.innerHTML = "Upload";

      uploadBtn.addEventListener("click", async ()=> {
        const filename = document.getElementById("filename").value;

        const file = new File([blob], filename + ".html", {type: "text/html"});

        const fileInput = document.createElement("input");
        fileInput.type = "file";
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        fileInput.files = dataTransfer.files;

        fileInput.id = "fileInput";
        document.body.appendChild(fileInput);

        currentPath = document.getElementById("path");
        if (currentPath && currentPath.innerHTML != ""){
          path = currentPath.textContent;
        }
        await uploadFile(user, path);

        document.body.removeChild(fileInput);
        uploadBtn.remove();
        alert("Blog saved to: " + path);

        document.getElementById("grayScreen").style.display = "none";
      });

      uploadBtnContainer.append(uploadBtn);
    });

    document.getElementById("cancel").addEventListener("click", ()=> {
      document.getElementById("uploadBtn").remove();
      document.getElementById("grayScreen").style.display = "none";
    });

    canvas.appendChild(createBlock('text', '<h1>タイトルを書いてください</h1>\n\t\t<p>イントロダクション...</p>'));
  </script>
</body>
</html>
