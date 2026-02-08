package com.animalprotection.service;

import com.animalprotection.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PublicService {
    private final JdbcTemplate jdbcTemplate;

    public PublicService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> messages(Long userId) {
        return jdbcTemplate.queryForList("SELECT id, title, content, created_at, read_at FROM ap_message WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    public List<Map<String, Object>> tasks() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT t.id, t.title, t.task_type, t.status, t.description, t.address, t.latitude, t.longitude, " +
                        "t.start_at, t.end_at, t.max_claims, t.creator_role, t.creator_user_id, t.created_at, " +
                        "(SELECT COUNT(1) FROM ap_task_claim c WHERE c.task_id = t.id) AS claimed_count " +
                "FROM ap_task t WHERE t.deleted_at IS NULL AND t.status = 'OPEN' ORDER BY t.created_at DESC"
        );
        for (Map<String, Object> row : list) {
            Number max = row.get("max_claims") instanceof Number ? (Number) row.get("max_claims") : 0;
            Number claimed = row.get("claimed_count") instanceof Number ? (Number) row.get("claimed_count") : 0;
            boolean canClaim = max.intValue() == 0 || claimed.intValue() < max.intValue();
            row.put("can_claim", canClaim);
        }
        return list;
    }

    public List<Map<String, Object>> tasksByCreator(String creatorRole, Long userId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT t.id, t.title, t.task_type, t.status, t.max_claims, t.creator_role, t.creator_user_id, t.created_at, " +
                        "(SELECT COUNT(1) FROM ap_task_claim c WHERE c.task_id = t.id) AS claimed_count " +
                        "FROM ap_task t WHERE t.deleted_at IS NULL AND t.creator_role = ? AND t.creator_user_id = ? " +
                        "ORDER BY t.created_at DESC",
                creatorRole, userId
        );
        for (Map<String, Object> row : list) {
            Number max = row.get("max_claims") instanceof Number ? (Number) row.get("max_claims") : 0;
            Number claimed = row.get("claimed_count") instanceof Number ? (Number) row.get("claimed_count") : 0;
            boolean canClaim = max.intValue() == 0 || claimed.intValue() < max.intValue();
            row.put("can_claim", canClaim);
        }
        return list;
    }

    public Map<String, Object> taskDetailByCreator(String creatorRole, Long userId, Long taskId) {
        return jdbcTemplate.queryForMap(
                "SELECT t.id, t.title, t.task_type, t.status, t.description, t.address, t.latitude, t.longitude, " +
                        "t.start_at, t.end_at, t.max_claims, t.creator_role, t.creator_user_id, t.created_at " +
                        "FROM ap_task t WHERE t.id = ? AND t.creator_role = ? AND t.creator_user_id = ? AND t.deleted_at IS NULL",
                taskId, creatorRole, userId
        );
    }

    public void deleteTaskByCreator(String creatorRole, Long userId, Long taskId) {
        jdbcTemplate.update(
                "UPDATE ap_task SET status = 'CANCELLED', deleted_at = NOW(3), updated_at = NOW(3) " +
                        "WHERE id = ? AND creator_role = ? AND creator_user_id = ? AND deleted_at IS NULL",
                taskId, creatorRole, userId
        );
        jdbcTemplate.update(
                "DELETE FROM ap_attachment WHERE biz_type = 'RESCUE_SUPPORT_REPORT' AND biz_id IN (" +
                        "SELECT id FROM ap_rescue_support_report WHERE claim_id IN (" +
                        "SELECT id FROM ap_task_claim WHERE task_id = ?))",
                taskId
        );
        jdbcTemplate.update(
                "DELETE FROM ap_rescue_support_report WHERE claim_id IN (SELECT id FROM ap_task_claim WHERE task_id = ?)",
                taskId
        );
        jdbcTemplate.update(
                "DELETE FROM ap_patrol_track_point WHERE report_id IN (" +
                        "SELECT id FROM ap_patrol_report WHERE claim_id IN (" +
                        "SELECT id FROM ap_task_claim WHERE task_id = ?))",
                taskId
        );
        jdbcTemplate.update(
                "DELETE FROM ap_risk_point WHERE report_id IN (" +
                        "SELECT id FROM ap_patrol_report WHERE claim_id IN (" +
                        "SELECT id FROM ap_task_claim WHERE task_id = ?))",
                taskId
        );
        jdbcTemplate.update(
                "DELETE FROM ap_patrol_report WHERE claim_id IN (SELECT id FROM ap_task_claim WHERE task_id = ?)",
                taskId
        );
        jdbcTemplate.update(
                "DELETE FROM ap_task_claim WHERE task_id = ?",
                taskId
        );
    }

    public java.util.Map<String, Object> claimTask(Long taskId, Long userId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        Integer volunteer = jdbcTemplate.queryForObject(
                "SELECT is_volunteer FROM ap_user WHERE id = ?",
                Integer.class,
                userId
        );
        if (volunteer == null || volunteer == 0) {
            result.put("ok", false);
            result.put("message", "请先注册志愿者");
            return result;
        }
        Integer taskCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_task WHERE id = ? AND status = 'OPEN' AND deleted_at IS NULL",
                Integer.class,
                taskId
        );
        if (taskCount == null || taskCount == 0) {
            result.put("ok", false);
            result.put("message", "任务不可认领");
            return result;
        }
        Integer claimed = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_task_claim WHERE task_id = ? AND user_id = ?",
                Integer.class,
                taskId, userId
        );
        if (claimed != null && claimed > 0) {
            result.put("ok", false);
            result.put("message", "你已认领该任务");
            return result;
        }
        Integer maxClaims = jdbcTemplate.queryForObject(
                "SELECT max_claims FROM ap_task WHERE id = ?",
                Integer.class,
                taskId
        );
        Integer claimedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_task_claim WHERE task_id = ?",
                Integer.class,
                taskId
        );
        if (maxClaims != null && maxClaims > 0 && claimedCount != null && claimedCount >= maxClaims) {
            result.put("ok", false);
            result.put("message", "任务已被认领完");
            return result;
        }
        String sql = "INSERT INTO ap_task_claim (task_id, user_id, status, claimed_at, created_at, updated_at) VALUES (?, ?, 'CLAIMED', NOW(3), NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
            return ps;
        }, keyHolder);
        if (maxClaims != null && maxClaims > 0 && claimedCount != null && (claimedCount + 1) >= maxClaims) {
            jdbcTemplate.update("UPDATE ap_task SET status = 'CLOSED', updated_at = NOW(3) WHERE id = ?", taskId);
        }
        result.put("ok", true);
        result.put("claimId", keyHolder.getKey().longValue());
        return result;
    }

    public void registerVolunteer(Long userId) {
        jdbcTemplate.update("UPDATE ap_user SET is_volunteer = 1, updated_at = NOW(3) WHERE id = ?", userId);
    }

    public List<Map<String, Object>> myTaskClaims(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT c.id, c.task_id, c.status, c.claimed_at, c.started_at, c.finished_at, " +
                        "t.title, t.task_type, t.address, t.start_at, t.end_at " +
                        "FROM ap_task_claim c " +
                        "LEFT JOIN ap_task t ON c.task_id = t.id " +
                        "WHERE c.user_id = ? AND t.deleted_at IS NULL ORDER BY c.claimed_at DESC",
                userId
        );
    }

    public Map<String, Object> myTaskClaimDetail(Long claimId, Long userId) {
        Map<String, Object> detail = jdbcTemplate.queryForMap(
                "SELECT c.id AS claim_id, c.status AS claim_status, c.claimed_at, c.started_at, c.finished_at, " +
                        "t.id AS task_id, t.title, t.task_type, t.description, t.address, t.latitude, t.longitude, t.start_at, t.end_at " +
                        "FROM ap_task_claim c " +
                        "JOIN ap_task t ON c.task_id = t.id " +
                        "WHERE c.id = ? AND c.user_id = ?",
                claimId, userId
        );
        List<Map<String, Object>> reportRows = jdbcTemplate.queryForList(
                "SELECT id, summary, distance_km, duration_sec, submitted_at FROM ap_patrol_report WHERE claim_id = ?",
                claimId
        );
        if (!reportRows.isEmpty()) {
            Map<String, Object> report = reportRows.get(0);
            Long reportId = ((Number) report.get("id")).longValue();
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
            detail.put("report", report);
        } else {
            List<Map<String, Object>> rescueRows = jdbcTemplate.queryForList(
                    "SELECT id, description, address, latitude, longitude, submitted_at FROM ap_rescue_support_report WHERE claim_id = ?",
                    claimId
            );
            if (!rescueRows.isEmpty()) {
                Map<String, Object> report = rescueRows.get(0);
                Long reportId = ((Number) report.get("id")).longValue();
                List<Map<String, Object>> attachments = jdbcTemplate.queryForList(
                        "SELECT file_url FROM ap_attachment WHERE biz_type = 'RESCUE_SUPPORT_REPORT' AND biz_id = ? ORDER BY created_at ASC",
                        reportId
                );
                java.util.List<String> urls = new java.util.ArrayList<>();
                for (Map<String, Object> row : attachments) {
                    Object url = row.get("file_url");
                    if (url != null) {
                        urls.add(url.toString());
                    }
                }
                report.put("attachments", urls);
                detail.put("rescue_report", report);
            }
        }
        return detail;
    }

    public Long createRescueSupportReport(RescueSupportReportRequest request, Long userId) {
        String sql = "INSERT INTO ap_rescue_support_report (claim_id, description, address, latitude, longitude, submitted_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getClaimId());
            ps.setString(2, request.getDescription());
            ps.setString(3, request.getAddress());
            ps.setObject(4, request.getLatitude());
            ps.setObject(5, request.getLongitude());
            return ps;
        }, keyHolder);
        Long reportId = keyHolder.getKey().longValue();
        saveAttachments("RESCUE_SUPPORT_REPORT", reportId, request.getAttachments(), userId);
        jdbcTemplate.update("UPDATE ap_task_claim SET status = 'FINISHED', finished_at = NOW(3), updated_at = NOW(3) WHERE id = ?",
                request.getClaimId());
        return reportId;
    }

    public List<Map<String, Object>> rescueSupportReportsByCreator(Long userId, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.id AS claim_id, c.status AS claim_status, c.claimed_at, c.finished_at, " +
                        "t.id AS task_id, t.title, t.task_type, " +
                        "u.id AS volunteer_user_id, COALESCE(u.nickname, u.phone) AS volunteer_name, " +
                        "r.id AS report_id, r.description, r.submitted_at " +
                        "FROM ap_task_claim c " +
                        "JOIN ap_task t ON c.task_id = t.id " +
                        "LEFT JOIN ap_rescue_support_report r ON r.claim_id = c.id " +
                        "LEFT JOIN ap_user u ON c.user_id = u.id " +
                        "WHERE t.creator_role = 'RESCUE' AND t.creator_user_id = ? AND t.task_type = 'RESCUE_SUPPORT' "
        );
        if ("FINISHED".equalsIgnoreCase(status)) {
            sql.append("AND c.status = 'FINISHED' ");
        } else if ("UNFINISHED".equalsIgnoreCase(status)) {
            sql.append("AND c.status <> 'FINISHED' ");
        }
        sql.append("ORDER BY c.claimed_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), userId);
    }

    public Map<String, Object> rescueSupportReportDetail(Long reportId, Long userId) {
        Map<String, Object> report = jdbcTemplate.queryForMap(
                "SELECT r.id, r.claim_id, r.description, r.address, r.latitude, r.longitude, r.submitted_at, " +
                        "t.id AS task_id, t.title, t.task_type, " +
                        "u.id AS volunteer_user_id, COALESCE(u.nickname, u.phone) AS volunteer_name " +
                        "FROM ap_rescue_support_report r " +
                        "JOIN ap_task_claim c ON r.claim_id = c.id " +
                        "JOIN ap_task t ON c.task_id = t.id " +
                        "LEFT JOIN ap_user u ON c.user_id = u.id " +
                        "WHERE r.id = ? AND t.creator_role = 'RESCUE' AND t.creator_user_id = ?",
                reportId, userId
        );
        List<Map<String, Object>> attachments = jdbcTemplate.queryForList(
                "SELECT file_url FROM ap_attachment WHERE biz_type = 'RESCUE_SUPPORT_REPORT' AND biz_id = ? ORDER BY created_at ASC",
                reportId
        );
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (Map<String, Object> row : attachments) {
            Object url = row.get("file_url");
            if (url != null) {
                urls.add(url.toString());
            }
        }
        report.put("attachments", urls);
        return report;
    }

    public Long createTask(String taskType, String creatorRole, Long creatorUserId, com.animalprotection.dto.VolunteerTaskCreateRequest request) {
        String sql = "INSERT INTO ap_task (task_type, title, description, address, latitude, longitude, start_at, end_at, max_claims, status, creator_role, creator_user_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, taskType);
            ps.setString(2, request.getTitle());
            ps.setString(3, request.getDescription());
            ps.setString(4, request.getAddress());
            ps.setObject(5, request.getLatitude());
            ps.setObject(6, request.getLongitude());
            ps.setString(7, request.getStartAt());
            ps.setString(8, request.getEndAt());
            ps.setObject(9, request.getMaxClaims());
            ps.setString(10, creatorRole);
            ps.setObject(11, creatorUserId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void startClaim(Long claimId) {
        jdbcTemplate.update("UPDATE ap_task_claim SET status = 'STARTED', started_at = NOW(3), updated_at = NOW(3) WHERE id = ?", claimId);
    }

    public Long createPatrolReport(PatrolReportRequest request) {
        String sql = "INSERT INTO ap_patrol_report (claim_id, summary, distance_km, duration_sec, submitted_at, created_at) VALUES (?, ?, ?, ?, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getClaimId());
            ps.setString(2, request.getSummary());
            ps.setObject(3, request.getDistanceKm());
            ps.setObject(4, request.getDurationSec());
            return ps;
        }, keyHolder);
        Long reportId = keyHolder.getKey().longValue();

        if (request.getTrackPoints() != null) {
            for (PatrolReportRequest.PatrolTrackPoint p : request.getTrackPoints()) {
                jdbcTemplate.update("INSERT INTO ap_patrol_track_point (report_id, seq_no, latitude, longitude, point_time, created_at) VALUES (?, ?, ?, ?, NOW(3), NOW(3))",
                        reportId, p.getSeqNo(), p.getLatitude(), p.getLongitude());
            }
        }
        if (request.getRiskPoints() != null) {
            for (PatrolReportRequest.RiskPoint rp : request.getRiskPoints()) {
                jdbcTemplate.update("INSERT INTO ap_risk_point (report_id, risk_type, description, address, latitude, longitude, found_at, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                        reportId, rp.getRiskType(), rp.getDescription(), rp.getAddress(), rp.getLatitude(), rp.getLongitude());
            }
        }
        jdbcTemplate.update("UPDATE ap_task_claim SET status = 'FINISHED', finished_at = NOW(3), updated_at = NOW(3) WHERE id = ?",
                request.getClaimId());
        return reportId;
    }

    public List<Map<String, Object>> adoptionListings() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT l.id, l.title, l.description, l.status, l.published_at, " +
                        "a.id AS animal_id, a.name AS animal_name, a.species, a.health_summary, a.status AS animal_status " +
                        "FROM ap_adoption_listing l " +
                        "JOIN ap_animal a ON l.animal_id = a.id " +
                        "WHERE l.status = 'OPEN' AND l.deleted_at IS NULL " +
                        "ORDER BY l.published_at DESC"
        );
        for (Map<String, Object> row : list) {
            Long listingId = ((Number) row.get("id")).longValue();
            row.put("attachments", listAttachmentUrls("ADOPTION_LISTING", listingId));
        }
        return list;
    }

    public Map<String, Object> adoptionListingDetail(Long listingId) {
        Map<String, Object> detail = jdbcTemplate.queryForMap(
                "SELECT l.id, l.title, l.description, l.status, l.published_at, " +
                        "a.id AS animal_id, a.name AS animal_name, a.species, a.health_summary, a.status AS animal_status " +
                        "FROM ap_adoption_listing l " +
                        "JOIN ap_animal a ON l.animal_id = a.id " +
                        "WHERE l.id = ? AND l.deleted_at IS NULL",
                listingId
        );
        detail.put("attachments", listAttachmentUrls("ADOPTION_LISTING", listingId));
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT id, record_type, record_content, recorded_at FROM ap_medical_record WHERE animal_id = ? ORDER BY recorded_at DESC",
                detail.get("animal_id")
        );
        detail.put("medical_records", records);
        return detail;
    }

    public Long applyAdoption(Long listingId, String applyForm, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_adoption_listing WHERE id = ? AND status = 'OPEN' AND deleted_at IS NULL",
                Integer.class,
                listingId
        );
        if (count == null || count == 0) {
            return null;
        }
        Integer claimed = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_adoption WHERE applicant_user_id = ? AND animal_id = " +
                        "(SELECT animal_id FROM ap_adoption_listing WHERE id = ?)",
                Integer.class,
                userId,
                listingId
        );
        if (claimed != null && claimed > 0) {
            return null;
        }
        String sql = "INSERT INTO ap_adoption (animal_id, applicant_user_id, status, apply_form, applied_at, decided_at, created_at, updated_at) " +
                "VALUES ((SELECT animal_id FROM ap_adoption_listing WHERE id = ?), ?, 'APPROVED', ?, NOW(3), NOW(3), NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, listingId);
            ps.setLong(2, userId);
            ps.setString(3, applyForm);
            return ps;
        }, keyHolder);
        jdbcTemplate.update("UPDATE ap_adoption_listing SET status = 'ADOPTED', updated_at = NOW(3) WHERE id = ?", listingId);
        jdbcTemplate.update("UPDATE ap_animal SET status = 'ADOPTED', updated_at = NOW(3) WHERE id = (SELECT animal_id FROM ap_adoption_listing WHERE id = ?)",
                listingId);
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_rescue_task WHERE id = (SELECT rescue_task_id FROM ap_animal WHERE id = (SELECT animal_id FROM ap_adoption_listing WHERE id = ?))), " +
                        "'领养', ?, 'PUBLIC', ?, NOW(3))",
                listingId, "动物已被领养", userId);
        return keyHolder.getKey().longValue();
    }

    public Long findListingIdByAnimal(Long animalId) {
        if (animalId == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM ap_adoption_listing WHERE animal_id = ? AND status = 'OPEN' AND deleted_at IS NULL ORDER BY id DESC LIMIT 1",
                animalId
        );
        if (rows.isEmpty()) {
            return null;
        }
        Object id = rows.get(0).get("id");
        return id instanceof Number ? ((Number) id).longValue() : null;
    }

    public List<Map<String, Object>> myAdoptions(Long userId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT a.id, a.animal_id, a.status, a.applied_at, a.decided_at, " +
                        "l.id AS listing_id, l.title, l.description, l.status AS listing_status, " +
                        "an.name AS animal_name, an.species, an.health_summary, an.status AS animal_status " +
                        "FROM ap_adoption a " +
                        "JOIN ap_animal an ON a.animal_id = an.id " +
                        "LEFT JOIN ap_adoption_listing l ON l.animal_id = an.id " +
                        "WHERE a.applicant_user_id = ? ORDER BY a.applied_at DESC",
                userId
        );
        for (Map<String, Object> row : list) {
            Object listingId = row.get("listing_id");
            if (listingId instanceof Number) {
                row.put("attachments", listAttachmentUrls("ADOPTION_LISTING", ((Number) listingId).longValue()));
            } else {
                row.put("attachments", java.util.Collections.emptyList());
            }
        }
        return list;
    }

    public List<Map<String, Object>> followupTasks(Long userId, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.id, t.adoption_id, t.status, t.questionnaire_template, t.sent_at, t.submitted_at, " +
                        "a.animal_id, an.name AS animal_name, an.species, " +
                        "l.id AS listing_id, l.title " +
                        "FROM ap_follow_up_task t " +
                        "JOIN ap_adoption a ON t.adoption_id = a.id " +
                        "LEFT JOIN ap_adoption_listing l ON a.animal_id = l.animal_id " +
                        "LEFT JOIN ap_animal an ON a.animal_id = an.id " +
                        "WHERE a.applicant_user_id = ? "
        );
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND t.status = ? ");
        }
        sql.append("ORDER BY t.sent_at DESC");
        if (status != null && !status.trim().isEmpty()) {
            return jdbcTemplate.queryForList(sql.toString(), userId, status);
        }
        return jdbcTemplate.queryForList(sql.toString(), userId);
    }

    public Map<String, Object> followupTaskDetail(Long taskId, Long userId) {
        Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT t.id, t.adoption_id, t.status, t.questionnaire_template, t.sent_at, t.submitted_at, " +
                        "a.animal_id, an.name AS animal_name, an.species, " +
                        "l.id AS listing_id, l.title " +
                        "FROM ap_follow_up_task t " +
                        "JOIN ap_adoption a ON t.adoption_id = a.id " +
                        "LEFT JOIN ap_adoption_listing l ON a.animal_id = l.animal_id " +
                        "LEFT JOIN ap_animal an ON a.animal_id = an.id " +
                        "WHERE t.id = ? AND a.applicant_user_id = ?",
                taskId, userId
        );
        List<Map<String, Object>> followups = jdbcTemplate.queryForList(
                "SELECT id, questionnaire, submitted_at FROM ap_follow_up WHERE adoption_id = ? ORDER BY submitted_at DESC LIMIT 1",
                task.get("adoption_id")
        );
        if (!followups.isEmpty()) {
            Map<String, Object> answer = followups.get(0);
            Object followupId = answer.get("id");
            if (followupId instanceof Number) {
                answer.put("attachments", listAttachmentUrls("FOLLOW_UP", ((Number) followupId).longValue()));
            } else {
                answer.put("attachments", java.util.Collections.emptyList());
            }
            task.put("answer", answer);
        }
        return task;
    }

    public Long submitFollowupTask(Long taskId, String questionnaire, List<String> attachments, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_follow_up_task t JOIN ap_adoption a ON t.adoption_id = a.id " +
                        "WHERE t.id = ? AND t.status = 'PENDING' AND a.applicant_user_id = ?",
                Integer.class,
                taskId, userId
        );
        if (count == null || count == 0) {
            return null;
        }
        Long adoptionId = jdbcTemplate.queryForObject(
                "SELECT adoption_id FROM ap_follow_up_task WHERE id = ?",
                Long.class,
                taskId
        );
        String sql = "INSERT INTO ap_follow_up (adoption_id, due_at, questionnaire, submitted_at, created_at) " +
                "VALUES (?, DATE_ADD((SELECT sent_at FROM ap_follow_up_task WHERE id = ?), INTERVAL 7 DAY), ?, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, adoptionId);
            ps.setLong(2, taskId);
            ps.setString(3, questionnaire);
            return ps;
        }, keyHolder);
        Long followupId = keyHolder.getKey().longValue();
        saveAttachments("FOLLOW_UP", followupId, attachments, userId);
        jdbcTemplate.update("UPDATE ap_follow_up_task SET status = 'SUBMITTED', submitted_at = NOW(3), updated_at = NOW(3) WHERE id = ?", taskId);
        return followupId;
    }

    public Long donate(DonationRequest request, Long userId) {
        String sql = "INSERT INTO ap_donation (donor_user_id, anonymous, target_type, target_id, amount, status, donated_at, created_at) VALUES (?, ?, ?, ?, ?, 'SUCCEEDED', NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setBoolean(2, request.isAnonymous());
            ps.setString(3, request.getTargetType());
            ps.setLong(4, request.getTargetId());
            ps.setDouble(5, request.getAmount());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> contentList() {
        return jdbcTemplate.queryForList("SELECT id, title, content_type, published_at FROM ap_content WHERE status = 'PUBLISHED' ORDER BY published_at DESC");
    }

    public Map<String, Object> contentDetail(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM ap_content WHERE id = ?", id);
        return list.isEmpty() ? Collections.emptyMap() : list.get(0);
    }

    private void saveAttachments(String bizType, Long bizId, List<String> urls, Long uploaderUserId) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        for (String url : urls) {
            jdbcTemplate.update("INSERT INTO ap_attachment (biz_type, biz_id, file_url, uploader_user_id, created_at) VALUES (?, ?, ?, ?, NOW(3))",
                    bizType, bizId, url, uploaderUserId);
        }
    }

    private java.util.List<String> listAttachmentUrls(String bizType, Long bizId) {
        List<Map<String, Object>> attachments = jdbcTemplate.queryForList(
                "SELECT file_url FROM ap_attachment WHERE biz_type = ? AND biz_id = ? ORDER BY created_at ASC",
                bizType,
                bizId
        );
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (Map<String, Object> row : attachments) {
            Object url = row.get("file_url");
            if (url != null) {
                urls.add(url.toString());
            }
        }
        return urls;
    }
}
