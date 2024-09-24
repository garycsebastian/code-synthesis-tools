package com.encora.codesynthesistool.util;

import com.encora.codesynthesistool.model.TaskStatus;
import com.encora.codesynthesistool.model.User;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Utility class providing common helper methods. */
public class Utils {

    // Define allowed characters and patterns (customize these to your needs)
    private static final Pattern ALLOWED_DESCRIPTION_PATTERN =
            Pattern.compile("^[a-zA-Z0-9\\s.,!?()-]{0,255}$");
    private static final Pattern ALLOWED_TITLE_PATTERN =
            Pattern.compile("^[a-zA-Z0-9\\s-]{0,100}$");

    /**
     * Sanitizes a task description to prevent XSS vulnerabilities.
     *
     * @param description The task description to sanitize.
     * @return The sanitized task description.
     * @throws IllegalArgumentException If the description contains invalid characters.
     */
    public static String sanitizeDescription(String description) {
        if (description == null || ALLOWED_DESCRIPTION_PATTERN.matcher(description).matches()) {
            return description; // Input is valid
        } else {
            throw new IllegalArgumentException("Invalid characters in task description.");
        }
    }

    /**
     * Sanitizes a task title to prevent XSS vulnerabilities.
     *
     * @param title The task title to sanitize.
     * @return The sanitized task title.
     * @throws IllegalArgumentException If the title contains invalid characters.
     */
    public static String sanitizeTitle(String title) {
        if (title == null || ALLOWED_TITLE_PATTERN.matcher(title).matches()) {
            return title; // Input is valid
        } else {
            throw new IllegalArgumentException("Invalid characters in task title.");
        }
    }

    /**
     * Extracts the JWT token from a ServerHttpRequest.
     *
     * @param request The ServerHttpRequest object.
     * @return The JWT token string, or null if not found.
     */
    public static String extractJwtFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Maps a User object to a Spring Security UserDetails object.
     *
     * @param user The User object to map.
     * @return The corresponding UserDetails object.
     */
    public static UserDetails mapUserToUserDetails(User user) {
        // Si el usuario no tiene roles, se le asigna el rol "USER" por defecto
        List<GrantedAuthority> authorities =
                user.getRoles() == null || user.getRoles().isEmpty()
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        : user.getRoles().stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.isActive())
                .credentialsExpired(!user.isActive())
                .disabled(!user.isActive())
                .accountLocked(!user.isActive())
                .build();
    }

    /**
     * Checks if a TaskStatus is valid.
     *
     * @param status The TaskStatus to check.
     * @return True if the status is valid, false otherwise.
     */
    public static boolean isValidStatus(TaskStatus status) {
        return status == TaskStatus.PENDING
                || status == TaskStatus.IN_PROGRESS
                || status == TaskStatus.COMPLETED;
    }
}
