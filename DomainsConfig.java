import java.io.*;
import java.util.*;

public class DomainsConfig {
    public static final Map<String, String> domainMap = new HashMap<>();
    
    static {
        try (InputStream input = new FileInputStream("domains.properties")){
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