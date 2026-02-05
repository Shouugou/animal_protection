<template>
  <el-card>
    <div slot="header">数据监控大屏</div>
    <el-row :gutter="12">
      <el-col :span="6">
        <el-card><div>今日新增</div><h2>12</h2></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div>处理中</div><h2>7</h2></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div>已闭环</div><h2>5</h2></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div>领养成功</div><h2>3</h2></el-card>
      </el-col>
    </el-row>
    <el-divider />
    <div>地图态势/趋势图（对接 ECharts）</div>
  </el-card>
</template>

<script>
export default { name: "AdminDashboard" };
</script>
