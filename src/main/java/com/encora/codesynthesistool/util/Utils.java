package com.encora.codesynthesistool.util;

import com.encora.codesynthesistool.model.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {

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
        List<GrantedAuthority> authorities = user.getRoles() == null || user.getRoles().isEmpty() ?
                List.of(new SimpleGrantedAuthority("ROLE_USER")) :
                user.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.isActive())
                .credentialsExpired(!user.isActive())
                .disabled(!user.isActive())
                .accountLocked(!user.isActive())
                .build();
    }
}
