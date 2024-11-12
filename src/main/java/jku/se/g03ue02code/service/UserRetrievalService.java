package jku.se.g03ue02code.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service class for retrieving and managing users from Keycloak.
 */
@Service
public class UserRetrievalService extends DefaultOAuth2UserService {

    @Lazy
    @Autowired
    private RestTemplate keycloakRestTemplate;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    /**
     * Retrieves the list of users from Keycloak.
     *
     * @return A list of users who are assigned the "student" or"default-roles-lvaservice" role in Keycloak.
     * @throws RuntimeException if there is an error during user retrieval.
     */
    public List<Map<String, Object>> getUsers() {
        String url = keycloakServerUrl + "admin/realms/" + keycloakRealm + "/users";
        String accessToken = getAccessToken();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map[]> response = keycloakRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map[].class
            );
            List<Map<String, Object>> users = Arrays.asList(response.getBody());

            List<Map<String, Object>> students = new ArrayList<>();

            String clientsUrl = keycloakServerUrl + "admin/realms/" + keycloakRealm + "/clients";
            ResponseEntity<List<Map<String, Object>>> clientsResponse = keycloakRestTemplate.exchange(
                    clientsUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> clients = clientsResponse.getBody();
            String clientId = null;
            for (Map<String, Object> client : clients) {
                if (keycloakClientId.equals(client.get("clientId"))) {
                    clientId = (String) client.get("id");
                    break;
                }
            }

            if (clientId == null) {
                throw new RuntimeException("Client not found");
            }

            for (Map<String, Object> user : users) {
                String userId = (String) user.get("id");

                String realmRolesUrl = keycloakServerUrl + "admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm";
                ResponseEntity<List<Map<String, Object>>> realmRolesResponse = keycloakRestTemplate.exchange(
                        realmRolesUrl,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                List<Map<String, Object>> realmRoles = realmRolesResponse.getBody();

                String clientRolesUrl = keycloakServerUrl + "admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/clients/" + clientId;
                ResponseEntity<List<Map<String, Object>>> clientRolesResponse = keycloakRestTemplate.exchange(
                        clientRolesUrl,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                List<Map<String, Object>> clientRoles = clientRolesResponse.getBody();

                boolean isStudent = (realmRoles != null && realmRoles.stream()
                        .anyMatch(role -> "default-roles-lvaservice".equals(role.get("name")))) ||
                        (clientRoles != null && clientRoles.stream()
                                .anyMatch(role -> "student".equals(role.get("name"))));

                if (isStudent) {
                    students.add(user);
                }
            }
            return students;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error retrieving users from Keycloak", e);
        }
    }

    /**
     * Deletes a user from Keycloak based on their user ID.
     *
     * @param userId The ID of the user to be deleted.
     * @throws RuntimeException if there is an error during user deletion.
     */
    public void deleteUser(String userId) {
        String url = keycloakServerUrl + "admin/realms/" + keycloakRealm + "/users/" + userId;
        String accessToken = getAccessToken();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = keycloakRestTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("User successfully deleted: " + userId);
            } else {
                throw new RuntimeException("Error deleting user. Status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting user from Keycloak", e);
        }
    }

    /**
     * Retrieves the access token for the current authenticated user.
     *
     * @return The access token for the authenticated user.
     * @throws RuntimeException if the access token cannot be retrieved.
     */
    private String getAccessToken() {
        OAuth2AuthenticationToken auth = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                auth.getAuthorizedClientRegistrationId(),
                auth.getName());

        if (authorizedClient == null) {
            throw new RuntimeException("Authorized client not found.");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    /**
     * Loads the OAuth2 user details from the OAuth2 authentication request.
     *
     * @param userRequest The OAuth2 user request.
     * @return An OAuth2User containing the user's authorities and attributes.
     * @throws OAuth2AuthenticationException if there is an error during the user load.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oauth2User.getAttributes();

        Set<GrantedAuthority> authorities = new HashSet<>();

        Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                for (String roleName : roles) {
                    authorities.add(new SimpleGrantedAuthority(roleName));
                }
            }
        }

        return new DefaultOAuth2User(authorities, attributes, "email");
    }
}