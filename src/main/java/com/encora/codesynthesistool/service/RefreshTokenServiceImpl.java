package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.RefreshToken;
import com.encora.codesynthesistool.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Mono<Void> delete(RefreshToken token) {
        return refreshTokenRepository.delete(token);
    }

    @Override
    public Mono<Void> deleteByToken(String token) {
        return refreshTokenRepository.deleteByToken(token);
    }

    @Override
    public Mono<Void> deleteByUsername(String username) {
        return refreshTokenRepository.deleteByUsername(username);
    }
}
