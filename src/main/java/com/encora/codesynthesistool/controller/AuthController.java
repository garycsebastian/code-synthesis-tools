package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.dto.LoginRequest;
import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.service.JwtBlacklistService;
import com.encora.codesynthesistool.service.JwtService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.encora.codesynthesistool.util.Utils.extractJwtFromRequest;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/auth") // New endpoint for authentication
public class AuthController {

    private final JwtService jwtService;
    private final MongoReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;

    @PostMapping("/signup")
    public Mono<ResponseEntity<User>> registerUser(@RequestBody User user) {
        return userDetailsService.createUser(user)
                .map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(savedUser))
                .onErrorReturn(ResponseStatusException.class,
                        ResponseEntity.badRequest().build()); // Return only the ResponseEntity
    }

    @PostMapping("/login")
    public Mono<String> login(@RequestBody LoginRequest loginRequest) {
        return userDetailsService.findByUsername(loginRequest.getUsername())
                .filter(userDetails -> passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword()))
                .map(jwtService::generateToken)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid username or password")));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    String token = extractJwtFromRequest(exchange.getRequest());
                    if (token != null && !jwtBlacklistService.isTokenBlacklisted(token)) {
                        log.info("Token blacklisted: {}", token);
                        jwtBlacklistService.blacklistToken(token);
                        securityContext.setAuthentication(null);
                    }
                    return Mono.empty(); // Return Mono.empty() here
                })
                .then(Mono.just(ResponseEntity.noContent().build())); // then return the response
    }
}
