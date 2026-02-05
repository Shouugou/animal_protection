package com.animalprotection.service;

import com.animalprotection.dto.ApprovalFlowRequest;
import com.animalprotection.dto.RolePermissionRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final JdbcTemplate jdbcTemplate;

    public AdminService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> metrics() {
        Map<String, Object> data = new HashMap<>();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ap_event", Long.class);
        Long processing = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ap_event WHERE status = 'PROCESSING'", Long.class);
        Long closed = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ap_event WHERE status = 'CLOSED'", Long.class);
        data.put("total", total);
        data.put("processing", processing);
        data.put("closed", closed);
        return data;
    }

    public List<Map<String, Object>> reports() {
        return jdbcTemplate.queryForList("SELECT event_type, COUNT(1) AS cnt FROM ap_event GROUP BY event_type");
    }

    public List<Map<String, Object>> permissions() {
        return jdbcTemplate.queryForList("SELECT id, perm_code, perm_name FROM ap_permission");
    }

    public void saveRolePermissions(RolePermissionRequest request) {
        jdbcTemplate.update("DELETE FROM ap_role_permission WHERE role_id = ?", request.getRoleId());
        if (request.getPermIds() == null) return;
        for (Long permId : request.getPermIds()) {
            jdbcTemplate.update("INSERT INTO ap_role_permission (role_id, perm_id, created_at) VALUES (?, ?, NOW(3))",
                    request.getRoleId(), permId);
        }
    }

    public List<Map<String, Object>> approvalFlows() {
        return jdbcTemplate.queryForList("SELECT id, flow_code, flow_name, biz_type, steps FROM ap_approval_flow");
    }

    public void saveApprovalFlow(ApprovalFlowRequest request) {
        if (request.getId() == null) {
            jdbcTemplate.update("INSERT INTO ap_approval_flow (flow_code, flow_name, biz_type, steps, status, created_at, updated_at) VALUES (?, ?, ?, ?, 1, NOW(3), NOW(3))",
                    request.getFlowCode(), request.getFlowName(), request.getBizType(), request.getSteps());
        } else {
            jdbcTemplate.update("UPDATE ap_approval_flow SET flow_code = ?, flow_name = ?, biz_type = ?, steps = ?, updated_at = NOW(3) WHERE id = ?",
                    request.getFlowCode(), request.getFlowName(), request.getBizType(), request.getSteps(), request.getId());
        }
    }
}
