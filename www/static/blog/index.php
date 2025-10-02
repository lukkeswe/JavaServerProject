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
  <style>
    :root{--bg:#0b0b0d;--panel:#111214;--muted:#9aa0a6;--accent:#6ee7b7;--card:#131316}
    html,body{height:100%;margin:0;font-family:Inter,ui-sans-serif,system-ui,Segoe UI,Roboto,'Helvetica Neue',Arial}
    body{background:linear-gradient(180deg,#070708 0%, #0f0f10 100%);color:#e6eef3}

    header{position:sticky;top:0;z-index:30;background:linear-gradient(180deg,rgba(255,255,255,0.02),transparent);backdrop-filter:blur(4px);border-bottom:1px solid rgba(255,255,255,0.03)}
    .toolbar{display:flex;gap:12px;align-items:center;padding:12px 16px}
    .title{font-weight:700;font-size:18px}
    .controls{margin-left:auto;display:flex;gap:8px}

    button,select,input[type="range"]{background:transparent;border:1px solid rgba(255,255,255,0.06);color:var(--muted);padding:8px 10px;border-radius:8px;font-size:13px}
    button:hover{border-color:rgba(255,255,255,0.12);color:#fff}

    main{max-width:900px;margin:24px auto;padding:8px}
    .canvas{min-height:60vh;border-radius:12px;padding:18px;background:linear-gradient(180deg, rgba(255,255,255,0.01), rgba(255,255,255,0.005));box-shadow:0 6px 20px rgba(2,6,10,0.6)}

    .block{position:relative;margin:18px 0;padding:12px;border-radius:10px;background:rgba(255,255,255,0.01);overflow:hidden}
    .block:hover{box-shadow:0 6px 20px rgba(0,0,0,0.6)}

    /* overlay toolbar placed on top of the block */
    .block .overlay{display:flex;gap:8px;align-items:center;z-index:20;backdrop-filter:blur(4px);padding:6px;border-radius:8px;border:1px solid rgba(255,255,255,0.04);background:linear-gradient(180deg, rgba(0,0,0,0.35), rgba(255,255,255,0.02))}

    .text-content[contenteditable]{min-height:60px;outline:none}
    .text-content.small{font-size:14px}
    .text-content.medium{font-size:18px}
    .text-content.large{font-size:24px}

    .img-wrap{display:flex;align-items:center;justify-content:center}
    .img-wrap img{max-width:100%;border-radius:6px}

    .muted{color:var(--muted);font-size:13px}

    footer{max-width:900px;margin:12px auto;padding:8px;color:var(--muted);font-size:13px}

    /* small responsive */
    @media (max-width:640px){.toolbar{flex-wrap:wrap}.controls{width:100%;margin:0;justify-content:space-between}}
  </style>
  <script src="../js/upmanager.js"></script>
</head>
<body>
  <header>
    <div class="toolbar">
      <div class="title">Blog creater tool — ブログ作成ツール</div>
      <div class="muted" style="margin-left:12px">簡単にブログを作ってHTMLをダウンロード</div>
      <div class="controls">
        <button id="addText">＋ 文章を追加</button>
        <button id="addImage">＋ 画像を追加</button>
        <label><input type="checkbox" id="toggleBase64" checked> Base64画像</label>
        <button id="download">↓ HTMLをダウンロード</button>
        <input id="hiddenFile" type="file" accept="image/*" style="display:none" />
      </div>
    </div>
  </header>

  <main>
    <div class="canvas" id="canvas" aria-label="ブログキャンバス"></div>
    <div class="muted" style="margin-top:8px">各ブロックにマウスを合わせると、ブロックの上に編集ボタン（オーバーレイ）が表示されます。</div>
  </main>

  <footer>Background: dark UI • ブロック上のボタンでフォントスタイルやサイズを変更できます</footer>

  <script>
    const canvas = document.getElementById('canvas');
    const addTextBtn = document.getElementById('addText');
    const addImageBtn = document.getElementById('addImage');
    const hiddenFile = document.getElementById('hiddenFile');
    const downloadBtn = document.getElementById('download');
    const toggleBase64 = document.getElementById('toggleBase64');

    function createBlock(type, initial){
      const block = document.createElement('section');
      block.className = 'block';

      const overlay = document.createElement('div');
      overlay.className = 'overlay';

      const fontSelect = document.createElement('select');
      ['Sans','Serif','Monospace','Cursive','Georgia','Tahoma'].forEach(f=>{
        const opt = document.createElement('option'); opt.value = f; opt.textContent = f; fontSelect.appendChild(opt);
      });
      overlay.appendChild(fontSelect);

      const sizeInput = document.createElement('input');
      sizeInput.type = 'range'; sizeInput.min = 12; sizeInput.max = 48; sizeInput.value = 18;
      overlay.appendChild(sizeInput);

      const boldBtn = document.createElement('button'); boldBtn.textContent = 'B'; overlay.appendChild(boldBtn);
      const italicBtn = document.createElement('button'); italicBtn.textContent = 'I'; overlay.appendChild(italicBtn);

      const delBtn = document.createElement('button'); delBtn.textContent = '削除'; overlay.appendChild(delBtn);
      const upBtn = document.createElement('button'); upBtn.textContent = '↑'; overlay.appendChild(upBtn);
      const downBtn = document.createElement('button'); downBtn.textContent = '↓'; overlay.appendChild(downBtn);

      block.appendChild(overlay);

      if(type==='text'){
        const t = document.createElement('div');
        t.className = 'text-content medium';
        t.contentEditable = true;
        t.spellcheck = false;
        t.innerHTML = initial || '<h1>タイトルを書いてください</h1>\n\t\t<p>ここに文章を入力してください...</p>';
        t.style.fontFamily = 'Sans';
        block.appendChild(t);

        fontSelect.addEventListener('change',()=>{ t.style.fontFamily = fontSelect.value; });
        sizeInput.addEventListener('input',()=>{ t.style.fontSize = sizeInput.value + 'px'; });
        boldBtn.addEventListener('click',()=>{ t.style.fontWeight = (t.style.fontWeight === '700' ? '400' : '700'); });
        italicBtn.addEventListener('click',()=>{ t.style.fontStyle = (t.style.fontStyle === 'italic' ? 'normal' : 'italic'); });
      } else if(type==='image'){
        const wrap = document.createElement('div'); wrap.className='img-wrap';
        const img = document.createElement('img'); img.alt = 'uploaded image';
        if(initial) img.src = initial;
        wrap.appendChild(img);
        block.appendChild(wrap);

        fontSelect.style.display='none';
        sizeInput.style.display='none';
        boldBtn.style.display='none';
        italicBtn.style.display='none';

        overlay.addEventListener('click',async ()=>{
          hiddenFile.onchange = async e=>{
            const f = e.target.files[0]; if(!f) return;
            if(toggleBase64.checked){
              const r = new FileReader(); r.onload = ev=>{ img.src = ev.target.result; }; r.readAsDataURL(f);
            } else {
              const path = f.name;
              const uploadedPath = await uploadFile("<?php echo $db->username; ?>", path);
              img.src = uploadedPath;
            }
            hiddenFile.value = '';
          };
          hiddenFile.click();
        });
      }

      delBtn.addEventListener('click',()=>{ if(confirm('このブロックを削除しますか？')) block.remove(); });
      upBtn.addEventListener('click',()=>{ const prev = block.previousElementSibling; if(prev) canvas.insertBefore(block, prev); });
      downBtn.addEventListener('click',()=>{ const next = block.nextElementSibling; if(next) canvas.insertBefore(next, block); });

      return block;
    }

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
          bodyInner += `\t<div style="${style.join(';')};margin:18px 0;">\n
                          \t\t${html}\n
                        \t</div>\n`;
        } else if(b.querySelector('img')){
          const img = b.querySelector('img');
          bodyInner += `\t<div style=\"margin:18px 0;text-align:center;\">\n
                          \t\t<img src=\"${img.src}\" alt=\"image\" style=\"max-width:100%;height:auto;border-radius:6px;\"/>\n
                        \t</div>`;
        }
      });

      const template = `<!doctype html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n<title>My Blog</title>\n<style>body{background:#111;color:#fff;font-family:system-ui,Arial;line-height:1.6;padding:28px;max-width:750px;margin:0 auto}</style>\n</head>\n<body>\n${bodyInner}\n</body>\n</html>`;

      const blob = new Blob([template], {type:'text/html'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = 'blog.html'; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
    });

    canvas.appendChild(createBlock('text', '<h1>タイトルを書いてください</h1>\n\t\t<p>イントロダクション...</p>'));
  </script>
</body>
</html>
