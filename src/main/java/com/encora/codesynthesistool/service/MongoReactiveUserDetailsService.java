package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Primary
@Slf4j
public class MongoReactiveUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        log.info("findByUsername: {}", username);
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .map(this::mapUserToUserDetails)
                .doOnSuccess(user -> log.info("User found: {}", user.getUsername()))
                .doOnError(error -> log.error("Error finding user: {}", error.getMessage()));
    }

    private UserDetails mapUserToUserDetails(User user) {
        // Si el usuario no tiene roles, se le asigna el rol "USER" por defecto
        List<GrantedAuthority> authorities = user.getRoles() == null || user.getRoles().isEmpty() ?
                List.of(new SimpleGrantedAuthority("ROLE_USER")) :
                user.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.isActive())
                .credentialsExpired(!user.isActive())
                .disabled(!user.isActive())
                .accountLocked(!user.isActive())
                .build();
    }

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

    public Mono<Void> deleteUser(String username) {
        return userRepository.findByUsername(username)
                .flatMap(userRepository::delete)
                .onErrorResume(UsernameNotFoundException.class,
                        ex -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage())));
    }
}
