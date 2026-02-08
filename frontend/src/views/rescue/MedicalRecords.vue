<template>
  <el-card>
    <div slot="header">治疗记录</div>
    <el-form label-width="90px" style="max-width:560px">
        <el-form-item label="选择动物">
          <el-select v-model="selectedId" placeholder="选择动物" @change="onSelect">
            <el-option
              v-for="a in animals"
              :key="a.id"
              :label="`${a.name || '未命名'}-${a.species}`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
      <el-form-item label="记录类型">
        <el-select v-model="form.type">
          <el-option label="检查" value="CHECKUP" />
          <el-option label="治疗" value="TREATMENT" />
          <el-option label="用药" value="MEDICATION" />
          <el-option label="康复" value="REHAB" />
          <el-option label="治疗完成" value="TREATMENT_DONE" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.type !== 'MEDICATION'" label="记录内容">
        <el-input type="textarea" v-model="form.content" rows="3" />
      </el-form-item>
      <el-form-item v-else label="药品选择">
        <el-select v-model="medication.itemId" placeholder="选择药品">
          <el-option
            v-for="item in inventory"
            :key="item.id"
            :label="`${item.item_name} (库存:${item.stock_qty})`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.type === 'MEDICATION'" label="用药份数">
        <el-input-number v-model="medication.qty" :min="1" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>

    <el-divider />
    <el-table :data="list" style="width:100%" v-loading="loading">
      <el-table-column prop="record_type_text" label="类型" width="140" />
      <el-table-column prop="record_content" label="内容" />
      <el-table-column label="时间" width="240">
        <template slot-scope="scope">
          <span class="nowrap">{{ scope.row.recorded_at }}</span>
        </template>
      </el-table-column>
    </el-table>

  </el-card>
</template>

<script>
import {
  listMedicalRecords,
  addMedicalRecord,
  listRescueAnimals,
  listInventoryItems,
  addInventoryTxn
} from "@/api";

export default {
  name: "MedicalRecords",
  data() {
    return {
      selectedId: null,
      animals: [],
      inventory: [],
      form: { type: "CHECKUP", content: "" },
      medication: { itemId: null, qty: 1 },
      list: [],
      loading: false,
      saving: false
    };
  },
  created() {
    this.loadAnimals();
    this.loadInventory();
  },
  methods: {
    async loadInventory() {
      const resp = await listInventoryItems();
      if (resp.code === 0) {
        this.inventory = resp.data || [];
      }
    },
    formatTime(value) {
      if (!value) return "";
      let d = null;
      if (typeof value === "number") {
        d = new Date(value);
      } else if (typeof value === "string") {
        const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/);
        if (match) {
          d = new Date(
            Number(match[1]),
            Number(match[2]) - 1,
            Number(match[3]),
            Number(match[4]),
            Number(match[5]),
            Number(match[6])
          );
        } else {
          d = new Date(value);
        }
      } else if (value instanceof Date) {
        d = value;
      }
      if (!d || Number.isNaN(d.getTime())) return "";
      const pad = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日${pad(d.getHours())}时${pad(d.getMinutes())}分${pad(d.getSeconds())}秒`;
    },
    parseContent(raw) {
      if (!raw) return "";
      try {
        const obj = JSON.parse(raw);
        return obj.text || raw;
      } catch (e) {
        return raw;
      }
    },
    async loadAnimals() {
      const resp = await listRescueAnimals();
      if (resp.code === 0) {
        this.animals = resp.data || [];
        const routeId = Number(this.$route.params.id);
        if (routeId) {
          this.selectedId = routeId;
        } else if (this.animals.length > 0) {
          this.selectedId = this.animals[0].id;
        }
        if (this.selectedId) {
          this.fetch();
        }
      }
    },
    async fetch() {
      if (!this.selectedId) {
        this.list = [];
        return;
      }
      this.loading = true;
      try {
        const resp = await listMedicalRecords(this.selectedId);
        if (resp.code === 0) {
          this.list = (resp.data || []).map((item) => ({
            ...item,
            record_type_text: this.recordTypeText(item.record_type),
            record_content: this.parseContent(item.record_content),
            recorded_at: this.formatTime(item.recorded_at)
          }));
        }
      } finally {
        this.loading = false;
      }
    },
    recordTypeText(type) {
      return {
        CHECKUP: "检查",
        TREATMENT: "治疗",
        MEDICATION: "用药",
        REHAB: "康复",
        TREATMENT_DONE: "治疗完成",
        OTHER: "其他"
      }[type] || type || "未知";
    },
    onSelect() {
      if (this.selectedId) {
        this.$router.replace(`/rescue/animals/${this.selectedId}/records`);
      }
      this.fetch();
    },
    async save() {
      if (this.form.type !== "MEDICATION" && !this.form.content) {
        this.$message.warning("请填写记录内容");
        return;
      }
      if (!this.selectedId) {
        this.$message.warning("请选择动物");
        return;
      }
      if (this.form.type === "MEDICATION") {
        if (!this.medication.itemId || !this.medication.qty) {
          this.$message.warning("请选择药品并填写用药份数");
          return;
        }
        const item = this.inventory.find((i) => i.id === this.medication.itemId);
        if (!item) {
          this.$message.warning("药品不存在");
          return;
        }
        const txnResp = await addInventoryTxn({
          itemId: this.medication.itemId,
          txnType: "OUT",
          qty: Number(this.medication.qty),
          note: `动物用药-${item.item_name}`
        });
        if (txnResp.code !== 0) {
          this.$message.error(txnResp.message || "库存出库失败");
          return;
        }
        this.form.content = `${item.item_name} x${this.medication.qty}`;
      }
      this.saving = true;
      try {
        const resp = await addMedicalRecord({
          animalId: Number(this.selectedId),
          recordType: this.form.type,
          recordContent: this.form.content
        });
        if (resp.code === 0) {
          this.$message.success("已保存");
          this.form.content = "";
          this.medication = { itemId: null, qty: 1 };
          this.loadInventory();
          this.fetch();
        }
      } finally {
        this.saving = false;
      }
    },
  }
};
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
}
</style>
