package guru.springframework.spring6resttemplate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
//     @Value("${rest.template.username}")
//     private String USERNAME;
//     @Value("${rest.template.password}")
//     private String PASSWORD;


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

    // V247 - No hace aqui la inyección de dependencias, lo pasa al método oAuth2AuthorizedClientManager
    // Porque sólo lo usa en ese método
//    private final ClientRegistrationRepository clientRegistrationRepository;
//    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
//
//    public RestTemplateBuilderConfig(ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
//        this.clientRegistrationRepository = clientRegistrationRepository;
//        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
//    }

    // V245
    // La configuración de seguridad OAuth 2.0 que hemos establecido en las propiedades se cargan automáticamente
    // En estos componentes

    // Estos componentes se usarán en conjunción con RestTemplateBuilder
    // IMPORTANTE:
    // Si no tenemos un Token de autenticación en el Contexto (cacheado), usará la credenciales establecidas en las propiedades
    // para obtener un Token válido dese el servidor de Autorización que también hemos configurado en las propiedades
    // TODOS ESTOS COMPONENTES LOS PROPORCIONA LA DEPENDENCIA spring-boot-starter-oauth2-client
    @Bean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService oAuth2AuthorizedClientService
    ){
        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        // VER QUE CONFIGURACIÓN COGEN ESTOS DOS COMPONENTES DEL application.properties
        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                oAuth2AuthorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    RestTemplateBuilder restTemplateBuilder(
            RestTemplateBuilderConfigurer configurer,
            OAuthClientInterceptor interceptor)
    {

        assert BASE_PATH != null;

        // Inicializa el RestTemplateBuilder con la configuración por defecto de Spring Boot (Nos da de caja toda la configuración)
        RestTemplateBuilder builder = configurer.configure((new RestTemplateBuilder()));

        // Cargamos el * base path *
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory( BASE_PATH + ":" + PORT);

        // V222
        // return builder.uriTemplateHandler(uriBuilderFactory);
        // IMPORTANTE: EL ESTABLECER LA PROPIEDAD builder.basicAuthentication(USERNAME, PASSWORD) devuelve una * nueva instancia * que hay que recoger
        // builderWithAuth, sino no aplica las propiedades, es un error común pensar que por establecer la propiedad se configura
        // V244 - Quitamos Basic Authentication para poner Oauth 2.0
        // RestTemplateBuilder builderWithAuth = builder.basicAuthentication(USERNAME, PASSWORD);
        // return builderWithAuth.uriTemplateHandler(uriBuilderFactory);

        // V223
        // En un solo paso
//        return configurer.configure(new RestTemplateBuilder())
//                .basicAuthentication(USERNAME, PASSWORD)
//                .uriTemplateHandler(new DefaultUriBuilderFactory(BASE_PATH + ":" + PORT));

        // V247 -- ADD INTERCEPTOR TO RestTemplateBuilder
        // TODO - RestTemplate que se contruya con este RestTemplateBuilder tendra este inteceptor que nos genera el Token JWT y lo añade a la peticíón
        return configurer.configure(new RestTemplateBuilder())
                .additionalInterceptors(interceptor)  //V247 - Add ClientHttpRequestInterceptor
                .uriTemplateHandler(new DefaultUriBuilderFactory(BASE_PATH + ":" + PORT));

    }
}
