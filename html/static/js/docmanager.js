class DocumentManager {
    constructor() {
        this.container  = document.getElementById("container");
        this.table      = document.getElementById("table");
        this.db;
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
        // Append the table into the container
        this.container.append(this.table);
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
        this.container.append(this.table);
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
}