<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>任务大厅</span>
    </div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="title" label="任务" />
      <el-table-column prop="task_type" label="类型" width="140" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="claim(scope.row)">认领</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
export default {
  name: "PublicTasks",
  data() {
    return {
      list: [{ id: 1, title: "西城区巡护", task_type: "PATROL", status: "OPEN" }]
    };
  },
  methods: {
    claim(item) {
      this.$router.push(`/public/patrol/submit/${item.id}`);
    }
  }
};
</script>
