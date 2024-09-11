package com.encora.codesynthesistool.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
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

    private final byte[] SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
    private final long EXPIRATION_TIME = 86400000; // 1 día en milisegundos

    private final ReactiveUserDetailsService userDetailsService;

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY), SignatureAlgorithm.HS512)
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
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY))
                    .build()
                    .parseClaimsJws(token); // This will throw an exception if invalid

            String username = extractUsername(token);
            // Encuentra los detalles del usuario y valida si el token ha expirado
            return userDetailsService.findByUsername(username)
                    .filter(userDetails -> !isTokenExpired(token))
                    .switchIfEmpty(Mono.error(new RuntimeException("Token expired or invalid user")));
        } catch (ExpiredJwtException e) {
            return Mono.error(new JwtException("Token expired"));
        } catch (UnsupportedJwtException e) {
            return Mono.error(new JwtException("Unsupported JWT token"));
        } catch (MalformedJwtException e) {
            return Mono.error(new JwtException("Invalid JWT token"));
        } catch (SignatureException e) {
            return Mono.error(new JwtException("Invalid JWT signature"));
        } catch (IllegalArgumentException e) {
            return Mono.error(new JwtException("JWT claims string is empty"));
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}