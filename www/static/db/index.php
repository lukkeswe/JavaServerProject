<?php
$config = require("/home/lukas/db.php");

$conn = new PDO(
    "mysql:host=localhost;dbname=webserver;charset=utf8mb4",
    $config["user"],
    $config["password"],
    [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]
);
$tables = $conn->query("SHOW TABLES")->fetchAll(PDO::FETCH_COLUMN);
?>

<?php
function valid_identifier(string $name): bool {
    return preg_match('/^[a-zA-Z0-9_]+$/', $name);
}

if (isset($_POST["create_table"])) {
    $table = $_POST["table_name"];

    if (valid_identifier($table)) {
        $sql = "
            CREATE TABLE `$table` (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ";
        $conn->exec($sql);
    }
}

if (isset($_POST["delete_table"])) {
    $table = $_POST["table_name"];

    if (valid_identifier($table)) {
        $conn->exec("DROP TABLE `$table`");
    }
}
?>

<?php
// INSERT
if (isset($_POST["insert_row"])) {
    $table = $_POST["table"];
    $name  = $_POST["name"];

    if (valid_identifier($table)) {
        $stmt = $conn->prepare("INSERT INTO `$table` (name) VALUES (:name)");
        $stmt->execute([
            ":name" => $name
        ]);
    }
}

// DELETE
if (isset($_POST["delete_row"])) {
    $table = $_POST["table"];
    $id    = (int)$_POST["id"];

    if (valid_identifier($table)) {
        $stmt = $conn->prepare("DELETE FROM `$table` WHERE id = :id");
        $stmt->execute([
            ":id" => $id
        ]);
    }
}
?>

<?php
$results = [];

if (isset($_GET["search"])) {
    $table = $_GET["table"];
    $term  = $_GET["term"] ?? "";
    $order = ($_GET["order"] === "desc") ? "DESC" : "ASC";

    if (valid_identifier($table)) {
        $stmt = $conn->prepare("
            SELECT * FROM `$table`
            WHERE name LIKE :term
            ORDER BY name $order
        ");

        $stmt->execute([
            ":term" => "%$term%"
        ]);

        $results = $stmt->fetchAll();
    }
}
?>

<h2>Table Management</h2>
<form method="post">
    <input name="table_name" placeholder="Table name" required>
    <button name="create_table">Create</button>
    <button name="delete_table">Delete</button>
</form>

<h2>Row Management</h2>
<form method="post">
    <input name="table" placeholder="Table name" required>
    <input name="name" placeholder="Name value" required>
    <button name="insert_row">Insert</button>
</form>

<form method="post">
    <input name="table" placeholder="Table name" required>
    <input name="id" placeholder="Row ID" required>
    <button name="delete_row">Delete</button>
</form>

<h2>Search</h2>
<form method="get">
    <select name="table">
    <?php foreach ($tables as $t): ?>
        <option value="<?= htmlspecialchars($t) ?>">
            <?= htmlspecialchars($t) ?>
        </option>
    <?php endforeach; ?>
    </select>
    <input name="term" placeholder="Search term">
    <select name="order">
        <option value="id">ID</option>
        <option value="asc">A → Z</option>
        <option value="desc">Z → A</option>
    </select>
    <button name="search">Search</button>
</form>

<?php if (!empty($results)): ?>
<table border="1">
    <tr>
        <?php foreach (array_keys($results[0]) as $col): ?>
            <th><?= htmlspecialchars($col) ?></th>
        <?php endforeach; ?>
    </tr>

    <?php foreach ($results as $row): ?>
        <tr>
            <?php foreach ($row as $value): ?>
                <td><?= htmlspecialchars((string)$value) ?></td>
            <?php endforeach; ?>
        </tr>
    <?php endforeach; ?>
</table>
<?php endif; ?>