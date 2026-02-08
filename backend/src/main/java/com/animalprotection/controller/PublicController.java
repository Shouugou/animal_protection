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
        return ApiResponse.ok(publicService.adoptionListings());
    }

    @GetMapping("/adoption-listings")
    public ApiResponse<?> adoptionListings() {
        return ApiResponse.ok(publicService.adoptionListings());
    }

    @GetMapping("/adoption-listings/{id}")
    public ApiResponse<?> adoptionListingDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.adoptionListingDetail(id));
    }

    @PostMapping("/adoption-listings/{id}/apply")
    public ApiResponse<?> applyAdoption(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        String applyForm = body.get("applyForm") == null ? "{}" : body.get("applyForm").toString();
        Long adoptionId = publicService.applyAdoption(id, applyForm, userId);
        if (adoptionId == null) {
            return ApiResponse.error("领养申请失败");
        }
        return ApiResponse.ok(adoptionId);
    }

    @PostMapping("/adoptions")
    public ApiResponse<?> adoptions(@RequestBody AdoptionRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long listingId = request.getAnimalId();
        if (listingId != null) {
            Long found = publicService.findListingIdByAnimal(listingId);
            if (found != null) {
                listingId = found;
            }
        }
        if (listingId == null) {
            return ApiResponse.error("领养申请参数错误");
        }
        Long adoptionId = publicService.applyAdoption(listingId, request.getApplyForm(), userId);
        if (adoptionId == null) {
            return ApiResponse.error("领养申请失败");
        }
        return ApiResponse.ok(adoptionId);
    }

    @GetMapping("/adoptions/my")
    public ApiResponse<?> myAdoptions() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.myAdoptions(userId));
    }

    @GetMapping("/followups")
    public ApiResponse<?> followups() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.followupTasks(userId, null));
    }

    @GetMapping("/followup-tasks")
    public ApiResponse<?> followupTasks(@RequestParam(required = false) String status) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.followupTasks(userId, status));
    }

    @GetMapping("/followup-tasks/{id}")
    public ApiResponse<?> followupTaskDetail(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.followupTaskDetail(id, userId));
    }

    @PostMapping("/followups")
    public ApiResponse<?> followupSubmit(@RequestBody FollowupRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long taskId = request.getAdoptionId();
        if (taskId == null) {
            return ApiResponse.error("回访参数错误");
        }
        Long id = publicService.submitFollowupTask(taskId, request.getQuestionnaire(), request.getAttachments(), userId);
        if (id == null) {
            return ApiResponse.error("回访提交失败");
        }
        return ApiResponse.ok(id);
    }

    @PostMapping("/followup-tasks/{id}/submit")
    public ApiResponse<?> followupTaskSubmit(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        String questionnaire = body.get("questionnaire") == null ? "{}" : body.get("questionnaire").toString();
        @SuppressWarnings("unchecked")
        java.util.List<String> attachments = (java.util.List<String>) body.get("attachments");
        Long followupId = publicService.submitFollowupTask(id, questionnaire, attachments, userId);
        if (followupId == null) {
            return ApiResponse.error("回访提交失败");
        }
        return ApiResponse.ok(followupId);
    }

    @PostMapping("/donations")
    public ApiResponse<?> donations(@RequestBody DonationRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long id = publicService.donate(request, userId);
        if (id == null) {
            return ApiResponse.error("捐赠目标不存在或参数错误");
        }
        return ApiResponse.ok(id);
    }

    @GetMapping("/donation-targets/events")
    public ApiResponse<?> donationEvents() {
        return ApiResponse.ok(publicService.donationEvents());
    }

    @GetMapping("/donation-targets/orgs")
    public ApiResponse<?> donationOrganizations() {
        return ApiResponse.ok(publicService.donationOrganizations());
    }

    @GetMapping("/donations/my")
    public ApiResponse<?> myDonations() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.myDonations(userId));
    }

    @GetMapping("/donations/org")
    public ApiResponse<?> orgDonations() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.orgDonations(userId));
    }

    @GetMapping("/content/categories")
    public ApiResponse<?> contentCategories() {
        return ApiResponse.ok(publicService.contentCategories());
    }

    @GetMapping("/content")
    public ApiResponse<?> contentList(@RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(publicService.contentList(categoryId));
    }

    @GetMapping("/content/{id}")
    public ApiResponse<?> contentDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.contentDetail(id));
    }

    @GetMapping("/content/my")
    public ApiResponse<?> myContent() {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(publicService.myContent(userId));
    }

    @PostMapping("/content")
    public ApiResponse<?> createContent(@RequestBody com.animalprotection.dto.ContentCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        Long id = publicService.createContent(request, userId, "PUBLIC");
        if (id == null) {
            return ApiResponse.error("该分类未配置审核流程或参数错误");
        }
        return ApiResponse.ok(id);
    }

    @DeleteMapping("/content/{id}")
    public ApiResponse<?> deleteMyContent(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        publicService.deleteMyContent(id, userId);
        return ApiResponse.ok(true);
    }
}
