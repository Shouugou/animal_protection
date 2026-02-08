package com.animalprotection.service;

import com.animalprotection.dto.AnimalCreateRequest;
import com.animalprotection.dto.InventoryTxnRequest;
import com.animalprotection.dto.MedicalRecordRequest;
import com.animalprotection.dto.RescueDispatchRequest;
import com.animalprotection.dto.RescueEvaluateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
public class RescueService {
    private final JdbcTemplate jdbcTemplate;

    public RescueService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> tasks(Long userId, String status) {
        Long orgId = findOrgId(userId, false);
        StringBuilder sql = new StringBuilder(
                "SELECT rt.id, rt.event_id, rt.status, rt.need_rescue, rt.dispatch_note, rt.dispatch_at, rt.arrived_at, rt.intake_at, " +
                        "e.event_type, e.address, e.latitude, e.longitude, e.reported_at " +
                        "FROM ap_rescue_task rt " +
                        "LEFT JOIN ap_event e ON rt.event_id = e.id " +
                        "WHERE rt.deleted_at IS NULL "
        );
        boolean hasStatus = status != null && !status.trim().isEmpty();
        if (hasStatus) {
            sql.append("AND rt.status = ? ");
            if (!"NEW".equalsIgnoreCase(status)) {
                if (orgId == null) {
                    return java.util.Collections.emptyList();
                }
                sql.append("AND rt.rescue_org_id = ? ");
            } else if (orgId != null) {
                sql.append("AND (rt.rescue_org_id IS NULL OR rt.rescue_org_id = ?) ");
            }
        } else {
            sql.append("AND (rt.status = 'NEW' ");
            if (orgId != null) {
                sql.append("AND (rt.rescue_org_id IS NULL OR rt.rescue_org_id = ?) ");
                sql.append("OR rt.rescue_org_id = ? ");
            }
            sql.append(") ");
        }
        sql.append("ORDER BY rt.created_at DESC");
        if (hasStatus) {
            if ("NEW".equalsIgnoreCase(status)) {
                if (orgId != null) {
                    return jdbcTemplate.queryForList(sql.toString(), status, orgId);
                }
                return jdbcTemplate.queryForList(sql.toString(), status);
            }
            return jdbcTemplate.queryForList(sql.toString(), status, orgId);
        }
        if (orgId != null) {
            return jdbcTemplate.queryForList(sql.toString(), orgId, orgId);
        }
        return jdbcTemplate.queryForList(sql.toString());
    }

