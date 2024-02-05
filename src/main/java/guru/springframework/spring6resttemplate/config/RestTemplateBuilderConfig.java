package guru.springframework.spring6resttemplate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class RestTemplateBuilderConfig {

    // OPCIÓN 1 - CONSTANTES
    // public static final String BASE_PATH = "http://Localhost:8080";
    // public static final String BEER_PATH = "/api/v1/beer";

    // OPCIÓN 2 - CARGAR DESDE EL FICHERO DE CONFIGURACIÓN
     @Value("${services.beer.base-path}")
     private String BASE_PATH;
     @Value("${services.beer.port}")
     private String PORT;

    // OPCIÓN 3 - PRIVATE FINAL DESDE EL FICHERO DE CONFIGURACION
    // Se asignan en el constructor, en este caso indicamos el * BIND * en cada parámetro a la * CONFIGURACION *

//    public RestTemplateBuilderConfig(
//            @Value("${services.beer.base-path}") String BASE_PATH,
//            @Value("${services.beer.port}") String PORT)
//    {
//        this.BASE_PATH = BASE_PATH;
//        this.PORT = PORT;
//    }

    // OPCION 4 - PRIVATE FINAL DESDE EL FICHERO DE CONFIGURACION
    // Se asignan en el constructor, en este caso el bind lo hacemos con la anotación @ConfigurationProperties from org.springframework.boot.context.properties.ConfigurationProperties
    // https://www.baeldung.com/configuration-properties-in-spring-boot
    // CREAMOS UNA CLASE ConfigProperties y la injectamos en el constructor

//    private final String BASE_PATH;
//    private final String PORT;
//
//    public RestTemplateBuilderConfig(ConfigProperties configProperties)
//    {
//        this.BASE_PATH = configProperties.getBasePath();
//        this.PORT = configProperties.getPort();
//    }

    @Bean
    RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer){

        // Inicializa el RestTemplateBuilder con la configuración por defecto de Spring Boot (Nos da de caja toda la configuración)
        RestTemplateBuilder builder = configurer.configure((new RestTemplateBuilder()));

        // Cargamos el * base path *
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory( BASE_PATH + ":" + PORT);

        return builder.uriTemplateHandler(uriBuilderFactory);
    }
}
