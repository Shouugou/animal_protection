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

    public List<Map<String, Object>> tasks() {
        return jdbcTemplate.queryForList("SELECT id, status, need_rescue FROM ap_rescue_task ORDER BY created_at DESC");
    }

    public void evaluate(Long id, RescueEvaluateRequest request) {
        jdbcTemplate.update("UPDATE ap_rescue_task SET need_rescue = ?, status = ?, updated_at = NOW(3) WHERE id = ?",
                request.getNeedRescue(), request.getNeedRescue() != null && request.getNeedRescue() ? "PROCESSING" : "REJECTED", id);
    }

    public void dispatch(Long id, RescueDispatchRequest request) {
        jdbcTemplate.update("UPDATE ap_rescue_task SET dispatch_note = ?, dispatch_at = NOW(3), arrived_at = ?, intake_at = ?, updated_at = NOW(3) WHERE id = ?",
                request.getNote(), request.getArrive(), request.getIntake(), id);
    }

    public Long createAnimal(AnimalCreateRequest request) {
        String sql = "INSERT INTO ap_animal (rescue_task_id, species, health_summary, status, created_at, updated_at) VALUES (?, ?, ?, 'IN_CARE', NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getRescueTaskId());
            ps.setString(2, request.getSpecies());
            ps.setString(3, request.getSummary());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> animals() {
        return jdbcTemplate.queryForList("SELECT id, species, status FROM ap_animal ORDER BY created_at DESC");
    }

    public Long addMedicalRecord(MedicalRecordRequest request, Long recorderUserId) {
        String sql = "INSERT INTO ap_medical_record (animal_id, record_type, record_content, recorded_at, recorder_user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(3), ?, NOW(3), NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getAnimalId());
            ps.setString(2, request.getRecordType());
            ps.setString(3, request.getRecordContent());
            ps.setObject(4, recorderUserId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> inventoryItems(Long orgId) {
        return jdbcTemplate.queryForList("SELECT id, item_name, low_stock_threshold FROM ap_inventory_item WHERE org_id = ?", orgId);
    }

    public Long addInventoryTxn(InventoryTxnRequest request, Long operatorUserId) {
        String sql = "INSERT INTO ap_inventory_txn (org_id, item_id, batch_id, txn_type, qty, ref_type, ref_id, note, operator_user_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(3))";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getOrgId());
            ps.setLong(2, request.getItemId());
            ps.setObject(3, request.getBatchId());
            ps.setString(4, request.getTxnType());
            ps.setObject(5, request.getQty());
            ps.setString(6, request.getRefType());
            ps.setObject(7, request.getRefId());
            ps.setString(8, request.getNote());
            ps.setObject(9, operatorUserId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
