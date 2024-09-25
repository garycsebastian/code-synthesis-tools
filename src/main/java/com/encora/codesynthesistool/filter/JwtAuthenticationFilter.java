package com.encora.codesynthesistool.filter;

import com.encora.codesynthesistool.service.JwtBlacklistService;
import com.encora.codesynthesistool.service.JwtService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;
    private final ServerSecurityContextRepository securityContextRepository;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    @NonNull public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // Extract token from Authorization header (if present)
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Check if the token is blacklisted
            if (jwtBlacklistService.isTokenBlacklisted(token)) {
                log.warn("Blacklisted token detected: {}", token);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return jwtService
                    .validateToken(token)
                    .flatMap(
                            userDetails -> {
                                // Create Authentication and SecurityContext
                                Authentication authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities());
                                SecurityContext securityContext =
                                        new SecurityContextImpl(authentication);

                                // Save SecurityContext and continue
                                return securityContextRepository
                                        .save(exchange, securityContext)
                                        .thenEmpty(chain.filter(exchange));
                            })
                    .onErrorResume(
                            error -> {
                                // Handle other validation errors (propagate the error)
                                log.error("Error validating token: {}", error.getMessage(), error);
                                if (!(error instanceof ResponseStatusException)) {
                                    return Mono.error(
                                            new ResponseStatusException(
                                                    HttpStatus.UNAUTHORIZED,
                                                    "Authentication failed"));
                                }
                                return Mono.error(error); // Propagate the error
                            });
        } else {
            // No token, continue to the next filter (SecurityConfig will handle authorization)
            log.warn("No token found in Authorization header");
            return chain.filter(exchange);
        }
    }
}
