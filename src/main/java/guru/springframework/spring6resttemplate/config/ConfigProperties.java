package guru.springframework.spring6resttemplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

// https://www.baeldung.com/configuration-properties-in-spring-boot#immutable-configurationproperties-binding
@ConfigurationProperties(prefix = "services.beer")
// @ConstructorBinding   // ESTO QUE SIGNIFICADO TIENE??
// Respuesta 7 siguiente entrada
// https://stackoverflow.com/questions/65181218/configurationproperties-for-final-fields-doesnt-work
public class ConfigProperties {
    private final String basePath;
    private final String port;

    public ConfigProperties(
            String basePath,
            String port)
    {
        this.basePath = basePath;
        this.port = port;
    }

    public String getBasePath(){
        return basePath;
    }

    public String getPort(){
        return port;
    }
}
