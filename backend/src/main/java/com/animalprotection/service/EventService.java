package com.animalprotection.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
public class EventService {
    private final JdbcTemplate jdbcTemplate;

    public EventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createEvent(String eventType, String urgency, String description, String address,
                            String latitude, String longitude, Long reporterUserId) {
        String sql = "INSERT INTO ap_event (event_type, urgency, description, status, reporter_user_id, address, latitude, longitude, reported_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, 'REPORTED', ?, ?, ?, ?, NOW(3), NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, eventType);
            ps.setString(2, urgency);
            ps.setString(3, description);
            ps.setObject(4, reporterUserId);
            ps.setString(5, address);
            if (latitude == null || latitude.trim().isEmpty()) {
                ps.setNull(6, java.sql.Types.DECIMAL);
            } else {
                ps.setBigDecimal(6, new java.math.BigDecimal(latitude));
            }
            if (longitude == null || longitude.trim().isEmpty()) {
                ps.setNull(7, java.sql.Types.DECIMAL);
            } else {
                ps.setBigDecimal(7, new java.math.BigDecimal(longitude));
            }
            return ps;
        }, keyHolder);
        Long eventId = keyHolder.getKey().longValue();
        Long lawOrgId = findDefaultLawOrgId();
        if (lawOrgId != null) {
            jdbcTemplate.update("INSERT INTO ap_work_order (event_id, law_org_id, status, need_law_enforcement, transfer_to_rescue, created_at, updated_at) " +
                            "VALUES (?, ?, 'NEW', 1, 0, NOW(3), NOW(3))",
                    eventId, lawOrgId);
        }
        return eventId;
    }

    private Long findDefaultLawOrgId() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id FROM ap_organization WHERE org_type = 'LAW' AND status = 1 AND deleted_at IS NULL ORDER BY id ASC LIMIT 1"
        );
        if (list.isEmpty()) {
            String sql = "INSERT INTO ap_organization (org_type, name, status, created_at, updated_at) VALUES ('LAW', '默认执法部门', 1, NOW(3), NOW(3))";
            org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(con -> con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS), keyHolder);
            if (keyHolder.getKey() == null) {
                return null;
            }
            return keyHolder.getKey().longValue();
        }
        return ((Number) list.get(0).get("id")).longValue();
    }

    public List<Map<String, Object>> listEvents(String status, String keyword, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT id, event_type, status, reported_at FROM ap_event WHERE deleted_at IS NULL");
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = '").append(status).append("'");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (event_type LIKE '%").append(keyword).append("%' OR description LIKE '%").append(keyword).append("%')");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ").append(limit).append(" OFFSET ").append(offset);
        return jdbcTemplate.queryForList(sql.toString());
    }

    public long countEvents(String status, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM ap_event WHERE deleted_at IS NULL");
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = '").append(status).append("'");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (event_type LIKE '%").append(keyword).append("%' OR description LIKE '%").append(keyword).append("%')");
        }
        return jdbcTemplate.queryForObject(sql.toString(), Long.class);
    }

    public Map<String, Object> getEvent(Long id) {
        return jdbcTemplate.queryForMap("SELECT * FROM ap_event WHERE id = ?", id);
    }

    public List<Map<String, Object>> attachments(String bizType, Long bizId) {
        return jdbcTemplate.queryForList(
                "SELECT file_url FROM ap_attachment WHERE biz_type = ? AND biz_id = ? ORDER BY created_at ASC",
                bizType, bizId
        );
    }

    public java.util.List<String> evidenceAttachmentsByEvent(Long eventId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT a.file_url " +
                        "FROM ap_attachment a " +
                        "WHERE a.biz_type = 'LAW_EVIDENCE' AND a.biz_id IN (" +
                        "  SELECT le.id FROM ap_law_evidence le WHERE le.work_order_id IN (" +
                        "    SELECT wo.id FROM ap_work_order wo WHERE wo.event_id = ?" +
                        "  )" +
                        ") ORDER BY a.created_at ASC",
                eventId
        );
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object url = row.get("file_url");
            if (url != null) {
                urls.add(url.toString());
            }
        }
        return urls;
    }

    public List<Map<String, Object>> timeline(Long eventId) {
        return jdbcTemplate.queryForList("SELECT id, node_type, content, created_at FROM ap_event_timeline WHERE event_id = ? ORDER BY created_at ASC", eventId);
    }

    public void addTimeline(Long eventId, String nodeType, String content, Long operatorUserId) {
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) VALUES (?, ?, ?, ?, ?, NOW(3))",
                eventId, nodeType, content, null, operatorUserId);
    }

    public List<Map<String, Object>> comments(Long eventId) {
        return jdbcTemplate.queryForList(
                "SELECT c.id, c.content, c.created_at, " +
                        "COALESCE(u.nickname, u.phone, '匿名') AS author_name " +
                        "FROM ap_comment c " +
                        "LEFT JOIN ap_user u ON c.author_user_id = u.id " +
                        "WHERE c.event_id = ? ORDER BY c.created_at DESC",
                eventId
        );
    }

    public void addComment(Long eventId, Long userId, Long parentId, String content) {
        jdbcTemplate.update("INSERT INTO ap_comment (event_id, parent_id, author_user_id, content, status, created_at) VALUES (?, ?, ?, ?, 1, NOW(3))",
                eventId, parentId, userId, content);
    }

    public void saveAttachments(String bizType, Long bizId, List<String> urls, Long uploaderUserId) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        for (String url : urls) {
            jdbcTemplate.update("INSERT INTO ap_attachment (biz_type, biz_id, file_url, uploader_user_id, created_at) VALUES (?, ?, ?, ?, NOW(3))",
                    bizType, bizId, url, uploaderUserId);
        }
    }

    public boolean deleteEvent(Long eventId, Long userId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT reporter_user_id FROM ap_event WHERE id = ?", eventId);
        Object reporter = row.get("reporter_user_id");
        if (reporter != null && userId != null) {
            long rid = ((Number) reporter).longValue();
            if (rid != userId.longValue()) {
                return false;
            }
        }
        List<Map<String, Object>> atts = attachments("EVENT", eventId);
        List<Map<String, Object>> evidenceAtts = jdbcTemplate.queryForList(
                "SELECT a.file_url FROM ap_attachment a WHERE a.biz_type = 'LAW_EVIDENCE' AND a.biz_id IN (" +
                        "SELECT le.id FROM ap_law_evidence le WHERE le.work_order_id IN (" +
                        "SELECT wo.id FROM ap_work_order wo WHERE wo.event_id = ?" +
                        "))",
                eventId
        );
        jdbcTemplate.update("DELETE FROM ap_event_timeline WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM ap_comment WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM ap_attachment WHERE biz_type = 'EVENT' AND biz_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM ap_attachment WHERE biz_type = 'LAW_EVIDENCE' AND biz_id IN (" +
                "SELECT le.id FROM ap_law_evidence le WHERE le.work_order_id IN (" +
                "SELECT wo.id FROM ap_work_order wo WHERE wo.event_id = ?" +
                "))", eventId);
        jdbcTemplate.update("DELETE FROM ap_law_evidence WHERE work_order_id IN (SELECT id FROM ap_work_order WHERE event_id = ?)", eventId);
        jdbcTemplate.update("DELETE FROM ap_law_result WHERE work_order_id IN (SELECT id FROM ap_work_order WHERE event_id = ?)", eventId);
        jdbcTemplate.update("DELETE FROM ap_case_archive WHERE work_order_id IN (SELECT id FROM ap_work_order WHERE event_id = ?)", eventId);
        jdbcTemplate.update("DELETE FROM ap_rescue_task WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM ap_work_order WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM ap_event WHERE id = ?", eventId);
        deleteLocalFiles(atts);
        deleteLocalFiles(evidenceAtts);
        return true;
    }

    private void deleteLocalFiles(List<Map<String, Object>> atts) {
        if (atts == null) return;
        String baseDir = System.getProperty("user.dir") + java.io.File.separator + "uploads" + java.io.File.separator;
        for (Map<String, Object> row : atts) {
            Object url = row.get("file_url");
            if (url == null) continue;
            String u = url.toString();
            String name = null;
            if (u.contains("/uploads/")) {
                name = u.substring(u.lastIndexOf("/uploads/") + "/uploads/".length());
            }
            if (name != null && !name.trim().isEmpty()) {
                java.io.File f = new java.io.File(baseDir + name);
                if (f.exists()) {
                    f.delete();
                }
            }
        }
    }
}
