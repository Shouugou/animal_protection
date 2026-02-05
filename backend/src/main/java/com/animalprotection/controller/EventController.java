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
        Long eventId = eventService.createEvent(request.getEventType(), request.getUrgency(), request.getDescription(),
                request.getAddress(), request.getLatitude(), request.getLongitude(), 1L);
        eventService.addTimeline(eventId, "上报", "事件已上报", 1L);
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
        return ApiResponse.ok(eventService.getEvent(id));
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<?> timeline(@PathVariable Long id) {
        return ApiResponse.ok(eventService.timeline(id));
    }

    @PostMapping("/{id}/supplements")
    public ApiResponse<?> supplement(@PathVariable Long id, @RequestBody EventSupplementRequest request) {
        eventService.addTimeline(id, "补充", request.getContent(), 1L);
        return ApiResponse.ok(true);
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<?> comments(@PathVariable Long id) {
        return ApiResponse.ok(eventService.comments(id));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<?> postComment(@PathVariable Long id, @RequestBody CommentRequest request) {
        eventService.addComment(id, 1L, request.getParentId(), request.getContent());
        return ApiResponse.ok(true);
    }
}
