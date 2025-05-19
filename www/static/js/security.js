function bouncer(){
    if (sessionStorage.getItem("username")){
            console.log("Security...");
            securityCheck();
            console.log(`OK! Welcome ${sessionStorage.getItem("username")}`);
        } else {
            console.log("Redirecting");
            window.location.href = "/";
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
            window.location.href = "/";
        } else {console.log("Permission granted.");}
    } catch (error){
        console.error(error);
        window.location.href = "/";
    }
}