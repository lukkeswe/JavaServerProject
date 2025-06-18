<?php
class DBmanager {
    public string $username;
    public string $password;

    function __construct(string $username, string $password)
    {
        $this->username = $username;
        $this->password = $password;
    } 
    public function login(){
        try {
            $conn = new PDO("mysql:host=localhost;dbname=webserver", "lukas", "Tvt!77@ren");
            $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $sql = "SELECT * FROM users WHERE name = :name";
            $stmt = $conn->prepare($sql);
            $stmt->bindValue(":name", $this->username, PDO::PARAM_STR);
            $stmt->execute();
            $result = $stmt->fetchAll();
            foreach($result as $row){
                if ($row["password"] == $this->password) {
                    return true;
                }
            }
            return false;
        } catch (PDOException $e) {
            echo $sql . "<br>" . $e->getMessage();
        }
        
    }
}
?>