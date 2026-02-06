<template>
  <div>
    <el-card>
      <div slot="header">工单详情 #{{ id }}</div>
      <el-card v-if="detail.event_type" style="margin-bottom:12px">
        <div slot="header">关联事件</div>
        <el-descriptions :column="2">
          <el-descriptions-item label="事件类型">{{ detail.event_type }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">{{ detail.urgency || "-" }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ detail.address || "未提供" }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ formatTime(detail.reported_at) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:8px;line-height:1.6;color:#374151" v-if="detail.description">
          {{ detail.description }}
        </div>
      </el-card>
    </el-card>

    <MapViewer :latitude="detail.latitude" :longitude="detail.longitude" />

    <el-card v-if="detail.event_attachments && detail.event_attachments.length > 0">
      <div slot="header">事件附件</div>
      <div style="display:flex;flex-wrap:wrap;gap:12px">
        <el-image
          v-for="(url, idx) in imageUrls"
          :key="`img-${idx}`"
          :src="url"
          style="width:140px;height:100px"
          fit="cover"
        />
        <video
          v-for="(url, idx) in videoUrls"
          :key="`vid-${idx}`"
          :src="url"
          style="width:180px;height:120px"
          controls
        />
      </div>
    </el-card>

    <el-card v-if="detail.evidence_attachments && detail.evidence_attachments.length > 0">
      <div slot="header">取证附件</div>
      <div style="display:flex;flex-wrap:wrap;gap:12px">
        <el-image
          v-for="(url, idx) in evidenceImageUrls"
          :key="`eimg-${idx}`"
          :src="url"
          style="width:140px;height:100px"
          fit="cover"
        />
        <video
          v-for="(url, idx) in evidenceVideoUrls"
          :key="`evid-${idx}`"
          :src="url"
          style="width:180px;height:120px"
          controls
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import MapViewer from "@/components/MapViewer.vue";
import { getWorkOrder } from "@/api";

export default {
  name: "LawWorkOrderDetail",
  components: { MapViewer },
  data() {
    return {
      id: this.$route.params.id,
      detail: {
        need_law_enforcement: true,
        transfer_to_rescue: false,
        result_text: ""
      }
    };
  },
  created() {
    this.fetchDetail();
  },
  computed: {
    imageUrls() {
      return (this.detail.event_attachments || []).filter((u) => !this.isVideo(u));
    },
    videoUrls() {
      return (this.detail.event_attachments || []).filter((u) => this.isVideo(u));
    },
    evidenceImageUrls() {
      return (this.detail.evidence_attachments || []).filter((u) => !this.isVideo(u));
    },
    evidenceVideoUrls() {
      return (this.detail.evidence_attachments || []).filter((u) => this.isVideo(u));
    }
  },
  methods: {
    isVideo(url) {
      return /\.(mp4|webm|ogg|mov)$/i.test(url || "");
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
    async fetchDetail() {
      const resp = await getWorkOrder(this.id);
      if (resp.code === 0) {
        const data = resp.data || {};
        this.detail = {
          ...this.detail,
          ...data,
          need_law_enforcement: data.need_law_enforcement === 1 || data.need_law_enforcement === true,
          transfer_to_rescue: data.transfer_to_rescue === 1 || data.transfer_to_rescue === true
        };
      }
    }
  }
};
</script>
