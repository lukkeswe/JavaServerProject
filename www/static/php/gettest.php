<?php
$msg = "";
if (isset($_GET["value"])){
    $msg = $_GET["value"];
} else {
    $msg = "GET does not work";
}
?>

<p><?php echo $msg; ?></p>

<form action="posttest.php" method="post">
    <input type="text" name="message">
    <input type="submit" value="send">
</form>