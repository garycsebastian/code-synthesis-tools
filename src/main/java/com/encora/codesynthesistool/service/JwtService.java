package com.encora.codesynthesistool.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Slf4j
@Service
public class JwtService {

    private final byte[] SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
    private final long EXPIRATION_TIME = 3600000; // 1 hora en milisegundos

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
            return userDetailsService
                    .findByUsername(username) // Usamos el servicio reactive
                    .map(
                            userDetails ->
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()));
        }
        return Mono.empty();
    }

    public Mono<UserDetails> validateToken(String token) {
        return Mono.fromCallable(
                        () -> {
                            try {
                                log.info("validateToken: {}", token);
                                Jwts.parserBuilder()
                                        .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY))
                                        .build()
                                        .parseClaimsJws(
                                                token); // This will throw an exception if invalid
                                return extractUsername(token);
                            } catch (ExpiredJwtException e) {
                                //  ExpiredJwtException: Thrown when the token's expiration date has
                                // passed.
                                log.error("JWT token expired: {}", e.getMessage());
                                throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "JWT token expired: " + e.getMessage());
                            } catch (UnsupportedJwtException e) {
                                //  UnsupportedJwtException: Thrown for JWTs that are not supported
                                // by the parser
                                // (e.g., using an unsupported algorithm).
                                log.error("Unsupported JWT token: {}", e.getMessage());
                                throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Unsupported JWT token: " + e.getMessage());
                            } catch (MalformedJwtException e) {
                                log.error("Invalid JWT token: {}", e.getMessage());
                                throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Invalid JWT token: " + e.getMessage());
                            } catch (
                                    SignatureException
                                            e) { // Use io.jsonwebtoken.security.SignatureException
                                //  SignatureException: Thrown when the JWT's signature validation
                                // fails.
                                log.error("Invalid JWT signature: {}", e.getMessage());
                                throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Invalid JWT signature: " + e.getMessage());
                            } catch (IllegalArgumentException e) {
                                //  IllegalArgumentException: A general exception that might be
                                // thrown for various
                                // reasons, including an empty JWT string.
                                log.error("JWT claims string is empty: {}", e.getMessage());
                                throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "JWT claims string is empty: " + e.getMessage());
                            }
                        })
                .flatMap(
                        username -> { // Use flatMap to continue the reactive chain
                            if (username != null) {
                                // Encuentra los detalles del usuario y valida si el token ha
                                // expirado
                                return userDetailsService
                                        .findByUsername(username)
                                        .filter(userDetails -> !isTokenExpired(token))
                                        .switchIfEmpty(
                                                Mono.error(
                                                        new ResponseStatusException(
                                                                HttpStatus.UNAUTHORIZED,
                                                                "Token expired or invalid user")));
                            } else {
                                return Mono.error(
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "Invalid token format"));
                            }
                        });
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
