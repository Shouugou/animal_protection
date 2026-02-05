<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>消息中心</span>
      <el-button size="mini" @click="fetch">刷新</el-button>
    </div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="created_at" label="时间" width="160" />
    </el-table>
  </el-card>
</template>

<script>
import { fetchMessages } from "@/api";

export default {
  name: "Messages",
  data() {
    return { list: [], loading: false };
  },
  created() {
    this.fetch();
  },
  methods: {
    async fetch() {
      this.loading = true;
      try {
        const resp = await fetchMessages();
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
