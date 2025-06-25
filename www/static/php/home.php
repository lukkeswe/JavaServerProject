<?php
require_once(__DIR__ . "/dbmanager.php");
if (!isset($_COOKIE["email"]) || !isset($_COOKIE["password"])){
    header("Location:server.php");
    exit();
} else {
    $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
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
        sessionStorage.setItem("domain", "<?php echo $db->domain;?>");
        sessionStorage.setItem("phone", <?php echo $db->phone;?>);
        sessionStorage.setItem("password", "<?php echo $db->password;?>");
    </script>
</head>
<body>
    <div id="container"></div>
    <script type="text/javascript">
        if (sessionStorage.getItem("username")){
            const dm = new DocumentManager();
            dm.flexContainer();
            dm.burgerMenu();
            dm.showUserDash();
        } else {
            console.log("No username!");
            
        }
    </script>
</body>
</html>