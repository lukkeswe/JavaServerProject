import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class DomainConfigUtil {
    private static final String CONFIG_FILE = "domains.properties";

    public static synchronized boolean addDomainMapping(String domain, String path) {
        Properties props = loadProperties();

        if (props == null) return false;

        if(props.containsKey(domain.toLowerCase())){
            System.out.println("Domain already exists.");
            return false;
        }

        props.setProperty(domain.toLowerCase(), path);

        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)){
            props.store(output, "Updated domain mappings");
            System.out.println("Domain added successfully.");
        } catch (IOException e){
            System.err.println("Failed to write to config file: " + e.getMessage());
            return false;
        }
        // Reload in-memory map
        DomainsConfig.reload();
        return true;
    }

    public static synchronized boolean updateDomainMapping(String domain, String newPath){
        Properties props = loadProperties();

        if (props == null) return false;

        if (!props.containsKey(domain.toLowerCase())){
            System.out.println("Domain not found.");
            return false;
        }

        props.setProperty(domain.toLowerCase(), newPath);
        return saveProperties(props, "Updated domain path");
    }

    public static synchronized boolean removeDomainMapping(String domain) {
        Properties props = loadProperties();
        
        if (props == null) return false;

        if (!props.containsKey(domain.toLowerCase())){
            System.out.println("Domain not found.");
            return false;
        }

        props.remove(domain.toLowerCase());
        return saveProperties(props, "Removed domain");
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
