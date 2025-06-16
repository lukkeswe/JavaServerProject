<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <script src="static/js/docmanager.js"></script>
    <script src="static/js/security.js"></script>
    <title>Wait</title>
</head>
<body>
    <div id="container"><img src="img/muppet-load.gif" alt="loading"></div>
    <script type="text/javascript">
        async function init(){
            const inviteStatus = await magproblem();
            if (inviteStatus == "ok"){
                const dm = new DocumentManager();
                dm.userSignUp();
            }
        }
        init();
    </script>
</body>
</html>