import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DBConfig {
    public static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("/etc/jlnserver/db.properties")){
            props.load(fis);
        }
        return props;
    }
}
