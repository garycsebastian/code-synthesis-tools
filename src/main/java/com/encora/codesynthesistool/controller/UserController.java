package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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


    @PutMapping("/{username}")
    public Mono<ResponseEntity<User>> updateUser(@PathVariable String username, @RequestBody User updatedUser) {
        return userDetailsService.updateUser(username, updatedUser)
                .map(ResponseEntity::ok)
                .onErrorMap(UsernameNotFoundException.class,
                        ex -> new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{username}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable String username) {
        return userDetailsService.deleteUser(username)
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build()) // 204 No Content on success
                .onErrorMap(ResponseStatusException.class, ex -> ex) // Re-throw for Spring to handle
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build()); // Explicit 404 with <Void>
    }
}
