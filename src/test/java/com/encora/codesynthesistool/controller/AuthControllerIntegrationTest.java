package com.encora.codesynthesistool.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.encora.codesynthesistool.dto.LoginRequest;
import com.encora.codesynthesistool.dto.LoginResponse;
import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.repository.RefreshTokenRepository;
import com.encora.codesynthesistool.service.JwtBlacklistService;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureWebTestClient
class AuthControllerIntegrationTest {

    @Autowired private WebTestClient webTestClient;

    @Autowired private MongoReactiveUserDetailsService userDetailsService;

    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @Autowired private JwtBlacklistService jwtBlacklistService;

    private User testUser;
    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder().username("testuser").password("password").build();
        testUser = userDetailsService.createUser(testUser).block();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        LoginResponse loginResponse =
                webTestClient
                        .post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(loginRequest), LoginRequest.class)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(LoginResponse.class)
                        .returnResult()
                        .getResponseBody();

        assert loginResponse != null;
        accessToken = loginResponse.getAccessToken();
        refreshToken = loginResponse.getRefreshToken();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll().block();
        userDetailsService.deleteUser(testUser.getUsername()).block();
    }

    @Test
    void testRegisterUser_Success() {
        User newUser = User.builder().username("newuser").password("password").build();

        webTestClient
                .post()
                .uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(newUser), User.class)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(User.class)
                .value(
                        user -> {
                            assertNotNull(user.getId());
                            assertEquals(newUser.getUsername(), user.getUsername());
                        });

        userDetailsService.deleteUser(newUser.getUsername()).block();
    }

    @Test
    void testLogin_Success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        webTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(loginRequest), LoginRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(LoginResponse.class)
                .value(
                        response -> {
                            assertNotNull(response.getAccessToken());
                            assertNotNull(response.getRefreshToken());
                        });
    }

    @Test
    void testRefreshToken_Success() {
        webTestClient
                .post()
                .uri("/api/auth/refreshtoken")
                .header(HttpHeaders.AUTHORIZATION, "RefreshToken " + refreshToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(LoginResponse.class)
                .value(
                        response -> {
                            assertNotNull(response.getAccessToken());
                            assertNotNull(response.getRefreshToken());
                        });
    }

    @Test
    void testLogout_Success() {
        webTestClient
                .post()
                .uri("/api/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus()
                .isNoContent();

        assertTrue(jwtBlacklistService.isTokenBlacklisted(accessToken));
        assertNotEquals(
                Boolean.TRUE,
                refreshTokenRepository.findByUsername(testUser.getUsername()).hasElement().block());
    }
}
