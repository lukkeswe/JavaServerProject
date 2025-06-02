async function bouncer(){
    if (sessionStorage.getItem("username")){
            console.log("Security...");
            await securityCheck();
            console.log(`OK! Welcome ${sessionStorage.getItem("username")}`);
        } else {
            console.log("Redirecting");
            window.location.href = "/server.html";
        }
}
async function securityCheck(){
    let requestObject = {
        "username"  : sessionStorage.getItem("username"),
        "password"  : sessionStorage.getItem("password")
    };
    try {
        const response = await fetch('/login', {
            method  : 'POST',
            headers : {'Content-Type': 'application/json'},
            body    : JSON.stringify(requestObject)
        });
        if (!response.ok) {
            window.location.href = "/server.html";
        } else {console.log("Permission granted.");}
    } catch (error){
        console.error(error);
        window.location.href = "/server.html";
    }
}

async function magproblem() {
    console.log("in here")
    const params = new URLSearchParams(window.location.search);
    const invite = params.get('invite');
    console.log(`Invite code: ${invite}`);
    const requsestObject = {"invite": invite};
    try {
        const response = await fetch('/invite', {
            method  : 'POST',
            headers : {'Content-Type': 'application/json'},
            body    : JSON.stringify(requsestObject)
        });
        if(!response.ok){
            throw new Error(`Server status ${response.status}`);
        }
        const data = await response.json();
        if (data[0]["status"] == "ok"){
            console.log("Invite: ok");
            
            return data[0]["status"];
        } else {
            console.log(data[0]["status"]);
            window.location.href = "/server.html";
        }
    } catch (error){
        console.error("Error: ", error);
        window.location.href = "/server.html";
    }
}

function logout(){
    if(sessionStorage.getItem("username")){
        sessionStorage.removeItem("username");
        sessionStorage.removeItem("password");
    }
}