package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.AnimalCreateRequest;
import com.animalprotection.dto.AdoptionListingRequest;
import com.animalprotection.dto.InventoryTxnRequest;
import com.animalprotection.dto.MedicalRecordRequest;
import com.animalprotection.dto.RescueDispatchRequest;
import com.animalprotection.dto.RescueEvaluateRequest;
import com.animalprotection.dto.RescueVehicleRequest;
import com.animalprotection.dto.VolunteerTaskCreateRequest;
import com.animalprotection.dto.ContentCreateRequest;
import com.animalprotection.service.AdminService;
import com.animalprotection.service.PublicService;
import com.animalprotection.service.RescueService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rescue")
public class RescueController {
    private final RescueService rescueService;
    private final PublicService publicService;
    private final AdminService adminService;

    public RescueController(RescueService rescueService, PublicService publicService, AdminService adminService) {
        this.rescueService = rescueService;
        this.publicService = publicService;
        this.adminService = adminService;
    }

    @GetMapping("/tasks")
    public ApiResponse<?> tasks(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.tasks(userId, status));
    }

    @PostMapping("/tasks/{id}/grab")
    public ApiResponse<?> grab(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.grab(id, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/tasks/{id}/evaluate")
    public ApiResponse<?> evaluate(@PathVariable Long id, @RequestBody RescueEvaluateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.evaluate(id, request, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/tasks/{id}/dispatch")
    public ApiResponse<?> dispatch(@PathVariable Long id, @RequestBody RescueDispatchRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        boolean ok = rescueService.dispatch(id, request, userId);
        if (!ok) {
            return ApiResponse.error("调度人员或车辆已被占用");
        }
        return ApiResponse.ok(true);
    }

    @GetMapping("/assignees")
    public ApiResponse<?> assignees() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.assignees(userId));
    }

    @GetMapping("/assignees/available")
    public ApiResponse<?> availableAssignees() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.availableAssignees(userId));
    }

    @GetMapping("/vehicles")
    public ApiResponse<?> vehicles() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.vehicles(userId));
    }

    @GetMapping("/vehicles/available")
    public ApiResponse<?> availableVehicles() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.availableVehicles(userId));
    }

    @PostMapping("/vehicles")
    public ApiResponse<?> createVehicle(@RequestBody RescueVehicleRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.createVehicle(request, userId));
    }

    @PutMapping("/vehicles/{id}")
    public ApiResponse<?> updateVehicle(@PathVariable Long id, @RequestBody RescueVehicleRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.updateVehicle(id, request, userId);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/vehicles/{id}")
    public ApiResponse<?> deleteVehicle(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.deleteVehicle(id, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/animals")
    public ApiResponse<?> createAnimal(@RequestBody AnimalCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.createAnimal(request, userId));
    }

    @GetMapping("/animals")
    public ApiResponse<?> animals() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.animals(userId));
    }

    @PostMapping("/medical-records")
    public ApiResponse<?> medicalRecords(@RequestBody MedicalRecordRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.addMedicalRecord(request, userId));
    }

    @GetMapping("/medical-records")
    public ApiResponse<?> medicalRecordsList(@RequestParam Long animalId) {
        return ApiResponse.ok(rescueService.medicalRecords(animalId));
    }

    @PostMapping("/medical-records/share")
    public ApiResponse<?> share(@RequestBody com.animalprotection.dto.MedicalShareRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.shareMedicalRecord(request, userId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/organizations")
    public ApiResponse<?> rescueOrganizations() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.rescueOrgs(userId));
    }

    @PostMapping("/case-shares")
    public ApiResponse<?> createCaseShare(@RequestBody java.util.Map<String, Object> body) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long animalId = body.get("animalId") == null ? null : ((Number) body.get("animalId")).longValue();
        Long targetOrgId = body.get("targetOrgId") == null ? null : ((Number) body.get("targetOrgId")).longValue();
        String note = body.get("note") == null ? null : body.get("note").toString();
        return ApiResponse.ok(rescueService.createCaseShare(animalId, targetOrgId, note, userId));
    }

    @GetMapping("/case-shares")
    public ApiResponse<?> caseShares(@RequestParam(required = false) String direction) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.caseShares(userId, direction));
    }

    @GetMapping("/case-shares/{id}")
    public ApiResponse<?> caseShareDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.caseShareDetail(id, userId));
    }

    @PostMapping("/case-shares/{id}/messages")
    public ApiResponse<?> caseShareMessage(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        String content = body.get("content") == null ? "" : body.get("content").toString();
        rescueService.addCaseShareMessage(id, content, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/case-shares/{id}/close")
    public ApiResponse<?> closeCaseShare(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.closeCaseShare(id, userId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/inventory/items")
    public ApiResponse<?> inventoryItems() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.inventoryItems(userId));
    }

    @PostMapping("/inventory/items")
    public ApiResponse<?> createItem(@RequestBody com.animalprotection.dto.InventoryItemCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.createInventoryItem(request, userId));
    }

    @GetMapping("/inventory/txns")
    public ApiResponse<?> inventoryTxns(@RequestParam Long itemId) {
        return ApiResponse.ok(rescueService.inventoryTxns(itemId));
    }

    @GetMapping("/inventory/alerts")
    public ApiResponse<?> inventoryAlerts() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.inventoryAlerts(userId));
    }

    @PostMapping("/inventory/txns")
    public ApiResponse<?> inventoryTxns(@RequestBody InventoryTxnRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.addInventoryTxn(request, userId));
    }

    @DeleteMapping("/inventory/items/{id}")
    public ApiResponse<?> deleteItem(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.deleteInventoryItem(id, userId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/volunteer-tasks")
    public ApiResponse<?> volunteerTasks() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.tasksByCreator("RESCUE", userId));
    }

    @GetMapping("/volunteer-reports")
    public ApiResponse<?> volunteerReports(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.rescueSupportReportsByCreator(userId, status));
    }

    @GetMapping("/volunteer-reports/{id}")
    public ApiResponse<?> volunteerReportDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.rescueSupportReportDetail(id, userId));
    }

    @GetMapping("/volunteer-tasks/{id}")
    public ApiResponse<?> volunteerTaskDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.taskDetailByCreator("RESCUE", userId, id));
    }

    @DeleteMapping("/volunteer-tasks/{id}")
    public ApiResponse<?> deleteVolunteerTask(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        publicService.deleteTaskByCreator("RESCUE", userId, id);
        return ApiResponse.ok(true);
    }

    @PostMapping("/volunteer-tasks")
    public ApiResponse<?> createVolunteerTask(@RequestBody VolunteerTaskCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.createTask("RESCUE_SUPPORT", "RESCUE", userId, request));
    }

    @GetMapping("/employees")
    public ApiResponse<?> employees() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.employees(userId));
    }

    @PostMapping("/employees")
    public ApiResponse<?> createEmployee(@RequestBody com.animalprotection.dto.EmployeeCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.createEmployee(request, userId));
    }

    @PutMapping("/employees/{id}")
    public ApiResponse<?> updateEmployee(@PathVariable Long id, @RequestBody com.animalprotection.dto.EmployeeUpdateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.updateEmployee(id, request, userId);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/employees/{id}")
    public ApiResponse<?> deleteEmployee(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.deleteEmployee(id, userId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/adoption-listings")
    public ApiResponse<?> adoptionListings(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.adoptionListings(userId, status));
    }

    @PostMapping("/adoption-listings")
    public ApiResponse<?> createAdoptionListing(@RequestBody AdoptionListingRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long id = rescueService.createAdoptionListing(request, userId);
        if (id == null) {
            return ApiResponse.error("领养发布失败");
        }
        return ApiResponse.ok(id);
    }

    @DeleteMapping("/adoption-listings/{id}")
    public ApiResponse<?> deleteAdoptionListing(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        rescueService.deleteAdoptionListing(id, userId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/adoptions/{id}/followup")
    public ApiResponse<?> sendFollowup(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long taskId = rescueService.sendFollowupTask(id, userId);
        if (taskId == null) {
            return ApiResponse.error("发送回访失败");
        }
        return ApiResponse.ok(taskId);
    }

    @GetMapping("/adoptions/{id}/followup-detail")
    public ApiResponse<?> followupDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.followupDetail(id, userId));
    }

    @GetMapping("/content")
    public ApiResponse<?> myContent() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.myContent(userId));
    }

    @PostMapping("/content")
    public ApiResponse<?> createContent(@RequestBody ContentCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long id = publicService.createContent(request, userId, "RESCUE");
        if (id == null) {
            return ApiResponse.error("内容发布失败");
        }
        return ApiResponse.ok(id);
    }

    @GetMapping("/content-approvals")
    public ApiResponse<?> contentApprovals(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long orgId = rescueService.findOrgIdPublic(userId);
        return ApiResponse.ok(adminService.contentApprovalsByRole(status, "RESCUE", orgId));
    }

    @PostMapping("/content-approvals/{id}/approve")
    public ApiResponse<?> approveContent(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long orgId = rescueService.findOrgIdPublic(userId);
        adminService.approveContentByRole(id, "RESCUE", orgId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/content-approvals/{id}/reject")
    public ApiResponse<?> rejectContent(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long orgId = rescueService.findOrgIdPublic(userId);
        String note = body == null ? null : (body.get("note") == null ? null : body.get("note").toString());
        adminService.rejectContentByRole(id, "RESCUE", orgId, note);
        return ApiResponse.ok(true);
    }
}
