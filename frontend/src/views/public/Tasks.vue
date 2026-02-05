<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>任务大厅</span>
    </div>
    <el-table :data="list" v-loading="loading" style="width:100%">
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
import { listTasks, claimTask } from "@/api";

export default {
  name: "PublicTasks",
  data() {
    return {
      list: [],
      loading: false
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    async fetch() {
      this.loading = true;
      try {
        const resp = await listTasks();
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    async claim(item) {
      const resp = await claimTask(item.id);
      if (resp.code === 0) {
        const claimId = resp.data;
        this.$message.success("认领成功");
        this.$router.push(`/public/patrol/submit/${claimId}`);
      }
    }
  }
};
</script>
