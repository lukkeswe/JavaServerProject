<?php
$msg = "";
if (!isset($_POST["message"])){
    $msg = "POST does not work";
} else {
    $msg = $_POST["message"];
}
?>

<p><?php echo $msg; ?></p>