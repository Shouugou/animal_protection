<template>
  <el-card>
    <div slot="header">统计报表</div>
    <el-form inline class="filter-form">
      <el-form-item label="时间">
        <el-date-picker
          v-model="range"
          type="daterange"
          range-separator="至"
          value-format="yyyy-MM-dd"
          class="range-picker"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-divider />
    <el-row :gutter="12">
      <el-col :span="12">
        <el-card>
          <div slot="header">事件类型分布</div>
          <div class="bar-list">
            <div v-for="item in data.byType" :key="item.event_type" class="bar-row">
              <div class="bar-label">{{ item.event_type }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(item.cnt, data.byType) }"></div>
              </div>
              <div class="bar-value">{{ item.cnt }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div slot="header">事件状态分布</div>
          <div class="bar-list">
            <div v-for="item in data.byStatus" :key="item.status" class="bar-row">
              <div class="bar-label">{{ statusText(item.status) }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(item.cnt, data.byStatus) }"></div>
              </div>
              <div class="bar-value">{{ item.cnt }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="12">
        <el-card>
          <div slot="header">捐赠统计</div>
          <el-table :data="data.donationByTarget" size="mini">
            <el-table-column prop="target_type" label="类型" width="100" />
            <el-table-column prop="cnt" label="笔数" width="120" />
            <el-table-column prop="amount" label="金额" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div slot="header">领养状态</div>
          <el-table :data="data.adoptionByStatus" size="mini">
            <el-table-column prop="status" label="状态" width="140" />
            <el-table-column prop="cnt" label="数量" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<script>
import { fetchAdminReports } from "@/api";

export default {
  name: "AdminReports",
  data() {
    return {
      range: [],
      loading: false,
      data: {
        byType: [],
        byStatus: [],
        byDay: [],
        donationByTarget: [],
        adoptionByStatus: []
      }
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      return {
        REPORTED: "已上报",
        TRIAGED: "已分流",
        DISPATCHED: "已派单",
        PROCESSING: "处理中",
        RESCUE_PROCESSING: "救助中",
        CLOSED: "已办结",
        REJECTED: "不予受理",
        RESOLVED: "已处理"
      }[status] || status;
    },
    barWidth(count, list) {
      const max = Math.max(...list.map((i) => i.cnt || 0), 1);
      return `${Math.round((count / max) * 100)}%`;
    },
    async fetch() {
      this.loading = true;
      try {
        const params = {};
        if (this.range && this.range.length === 2) {
          params.startDate = this.range[0];
          params.endDate = this.range[1];
        }
        const resp = await fetchAdminReports(params);
        if (resp.code === 0) {
          this.data = resp.data || this.data;
        }
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.filter-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.range-picker {
  width: 360px;
}
.range-picker ::v-deep .el-range-separator {
  width: 24px;
  text-align: center;
}
.bar-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bar-label {
  width: 100px;
  color: #555;
}
.bar-track {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: #67c23a;
}
.bar-value {
  width: 40px;
  text-align: right;
}
</style>
