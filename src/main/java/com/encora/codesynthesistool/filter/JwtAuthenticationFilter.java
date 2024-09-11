package com.encora.codesynthesistool.filter;

import com.encora.codesynthesistool.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
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
    private final ServerSecurityContextRepository securityContextRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ServerWebExchangeMatchers.pathMatchers("/api/users/login", "/api/public/**")
                .matches(exchange)
                .flatMap(matchResult -> {
                    if (matchResult.isMatch()) {
                        // Si es un endpoint público, continua sin validar el token
                        return chain.filter(exchange);
                    } else {
                        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                        log.info("authHeader: {}", authHeader);
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        String token = authHeader.substring(7);
                        return jwtService.validateToken(token) // Validar token y usuario
                                .flatMap(userDetails -> {
                                    log.info("userDetails: {}", userDetails);
                                    // Crear el Authentication y SecurityContext con el usuario autenticado
                                    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, userDetails.getAuthorities());
                                    SecurityContext securityContext = new SecurityContextImpl(authentication);
                                    log.info("securityContext: {}", securityContext);
                                    return securityContextRepository.save(exchange, securityContext)  // Guardar el contexto de seguridad
                                            .then(chain.filter(exchange));  // Continuar con el proceso de filtros
                                })
                                .onErrorResume(error -> { // Manejar errores de autenticación
                                    log.error("Error validating token: {}", error.getMessage());
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete();
                                });
                    }
                });
    }
}
