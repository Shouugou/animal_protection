<template>
  <el-card>
    <div slot="header">治疗记录（动物ID: {{ id }}）</div>
    <el-form label-width="90px" style="max-width:520px">
      <el-form-item label="记录类型">
        <el-select v-model="form.type">
          <el-option label="检查" value="CHECKUP" />
          <el-option label="治疗" value="TREATMENT" />
          <el-option label="用药" value="MEDICATION" />
          <el-option label="康复" value="REHAB" />
        </el-select>
      </el-form-item>
      <el-form-item label="记录内容">
        <el-input type="textarea" v-model="form.content" rows="3" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>

    <el-divider />
    <el-table :data="list" style="width:100%" v-loading="loading">
      <el-table-column prop="record_type" label="类型" width="140" />
      <el-table-column prop="record_content" label="内容" />
      <el-table-column prop="recorded_at" label="时间" width="200" />
    </el-table>

    <el-divider />
    <el-card>
      <div slot="header">病历共享</div>
      <el-form label-width="90px">
        <el-form-item label="共享对象">
          <el-select v-model="share.targets" multiple placeholder="选择机构/人员">
            <el-option label="救助机构A" value="ORG_A" />
            <el-option label="执法部门B" value="LAW_B" />
          </el-select>
        </el-form-item>
        <el-form-item label="共享说明">
          <el-input type="textarea" v-model="share.note" rows="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="sharing" @click="shareRecord">发起共享</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </el-card>
</template>

<script>
import { listMedicalRecords, addMedicalRecord, shareMedicalRecord } from "@/api";

export default {
  name: "MedicalRecords",
  data() {
    return {
      id: this.$route.params.id,
      form: { type: "CHECKUP", content: "" },
      list: [],
      share: { targets: [], note: "" },
      loading: false,
      saving: false,
      sharing: false
    };
  },
  created() {
    this.fetch();
  },
  methods: {
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
    async fetch() {
      this.loading = true;
      try {
        const resp = await listMedicalRecords(this.id);
        if (resp.code === 0) {
          this.list = (resp.data || []).map((item) => ({
            ...item,
            record_content: this.parseContent(item.record_content),
            recorded_at: this.formatTime(item.recorded_at)
          }));
        }
      } finally {
        this.loading = false;
      }
    },
    async save() {
      if (!this.form.content) {
        this.$message.warning("请填写记录内容");
        return;
      }
      this.saving = true;
      try {
        const resp = await addMedicalRecord({
          animalId: Number(this.id),
          recordType: this.form.type,
          recordContent: this.form.content
        });
        if (resp.code === 0) {
          this.$message.success("已保存");
          this.form.content = "";
          this.fetch();
        }
      } finally {
        this.saving = false;
      }
    },
    async shareRecord() {
      this.sharing = true;
      try {
        const resp = await shareMedicalRecord({
          animalId: Number(this.id),
          targets: this.share.targets,
          note: this.share.note
        });
        if (resp.code === 0) {
          this.$message.success("共享已发起");
          this.share.targets = [];
          this.share.note = "";
        }
      } finally {
        this.sharing = false;
      }
    }
  }
};
</script>
