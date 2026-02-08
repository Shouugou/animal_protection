package com.animalprotection.service;

import com.animalprotection.dto.LawArchiveRequest;
import com.animalprotection.dto.LawEvidenceRequest;
import com.animalprotection.dto.LawResultRequest;
import com.animalprotection.dto.WorkOrderAcceptRequest;
import com.animalprotection.dto.WorkOrderAssignRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
public class LawService {
    private final JdbcTemplate jdbcTemplate;

    public LawService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> workOrders(String status, Long userId) {
        Long orgId = findOrgId(userId);
        StringBuilder sql = new StringBuilder(
                "SELECT wo.id, wo.event_id, wo.status, wo.need_law_enforcement, wo.transfer_to_rescue, " +
                        "wo.assignee_user_id, " +
                        "COALESCE(u.nickname, u.phone) AS assignee_name, " +
                        "e.event_type, e.address, e.reported_at, e.status AS event_status " +
                "FROM ap_work_order wo " +
                "LEFT JOIN ap_event e ON wo.event_id = e.id " +
                "LEFT JOIN ap_user u ON wo.assignee_user_id = u.id " +
                "WHERE wo.deleted_at IS NULL "
        );
        if (orgId != null) {
            sql.append("AND wo.law_org_id = ? ");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND wo.status = ? ");
            sql.append("ORDER BY wo.created_at DESC");
            if (orgId != null) {
                return jdbcTemplate.queryForList(sql.toString(), orgId, status);
            }
            return jdbcTemplate.queryForList(sql.toString(), status);
        }
        sql.append("ORDER BY wo.created_at DESC");
        if (orgId != null) {
            return jdbcTemplate.queryForList(sql.toString(), orgId);
        }
        return jdbcTemplate.queryForList(sql.toString());
    }

    public Map<String, Object> workOrderDetail(Long id, Long userId) {
        Long orgId = findOrgId(userId);
        Map<String, Object> data = jdbcTemplate.queryForMap(
                "SELECT wo.*, e.event_type, e.urgency, e.description, e.address, e.latitude, e.longitude, e.reported_at, e.status AS event_status, " +
                        "COALESCE(u.nickname, u.phone) AS assignee_name " +
                "FROM ap_work_order wo " +
                "LEFT JOIN ap_event e ON wo.event_id = e.id " +
                "LEFT JOIN ap_user u ON wo.assignee_user_id = u.id " +
                "WHERE wo.id = ?" + (orgId != null ? " AND wo.law_org_id = ?" : ""),
                orgId != null ? new Object[]{id, orgId} : new Object[]{id}
        );
        Object eventId = data.get("event_id");
        if (eventId != null) {
            List<Map<String, Object>> atts = jdbcTemplate.queryForList(
                    "SELECT file_url FROM ap_attachment WHERE biz_type = 'EVENT' AND biz_id = ? ORDER BY created_at ASC",
                    eventId
            );
            java.util.List<String> urls = new java.util.ArrayList<>();
            for (Map<String, Object> row : atts) {
                Object url = row.get("file_url");
                if (url != null) {
                    urls.add(url.toString());
                }
            }
            data.put("event_attachments", urls);
            List<Map<String, Object>> evRows = jdbcTemplate.queryForList(
                    "SELECT a.file_url FROM ap_attachment a WHERE a.biz_type = 'LAW_EVIDENCE' AND a.biz_id IN (" +
                            "SELECT le.id FROM ap_law_evidence le WHERE le.work_order_id IN (" +
                            "SELECT wo.id FROM ap_work_order wo WHERE wo.event_id = ?)) ORDER BY a.created_at ASC",
                    eventId
            );
            java.util.List<String> evUrls = new java.util.ArrayList<>();
            for (Map<String, Object> row : evRows) {
                Object url = row.get("file_url");
                if (url != null) {
                    evUrls.add(url.toString());
                }
            }
            data.put("evidence_attachments", evUrls);
        }
        return data;
    }

