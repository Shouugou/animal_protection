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
        return ApiResponse.ok(publicService.claimTask(id, userId));
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
