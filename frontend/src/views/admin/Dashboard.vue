<template>
  <div class="dashboard">
    <el-card>
      <div slot="header">数据监控大屏</div>
      <el-row :gutter="12">
        <el-col :span="6">
          <el-card class="stat-card"><div>事件总量</div><h2>{{ metrics.total || 0 }}</h2></el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card"><div>今日新增</div><h2>{{ metrics.today || 0 }}</h2></el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card"><div>处理中</div><h2>{{ metrics.processing || 0 }}</h2></el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card"><div>已办结</div><h2>{{ metrics.closed || 0 }}</h2></el-card>
        </el-col>
      </el-row>
      <el-row :gutter="12" style="margin-top:12px">
        <el-col :span="6">
          <el-card class="stat-card"><div>领养成功</div><h2>{{ metrics.adoption_count || 0 }}</h2></el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card"><div>捐赠总额</div><h2>{{ metrics.donation_amount || 0 }}</h2></el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="14">
        <el-card>
          <div slot="header">事件态势地图</div>
          <div v-if="!hasMapData" class="empty-map">暂无事件定位数据</div>
          <div v-else class="map-panel" ref="map"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <div slot="header">最新事件动态</div>
          <el-table :data="events" height="320" v-loading="loadingEvents" size="mini">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="event_type" label="类型" width="120" />
            <el-table-column label="状态" width="120">
              <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
            </el-table-column>
            <el-table-column prop="address" label="地点" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="24">
        <el-card>
          <div slot="header">事件状态分布</div>
          <div class="bar-list" v-loading="loadingStatus">
            <div v-for="item in statusStats" :key="item.status" class="bar-row">
              <div class="bar-label">{{ statusText(item.status) }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(item.cnt) }"></div>
              </div>
              <div class="bar-value">{{ item.cnt }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { fetchDashboardMetrics, fetchDashboardEvents, fetchAdminReports } from "@/api";

export default {
  name: "AdminDashboard",
  computed: {
    hasMapData() {
      return (this.events || []).some((e) => e.latitude && e.longitude);
    }
  },
  data() {
    return {
      metrics: {},
      events: [],
      statusStats: [],
      loadingEvents: false,
      loadingStatus: false,
      map: null,
      markers: []
    };
  },
  created() {
    this.loadMetrics();
    this.loadEvents();
    this.loadStatus();
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
    barWidth(count) {
      const max = Math.max(...this.statusStats.map((i) => i.cnt || 0), 1);
      return `${Math.round((count / max) * 100)}%`;
    },
    async loadMetrics() {
      const resp = await fetchDashboardMetrics();
      if (resp.code === 0) this.metrics = resp.data || {};
    },
    async loadEvents() {
      this.loadingEvents = true;
      try {
        const resp = await fetchDashboardEvents();
        if (resp.code === 0) this.events = resp.data || [];
        this.$nextTick(() => this.renderMap());
      } finally {
        this.loadingEvents = false;
      }
    },
    async loadStatus() {
      this.loadingStatus = true;
      try {
        const resp = await fetchAdminReports();
        if (resp.code === 0) this.statusStats = (resp.data && resp.data.byStatus) || [];
      } finally {
        this.loadingStatus = false;
      }
    },
    renderMap() {
      if (!window.BMap || !window.BMap.Map) return;
      const container = this.$refs.map;
      if (!container) return;
      const points = this.events
        .filter((e) => e.latitude && e.longitude)
        .map((e) => ({
          id: e.id,
          event_type: e.event_type,
          lat: Number(e.latitude),
          lng: Number(e.longitude)
        }))
        .filter((e) => !Number.isNaN(e.lat) && !Number.isNaN(e.lng));
      if (!this.map) {
        this.map = new window.BMap.Map(container);
        this.map.enableScrollWheelZoom(true);
      }
      this.markers.forEach((m) => this.map.removeOverlay(m));
      this.markers = [];
      if (points.length === 0) {
        return;
      }
      const first = points[0];
      const center = new window.BMap.Point(first.lng, first.lat);
      this.map.centerAndZoom(center, 12);
      points.forEach((p) => {
        const point = new window.BMap.Point(p.lng, p.lat);
        const marker = new window.BMap.Marker(point);
        const label = new window.BMap.Label(`${p.id} ${p.event_type || ""}`.trim(), {
          offset: new window.BMap.Size(20, -10)
        });
        marker.setLabel(label);
        this.map.addOverlay(marker);
        this.markers.push(marker);
      });
    }
  }
};
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.stat-card h2 {
  margin: 6px 0 0;
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
  width: 90px;
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
  background: #409eff;
}
.bar-value {
  width: 40px;
  text-align: right;
}
.map-panel {
  width: 100%;
  height: 360px;
  border-radius: 8px;
  overflow: hidden;
}
.empty-map {
  height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f6f7f9;
  border-radius: 8px;
}
</style>
