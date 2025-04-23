class DBmanager {
    constructor (database = "webserver", username = "root", password = "tvtittaren"){
        this.database = database;
        this.username = username;
        this.password = password;
    }
    async executeQuery(){
        const input = document.getElementById("input");
        let query = input.value;
        let requestObject = {
            "database"  : this.database,
            "username"  : this.username,
            "password"  : this.password,
            "query"     : query
        };
        try {
            const response = await fetch('/query', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(requestObject)
            });
            if (!response.ok) {
                throw new Error(`Server responded with status ${response.status}`);
            }
            const data = await response.json();
            console.log("Response from server:", data);
        } catch (error) {
            console.error('Error executing query:', error);
        }
    }

}

const db = new DBmanager();