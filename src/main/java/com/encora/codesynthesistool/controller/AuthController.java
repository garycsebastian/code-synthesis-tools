package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.dto.LoginRequest;
import com.encora.codesynthesistool.service.JwtService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth") // New endpoint for authentication
public class AuthController {

    private final JwtService jwtService;
    private final MongoReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Mono<String> login(@RequestBody LoginRequest loginRequest) {
        return userDetailsService.findByUsername(loginRequest.getUsername())
                .filter(userDetails -> passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword()))
                .map(jwtService::generateToken)
                .defaultIfEmpty("Invalid username or password")
                .handle((token, sink) -> {
                    if (token.equals("Invalid username or password")) {
                        sink.error(new RuntimeException("Invalid username or password"));
                        return;
                    }
                    sink.next(token);
                });
    }
}
