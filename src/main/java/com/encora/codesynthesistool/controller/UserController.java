package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public Mono<ResponseEntity<User>> registerUser(@RequestBody User user) {
        // Lógica para validar el usuario (por ejemplo, comprobar si el nombre de usuario ya existe)

        // Cifra la contraseña antes de guardarla
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user)
                .map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(savedUser));
    }
}