package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.LawArchiveRequest;
import com.animalprotection.dto.LawEvidenceRequest;
import com.animalprotection.dto.LawResultRequest;
import com.animalprotection.dto.WorkOrderAcceptRequest;
import com.animalprotection.dto.WorkOrderAssignRequest;
import com.animalprotection.service.LawService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/law")
public class LawController {
    private final LawService lawService;

    public LawController(LawService lawService) {
        this.lawService = lawService;
    }

    @GetMapping("/workorders")
    public ApiResponse<?> list() {
        return ApiResponse.ok(lawService.workOrders());
    }

    @GetMapping("/workorders/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(lawService.workOrderDetail(id));
    }

    @PostMapping("/workorders/{id}/accept")
    public ApiResponse<?> accept(@PathVariable Long id, @RequestBody WorkOrderAcceptRequest request) {
        lawService.accept(id, request);
        return ApiResponse.ok(true);
    }

    @PostMapping("/workorders/{id}/assign")
    public ApiResponse<?> assign(@PathVariable Long id, @RequestBody WorkOrderAssignRequest request) {
        lawService.assign(id, request);
        return ApiResponse.ok(true);
    }

    @PostMapping("/evidence")
    public ApiResponse<?> evidence(@RequestBody LawEvidenceRequest request) {
        return ApiResponse.ok(lawService.addEvidence(request, 1L));
    }

    @PostMapping("/workorders/{id}/result")
    public ApiResponse<?> result(@PathVariable Long id, @RequestBody LawResultRequest request) {
        lawService.saveResult(id, request, 1L);
        return ApiResponse.ok(true);
    }

    @PostMapping("/workorders/{id}/archive")
    public ApiResponse<?> archive(@PathVariable Long id, @RequestBody LawArchiveRequest request) {
        lawService.archive(id, request, 1L);
        return ApiResponse.ok(true);
    }

    @PostMapping("/workorders/{id}/transfer-to-rescue")
    public ApiResponse<?> transfer(@PathVariable Long id, @RequestParam(defaultValue = "0") Long rescueOrgId) {
        lawService.transferToRescue(id, rescueOrgId);
        return ApiResponse.ok(true);
    }
}
