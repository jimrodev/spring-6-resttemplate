package guru.springframework.spring6resttemplate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6resttemplate.config.ConfigProperties;
import guru.springframework.spring6resttemplate.config.OAuthClientInterceptor;
import guru.springframework.spring6resttemplate.config.RestTemplateBuilderConfig;
import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import guru.springframework.spring6resttemplate.model.RestPageImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Created by jt, Spring Framework Guru.
 */
// Rest Template splace
@RestClientTest                             // (BeerClientImpl.class)
@Import(RestTemplateBuilderConfig.class)    // @Import({ConfigProperties.class, RestTemplateBuilderConfig.class})  // VER PORQUE NO ES CAPAZ DE CARGAR LA DEPENENCIA DE ConfigProperties
public class BeerClientMockTest {

    // PODIAMOS: Poner publicas las variables en la clase BeerClientImpl y obtenerlas desde allí
    static final String BASE_PATH       = "http://localhost";
    static final String PORT            = "8080";
    static final String SERVICE_PATH    = "/api/v1/beer";
    static final String ID_PATH         = "/{id}";

    final String URL = BASE_PATH + ":" + PORT + SERVICE_PATH;

    // V248 - Mock Oauth 2.0 Manager
    static final String REGISTRATION_ID = "springauth";
    static final String BEARER_TEST = "Bearer test";

    // V208
    // @Autowired
    BeerClient beerClient;

    // V208
    // @Autowired
    MockRestServiceServer server;

    // V208
    @Autowired
    RestTemplateBuilder restTemplateBuilderConfigured;

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    RestTemplateBuilder mockRestTemplateBuilder = new RestTemplateBuilder(new MockServerRestTemplateCustomizer());

    BeerDTO beer;
    String responseBody;

    // V248 - Mock Oauth 2.0 Manager
    // No queremos usar la implementación real que llama al * Authorization Service * por lo que moqueamos OAuth2AuthorizedClientManager
    @MockBean
    OAuth2AuthorizedClientManager manager;

    // V248
    // Aquí Srping Boot Test, El * Test Context * coge esto como configuración y los bean los pone en el contexto de la prueba
    // @Import(RestTemplateBuilderConfig.class), Como tenemos anotada la clase con @Import(RestTemplateBuilderConfig.class)
    // Tenemos que proporcionar las dependencias que esta clase tiene
    // 1 - ClientRegistrationRepository
    // 2 - OAuth2AuthorizedClientService
    // 3 - Creamos unas instancia del Interceptor
    @TestConfiguration
    public static class TestConfig{

        @Bean
        ClientRegistrationRepository clientRegistrationRepository(){
            return new InMemoryClientRegistrationRepository(ClientRegistration
                    .withRegistrationId("springauth")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientId("test")
                    .tokenUri("test")
                    .build());
        }

        @Bean
        OAuth2AuthorizedClientService auth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository){
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }

        @Bean
        OAuthClientInterceptor oAuthClientInterceptor(OAuth2AuthorizedClientManager manager,
                                                      ClientRegistrationRepository clientRegistrationRepository){
            return new OAuthClientInterceptor(manager, clientRegistrationRepository);
        }
    }

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // V248
        ClientRegistration clientRegistration = clientRegistrationRepository
                .findByRegistrationId(REGISTRATION_ID);

        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test", Instant.MIN, Instant.MAX);

        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(clientRegistration,
                "test", token));

        // V208
        // Cargamos el * base path *
        // REDUNDANTE PORQUE YA LO COGEMOS DEL @Autowired -> RestTemplateBuilder restTemplateBuilderConfigured;
