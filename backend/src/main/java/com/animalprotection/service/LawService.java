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

    public List<Map<String, Object>> workOrders() {
        return jdbcTemplate.queryForList("SELECT id, status, need_law_enforcement, transfer_to_rescue FROM ap_work_order ORDER BY created_at DESC");
    }

    public Map<String, Object> workOrderDetail(Long id) {
        return jdbcTemplate.queryForMap("SELECT * FROM ap_work_order WHERE id = ?", id);
    }

    public void accept(Long id, WorkOrderAcceptRequest request) {
        jdbcTemplate.update("UPDATE ap_work_order SET need_law_enforcement = ?, transfer_to_rescue = ?, status = 'ACCEPTED', updated_at = NOW(3) WHERE id = ?",
                request.getNeedLawEnforcement(), request.getTransferToRescue(), id);
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, created_at) VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), '受理', ?, NOW(3))",
                id, request.getResultText());
    }

    public void assign(Long id, WorkOrderAssignRequest request) {
        jdbcTemplate.update("UPDATE ap_work_order SET assignee_user_id = ?, status = 'ASSIGNED', updated_at = NOW(3) WHERE id = ?",
                request.getAssigneeUserId(), id);
    }

    public Long addEvidence(LawEvidenceRequest request, Long collectorUserId) {
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
        return keyHolder.getKey().longValue();
    }

    public void saveResult(Long workOrderId, LawResultRequest request, Long inputUserId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ap_law_result WHERE work_order_id = ?", Integer.class, workOrderId);
        if (count != null && count > 0) {
            jdbcTemplate.update("UPDATE ap_law_result SET result_text = ?, public_text = ?, published_at = NOW(3), updated_at = NOW(3) WHERE work_order_id = ?",
                    request.getResultText(), request.getPublicText(), workOrderId);
        } else {
            jdbcTemplate.update("INSERT INTO ap_law_result (work_order_id, result_text, public_text, published_at, input_user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(3), ?, NOW(3), NOW(3))",
                    workOrderId, request.getResultText(), request.getPublicText(), inputUserId);
        }
    }

    public void archive(Long workOrderId, LawArchiveRequest request, Long archiverUserId) {
        jdbcTemplate.update("INSERT INTO ap_case_archive (work_order_id, archive_no, summary, archived_at, archiver_user_id, created_at) VALUES (?, ?, ?, NOW(3), ?, NOW(3))",
                workOrderId, request.getArchiveNo(), request.getSummary(), archiverUserId);
        jdbcTemplate.update("UPDATE ap_work_order SET status = 'ARCHIVED', updated_at = NOW(3) WHERE id = ?", workOrderId);
    }

    public void transferToRescue(Long workOrderId, Long rescueOrgId) {
        jdbcTemplate.update("UPDATE ap_work_order SET transfer_to_rescue = 1, updated_at = NOW(3) WHERE id = ?", workOrderId);
        jdbcTemplate.update("INSERT INTO ap_rescue_task (event_id, rescue_org_id, status, need_rescue, created_at, updated_at) VALUES ((SELECT event_id FROM ap_work_order WHERE id = ?), ?, 'NEW', 1, NOW(3), NOW(3))",
                workOrderId, rescueOrgId);
    }
}
