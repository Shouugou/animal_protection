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
        rescueService.dispatch(id, request, userId);
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
}
