package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.LawArchiveRequest;
import com.animalprotection.dto.LawEvidenceRequest;
import com.animalprotection.dto.LawResultRequest;
import com.animalprotection.dto.VolunteerTaskCreateRequest;
import com.animalprotection.dto.WorkOrderAcceptRequest;
import com.animalprotection.dto.WorkOrderAssignRequest;
import com.animalprotection.dto.ContentCreateRequest;
import com.animalprotection.service.LawService;
import com.animalprotection.service.AdminService;
import com.animalprotection.service.PublicService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/law")
public class LawController {
    private final LawService lawService;
    private final PublicService publicService;
    private final AdminService adminService;

    public LawController(LawService lawService, PublicService publicService, AdminService adminService) {
        this.lawService = lawService;
        this.publicService = publicService;
        this.adminService = adminService;
    }

    @GetMapping("/workorders")
    public ApiResponse<?> list(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.workOrders(status, userId));
    }

    @GetMapping("/workorders/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.workOrderDetail(id, userId));
    }

    @PostMapping("/workorders/{id}/accept")
    public ApiResponse<?> accept(@PathVariable Long id, @RequestBody WorkOrderAcceptRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        lawService.accept(id, request, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/workorders/{id}/assign")
    public ApiResponse<?> assign(@PathVariable Long id, @RequestBody WorkOrderAssignRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        lawService.assign(id, request, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/evidence")
    public ApiResponse<?> evidence(@RequestBody LawEvidenceRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.addEvidence(request, userId));
    }

    @PostMapping("/workorders/{id}/result")
    public ApiResponse<?> result(@PathVariable Long id, @RequestBody LawResultRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        lawService.saveResult(id, request, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/workorders/{id}/archive")
    public ApiResponse<?> archive(@PathVariable Long id, @RequestBody LawArchiveRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        lawService.archive(id, request, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/workorders/{id}/transfer-to-rescue")
    public ApiResponse<?> transfer(@PathVariable Long id, @RequestParam(defaultValue = "0") Long rescueOrgId) {
        lawService.transferToRescue(id, rescueOrgId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/assignees")
    public ApiResponse<?> assignees() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.availableAssignees(userId));
    }

    @GetMapping("/my-workorders")
    public ApiResponse<?> myWorkOrders(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        if (userId == null) {
            return ApiResponse.error("未登录");
        }
        return ApiResponse.ok(lawService.workOrdersByAssignee(userId, status));
    }

    @GetMapping("/archived-workorders")
    public ApiResponse<?> archived(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.archivedWorkOrders(status, userId));
    }

    @GetMapping("/patrol-reports")
    public ApiResponse<?> patrolReports(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.patrolReports(userId, status));
    }

    @GetMapping("/patrol-reports/{id}")
    public ApiResponse<?> patrolReportDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.patrolReportDetail(id, userId));
    }

    @GetMapping("/volunteer-tasks")
    public ApiResponse<?> volunteerTasks() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.tasksByCreator("LAW", userId));
    }

    @GetMapping("/volunteer-tasks/{id}")
    public ApiResponse<?> volunteerTaskDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.taskDetailByCreator("LAW", userId, id));
    }

    @DeleteMapping("/volunteer-tasks/{id}")
    public ApiResponse<?> deleteVolunteerTask(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        publicService.deleteTaskByCreator("LAW", userId, id);
        return ApiResponse.ok(true);
    }

    @PostMapping("/volunteer-tasks")
    public ApiResponse<?> createVolunteerTask(@RequestBody VolunteerTaskCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.createTask("PATROL", "LAW", userId, request));
    }

    @GetMapping("/employees")
    public ApiResponse<?> employees() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.employees(userId));
    }

    @PostMapping("/employees")
    public ApiResponse<?> createEmployee(@RequestBody com.animalprotection.dto.EmployeeCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(lawService.createEmployee(request, userId));
    }

    @PutMapping("/employees/{id}")
    public ApiResponse<?> updateEmployee(@PathVariable Long id, @RequestBody com.animalprotection.dto.EmployeeUpdateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        lawService.updateEmployee(id, request, userId);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/employees/{id}")
    public ApiResponse<?> deleteEmployee(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        lawService.deleteEmployee(id, userId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/content")
    public ApiResponse<?> myContent() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.myContent(userId));
    }

    @PostMapping("/content")
    public ApiResponse<?> createContent(@RequestBody ContentCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long id = publicService.createContent(request, userId, "LAW");
        if (id == null) {
            return ApiResponse.error("内容发布失败");
        }
        return ApiResponse.ok(id);
    }

    @GetMapping("/content-approvals")
    public ApiResponse<?> contentApprovals(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long orgId = lawService.findOrgIdPublic(userId);
        return ApiResponse.ok(adminService.contentApprovalsByRole(status, "LAW", orgId));
    }

    @PostMapping("/content-approvals/{id}/approve")
    public ApiResponse<?> approveContent(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long orgId = lawService.findOrgIdPublic(userId);
        adminService.approveContentByRole(id, "LAW", orgId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/content-approvals/{id}/reject")
    public ApiResponse<?> rejectContent(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long orgId = lawService.findOrgIdPublic(userId);
        String note = body == null ? null : (body.get("note") == null ? null : body.get("note").toString());
        adminService.rejectContentByRole(id, "LAW", orgId, note);
        return ApiResponse.ok(true);
    }
}
