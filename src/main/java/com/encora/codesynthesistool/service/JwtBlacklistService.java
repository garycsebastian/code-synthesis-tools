package com.encora.codesynthesistool.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class JwtBlacklistService {

    private final Cache blacklistedTokens;

    public JwtBlacklistService(CacheManager cacheManager) {
        this.blacklistedTokens = cacheManager.getCache("blacklistedTokens");
        if (this.blacklistedTokens == null) {
            throw new IllegalStateException("Cache 'blacklistedTokens' is not configured");
        }
    }

    public void blacklistToken(String token) {
        blacklistedTokens.put(token, true);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.get(token) != null;
    }
}