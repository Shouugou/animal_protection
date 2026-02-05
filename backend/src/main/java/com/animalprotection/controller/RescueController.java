package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.dto.AnimalCreateRequest;
import com.animalprotection.dto.InventoryTxnRequest;
import com.animalprotection.dto.MedicalRecordRequest;
import com.animalprotection.dto.RescueDispatchRequest;
import com.animalprotection.dto.RescueEvaluateRequest;
import com.animalprotection.service.RescueService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rescue")
public class RescueController {
    private final RescueService rescueService;

    public RescueController(RescueService rescueService) {
        this.rescueService = rescueService;
    }

    @GetMapping("/tasks")
    public ApiResponse<?> tasks() {
        return ApiResponse.ok(rescueService.tasks());
    }

    @PostMapping("/tasks/{id}/evaluate")
    public ApiResponse<?> evaluate(@PathVariable Long id, @RequestBody RescueEvaluateRequest request) {
        rescueService.evaluate(id, request);
        return ApiResponse.ok(true);
    }

    @PostMapping("/tasks/{id}/dispatch")
    public ApiResponse<?> dispatch(@PathVariable Long id, @RequestBody RescueDispatchRequest request) {
        rescueService.dispatch(id, request);
        return ApiResponse.ok(true);
    }

    @PostMapping("/animals")
    public ApiResponse<?> createAnimal(@RequestBody AnimalCreateRequest request) {
        return ApiResponse.ok(rescueService.createAnimal(request));
    }

    @GetMapping("/animals")
    public ApiResponse<?> animals() {
        return ApiResponse.ok(rescueService.animals());
    }

    @PostMapping("/medical-records")
    public ApiResponse<?> medicalRecords(@RequestBody MedicalRecordRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.addMedicalRecord(request, userId));
    }

    @GetMapping("/inventory/items")
    public ApiResponse<?> inventoryItems(@RequestParam(defaultValue = "0") Long orgId) {
        return ApiResponse.ok(rescueService.inventoryItems(orgId));
    }

    @PostMapping("/inventory/txns")
    public ApiResponse<?> inventoryTxns(@RequestBody InventoryTxnRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        return ApiResponse.ok(rescueService.addInventoryTxn(request, userId));
    }
}
