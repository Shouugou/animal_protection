package com.animalprotection.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MockDataService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> loginResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("token", "mock-token");
        Map<String, Object> profile = new HashMap<>();
        profile.put("role_code", "PUBLIC");
        profile.put("user_id", 1);
        profile.put("org_id", null);
        profile.put("name", "演示用户");
        data.put("profile", profile);
        data.put("permCodes", new ArrayList<>());
        return data;
    }

    public Map<String, Object> eventDetail(Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("event_type", "受伤动物");
        data.put("urgency", "中");
        data.put("status", "REPORTED");
        data.put("address", "XX路");
        data.put("reported_at", now());
        return data;
    }

    public List<Map<String, Object>> eventList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", 10001);
        item.put("event_type", "受伤动物");
        item.put("status", "REPORTED");
        item.put("reported_at", now());
        list.add(item);
        return list;
    }

    public List<Map<String, Object>> timeline(Long eventId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", 1);
        item.put("node_type", "上报");
        item.put("content", "事件已上报");
        item.put("created_at", now());
        list.add(item);
        return list;
    }

    public List<Map<String, Object>> messages() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("id", 1);
        msg.put("title", "事件反馈");
        msg.put("content", "事件已受理");
        msg.put("created_at", now());
        msg.put("read", false);
        list.add(msg);
        return list;
    }

    public List<Map<String, Object>> workOrders() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", 3001);
        item.put("status", "NEW");
        item.put("need_law_enforcement", true);
        list.add(item);
        return list;
    }

    public Map<String, Object> workOrderDetail(Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("need_law_enforcement", true);
        data.put("transfer_to_rescue", false);
        data.put("result_text", "");
        return data;
    }

    public List<Map<String, Object>> rescueTasks() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", 5001);
        item.put("status", "NEW");
        item.put("need_rescue", true);
        list.add(item);
        return list;
    }

    public List<Map<String, Object>> animals() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", 9001);
        item.put("species", "犬");
        item.put("status", "IN_CARE");
        list.add(item);
        return list;
    }

    public List<Map<String, Object>> contentList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", 1);
        item.put("title", "如何科学救助流浪动物");
        item.put("type", "ARTICLE");
        list.add(item);
        return list;
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
