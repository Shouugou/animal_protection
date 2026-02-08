<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>我的任务</span>
    </div>
    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <el-tab-pane label="未完成" name="UNFINISHED" />
      <el-tab-pane label="已完成" name="FINISHED" />
    </el-tabs>
    <el-table :data="filteredList" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="任务标题" />
      <el-table-column label="类型" width="140">
        <template slot-scope="scope">
          {{ taskTypeText(scope.row.task_type) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template slot-scope="scope">
          {{ statusText(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column prop="address" label="地址" />
      <el-table-column label="认领时间" width="200">
        <template slot-scope="scope">
          <span style="white-space:nowrap">{{ formatTime(scope.row.claimed_at) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.task_type === 'PATROL' && scope.row.status !== 'FINISHED'"
            size="mini"
            type="primary"
            @click="goReport(scope.row)"
          >巡护上报</el-button>
          <el-button
            v-else-if="scope.row.task_type === 'RESCUE_SUPPORT' && scope.row.status !== 'FINISHED'"
            size="mini"
            type="primary"
            @click="goRescueSupport(scope.row)"
          >协助上报</el-button>
          <el-button v-else size="mini" @click="openDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="任务详情" :visible.sync="detailDialog.visible" width="860px">
      <el-descriptions :column="2" v-if="detail.task_id">
        <el-descriptions-item label="任务标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ taskTypeText(detail.task_type) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.claim_status) }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address || "-" }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.start_at || "-" }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.end_at || "-" }}</el-descriptions-item>
      </el-descriptions>
      <MapViewer :latitude="detail.latitude" :longitude="detail.longitude" :height="200" />

      <el-card style="margin-top:12px" v-if="detail.report">
        <div slot="header">巡护上报</div>
        <el-descriptions :column="2">
          <el-descriptions-item label="总结">{{ detail.report.summary || "-" }}</el-descriptions-item>
          <el-descriptions-item label="里程(km)">{{ detail.report.distance_km || "-" }}</el-descriptions-item>
          <el-descriptions-item label="时长(s)">{{ detail.report.duration_sec || "-" }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ formatTime(detail.report.submitted_at) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card style="margin-top:12px" v-if="detail.rescue_report">
        <div slot="header">救助协助上报</div>
        <el-descriptions :column="2">
          <el-descriptions-item label="详情">{{ detail.rescue_report.description || "-" }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ detail.rescue_report.address || "-" }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ formatTime(detail.rescue_report.submitted_at) }}</el-descriptions-item>
        </el-descriptions>
        <MapViewer :latitude="detail.rescue_report.latitude" :longitude="detail.rescue_report.longitude" :height="200" />
        <div v-if="detail.rescue_report.attachments && detail.rescue_report.attachments.length > 0" style="margin-top:10px;display:flex;flex-wrap:wrap;gap:10px">
          <el-image
            v-for="(url, idx) in imageUrls(detail.rescue_report.attachments)"
            :key="`rs-img-${idx}`"
            :src="url"
            style="width:120px;height:90px"
            fit="cover"
          />
          <video
            v-for="(url, idx) in videoUrls(detail.rescue_report.attachments)"
            :key="`rs-vid-${idx}`"
            :src="url"
            style="width:160px;height:110px"
            controls
          />
        </div>
      </el-card>

      <el-card style="margin-top:12px" v-if="detail.report && detail.report.track_points">
        <div slot="header">轨迹点</div>
        <div ref="trackMap" class="map-container" style="height:220px"></div>
        <el-table :data="detail.report.track_points" style="width:100%;margin-top:12px">
          <el-table-column prop="seq_no" label="序号" width="80" />
          <el-table-column prop="address" label="地址" />
          <el-table-column prop="point_time" label="时间" width="200" />
        </el-table>
      </el-card>

      <el-card style="margin-top:12px" v-if="detail.report && detail.report.risk_points">
        <div slot="header">风险点</div>
        <el-table :data="detail.report.risk_points" style="width:100%">
          <el-table-column prop="risk_type" label="类型" width="140" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="address" label="地址" />
          <el-table-column prop="found_at" label="时间" width="180" />
        </el-table>
      </el-card>
    </el-dialog>
  </el-card>
</template>

<script>
import { listMyTaskClaims, getMyTaskClaim } from "@/api";
import MapViewer from "@/components/MapViewer.vue";

export default {
  name: "PublicMyTasks",
  components: { MapViewer },
  data() {
    return {
      list: [],
      loading: false,
      activeTab: "UNFINISHED",
      detailDialog: { visible: false },
      detail: {},
      trackMap: null,
      trackMarkers: []
    };
  },
  computed: {
    filteredList() {
      if (this.activeTab === "FINISHED") {
        return this.list.filter((item) => item.status === "FINISHED");
      }
      return this.list.filter((item) => item.status !== "FINISHED");
    }
  },
  created() {
    this.fetch();
  },
  methods: {
    imageUrls(list) {
      return (list || []).filter((u) => !this.isVideo(u));
    },
    videoUrls(list) {
      return (list || []).filter((u) => this.isVideo(u));
    },
    isVideo(url) {
      return /\.(mp4|webm|ogg|mov)$/i.test(url || "");
    },
    taskTypeText(type) {
      return {
        PATROL: "巡护任务",
        RESCUE_SUPPORT: "救助协助"
      }[type] || type || "未知";
    },
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
        const resp = await listMyTaskClaims();
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    goReport(row) {
      this.$router.push(`/public/patrol/submit/${row.id}`);
    },
    async openDetail(row) {
      const resp = await getMyTaskClaim(row.id);
      if (resp.code === 0) {
        this.detail = resp.data || {};
        await this.fillTrackAddresses();
        this.$nextTick(() => {
          this.renderTrackMap();
        });
        this.detailDialog.visible = true;
      }
    },
    goRescueSupport(row) {
      this.$router.push(`/public/rescue-support/submit/${row.id}`);
    },
    async fillTrackAddresses() {
      if (!window.BMap || !this.detail.report || !this.detail.report.track_points) return;
      const points = this.detail.report.track_points;
      if (points.length === 0) return;
      const geocoder = new window.BMap.Geocoder();
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
      this.detail.report.track_points = [...points];
    },
    renderTrackMap() {
      if (!this.detail.report || !this.detail.report.track_points) return;
      if (!window.BMap || !this.$refs.trackMap) return;
      if (!this.trackMap) {
        this.trackMap = new window.BMap.Map(this.$refs.trackMap);
        this.trackMap.enableScrollWheelZoom(true);
      }
      this.trackMarkers.forEach((m) => this.trackMap.removeOverlay(m));
      this.trackMarkers = [];
      const pts = [];
      this.detail.report.track_points.forEach((p, idx) => {
        const point = new window.BMap.Point(Number(p.longitude), Number(p.latitude));
        pts.push(point);
        const marker = new window.BMap.Marker(point);
        const label = new window.BMap.Label(String(idx + 1), { offset: new window.BMap.Size(6, -6) });
        marker.setLabel(label);
        this.trackMap.addOverlay(marker);
        this.trackMarkers.push(marker);
      });
      if (pts.length > 0) {
        this.trackMap.setViewport(pts);
      }
    }
  }
};
</script>
    onTabChange() {
      // client-side filtering
    },
