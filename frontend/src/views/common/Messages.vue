<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>消息中心</span>
      <el-button size="mini" @click="markAll">全部已读</el-button>
    </div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="created_at" label="时间" width="160" />
      <el-table-column label="状态" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.read ? 'success' : 'warning'">
            {{ scope.row.read ? "已读" : "未读" }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
export default {
  name: "Messages",
  data() {
    return {
      list: [
        { id: 1, title: "事件反馈", content: "事件已受理", created_at: "2026-02-05", read: false }
      ]
    };
  },
  methods: {
    markAll() {
      this.list = this.list.map((i) => ({ ...i, read: true }));
    }
  }
};
</script>
