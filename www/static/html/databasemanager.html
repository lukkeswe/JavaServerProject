<!DOCTYPE html>
<html lang="ja">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="static/css/style.css" />
    <link rel="stylesheet" href="static/css/reset.css" />
    <script src="static/js/dbmanager.js"></script>
    <script src="static/js/docmanager.js"></script>
    <script src="static/js/usermanager.js"></script>
    <script src="static/js/security.js"></script>
    <title>Admin</title>
  </head>
  <body>
    <div id="container">
      <h1>データベース管理</h1>
      <div id="dbManager">
        <button id="showTables">接続</button>
        <input type="text" id="input" class="textInput">
        <button id="run" class="btn">実行</button>
      </div>
    </div>
    <script type="text/javascript">
      bouncer();
      const db = new DBmanager();
      const dm = new DocumentManager();
      dm.dataBurgerMenu();
      async function init(){
        if(sessionStorage.getItem("username")){
          db.username = sessionStorage.getItem("username");
          db.password = sessionStorage.getItem("password");
          db.database = sessionStorage.getItem("username") + "_db";
          dm.db = db;
          let data = await db.getTables();
          dm.showTables(data);
        }
      }
      init();
    </script>
  </body>
</html>
