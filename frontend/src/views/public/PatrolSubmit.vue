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
        <el-button size="mini" @click="addTrack">添加轨迹点</el-button>
      </div>
      <el-tag v-for="(p, idx) in track" :key="idx" size="mini" style="margin-right:6px">
        {{ p.latitude }},{{ p.longitude }}
      </el-tag>
      <el-empty v-if="track.length === 0" description="暂无轨迹点" />
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

    <el-button type="primary" :loading="loading" @click="submit">提交巡护</el-button>
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
      risk: { type: "聚集点", address: "", latitude: "", longitude: "" },
      riskPoints: [],
      loading: false
    };
  },
  methods: {
    onRiskPick(payload) {
      this.risk = { ...this.risk, address: payload.address, latitude: payload.latitude, longitude: payload.longitude };
    },
    onFiles(urls) {
      this.files = urls;
    },
    addTrack() {
      this.track.push({
        seqNo: this.track.length + 1,
        latitude: Number((39.9 + Math.random()).toFixed(6)),
        longitude: Number((116.3 + Math.random()).toFixed(6)),
        pointTime: new Date().toISOString()
      });
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
          this.$router.push("/public/tasks");
        }
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
