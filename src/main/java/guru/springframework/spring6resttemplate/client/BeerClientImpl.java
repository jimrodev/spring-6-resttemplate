package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import guru.springframework.spring6resttemplate.model.RestPageImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Configuration
// @RequiredArgsConstructor
@Service
public class BeerClientImpl implements BeerClient {

    // OPCIÓN 1 - CONSTANTES
    // public static final String BASE_PATH = "http://Localhost:8080";
    // public static final String BEER_PATH = "/api/v1/beer";
    // public static final String BEER_PATH_ID = BEER_PATH + "/{beerId}" ;

    // OPCIÓN 2 - CARGAR DESDE EL FICHERO DE CONFIGURACIÓN
    // @Value("${services.beer.base-path}")
    // private String BASE_PATH;
    // @Value("${services.beer.port}")
    // private String PORT;
    // @Value("${services.beer.service-path}")
    // private String BEER_PATH;
    // @Value("${services.beer.id-path}")
    // private String ID_PATH;

    // OPCIÓN 3 - PRIVATE FINAL DESDE EL FICHERO DE CONFIGURACION -> Se asignan en el constructor
    private final String BASE_PATH;
    private final String PORT;
    private final String SERVICE_PATH;
    private final String ID_PATH;

    // V193 - Utilizamos RestTemplateBuider porque con proporciona un rest template builder
    // QUE ES CONFIGURADO con una * configuración por defecto * de la misma forma que lo hace spring boot
    private final RestTemplateBuilder restTemplateBuilder;

