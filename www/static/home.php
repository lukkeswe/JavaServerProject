<?php
require_once(__DIR__ . "/dbmanager.php");
require_once(__DIR__. "/config.php");

$endpoint = "";
if (isset($_COOKIE["email"]) && isset($_COOKIE["password"])){
    $db = new DBmanager($_COOKIE["email"], $_COOKIE["password"]);
    if (!$db->login()){
    header("Location:server.php");
    exit();
    }
    $endpoint = API_SERVER . "/create-session";
} else if(isset($_POST["email"]) && isset($_POST["password"])) {
    $db = new DBmanager($_POST["email"], $_POST["password"]);
    if(!$db->login()){
        header("Location:server.php");
        exit();
    } else {
        // Initialize cURL
        $ch = curl_init();
        // Target server
        $url = API_SERVER . "/create-session";
        curl_setopt($ch, CURLOPT_URL, $url);
        // Set timeout
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
        // Enable POST  method
        curl_setopt($ch, CURLOPT_POST, true);
        // Request data
        $data = [
            "email" => $_POST["email"]
        ];
        // Convert to JSON
        $jsonData = json_encode($data);
        // -- Set the request options --
        // Return response as a string
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        // Prepare POST
        curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonData);
        // Set the header with apropriate content type and leangth
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json',
            'Content-Length: ' . strlen($jsonData)
        ]);
        // Execute POST request
        $response = curl_exec($ch);
        // Error check
        if ($response === false) {
            header("Location:server.php");
            exit();
        } else {
            // Set the cookies
            setcookie("javasession", $response, 0);
            setcookie("email", $_POST["email"], 0, "/", "", true, true);
            setcookie("password", $_POST["password"], 0, "/", "", true, true);
        }
        curl_close($ch);
    }
} else {
    header("Location:server.php");
    exit();
}
?>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="css/reset.css">
    <link rel="stylesheet" href="css/style.css">
    <link rel="icon" type="image/x-icon" href="favicon.ico">
    <script src="js/docmanager.js"></script>
    <title>Home</title>
</head>
<body>
    <header><h1>サーバーチーム</h1></header>
    <div id="container"></div>
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <script type="text/javascript">
        if (getCookie("javasession") != null){
            sessionStorage.setItem("email", "<?php echo $db->email; ?>");
            sessionStorage.setItem("domain", "<?php echo $db->domain;?>");
            sessionStorage.setItem("phone", <?php echo $db->phone;?>);
            const dm = new DocumentManager();
            dm.flexContainer();
            dm.burgerMenu();
            dm.mainContainer();
            dm.main = document.getElementById("main-content");
            dm.showUserDash();
        } else {
            console.log("No session!");
            
        }

        function getCookie(key){
            const value = `; ${document.cookie}`;
            const parts = value.split(`; ${key}=`);
            if (parts.length === 2){
                return parts.pop().split(";").shift();
            }
            return null;
        }
    </script>
</body>
</html>