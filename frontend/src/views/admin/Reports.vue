<template>
  <el-card>
    <div slot="header">统计报表</div>
    <el-form inline>
      <el-form-item label="时间">
        <el-date-picker v-model="range" type="daterange" range-separator="至" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary">查询</el-button>
      </el-form-item>
    </el-form>
    <el-divider />
    <div>图表区域（对接 /api/admin/reports）</div>
  </el-card>
</template>

<script>
export default {
  name: "AdminReports",
  data() {
    return { range: [] };
  }
};
</script>
