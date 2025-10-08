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