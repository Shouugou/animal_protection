<template>
  <div>
    <el-card>
      <div slot="header">归档案件</div>
      <el-table :data="list" style="width:100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="event_id" label="事件ID" width="100" />
        <el-table-column prop="event_type" label="事件类型" />
        <el-table-column prop="address" label="地址" />
        <el-table-column label="状态" width="160">
          <template slot-scope="scope">
            {{ statusText(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="上报时间" width="240">
          <template slot-scope="scope">
            <span style="white-space:nowrap">{{ formatTime(scope.row.reported_at) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="执法人员" width="160">
          <template slot-scope="scope">
            {{ scope.row.assignee_name || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template slot-scope="scope">
            <el-link type="primary" @click="openDetail(scope.row)">查看详情</el-link>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="案件详情" :visible.sync="detailDialog.visible" width="860px">
      <el-card v-if="detail.event_type" style="margin-bottom:12px">
        <div slot="header">关联事件</div>
        <el-descriptions :column="2">
          <el-descriptions-item label="事件类型">{{ detail.event_type }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">{{ detail.urgency || "-" }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ detail.address || "未提供" }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ formatTime(detail.reported_at) }}</el-descriptions-item>
          <el-descriptions-item label="工单状态">{{ statusText(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="执法人员">{{ detail.assignee_name || "-" }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:8px;line-height:1.6;color:#374151" v-if="detail.description">
          {{ detail.description }}
        </div>
      </el-card>

      <MapViewer :latitude="detail.latitude" :longitude="detail.longitude" :height="220" />

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

      <TimelineList :items="timeline" />
    </el-dialog>
  </div>
</template>

<script>
import { listArchivedWorkOrders, getWorkOrder, getEventTimeline } from "@/api";
import MapViewer from "@/components/MapViewer.vue";
import TimelineList from "@/components/TimelineList.vue";

export default {
  name: "LawArchivedCases",
  components: { MapViewer, TimelineList },
  data() {
    return {
      list: [],
      loading: false,
      detailDialog: {
        visible: false
      },
      detail: {},
      timeline: []
    };
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
  created() {
    this.fetch();
  },
  methods: {
    isVideo(url) {
      return /\.(mp4|webm|ogg|mov)$/i.test(url || "");
    },
    statusText(status) {
      const map = {
        ARCHIVED: "已归档",
        TRANSFERRED: "已推送救助医疗机构"
      };
      return map[status] || status || "未知";
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
        const resp = await listArchivedWorkOrders();
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    async openDetail(row) {
      const resp = await getWorkOrder(row.id);
      if (resp.code === 0) {
        this.detail = resp.data || {};
        const eventId = this.detail.event_id;
        if (eventId) {
          const tl = await getEventTimeline(eventId);
          if (tl.code === 0) {
            this.timeline = (tl.data || []).map((item) => ({
              ...item,
              created_at: this.formatTime(item.created_at)
            }));
          }
        }
        this.detailDialog.visible = true;
      }
    }
  }
};
</script>
