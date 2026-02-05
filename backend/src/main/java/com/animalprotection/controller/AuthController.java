package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.LoginRequest;
import com.animalprotection.service.MockDataService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final MockDataService mockDataService;

    public AuthController(MockDataService mockDataService) {
        this.mockDataService = mockDataService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(mockDataService.loginResponse());
    }
}
