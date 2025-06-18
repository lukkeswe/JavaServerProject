<?php
require_once(__DIR__ . "/dbmanager.php");
$msg = "no message";
if (!isset($_COOKIE["email"]) && !isset($_COOKIE["password"])){
    header("Location:test2.php");
    exit();
} else {
    $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
    if (!$db->login()) {
        header("Location:test2.php");
        exit();
    }
}
?>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>test</title>
</head>
<body>
<p>Welcome <?= htmlspecialchars($_COOKIE['email']) ?>!</p>
</body>
</html>