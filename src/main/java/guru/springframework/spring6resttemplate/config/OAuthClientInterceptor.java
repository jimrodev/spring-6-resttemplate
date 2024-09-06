package guru.springframework.spring6resttemplate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static java.util.Objects.isNull;

/**
 * Created by jt, Spring Framework Guru.
 */
// V246 - Configuramos que el * RestTemplate tenga un interceptor en él * para que inspeccione Si tiene o no * Autorización *
// IMPORTANTE: Si no tiene autorización * se la añadirá *, es decir, obtendrá un Token válido desde es sevidor de Autorización
// Todo esto lo hace en conjunción con el * Authentication Manager *

@Component
public class OAuthClientInterceptor implements ClientHttpRequestInterceptor {

    // @Value("${spring.security.oauth2.client.registration.spring-auth.provider}")
    private final String REGISTRATION_ID = "springauth";

    private final OAuth2AuthorizedClientManager manager;
    private final Authentication principal;
    private final ClientRegistration clientRegistration;


    public OAuthClientInterceptor(OAuth2AuthorizedClientManager manager, ClientRegistrationRepository clientRegistrationRepository) {
        this.manager = manager;
        this.principal = createPrincipal();
        this.clientRegistration = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        OAuth2AuthorizeRequest oAuth2AuthorizeRequest = OAuth2AuthorizeRequest
                // V246 - clientRegistration se configura desde el application.properties spring.security.oauth2.client.registration.springauth...
                .withClientRegistrationId(clientRegistration.getRegistrationId())
                .principal(principal)
                .build();

        // OAuth2AuthorizedClientManager obtiene el Token JWT de servidor de Authorización,
        // En el caso de que no lo tenga ya cacheado, haya expirado, etc
        OAuth2AuthorizedClient client = manager.authorize(oAuth2AuthorizeRequest);

        if (isNull(client)) {
            throw new IllegalStateException("Missing credentials");
        }

        request.getHeaders().add(HttpHeaders.AUTHORIZATION,
                "Bearer " + client.getAccessToken().getTokenValue());

        return execution.execute(request, body);
    }

    // V246 - * principal * que es: es alguien que tiene entidad y que ha sido autorizado en el contexto de spring security
    // Authentication es un * Standard Spring Security Component * que almacena información de seguridad acerca de un * principal *
    private Authentication createPrincipal() {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Collections.emptySet();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return this;
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }

            @Override
            public String getName() {
                return clientRegistration.getClientId();
            }
        };
    }
}
