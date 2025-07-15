import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PhpConfigUtil {
    private static final String CONFIG_FILE = "php.properties";

    public static synchronized boolean addPhpMapping(String domain, String name) {
        Properties props = loadProperties();

        if (props == null) return false;

        if(props.containsKey(domain.toLowerCase())){
            System.out.println("PHP config already exists.");
            return false;
        }

        props.setProperty(domain.toLowerCase(), name);

        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)){
            props.store(output, "Updated PHP mappings");
            System.out.println("PHP config added successfully.");
        } catch (IOException e){
            System.err.println("Failed to write to config file: " + e.getMessage());
            return false;
        }
        // Reload in-memory map
        DomainsConfig.reload();
        return true;
    }

    public static synchronized boolean updatePhpMapping(String domain, String newName){
        Properties props = loadProperties();

        if (props == null) return false;

        if (!props.containsKey(domain.toLowerCase())){
            System.out.println("PHP config not found.");
            return false;
        }

        props.setProperty(domain.toLowerCase(), newName);
        return saveProperties(props, "Updated PHP user");
    }

    public static synchronized boolean removePhpMapping(String domain) {
        Properties props = loadProperties();
        
        if (props == null) return false;

        if (!props.containsKey(domain.toLowerCase())){
            System.out.println("PHP config not found.");
            return false;
        }

        props.remove(domain.toLowerCase());
        return saveProperties(props, "Removed PHP user");
    }

    // === Shared helpers ===

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)){
            props.load(input);
            return props;
        } catch (IOException e){
            System.out.println("Failed to read config file: " + e.getMessage());
            return null;
        }
    }

    private static boolean saveProperties(Properties props, String comment) {
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)){
            props.store(output, comment);
            DomainsConfig.reload();
            System.out.println(comment + " and reloaded config.");
            return true;
        } catch (IOException e) {
            System.err.println("Failed to write config: " + e.getMessage());
            return false;
        }
    }
}
