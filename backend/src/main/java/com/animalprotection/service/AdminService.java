package com.animalprotection.service;

import com.animalprotection.dto.AdminUserRequest;
import com.animalprotection.dto.ApprovalFlowRequest;
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
