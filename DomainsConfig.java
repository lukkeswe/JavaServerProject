import java.io.*;
import java.util.*;

public class DomainsConfig {
    private static final String CONFIG_FILE = "domains.properties";
    public static final Map<String, String> domainMap = new HashMap<>();

    // Load once on startup
    static {
        reload();
    }
    // Reload method
    public static synchronized void reload() {
        domainMap.clear();
        try (InputStream input = new FileInputStream(CONFIG_FILE)){
            Properties prop = new Properties();
            prop.load(input);
            for (String key : prop.stringPropertyNames()){
                domainMap.put(key.toLowerCase(), prop.getProperty(key));
            }
        } catch (IOException e){
            System.out.println("Failed to load domain configuration: " + e.getMessage());
        }
    }
}