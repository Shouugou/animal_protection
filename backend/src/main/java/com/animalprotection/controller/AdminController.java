package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.AdminUserRequest;
import com.animalprotection.dto.ApprovalFlowRequest;
import com.animalprotection.dto.ContentCategoryRequest;
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
        return ApiResponse.ok(adminService.recentEvents());
    }

    @GetMapping("/reports")
    public ApiResponse<?> reports(@RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(adminService.reports(startDate, endDate));
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
        try {
            adminService.saveApprovalFlow(request);
            return ApiResponse.ok(true);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(ex.getMessage());
        }
    }

    @DeleteMapping("/approval-flows/{id}")
    public ApiResponse<?> deleteApprovalFlow(@PathVariable Long id) {
        adminService.deleteApprovalFlow(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/content-categories")
    public ApiResponse<?> contentCategories() {
        return ApiResponse.ok(adminService.contentCategories());
    }

    @PostMapping("/content-categories")
    public ApiResponse<?> saveContentCategory(@RequestBody ContentCategoryRequest request) {
        try {
            adminService.saveContentCategory(request);
            return ApiResponse.ok(true);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(ex.getMessage());
        }
    }

    @DeleteMapping("/content-categories/{id}")
    public ApiResponse<?> deleteContentCategory(@PathVariable Long id) {
        adminService.deleteContentCategory(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/content-approvals")
    public ApiResponse<?> contentApprovals(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.contentApprovalsByRole(status, "ADMIN", null));
    }

    @PostMapping("/content-approvals/{id}/approve")
    public ApiResponse<?> approveContent(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        adminService.approveContentByRole(id, "ADMIN", null);
        return ApiResponse.ok(true);
    }

    @PostMapping("/content-approvals/{id}/reject")
    public ApiResponse<?> rejectContent(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        String note = body == null ? null : (body.get("note") == null ? null : body.get("note").toString());
        adminService.rejectContentByRole(id, "ADMIN", null, note);
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
