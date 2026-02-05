package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.ApprovalFlowRequest;
import com.animalprotection.dto.RolePermissionRequest;
import com.animalprotection.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard/metrics")
    public ApiResponse<?> metrics() {
        return ApiResponse.ok(adminService.metrics());
    }

    @GetMapping("/dashboard/map")
    public ApiResponse<?> map() {
        return ApiResponse.ok(Collections.emptyList());
    }

    @GetMapping("/reports")
    public ApiResponse<?> reports() {
        return ApiResponse.ok(adminService.reports());
    }

    @GetMapping("/permissions")
    public ApiResponse<?> permissions() {
        return ApiResponse.ok(adminService.permissions());
    }

    @PostMapping("/role-permissions")
    public ApiResponse<?> rolePermissions(@RequestBody RolePermissionRequest request) {
        adminService.saveRolePermissions(request);
        return ApiResponse.ok(true);
    }

    @GetMapping("/approval-flows")
    public ApiResponse<?> approvalFlows() {
        return ApiResponse.ok(adminService.approvalFlows());
    }

    @PostMapping("/approval-flows")
    public ApiResponse<?> approvalFlowsSave(@RequestBody ApprovalFlowRequest request) {
        adminService.saveApprovalFlow(request);
        return ApiResponse.ok(true);
    }
}
