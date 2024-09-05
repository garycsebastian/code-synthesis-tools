package com.encora.codesynthesistool.config;

import com.encora.codesynthesistool.repository.UserRepository;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new MongoReactiveUserDetailsService(userRepository, passwordEncoder);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, UserRepository userRepository) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable) // Disable CSRF for now (important to address in production)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET,"/api/public/**", "/login", "/register").permitAll() // Public endpoints
                        .pathMatchers(HttpMethod.POST, "/api/users/register","/login").permitAll() // Permite el registro a todos
                        .pathMatchers("/api/tasks/**").authenticated() // private endpoints
//                        .anyExchange().authenticated() // All other endpoints require authentication
                )
                .authenticationManager(authenticationManager( userDetailsService(userRepository, passwordEncoder()),passwordEncoder()))
                .securityContextRepository(securityContextRepository())
                // Correct way to configure authenticationMatchers for formLogin
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/login"))
                // Manejador de inicio de sesión personalizado
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .authenticationSuccessHandler(authenticationSuccessHandler())
                        .authenticationFailureHandler(authenticationFailureHandler())
                )
                // Logout configuration
                .logout((logout) -> logout
                        .logoutUrl("/logout") // Customize the logout URL if needed
                        .logoutSuccessHandler(logoutSuccessHandler())
                );

        return http.build();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(ReactiveUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder); // Set password encoder here
        return manager;
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    private ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
        RedirectServerAuthenticationSuccessHandler successHandler = new RedirectServerAuthenticationSuccessHandler();
        successHandler.setLocation(URI.create("/api/tasks")); // Redirige a / después del inicio de sesión
        return successHandler;
    }

    private ServerAuthenticationFailureHandler authenticationFailureHandler() {
        return (web, ex) ->
                Mono.fromRunnable(() -> web.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED));
    }

    private ServerLogoutSuccessHandler logoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(URI.create("/api/public")); // Redirige a una página pública después del cierre de sesión
        return handler;
    }
}