    // OPCIÓN 3
    public BeerClientImpl(RestTemplateBuilder restTemplateBuilder,
                          @Value("${services.beer.base-path}") String BASE_PATH,
                          @Value("${services.beer.port}") String PORT,
                          @Value("${services.beer.service-path}") String SERVICE_PATH,
                          @Value("${services.beer.id-path}") String ID_PATH) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.BASE_PATH = BASE_PATH;
        this.PORT = PORT;
        this.SERVICE_PATH = SERVICE_PATH;
        this.ID_PATH = ID_PATH;
    }

    @Override
    public Page<BeerDTO> list(String name,
                              BeerStyle style,
                              Boolean showInventory,
                              Integer pageNumber,
                              Integer pageSize) {

        final String URI = BASE_PATH + ":" + PORT + SERVICE_PATH;

        RestTemplate restTemplate = restTemplateBuilder.build();

        // 1 - Get the response as string
//        ResponseEntity<String> stringResponse = restTemplate.getForEntity(URI, String.class);
//
//        System.out.println(stringResponse.getBody());

        // 2 - Get the respose as map
        // AQUÍ Spring invoca a Jackson para crear el objeto Map desde el Json de la respuesta
//        ResponseEntity<Map> mapResponse = restTemplate.getForEntity(URI, Map.class);
//
//        System.out.println(mapResponse);

        // 3 - Get the respose as JsonNode
        // AQUÍ Spring invoca a Jackson para deserializar la respuesta en un objeto JsonNode
        // JsonNode es la respuesta Json Genérica de Jackson
        // V195
//        ResponseEntity<JsonNode> jsonResponse = restTemplate.getForEntity(URI, JsonNode.class);
//
//        // Imprimimos el Json del body
//        System.out.println(jsonResponse.getBody());
//
//        // Imprimimos una lista de beer names
//        jsonResponse.getBody().findPath("content")
//                .elements().forEachRemaining(node -> {
//                    System.out.println(node.get("beerName").asText());
//                });

        // Al intentar indicarle a Jackson que haga bind de la respuesta al objeto * Page *
        // ERROR:
        // Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Cannot construct instance of `org.springframework.data.domain.Page` (no Creators, like default constructor, exist):
        // abstract types either need to be mapped to concrete types, have custom deserializer, or contain additional type information
        // ResponseEntity<Page> pageResponse =
        //        restTemplate.getForEntity(URI, Page.class);

        // SOLUCIÓN:
        // Page es una Interfaz y por eso Jackson no es capas de deserializar, tampoco PageImpl
        // Creamos una clase RestPageImpl que extienda PageImpl y así Jackson tendrá toda la información necestaria para poder deserializar
        // Para poder usar generics RestPageImpl<T>... hay que usar RestTemplate.exchange(...,...,...,new ParameterizedTypeReference<RestPageImpl<BeerDTO>>() {});
        // En el siguiente link aparecen otras opciones como devolver una Lista de objetos, etc
        // https://www.baeldung.com/spring-resttemplate-json-list

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(SERVICE_PATH);

        // V200
        //MultiValueMap requestParams = new LinkedMultiValueMap<String, String>();
        MultiValueMap requestParams = new LinkedMultiValueMap<String, String>();
        if(StringUtils.hasText(name))   { requestParams.add("name", name);}
        if(style != null)               { requestParams.add("style", style);}
        if(showInventory != null)       { requestParams.add("showInventory", showInventory);}
        if(pageNumber != null)          { requestParams.add("pageNumber", pageNumber);}
        if(pageSize != null)            { requestParams.add("pageSize", pageSize);}

        uriComponentsBuilder.queryParams(requestParams);

        ResponseEntity<RestPageImpl<BeerDTO>> pageResponse =
                // V197
                // Configurar la configuración por defecto de RestTemplateBuilder: config/RestTemplateBuilderConfig en injectarlo como dependencia
                // HEMOS AÑADIDO EL BASE PATH por lo que en la llamada .exchange sólo tenemos que pasarlel el resto de la uri BEER_PATH, en ver de la URI completa
                // restTemplate.exchange(URI, HttpMethod.GET, null, new ParameterizedTypeReference<RestPageImpl<BeerDTO>>() {});

                // V199 UriComponentBuilder
                // restTemplate.exchange(BEER_PATH, HttpMethod.GET, null, new ParameterizedTypeReference<RestPageImpl<BeerDTO>>() {});
                // restTemplate.exchange(uriComponentsBuilder.toUriString(), HttpMethod.GET, null, new ParameterizedTypeReference<RestPageImpl<BeerDTO>>() {});

                // 200 AÑADIR LOS PARÁMETROS A LA PETICIÓN
                // SI NO HACEMOS EL .build() antes del toUriString() en uriComponentsBuilder obtenemos un error de cast entre Integer y String
                restTemplate.exchange(uriComponentsBuilder.build().toUriString(), HttpMethod.GET, null, new ParameterizedTypeReference<RestPageImpl<BeerDTO>>() {});

        List<BeerDTO> listBeers = pageResponse.getBody().toList();

        return pageResponse.getBody();
    }

    @Override
    public BeerDTO getById(UUID id){

        String URI = SERVICE_PATH + ID_PATH;

        // Esto nos da la configuración por defecto
        RestTemplate restTemplate = restTemplateBuilder.build();

        return restTemplate.getForObject(URI, BeerDTO.class, id);
    }

    @Override
    public BeerDTO create(BeerDTO beer) {

        String URI = SERVICE_PATH;

        // Esto nos da la configuración por defecto
        RestTemplate restTemplate = restTemplateBuilder.build();

        // Opción 1
        // Aquí recogemos el BeerDTO devuelto por el servicio BeerDTO.class y lo devolvemos en la resupesta de nuestro método cliente
//        ResponseEntity<BeerDTO> response = restTemplate.postForEntity(URI, beer, BeerDTO.class);
//        return response.getBody();

        // V203
        // Opción 2 (RESPONSE - Location Header)
        // 2 PASOS
        //   1 - Vamos a través de la Response Location Header, que es la URI al recurso creado
        //   2 - Devolvemos el recurso accediendo por la URI (Header - Location)
        URI uri = restTemplate.postForLocation(SERVICE_PATH, beer);
        return restTemplate.getForObject(uri.getPath(), BeerDTO.class);
    }

    // V204
    @Override
    public BeerDTO update(BeerDTO beer) {

        String URI = SERVICE_PATH + ID_PATH;

        // Esto nos da la configuración por defecto
        RestTemplate restTemplate = restTemplateBuilder.build();

        restTemplate.put(URI, beer, beer.getId());

        BeerDTO updatedBeer = restTemplate.getForObject(URI, BeerDTO.class, beer.getId());

        // By client Method
        // BeerDTO updatedBeer = getById(beer.getId());

        return updatedBeer;
    }

    @Override
    public void delete(UUID id) {

        String URI = SERVICE_PATH + ID_PATH;

        // Esto nos da la configuración por defecto
        RestTemplate restTemplate = restTemplateBuilder.build();

        restTemplate.delete(URI, id);

    }
}
