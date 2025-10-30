<?php
require_once(__DIR__ . "/../dbmanager.php");
require_once(__DIR__. "/../config.php");

$msg = "";
$endpoint = "";
if (isset($_COOKIE["javasession"])){
    // Initialize cURL
    $ch = curl_init();
    // Target server
    $url = API_SERVER . "/check-session";
    curl_setopt($ch, CURLOPT_URL, $url);
    // Set timeout
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    // Enable POST
    curl_setopt($ch, CURLOPT_POST, true);
    // Request data
    $data = ["session" => $_COOKIE["javasession"]];
    // Convert to JSON
    $jsonData = json_encode($data);
    // -- Set request options --
    // Return response as a string
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    // Prepare POST
    curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonData);
    // Set the header with apropriate content type and leangth
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Content-Length: ' . strlen($jsonData) 
    ]);
    // Execute POST
    $response = curl_exec($ch);
    // Get the HTTP code
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    // Error check
    if ($response === false || $httpCode != 200) {
        header("Location:/logout/");
        exit();
    } else {
        $msg = "It worked! " . $response;
    }
} else if(isset($_POST["email"]) && isset($_POST["password"])) {
    $db = new DBmanager($_POST["email"], $_POST["password"]);
    if(!$db->login()){
        header("Location:/login/");
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
            header("Location:/login/");
            exit();
        } else {
            // Set the cookies
            setcookie("javasession", $response, 0, "/");
            setcookie("email", $_POST["email"], 0, "/", "", true, true);
            setcookie("password", $_POST["password"], 0, "/", "", true, true);
        }
        curl_close($ch);
    }
} else {
    header("Location:/login/");
    exit();
}
?>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="/css/reset.css">
    <link rel="stylesheet" href="/css/style.css">
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
    <script type="module" src="/js/docmanager.js"></script>
    <title>Home</title>
</head>
<body>
    <header><h1>サーバーチーム</h1></header>
    <div id="container"></div>
    <footer><p>&copy;Norlund J. Lukas</p></footer>
    <script type="module">
        import DM from '../js/docmanager.js';
        if (getCookie("javasession") != null){
            console.log("<?php echo $msg;?>");
            sessionStorage.setItem("email", "<?php echo $db->email; ?>");
            sessionStorage.setItem("domain", "<?php echo $db->domain;?>");
            sessionStorage.setItem("phone", "<?php echo $db->phone;?>");
            const dm = new DM();
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