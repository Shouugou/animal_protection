package com.animalprotection.service;

import com.animalprotection.dto.AdminUserRequest;
import com.animalprotection.dto.ApprovalFlowRequest;
import com.animalprotection.dto.ContentCategoryRequest;
import com.animalprotection.dto.OrganizationRequest;
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
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ap_event WHERE deleted_at IS NULL", Long.class);
        Long today = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_event WHERE deleted_at IS NULL AND DATE(reported_at) = CURDATE()",
                Long.class
        );
        Long processing = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_event WHERE deleted_at IS NULL AND status IN ('REPORTED','TRIAGED','DISPATCHED','PROCESSING','RESCUE_PROCESSING')",
                Long.class
        );
        Long closed = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_event WHERE deleted_at IS NULL AND status = 'CLOSED'",
                Long.class
        );
        data.put("total", total);
        data.put("today", today);
        data.put("processing", processing);
        data.put("closed", closed);
        Double donationAmount = jdbcTemplate.queryForObject(
                "SELECT IFNULL(SUM(amount),0) FROM ap_donation",
                Double.class
        );
        data.put("donation_amount", donationAmount);
        Long adoptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_adoption WHERE status = 'APPROVED'",
                Long.class
        );
        data.put("adoption_count", adoptionCount);
        return data;
    }

    public Map<String, Object> reports(String startDate, String endDate) {
        String eventWhere = "WHERE deleted_at IS NULL ";
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (startDate != null && !startDate.trim().isEmpty()) {
            eventWhere += "AND reported_at >= ? ";
            args.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            eventWhere += "AND reported_at <= ? ";
            args.add(endDate + " 23:59:59");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("byType", jdbcTemplate.queryForList(
                "SELECT event_type, COUNT(1) AS cnt FROM ap_event " + eventWhere + " GROUP BY event_type",
                args.toArray()
        ));
        result.put("byStatus", jdbcTemplate.queryForList(
                "SELECT status, COUNT(1) AS cnt FROM ap_event " + eventWhere + " GROUP BY status",
                args.toArray()
        ));
        result.put("byDay", jdbcTemplate.queryForList(
                "SELECT DATE(reported_at) AS day, COUNT(1) AS cnt FROM ap_event " + eventWhere + " GROUP BY DATE(reported_at) ORDER BY day",
                args.toArray()
        ));
        String donationWhere = "WHERE 1=1 ";
        java.util.List<Object> dArgs = new java.util.ArrayList<>();
        if (startDate != null && !startDate.trim().isEmpty()) {
            donationWhere += "AND donated_at >= ? ";
            dArgs.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            donationWhere += "AND donated_at <= ? ";
            dArgs.add(endDate + " 23:59:59");
        }
        result.put("donationByTarget", jdbcTemplate.queryForList(
                "SELECT target_type, COUNT(1) AS cnt, IFNULL(SUM(amount),0) AS amount FROM ap_donation " + donationWhere + " GROUP BY target_type",
                dArgs.toArray()
        ));
        result.put("adoptionByStatus", jdbcTemplate.queryForList(
                "SELECT status, COUNT(1) AS cnt FROM ap_adoption GROUP BY status"
        ));
        return result;
    }

    public List<Map<String, Object>> recentEvents() {
        return jdbcTemplate.queryForList(
                "SELECT id, event_type, status, address, latitude, longitude, reported_at " +
                        "FROM ap_event WHERE deleted_at IS NULL ORDER BY reported_at DESC LIMIT 20"
        );
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
        return jdbcTemplate.queryForList(
                "SELECT id, flow_code, flow_name, biz_type, steps FROM ap_approval_flow WHERE status = 1"
        );
    }

    public void saveApprovalFlow(ApprovalFlowRequest request) {
        Integer dup = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_approval_flow WHERE flow_code = ? AND (? IS NULL OR id <> ?)",
                Integer.class,
                request.getFlowCode(),
                request.getId(),
                request.getId()
        );
        if (dup != null && dup > 0) {
            throw new IllegalArgumentException("流程编码已存在");
        }
        if (request.getId() == null) {
            jdbcTemplate.update("INSERT INTO ap_approval_flow (flow_code, flow_name, biz_type, steps, status, created_at, updated_at) VALUES (?, ?, ?, ?, 1, NOW(3), NOW(3))",
                    request.getFlowCode(), request.getFlowName(), request.getBizType(), request.getSteps());
        } else {
            jdbcTemplate.update("UPDATE ap_approval_flow SET flow_code = ?, flow_name = ?, biz_type = ?, steps = ?, updated_at = NOW(3) WHERE id = ?",
                    request.getFlowCode(), request.getFlowName(), request.getBizType(), request.getSteps(), request.getId());
        }
    }

    public void deleteApprovalFlow(Long id) {
        jdbcTemplate.update("DELETE FROM ap_approval_flow WHERE id = ?", id);
    }

    public List<Map<String, Object>> contentCategories() {
        return jdbcTemplate.queryForList(
                "SELECT c.id, c.name, c.sort_no, c.status, c.approval_flow_id, f.flow_name " +
                        "FROM ap_content_category c " +
                        "LEFT JOIN ap_approval_flow f ON c.approval_flow_id = f.id " +
                        "ORDER BY c.sort_no ASC, c.id ASC"
        );
    }

    public void saveContentCategory(ContentCategoryRequest request) {
        Integer dup = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_content_category WHERE name = ? AND (? IS NULL OR id <> ?)",
                Integer.class,
                request.getName(),
                request.getId(),
                request.getId()
        );
        if (dup != null && dup > 0) {
            throw new IllegalArgumentException("分类名称已存在");
        }
        if (request.getId() == null) {
            jdbcTemplate.update("INSERT INTO ap_content_category (name, sort_no, status, approval_flow_id, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, NOW(3), NOW(3))",
                    request.getName(),
                    request.getSortNo() == null ? 0 : request.getSortNo(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    request.getApprovalFlowId());
        } else {
            jdbcTemplate.update("UPDATE ap_content_category SET name = ?, sort_no = ?, status = ?, approval_flow_id = ?, updated_at = NOW(3) WHERE id = ?",
                    request.getName(),
                    request.getSortNo() == null ? 0 : request.getSortNo(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    request.getApprovalFlowId(),
                    request.getId());
        }
    }

    public void deleteContentCategory(Long id) {
        jdbcTemplate.update("DELETE FROM ap_content_category WHERE id = ?", id);
    }

    public List<Map<String, Object>> contentApprovals(String status) {
        return contentApprovalsByRole(status, "ADMIN", null);
    }

    public void approveContent(Long instanceId, Long adminUserId) {
        processApproval(instanceId, "ADMIN", null, true, null);
    }

    public void rejectContent(Long instanceId, String note, Long adminUserId) {
        processApproval(instanceId, "ADMIN", null, false, note);
    }

    public List<Map<String, Object>> contentApprovalsByRole(String status, String roleCode, Long orgId) {
        StringBuilder sql = new StringBuilder(
                "SELECT i.id AS instance_id, i.status AS approval_status, i.current_step, i.created_at, " +
                        "f.steps, " +
                        "c.id AS content_id, c.title, c.content_type, c.cover_url, c.author_role, c.author_user_id, c.target_org_id, " +
                        "cat.name AS category_name, COALESCE(u.nickname, u.phone) AS author_name, " +
                        "o.name AS target_org_name " +
                        "FROM ap_approval_instance i " +
                        "JOIN ap_approval_flow f ON i.flow_id = f.id " +
                        "JOIN ap_content c ON i.biz_id = c.id " +
                        "LEFT JOIN ap_content_category cat ON c.category_id = cat.id " +
                        "LEFT JOIN ap_user u ON c.author_user_id = u.id " +
                        "LEFT JOIN ap_organization o ON c.target_org_id = o.id " +
                        "WHERE i.biz_type = 'CONTENT' "
        );
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND i.status = ? ");
            args.add(status);
        }
        sql.append("ORDER BY i.created_at DESC");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        java.util.List<Map<String, Object>> filtered = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object steps = row.get("steps");
            Object currentStep = row.get("current_step");
            String currentRole = resolveCurrentRole(steps, currentStep);
            Long currentOrgId = resolveCurrentOrgId(steps, currentStep);
            if (currentRole == null) {
                continue;
            }
            if (roleCode != null && !roleCode.equalsIgnoreCase(currentRole)) {
                continue;
            }
            if (( "LAW".equalsIgnoreCase(currentRole) || "RESCUE".equalsIgnoreCase(currentRole) )
                    && orgId != null) {
                if (currentOrgId == null || !currentOrgId.equals(orgId)) {
                    continue;
                }
            }
            row.put("current_role", currentRole);
            filtered.add(row);
        }
        return filtered;
    }

    public void approveContentByRole(Long instanceId, String roleCode, Long orgId) {
        processApproval(instanceId, roleCode, orgId, true, null);
    }

    public void rejectContentByRole(Long instanceId, String roleCode, Long orgId, String note) {
        processApproval(instanceId, roleCode, orgId, false, note);
    }

    private void processApproval(Long instanceId, String roleCode, Long orgId, boolean approve, String note) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT i.id, i.current_step, i.status, f.steps, c.target_org_id, c.id AS content_id " +
                        "FROM ap_approval_instance i " +
                        "JOIN ap_approval_flow f ON i.flow_id = f.id " +
                        "JOIN ap_content c ON i.biz_id = c.id " +
                        "WHERE i.id = ?",
                instanceId
        );
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        Object steps = row.get("steps");
        Object currentStep = row.get("current_step");
        String currentRole = resolveCurrentRole(steps, currentStep);
        Long currentOrgId = resolveCurrentOrgId(steps, currentStep);
        if (currentRole == null || (roleCode != null && !roleCode.equalsIgnoreCase(currentRole))) {
            return;
        }
        if (("LAW".equalsIgnoreCase(currentRole) || "RESCUE".equalsIgnoreCase(currentRole)) && orgId != null) {
            if (currentOrgId == null || !currentOrgId.equals(orgId)) {
                return;
            }
        }
        Long contentId = ((Number) row.get("content_id")).longValue();
        if (!approve) {
            jdbcTemplate.update("UPDATE ap_approval_instance SET status = 'REJECTED', updated_at = NOW(3) WHERE id = ?",
                    instanceId);
            jdbcTemplate.update("UPDATE ap_content SET status = 'REJECTED', updated_at = NOW(3) WHERE id = ?", contentId);
            return;
        }
        int nextStep = ((Number) row.get("current_step")).intValue() + 1;
        String nextRole = resolveCurrentRole(row.get("steps"), nextStep);
        if (nextRole == null) {
            jdbcTemplate.update("UPDATE ap_approval_instance SET status = 'APPROVED', updated_at = NOW(3) WHERE id = ?",
                    instanceId);
            jdbcTemplate.update("UPDATE ap_content SET status = 'PUBLISHED', published_at = NOW(3), updated_at = NOW(3) WHERE id = ?",
                    contentId);
        } else {
            jdbcTemplate.update("UPDATE ap_approval_instance SET current_step = ?, updated_at = NOW(3) WHERE id = ?",
                    nextStep, instanceId);
        }
    }

    private String resolveCurrentRole(Object stepsObj, Object currentStepObj) {
        if (stepsObj == null || currentStepObj == null) {
            return null;
        }
        int step = currentStepObj instanceof Number ? ((Number) currentStepObj).intValue() : 1;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> list = mapper.readValue(stepsObj.toString(), java.util.List.class);
            if (step <= 0 || step > list.size()) {
                return null;
            }
            Object role = list.get(step - 1).get("role");
            return role == null ? null : role.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveCurrentOrgId(Object stepsObj, Object currentStepObj) {
        if (stepsObj == null || currentStepObj == null) {
            return null;
        }
        int step = currentStepObj instanceof Number ? ((Number) currentStepObj).intValue() : 1;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> list = mapper.readValue(stepsObj.toString(), java.util.List.class);
            if (step <= 0 || step > list.size()) {
                return null;
            }
            Object orgId = list.get(step - 1).get("orgId");
            if (orgId instanceof Number) {
                return ((Number) orgId).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> organizations() {
        return jdbcTemplate.queryForList(
                "SELECT o.id, o.org_type, o.name, o.region_code, o.address, o.contact_name, o.contact_phone, " +
                        "o.admin_user_id, COALESCE(u.nickname, u.phone) AS admin_name, o.status, o.created_at " +
                        "FROM ap_organization o " +
                        "LEFT JOIN ap_user u ON o.admin_user_id = u.id " +
                        "WHERE o.deleted_at IS NULL ORDER BY o.id ASC"
        );
    }

    public Long createOrganization(OrganizationRequest request) {
        jdbcTemplate.update(
                "INSERT INTO ap_organization (org_type, name, region_code, address, contact_name, contact_phone, admin_user_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                request.getOrgType(),
                request.getName(),
                request.getRegionCode(),
                request.getAddress(),
                request.getContactName(),
                request.getContactPhone(),
                request.getAdminUserId(),
                request.getStatus() == null ? 1 : request.getStatus()
        );
        return jdbcTemplate.queryForObject("SELECT id FROM ap_organization WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, request.getName());
    }

    public void updateOrganization(OrganizationRequest request) {
        if (request.getId() == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE ap_organization SET org_type = ?, name = ?, region_code = ?, address = ?, contact_name = ?, contact_phone = ?, " +
                        "admin_user_id = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                request.getOrgType(),
                request.getName(),
                request.getRegionCode(),
                request.getAddress(),
                request.getContactName(),
                request.getContactPhone(),
                request.getAdminUserId(),
                request.getStatus() == null ? 1 : request.getStatus(),
                request.getId()
        );
    }

    public void deleteOrganization(Long id) {
        jdbcTemplate.update("UPDATE ap_organization SET deleted_at = NOW(3), updated_at = NOW(3) WHERE id = ?", id);
    }

    public List<Map<String, Object>> users(String roleCode, Long orgId, String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.role_code, u.org_id, u.phone, u.nickname, u.is_volunteer, u.status, u.created_at, " +
                        "o.name AS org_name " +
                        "FROM ap_user u " +
                        "LEFT JOIN ap_organization o ON u.org_id = o.id " +
                        "WHERE u.deleted_at IS NULL "
        );
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (roleCode != null && !roleCode.trim().isEmpty()) {
            sql.append("AND u.role_code = ? ");
            args.add(roleCode);
        }
        if (orgId != null) {
            sql.append("AND u.org_id = ? ");
            args.add(orgId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (u.phone LIKE ? OR u.nickname LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append("ORDER BY u.id ASC");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public Long createUser(AdminUserRequest request) {
        normalizeOrgByRole(request);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_user WHERE phone = ? AND deleted_at IS NULL",
                Integer.class,
                request.getPhone()
        );
        if (count != null && count > 0) {
            return null;
        }
        jdbcTemplate.update(
                "INSERT INTO ap_user (role_code, org_id, phone, password_hash, nickname, is_volunteer, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                request.getRoleCode(),
                request.getOrgId(),
                request.getPhone(),
                request.getPassword(),
                request.getNickname(),
                request.getIsVolunteer() == null ? 0 : request.getIsVolunteer(),
                request.getStatus() == null ? 1 : request.getStatus()
        );
        return jdbcTemplate.queryForObject("SELECT id FROM ap_user WHERE phone = ?", Long.class, request.getPhone());
    }

    public void updateUser(AdminUserRequest request) {
        if (request.getId() == null) {
            return;
        }
        normalizeOrgByRole(request);
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE ap_user SET role_code = ?, org_id = ?, phone = ?, password_hash = ?, nickname = ?, is_volunteer = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                    request.getRoleCode(),
                    request.getOrgId(),
                    request.getPhone(),
                    request.getPassword(),
                    request.getNickname(),
                    request.getIsVolunteer() == null ? 0 : request.getIsVolunteer(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    request.getId()
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE ap_user SET role_code = ?, org_id = ?, phone = ?, nickname = ?, is_volunteer = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                    request.getRoleCode(),
                    request.getOrgId(),
                    request.getPhone(),
                    request.getNickname(),
                    request.getIsVolunteer() == null ? 0 : request.getIsVolunteer(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    request.getId()
            );
        }
    }

    public void deleteUser(Long id) {
        jdbcTemplate.update("UPDATE ap_user SET deleted_at = NOW(3), updated_at = NOW(3) WHERE id = ?", id);
    }

    private void normalizeOrgByRole(AdminUserRequest request) {
        String role = request.getRoleCode();
        if (role == null) {
            return;
        }
        if ("PUBLIC".equalsIgnoreCase(role)) {
            request.setOrgId(null);
            return;
        }
        if (request.getOrgId() == null) {
            return;
        }
        String expectedOrgType = "ADMIN".equalsIgnoreCase(role) ? "PLATFORM" : role.toUpperCase();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_organization WHERE id = ? AND org_type = ? AND deleted_at IS NULL",
                Integer.class,
                request.getOrgId(),
                expectedOrgType
        );
        if (count == null || count == 0) {
            request.setOrgId(null);
        }
    }
}
