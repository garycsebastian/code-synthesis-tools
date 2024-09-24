package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final MongoReactiveUserDetailsService userDetailsService;

    @Operation(
            summary = "Update current user",
            description = "Updates the currently logged-in user's information.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "User updated successfully"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Bad request, such as invalid user data"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - User not logged in"),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - User not allowed to update this profile")
            })
    @PutMapping("/me") // Endpoint changed to /me for current user
    @PreAuthorize("isAuthenticated()") // Ensures the user is authenticated
    public Mono<ResponseEntity<User>> updateCurrentUser(
            @RequestBody User updatedUser,
            @AuthenticationPrincipal Mono<UserDetails> userDetailsMono) {
        return userDetailsMono
                .flatMap(
                        userDetails ->
                                userDetailsService
                                        .updateUser(userDetails.getUsername(), updatedUser)
                                        .map(ResponseEntity::ok)
                                        .onErrorMap(
                                                RuntimeException.class,
                                                ex ->
                                                        new ResponseStatusException(
                                                                HttpStatus.BAD_REQUEST,
                                                                ex.getMessage())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Delete current user",
            description = "Deletes the currently logged-in user's account.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - User not logged in"),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - User not allowed to delete this profile")
            })
    @DeleteMapping("/me") // Endpoint changed to /me for current user
    @PreAuthorize("isAuthenticated()") // Ensures the user is authenticated
    public Mono<ResponseEntity<Void>> deleteCurrentUser(
            @AuthenticationPrincipal Mono<UserDetails> userDetailsMono) {
        return userDetailsMono
                .flatMap(
                        userDetails ->
                                userDetailsService
                                        .deleteUser(userDetails.getUsername())
                                        .thenReturn(
                                                ResponseEntity.status(HttpStatus.NO_CONTENT)
                                                        .<Void>build())
                                        .onErrorMap(
                                                RuntimeException.class,
                                                ex ->
                                                        new ResponseStatusException(
                                                                HttpStatus.BAD_REQUEST,
                                                                ex.getMessage())))
                .defaultIfEmpty(
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .build()); // Explicit 404 with <Void>
    }
}
