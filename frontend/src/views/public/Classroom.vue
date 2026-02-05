<template>
  <el-card>
    <div slot="header">动保课堂</div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content_type" label="类型" width="120" />
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button size="mini" @click="open(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="内容详情" :visible.sync="show" width="600px">
      <p>{{ current.title }}</p>
      <p>{{ current.body }}</p>
      <span slot="footer">
        <el-button @click="show=false">关闭</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import { listContent, getContent } from "@/api";

export default {
  name: "Classroom",
  data() {
    return {
      list: [],
      loading: false,
      show: false,
      current: {}
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    async fetch() {
      this.loading = true;
      try {
        const resp = await listContent();
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    async open(row) {
      const resp = await getContent(row.id);
      if (resp.code === 0) {
        this.current = resp.data || {};
        this.show = true;
      }
    }
  }
};
</script>