//        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory( BASE_PATH + ":" + PORT);
//        restTemplateBuilderConfigured.uriTemplateHandler(uriBuilderFactory);

        // Obtenemos el restTemplate desde la configuración por defecto de Spring, mas la configuración del application.yml por defecto
        RestTemplate restTemplate = restTemplateBuilderConfigured.build();

        // BIND TO MOCK SERVER. Cogemos el RestTemplate y lo unimos al servidor MOCK, MockRestServiceServer
        server = MockRestServiceServer.bindTo(restTemplate).build();
        // Configuaramos el comportamiento del RestTemplateBuilder para que devuelva el RestTemplate enlazado al servidor mock
        when(mockRestTemplateBuilder.build()).thenReturn(restTemplate);
        // Inicializamos el Client para que se inicialize con el RestTemplateBuilder que acabamos de configurar.
        beerClient = new BeerClientImpl(mockRestTemplateBuilder, BASE_PATH, PORT, SERVICE_PATH, ID_PATH);

        beer = getBeerDto();
        responseBody = objectMapper.writeValueAsString(beer);
    }

    // V207
    // Si ejecutamos así el test nos devuelve
    // java.lang.IllegalStateException: Unable to use auto-configured MockRestServiceServer since MockServerRestTemplateCustomizer has not been bound to a RestTemplate
    // PROBLEMA:
    // Para crear el RestTemplate estamos usando un RestTemplateBuilder, en la clase RestTemplateBuilderConfig
    // Cuando creamos el RestTemplate este no está asociado al Server que está siendo mockeado
    // @Autowired
    // MockRestServiceServer server
    // 208
    // SOLUCIÓN:
    //
    @Test
    void testList() throws JsonProcessingException {
        //final String URL = BASE_PATH + ":" + PORT + SERVICE_PATH;

        String payload = objectMapper.writeValueAsString(getPage());

        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                // V248 - Tenemos que modificar .anExpect para que valide ahora el * Token MOCK *
                // java.lang.AssertionError: Request header [Authorization] expected:<Basic dXNlcjE6cGFzc3dvcmQ=> but was:<Bearer test>
                // Expected :Basic dXNlcjE6cGFzc3dvcmQ=
                // Actual   :Bearer test
                // .andExpect(header("Authorization","Basic dXNlcjE6cGFzc3dvcmQ="))
                .andExpect(header("Authorization",BEARER_TEST))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Page<BeerDTO> dtos = beerClient.list(null, null, null, null, null);
        assertThat(dtos.getContent().size()).isGreaterThan(0);
    }

    private void mockGetOperation(){
        //final String URL = BASE_PATH + ":" + PORT + SERVICE_PATH;

        server.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(URL + ID_PATH, beer.getId()))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                // V248 - Tenemos que modificar .anExpect para que valide ahora el * Token MOCK *
                // java.lang.AssertionError: Request header [Authorization] expected:<Basic dXNlcjE6cGFzc3dvcmQ=> but was:<Bearer test>
                // Expected :Basic dXNlcjE6cGFzc3dvcmQ=
                // Actual   :Bearer test
                // .andExpect(header("Authorization","Basic dXNlcjE6cGFzc3dvcmQ="))
                .andExpect(header("Authorization",BEARER_TEST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    @Test
    void testCreate() {
        //final String URL = BASE_PATH + ":" + PORT + SERVICE_PATH;

        URI uri = UriComponentsBuilder.fromPath(SERVICE_PATH + ID_PATH).build(beer.getId());

        // AQUI: Configuramos el comportamiento del Mock Server para las dos peticiones que hacemos en el métod Create del client
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                // V248 - Tenemos que modificar .anExpect para que valide ahora el * Token MOCK *
                // java.lang.AssertionError: Request header [Authorization] expected:<Basic dXNlcjE6cGFzc3dvcmQ=> but was:<Bearer test>
                // Expected :Basic dXNlcjE6cGFzc3dvcmQ=
                // Actual   :Bearer test
                // .andExpect(header("Authorization","Basic dXNlcjE6cGFzc3dvcmQ="))
                .andExpect(header("Authorization",BEARER_TEST))
                //.andRespond(withAccepted().location(uri));  // Con esta solo funciona la opción 2 de la implementacion
                // CON ESTA CONFIGURACIÓN FUNCIONAN LAS DOS IMPLEMENTACION DEL METODO Create del Cliente (VER OPCIONES DE IMPLEMENTACIÓN)
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON).location(uri));

        // mockGetOperation();  // PARA EVITAR REPETIR CÓDIGO
        server.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(URL + ID_PATH, beer.getId()))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                // v248
                .andExpect(header("Authorization",BEARER_TEST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        BeerDTO response = beerClient.create(beer);
        assertThat(response.getId()).isEqualTo(beer.getId());
    }

    @Test
    void testGetById() {
        //final String URL = BASE_PATH + ":" + PORT + SERVICE_PATH;

        server.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(URL + ID_PATH, beer.getId()))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                .andExpect(header("Authorization",BEARER_TEST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        BeerDTO response = beerClient.getById(beer.getId());
        assertThat(response.getId()).isEqualTo(beer.getId());
    }

    @Test
    void testUpdate() {

        //final String URL = BASE_PATH + ":" + PORT + SERVICE_PATH;

        server.expect(method(HttpMethod.PUT))
                .andExpect(requestToUriTemplate(URL + ID_PATH, beer.getId()))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                .andExpect(header("Authorization",BEARER_TEST))
                .andRespond(withSuccess());

        // Extraemos el código repetido * OPERACION GET * En el test Create, Update y lo convertimos en una función para ser llamada en los dos métodos
        mockGetOperation();

        BeerDTO response = beerClient.update(beer);
        assertThat(response.getId()).isEqualTo(beer.getId());

    }

    @Test
    void testDelete() {
        // Configuramos el comportamiento del Mock Server
        server.expect(method(HttpMethod.DELETE))
                .andExpect(requestToUriTemplate(URL + ID_PATH, beer.getId()))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                // V248 - Tenemos que modificar .anExpect para que valide ahora el * Token MOCK *
                // java.lang.AssertionError: Request header [Authorization] expected:<Basic dXNlcjE6cGFzc3dvcmQ=> but was:<Bearer test>
                // Expected :Basic dXNlcjE6cGFzc3dvcmQ=
                // Actual   :Bearer test
                // .andExpect(header("Authorization","Basic dXNlcjE6cGFzc3dvcmQ="))
                .andExpect(header("Authorization",BEARER_TEST))
                .andRespond(withSuccess());

        beerClient.delete(beer.getId());

        // Llamamos a verify, para que verfique toda la interación de la operación delete, es decir que se llame al método, con la url, parámetros, etc
        server.verify();
    }

    // V2013
    @Test
    void testDeleteThowsNotFoundException() {
        // Configuramos el comportamiento del Mock Server
        server.expect(method(HttpMethod.DELETE))
                .andExpect(requestToUriTemplate(URL + ID_PATH, beer.getId()))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                .andExpect(header("Authorization",BEARER_TEST))
                .andRespond(withResourceNotFound());

        // Aqui lanza la excepción
        // org.springframework.web.client.HttpClientErrorException$NotFound: 404 Not Found: [no body]
        // SOLUCIÓN: lO metemos dentro de un assertThrows

        assertThrows(HttpClientErrorException.class, () -> {
            beerClient.delete(beer.getId());
        });

        // Llamamos a verify, para que verfique toda la interación de la operación delete, es decir que se llame al método, con la url, parámetros, etc
        server.verify();
    }

    @Test
    void testListWithQueryParam() throws JsonProcessingException {
        String response = objectMapper.writeValueAsString(getPage());

        // CREAR UN MultiValueMap para usar el método .queryParams de UriComponentBuilder
        // UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(URL);
        // MultiValueMap requestParams = new LinkedMultiValueMap<String, String>();
        // ADD PARAMS
        // uriComponentsBuilder.queryParams(requestParams);
        // uriComponentsBuilder.build().toUriString();

        URI uri = UriComponentsBuilder.fromHttpUrl(URL)
                .queryParam("name", "ALE")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .build().toUri();

        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(uri))
                // V224 With Basic Authorization Header
                // Le indicamos al Mock Server que le tiene que llegar la cabecera de serguridad
                .andExpect(header("Authorization",BEARER_TEST))
                .andExpect(queryParam("name", "ALE"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        Page<BeerDTO> responsePage = beerClient.list("ALE", null, null, 1, 1);

        assertThat(responsePage.getContent().size()).isEqualTo(1);
    }

    BeerDTO getBeerDto(){
        return BeerDTO.builder()
                .id(UUID.randomUUID())
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("123245")
                .build();
    }

    RestPageImpl getPage(){
        return new RestPageImpl(Arrays.asList(getBeerDto()), 1, 25, 1);
    }
}
