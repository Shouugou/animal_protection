package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.LoginRequest;
import com.animalprotection.dto.RegisterRequest;
import com.animalprotection.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        if (request.getPhone() == null || request.getPassword() == null) {
            return ApiResponse.error("账号或密码不能为空");
        }
        Object data = authService.login(request.getPhone(), request.getPassword());
        if (data == null) {
            return ApiResponse.error("账号或密码错误");
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        if (request.getPhone() == null || request.getPassword() == null) {
            return ApiResponse.error("账号或密码不能为空");
        }
        boolean ok = authService.registerPublicUser(request.getPhone(), request.getPassword(), request.getNickname());
        if (!ok) {
            return ApiResponse.error("账号已存在");
        }
        return ApiResponse.ok(true);
    }
}
