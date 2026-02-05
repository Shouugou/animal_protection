-- 城市动保协同平台 - 数据库初始化（MySQL 5.7）
-- 说明：与 docs/需求规格说明书.md、docs/数据库设计说明书.md 配套

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `animal_protection`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `animal_protection`;

-- =========================
-- 1) 组织与账号
-- =========================

CREATE TABLE IF NOT EXISTS `ap_organization` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `org_type` VARCHAR(16) NOT NULL COMMENT 'LAW=执法部门, RESCUE=救助医疗机构, PLATFORM=平台方',
  `name` VARCHAR(128) NOT NULL,
  `region_code` VARCHAR(12) DEFAULT NULL COMMENT '行政区划编码（可用于数据范围）',
  `address` VARCHAR(255) DEFAULT NULL,
  `contact_name` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_org_type_region` (`org_type`, `region_code`),
  KEY `idx_org_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织（执法/救助/平台）';

CREATE TABLE IF NOT EXISTS `ap_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(16) NOT NULL COMMENT 'PUBLIC/LAW/RESCUE/ADMIN',
  `role_name` VARCHAR(64) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='四端角色';

CREATE TABLE IF NOT EXISTS `ap_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `perm_code` VARCHAR(64) NOT NULL COMMENT '权限点编码（菜单/接口/操作）',
  `perm_name` VARCHAR(128) NOT NULL,
  `perm_type` VARCHAR(16) NOT NULL COMMENT 'MENU/API/ACTION',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_perm_type` (`perm_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限点';

CREATE TABLE IF NOT EXISTS `ap_role_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT UNSIGNED NOT NULL,
  `perm_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
  KEY `idx_rp_role` (`role_id`),
  KEY `idx_rp_perm` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限点';

