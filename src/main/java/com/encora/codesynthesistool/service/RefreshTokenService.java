package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.RefreshToken;
import reactor.core.publisher.Mono;

public interface RefreshTokenService {

    Mono<Void> delete(RefreshToken token);

    Mono<Void> deleteByToken(String token);

    Mono<Void> deleteByUsername(String username);
}
