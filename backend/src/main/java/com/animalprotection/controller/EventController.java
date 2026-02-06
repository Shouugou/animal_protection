package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import com.animalprotection.common.PageResponse;
import com.animalprotection.dto.CommentRequest;
import com.animalprotection.dto.EventCreateRequest;
import com.animalprotection.dto.EventSupplementRequest;
import com.animalprotection.service.EventService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody EventCreateRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        if (userId == null) {
            return ApiResponse.error("未登录");
        }
        Long eventId = eventService.createEvent(request.getEventType(), request.getUrgency(), request.getDescription(),
                request.getAddress(), request.getLatitude(), request.getLongitude(), userId);
        eventService.addTimeline(eventId, "上报", "事件已上报", userId);
        eventService.saveAttachments("EVENT", eventId, request.getAttachments(), userId);
        return ApiResponse.ok(eventService.getEvent(eventId));
    }

    @GetMapping
    public ApiResponse<PageResponse<?>> list(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") int pageNo,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (pageNo - 1) * pageSize;
        return ApiResponse.ok(new PageResponse<>(
                eventService.countEvents(status, keyword),
                eventService.listEvents(status, keyword, offset, pageSize)
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        java.util.Map<String, Object> data = eventService.getEvent(id);
        java.util.List<java.util.Map<String, Object>> atts = eventService.attachments("EVENT", id);
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> row : atts) {
            Object url = row.get("file_url");
            if (url != null) {
                urls.add(url.toString());
            }
        }
        data.put("attachments", urls);
        java.util.List<String> evidenceUrls = eventService.evidenceAttachmentsByEvent(id);
        data.put("evidence_attachments", evidenceUrls);
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<?> timeline(@PathVariable Long id) {
        return ApiResponse.ok(eventService.timeline(id));
    }

    @PostMapping("/{id}/supplements")
    public ApiResponse<?> supplement(@PathVariable Long id, @RequestBody EventSupplementRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        eventService.addTimeline(id, "补充", request.getContent(), userId);
        eventService.saveAttachments("EVENT", id, request.getAttachments(), userId);
        return ApiResponse.ok(true);
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<?> comments(@PathVariable Long id) {
        return ApiResponse.ok(eventService.comments(id));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<?> postComment(@PathVariable Long id, @RequestBody CommentRequest request) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        eventService.addComment(id, userId, request.getParentId(), request.getContent());
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        Long userId = com.animalprotection.common.AuthContext.getUserId();
        boolean deleted = eventService.deleteEvent(id, userId);
        if (!deleted) {
            return ApiResponse.error("无权限删除该事件");
        }
        return ApiResponse.ok(true);
    }
}
