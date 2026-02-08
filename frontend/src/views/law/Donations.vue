<template>
  <el-card>
    <div slot="header">捐赠记录</div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="记录ID" width="90" />
      <el-table-column label="捐赠目标">
        <template slot-scope="scope">
          <span v-if="scope.row.target_type === 'EVENT'">
            事件 #{{ scope.row.target_id }} {{ scope.row.event_type || "" }} {{ scope.row.event_address || "" }}
          </span>
          <span v-else>
            机构 {{ scope.row.org_name || "" }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="donor_name" label="捐赠人" width="140" />
      <el-table-column prop="amount" label="金额" width="120" />
      <el-table-column prop="donated_at" label="时间" width="180" />
      <el-table-column prop="status" label="状态" width="120" />
    </el-table>
  </el-card>
</template>

<script>
import { listOrgDonations } from "@/api";

export default {
  name: "LawDonations",
  data() {
    return {
      loading: false,
      list: []
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    async fetch() {
      this.loading = true;
      try {
        const resp = await listOrgDonations();
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
