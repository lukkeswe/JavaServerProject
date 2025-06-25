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
    <header><h1>データベース管理</h1></header>
    <div id="container"></div>
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <script type="text/javascript">
      (async()=>{
        const db = new DBmanager();
        const dm = new DocumentManager();
        dm.flexContainer();
        dm.dataBurgerMenu();
        dm.mainContainer();
        dm.main = document.getElementById("main-content");
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
