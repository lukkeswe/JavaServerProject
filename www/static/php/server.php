<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="static/css/reset.css">
    <link rel="stylesheet" href="static/css/style.css">
    <title>index</title>
</head>
<body>
    <h1 class="welcome">ようこそ！</h1>
    <form id="loginContainer" method="post" action="home.php">
        <label for="username">ユーザー名：</label><br>
        <input type="text" name="email" id="email" class="textInput"><br>
        <label for="password">パスワード：</label><br>
        <input type="password" name="password" id="password" class="textInput"><br>
        <button type="submit" class="btn" id="loginBtn">ログイン</button><br>
    </form>
    <div id="container">
        <a class="btn" href="contact.html">contact</a>
    </div>
</body>
</html>