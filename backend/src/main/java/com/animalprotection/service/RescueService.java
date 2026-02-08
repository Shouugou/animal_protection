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
                        "rt.assignee_user_id, COALESCE(u.nickname, u.phone) AS assignee_name, " +
                        "rt.vehicle_id, v.plate_no AS vehicle_plate, v.vehicle_type, " +
                        "e.event_type, e.address, e.latitude, e.longitude, e.reported_at " +
                        "FROM ap_rescue_task rt " +
                        "LEFT JOIN ap_event e ON rt.event_id = e.id " +
                        "LEFT JOIN ap_user u ON rt.assignee_user_id = u.id " +
                        "LEFT JOIN ap_rescue_vehicle v ON rt.vehicle_id = v.id " +
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
        Long orgId = findOrgId(userId, true);
        if (orgId == null || !belongsToOrg(id, orgId)) {
            return;
        }
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

    public boolean dispatch(Long id, RescueDispatchRequest request, Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null || !belongsToOrg(id, orgId)) {
            return false;
        }
        Long assigneeUserId = request.getAssigneeUserId();
        if (assigneeUserId != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM ap_user WHERE id = ? AND org_id = ? AND role_code = 'RESCUE' AND deleted_at IS NULL",
                    Integer.class,
                    assigneeUserId,
                    orgId
            );
            if (count == null || count == 0) {
                assigneeUserId = null;
            }
        }
        if (assigneeUserId != null && !assigneeAvailable(orgId, assigneeUserId)) {
            return false;
        }
        Long vehicleId = request.getVehicleId();
        if (vehicleId != null) {
            Integer vehicleCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM ap_rescue_vehicle WHERE id = ? AND org_id = ? AND deleted_at IS NULL",
                    Integer.class,
                    vehicleId,
                    orgId
            );
            if (vehicleCount == null || vehicleCount == 0) {
                vehicleId = null;
            }
        }
        if (vehicleId != null && !vehicleAvailable(orgId, vehicleId)) {
            return false;
        }
        String status;
        String nodeType;
        String content;
        String startAt = normalizeDateTime(request.getStart());
        String arriveAt = normalizeDateTime(request.getArrive());
        String intakeAt = normalizeDateTime(request.getIntake());
        if (intakeAt != null && !intakeAt.isEmpty()) {
            status = "INTAKE";
            nodeType = "入站";
            content = "已入站";
            jdbcTemplate.update("UPDATE ap_rescue_task SET intake_at = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                    intakeAt, status, id);
        } else if (arriveAt != null && !arriveAt.isEmpty()) {
            status = "ARRIVED";
            nodeType = "到达";
            content = "已到达现场";
            jdbcTemplate.update("UPDATE ap_rescue_task SET arrived_at = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                    arriveAt, status, id);
        } else {
            status = "DEPARTED";
            nodeType = "出发";
            content = request.getNote() == null || request.getNote().trim().isEmpty() ? "救助已出发" : request.getNote();
            jdbcTemplate.update("UPDATE ap_rescue_task SET assignee_user_id = ?, vehicle_id = ?, dispatch_note = ?, dispatch_at = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                    assigneeUserId, vehicleId, request.getNote(), startAt, status, id);
        }
        jdbcTemplate.update("INSERT INTO ap_event_timeline (event_id, node_type, content, operator_role, operator_user_id, created_at) " +
                        "VALUES ((SELECT event_id FROM ap_rescue_task WHERE id = ?), ?, ?, 'RESCUE', ?, NOW(3))",
                id, nodeType, content, userId);
        return true;
    }

    public Long createAnimal(AnimalCreateRequest request, Long userId) {
        String sql = "INSERT INTO ap_animal (rescue_task_id, name, species, health_summary, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'IN_CARE', NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getRescueTaskId());
            ps.setString(2, request.getName());
            ps.setString(3, request.getSpecies());
            ps.setString(4, request.getSummary());
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
                "SELECT a.id, a.rescue_task_id, a.name, a.species, a.status, a.health_summary, a.created_at " +
                "FROM ap_animal a ORDER BY a.created_at DESC"
        );
        }
        return jdbcTemplate.queryForList(
                "SELECT a.id, a.rescue_task_id, a.name, a.species, a.status, a.health_summary, a.created_at " +
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

    public List<Map<String, Object>> employees(Long userId) {
        Long orgId = findOrgId(userId, true);
        if (!isOrgAdmin(userId, "RESCUE")) {
            return java.util.Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT id, phone, nickname, status, created_at FROM ap_user " +
                        "WHERE role_code = 'RESCUE' AND org_id = ? AND deleted_at IS NULL ORDER BY id ASC",
                orgId
        );
    }

    public List<Map<String, Object>> assignees(Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null) {
            return java.util.Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT id, phone, nickname FROM ap_user WHERE role_code = 'RESCUE' AND org_id = ? AND status = 1 AND deleted_at IS NULL ORDER BY id ASC",
                orgId
        );
    }

    public List<Map<String, Object>> availableAssignees(Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null) {
            return java.util.Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT id, phone, nickname FROM ap_user " +
                        "WHERE role_code = 'RESCUE' AND org_id = ? AND status = 1 AND deleted_at IS NULL " +
                        "AND id NOT IN (SELECT assignee_user_id FROM ap_rescue_task WHERE rescue_org_id = ? " +
                        "AND status IN ('DISPATCHING','DEPARTED','ARRIVED') AND assignee_user_id IS NOT NULL AND deleted_at IS NULL) " +
                        "ORDER BY id ASC",
                orgId, orgId
        );
    }

    public List<Map<String, Object>> vehicles(Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null) {
            return java.util.Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT id, plate_no, vehicle_type, capacity, status, note, created_at " +
                        "FROM ap_rescue_vehicle WHERE org_id = ? AND deleted_at IS NULL ORDER BY id ASC",
                orgId
        );
    }

    public List<Map<String, Object>> availableVehicles(Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null) {
            return java.util.Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT id, plate_no, vehicle_type, capacity, status, note, created_at " +
                        "FROM ap_rescue_vehicle WHERE org_id = ? AND status = 1 AND deleted_at IS NULL " +
                        "AND id NOT IN (SELECT vehicle_id FROM ap_rescue_task WHERE rescue_org_id = ? " +
                        "AND status IN ('DISPATCHING','DEPARTED','ARRIVED') AND vehicle_id IS NOT NULL AND deleted_at IS NULL) " +
                        "ORDER BY id ASC",
                orgId, orgId
        );
    }

    public Long createVehicle(com.animalprotection.dto.RescueVehicleRequest request, Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null) {
            return null;
        }
        jdbcTemplate.update(
                "INSERT INTO ap_rescue_vehicle (org_id, plate_no, vehicle_type, capacity, status, note, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                orgId,
                request.getPlateNo(),
                request.getVehicleType(),
                request.getCapacity(),
                request.getStatus() == null ? 1 : request.getStatus(),
                request.getNote()
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM ap_rescue_vehicle WHERE org_id = ? AND plate_no = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                orgId,
                request.getPlateNo()
        );
    }

    public void updateVehicle(Long id, com.animalprotection.dto.RescueVehicleRequest request, Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE ap_rescue_vehicle SET plate_no = ?, vehicle_type = ?, capacity = ?, status = ?, note = ?, updated_at = NOW(3) " +
                        "WHERE id = ? AND org_id = ? AND deleted_at IS NULL",
                request.getPlateNo(),
                request.getVehicleType(),
                request.getCapacity(),
                request.getStatus() == null ? 1 : request.getStatus(),
                request.getNote(),
                id,
                orgId
        );
    }

    public void deleteVehicle(Long id, Long userId) {
        Long orgId = findOrgId(userId, true);
        if (orgId == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE ap_rescue_vehicle SET deleted_at = NOW(3), updated_at = NOW(3) WHERE id = ? AND org_id = ?",
                id,
                orgId
        );
    }

    public Long createEmployee(com.animalprotection.dto.EmployeeCreateRequest request, Long userId) {
        Long orgId = findOrgId(userId, true);
        if (!isOrgAdmin(userId, "RESCUE")) {
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
                        "VALUES ('RESCUE', ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                orgId, phone, password, request.getNickname(), status
        );
        return jdbcTemplate.queryForObject("SELECT id FROM ap_user WHERE phone = ?", Long.class, phone);
    }

    public void updateEmployee(Long id, com.animalprotection.dto.EmployeeUpdateRequest request, Long userId) {
        Long orgId = findOrgId(userId, true);
        if (!isOrgAdmin(userId, "RESCUE")) {
            return;
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE ap_user SET nickname = ?, password_hash = ?, status = ?, updated_at = NOW(3) " +
                            "WHERE id = ? AND org_id = ? AND role_code = 'RESCUE' AND deleted_at IS NULL",
                    request.getNickname(),
                    request.getPassword(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    id,
                    orgId
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE ap_user SET nickname = ?, status = ?, updated_at = NOW(3) " +
                            "WHERE id = ? AND org_id = ? AND role_code = 'RESCUE' AND deleted_at IS NULL",
                    request.getNickname(),
                    request.getStatus() == null ? 1 : request.getStatus(),
                    id,
                    orgId
            );
        }
    }

    public void deleteEmployee(Long id, Long userId) {
        Long orgId = findOrgId(userId, true);
        if (!isOrgAdmin(userId, "RESCUE")) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE ap_user SET deleted_at = NOW(3), updated_at = NOW(3) WHERE id = ? AND org_id = ? AND role_code = 'RESCUE'",
                id, orgId
        );
    }

    private boolean isOrgAdmin(Long userId, String orgType) {
        if (userId == null) {
            return false;
        }
        Long orgId = findOrgId(userId, true);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_organization WHERE id = ? AND org_type = ? AND admin_user_id = ? AND deleted_at IS NULL",
                Integer.class,
                orgId,
                orgType,
                userId
        );
        return count != null && count > 0;
    }

    private boolean belongsToOrg(Long rescueTaskId, Long orgId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_rescue_task WHERE id = ? AND rescue_org_id = ? AND deleted_at IS NULL",
                Integer.class,
                rescueTaskId,
                orgId
        );
        return count != null && count > 0;
    }

    private boolean assigneeAvailable(Long orgId, Long assigneeUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_rescue_task WHERE rescue_org_id = ? AND assignee_user_id = ? " +
                        "AND status IN ('DISPATCHING','DEPARTED','ARRIVED') AND deleted_at IS NULL",
                Integer.class,
                orgId,
                assigneeUserId
        );
        return count == null || count == 0;
    }

    private boolean vehicleAvailable(Long orgId, Long vehicleId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ap_rescue_task WHERE rescue_org_id = ? AND vehicle_id = ? " +
                        "AND status IN ('DISPATCHING','DEPARTED','ARRIVED') AND deleted_at IS NULL",
                Integer.class,
                orgId,
                vehicleId
        );
        return count == null || count == 0;
    }

    private String normalizeDateTime(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int tIndex = trimmed.indexOf('T');
        if (tIndex > 0) {
            String base = trimmed.substring(0, tIndex);
            int dotIndex = trimmed.indexOf('.');
            String timePart = trimmed.substring(tIndex + 1);
            if (dotIndex > 0) {
                timePart = trimmed.substring(tIndex + 1, dotIndex);
            }
            if (timePart.length() >= 8) {
                return base + " " + timePart.substring(0, 8);
            }
            if (timePart.length() >= 5) {
                return base + " " + timePart.substring(0, 5) + ":00";
            }
            return base + " 00:00:00";
        }
        if (trimmed.length() == 10) {
            return trimmed + " 00:00:00";
        }
        return trimmed;
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
