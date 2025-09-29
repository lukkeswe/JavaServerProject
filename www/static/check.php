<?php
require_once(__DIR__ . "/dbmanager.php");
if (!isset($_GET["invite"])){
    header("Location:notfound.html");
    exit();
} else {
    $db = new DBmanager("example@mail.com", "password");
    if (!$db->checkInvite($_GET["invite"])) {
        header("Location:notfound.html");
        exit();
    }
    $invite = $_GET["invite"];
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
    <title>Sign up</title>
</head>
<body>
    <?php echo "<input type='hidden' id='invite' value='{$invite}'>"; ?>
    <div id="container"></div>
    <script type="text/javascript">
        const dm = new DocumentManager();
        dm.userSignUp();
    </script>
</body>
</html>