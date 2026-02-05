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
        <el-button type="primary" @click="save">保存</el-button>
      </el-form-item>
    </el-form>

    <el-divider />
    <el-table :data="list" style="width:100%">
      <el-table-column prop="type" label="类型" width="140" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="time" label="时间" width="160" />
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
          <el-button type="primary" @click="shareRecord">发起共享</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </el-card>
</template>

<script>
export default {
  name: "MedicalRecords",
  data() {
    return {
      id: this.$route.params.id,
      form: { type: "CHECKUP", content: "" },
      list: [],
      share: { targets: [], note: "" }
    };
  },
  methods: {
    save() {
      this.list.unshift({
        type: this.form.type,
        content: this.form.content,
        time: new Date().toISOString().slice(0, 19).replace("T", " ")
      });
      this.form.content = "";
    },
    shareRecord() {
      this.$message.success("共享已发起（演示）");
    }
  }
};
</script>
