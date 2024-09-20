package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.dto.LoginRequest;
import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.service.JwtBlacklistService;
import com.encora.codesynthesistool.service.JwtService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import com.encora.codesynthesistool.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.encora.codesynthesistool.util.TestUtils.getTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private MongoReactiveUserDetailsService userDetailsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private UserDetails userDetails;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = getTestUser();
        userDetails = Utils.mapUserToUserDetails(testUser);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");
    }

    @Test
    void testRegisterUser_Success() {
        when(userDetailsService.createUser(any(User.class))).thenReturn(Mono.just(testUser));

        Mono<ResponseEntity<User>> response = authController.registerUser(testUser);

        StepVerifier.create(response)
                .assertNext(userResponseEntity -> {
                    assertEquals(HttpStatus.CREATED, userResponseEntity.getStatusCode());
                    assertEquals(testUser, userResponseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    void testRegisterUser_DuplicateUsername() {
        when(userDetailsService.createUser(any(User.class)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists")));

        Mono<ResponseEntity<User>> response = authController.registerUser(testUser);

        StepVerifier.create(response)
                .assertNext(userResponseEntity -> assertEquals(HttpStatus.BAD_REQUEST, userResponseEntity.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void testLogin_Success() {
        when(userDetailsService.findByUsername(loginRequest.getUsername())).thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("testToken");

        Mono<String> response = authController.login(loginRequest);

        StepVerifier.create(response)
                .expectNext("testToken")
                .verifyComplete();
    }

    @Test
    void testLogin_InvalidCredentials() {
        when(userDetailsService.findByUsername(loginRequest.getUsername())).thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        Mono<String> response = authController.login(loginRequest);

        StepVerifier.create(response)
                .expectError(RuntimeException.class)
                .verify();
    }

    // this test makes with claude.ai, gemini not pass this challenge
    @Test
    void testLogoutWithValidToken() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(httpHeaders);

        // Arrange
        String token = "validToken";
        String bearerToken = "Bearer " + token;
        when(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(bearerToken);

        Mono<SecurityContext> securityContextMono = Mono.just(securityContext);

        // Act
        Mono<ResponseEntity<Void>> result = authController.logout(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(securityContextMono));

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful())
                .verifyComplete();

        verify(jwtBlacklistService).blacklistToken(token);
        verify(securityContext).setAuthentication(null);
    }

    @Test
    void testLogoutWithoutToken() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(httpHeaders);

        // Arrange
        when(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        Mono<SecurityContext> securityContextMono = Mono.just(securityContext);

        // Act
        Mono<ResponseEntity<Void>> result = authController.logout(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(securityContextMono));

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful())
                .verifyComplete();

        verify(jwtBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    void testLogoutWithInvalidTokenFormat() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(httpHeaders);

        // Arrange
        when(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("InvalidToken");

        Mono<SecurityContext> securityContextMono = Mono.just(securityContext);

        // Act
        Mono<ResponseEntity<Void>> result = authController.logout(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(securityContextMono));

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful())
                .verifyComplete();

        verify(jwtBlacklistService, never()).blacklistToken(anyString());
    }
}