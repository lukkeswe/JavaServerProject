class DBmanager {
    constructor (
        database = "webserver", 
        username, 
        password, 
        table
    ){
        this.database   = database;
        this.username   = username;
        this.password   = password;
        this.table      = table;
    }
    // Show the tables in a database
    async getTables(){
        let requestObject = {
            "database"  : this.database,
            "username"  : this.username,
            "password"  : this.password,
            "query"     : "SHOW TABLES"
        };
        try {
            const response = await fetch('/query', {
                method  : 'POST',
                headers : {'Content-Type': 'application/json'},
                body    : JSON.stringify(requestObject)
            });
            if (!response.ok) {
                throw new Error(`Server responded with status ${response.status}`);
            }
            const data = await response.json();
            console.log("Response from server:", data);
            return data;
        } catch (error) {
            console.error('Error execution query:', error);
        }
    }
    // Free query
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
            let data = await response.json();
            console.log("Response from server:", data);
            // Check if it is a query of a table
            if (this.getTableName(query)) {
                // Append table name to the data array
                data.push(this.getTableName(query));
            }
            return data;
        } catch (error) {
            console.error('Error executing query:', error);
        }
    }
    // Select everything from a table
    async selectEverything(table){
        let requestObject = {
            "database"  : this.database,
            "username"  : this.username,
            "password"  : this.password,
            "query"     : "SELECT * FROM " + table
        };
        try {
            const response = await fetch('/query', {
                method  : 'POST',
                headers : {'Content-Type': 'application/json'},
                body    : JSON.stringify(requestObject)
            });
            if (!response.ok) {
                throw new Error(`Server responded with status ${response.status}`);
            }
            const data = await response.json();
            console.log("Response from server:", data);
            return data;
        } catch (error) {
            console.error('Error execution query:', error);
        }
    }
    async deleteRowById(column, id){
        let requestObject = {
            "database"  : this.database,
            "username"  : this.username,
            "password"  : this.password,
            "table"     : this.table,
            "column"    : column,
            "id"        : id
        }
        try {
            const response = await fetch('/deleteRows', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(requestObject)
            });
            if (!response.ok){
                throw new Error(`Server responded with status ${response.status}`);
            }
            const data = await response.json();
            console.log("Response from server:", data);
            return data;
        } catch (error){
            console.error('Error deleting row:', error);
        }
    }
    getTableName(query){
        let parts = query.trim().split(/\s+/).map(word => word.toLowerCase());
        for(let part = 0; part < parts.length; part++){
            if (parts[part] == "from") {
                return parts[part + 1];
            }
        }
        return null;
    }
    async contact(name, email, content){
        const requestObject = {
            "name"  : name,
            "email" : email,
            "content": content
        };
        try {
            const response = await fetch('/questionForm', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(requestObject)
            });
            if (!response.ok){
                throw new Error(`Server status ${response.status}`);
            }
            const data = await response.json();
            console.log("Response from server:", data);
            
        } catch (error){
            console.error("Error:", error);
            
        }
    }
}