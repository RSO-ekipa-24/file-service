package essa.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@ApplicationScoped
public class KeycloakAdminProvider {

    @Inject
    @ConfigProperty(name = "quarkus.oidc-client.auth-server-url")
    String serverUrl;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-client.client-id")
    String clientId;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-client.credentials.secret")
    String secret;

    @Inject
    @ConfigProperty(name = "oidc-client.realm")
    String realm;

    public Keycloak getKeycloakClient() {
        String baseUrl = serverUrl.replaceAll("/realms/.*$", "");
        return KeycloakBuilder.builder()
                .serverUrl(baseUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(secret)
                .build();
    }
}
