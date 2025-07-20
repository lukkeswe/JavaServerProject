<?php
session_start();

if ($_SESSION["email"] || $_SESSION["password"]){
    unset($_SESSION["email"]);
    unset($_SESSION["password"]);

    header("Location:server.php");
    exit();
} else {
    header("Location:server.php");
    exit();
}
?>