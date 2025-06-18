<?php
class DBmanager {
    public string $email;
    public string $password;

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