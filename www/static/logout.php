<?php
setcookie("email", time() - 3600, "/");
setcookie("password", time() - 3600, "/");
setcookie("javasession", time() - 3600, "/");

header("Location:server.php");
exit();

?>