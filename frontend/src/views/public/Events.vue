<template>
  <div>
    <el-card>
      <div slot="header" class="section-title">
        <span>事件列表</span>
        <el-button type="primary" size="mini" @click="$router.push('/public/events/new')">上报事件</el-button>
      </div>
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="类型/描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="status" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="已上报" value="REPORTED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已处置" value="RESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" style="width:100%">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="event_type" label="类型" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="reported_at" label="时间" width="160" />
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-link type="primary" @click="open(scope.row)">查看</el-link>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "PublicEvents",
  data() {
    return {
      keyword: "",
      status: "",
      list: [
        { id: 10001, event_type: "受伤动物", status: "REPORTED", reported_at: "2026-02-05" }
      ]
    };
  },
  methods: {
    search() {},
    open(row) {
      this.$router.push(`/public/events/${row.id}`);
    }
  }
};
</script>