CREATE TABLE IF NOT EXISTS `ap_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(16) NOT NULL COMMENT 'PUBLIC/LAW/RESCUE/ADMIN',
  `org_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '执法/救助/管理员所属组织；公众为空',
  `phone` VARCHAR(32) NOT NULL,
  `password_hash` VARCHAR(255) DEFAULT NULL COMMENT '可选（若启用密码）',
  `nickname` VARCHAR(64) DEFAULT NULL,
  `is_volunteer` TINYINT NOT NULL DEFAULT 0 COMMENT '公众用户子身份：注册志愿者(1/0)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `last_login_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_role` (`role_code`),
  KEY `idx_user_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户（四端角色）';

-- =========================
-- 2) 事件闭环（核心）
-- =========================

CREATE TABLE IF NOT EXISTS `ap_event` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型（字典）',
  `urgency` VARCHAR(16) NOT NULL COMMENT '紧急程度（字典）',
  `description` TEXT NOT NULL,
  `status` VARCHAR(24) NOT NULL COMMENT '事件状态（REPORTED/TRIAGED/DISPATCHED/PROCESSING/RESOLVED/CLOSED/REJECTED/DUPLICATE等）',
  `reporter_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '上报人（公众）',
  `reporter_anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '1匿名展示 0不匿名',
  `region_code` VARCHAR(12) DEFAULT NULL COMMENT '行政区划编码',
  `address` VARCHAR(255) DEFAULT NULL,
  `latitude` DECIMAL(10,7) DEFAULT NULL,
  `longitude` DECIMAL(10,7) DEFAULT NULL,
  `reported_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '上报时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event_region_status_time` (`region_code`, `status`, `created_at`),
  KEY `idx_event_reporter` (`reporter_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件（公众上报起点）';

CREATE TABLE IF NOT EXISTS `ap_event_timeline` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `event_id` BIGINT UNSIGNED NOT NULL,
  `node_type` VARCHAR(32) NOT NULL COMMENT '时间线节点类型（上报/补充/派单/取证/救助/反馈/公示/归档等）',
  `content` TEXT NOT NULL,
  `operator_role` VARCHAR(16) DEFAULT NULL COMMENT 'PUBLIC/LAW/RESCUE/ADMIN',
  `operator_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_tl_event_time` (`event_id`, `created_at`),
  KEY `idx_tl_operator` (`operator_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件时间线';

CREATE TABLE IF NOT EXISTS `ap_attachment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `biz_type` VARCHAR(32) NOT NULL COMMENT '关联对象类型（EVENT/LAW_EVIDENCE/MEDICAL_RECORD/FOLLOW_UP等）',
  `biz_id` BIGINT UNSIGNED NOT NULL COMMENT '关联对象ID',
  `file_name` VARCHAR(255) DEFAULT NULL,
  `file_url` VARCHAR(512) NOT NULL,
  `mime_type` VARCHAR(64) DEFAULT NULL,
  `file_size` BIGINT UNSIGNED DEFAULT NULL,
  `sha256` CHAR(64) DEFAULT NULL COMMENT '用于防篡改/去重（可选）',
  `uploader_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_att_biz` (`biz_type`, `biz_id`, `created_at`),
  KEY `idx_att_uploader` (`uploader_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一附件';

CREATE TABLE IF NOT EXISTS `ap_comment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `event_id` BIGINT UNSIGNED NOT NULL,
  `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论',
  `author_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `content` TEXT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0隐藏/删除',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cmt_event_time` (`event_id`, `created_at`),
  KEY `idx_cmt_parent` (`parent_id`),
  KEY `idx_cmt_author` (`author_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件评论';

CREATE TABLE IF NOT EXISTS `ap_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `msg_type` VARCHAR(32) NOT NULL COMMENT 'EVENT_UPDATE/WORK_ORDER/RESCUE_TASK/ADOPTION/DONATION等',
  `title` VARCHAR(128) NOT NULL,
  `content` TEXT NOT NULL,
  `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联对象类型',
  `ref_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联对象ID',
  `read_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_msg_user_time` (`user_id`, `created_at`),
  KEY `idx_msg_ref` (`ref_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息/反馈推送记录';

-- =========================
-- 2.1) 志愿者任务与巡护上报（公众端）
-- =========================

CREATE TABLE IF NOT EXISTS `ap_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_type` VARCHAR(24) NOT NULL COMMENT 'PATROL=巡护, RESCUE_SUPPORT=救助协助',
  `title` VARCHAR(128) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `region_code` VARCHAR(12) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `latitude` DECIMAL(10,7) DEFAULT NULL,
  `longitude` DECIMAL(10,7) DEFAULT NULL,
  `start_at` DATETIME(3) DEFAULT NULL,
  `end_at` DATETIME(3) DEFAULT NULL,
  `max_claims` INT NOT NULL DEFAULT 1 COMMENT '最多可认领人数（默认1）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED/CANCELLED',
  `creator_role` VARCHAR(16) DEFAULT NULL COMMENT 'LAW/RESCUE/ADMIN',
  `creator_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_region_status_time` (`region_code`, `status`, `created_at`),
  KEY `idx_task_type_time` (`task_type`, `created_at`),
  KEY `idx_task_creator` (`creator_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者任务（巡护/救助协助）';

CREATE TABLE IF NOT EXISTS `ap_task_claim` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '认领人（公众用户，需为志愿者）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'CLAIMED' COMMENT 'CLAIMED/STARTED/FINISHED/CANCELLED',
  `claimed_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `started_at` DATETIME(3) DEFAULT NULL,
  `finished_at` DATETIME(3) DEFAULT NULL,
  `cancel_reason` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_claim_task_status` (`task_id`, `status`),
  KEY `idx_claim_user_time` (`user_id`, `claimed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务认领记录';

CREATE TABLE IF NOT EXISTS `ap_patrol_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `claim_id` BIGINT UNSIGNED NOT NULL,
  `summary` TEXT DEFAULT NULL COMMENT '巡护总结/描述',
  `distance_km` DECIMAL(10,2) DEFAULT NULL,
  `duration_sec` INT DEFAULT NULL,
  `submitted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_claim` (`claim_id`),
  KEY `idx_report_time` (`submitted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡护上报（轨迹与风险点见下表/附件表）';

CREATE TABLE IF NOT EXISTS `ap_patrol_track_point` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `report_id` BIGINT UNSIGNED NOT NULL,
  `seq_no` INT NOT NULL COMMENT '点序号（从1开始）',
  `latitude` DECIMAL(10,7) NOT NULL,
  `longitude` DECIMAL(10,7) NOT NULL,
  `point_time` DATETIME(3) NOT NULL COMMENT '采集时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_track_report_seq` (`report_id`, `seq_no`),
  KEY `idx_track_report_time` (`report_id`, `point_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡护轨迹点（高频数据，后续可按时间分区扩展）';

CREATE TABLE IF NOT EXISTS `ap_risk_point` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `report_id` BIGINT UNSIGNED NOT NULL,
  `risk_type` VARCHAR(32) NOT NULL COMMENT '风险点类型（字典）',
  `description` TEXT DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `latitude` DECIMAL(10,7) DEFAULT NULL,
  `longitude` DECIMAL(10,7) DEFAULT NULL,
  `found_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '标注时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_rp_report_time` (`report_id`, `found_at`),
  KEY `idx_rp_type_time` (`risk_type`, `found_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡护风险点（附件见 ap_attachment）';

-- =========================
-- 3) 执法部门端
-- =========================

CREATE TABLE IF NOT EXISTS `ap_work_order` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `event_id` BIGINT UNSIGNED NOT NULL,
  `law_org_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(24) NOT NULL COMMENT 'NEW/ACCEPTED/ASSIGNED/ON_SITE/FINISHED/ARCHIVED/REJECTED',
  `need_law_enforcement` TINYINT NOT NULL DEFAULT 1 COMMENT '是否需执法（流程图判断）',
  `transfer_to_rescue` TINYINT NOT NULL DEFAULT 0 COMMENT '是否转送救助医疗机构（流程图判断）',
  `assignee_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被派发执法人员（可选）',
  `accepted_at` DATETIME(3) DEFAULT NULL,
  `finished_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wo_event` (`event_id`),
  KEY `idx_wo_org_status_time` (`law_org_id`, `status`, `created_at`),
  KEY `idx_wo_assignee` (`assignee_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执法工单';

CREATE TABLE IF NOT EXISTS `ap_law_evidence` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `note` TEXT DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `latitude` DECIMAL(10,7) DEFAULT NULL,
  `longitude` DECIMAL(10,7) DEFAULT NULL,
  `collected_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '取证时间',
  `collector_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ev_wo_time` (`work_order_id`, `collected_at`),
  KEY `idx_ev_collector` (`collector_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执法取证记录（附件见 ap_attachment）';

CREATE TABLE IF NOT EXISTS `ap_law_result` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `result_text` TEXT NOT NULL COMMENT '处罚结果/结论（内部）',
  `public_text` TEXT DEFAULT NULL COMMENT '可公示文本（脱敏后）',
  `published_at` DATETIME(3) DEFAULT NULL COMMENT '公示时间',
  `input_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lr_work_order` (`work_order_id`),
  KEY `idx_lr_published` (`published_at`),
  KEY `idx_lr_input_user` (`input_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执法结果录入与公示';

CREATE TABLE IF NOT EXISTS `ap_case_archive` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `archive_no` VARCHAR(64) DEFAULT NULL COMMENT '归档编号',
  `summary` TEXT DEFAULT NULL,
  `archived_at` DATETIME(3) DEFAULT NULL,
  `archiver_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ca_work_order` (`work_order_id`),
  KEY `idx_ca_archive_no` (`archive_no`),
  KEY `idx_ca_archiver` (`archiver_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案件归档（归档包附件见 ap_attachment）';

-- =========================
-- 4) 救助医疗机构端
-- =========================

CREATE TABLE IF NOT EXISTS `ap_rescue_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `event_id` BIGINT UNSIGNED NOT NULL,
  `rescue_org_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(24) NOT NULL COMMENT 'NEW/GRABBED/DISPATCHING/ARRIVED/INTAKE/TREATING/CLOSED/REJECTED',
  `need_rescue` TINYINT NOT NULL DEFAULT 1 COMMENT '评估是否救助（流程图判断）',
  `dispatch_note` TEXT DEFAULT NULL COMMENT '调度说明',
  `dispatch_at` DATETIME(3) DEFAULT NULL,
  `arrived_at` DATETIME(3) DEFAULT NULL,
  `intake_at` DATETIME(3) DEFAULT NULL,
  `closed_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rt_event` (`event_id`),
  KEY `idx_rt_org_status_time` (`rescue_org_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='救助任务';

CREATE TABLE IF NOT EXISTS `ap_animal` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rescue_task_id` BIGINT UNSIGNED NOT NULL,
  `shelter_no` VARCHAR(64) DEFAULT NULL COMMENT '入站编号',
  `species` VARCHAR(32) NOT NULL COMMENT '物种',
  `breed` VARCHAR(64) DEFAULT NULL COMMENT '品种/花色（可选）',
  `sex` VARCHAR(8) DEFAULT NULL COMMENT 'M/F/U',
  `age_estimate` VARCHAR(32) DEFAULT NULL COMMENT '年龄估计',
  `health_summary` TEXT DEFAULT NULL COMMENT '体检/健康摘要',
  `status` VARCHAR(24) NOT NULL DEFAULT 'IN_CARE' COMMENT 'IN_CARE/READY_FOR_ADOPTION/ADOPTED/RELEASED/DECEASED',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_animal_task` (`rescue_task_id`),
  KEY `idx_animal_shelter_no` (`shelter_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动物档案（入站体检建档）';

CREATE TABLE IF NOT EXISTS `ap_medical_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `animal_id` BIGINT UNSIGNED NOT NULL,
  `record_type` VARCHAR(24) NOT NULL COMMENT 'CHECKUP/TREATMENT/MEDICATION/REHAB/OTHER',
  `record_content` JSON NOT NULL COMMENT '结构化记录（诊疗/用药/康复等）',
  `recorded_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `recorder_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_mr_animal_time` (`animal_id`, `recorded_at`),
  KEY `idx_mr_recorder` (`recorder_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗记录（附件见 ap_attachment）';

CREATE TABLE IF NOT EXISTS `ap_inventory_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `org_id` BIGINT UNSIGNED NOT NULL,
  `item_name` VARCHAR(128) NOT NULL,
  `spec` VARCHAR(128) DEFAULT NULL,
  `unit` VARCHAR(16) DEFAULT NULL,
  `low_stock_threshold` DECIMAL(18,3) DEFAULT NULL COMMENT '低库存预警阈值',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inv_item_org` (`org_id`, `item_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存物资/药品品项';

CREATE TABLE IF NOT EXISTS `ap_inventory_batch` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `item_id` BIGINT UNSIGNED NOT NULL,
  `batch_no` VARCHAR(64) DEFAULT NULL,
  `expiry_date` DATE DEFAULT NULL,
  `qty` DECIMAL(18,3) NOT NULL DEFAULT 0 COMMENT '当前库存数量（可用于统计与预警）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_inv_batch_item` (`item_id`),
  KEY `idx_inv_batch_expiry` (`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存批次（过期预警）';

CREATE TABLE IF NOT EXISTS `ap_inventory_txn` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `org_id` BIGINT UNSIGNED NOT NULL,
  `item_id` BIGINT UNSIGNED NOT NULL,
  `batch_id` BIGINT UNSIGNED DEFAULT NULL,
  `txn_type` VARCHAR(16) NOT NULL COMMENT 'IN/OUT/ADJUST',
  `qty` DECIMAL(18,3) NOT NULL,
  `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联对象类型（RESCUE_TASK/ANIMAL等）',
  `ref_id` BIGINT UNSIGNED DEFAULT NULL,
  `note` VARCHAR(255) DEFAULT NULL,
  `operator_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_inv_txn_org_time` (`org_id`, `created_at`),
  KEY `idx_inv_txn_item_time` (`item_id`, `created_at`),
  KEY `idx_inv_txn_batch` (`batch_id`),
  KEY `idx_inv_txn_operator` (`operator_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水';

-- =========================
-- 5) 领养与回访
-- =========================

CREATE TABLE IF NOT EXISTS `ap_adoption` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `animal_id` BIGINT UNSIGNED NOT NULL,
  `applicant_user_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(24) NOT NULL COMMENT 'APPLIED/APPROVED/REJECTED/CANCELLED',
  `apply_form` JSON NOT NULL COMMENT '领养申请表（结构化）',
  `applied_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `decided_at` DATETIME(3) DEFAULT NULL,
  `decision_note` TEXT DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_adopt_animal` (`animal_id`),
  KEY `idx_adopt_applicant_time` (`applicant_user_id`, `applied_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领养申请';

CREATE TABLE IF NOT EXISTS `ap_follow_up` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `adoption_id` BIGINT UNSIGNED NOT NULL,
  `due_at` DATETIME(3) DEFAULT NULL COMMENT '回访到期时间（可按规则生成）',
  `questionnaire` JSON NOT NULL COMMENT '回访问卷答案',
  `submitted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_fu_adoption_time` (`adoption_id`, `submitted_at`),
  KEY `idx_fu_due` (`due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回访记录（附件见 ap_attachment）';

-- =========================
-- 6) 公益捐助 & 动保课堂
-- =========================

CREATE TABLE IF NOT EXISTS `ap_donation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `donor_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '捐赠人（可匿名/为空）',
  `anonymous` TINYINT NOT NULL DEFAULT 0,
  `target_type` VARCHAR(16) NOT NULL COMMENT 'EVENT/ORG',
  `target_id` BIGINT UNSIGNED NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `status` VARCHAR(16) NOT NULL COMMENT 'PENDING/SUCCEEDED/FAILED/OFFLINE_RECORDED',
  `donated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_don_target_time` (`target_type`, `target_id`, `donated_at`),
  KEY `idx_don_donor_time` (`donor_user_id`, `donated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定向捐赠';

CREATE TABLE IF NOT EXISTS `ap_content_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort_no` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cat_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动保课堂分类';

CREATE TABLE IF NOT EXISTS `ap_content` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `content_type` VARCHAR(16) NOT NULL COMMENT 'ARTICLE/VIDEO',
  `category_id` BIGINT UNSIGNED DEFAULT NULL,
  `title` VARCHAR(128) NOT NULL,
  `body` MEDIUMTEXT DEFAULT NULL COMMENT '图文正文',
  `video_url` VARCHAR(512) DEFAULT NULL COMMENT '视频地址',
  `cover_url` VARCHAR(512) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
  `published_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ct_cat_time` (`category_id`, `published_at`),
  KEY `idx_ct_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动保课堂内容';

-- =========================
-- 7) 审核与审计（管理员端）
-- =========================

CREATE TABLE IF NOT EXISTS `ap_approval_flow` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `flow_code` VARCHAR(32) NOT NULL COMMENT '流程编码',
  `flow_name` VARCHAR(64) NOT NULL,
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型（ADOPTION/DONATION/OTHER等，按需扩展）',
  `steps` JSON NOT NULL COMMENT '多级审核节点定义（顺序、角色、规则）',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_code` (`flow_code`),
  KEY `idx_flow_biz` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核流程定义';

CREATE TABLE IF NOT EXISTS `ap_approval_instance` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `flow_id` BIGINT UNSIGNED NOT NULL,
  `biz_type` VARCHAR(32) NOT NULL,
  `biz_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(16) NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  `current_step` INT NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inst_biz` (`biz_type`, `biz_id`),
  KEY `idx_inst_flow_status` (`flow_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核实例';

CREATE TABLE IF NOT EXISTS `ap_audit_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `actor_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `actor_role` VARCHAR(16) DEFAULT NULL,
  `action` VARCHAR(64) NOT NULL COMMENT '操作类型（如 ROLE_UPDATE/FLOW_UPDATE/WORK_ORDER_UPDATE等）',
  `target_type` VARCHAR(32) DEFAULT NULL,
  `target_id` BIGINT UNSIGNED DEFAULT NULL,
  `detail` JSON DEFAULT NULL COMMENT '操作明细（前后差异/参数等）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_audit_actor_time` (`actor_user_id`, `created_at`),
  KEY `idx_audit_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';

-- =========================
-- 8) 基础数据（四端角色）
-- =========================

INSERT IGNORE INTO `ap_role` (`role_code`, `role_name`, `status`)
VALUES
  ('PUBLIC', '公众用户', 1),
  ('LAW', '执法部门', 1),
  ('RESCUE', '救助医疗机构', 1),
  ('ADMIN', '系统管理员', 1);
