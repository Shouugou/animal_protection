<template>
  <el-card>
    <div slot="header">执法工单</div>
    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="status" placeholder="全部">
          <el-option label="全部" value="" />
          <el-option label="NEW" value="NEW" />
          <el-option label="FINISHED" value="FINISHED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="need_law_enforcement" label="是否执法">
        <template slot-scope="scope">
          {{ scope.row.need_law_enforcement ? "是" : "否" }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-link type="primary" @click="open(scope.row)">处理</el-link>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
export default {
  name: "LawWorkOrders",
  data() {
    return {
      status: "",
      list: [{ id: 3001, status: "NEW", need_law_enforcement: true }]
    };
  },
  methods: {
    search() {},
    open(row) {
      this.$router.push(`/law/workorders/${row.id}`);
    }
  }
};
</script>
