package jku.se.g03ue02code.controller;

import jku.se.g03ue02code.service.UserRetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * Controller for handling user-related actions such as retrieving user information and deleting users.
 */
@RestController
public class UserController {

    @Autowired
    private UserRetrievalService userRetrievalService;

    /**
     * Retrieves information about the authenticated user, including their name and role.
     *
     * @param principal The OAuth2 authenticated user principal.
     * @return A map containing the user's name and role.
     */
    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        String name = principal.getAttribute("name");

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        return Map.of(
                "name", Optional.ofNullable(name).orElse(""),
                "role", role
        );
    }

    /**
     * Retrieves a list of all users with the "student" role.
     *
     * @return A ResponseEntity containing a list of students or an error message in case of a problem.
     */
    @GetMapping("/students")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<Map<String, Object>> users = userRetrievalService.getUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("There was a problem retrieving the students.");
        }
    }

    /**
     * Deletes a user based on the provided user ID.
     *
     * @param userId The ID of the user to be deleted.
     * @return A ResponseEntity with a success message if the deletion is successful, or an error message otherwise.
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        try {
            userRetrievalService.deleteUser(userId);
            return ResponseEntity.ok("User successfully deleted.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("There was a problem deleting the user.");
        }
    }
}