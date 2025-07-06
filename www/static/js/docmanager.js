class DocumentManager {
    constructor() {
        this.container  = document.getElementById("container");
        this.main       = document.getElementById("main-content");
        this.table      = document.getElementById("table");
        this.db;
    }
    flexContainer(){
        this.container.style.display = "flex";
    }
    mainContainer(){
        const main = document.createElement("main");
        main.id = "main-content";
        this.container.append(main);
    }
    makeTable(data) {
        this.resetTable();
        if (typeof data[0] === "string") {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.innerHTML = data[0];
            tr.append(td);
            this.table.append(tr);
        } else { 
            // Extract the keys from the rows
            let keys = Object.keys(data[0]);
            let rowCount = data.length;
            const tableHead = document.createElement("thead");
            // Append a filler td element for the upper left corner
            const cornerTd = document.createElement("td");
            // if (keys.length > 1) {
            //     rowCount -= 1;
            //     cornerTd.innerHTML = data[rowCount];
            // }
            tableHead.append(cornerTd);
            for(let col = 0; col < keys.length; col++){
                const td = document.createElement("td");
                td.innerHTML = keys[col];
                tableHead.append(td);
            }
            this.table.append(tableHead);
            for(let row = 0; row < rowCount; row++){
                // Create a table row for each row in the dataset
                const tr = document.createElement("tr");
                // Create a delete button for each row
                const btn = document.createElement("button");
                btn.textContent = "削除";
                btn.addEventListener("click", () =>{
                    // Delete the row from the database
                    this.db.deleteRowById(keys[0], data[row][keys[0]]);
                    // Delete the row from the table
                    this.updateTable(this.table, data[row][keys[0]]);
                });
                const eraseTd = document.createElement("td");
                // Append delete button
                eraseTd.append(btn);
                tr.append(eraseTd);
                for(let col = 0; col < keys.length; col++){
                    // Create a table value for each column in the targeted data row
                    const td = document.createElement("td");
                    td.innerHTML = data[row][keys[col]];
                    // Append the "td" into the table row
                    tr.append(td);
                }
                // Append the table roe into the table
                this.table.append(tr);
            }
        }
        // Append the table into the main container
        this.main.append(this.table);
    }
    updateTable(table, targetId){
        console.log("Entered updateTable");
        
        for(let i = table.rows.length - 1; i >= 0; i--){
            const row = table.rows[i];
            const id = row.cells[1]?.innerHTML.trim();
            console.log("target id:", targetId);
            
            console.log("innerHTML:", id);
            
            if (id === targetId) {
                table.deleteRow(i);
                break;
            }
        }
    }
    showTables(data){
        this.resetTable();
        // Get table key
        let keys = Object.keys(data[0]);
        let key = keys[0];
        let tableHead = document.createElement("thead");
        tableHead.append(document.createElement("td").innerHTML = "Tables");
        this.table.append(tableHead);
        for (let row =  0; row < data.length; row++){
            // Create a table row for each row in the dataset
            const tr = document.createElement("tr");
            // Create a table value for the table
            const td = document.createElement("td");
            // Create a select button for fetching the data from that table
            const btn = document.createElement("button");
            btn.className = "tableBtn";
            btn.innerHTML = data[row][key];
            // Add a function that will fetch data
            btn.addEventListener('click', async () => {
                // Set the DBmanager's table
                this.db.table = data[row][key];
                let result = await db.selectEverything(data[row][key]);
                // Make a table out of the result
                this.makeTable(result);
            });
            td.append(btn);
            tr.append(td);
            this.table.append(tr);
        }
        this.main.append(this.table);
    }
    resetTable(){
        this.table = document.getElementById("table");
        if (!this.table) {
            this.table = document.createElement("table");
            this.table.id = "table";
        } else {
            this.table.innerHTML = "";
        }
    }
    showUserDash(){
        // Create elements
        const dashBoard = document.createElement("table");
        const domain    = document.createElement("td");
        const row      = document.createElement("tr");
        // Asign ids
        dashBoard.id    = "dashBoard";
        domain.id       = "domain";
        // Add content text
        domain.innerHTML    = sessionStorage.getItem("domain");
        // Add eventlistener
        domain.addEventListener("click", () => {
            dashBoard.remove();
            this.showDomainInfo();
        });
        // Append elements
        row.append(domain);
        dashBoard.append(row);
        this.main.append(dashBoard);

    }
    showUserInfo(){
        // Create elements
        const backBtn = document.createElement("button");
        const dashBoard = document.createElement("table");
        const username  = document.createElement("td");
        const email     = document.createElement("td");
        const phone     = document.createElement("td");
        const row1      = document.createElement("tr");
        const row2      = document.createElement("tr");
        const row3      = document.createElement("tr");
        // Asign ids
        dashBoard.id    = "dashBoard";
        username.id     = "username";
        email.id        = "email";
        phone.id        = "phone";
        // Add content text
        backBtn.textContent = "戻る";
        username.innerHTML  = sessionStorage.getItem("username");
        email.innerHTML     = sessionStorage.getItem("email");
        phone.innerHTML     = sessionStorage.getItem("phone");
        // Add class name
        backBtn.className = "btn"
        // Add eventlistener
        backBtn.addEventListener("click", () => {
            dashBoard.remove();
            this.showUserDash();
        });
        // Append elements
        row1.append(username);
        row2.append(email);
        row3.append(phone);
        dashBoard.append(backBtn);
        dashBoard.append(row1);
        dashBoard.append(row2);
        dashBoard.append(row3);
        this.main.append(dashBoard);
    }
    showDomainInfo(){
        // Create elements
        const backBtn = document.createElement("button");
        const dashBoard = document.createElement("table");
        const domain  = document.createElement("td");
        const row1      = document.createElement("tr");
        // Asign ids
        dashBoard.id    = "dashBoard";
        domain.id       = "domain";
        // Add content text
        backBtn.textContent = "戻る";
        domain.innerHTML  = sessionStorage.getItem("domain");
        // Add class name
        backBtn.className = "btn";
        // Add eventlistener
        backBtn.addEventListener("click", () => {
            dashBoard.remove();
            this.showUserDash();
        });
        // Append elements
        row1.append(domain);
        dashBoard.append(backBtn);
        dashBoard.append(row1);
        this.main.append(dashBoard);
    }
    burgerMenu(){
        // Create the elements
        const burgerContainer   = document.createElement("aside");
        const burgerBtn         = document.createElement("button");
        const burger            = document.createElement("ul");
        const user              = document.createElement("li");
        const domain            = document.createElement("li");
        const database          = document.createElement("li");
        const file              = document.createElement("li");
        // Add ids
        burgerContainer.id  = "burgerContainer";
        burgerBtn.id        = "burgerBtn";
        burger.id           = "burger";
        user.id             = "userBurger";
        domain.id           = "domainBurger";
        database.id         = "databaseBurger";
        file.id             = "fileBurger";
        // Add text content
        burgerBtn.innerHTML = "🈪";
        user.innerHTML      = "View/edit user";
        domain.innerHTML    = "Domain";
        database.innerHTML  = "Database tool";
        file.innerHTML      = "File manager";
        // Add class name
        burger.classList.add("inactive");
        // Add event listeners
        burgerBtn.addEventListener("click", () => {
            // burger.style.display = "block";
            burger.classList.toggle("active");
            burger.classList.toggle("inactive");
        });
        user.addEventListener("click", ()=> {
            const dash = document.getElementById("dashBoard");
            if (dash) {
                console.log("dash found");
                
                dash.remove();}
            else {console.log("dash not found");
            }
            this.showUserInfo();
        });
        domain.addEventListener("click", () => {
            const dash = document.getElementById("dashBoard");
            if (dash){dash.remove();}
            this.showDomainInfo();
        });
        database.addEventListener("click", () => {
            window.location.href = "databasemanager.php";
        });
        file.addEventListener("click", () => {
            window.location.href = "filemanager.php";
        });
        // Append elements
        burger.append(user);
        burger.append(domain);
        burger.append(database);
        burger.append(file);
        burgerContainer.append(burgerBtn);
        burgerContainer.append(burger);
        this.container.append(burgerContainer);
    }
    userSignUp(){
        // Create elements
        const form      = document.createElement("div");
        const password  = document.createElement("input");
        const passLabel = document.createElement("label");
        const passCheck = document.createElement("input");
        const checkLabel= document.createElement("label");
        const domain    = document.createElement("input");
        const domLabel  = document.createElement("label");
        const email     = document.createElement("input");
        const emailLabel= document.createElement("label");
        const phone     = document.createElement("input");
        const phoneLabel= document.createElement("label");
        const submit    = document.createElement("button");
        // Add ids
        form.id         = "signUpForm";
        password.id     = "password";
        passCheck.id    = "passCheck";
        domain.id       = "domain";
        email.id        = "email";
        phone.id        = "phone";
        // Set class name
        submit.className = "btn";
        // Set the type for input elements
        password.type   = "password";
        passCheck.type  = "password";
        domain.type     = "text";
        email.type      = "text";
        phone.type      = "text";
        // Content text
        passLabel.textContent   = "パスワード：";
        checkLabel.textContent  = "パスワード（確認用）：";
        domLabel.textContent    = "ドメイン：";
        emailLabel.textContent  = "メール：";
        phoneLabel.textContent  = "電話番号：";
        submit.innerHTML = "送信";
        // Add eventlistener
        submit.addEventListener("click", async () => {
            if(password.value == passCheck.value){
                const invite = document.getElementById("invite");
                let requestObject = {
                "password"  : password.value,
                "domain"    : domain.value,
                "email"     : email.value,
                "phone"     : phone.value,
                "invite"    : invite.value
                };
                try {
                    const response = await fetch('/newuser', {
                        method  : 'POST',
                        headers : {'Content-Type': 'application/json'},
                        body    : JSON.stringify(requestObject)
                    });
                    if (!response.ok){
                        throw new Error(`Server status ${response.status}`);
                    }
                    const data = await response.json();
                    console.log("Response:", data);
                    if (data[0]["status"] == "ok") {
                        let requestObject = {
                            "email" : email.value,
                            "password" : password.value
                        };
                        try {
                            const response = await fetch('/login', {
                                method: 'Post',
                                headers: {'Content-Type': 'application/json'},
                                body : JSON.stringify(requestObject)
                            });
                            const data = await response.json();
                            console.log("Response:", data);
                            
                            if (data[0]["status"] == "failed"){
                                alert("Failed to log in :(");
                            } else if(data[0]["id"]){
                                document.cookie = `email=${email.value}; path=/; max-age=86400`;
                                document.cookie= `password=${password.value}; path=/; max-age=86400`;
                                console.log(email.value + " logged in.");
                                setTimeout(() => {window.location.href = "/home.php";}, 100);
                            }
                        } catch (error){
                            console.error("Error", error);
                        }
                    }
                } catch (error){
                    console.error("Error: ", error);
                    alert("Error: ", error);
                }
            } else {
                alert("Passwords don't match");
            }
        });
        this.container.innerHTML = "";
        form.append(emailLabel);
        form.append(email);
        form.append(passLabel);
        form.append(password);
        form.append(checkLabel);
        form.append(passCheck);
        form.append(domLabel);
        form.append(domain);
        form.append(phoneLabel);
        form.append(phone);
        form.append(submit);
        this.container.append(form);
    }
    dataBurgerMenu(){
        // Create the elements
        const burgerContainer   = document.createElement("aside");
        const burgerBtn         = document.createElement("button");
        const burger            = document.createElement("ul");
        const table             = document.createElement("li");
        // Add ids
        burgerContainer.id  = "burgerContainer";
        burgerBtn.id        = "burgerBtn";
        burger.id           = "burger";
        table.id             = "userBurger";
        // Add text content
        burgerBtn.innerHTML = "🈪";
        table.innerHTML      = "Create table";
        // Add class name
        burger.classList.add("inactive");
        // Add event listeners
        burgerBtn.addEventListener("click", () => {
            // burger.style.display = "block";
            burger.classList.toggle("active");
            burger.classList.toggle("inactive");
        });
        table.addEventListener("click", () => {
            this.showNewTableOptions();
        });
        // Append elements
        burger.append(table);
        burgerContainer.append(burgerBtn);
        burgerContainer.append(burger);
        this.container.append(burgerContainer);
    }
    showNewTableOptions(){
        // Remove the table
        this.table.remove();
        // Create the emlements
        const editContainer     = document.createElement("div");
        const tabelNameLabel    = document.createElement("label");
        const nameInput         = document.createElement("input");
        const columnAdder       = document.createElement("button");
        const columnContainer   = document.createElement("table");
        const createBtn         = document.createElement("button");
        // Add ids
        editContainer.id        = "editContainer";
        nameInput.id            = "nameInput";
        columnAdder.id          = "columnAdder";
        columnContainer.id      = "columnContainer";
        // Add text-content
        tabelNameLabel.textContent  = "Table name";
        columnAdder.textContent     = "Add +";
        createBtn.textContent       = "Run";
        // Set type for inputs
        nameInput.type      = "text";
        // Add eventlisteners
        columnAdder.addEventListener("click", ()=> {
            // Create the elements for the column options
            const columnRow         = document.createElement("tr");
            const columnNameTd      = document.createElement("td");
            const columnNameLabel   = document.createElement("label");
            const columnName        = document.createElement("input");
            const columnTypeTd      = document.createElement("td");
            const columnTypeLabel   = document.createElement("label");
            const columnType        = document.createElement("select");
            // Add class names
            columnName.classList.add("columnName");
            columnType.classList.add("columnType");
            // Set the type for the inputs
            columnName.type     = "text";
            // Create the options for the type select
            const varcharOption       = document.createElement("option");
            const intOption           = document.createElement("option");
            // Set values
            varcharOption.value       = "VARCHAR";
            intOption.value           = "INT";
            // Add text-content
            varcharOption.textContent = "VARCHAR";
            intOption.textContent     = "INT";
            // Append into the select element
            columnType.append(varcharOption);
            columnType.append(intOption);
            // Append the elements
            columnNameTd.append(columnNameLabel);
            columnNameTd.append(columnName);
            columnTypeTd.append(columnTypeLabel);
            columnTypeTd.append(columnType);
            columnRow.append(columnNameTd);
            columnRow.append(columnTypeTd);
            columnContainer.append(columnRow);
        });
        createBtn.addEventListener("click", async ()=> {
            let requestObject = {
                "username"  : sessionStorage.getItem("username"),
                "password"  : sessionStorage.getItem("password"),
                "table"     : nameInput.value
            };
            // Select all input elements
            const columnNameElements = document.querySelectorAll(".columnName");
            const columnTypeElements = document.querySelectorAll(".columnType");
            // Put the values of the inputs into array lists
            const columnNames = Array.from(columnNameElements).map(element => element.value);
            const columnTypes = Array.from(columnTypeElements).map(element => element.value);
            // Take the values and put them into the request object
            for(let i = 0; i < columnNames.length; i++){
                requestObject[columnNames[i]] = columnTypes[i];
            }
            try {
                const response = await fetch('/createTable', {
                    method  : 'POST',
                    headers : {'Content-Type': 'application/json'},
                    body    : JSON.stringify(requestObject)
                });
                if (!response.ok) {
                    throw new Error(`Server status ${response.status}`);
                }
                const data = await response.json();
                console.log("Server response:", data);
                if (data[0]["status"] == "ok"){
                    alert(`Created ${nameInput.value} successfully!`);
                    nameInput.value = "";
                    columnContainer.innerHTML = "";
                } else {
                    alert("Could not create the table :(");
                }
            } catch (error){
                console.error(error);
            }
            
        });
        // Append elements
        editContainer.append(tabelNameLabel);
        editContainer.append(nameInput);
        editContainer.append(columnAdder);
        editContainer.append(columnContainer);
        editContainer.append(createBtn);
        this.main.append(editContainer);
    }
    async getFiles(user){
        console.log("Fetching files...");
        
        let requestObject = {"user" : user};
        try {
            const response = await fetch("/listAllFiles", {
                method  : 'POST',
                headers : {"Content-Type": "application/json"},
                body    : JSON.stringify(requestObject)
            });
            if (!response.ok){
                throw new Error(`Server responded with status ${response.status}`);
            }
            const data = await response.json();
            console.log("Response from server:", data);
            
            const filesContainer    = document.getElementById("filesContainer");
            filesContainer.innerHTML = "";

            const fileTypes = ["html", "css", "img"];
            for (let type of fileTypes){
                const ul    = document.createElement("ul");
                ul.id       = type + "List";
                let list = [];
                for (let file = 0; file < data[0][type].length; file++){
                    const fileObject  = document.createElement("li");
                    
                    const name      = document.createElement("span");
                    name.className  = "fileName";
                    name.innerHTML  = data[0][type][file];
                    fileObject.append(name);

                    const erase     = document.createElement("button");
                    erase.innerHTML = "X";
                    erase.className = "btn";
                    erase.addEventListener("click", () => {

                    });
                    fileObject.append(erase);
                    
                    ul.append(fileObject);
                    list.push(data[0][type][file]);
                }
                sessionStorage.setItem(ul.id, list);
                filesContainer.append(ul);
            }

        } catch (error) {
            console.error(error);
        }
    }
}