    public void accept(Long id, WorkOrderAcceptRequest request, Long operatorUserId) {
        Long orgId = findOrgId(operatorUserId);
        if (orgId == null || !belongsToOrg(id, orgId)) {
            return;
        }
        boolean needLaw = request.getNeedLawEnforcement() != null && request.getNeedLawEnforcement();
        boolean transfer = request.getTransferToRescue() != null && request.getTransferToRescue();
        if (!needLaw && transfer) {
            jdbcTemplate.update("UPDATE ap_work_order SET need_law_enforcement = 0, transfer_to_rescue = 1, status = 'TRANSFERRED', updated_at = NOW(3) WHERE id = ?",
                    id);
            jdbcTemplate.update("INSERT INTO ap_rescue_task (event_id, rescue_org_id, status, need_rescue, created_at, updated_at) VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), NULL, 'NEW', 1, NOW(3), NOW(3))",
                    id);
            jdbcTemplate.update("UPDATE ap_event SET status = 'DISPATCHED', updated_at = NOW(3) WHERE id = (SELECT event_id FROM ap_work_order WHERE id = ?)",
                    id);
            jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                            "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '推送救助', ?, 'LAW', ?, NOW(3))",
                    id, "已推送救助医疗机构", operatorUserId);
            return;
        }
        if (!needLaw && !transfer) {
            jdbcTemplate.update("UPDATE ap_work_order SET need_law_enforcement = 0, transfer_to_rescue = 0, status = 'REJECTED', updated_at = NOW(3) WHERE id = ?",
                    id);
            jdbcTemplate.update("UPDATE ap_event SET status = 'REJECTED', updated_at = NOW(3) WHERE id = (SELECT event_id FROM ap_work_order WHERE id = ?)",
                    id);
            jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                            "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '不予受理', ?, 'LAW', ?, NOW(3))",
                    id, "不予受理", operatorUserId);
            return;
        }
        jdbcTemplate.update("UPDATE ap_work_order SET need_law_enforcement = 1, transfer_to_rescue = ?, status = 'ACCEPTED', updated_at = NOW(3) WHERE id = ?",
                transfer ? 1 : 0, id);
        jdbcTemplate.update("UPDATE ap_event SET status = 'PROCESSING', updated_at = NOW(3) WHERE id = (SELECT event_id FROM ap_work_order WHERE id = ?)",
                id);
        String content = request.getResultText() == null || request.getResultText().trim().isEmpty() ? "已受理" : request.getResultText().trim();
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '受理', ?, 'LAW', ?, NOW(3))",
                id, content, operatorUserId);
    }

    public void assign(Long id, WorkOrderAssignRequest request, Long operatorUserId) {
        Long orgId = findOrgId(operatorUserId);
        if (orgId == null || !belongsToOrg(id, orgId)) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_user WHERE id = ? AND org_id = ? AND role_code = 'LAW' AND deleted_at IS NULL",
                Integer.class,
                request.getAssigneeUserId(),
                orgId
        );
        if (count == null || count == 0) {
            return;
        }
        jdbcTemplate.update("UPDATE ap_work_order SET assignee_user_id = ?, status = 'ASSIGNED', updated_at = NOW(3) WHERE id = ?",
                request.getAssigneeUserId(), id);
        String name = fetchUserDisplayName(request.getAssigneeUserId());
        String content = name == null ? "已分派执法人员" : ("已分派执法人员：" + name);
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '分派', ?, 'LAW', NOW(3))",
                id, content);
    }

    public Long addEvidence(LawEvidenceRequest request, Long collectorUserId) {
        Long orgId = findOrgId(collectorUserId);
        if (orgId == null || !belongsToOrg(request.getWorkOrderId(), orgId)) {
            return null;
        }
        String sql = "INSERT INTO ap_law_evidence (work_order_id, note, address, latitude, longitude, collected_at, collector_user_id, created_at) VALUES (?, ?, ?, ?, ?, NOW(3), ?, NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getWorkOrderId());
            ps.setString(2, request.getNote());
            ps.setString(3, request.getAddress());
            ps.setObject(4, request.getLatitude());
            ps.setObject(5, request.getLongitude());
            ps.setObject(6, collectorUserId);
            return ps;
        }, keyHolder);
        Long evidenceId = keyHolder.getKey().longValue();
        saveEvidenceAttachments(evidenceId, request.getAttachments(), collectorUserId);
        jdbcTemplate.update("UPDATE ap_work_order SET status = 'ON_SITE', updated_at = NOW(3) WHERE id = ?", request.getWorkOrderId());
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '取证', ?, 'LAW', ?, NOW(3))",
                request.getWorkOrderId(), request.getNote() == null ? "完成取证" : request.getNote(), collectorUserId);
        return evidenceId;
    }

    public void saveResult(Long workOrderId, LawResultRequest request, Long inputUserId) {
        Long orgId = findOrgId(inputUserId);
        if (orgId == null || !belongsToOrg(workOrderId, orgId)) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ap_law_result WHERE work_order_id = ?", Integer.class, workOrderId);
        if (count != null && count > 0) {
            jdbcTemplate.update("UPDATE ap_law_result SET result_text = ?, public_text = ?, published_at = NOW(3), updated_at = NOW(3) WHERE work_order_id = ?",
                    request.getResultText(), request.getPublicText(), workOrderId);
        } else {
            jdbcTemplate.update("INSERT INTO ap_law_result (work_order_id, result_text, public_text, published_at, input_user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(3), ?, NOW(3), NOW(3))",
                    workOrderId, request.getResultText(), request.getPublicText(), inputUserId);
        }
        jdbcTemplate.update("UPDATE ap_work_order SET status = 'FINISHED', updated_at = NOW(3) WHERE id = ?", workOrderId);
        String content = request.getPublicText() == null || request.getPublicText().trim().isEmpty() ? "已录入处理结果" : request.getPublicText();
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '结果录入', ?, 'LAW', ?, NOW(3))",
                workOrderId, content, inputUserId);
    }

    public void archive(Long workOrderId, LawArchiveRequest request, Long archiverUserId) {
        Long orgId = findOrgId(archiverUserId);
        if (orgId == null || !belongsToOrg(workOrderId, orgId)) {
            return;
        }
        jdbcTemplate.update("INSERT INTO ap_case_archive (work_order_id, archive_no, summary, archived_at, archiver_user_id, created_at) VALUES (?, ?, ?, NOW(3), ?, NOW(3))",
                workOrderId, request.getArchiveNo(), request.getSummary(), archiverUserId);
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '归档', ?, 'LAW', ?, NOW(3))",
                workOrderId, "工单已归档", archiverUserId);
        Integer transfer = jdbcTemplate.queryForObject(
                "SELECT transfer_to_rescue FROM ap_work_order WHERE id = ?",
                Integer.class,
                workOrderId
        );
        if (transfer != null && transfer == 1) {
            ensureRescueTask(workOrderId, null);
            jdbcTemplate.update("UPDATE ap_work_order SET status = 'TRANSFERRED', updated_at = NOW(3) WHERE id = ?", workOrderId);
            jdbcTemplate.update("UPDATE ap_event SET status = 'DISPATCHED', updated_at = NOW(3) WHERE id = (SELECT event_id FROM ap_work_order WHERE id = ?)",
                    workOrderId);
            jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                            "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '推送救助', ?, 'LAW', ?, NOW(3))",
                    workOrderId, "已推送救助医疗机构", archiverUserId);
        } else {
            jdbcTemplate.update("UPDATE ap_work_order SET status = 'ARCHIVED', updated_at = NOW(3) WHERE id = ?", workOrderId);
            jdbcTemplate.update("UPDATE ap_event SET status = 'CLOSED', updated_at = NOW(3) WHERE id = (SELECT event_id FROM ap_work_order WHERE id = ?)",
                    workOrderId);
        }
    }

    public void transferToRescue(Long workOrderId, Long rescueOrgId) {
        jdbcTemplate.update("UPDATE ap_work_order SET transfer_to_rescue = 1, updated_at = NOW(3) WHERE id = ?", workOrderId);
        jdbcTemplate.update("INSERT INTO ap_rescue_task (event_id, rescue_org_id, status, need_rescue, created_at, updated_at) VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), NULL, 'NEW', 1, NOW(3), NOW(3))",
                workOrderId);
    }

    public List<Map<String, Object>> availableAssignees(Long userId) {
        Long orgId = findOrgId(userId);
        String sql = "SELECT u.id, u.phone, u.nickname " +
                "FROM ap_user u " +
                "WHERE u.role_code = 'LAW' AND u.status = 1 AND u.deleted_at IS NULL " +
                (orgId != null ? "AND u.org_id = ? " : "") +
                "AND u.id NOT IN (" +
                "  SELECT DISTINCT assignee_user_id FROM ap_work_order " +
                "  WHERE assignee_user_id IS NOT NULL AND status IN ('ASSIGNED','ON_SITE') AND deleted_at IS NULL" +
                ") " +
                "ORDER BY u.id ASC";
        if (orgId != null) {
            return jdbcTemplate.queryForList(sql, orgId);
        }
        return jdbcTemplate.queryForList(sql);
    }

    private String fetchUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT nickname, phone FROM ap_user WHERE id = ?",
                userId
        );
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        Object nickname = row.get("nickname");
        if (nickname != null && !nickname.toString().trim().isEmpty()) {
            return nickname.toString();
        }
        Object phone = row.get("phone");
        return phone != null ? phone.toString() : null;
    }

    private void ensureRescueTask(Long workOrderId, Long rescueOrgId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_rescue_task WHERE event_id = (SELECT event_id FROM ap_work_order WHERE id = ?)",
                Integer.class,
                workOrderId
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO ap_rescue_task (event_id, rescue_org_id, status, need_rescue, created_at, updated_at) " +
                        "VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), ?, 'NEW', 1, NOW(3), NOW(3))",
                workOrderId, rescueOrgId);
    }

    public List<Map<String, Object>> workOrdersByAssignee(Long userId, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT wo.id, wo.event_id, wo.status, wo.need_law_enforcement, wo.transfer_to_rescue, " +
                        "wo.assignee_user_id, " +
                        "COALESCE(u.nickname, u.phone) AS assignee_name, " +
                        "e.event_type, e.address, e.reported_at, e.status AS event_status " +
                        "FROM ap_work_order wo " +
                        "LEFT JOIN ap_event e ON wo.event_id = e.id " +
                        "LEFT JOIN ap_user u ON wo.assignee_user_id = u.id " +
                        "WHERE wo.deleted_at IS NULL AND wo.assignee_user_id = ? "
        );
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND wo.status = ? ");
            sql.append("ORDER BY wo.created_at DESC");
            return jdbcTemplate.queryForList(sql.toString(), userId, status);
        }
        sql.append("ORDER BY wo.created_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), userId);
    }

    public List<Map<String, Object>> archivedWorkOrders(String status, Long userId) {
        Long orgId = findOrgId(userId);
        StringBuilder sql = new StringBuilder(
                "SELECT wo.id, wo.event_id, wo.status, wo.need_law_enforcement, wo.transfer_to_rescue, " +
                        "wo.assignee_user_id, " +
                        "COALESCE(u.nickname, u.phone) AS assignee_name, " +
                        "e.event_type, e.address, e.reported_at, e.status AS event_status " +
                "FROM ap_work_order wo " +
                "LEFT JOIN ap_event e ON wo.event_id = e.id " +
                "LEFT JOIN ap_user u ON wo.assignee_user_id = u.id " +
                "WHERE wo.deleted_at IS NULL "
        );
        if (orgId != null) {
            sql.append("AND wo.law_org_id = ? ");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND wo.status = ? ");
            sql.append("ORDER BY wo.created_at DESC");
            if (orgId != null) {
                return jdbcTemplate.queryForList(sql.toString(), orgId, status);
            }
            return jdbcTemplate.queryForList(sql.toString(), status);
        }
        sql.append("AND wo.status IN ('ARCHIVED','TRANSFERRED') ");
        sql.append("ORDER BY wo.created_at DESC");
        if (orgId != null) {
            return jdbcTemplate.queryForList(sql.toString(), orgId);
        }
        return jdbcTemplate.queryForList(sql.toString());
    }

    public List<Map<String, Object>> employees(Long userId) {
        Long orgId = findOrgId(userId);
        if (!isOrgAdmin(userId, "LAW")) {
            return java.util.Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT id, phone, nickname, status, created_at FROM ap_user " +
                        "WHERE role_code = 'LAW' AND org_id = ? AND deleted_at IS NULL ORDER BY id ASC",
                orgId
        );
    }

    public Long createEmployee(com.animalprotection.dto.EmployeeCreateRequest request, Long userId) {
        Long orgId = findOrgId(userId);
        if (!isOrgAdmin(userId, "LAW")) {
            return null;
        }
        String phone = request.getPhone();
        String password = request.getPassword() == null ? "123456" : request.getPassword();
        Integer status = request.getStatus() == null ? 1 : request.getStatus();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_user WHERE phone = ? AND deleted_at IS NULL",
                Integer.class,
                phone
        );
        if (count != null && count > 0) {
            return null;
        }
        jdbcTemplate.update(
                "INSERT INTO ap_user (role_code, org_id, phone, password_hash, nickname, status, created_at, updated_at) " +
                        "VALUES ('LAW', ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                orgId, phone, password, request.getNickname(), status
        );
        Long id = jdbcTemplate.queryForObject("SELECT id FROM ap_user WHERE phone = ?", Long.class, phone);
        return id;
    }

    public void updateEmployee(Long id, com.animalprotection.dto.EmployeeUpdateRequest request, Long userId) {
        Long orgId = findOrgId(userId);
        if (!isOrgAdmin(userId, "LAW")) {
            return;
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE ap_user SET nickname = ?, password_hash = ?, status = ?, updated_at = NOW(3) " +
                            "WHERE id = ? AND org_id = ? AND role_code = 'LAW' AND deleted_at IS NULL",
                    request.getNickname(),
                    request.getPassword(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    id,
                    orgId
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE ap_user SET nickname = ?, status = ?, updated_at = NOW(3) " +
                            "WHERE id = ? AND org_id = ? AND role_code = 'LAW' AND deleted_at IS NULL",
                    request.getNickname(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    id,
                    orgId
            );
        }
    }

    public void deleteEmployee(Long id, Long userId) {
        Long orgId = findOrgId(userId);
        if (!isOrgAdmin(userId, "LAW")) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE ap_user SET deleted_at = NOW(3), updated_at = NOW(3) WHERE id = ? AND org_id = ? AND role_code = 'LAW'",
                id, orgId
        );
    }

    private boolean isOrgAdmin(Long userId, String orgType) {
        if (userId == null) {
            return false;
        }
        Long orgId = findOrgId(userId);
        if (orgId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_organization WHERE id = ? AND org_type = ? AND admin_user_id = ? AND deleted_at IS NULL",
                Integer.class,
                orgId,
                orgType,
                userId
        );
        return count != null && count > 0;
    }

    private Long findOrgId(Long userId) {
        if (userId == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT org_id FROM ap_user WHERE id = ?", userId);
        if (!rows.isEmpty()) {
            Object orgId = rows.get(0).get("org_id");
            if (orgId instanceof Number) {
                return ((Number) orgId).longValue();
            }
        }
        return null;
    }

    public Long findOrgIdPublic(Long userId) {
        return findOrgId(userId);
    }

    public List<Map<String, Object>> patrolReports(Long userId, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.id AS claim_id, c.status AS claim_status, c.claimed_at, c.finished_at, " +
                        "t.id AS task_id, t.title, t.task_type, " +
                        "u.id AS volunteer_user_id, COALESCE(u.nickname, u.phone) AS volunteer_name, " +
                        "r.id AS report_id, r.summary, r.distance_km, r.duration_sec, r.submitted_at " +
                        "FROM ap_task_claim c " +
                        "JOIN ap_task t ON c.task_id = t.id " +
                        "LEFT JOIN ap_patrol_report r ON r.claim_id = c.id " +
                        "LEFT JOIN ap_user u ON c.user_id = u.id " +
                        "WHERE t.creator_role = 'LAW' AND t.creator_user_id = ? AND t.task_type = 'PATROL' "
        );
        if ("FINISHED".equalsIgnoreCase(status)) {
            sql.append("AND c.status = 'FINISHED' ");
        } else if ("UNFINISHED".equalsIgnoreCase(status)) {
            sql.append("AND c.status <> 'FINISHED' ");
        }
        sql.append("ORDER BY c.claimed_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), userId);
    }

    public Map<String, Object> patrolReportDetail(Long reportId, Long userId) {
        Map<String, Object> report = jdbcTemplate.queryForMap(
                "SELECT r.id, r.claim_id, r.summary, r.distance_km, r.duration_sec, r.submitted_at, " +
                        "t.id AS task_id, t.title, t.task_type, " +
                        "u.id AS volunteer_user_id, COALESCE(u.nickname, u.phone) AS volunteer_name " +
                        "FROM ap_patrol_report r " +
                        "JOIN ap_task_claim c ON r.claim_id = c.id " +
                        "JOIN ap_task t ON c.task_id = t.id " +
                        "LEFT JOIN ap_user u ON c.user_id = u.id " +
                        "WHERE r.id = ? AND t.creator_role = 'LAW' AND t.creator_user_id = ?",
                reportId, userId
        );
        List<Map<String, Object>> trackPoints = jdbcTemplate.queryForList(
                "SELECT seq_no, latitude, longitude, point_time FROM ap_patrol_track_point WHERE report_id = ? ORDER BY seq_no ASC",
                reportId
        );
        List<Map<String, Object>> riskPoints = jdbcTemplate.queryForList(
                "SELECT risk_type, description, address, latitude, longitude, found_at FROM ap_risk_point WHERE report_id = ? ORDER BY found_at DESC",
                reportId
        );
        report.put("track_points", trackPoints);
        report.put("risk_points", riskPoints);
        return report;
    }

    private void saveEvidenceAttachments(Long evidenceId, List<String> urls, Long uploaderUserId) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        for (String url : urls) {
            jdbcTemplate.update("INSERT INTO ap_attachment (biz_type, biz_id, file_url, uploader_user_id, created_at) VALUES ('LAW_EVIDENCE', ?, ?, ?, NOW(3))",
                    evidenceId, url, uploaderUserId);
        }
    }

    private boolean belongsToOrg(Long workOrderId, Long orgId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_work_order WHERE id = ? AND law_org_id = ? AND deleted_at IS NULL",
                Integer.class,
                workOrderId,
                orgId
        );
        return count != null && count > 0;
    }
}
