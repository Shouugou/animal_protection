<template>
  <el-card>
    <div slot="header">公益捐助</div>
    <el-form label-width="90px" style="max-width:420px">
      <el-form-item label="目标类型">
        <el-select v-model="form.target_type">
          <el-option label="事件" value="EVENT" />
          <el-option label="机构" value="ORG" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标ID">
        <el-input v-model="form.target_id" />
      </el-form-item>
      <el-form-item label="金额">
        <el-input v-model="form.amount" />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="form.anonymous">匿名捐赠</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="donate">捐赠</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
export default {
  name: "Donations",
  data() {
    return {
      form: { target_type: "EVENT", target_id: "", amount: "", anonymous: false }
    };
  },
  methods: {
    donate() {
      this.$message.success("捐赠已提交（演示）");
    }
  }
};
</script>
