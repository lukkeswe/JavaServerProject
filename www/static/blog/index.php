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
  <link rel="stylesheet" href="../css/style.css">
  <link rel="stylesheet" href="css/style.css">
  <script src="../js/upmanager.js"></script>
  <script src="../js/docmanager.js"></script>
  <style>footer{background: none;border: none;}</style>
</head>
<body>
  <div id="grayScreen">
    <div id="loadImage" style="width: 50%;margin:0 auto;display:none;">
      <img src="../img/muppet-load.gif" alt="muppet load image">
    </div>
    <div id="miniExplorer" style="display: none;">
      <input id="filename" type="text" value="index" style="width: 150px;"><span>.blog</span>
      <div id="pathContainerMini"><p id="pathMini"></p></div>
      <div id="optionsContainerMini"></div>
      <div id="displayContainerMini" style="display: none;"></div>
      <div id="filesContainerMini"></div>
      <div class="spaceBetween" style="margin-top:10px;">
        <div id="uploadBtnContainerMini"></div>
        <button id="cancel" class="btn cancel"></button>
      </div>
    </div>
  </div>
  <header>
    <div id="toolbar" class="toolbar">
      <div class="title">Blog creater tool — ブログ作成ツール</div>
      <div class="muted" style="margin-left:12px">簡単にブログを作ってHTMLをダウンロード</div>
      <div class="controls">
        <button id="addTitle">＋タイトル追加</button>
        <button id="addText">＋ 文章を追加</button>
        <button id="addImage">＋ 画像を追加</button>
        <label><input type="checkbox" id="toggleBase64" checked> Base64画像</label>
        <button id="download">↓ HTMLをダウンロード</button>
        <button id="save">↑ BLOGを保存</button>
        <input id="hiddenFile" type="file" accept="image/*" style="display:none" />
      </div>
    </div>
  </header>

  <main>
    <div class="canvas" id="canvas" aria-label="ブログキャンバス"></div>
    <div class="muted" style="margin-top:8px">各ブロックにマウスを合わせると、ブロックの上に編集ボタン（オーバーレイ）が表示されます。</div>
  </main>

  <footer>Norlund J. Lukas</footer>

  <script src="js/blogcreator.js"></script>
  <script type="module">
    const grayScreen = document.getElementById('grayScreen');
    const miniExplorer = document.getElementById('miniExplorer');
    const loadImage = document.getElementById("loadImage");
    const toolbar = document.getElementById('toolbar');
    const canvas = document.getElementById('canvas');
    const addTitleBtn = document.getElementById('addTitle');
    const addTextBtn = document.getElementById('addText');
    const addImageBtn = document.getElementById('addImage');
    const hiddenFile = document.getElementById('hiddenFile');
    const downloadBtn = document.getElementById('download');
    const toggleBase64 = document.getElementById('toggleBase64');
    const saveBtn = document.getElementById('save');

    import DM from '../js/docmanager.js';
    import * as UpManager from "../js/upmanager.js";

    sessionStorage.setItem("domain", "<?php echo $db->domain;?>");
    const dm = new DM();

    addTitleBtn.addEventListener('click', ()=> {
      const b = createBlock('title');
      canvas.appendChild(b);
      setTimeout(()=>{ b.querySelector('[contenteditable]').focus(); }, 100);
    });
    
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
          grayScreen.style.display = "block";
          loadImage.style.display = "block";
          const uploadedPath = await UpManager.uploadTempFile(hiddenFile);
          grayScreen.style.display = "none";
          loadImage.style.display = "none";
          const b = createBlock('image', "https://<?php echo $db->domain;?>/temp/" + path);
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

      const template = `<!doctype html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n<title>My Blog</title>\n<style>body{background:#fff;color:#111;font-family:system-ui,Arial;line-height:1.6;padding:28px;max-width:750px;margin:0 auto;margin-top:50px;box-shadow: 0 6px 20px rgba(2, 6, 10, 0.6);border-radius: 12px;}</style>\n</head>\n<body>\n${bodyInner}\n</body>\n</html>`;

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
          // Clean HTML
          let html = t.innerHTML.replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '');
          // Read metadata from the block itself
          const type = b.dataset.block || 'text';
          const font = b.dataset.font || t.style.fontFamily || '';
          const size = b.dataset.size || t.style.fontSize || '';
          const bold = b.dataset.bold || t.style.fontWeight || '';
          const italic = b.dataset.italic || t.style.fontStyle || '';

          const style = [];
          if (font) style.push('font-family:' + font);
          if (size) style.push('font-size:' + size);
          if (bold) style.push('font-weight:' + bold);
          if (italic) style.push('font-style:' + italic);
          bodyInner += `\t<div 
    data-block="${type}"
    data-font="${font}"
    data-size="${size}"
    data-bold="${bold}"
    data-italic="${italic}"
    style="${style.join(';')};margin:18px 0;">
        ${html}
    </div>\n`;

        } else if(b.querySelector('img')){
          const img = b.querySelector('img');
          //Image metadata
          const type = b.dataset.block || 'image';

          bodyInner += `\t<div 
    data-block="${type}"
    style="margin:18px 0;text-align:center;">
        <img src="${img.src}" alt="image" style="max-width:100%;height:auto;border-radius:6px;"/>
    </div>\n`;
        }
      });

      const template = `<!doctype html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n<title>My Blog</title>\n<style>body{background:#fff;color:#111;font-family:system-ui,Arial;line-height:1.6;padding:28px;max-width:750px;margin:0 auto;margin-top:50px;box-shadow: 0 6px 20px rgba(2, 6, 10, 0.6);border-radius: 12px;}</style>\n</head>\n<body>\n${bodyInner}\n</body>\n</html>`;

      const blob = new Blob([template], {type:'text/html'});

      let path = "";
      // Show the explorer
      grayScreen.style.display = "block";
      miniExplorer.style.display = "block";
      // Get the current path
      let currentPath = document.getElementById("pathMini");
      // Get the files
      if (currentPath) dm.getFilesMini(currentPath.textContent);
      else dm.getFilesMini("");
      // Create an upload button
      const uploadBtnContainer = document.getElementById("uploadBtnContainerMini");
      const uploadBtn = document.createElement("button");
      uploadBtn.id = "uploadBtn";
      uploadBtn.classList.add("btn");
      uploadBtn.classList.add("optionsIcon");
      uploadBtn.classList.add("save");
      // Add an eventlistener
      uploadBtn.addEventListener("click", async ()=> {
        // Get the filename
        const filename = document.getElementById("filename").value;
        // Create a file
        const file = new File([blob], filename + ".html", {type: "text/html"});
        // Get the current path
        currentPath = document.getElementById("pathMini");
        if (currentPath && currentPath.innerHTML != ""){
          path = currentPath.textContent;
        }
        // Hide the explorer
        miniExplorer.style.display = "none";
        // Show the loading image
        loadImage.style.display = "block";
        // Upload the file to the server
        await UpManager.saveBlog(blob, path, filename + ".html");
        // Remove temporary elements
        uploadBtn.remove();
        // Alert the user
        alert("Blog saved to: " + path);
        // Hide the loading image and grayscreen
        loadImage.style.display = "none";
        grayScreen.style.display = "none";
      });
      // Append the upload button
      uploadBtnContainer.append(uploadBtn);
    });

    document.getElementById("cancel").addEventListener("click", ()=> {
      document.getElementById("uploadBtn").remove();
      miniExplorer.style.display = "none";
      grayScreen.style.display = "none";
    });

    const params = new URLSearchParams(window.location.search);

    const filename = params.get("filename");
    const path = params.get("path");

    if (filename){
      const content = await UpManager.fetchBlogContent(filename, path);
      if (content) {
        importHTML(content, canvas);
      } else console.error("Error: importHTML");
    } else {
      canvas.appendChild(createBlock('text', '<p>Text here...</p>'));
    }
  </script>
</body>
</html>
