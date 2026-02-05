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
        return jdbcTemplate.queryForList("SELECT id, title, task_type, status FROM ap_task WHERE deleted_at IS NULL ORDER BY created_at DESC");
    }

    public Long claimTask(Long taskId, Long userId) {
        String sql = "INSERT INTO ap_task_claim (task_id, user_id, status, claimed_at, created_at, updated_at) VALUES (?, ?, 'CLAIMED', NOW(3), NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
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
        return reportId;
    }

    public List<Map<String, Object>> animals() {
        return jdbcTemplate.queryForList("SELECT id, species, status FROM ap_animal WHERE deleted_at IS NULL AND status = 'READY_FOR_ADOPTION' ORDER BY created_at DESC");
    }

    public Long createAdoption(AdoptionRequest request, Long userId) {
        String sql = "INSERT INTO ap_adoption (animal_id, applicant_user_id, status, apply_form, applied_at, created_at, updated_at) VALUES (?, ?, 'APPLIED', ?, NOW(3), NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getAnimalId());
            ps.setLong(2, userId);
            ps.setString(3, request.getApplyForm());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> followups(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT f.id, f.adoption_id, f.submitted_at, f.due_at " +
                        "FROM ap_follow_up f JOIN ap_adoption a ON f.adoption_id = a.id " +
                        "WHERE a.applicant_user_id = ? ORDER BY f.submitted_at DESC",
                userId
        );
    }

    public Long submitFollowup(FollowupRequest request) {
        String sql = "INSERT INTO ap_follow_up (adoption_id, due_at, questionnaire, submitted_at, created_at) VALUES (?, ?, ?, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getAdoptionId());
            ps.setString(2, request.getDueAt());
            ps.setString(3, request.getQuestionnaire());
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        saveAttachments("FOLLOW_UP", id, request.getAttachments(), 1L);
        return id;
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
}
