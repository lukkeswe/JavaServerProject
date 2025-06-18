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
            $conn = new PDO("mysql:host=localhost;dbname=webserver", "lukas", "Tvt!77@ren");
            $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $sql = "SELECT * FROM users WHERE email = :email";
            $stmt = $conn->prepare($sql);
            $stmt->bindValue(":email", $this->email, PDO::PARAM_STR);
            $stmt->execute();
            
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($row["password"] == $this->password) {
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
}
?>