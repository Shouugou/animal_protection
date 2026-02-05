package com.animalprotection.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    public String issueToken(Long userId) {
        String token = String.valueOf(userId);
        tokenStore.put(token, userId);
        return token;
    }

    public Long getUserId(String token) {
        Long userId = tokenStore.get(token);
        if (userId != null) {
            return userId;
        }
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
