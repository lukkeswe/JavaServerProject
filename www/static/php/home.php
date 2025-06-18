<?php
header("Location:server.php");
exit();

$host       = "localhost";
$db         = "webserver";
$user       = "lukas";
$password   = "Tvt!77@ren";

try {
    $conn = new PDO("mysql:host=$host;dbname=$db", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e){
    echo $e->getMessage();
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
    <script src="static/js/usermanager.js"></script>
    <script src="static/js/security.js"></script>
    <title>Home</title>
    
</head>
<body>
    <div id="container"><?php echo "hello"; ?></div>
    <script type="text/javascript">
        //bouncer();
        if (sessionStorage.getItem("username")){
            const usr = new UserManager(
            sessionStorage.getItem("username"),
            sessionStorage.getItem("password")
            );
            const dm = new DocumentManager();
            dm.flexContainer();
            dm.burgerMenu();
            dm.showUserDash();
        }
    </script>
</body>
</html>