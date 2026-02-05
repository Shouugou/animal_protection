package com.animalprotection.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final TokenService tokenService;

    public AuthService(JdbcTemplate jdbcTemplate, TokenService tokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenService = tokenService;
    }

    public Map<String, Object> login(String phone, String password) {
        if (phone == null || password == null) {
            return null;
        }
        String p = phone.trim();
        String pwdInput = password.trim();
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id, role_code, org_id, phone, nickname, password_hash FROM ap_user WHERE phone = ? AND deleted_at IS NULL",
                p
        );
        if (list.isEmpty()) {
            return null;
        }
        Map<String, Object> user = list.get(0);
        String pwd = (String) user.get("password_hash");
        if (pwd == null || !pwd.equals(pwdInput)) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        Long userId = ((Number) user.get("id")).longValue();
        String token = tokenService.issueToken(userId);
        data.put("token", token);
        Map<String, Object> profile = new HashMap<>();
        profile.put("role_code", user.get("role_code"));
        profile.put("user_id", userId);
        profile.put("org_id", user.get("org_id"));
        profile.put("name", user.get("nickname") != null ? user.get("nickname") : user.get("phone"));
        data.put("profile", profile);
        data.put("permCodes", new java.util.ArrayList<>());
        return data;
    }

    public boolean registerPublicUser(String phone, String password, String nickname) {
        if (phone == null || password == null) {
            return false;
        }
        String p = phone.trim();
        String pwd = password.trim();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_user WHERE phone = ? AND deleted_at IS NULL",
                Integer.class,
                p
        );
        if (count != null && count > 0) {
            return false;
        }
        jdbcTemplate.update(
                "INSERT INTO ap_user (role_code, org_id, phone, password_hash, nickname, is_volunteer, status, created_at, updated_at) " +
                        "VALUES ('PUBLIC', NULL, ?, ?, ?, 0, 1, NOW(3), NOW(3))",
                p, pwd, nickname
        );
        return true;
    }
}
