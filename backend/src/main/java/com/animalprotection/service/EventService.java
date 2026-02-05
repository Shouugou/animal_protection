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
            ps.setString(6, latitude);
            ps.setString(7, longitude);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
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

    public List<Map<String, Object>> timeline(Long eventId) {
        return jdbcTemplate.queryForList("SELECT id, node_type, content, created_at FROM ap_event_timeline WHERE event_id = ? ORDER BY created_at ASC", eventId);
    }

    public void addTimeline(Long eventId, String nodeType, String content, Long operatorUserId) {
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) VALUES (?, ?, ?, ?, ?, NOW(3))",
                eventId, nodeType, content, null, operatorUserId);
    }

    public List<Map<String, Object>> comments(Long eventId) {
        return jdbcTemplate.queryForList("SELECT id, content, created_at FROM ap_comment WHERE event_id = ? ORDER BY created_at DESC", eventId);
    }

    public void addComment(Long eventId, Long userId, Long parentId, String content) {
        jdbcTemplate.update("INSERT INTO ap_comment (event_id, parent_id, author_user_id, content, status, created_at) VALUES (?, ?, ?, ?, 1, NOW(3))",
                eventId, parentId, userId, content);
    }
}
