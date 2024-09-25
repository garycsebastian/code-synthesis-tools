package com.encora.codesynthesistool.controller;

import static com.encora.codesynthesistool.util.TestUtils.getTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.encora.codesynthesistool.dto.LoginRequest;
import com.encora.codesynthesistool.dto.LoginResponse;
import com.encora.codesynthesistool.model.RefreshToken;
import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.service.JwtBlacklistService;
import com.encora.codesynthesistool.service.JwtService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import com.encora.codesynthesistool.util.Utils;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private JwtService jwtService;
    @Mock private MongoReactiveUserDetailsService userDetailsService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtBlacklistService jwtBlacklistService;
    @Mock private Supplier<Bucket> bucketSupplier;
    @InjectMocks private AuthController authController;

    private User testUser;
    private UserDetails userDetails;
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private RefreshToken refreshToken;

    private final Bucket mockBucket = Mockito.mock(Bucket.class);

    @BeforeEach
    void setUp() {

        testUser = getTestUser();
        userDetails = Utils.mapUserToUserDetails(testUser);
        refreshToken =
                RefreshToken.builder()
                        .id("testRefreshTokenId")
                        .token("testRefreshToken")
                        .username(testUser.getUsername())
                        .expiryDate(LocalDateTime.now().plusDays(1))
                        .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        loginResponse = new LoginResponse("testToken", "testRefreshToken");
    }

    @Test
    void testRegisterUser_Success() {
        when(userDetailsService.createUser(any(User.class))).thenReturn(Mono.just(testUser));

        Mono<ResponseEntity<User>> response = authController.registerUser(testUser);

        StepVerifier.create(response)
                .assertNext(
                        userResponseEntity -> {
                            assertEquals(HttpStatus.CREATED, userResponseEntity.getStatusCode());
                            assertEquals(testUser, userResponseEntity.getBody());
                        })
                .verifyComplete();

        verify(userDetailsService).createUser(any(User.class));
    }

    @Test
    void testLogin_Success() {
        when(bucketSupplier.get()).thenReturn(mockBucket);
        when(bucketSupplier.get().tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.consumed(1, 0));
        when(userDetailsService.findByUsername(loginRequest.getUsername()))
                .thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                .thenReturn(true);
        when(jwtService.generateTokens(any())).thenReturn(Mono.just(loginResponse));

        Mono<ResponseEntity<LoginResponse>> response = authController.login(loginRequest);

        StepVerifier.create(response).expectNext(ResponseEntity.ok(loginResponse)).verifyComplete();

        verify(userDetailsService).findByUsername(loginRequest.getUsername());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtService).generateTokens(userDetails);
    }

    @Test
    void testLogin_InvalidCredentials() {
        when(bucketSupplier.get()).thenReturn(mockBucket);
        when(mockBucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.consumed(1, 0));

        when(userDetailsService.findByUsername(loginRequest.getUsername()))
                .thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
                .thenReturn(false);

        Mono<ResponseEntity<LoginResponse>> response = authController.login(loginRequest);

        StepVerifier.create(response).expectError(RuntimeException.class).verify();

        verify(userDetailsService).findByUsername(loginRequest.getUsername());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtService, never()).generateTokens(any());
    }

    @Test
    void testRefreshToken_Success() {
        MockServerHttpRequest request =
                MockServerHttpRequest.post("/api/auth/refreshtoken")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken.getToken())
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtService.refreshAccessToken(refreshToken.getToken()))
                .thenReturn(Mono.just(new LoginResponse("newAccessToken", "newRefreshToken")));

        Mono<ResponseEntity<LoginResponse>> response = authController.refreshToken(exchange);

        StepVerifier.create(response)
                .expectNextMatches(
                        entity ->
                                entity.getStatusCode().is2xxSuccessful()
                                        && entity.getBody() != null
                                        && "newAccessToken"
                                                .equals(entity.getBody().getAccessToken())
                                        && "newRefreshToken"
                                                .equals(entity.getBody().getRefreshToken()))
                .verifyComplete();

        verify(jwtService).refreshAccessToken(refreshToken.getToken());
    }

    @Test
    void testLogout_Success() {
        String token = "testToken";
        MockServerHttpRequest request =
                MockServerHttpRequest.post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtBlacklistService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn(testUser.getUsername());
        doNothing().when(jwtBlacklistService).blacklistToken(token);
        doNothing().when(jwtService).invalidateRefreshTokenByUsername(testUser.getUsername());

        Mono<ResponseEntity<Void>> response =
                authController
                        .logout(exchange)
                        .contextWrite(
                                ReactiveSecurityContextHolder.withAuthentication(
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities())));

        StepVerifier.create(response)
                .expectNextMatches(res -> res.getStatusCode() == HttpStatus.NO_CONTENT)
                .verifyComplete();

        verify(jwtBlacklistService).blacklistToken(token);
        verify(jwtService).invalidateRefreshTokenByUsername(testUser.getUsername());
    }

    @Test
    void testLogout_TokenBlacklisted() {
        String token = "testToken";
        MockServerHttpRequest request =
                MockServerHttpRequest.post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<ResponseEntity<Void>> response = authController.logout(exchange);

        StepVerifier.create(response)
                .expectNextMatches(res -> res.getStatusCode() == HttpStatus.NO_CONTENT)
                .verifyComplete();

        verify(jwtBlacklistService, never()).blacklistToken(any());
        verify(jwtService, never()).invalidateRefreshTokenByUsername(any());
    }

    @Test
    void testLogout_NoToken() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/logout").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<ResponseEntity<Void>> response = authController.logout(exchange);

        StepVerifier.create(response)
                .expectNextMatches(res -> res.getStatusCode() == HttpStatus.NO_CONTENT)
                .verifyComplete();

        verify(jwtBlacklistService, never()).blacklistToken(any());
        verify(jwtService, never()).invalidateRefreshTokenByUsername(any());
    }
}
