package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.dto.LoginResponse;
import com.encora.codesynthesistool.model.RefreshToken;
import com.encora.codesynthesistool.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Service
public class JwtService {

    private final byte[] SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();

    @Value("${jwt.token-expiration-time}")
    private long tokenExpirationTime;

    @Value("${jwt.refresh-expiration-time}")
    private long refreshExpirationTime;

    private final ReactiveUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    public Mono<LoginResponse> generateTokens(UserDetails userDetails) {
        String accessToken = generateAccessToken(userDetails);
        return generateRefreshToken(userDetails.getUsername())
                .map(refreshToken -> new LoginResponse(accessToken, refreshToken.getToken()));
    }

    private String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpirationTime))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY), SignatureAlgorithm.HS512)
                .compact();
    }

    private Mono<RefreshToken> generateRefreshToken(String username) {
        RefreshToken refreshToken =
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .username(username)
                        .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationTime / 1000))
                        .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public Mono<LoginResponse> refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token for refresh token: {}", refreshToken);
        return refreshTokenRepository
                .findByToken(refreshToken)
                .flatMap(this::verifyExpiration)
                .flatMap(token -> userDetailsService.findByUsername(token.getUsername()))
                .flatMap(
                        userDetails -> {
                            String newAccessToken = generateAccessToken(userDetails);
                            return generateRefreshToken(userDetails.getUsername())
                                    .flatMap(
                                            newRefreshToken ->
                                                    refreshTokenService
                                                            .deleteByToken(refreshToken)
                                                            .thenReturn(
                                                                    new LoginResponse(
                                                                            newAccessToken,
                                                                            newRefreshToken
                                                                                    .getToken())));
                        })
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Invalid refresh token")));
    }

    private Mono<RefreshToken> verifyExpiration(RefreshToken token) {
        return Mono.just(token)
                .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
                .switchIfEmpty(
                        refreshTokenService
                                .delete(token) // Delete if expired
                                .then(
                                        Mono.error(
                                            new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "Refresh token was expired. Please make a new signin request"))));
    }

    public Mono<Void> invalidateRefreshTokenByUsername(String username) {
        log.info("Invalidating refresh token for user: {}", username);
        return refreshTokenService.deleteByUsername(username);
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
