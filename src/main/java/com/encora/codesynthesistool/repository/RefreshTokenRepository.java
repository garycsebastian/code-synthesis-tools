package com.encora.codesynthesistool.repository;

import com.encora.codesynthesistool.model.RefreshToken;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveMongoRepository<RefreshToken, String> {
    Mono<RefreshToken> findByToken(String token);

    Mono<RefreshToken> findByUsername(String token);

    Mono<Void> deleteByUsername(String username);

    Mono<Void> deleteByToken(String token);
}
