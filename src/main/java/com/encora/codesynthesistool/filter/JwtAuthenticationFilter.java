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
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
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

            return jwtService.validateToken(token)
                    .flatMap(userDetails -> {
                        // Create Authentication and SecurityContext
                        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContext securityContext = new SecurityContextImpl(authentication);

                        // Save SecurityContext and continue
                        return securityContextRepository.save(exchange, securityContext)
                                .then(chain.filter(exchange));
                    })
                    .onErrorResume(error -> {
                        // Handle authentication errors (e.g., invalid token)
                        log.error("Error validating token: {}", error.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        } else {
            // No token, continue to the next filter (SecurityConfig will handle authorization)
            return chain.filter(exchange);
        }
    }
}
