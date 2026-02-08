<template>
  <div>
    <el-card>
      <div slot="header">巡护上报（认领ID: {{ claimId }}）</div>
      <el-form label-width="90px">
        <el-form-item label="巡护总结">
          <el-input type="textarea" v-model="summary" rows="3" />
        </el-form-item>
        <el-form-item label="里程(公里)">
          <el-input v-model="distance" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input v-model="duration" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div slot="header" class="section-title">
        <span>轨迹采集</span>
        <el-button size="mini" @click="addTrack">定位并添加轨迹点</el-button>
      </div>
      <div ref="trackMap" class="map-container" style="height:240px"></div>
      <el-table v-if="track.length > 0" :data="track" style="width:100%;margin-top:12px">
        <el-table-column prop="seqNo" label="序号" width="80" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="pointTime" label="时间" width="200" />
      </el-table>
      <el-empty v-else description="暂无轨迹点" />
    </el-card>

    <el-card>
      <div slot="header" class="section-title">
        <span>风险点标注</span>
      </div>
      <el-form inline>
        <el-form-item label="类型">
          <el-select v-model="risk.type">
            <el-option label="聚集点" value="聚集点" />
            <el-option label="投喂点" value="投喂点" />
            <el-option label="伤病点" value="伤病点" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="mini" @click="addRisk">添加风险点</el-button>
        </el-form-item>
      </el-form>
      <MapPicker @pick="onRiskPick" />
      <Uploader title="风险点附件" @change="onFiles" />
      <el-table :data="riskPoints" style="width:100%">
        <el-table-column prop="riskType" label="类型" width="120" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="latitude" label="纬度" width="120" />
        <el-table-column prop="longitude" label="经度" width="120" />
      </el-table>
    </el-card>

    <div style="margin-top:12px">
      <el-button @click="manualSaveDraft">暂存</el-button>
      <el-button type="primary" :loading="loading" @click="submit">提交巡护</el-button>
    </div>
  </div>
</template>

<script>
import MapPicker from "@/components/MapPicker.vue";
import Uploader from "@/components/Uploader.vue";
import { submitPatrolReport } from "@/api";

