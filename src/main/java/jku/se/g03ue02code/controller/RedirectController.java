package jku.se.g03ue02code.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

/**
 * Controller that handles redirecting users to different pages based on their roles.
 * The roles are checked from the authentication object, and depending on the role,
 * the user is redirected to specific URLs.
 */
@Controller
public class RedirectController {

    /**
     * Redirects the user to a different URL based on their role.
     *
     * This method checks the user's authorities (roles) from the provided authentication object.
     * - If the user has the "lecturer" role, they will be redirected to "/lecturer/list.html".
     * - If the user has the "student" role, they will be redirected to "/student/list.html".
     * - If the user has none of these roles, they will be redirected to the external login URL.
     *
     * @param authentication The authentication object containing the user's details and roles.
     * @return A redirect URL based on the user's role.
     */
    @GetMapping("/redirectBasedOnRole")
    public String redirectBasedOnRole(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("lecturer"))) {
            return "redirect:/lecturer/list.html";
        }
        else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("student"))) {
            return "redirect:/student/list.html";
        }

        return "redirect:http://localhost:8081/login/oauth2/code/keycloak";
    }
}