package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.AdminUserRequest;
import com.animalprotection.dto.ApprovalFlowRequest;
import com.animalprotection.dto.OrganizationRequest;
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

    @GetMapping("/organizations")
    public ApiResponse<?> organizations() {
        return ApiResponse.ok(adminService.organizations());
    }

    @PostMapping("/organizations")
    public ApiResponse<?> createOrganization(@RequestBody OrganizationRequest request) {
        return ApiResponse.ok(adminService.createOrganization(request));
    }

    @PutMapping("/organizations/{id}")
    public ApiResponse<?> updateOrganization(@PathVariable Long id, @RequestBody OrganizationRequest request) {
        request.setId(id);
        adminService.updateOrganization(request);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/organizations/{id}")
    public ApiResponse<?> deleteOrganization(@PathVariable Long id) {
        adminService.deleteOrganization(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/users")
    public ApiResponse<?> users(@RequestParam(required = false) String roleCode,
                                @RequestParam(required = false) Long orgId,
                                @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.users(roleCode, orgId, keyword));
    }

    @PostMapping("/users")
    public ApiResponse<?> createUser(@RequestBody AdminUserRequest request) {
        return ApiResponse.ok(adminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<?> updateUser(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        request.setId(id);
        adminService.updateUser(request);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<?> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ApiResponse.ok(true);
    }
}
