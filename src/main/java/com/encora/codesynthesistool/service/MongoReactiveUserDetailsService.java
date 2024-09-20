package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Service class for handling user-related operations, specifically for MongoDB.
 * Implements Spring Security's ReactiveUserDetailsService for reactive user details retrieval.
 */
@AllArgsConstructor
@Service
@Primary
@Slf4j
public class MongoReactiveUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Finds a user by their username.
     *
     * @param username The username of the user to retrieve.
     * @return A Mono emitting the UserDetails object if found, or an error signal if not found.
     */
    @Override
    public Mono<UserDetails> findByUsername(String username) {
        log.info("findByUsername: {}", username);
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .cast(UserDetails.class)
                .doOnSuccess(user -> log.info("User found: {}", user.getUsername()))
                .doOnError(error -> log.error("Error finding user: {}", error.getMessage()));
    }

    /**
     * Creates a new user.
     *
     * @param user The User object to create.
     * @return A Mono emitting the created User object, or an error signal if creation fails (e.g., duplicate username).
     */
    public Mono<User> createUser(User user) {
        return Mono.just(user)
                .map(u -> {
                    u.setPassword(passwordEncoder.encode(u.getPassword()));
                    return u;
                })
                .flatMap(u -> userRepository.save(u)
                        .onErrorMap(DuplicateKeyException.class,
                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists"))) // Error handling moved here
                ;

    }

    /**
     * Updates an existing user.
     *
     * @param username    The username of the user to update.
     * @param updatedUser The User object containing the updated information.
     * @return A Mono emitting the updated User object, or an error signal if the user is not found.
     */
    public Mono<User> updateUser(String username, User updatedUser) {
        return userRepository.findByUsername(username)
                .flatMap(existingUser -> {
                    existingUser.setUsername(updatedUser.getUsername());
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }
                    existingUser.setRoles(updatedUser.getRoles());
                    existingUser.setActive(updatedUser.isActive());
                    return userRepository.save(existingUser);
                })
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)));
    }

    /**
     * Deletes a user by their username.
     *
     * @param username The username of the user to delete.
     * @return A Mono<Void> signaling completion, or an error signal if the user is not found.
     */
    public Mono<Void> deleteUser(String username) {
        return userRepository.findByUsername(username)
                .flatMap(userRepository::delete)
                .onErrorResume(UsernameNotFoundException.class,
                        ex -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage())));
    }
}
