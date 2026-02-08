package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.*;
import com.animalprotection.service.PublicService;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicController {
    private final PublicService publicService;

    public PublicController(PublicService publicService) {
        this.publicService = publicService;
    }

    @GetMapping("/messages")
    public ApiResponse<?> messages() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.messages(userId));
    }

    @GetMapping("/tasks")
    public ApiResponse<?> tasks() {
        return ApiResponse.ok(publicService.tasks());
    }

    @PostMapping("/tasks/{id}/claim")
    public ApiResponse<?> claim(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        java.util.Map<String, Object> result = publicService.claimTask(id, userId);
        Object ok = result.get("ok");
        if (ok instanceof Boolean && !(Boolean) ok) {
            return ApiResponse.error(result.get("message") == null ? "认领失败" : result.get("message").toString());
        }
        return ApiResponse.ok(result.get("claimId"));
    }

    @PostMapping("/volunteer/register")
    public ApiResponse<?> registerVolunteer() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        publicService.registerVolunteer(userId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/task-claims")
    public ApiResponse<?> myTaskClaims() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.myTaskClaims(userId));
    }

    @GetMapping("/task-claims/{id}")
    public ApiResponse<?> myTaskClaimDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.myTaskClaimDetail(id, userId));
    }

    @PostMapping("/task-claims/{id}/start")
    public ApiResponse<?> start(@PathVariable Long id) {
        publicService.startClaim(id);
        return ApiResponse.ok(true);
    }

    @PostMapping("/patrol-reports")
    public ApiResponse<?> patrolReports(@RequestBody PatrolReportRequest request) {
        return ApiResponse.ok(publicService.createPatrolReport(request));
    }

    @PostMapping("/rescue-support-reports")
    public ApiResponse<?> rescueSupportReports(@RequestBody RescueSupportReportRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.createRescueSupportReport(request, userId));
    }

    @GetMapping("/animals")
    public ApiResponse<?> animals() {
        return ApiResponse.ok(publicService.animals());
    }

    @PostMapping("/adoptions")
    public ApiResponse<?> adoptions(@RequestBody AdoptionRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.createAdoption(request, userId));
    }

    @GetMapping("/followups")
    public ApiResponse<?> followups() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.followups(userId));
    }

    @PostMapping("/followups")
    public ApiResponse<?> followupSubmit(@RequestBody FollowupRequest request) {
        return ApiResponse.ok(publicService.submitFollowup(request));
    }

    @PostMapping("/donations")
    public ApiResponse<?> donations(@RequestBody DonationRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.donate(request, userId));
    }

    @GetMapping("/content")
    public ApiResponse<?> contentList() {
        return ApiResponse.ok(publicService.contentList());
    }

    @GetMapping("/content/{id}")
    public ApiResponse<?> contentDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.contentDetail(id));
    }
}
