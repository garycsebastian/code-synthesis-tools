package com.encora.codesynthesistool.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;

@AllArgsConstructor
@Slf4j
@Service
public class JwtService {

    private final ReactiveUserDetailsService userDetailsService;

    private final byte[] SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
    private final long EXPIRATION_TIME = 86400000; // 1 día en milisegundos

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    public Mono<Authentication> getAuthentication(String token) {
        String username = extractUsername(token);
        if (username != null) {
            return userDetailsService.findByUsername(username) // Usamos el servicio reactive
                    .map(userDetails -> new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()));
        }
        return Mono.empty();
    }

    public Mono<UserDetails> validateToken(String token) {
        try {
            log.info("validateToken: {}", token);
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            String username = extractUsername(token);
            return userDetailsService.findByUsername(username)
                    .filter(userDetails -> !isTokenExpired(token));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Invalid token"));
        }
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}