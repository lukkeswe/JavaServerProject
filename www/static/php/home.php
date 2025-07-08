<?php
require_once(__DIR__ . "/dbmanager.php");
if (!isset($_POST["email"]) || !isset($_POST["password"])){
    header("Location:server.php");
    exit();
} else {
    $db = new DBmanager($_POST["email"], $_POST["password"]);
    if(!$db->login()){
        header("Location:server.php");
        exit();
    }
}
?>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="css/reset.css">
    <link rel="stylesheet" href="css/style.css">
    <script src="static/js/docmanager.js"></script>
    <title>Home</title>
    <script type="text/javascript">
        sessionStorage.setItem("username", "<?php echo $db->username;?>");
        sessionStorage.setItem("email", "<?php echo $db->email; ?>");
        sessionStorage.setItem("domain", "<?php echo $db->domain;?>");
        sessionStorage.setItem("phone", <?php echo $db->phone;?>);
        sessionStorage.setItem("password", "<?php echo $db->password;?>");
    </script>
</head>
<body>
    <header><h1>サーバーチーム</h1></header>
    <div id="container"></div>
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <script type="text/javascript">
        if (sessionStorage.getItem("username")){
            const dm = new DocumentManager();
            dm.flexContainer();
            dm.burgerMenu();
            dm.mainContainer();
            dm.main = document.getElementById("main-content");
            dm.showUserDash();
        } else {
            console.log("No username!");
            
        }
    </script>
</body>
</html>