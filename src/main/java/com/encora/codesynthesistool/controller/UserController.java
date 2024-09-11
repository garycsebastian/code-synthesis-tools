package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.service.MongoReactiveUserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final MongoReactiveUserDetailsService userDetailsService;

    @PostMapping("/register")
    public Mono<ResponseEntity<User>> registerUser(@RequestBody User user) {
        // Lógica para validar el usuario (por ejemplo, comprobar si el nombre de usuario ya existe)

        return userDetailsService.createUser(user)
                .map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(savedUser));
    }
}