    public void grab(Long id, Long userId) {
        Long orgId = findOrgId(userId, true);
        int updated = jdbcTemplate.update(
                "UPDATE ap_rescue_task SET rescue_org_id = ?, status = 'GRABBED', updated_at = NOW(3) WHERE id = ? AND status = 'NEW'",
                orgId, id
        );
        if (updated == 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_rescue_task WHERE id = ?), '救助接收', ?, 'RESCUE', ?, NOW(3))",
                id, "救助任务已接收", userId);
    }

    public void evaluate(Long id, RescueEvaluateRequest request, Long userId) {
        boolean need = request.getNeedRescue() != null && request.getNeedRescue();
        jdbcTemplate.update("UPDATE ap_rescue_task SET need_rescue = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                need, need ? "DISPATCHING" : "REJECTED", id);
        if (!need) {
            jdbcTemplate.update("UPDATE ap_event SET status = 'CLOSED', updated_at = NOW(3) WHERE id = (SELECT event_id FROM ap_rescue_task WHERE id = ?)",
                    id);
        }
        String content = need ? "评估通过，进入救助流程" : "评估不救助";
        if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
            content = content + "：" + request.getNote().trim();
        }
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_rescue_task WHERE id = ?), '救助评估', ?, 'RESCUE', ?, NOW(3))",
                id, content, userId);
    }

    public void dispatch(Long id, RescueDispatchRequest request, Long userId) {
        String status = "DISPATCHING";
        if (request.getIntake() != null && !request.getIntake().isEmpty()) {
            status = "INTAKE";
        } else if (request.getArrive() != null && !request.getArrive().isEmpty()) {
            status = "ARRIVED";
        }
        jdbcTemplate.update("UPDATE ap_rescue_task SET dispatch_note = ?, dispatch_at = ?, arrived_at = ?, intake_at = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                request.getNote(), request.getStart(), request.getArrive(), request.getIntake(), status, id);
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_rescue_task WHERE id = ?), '救助调度', ?, 'RESCUE', ?, NOW(3))",
                id, request.getNote() == null || request.getNote().trim().isEmpty() ? "救助调度处理中" : request.getNote(), userId);
    }

    public Long createAnimal(AnimalCreateRequest request, Long userId) {
        String sql = "INSERT INTO ap_animal (rescue_task_id, species, health_summary, status, created_at, updated_at) VALUES (?, ?, ?, 'IN_CARE', NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getRescueTaskId());
            ps.setString(2, request.getSpecies());
            ps.setString(3, request.getSummary());
            return ps;
        }, keyHolder);
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_rescue_task WHERE id = ?), '动物建档', ?, 'RESCUE', ?, NOW(3))",
                request.getRescueTaskId(), "已建立动物档案", userId);
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> animals(Long userId) {
        Long orgId = findOrgId(userId, false);
        if (orgId == null) {
            return jdbcTemplate.queryForList(
                    "SELECT a.id, a.rescue_task_id, a.species, a.status, a.health_summary, a.created_at " +
                            "FROM ap_animal a ORDER BY a.created_at DESC"
            );
        }
        return jdbcTemplate.queryForList(
                "SELECT a.id, a.rescue_task_id, a.species, a.status, a.health_summary, a.created_at " +
                        "FROM ap_animal a " +
                        "WHERE a.rescue_task_id IN (SELECT id FROM ap_rescue_task WHERE rescue_org_id = ?) " +
                        "ORDER BY a.created_at DESC",
                orgId
        );
    }

    public Long addMedicalRecord(MedicalRecordRequest request, Long recorderUserId) {
        String sql = "INSERT INTO ap_medical_record (animal_id, record_type, record_content, recorded_at, recorder_user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(3), ?, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getAnimalId());
            ps.setString(2, request.getRecordType());
            ps.setString(3, "{\"text\":\"" + request.getRecordContent().replace("\"", "\\\"") + "\"}");
            ps.setObject(4, recorderUserId);
            return ps;
        }, keyHolder);
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_rescue_task WHERE id = (SELECT rescue_task_id FROM ap_animal WHERE id = ?)), '治疗记录', ?, 'RESCUE', ?, NOW(3))",
                request.getAnimalId(), "新增治疗记录", recorderUserId);
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> medicalRecords(Long animalId) {
        return jdbcTemplate.queryForList(
                "SELECT id, record_type, record_content, recorded_at FROM ap_medical_record WHERE animal_id = ? ORDER BY recorded_at DESC",
                animalId
        );
    }

    public void shareMedicalRecord(com.animalprotection.dto.MedicalShareRequest request, Long userId) {
        String targets = request.getTargets() == null ? "" : String.join(",", request.getTargets());
        jdbcTemplate.update("INSERT INTO ap_message (user_id, msg_type, title, content, ref_type, ref_id, created_at) VALUES (?, 'MEDICAL_SHARE', ?, ?, 'ANIMAL', ?, NOW(3))",
                userId,
                "病历共享申请",
                "共享对象：" + targets + (request.getNote() != null ? ("；说明：" + request.getNote()) : ""),
                request.getAnimalId());
    }

    public List<Map<String, Object>> inventoryItems(Long userId) {
        Long orgId = findOrgId(userId, true);
        String sql = "SELECT i.id, i.item_name, i.production_date, i.expiry_date, i.stock_qty, i.low_stock_threshold, i.warning_days " +
                "FROM ap_inventory_item i WHERE i.org_id = ? AND i.deleted_at IS NULL ORDER BY i.created_at DESC";
        return jdbcTemplate.queryForList(sql, orgId);
    }

    public Long createInventoryItem(com.animalprotection.dto.InventoryItemCreateRequest request, Long userId) {
        Long orgId = findOrgId(userId, true);
        String sql = "INSERT INTO ap_inventory_item (org_id, item_name, production_date, expiry_date, stock_qty, low_stock_threshold, warning_days, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String productionDate = normalizeDate(request.getProductionDate());
        String expiryDate = normalizeDate(request.getExpiryDate());
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, orgId);
            ps.setString(2, request.getItemName());
            ps.setString(3, productionDate);
            ps.setString(4, expiryDate);
            ps.setObject(5, request.getStockQty() == null ? 0 : request.getStockQty());
            ps.setObject(6, request.getLowStockThreshold());
            ps.setObject(7, request.getWarningDays());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> inventoryTxns(Long itemId) {
        return jdbcTemplate.queryForList(
                "SELECT id, txn_no, item_id, txn_type, qty, note, created_at " +
                        "FROM ap_inventory_txn WHERE item_id = ? ORDER BY created_at DESC",
                itemId
        );
    }

    public List<Map<String, Object>> inventoryAlerts(Long userId) {
        Long orgId = findOrgId(userId, true);
        String sql = "SELECT i.id, i.item_name, i.stock_qty, i.low_stock_threshold, i.warning_days, i.expiry_date " +
                "FROM ap_inventory_item i WHERE i.org_id = ? AND i.deleted_at IS NULL " +
                "AND ( (i.low_stock_threshold IS NOT NULL AND i.stock_qty < i.low_stock_threshold) " +
                "OR (i.warning_days IS NOT NULL AND i.expiry_date IS NOT NULL AND i.expiry_date <= DATE_ADD(CURDATE(), INTERVAL i.warning_days DAY)) )";
        return jdbcTemplate.queryForList(sql, orgId);
    }

    public Long addInventoryTxn(InventoryTxnRequest request, Long operatorUserId) {
        Long orgId = request.getOrgId();
        if (orgId == null || orgId == 0) {
            orgId = findOrgId(operatorUserId, true);
        }
        String txnType = request.getTxnType();
        Double qty = request.getQty();
        Long itemId = request.getItemId();
        if (itemId == null || txnType == null || qty == null) {
            return null;
        }
        if ("OUT".equalsIgnoreCase(txnType) || "LOSS".equalsIgnoreCase(txnType)) {
            jdbcTemplate.update("UPDATE ap_inventory_item SET stock_qty = GREATEST(stock_qty - ?, 0), updated_at = NOW(3) WHERE id = ?",
                    qty, itemId);
        } else if ("IN".equalsIgnoreCase(txnType) || "RETURN".equalsIgnoreCase(txnType)) {
            jdbcTemplate.update("UPDATE ap_inventory_item SET stock_qty = stock_qty + ?, updated_at = NOW(3) WHERE id = ?",
                    qty, itemId);
        } else if ("ADJUST".equalsIgnoreCase(txnType)) {
            jdbcTemplate.update("UPDATE ap_inventory_item SET stock_qty = ?, updated_at = NOW(3) WHERE id = ?",
                    qty, itemId);
        }
        recordTxn(orgId, itemId, txnType, qty, operatorUserId, request.getNote());
        return 1L;
    }

    public void deleteInventoryItem(Long itemId, Long operatorUserId) {
        Long orgId = findOrgId(operatorUserId, true);
        jdbcTemplate.update("UPDATE ap_inventory_item SET stock_qty = 0, deleted_at = NOW(3), updated_at = NOW(3) WHERE id = ?",
                itemId);
        recordTxn(orgId, itemId, "ADJUST", 0.0, operatorUserId, "删除品项");
    }

    private void recordTxn(Long orgId, Long itemId, String txnType, Double qty, Long operatorUserId, String note) {
        String sql = "INSERT INTO ap_inventory_txn (txn_no, org_id, item_id, txn_type, qty, note, operator_user_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(3))";
        String txnNo = "TXN" + System.currentTimeMillis();
        jdbcTemplate.update(sql, txnNo, orgId, itemId, txnType, qty, note, operatorUserId);
    }

    private String normalizeDate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int tIndex = trimmed.indexOf('T');
        if (tIndex > 0) {
            return trimmed.substring(0, tIndex);
        }
        if (trimmed.length() > 10) {
            return trimmed.substring(0, 10);
        }
        return trimmed;
    }

    private Long findOrgId(Long userId, boolean ensureDefault) {
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
        return ensureDefault ? findDefaultRescueOrgId() : null;
    }

    private Long findDefaultRescueOrgId() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id FROM ap_organization WHERE org_type = 'RESCUE' AND status = 1 AND deleted_at IS NULL ORDER BY id ASC LIMIT 1"
        );
        if (list.isEmpty()) {
            String sql = "INSERT INTO ap_organization (org_type, name, status, created_at, updated_at) VALUES ('RESCUE', '默认救助机构', 1, NOW(3), NOW(3))";
            org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(con -> con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS), keyHolder);
            if (keyHolder.getKey() == null) {
                return null;
            }
            return keyHolder.getKey().longValue();
        }
        return ((Number) list.get(0).get("id")).longValue();
    }
}
