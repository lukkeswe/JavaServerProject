class DocumentManager {
    constructor() {
        this.container  = "container";
        this.table      = "table"; 
    }
    makeTable(data) {
        const dm = new DBmanager();
        const container = document.getElementById(this.container);
        let table       = document.getElementById(this.table);
        if (!table) {
            table = document.createElement("table");
            table.id = this.table;
        } else {
            table.innerHTML = "";
        }
        if (typeof data[0] === "string") {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.innerHTML = data[0];
            tr.append(td);
            table.append(tr);
        } else { 
            // Extract the keys from the rows
            let keys = Object.keys(data[0]);
            const tableHead = document.createElement("tr");
            const cornerTd = document.createElement("td");
            tableHead.append(cornerTd);
            for(let col = 0; col < keys.length; col++){
                const td = document.createElement("td");
                td.innerHTML = keys[col];
                tableHead.append(td);
            }
            table.append(tableHead);
            for(let row = 0; row < data.length; row++){
                // Create a table row for each row in the dataset
                const tr = document.createElement("tr");
                const btn = document.createElement("button");
                btn.textContent = "削除";
                console.log(`data[row][\"${keys[0]}\"]: `, data[row][keys[0]]);
                btn.addEventListener("click", function(){dm.deleteRowById("test", keys[0], data[row][keys[0]]);});
                const eraseTd = document.createElement("td");
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
                table.append(tr);
            }
        }
        // Append the table into the container
        container.append(table);
    }
}