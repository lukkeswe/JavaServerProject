<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="/css/reset.css">
    <link rel="stylesheet" href="/css/style.css">
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
    <title>Server</title>
</head>
<body>
    <h1 class="welcome">ようこそ！</h1>
    <form id="loginContainer" method="post" action="/home/">
        <label for="username">メールアドレス：</label><br>
        <input type="text" name="email" id="email" class="textInput"><br>
        <label for="password">パスワード：</label><br>
        <input type="password" name="password" id="password" class="textInput"><br>
        <button type="submit" class="btn" id="loginBtn">ログイン</button><br>
        <a id="contactBtn" href="/contact/">連絡先</a>
    </form>
</body>
</html>