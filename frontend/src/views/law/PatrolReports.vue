<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>巡护上报</span>
    </div>
    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="未完成" name="UNFINISHED" />
      <el-tab-pane label="已完成" name="FINISHED" />
    </el-tabs>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="任务标题" />
      <el-table-column label="志愿者" width="160">
        <template slot-scope="scope">
          {{ scope.row.volunteer_name || "-" }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template slot-scope="scope">
          {{ statusText(scope.row.claim_status) }}
        </template>
      </el-table-column>
      <el-table-column label="上报时间" width="200">
        <template slot-scope="scope">
          <span style="white-space:nowrap">{{ formatTime(scope.row.submitted_at) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button size="mini" :disabled="!scope.row.report_id" @click="openDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="巡护上报详情" :visible.sync="detailDialog.visible" width="860px">
      <el-descriptions :column="2" v-if="detail.id">
        <el-descriptions-item label="任务标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="志愿者">{{ detail.volunteer_name || "-" }}</el-descriptions-item>
        <el-descriptions-item label="距离(km)">{{ detail.distance_km || "-" }}</el-descriptions-item>
        <el-descriptions-item label="时长(s)">{{ detail.duration_sec || "-" }}</el-descriptions-item>
        <el-descriptions-item label="上报时间">{{ formatTime(detail.submitted_at) }}</el-descriptions-item>
      </el-descriptions>

      <el-card style="margin-top:12px">
        <div slot="header">巡护总结</div>
        <div>{{ detail.summary || "-" }}</div>
      </el-card>

      <el-card style="margin-top:12px">
        <div slot="header">轨迹点</div>
        <el-table :data="detail.track_points || []" style="width:100%">
          <el-table-column prop="seq_no" label="序号" width="80" />
          <el-table-column prop="address" label="地址" />
          <el-table-column prop="point_time" label="时间" />
        </el-table>
      </el-card>

      <el-card style="margin-top:12px">
        <div slot="header">风险点</div>
        <el-table :data="detail.risk_points || []" style="width:100%">
          <el-table-column prop="risk_type" label="类型" width="140" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="address" label="地址" />
          <el-table-column prop="latitude" label="纬度" width="120" />
          <el-table-column prop="longitude" label="经度" width="120" />
          <el-table-column prop="found_at" label="时间" width="180" />
        </el-table>
      </el-card>
    </el-dialog>
  </el-card>
</template>

<script>
import { listLawPatrolReports, getLawPatrolReport } from "@/api";

export default {
  name: "LawPatrolReports",
  data() {
    return {
      activeTab: "ALL",
      loading: false,
      list: [],
      detailDialog: { visible: false },
      detail: {}
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      return {
        CLAIMED: "已认领",
        STARTED: "执行中",
        FINISHED: "已完成",
        CANCELLED: "已取消"
      }[status] || status || "未知";
    },
    formatTime(value) {
      if (!value) return "";
      let d = null;
      if (typeof value === "number") {
        d = new Date(value);
      } else if (typeof value === "string") {
        const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/);
        if (match) {
          d = new Date(
            Number(match[1]),
            Number(match[2]) - 1,
            Number(match[3]),
            Number(match[4]),
            Number(match[5]),
            Number(match[6])
          );
        } else {
          d = new Date(value);
        }
      } else if (value instanceof Date) {
        d = value;
      }
      if (!d || Number.isNaN(d.getTime())) return "";
      const pad = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日${pad(d.getHours())}时${pad(d.getMinutes())}分${pad(d.getSeconds())}秒`;
    },
    async fetch() {
      this.loading = true;
      try {
        const status = this.activeTab === "ALL" ? "" : this.activeTab;
        const resp = await listLawPatrolReports({ status });
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    onTabChange() {
      this.fetch();
    },
    async openDetail(row) {
      const resp = await getLawPatrolReport(row.report_id);
      if (resp.code === 0) {
        this.detail = resp.data || {};
        await this.fillTrackAddresses();
        this.detailDialog.visible = true;
      }
    },
    async fillTrackAddresses() {
      if (!window.BMap || !this.detail.track_points || this.detail.track_points.length === 0) {
        return;
      }
      const geocoder = new window.BMap.Geocoder();
      const points = this.detail.track_points;
      for (let i = 0; i < points.length; i += 1) {
        const p = points[i];
        if (p.address) continue;
        const point = new window.BMap.Point(Number(p.longitude), Number(p.latitude));
        await new Promise((resolve) => {
          geocoder.getLocation(point, (rs) => {
            p.address = rs && rs.address ? rs.address : "未提供";
            resolve();
          });
        });
      }
      this.detail.track_points = [...points];
    }
  }
};
</script>
