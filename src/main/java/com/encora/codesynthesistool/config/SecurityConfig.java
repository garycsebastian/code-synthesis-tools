package com.encora.codesynthesistool.config;

import com.encora.codesynthesistool.filter.JwtAuthenticationFilter;
import com.encora.codesynthesistool.repository.UserRepository;
import com.encora.codesynthesistool.service.JwtService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Define a method to create a matcher for public endpoints
    private ServerWebExchangeMatcher publicEndpoints() {
        return ServerWebExchangeMatchers.pathMatchers(
                "/api/public/**",
                "/api/auth/login", // Authentication endpoint
                "/api/users/register" // Registration endpoint
        );
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, JwtService jwtService) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Disable CSRF for APIs
                .cors(ServerHttpSecurity.CorsSpec::disable) // Configura o permite CORS aquí
                .securityContextRepository(new WebSessionServerSecurityContextRepository()) // Use session-based repository
                .authorizeExchange(exchanges -> exchanges
                        .matchers(publicEndpoints()).permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(jwtService, securityContextRepository()), SecurityWebFiltersOrder.AUTHENTICATION)
                // ... otras configuraciones ...
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // Desactiva form login y usa endpoints API
                .build();
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository(); // Guarda el contexto de seguridad en la sesión
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, ServerSecurityContextRepository securityContextRepository) {
        return new JwtAuthenticationFilter(jwtService, securityContextRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new MongoReactiveUserDetailsService(userRepository, passwordEncoder);
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(ReactiveUserDetailsService userDetailsService) {
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder()); // Set password encoder here
        return manager;
    }
}
