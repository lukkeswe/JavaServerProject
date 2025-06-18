<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Login</title>
</head>
<body>
    <label>Username: </label>
    <input type="text" id="username"><br>
    <label>Password: </label>
    <input type="password" id="password"><br>
    <button id="login">Login</button>

    <script>
        document.getElementById("login").addEventListener("click", () => {
            const u = document.getElementById("username").value;
            const p = document.getElementById("password").value;

            // Set cookies with expiration in 1 day
            document.cookie = `username=${u}; path=/; max-age=86400`;
            document.cookie = `password=${p}; path=/; max-age=86400`;

            // Ensure cookie is flushed to browser before redirect
            setTimeout(() => {
                location.href = "test.php";
            }, 500); // 500ms to be safe
        });
    </script>
</body>
</html>
