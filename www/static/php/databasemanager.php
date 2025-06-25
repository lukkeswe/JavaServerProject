<?php
require_once(__DIR__ . "/dbmanager.php");
if(!isset($_COOKIE["email"]) || !isset($_COOKIE["password"])){
  header("Location:server.php");
  exit();
} else {
  $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
  if (!$db->login()){
    header("Location:server.php");
    exit();
  }
}
?>
<!DOCTYPE html>
<html lang="ja">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="static/css/style.css" />
    <link rel="stylesheet" href="static/css/reset.css" />
    <script src="static/js/dbmanager.js"></script>
    <script src="static/js/docmanager.js"></script>
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
      (async()=>{
        const db = new DBmanager();
        const dm = new DocumentManager();
        dm.dataBurgerMenu();
        db.username = sessionStorage.getItem("username");
        db.password = sessionStorage.getItem("password");
        db.database = sessionStorage.getItem("username") + "_db";
        dm.db = db;
        let data = await db.getTables();
        dm.showTables(data);
      })();
    </script>
  </body>
</html>
