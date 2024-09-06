package com.encora.codesynthesistool.filter;

import com.encora.codesynthesistool.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ServerWebExchangeMatchers.pathMatchers("/api/**")
                .matches(exchange)
                .flatMap(matchResult -> {
                    if (matchResult.isMatch()) {
                        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                        log.info("authHeader: {}", authHeader);
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            return jwtService.validateToken(token) // Validar token y usuario
                                    .flatMap(userDetails -> {
                                        log.info("userDetails: {}", userDetails);
                                        UsernamePasswordAuthenticationToken authentication =
                                                new UsernamePasswordAuthenticationToken(
                                                        userDetails, null, userDetails.getAuthorities());
                                        SecurityContextHolder.getContext().setAuthentication(authentication);
                                        return chain.filter(exchange);
                                    })
                                    .onErrorResume(error -> { // Manejar errores de autenticación
                                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                        return Mono.empty();
                                    });
                        }
                    }
                    return chain.filter(exchange);
                });
    }
}
