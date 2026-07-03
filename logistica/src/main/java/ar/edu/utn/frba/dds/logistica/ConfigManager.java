package ar.edu.utn.frba.dds.logistica;

import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Atencion: application.properties no encontrado, usando valores por defecto.");
            } else {
                properties.load(input);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String envVar = System.getenv(key.replace(".", "_").toUpperCase());
        if (envVar != null && !envVar.isEmpty()) {
            return envVar;
        }
        return properties.getProperty(key, defaultValue);
    }
}
