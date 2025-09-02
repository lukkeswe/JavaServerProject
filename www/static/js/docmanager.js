class DocumentManager {
    constructor(user) {
        this.container  = document.getElementById("container");
        this.main       = document.getElementById("main-content");
        this.table      = document.getElementById("table");
        this.db;
        this.user       = user;
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
        const row       = document.createElement("tr");
        const a         = document.createElement("a");
        // Asign ids
        dashBoard.id    = "dashBoard";
        domain.id       = "domain";
        // Add content text
        a.innerHTML     = sessionStorage.getItem("domain");
        // Add href
        a.href          = "https://" + sessionStorage.getItem("domain")
        // Add link setting
        a.target        = "_blank";
        // Add eventlistener
        domain.addEventListener("click", () => {
            dashBoard.remove();
            this.showDomainInfo();
        });
        // Append elements
        domain.append(a);
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
        const logout            = document.createElement("li");
        // Add ids
        burgerContainer.id  = "burgerContainer";
        burgerBtn.id        = "burgerBtn";
        burger.id           = "burger";
        user.id             = "userBurger";
        domain.id           = "domainBurger";
        database.id         = "databaseBurger";
        file.id             = "fileBurger";
        logout.id           = "logout";
        // Add text content
        burgerBtn.innerHTML = "🈪";
        user.innerHTML      = "ユーザー情報";
        domain.innerHTML    = "ドメイン";
        database.innerHTML  = "データベース管理";
        file.innerHTML      = "ファイル管理";
        logout.innerHTML    = "ログアウト";
        // Add event listeners
        burgerBtn.addEventListener("click", () => {
            burger.classList.toggle("open");
        });
        user.addEventListener("click", ()=> {
            const dash = document.getElementById("dashBoard");
            if (dash) {
                dash.remove();
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
        logout.addEventListener("click", () => {
            window.location.href = "logout.php";
        });
        // Append elements
        burger.append(user);
        burger.append(domain);
        //burger.append(database); // <- Add this back when the database manager is ready
        burger.append(file);
        burger.append(logout);
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
                                window.location.href = "server.php";
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
                alert("パスワード（確認用）エラー");
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
    fileBurgerMenu(){
        // Create the elements
        const burgerContainer   = document.createElement("aside");
        const burgerBtn         = document.createElement("button");
        const burger            = document.createElement("ul");
        const home              = document.createElement("li");
        const logout            = document.createElement("li");
        // Add ids
        burgerContainer.id  = "burgerContainer";
        burgerBtn.id        = "burgerBtn";
        burger.id           = "burger";
        home.id             = "homeBtn";
        logout.id           = "logout";
        // Add hyper link
        const a     = document.createElement("a");
        a.href      = "home.php";
        // Add text content
        burgerBtn.innerHTML     = "🈪";
        a.innerHTML             = "戻る";
        logout.innerHTML        = "ログアウト"; 
        // Add class name
        burger.classList.add("inactive");
        // Add event listeners
        burgerBtn.addEventListener("click", () => {
            // burger.style.display = "block";
            burger.classList.toggle("active");
            burger.classList.toggle("inactive");
        });
        logout.addEventListener("click", () => {
            window.location.href = "logout.php";
        });
        // Append elements
        home.append(a);
        burger.append(home);
        burger.append(logout);
        burgerContainer.append(burgerBtn);
        burgerContainer.append(burger);
        this.container.insertBefore(burgerContainer, this.main);
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
    async getFiles(user, path = ""){
        console.log("Fetching files...");
        
        let requestObject = {
            "user" : user,
            "path" : path
        };
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
            const pathContainer = document.getElementById("pathContainer");
            const optionsContainer = document.getElementById("optionsContainer");
            const currentPath = document.getElementById("path");
            const backBtn = document.createElement("button");
            let previousPath = "";
            if (currentPath != null && currentPath.textContent != "") {
                backBtn.innerHTML = "↑";
                backBtn.className = "btn";
                backBtn.addEventListener("click", () => {
                    const slice = currentPath.textContent.split("/");
                    console.log("slice: " + slice);
                    
                    for (let i = 0; i < slice.length - 2; i++) {
                        previousPath = previousPath + slice[i] + "/";
                    }
                    console.log("targetPath: " + previousPath);
                    
                    this.getFiles(sessionStorage.getItem("user"), previousPath);
                    currentPath.innerHTML = previousPath;
                    backBtn.remove();
                });
                document.getElementById("displayContainer").innerHTML = "";
                optionsContainer.innerHTML = "";
                optionsContainer.append(backBtn);
            }
            const filesContainer    = document.getElementById("filesContainer");
            filesContainer.innerHTML = "";
            // List of supported file types
            const fileTypes = ["html", "php", "css", "img", "js"];
            for (let type of fileTypes){
                const ul    = document.createElement("ul");
                ul.id       = type + "List";
                let list = [];
                for (let file = 0; file < data[0][type].length; file++){
                    const fileName = data[0][type][file];
                    
                    const fileObject  = document.createElement("li");
                    
                    const name      = document.createElement("span");
                    name.className  = "fileName";
                    name.innerHTML  = fileName;

                    // Create a delete button
                    const erase     = document.createElement("button");
                    erase.innerHTML = "X";
                    erase.className = "btn";
                    // Add an event listener
                    erase.addEventListener("click", async () => {
                        const path = document.getElementById("path");
                        // Call the delete function
                        if (path != null && path.textContent != "") {
                            console.log("Sending path: " + path.textContent);
                            await this.deleteFile(fileName, type, path.textContent);
                        } else {
                            console.log("No path: " + path.textContent);
                            await this.deleteFile(fileName, type);
                        }
                        
                        // Empty the display container
                        this.emptyDisplayContainer();
                        // Remove the element
                        fileObject.remove();
                    });

                    // Create am edit button
                    const edit      = document.createElement("button");
                    edit.className  = "btn";
                    edit.innerHTML  = "edit";
                    // Add eventlistener 
                    edit.addEventListener("click", async () => {
                        // Get the current path
                        const path = document.getElementById("path");
                        // If the path isn't root
                        if (path != null && path.textContent != "") {
                            // Fetch the file content from the server
                            const content = await this.fetchFileContent(fileName, type, path.textContent);
                            // Empty the files container
                            document.getElementById("filesContainer").innerHTML = "";
                            // Initialize the editor with the fetched content
                            if (type == "js") this.showEditor(content, "javascript");
                            else this.showEditor(content, type);
                        // If the path is root
                        } else {
                            const content = await this.fetchFileContent(fileName, type, "");
                            // Empty the files container
                            document.getElementById("filesContainer").innerHTML = "";
                            // Initialize the editor with the fetched content
                            if (type == "js") this.showEditor(content, "javascript");
                            else this.showEditor(content, type);
                        }
                        // Create a back button
                        const backBtn = document.createElement("button");
                        backBtn.className = "btn";
                        backBtn.innerHTML = "↑";
                        // Add an eventlistener
                        backBtn.addEventListener("click", () => {
                            // Get the path
                            const path = document.getElementById("path");
                            // Hide the editor
                            document.getElementById("editor").style.display = "none";
                            // Display the files in the current directory
                            if (path != null && path.textContent != "") this.getFiles(this.user, path.textContent);
                            else this.getFiles(this.user, "");
                            // Remove the back button
                            backBtn.remove();
                        });
                        optionsContainer.append(backBtn);
                    });

                    // If the file is a HTML or PHP file, add a hyper-link to that file
                    if (type == "html" || type == "php"){
                        
                        if (fileName.endsWith("/")){
                            const span = document.createElement("span");
                            span.innerHTML = fileName;
                            span.addEventListener("click", () => {
                                this.emptyDisplayContainer();
                                if (currentPath != null && currentPath.textContent.endsWith("/")) {
                                    this.getFiles(sessionStorage.getItem("user"), currentPath.textContent + fileName);
                                } else {
                                    this.getFiles(sessionStorage.getItem("user"), fileName);
                                }
                                this.appendElementToDisplayContainer(erase);
                                const path = document.createElement("p");
                                path.id = "path";
                                if (currentPath != null && currentPath.textContent.endsWith("/")) {
                                    path.innerHTML = currentPath.textContent + fileName;
                                } else {
                                    path.innerHTML = fileName;
                                }
                                pathContainer.innerHTML = "";
                                pathContainer.append(path);
                            });
                            fileObject.append(span);
                        } else {
                            // Create hyper-link
                            const a = document.createElement("a");
                            // Add a url
                            if (currentPath != null && currentPath.textContent.endsWith("/")) {
                                a.href = "https://" + sessionStorage["domain"] + "/" + currentPath.textContent + fileName;
                            } else {
                                a.href = "https://" + sessionStorage["domain"] + "/" + fileName;
                            }
                            // Add setting
                            a.target = "_blank";
                            // Add event listener
                            a.addEventListener("click", () => {
                                this.emptyDisplayContainer();
                                // Add an edit button
                                optionsContainer.append(edit);
                            });
                            // Append the span
                            a.append(name);
                            // Append the hyper-link
                            fileObject.append(a);
                            // Add eventlistener
                            a.addEventListener("click", () => {
                                this.emptyDisplayContainer();
                                optionsContainer.append(erase);
                                this.showInfo(fileName);
                            });
                        }
                    } else {
                        // Append the name object with the span element
                        fileObject.append(name);
                        // If the file type is of "img" type
                        if (type == "img") {
                            name.addEventListener("click", () => {
                                this.emptyDisplayContainer();
                                this.showInfo(fileName);
                                this.showImage(fileName, "https://" + sessionStorage.getItem("domain") + "/");

                                pathContainer.innerHTML = "";
                                pathContainer.append(currentPath);
                                
                                optionsContainer.innerHTML = "";
                                optionsContainer.append(backBtn);
                                optionsContainer.append(erase);
                            });
                        } else if(type == "css" || type == "js"){
                            // If the file is a css or JavaScript file
                            // Add event listener
                            name.addEventListener("click", () => {
                                this.emptyDisplayContainer();

                                this.showInfo(fileName);
                                optionsContainer.append(edit);
                            });
                        }
                    }
                    ul.append(fileObject);
                    list.push(fileName);
                }
                // Save the list of files in session storage
                sessionStorage.setItem(ul.id, JSON.stringify(list));
                // Append the ul
                filesContainer.append(ul);
            }

        } catch (error) {
            console.error(error);
        }
    }
    // Initialize the editor
    showEditor (content, mode) {
        // Initialize Ace editor
        const editor = ace.edit("editor");
        editor.setTheme("ace/theme/monokai");  // choose theme
        editor.session.setMode("ace/mode/" + mode); // syntax mode
        editor.setValue(content, -1); // Append the content
        document.getElementById("editor").style.display = "block"; // Display the editor
        document.getElementById("optionsContainer").innerHTML = ""; // Empty the options container
    }
    async fetchFileContent (file, type, path = "") {
        let requestObject = {
            "filename"  : file,
            "type"      : type,
            "path"      : path,
            "user"      : this.user
        };
        console.log(`Fetching ${path + file}'s content...`);
        let response = await fetch("/getFileContent", {
            method  : "POST",
            headers : {"Content-Type": "application/json"},
            body    : JSON.stringify(requestObject)
        });

        if (!response.ok){
            throw new Error(`Server responded with ${response.status}`);
        }

        const content = await response.text();
        return content;
    }
    async deleteFile(file, type, path = ""){
        let requsetObject = {
            "filename"  : file,
            "type"      : type,
            "path"      : path,
            "user"      : this.user
        };
        console.log(`Deleting ${path + file}`);
        let response = await fetch("/deleteFile", {
            method  : "POST",
            headers : {"Content-Type": "application/json"},
            body    : JSON.stringify(requsetObject)
        });

        if (!response.ok){
            throw new Error(`Server responded with status ${response.status}`);
        }
        const msg = await response.text();
        alert(msg);
    
        let list = JSON.parse(sessionStorage.getItem(type + "List") || "[]");
        console.log("List before: ", list);
        list = list.filter(item => item !== file);
        sessionStorage.setItem(type + "List", JSON.stringify(list));
        console.log("List after: ", list);
    }
    appendElementToDisplayContainer(element){
        const displayContainer = document.getElementById("displayContainer");
        displayContainer.append(element);
    }
    showImage(filename, domain){
        const display = document.getElementById("displayContainer");
        const img = document.createElement("img");
        const currentPath = document.getElementById("path");
        img.loading = "lazy";
        img.src = domain + "img/" + filename;
        if (currentPath != null && currentPath.textContent != "") {
            img.src = domain + currentPath.textContent + filename;
        }
        display.append(img);
    }
    showInfo(filename){
        if (!filename) return;
        // Get the display element
        const display = document.getElementById("displayContainer");
        // Create the information elements
        const h2 = document.createElement("h2");
        h2.innerHTML = filename;

        display.append(h2);
    }
    emptyDisplayContainer(){
        const displayContainer = document.getElementById("displayContainer");
        displayContainer.innerHTML = "";
    }
    logout(){
        sessionStorage.removeItem("email");
        sessionStorage.removeItem("password");
        sessionStorage.removeItem("domain");
        sessionStorage.removeItem("phone");
        sessionStorage.removeItem("username");
        window.location.href = "logout.php";
    }
    showFileUpload(){
        const btnContainer = document.getElementById("uploadBtnsContainer");
        btnContainer.style.display = "none";
        const fileBtnCon = document.getElementById("fileUploadContainer");
        fileBtnCon.style.display = "block";
        const backBtn = document.getElementById("uploadBackBtn");
        backBtn.style.display = "block";
    }
    showFolderUpload(){
        const btnContainer = document.getElementById("uploadBtnsContainer");
        btnContainer.style.display = "none";
        const fileBtnCon = document.getElementById("folderUploadContainer");
        fileBtnCon.style.display = "block";
        const backBtn = document.getElementById("uploadBackBtn");
        backBtn.style.display = "block";
    }
    uploadBack(){
        const backBtn = document.getElementById("uploadBackBtn");
        backBtn.style.display = "none";
        const fileCon = document.getElementById("fileUploadContainer");
        fileCon.style.display = "none";
        const folderCon = document.getElementById("folderUploadContainer");
        folderCon.style.display = "none";
        const btnCon = document.getElementById("uploadBtnsContainer");
        btnCon.style.display = "block";
    }
}