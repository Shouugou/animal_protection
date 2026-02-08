<template>
  <div class="panel">
    <div class="panel-header">
      <h3>车辆管理</h3>
      <el-button type="primary" size="mini" @click="openCreate">新增车辆</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="plate_no" label="车牌号" width="140" />
      <el-table-column prop="vehicle_type" label="车型/用途" min-width="140" />
      <el-table-column prop="capacity" label="容量" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? "可用" : "停用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="note" label="备注" />
      <el-table-column label="操作" width="200">
        <template slot-scope="scope">
          <el-button size="mini" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="editingId ? '编辑车辆' : '新增车辆'" :visible.sync="dialogVisible" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="车牌号">
          <div class="plate-input">
            <el-select v-model="plate.province" placeholder="省" size="mini" style="width:80px">
              <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
            </el-select>
            <el-select v-model="plate.letter" placeholder="字母" size="mini" style="width:90px">
              <el-option v-for="l in letters" :key="l" :label="l" :value="l" />
            </el-select>
            <el-input v-model="plate.code" placeholder="后5位" size="mini" maxlength="5" />
          </div>
        </el-form-item>
        <el-form-item label="车型/用途">
          <el-input v-model="form.vehicle_type" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option :value="1" label="可用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button size="mini" @click="dialogVisible = false">取消</el-button>
        <el-button size="mini" type="primary" @click="submit">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listRescueVehicles,
  createRescueVehicle,
  updateRescueVehicle,
  deleteRescueVehicle
} from "@/api";

export default {
  name: "RescueVehicles",
  data() {
    return {
      list: [],
      loading: false,
      dialogVisible: false,
      editingId: null,
      provinces: ["京", "津", "沪", "渝", "冀", "豫", "云", "辽", "黑", "湘", "皖", "鲁", "新", "苏", "浙", "赣", "鄂", "桂", "甘", "晋", "蒙", "陕", "吉", "闽", "贵", "粤", "青", "藏", "川", "宁", "琼"],
      letters: "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split(""),
      plate: {
        province: "",
        letter: "",
        code: ""
      },
      form: {
        plate_no: "",
        vehicle_type: "",
        capacity: 0,
        status: 1,
        note: ""
      }
    };
  },
  created() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      this.loading = true;
      try {
        const resp = await listRescueVehicles();
        this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    openCreate() {
      this.editingId = null;
      this.plate = { province: "", letter: "", code: "" };
      this.form = { plate_no: "", vehicle_type: "", capacity: 0, status: 1, note: "" };
      this.dialogVisible = true;
    },
    openEdit(row) {
      this.editingId = row.id;
      this.parsePlate(row.plate_no);
      this.form = {
        plate_no: row.plate_no,
        vehicle_type: row.vehicle_type,
        capacity: row.capacity,
        status: row.status,
        note: row.note
      };
      this.dialogVisible = true;
    },
    async submit() {
      this.form.plate_no = this.buildPlate();
      if (!this.form.plate_no) {
        this.$message.warning("请填写完整车牌号");
        return;
      }
      const payload = {
        plateNo: this.form.plate_no,
        vehicleType: this.form.vehicle_type,
        capacity: this.form.capacity,
        status: this.form.status,
        note: this.form.note
      };
      if (this.editingId) {
        await updateRescueVehicle(this.editingId, payload);
      } else {
        await createRescueVehicle(payload);
      }
      this.dialogVisible = false;
      this.fetchList();
    },
    async remove(row) {
      await deleteRescueVehicle(row.id);
      this.fetchList();
    },
    buildPlate() {
      const province = (this.plate.province || "").trim();
      const letter = (this.plate.letter || "").trim();
      const code = (this.plate.code || "").trim().toUpperCase();
      if (!province || !letter || code.length !== 5) {
        return "";
      }
      return `${province}${letter}${code}`;
    },
    parsePlate(plateNo) {
      if (!plateNo || plateNo.length < 2) {
        this.plate = { province: "", letter: "", code: "" };
        return;
      }
      this.plate = {
        province: plateNo.slice(0, 1),
        letter: plateNo.slice(1, 2),
        code: plateNo.slice(2)
      };
    }
  }
};
</script>

<style scoped>
.panel {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.plate-input {
  display: flex;
  gap: 8px;
  align-items: center;
}
.plate-input .el-input {
  width: 140px;
}
</style>