export default {
  name: "PatrolSubmit",
  components: { MapPicker, Uploader },
  data() {
    return {
      claimId: Number(this.$route.params.claimId),
      summary: "",
      distance: "",
      duration: "",
      files: [],
      track: [],
      trackMap: null,
      trackMarkers: [],
      geocoder: null,
      risk: { type: "聚集点", address: "", latitude: "", longitude: "" },
      riskPoints: [],
      loading: false,
      draftTimer: null
    };
  },
  mounted() {
    this.initTrackMap();
    this.loadDraft();
  },
  watch: {
    summary() {
      this.queueDraftSave();
    },
    distance() {
      this.queueDraftSave();
    },
    duration() {
      this.queueDraftSave();
    },
    track: {
      handler() {
        this.queueDraftSave();
      },
      deep: true
    },
    riskPoints: {
      handler() {
        this.queueDraftSave();
      },
      deep: true
    }
  },
  beforeDestroy() {
    this.saveDraft();
    if (this.draftTimer) {
      clearTimeout(this.draftTimer);
    }
  },
  methods: {
    initTrackMap() {
      if (!window.BMap || !this.$refs.trackMap) return;
      this.trackMap = new window.BMap.Map(this.$refs.trackMap);
      const point = new window.BMap.Point(116.404, 39.915);
      this.trackMap.centerAndZoom(point, 12);
      this.trackMap.enableScrollWheelZoom(true);
      this.geocoder = new window.BMap.Geocoder();
    },
    renderTrackMap() {
      if (!this.trackMap) return;
      this.trackMarkers.forEach((m) => this.trackMap.removeOverlay(m));
      this.trackMarkers = [];
      if (this.track.length === 0) return;
      const points = [];
      this.track.forEach((p, idx) => {
        const point = new window.BMap.Point(Number(p.longitude), Number(p.latitude));
        points.push(point);
        const marker = new window.BMap.Marker(point);
        const label = new window.BMap.Label(String(idx + 1), { offset: new window.BMap.Size(6, -6) });
        marker.setLabel(label);
        this.trackMap.addOverlay(marker);
        this.trackMarkers.push(marker);
      });
      this.trackMap.setViewport(points);
    },
    onRiskPick(payload) {
      this.risk = { ...this.risk, address: payload.address, latitude: payload.latitude, longitude: payload.longitude };
    },
    onFiles(urls) {
      this.files = urls;
    },
    addTrack() {
      if (!window.BMap) {
        this.$message.warning("地图未加载，无法定位");
        return;
      }
      const geolocation = new window.BMap.Geolocation();
      geolocation.getCurrentPosition(
        (r) => {
          if (geolocation.getStatus() === window.BMAP_STATUS_SUCCESS) {
            const point = r.point;
            const seqNo = this.track.length + 1;
            const pointTime = new Date().toISOString();
            const addPoint = (addr) => {
              this.track.push({
                seqNo,
                latitude: Number(point.lat.toFixed(6)),
                longitude: Number(point.lng.toFixed(6)),
                address: addr || "未提供",
                pointTime
              });
              this.renderTrackMap();
            };
            if (this.geocoder) {
              this.geocoder.getLocation(point, (rs) => {
                addPoint(rs && rs.address ? rs.address : "未提供");
              });
            } else {
              addPoint("未提供");
            }
          } else {
            this.$message.warning("定位失败，请重试");
          }
        },
        { enableHighAccuracy: true }
      );
    },
    addRisk() {
      if (!this.risk.latitude || !this.risk.longitude) {
        this.$message.warning("请先在地图上选择风险点位置");
        return;
      }
      this.riskPoints.push({
        riskType: this.risk.type,
        address: this.risk.address,
        latitude: Number(this.risk.latitude),
        longitude: Number(this.risk.longitude),
        foundAt: new Date().toISOString()
      });
      this.risk = { type: this.risk.type, address: "", latitude: "", longitude: "" };
    },
    saveDraft() {
      const payload = {
        summary: this.summary,
        distance: this.distance,
        duration: this.duration,
        track: this.track,
        riskPoints: this.riskPoints
      };
      localStorage.setItem(`ap_patrol_draft_${this.claimId}`, JSON.stringify(payload));
    },
    manualSaveDraft() {
      this.saveDraft();
      this.$message.success("已暂存");
    },
    queueDraftSave() {
      if (this.draftTimer) {
        clearTimeout(this.draftTimer);
      }
      this.draftTimer = setTimeout(() => {
        this.saveDraft();
      }, 400);
    },
    loadDraft() {
      const raw = localStorage.getItem(`ap_patrol_draft_${this.claimId}`);
      if (!raw) return;
      try {
        const payload = JSON.parse(raw);
        this.summary = payload.summary || "";
        this.distance = payload.distance || "";
        this.duration = payload.duration || "";
        this.track = payload.track || [];
        this.riskPoints = payload.riskPoints || [];
        this.renderTrackMap();
      } catch (e) {
        localStorage.removeItem(`ap_patrol_draft_${this.claimId}`);
      }
    },
    async submit() {
      this.loading = true;
      try {
        const resp = await submitPatrolReport({
          claimId: this.claimId,
          summary: this.summary,
          distanceKm: this.distance ? Number(this.distance) : null,
          durationSec: this.duration ? Number(this.duration) * 60 : null,
          trackPoints: this.track,
          riskPoints: this.riskPoints
        });
        if (resp.code === 0) {
          this.$message.success("巡护已提交");
          localStorage.removeItem(`ap_patrol_draft_${this.claimId}`);
          this.$router.push("/public/tasks");
        }
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
