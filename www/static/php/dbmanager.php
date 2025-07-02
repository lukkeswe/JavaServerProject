<?php
class DBmanager {
    public string $email;
    public string $password;
    public string $username;
    public string $domain;
    public string $phone;

    function __construct(string $email, string $password)
    {
        $this->email = $email;
        $this->password = $password;
    } 
    public function login(){
        try {
            $config = require(__DIR__ . "/db.php");
            $conn = new PDO("mysql:host=localhost;dbname=webserver", $config["user"], $config["password"]);
            $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $sql = "SELECT * FROM users WHERE email = :email";
            $stmt = $conn->prepare($sql);
            $stmt->bindValue(":email", $this->email, PDO::PARAM_STR);
            $stmt->execute();
            
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if (password_verify($this->password, $row["password"])) {
                $this->username = $row["name"];
                $this->domain = $row["domain"];
                $this->phone = $row["phone"];
                return true;
            }
            return false;
        } catch (PDOException $e) {
            echo $sql . "<br>" . $e->getMessage();
        }
        
    }

    public function checkInvite($invite){
        try {
            $config = require(__DIR__ . "/db.php");
            $conn = new PDO("mysql:host=localhost;dbname=webserver", $config["user"], $config["password"]);
            $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $sql = "SELECT * FROM invites WHERE invite = :invite";
            $stmt = $conn->prepare($sql);
            $stmt->bindValue(":invite", $invite, PDO::PARAM_STR);
            $stmt->execute();

            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($row["invite"] == $invite) {
                return true;
            }
            return false;
        } catch (PDOException $e){
            echo $e->getMessage();
            return false;
        }
    }
}
?>