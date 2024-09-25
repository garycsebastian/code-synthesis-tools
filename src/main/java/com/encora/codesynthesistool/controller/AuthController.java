package com.encora.codesynthesistool.controller;

import static com.encora.codesynthesistool.util.Utils.extractJwtFromRequest;

import com.encora.codesynthesistool.dto.LoginRequest;
import com.encora.codesynthesistool.dto.LoginResponse;
import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.service.JwtBlacklistService;
import com.encora.codesynthesistool.service.JwtService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/auth") // New endpoint for authentication
public class AuthController {

    private final JwtService jwtService;
    private final MongoReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;
    private final Supplier<Bucket> bucketSupplier; // Inject the bucket supplier

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided details.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "User registered successfully",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = User.class))
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description = "Bad request, such as invalid user data",
                        content = @Content)
            })
    @PostMapping("/signup")
    public Mono<ResponseEntity<User>> registerUser(@RequestBody User user) {
        return userDetailsService
                .createUser(user)
                .map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(savedUser))
                .onErrorReturn(
                        ResponseStatusException.class,
                        ResponseEntity.badRequest().build()); // Return only the ResponseEntity
    }

    @Operation(
            summary = "Login user",
            description = "Logs in a user with the provided credentials.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "User logged in successfully",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LoginResponse.class))
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description = "Bad request, such as invalid username or password.",
                        content = @Content),
                @ApiResponse(
                        responseCode = "429",
                        description = "Rate limit exceeded. Try again later.",
                        content = @Content)
            })
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        return Mono.just(loginRequest)
                .map(login -> bucketSupplier.get().tryConsumeAndReturnRemaining(1))
                .flatMap(
                        probe -> {
                            if (probe.isConsumed()) {
                                return userDetailsService
                                        .findByUsername(loginRequest.getUsername())
                                        .filter(
                                                userDetails ->
                                                        passwordEncoder.matches(
                                                                loginRequest.getPassword(),
                                                                userDetails.getPassword()))
                                        .flatMap(jwtService::generateTokens)
                                        .map(ResponseEntity::ok)
                                        .switchIfEmpty(
                                                Mono.error(
                                                        new RuntimeException(
                                                                "Invalid username or password")));
                            } else {
                                long waitForRefill = probe.getNanosToWaitForRefill();
                                return Mono.error(
                                        new RuntimeException(
                                                "Rate limit exceeded. Try again in "
                                                        + waitForRefill / 1_000_000_000
                                                        + " seconds."));
                            }
                        });
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Access token refreshed successfully.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LoginResponse.class))
                        }),
                @ApiResponse(
                        responseCode = "401",
                        description = "Invalid or missing refresh token.",
                        content = @Content)
            })
    @PostMapping("/refreshtoken")
    public Mono<ResponseEntity<LoginResponse>> refreshToken(ServerWebExchange exchange) {
        return Mono.just(Objects.requireNonNull(extractJwtFromRequest(exchange.getRequest())))
                .flatMap(jwtService::refreshAccessToken)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new RuntimeException("Refresh token is missing")));
    }

    @Operation(summary = "Logout user", description = "Logs out the current user.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "204",
                        description = "User logged out successfully",
                        content = @Content),
                @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
            })
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(
                        securityContext -> {
                            String token = extractJwtFromRequest(exchange.getRequest());
                            if (token != null && !jwtBlacklistService.isTokenBlacklisted(token)) {
                                log.info("Token blacklisted: {}", token);
                                jwtBlacklistService.blacklistToken(token);
                                String username = jwtService.extractUsername(token);
                                return jwtService
                                        .invalidateRefreshTokenByUsername(username)
                                        .then(Mono.empty()) // Subscribe to the deletion and
                                        // continue
                                        .doOnSuccess(
                                                v -> {
                                                    securityContext.setAuthentication(null);
                                                    log.info(
                                                            "Logout successful for user: {}",
                                                            username);
                                                });
                            }
                            return Mono.empty(); // Return Mono.empty() here
                        })
                .then(Mono.just(ResponseEntity.noContent().build())); // then return the response
    }
}
