import java.io.*;
import java.util.*;

public class PhpConfig {
    private static final String CONFIG_FILE = "php.properties";
    public static final Map<String, String> phpMap = new HashMap<>();

    // Load once on startup
    static {
        reload();
    }
    // Reload method
    public static synchronized void reload() {
        phpMap.clear();
        try (InputStream input = new FileInputStream(CONFIG_FILE)){
            Properties prop = new Properties();
            prop.load(input);
            for (String key : prop.stringPropertyNames()){
                phpMap.put(key.toLowerCase(), prop.getProperty(key));
            }
        } catch (IOException e){
            System.out.println("Failed to load domain configuration: " + e.getMessage());
        }
    }
}
