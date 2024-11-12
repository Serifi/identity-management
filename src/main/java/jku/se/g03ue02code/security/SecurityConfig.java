package jku.se.g03ue02code.security;

import jku.se.g03ue02code.controller.UserController;
import jku.se.g03ue02code.service.UserRetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Security configuration class to set up authentication, authorization, and logout behavior.
 * This class configures OAuth2 login, resource server settings, and role-based access control.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Lazy
    @Autowired
    private UserRetrievalService userRetrievalService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Creates and configures a RestTemplate bean that adds an OAuth2 Bearer token to the Authorization header
     * for outgoing requests.
     *
     * @return A configured RestTemplate with OAuth2 Bearer token interceptor.
     */
    @Bean(name = "keycloakRestTemplate")
    public RestTemplate keycloakRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public org.springframework.http.client.ClientHttpResponse intercept(
                    HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws java.io.IOException {
                String accessToken = getAccessToken();
                if (accessToken != null) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                }
                return execution.execute(request, body);
            }
        });
        return restTemplate;
    }

    /**
     * Retrieves the OAuth2 access token from the currently authenticated user's context.
     *
     * @return The access token as a String, or null if the token is not available.
     */
    private String getAccessToken() {
        OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                authenticationToken.getAuthorizedClientRegistrationId(),
                authenticationToken.getName());
        return (authorizedClient != null) ? authorizedClient.getAccessToken().getTokenValue() : null;
    }

    /**
     * Configures HTTP security settings, including authorization rules, OAuth2 login, and logout handling.
     * Defines access control for different URL patterns and enables OAuth2 resource server configuration.
     *
     * @param http The HttpSecurity object used for configuring the security filter chain.
     * @return A configured SecurityFilterChain object.
     * @throws Exception If there is an issue configuring HTTP security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/submissions/**").authenticated()
                        .requestMatchers("/students").authenticated()
                        .requestMatchers("/user").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                        .jwt(jwtConfigurer -> {})
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/redirectBasedOnRole", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService((userRequest) -> {
                                    return userRetrievalService.loadUser(userRequest);
                                })
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    /**
     * Configures the logout success handler to redirect the user to a specified URL after successful logout.
     *
     * @return A LogoutSuccessHandler that handles redirection after logout.
     */
    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        SimpleUrlLogoutSuccessHandler successHandler = new SimpleUrlLogoutSuccessHandler();
        successHandler.setDefaultTargetUrl("http://localhost:8080/realms/LVAservice/protocol/openid-connect/logout?redirect_uri=http://localhost:8081/");
        return successHandler;
    }
}