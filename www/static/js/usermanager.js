class UserManager{
    constructor(username, password){
        this.username = username,
        this.password = password
    }
    async login(){
        let requestObject = {
            "username"  : this.username,
            "password"  : this.password
        }
        try {
            const response = await fetch('/login', {
                method  : 'POST',
                headers : {'Content-Type': 'application/json'},
                body    : JSON.stringify(requestObject)
            });
            if (!response.ok){
                throw new Error(`Server responded with status ${response.status}`);
            }
            const data = await response.json();
            console.log("Response:", data);
            
            if (data[0]["status"] == "failed") {
                alert("Login failed :(");
            } else if (data[0]["id"]){
                sessionStorage.setItem("username", data[0]["name"]);
                sessionStorage.setItem("password", data[0]["password"]);
                sessionStorage.setItem("domain", data[0]["domain"]);
                sessionStorage.setItem("email", data[0]["email"]);
                sessionStorage.setItem("phone", data[0]["phone"]);
                console.log(sessionStorage.getItem("username") + " logged in.");
                
            }
        } catch (error){
            console.error('Error loging in:', error);            
        }
    }
}