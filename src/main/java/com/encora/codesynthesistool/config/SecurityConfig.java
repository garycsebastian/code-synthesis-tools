package com.encora.codesynthesistool.config;

import com.encora.codesynthesistool.filter.JwtAuthenticationFilter;
import com.encora.codesynthesistool.repository.UserRepository;
import com.encora.codesynthesistool.service.JwtBlacklistService;
import com.encora.codesynthesistool.service.JwtService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import java.util.Arrays;
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
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XXssProtectionServerHttpHeadersWriter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Define a method to create a matcher for public endpoints
    private ServerWebExchangeMatcher publicEndpoints() {
        return ServerWebExchangeMatchers.pathMatchers(
                "/api/public/**",
                "/api/auth/login", // Authentication endpoint
                "/api/auth/signup", // Registration endpoint
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**" // Añade esta línea
                );
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JwtService jwtService,
            JwtBlacklistService jwtBlacklistService) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(
                        headers ->
                                headers.frameOptions(
                                                frameOptions ->
                                                        frameOptions.mode(
                                                                XFrameOptionsServerHttpHeadersWriter
                                                                        .Mode.DENY))
                                        .xssProtection(
                                                xss ->
                                                        xss.headerValue(
                                                                XXssProtectionServerHttpHeadersWriter
                                                                        .HeaderValue
                                                                        .ENABLED_MODE_BLOCK))
                                        .contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                "default-src 'self'; frame-ancestors 'none';")))
                .securityContextRepository(
                        new WebSessionServerSecurityContextRepository()) // Use session-based
                // repository
                .authorizeExchange(
                        exchanges ->
                                exchanges
                                        .matchers(publicEndpoints())
                                        .permitAll()
                                        .anyExchange()
                                        .authenticated())
                // Add filter AFTER authentication processing:
                .addFilterAfter(
                        jwtAuthenticationFilter(
                                jwtService, securityContextRepository(), jwtBlacklistService),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                // ... otras configuraciones ...
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .build();
    }

    //    @Bean
    //    public ServerCsrfTokenRepository csrfTokenRepository() {
    //        WebSessionServerCsrfTokenRepository repository = new
    // WebSessionServerCsrfTokenRepository();
    //        repository.setHeaderName("X-XSRF-TOKEN");
    //        return repository;
    //    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "X-XSRF-TOKEN", "XSRF-TOKEN"));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository(); // Guarda el contexto de seguridad
        // en la
        // sesión
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            ServerSecurityContextRepository securityContextRepository,
            JwtBlacklistService jwtBlacklistService) {
        return new JwtAuthenticationFilter(
                jwtService, securityContextRepository, jwtBlacklistService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService(
            UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new MongoReactiveUserDetailsService(userRepository, passwordEncoder);
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService userDetailsService) {
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder()); // Set password encoder here
        return manager;
    }
}
