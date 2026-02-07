<template>
  <div>
    <el-card>
      <div slot="header" class="section-title">
        <div style="display:flex;align-items:center;gap:10px">
          <span>事件详情 #{{ id }}</span>
          <el-tag size="mini" :type="statusType(detail.status)">
            {{ statusText(detail.status) }}
          </el-tag>
        </div>
        <div style="display:flex;gap:8px">
          <el-button size="mini" @click="showSupplement = true">补充线索</el-button>
          <el-button v-if="canDelete" size="mini" type="danger" @click="removeEvent">删除事件</el-button>
        </div>
      </div>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-descriptions :column="1">
            <el-descriptions-item label="类型">{{ detail.event_type || "-" }}</el-descriptions-item>
            <el-descriptions-item label="紧急程度">{{ detail.urgency || "-" }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="12">
          <el-descriptions :column="1">
            <el-descriptions-item label="地址">{{ resolvedAddress || detail.address || "未提供" }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ formatTime(detail.reported_at) }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>
    </el-card>

    <el-card v-if="detail.description">
      <div slot="header">事件描述</div>
      <div style="line-height:1.6;color:#374151">{{ detail.description }}</div>
    </el-card>

    <MapViewer :latitude="detail.latitude" :longitude="detail.longitude" />

    <el-card v-if="detail.attachments && detail.attachments.length > 0">
      <div slot="header">现场附件</div>
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
      <div slot="header">执法取证附件</div>
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

    <el-card>
      <div slot="header">处理反馈</div>
      <el-empty v-if="feedbacks.length === 0" description="暂无反馈" />
      <el-table v-else :data="feedbacks" style="width:100%">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="时间" width="200">
          <template slot-scope="scope">
            {{ formatTime(scope.row.created_at || scope.row.time) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <div slot="header">评论</div>
      <el-input type="textarea" v-model="comment" rows="3" placeholder="留言评论" />
      <el-button type="primary" size="mini" style="margin-top:8px" @click="postComment">
        发布
      </el-button>
      <el-divider />
      <el-empty v-if="comments.length === 0" description="暂无评论" />
      <div v-else>
        <el-card v-for="(c, idx) in comments" :key="idx" style="margin-bottom:10px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
            <div style="font-weight:600;color:#111827">{{ c.author_name || "匿名" }}</div>
            <div style="font-size:12px;color:#6b7280">{{ formatTime(c.created_at) }}</div>
          </div>
          <div style="line-height:1.6;color:#374151">{{ c.content || c }}</div>
        </el-card>
      </div>
    </el-card>

    <el-dialog title="补充线索" :visible.sync="showSupplement" width="520px">
      <el-input type="textarea" v-model="supplement" rows="4" placeholder="补充说明" />
      <Uploader title="补充附件" @change="onFiles" />
      <span slot="footer">
        <el-button @click="showSupplement = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSupplement">提交</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import TimelineList from "@/components/TimelineList.vue";
import Uploader from "@/components/Uploader.vue";
import MapViewer from "@/components/MapViewer.vue";
import {
  getEvent,
  getEventTimeline,
  listEventComments,
  addEventComment,
  addEventSupplement,
  fetchMessages,
  deleteEvent,
  uploadFile
} from "@/api";

export default {
  name: "PublicEventDetail",
  components: { TimelineList, Uploader, MapViewer },
  computed: {
    canDelete() {
      const s = this.detail.status;
      return s === "REPORTED";
    },
    imageUrls() {
      return (this.detail.attachments || []).filter((u) => !this.isVideo(u));
    },
    videoUrls() {
      return (this.detail.attachments || []).filter((u) => this.isVideo(u));
    },
    evidenceImageUrls() {
      return (this.detail.evidence_attachments || []).filter((u) => !this.isVideo(u));
    },
    evidenceVideoUrls() {
      return (this.detail.evidence_attachments || []).filter((u) => this.isVideo(u));
    }
  },
  data() {
    return {
      id: this.$route.params.id,
      detail: {},
      timeline: [],
      feedbacks: [],
      showSupplement: false,
      supplement: "",
      files: [],
      comment: "",
      comments: [],
      saving: false,
      resolvedAddress: ""
    };
  },
  created() {
    this.fetchAll();
  },
  methods: {
    statusText(status) {
      const map = {
        REPORTED: "已上报",
        TRIAGED: "已分流",
        DISPATCHED: "已派单",
        PROCESSING: "处理中",
        PENDING_SUPPLEMENT: "待补充",
        RESOLVED: "已处置",
        CLOSED: "已办结",
        REJECTED: "不予受理",
        DUPLICATE: "重复事件"
      };
      return map[status] || status || "未知";
    },
    statusType(status) {
      const map = {
        REPORTED: "info",
        TRIAGED: "warning",
        DISPATCHED: "warning",
        PROCESSING: "warning",
        PENDING_SUPPLEMENT: "warning",
        RESOLVED: "success",
        CLOSED: "success",
        REJECTED: "danger",
        DUPLICATE: "danger"
      };
      return map[status] || "info";
    },
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
    async fetchAll() {
      const [detail, timeline, comments, messages] = await Promise.all([
        getEvent(this.id),
        getEventTimeline(this.id),
        listEventComments(this.id),
        fetchMessages()
      ]);
      if (detail.code === 0) this.detail = detail.data;
      this.resolveAddress();
      if (timeline.code === 0) {
        this.timeline = (timeline.data || []).map((item) => ({
          ...item,
          created_at: this.formatTime(item.created_at)
        }));
      }
      if (comments.code === 0) this.comments = comments.data || [];
      if (messages.code === 0) this.feedbacks = messages.data || [];
    },
    resolveAddress() {
      if (this.detail.address || !window.BMap) return;
      const lat = Number(this.detail.latitude);
      const lng = Number(this.detail.longitude);
      if (Number.isNaN(lat) || Number.isNaN(lng)) return;
      const geocoder = new window.BMap.Geocoder();
      const point = new window.BMap.Point(lng, lat);
      geocoder.getLocation(point, (rs) => {
        if (rs && rs.address) {
          this.resolvedAddress = rs.address;
        }
      });
    },
    onFiles(files) {
      this.files = files;
    },
    async uploadAll() {
      const urls = [];
      for (const f of this.files) {
        const resp = await uploadFile(f);
        if (resp.code === 0 && resp.data && resp.data.file_url) {
          urls.push(resp.data.file_url);
        }
      }
      return urls;
    },
    async submitSupplement() {
      if (!this.supplement) {
        this.$message.warning("请填写补充说明");
        return;
      }
      this.saving = true;
      try {
        const attachments = await this.uploadAll();
        const resp = await addEventSupplement(this.id, {
          content: this.supplement,
          attachments
        });
        if (resp.code === 0) {
          this.$message.success("补充成功");
          this.showSupplement = false;
          this.supplement = "";
          this.files = [];
          this.fetchAll();
        }
      } finally {
        this.saving = false;
      }
    },
    async postComment() {
      if (!this.comment) return;
      const resp = await addEventComment(this.id, { content: this.comment });
      if (resp.code === 0) {
        this.comment = "";
        this.fetchAll();
      }
    },
    removeEvent() {
      this.$confirm("确定删除该事件吗？删除后不可恢复。", "提示", {
        type: "warning"
      }).then(async () => {
        const resp = await deleteEvent(this.id);
        if (resp.code === 0) {
          this.$message.success("已删除");
          this.$router.push("/public/events");
        } else {
          this.$message.error(resp.message || "删除失败");
        }
      }).catch(() => {});
    }
  }
};
</script>
