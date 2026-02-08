<template>
  <el-card>
    <div slot="header">救助任务</div>
    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="待接收" name="NEW" />
      <el-tab-pane label="待评估" name="GRABBED" />
      <el-tab-pane label="待调度" name="DISPATCHING" />
      <el-tab-pane label="已到达" name="ARRIVED" />
      <el-tab-pane label="已入站" name="INTAKE" />
      <el-tab-pane label="不救助" name="REJECTED" />
    </el-tabs>
    <el-table :data="list" style="width:100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="event_id" label="事件ID" width="100" />
      <el-table-column prop="event_type" label="事件类型" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="状态" width="140">
        <template slot-scope="scope">
          {{ statusText(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="上报时间" width="220">
        <template slot-scope="scope">
          <span style="white-space:nowrap">{{ formatTime(scope.row.reported_at) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.status === 'NEW'"
            size="mini"
            type="primary"
            @click="grab(scope.row)"
          >接收</el-button>
          <el-button
            v-if="scope.row.status === 'GRABBED'"
            size="mini"
            @click="evaluate(scope.row)"
          >评估</el-button>
          <el-button
            v-if="scope.row.status === 'DISPATCHING'"
            size="mini"
            type="primary"
            @click="dispatch(scope.row)"
          >调度</el-button>
          <el-button
            v-if="scope.row.status === 'DISPATCHING'"
            size="mini"
            @click="openVolunteer(scope.row)"
          >发布志愿任务</el-button>
          <el-link type="primary" style="margin-left:8px" @click="openDetail(scope.row)">详情</el-link>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="评估是否救助" :visible.sync="showEval" width="420px">
      <el-form label-width="80px">
        <el-form-item label="是否救助">
          <el-select v-model="evalForm.need_rescue">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="结论">
          <el-input type="textarea" v-model="evalForm.note" rows="3" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showEval=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEval">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="救助调度" :visible.sync="showDispatch" width="520px">
      <el-form label-width="90px">
        <el-form-item label="调度说明">
          <el-input type="textarea" v-model="dispatchForm.note" rows="3" />
        </el-form-item>
        <el-form-item label="出发时间">
          <el-date-picker v-model="dispatchForm.start" type="datetime" />
        </el-form-item>
        <el-form-item label="到达时间">
          <el-date-picker v-model="dispatchForm.arrive" type="datetime" />
        </el-form-item>
        <el-form-item label="入站时间">
          <el-date-picker v-model="dispatchForm.intake" type="datetime" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showDispatch=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDispatch">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="发布救助协助志愿任务" :visible.sync="showVolunteer" width="560px">
      <el-form label-width="100px">
        <el-form-item label="任务标题">
          <el-input v-model="volunteerForm.title" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input type="textarea" v-model="volunteerForm.description" rows="3" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="volunteerForm.address" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input v-model="volunteerForm.latitude" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input v-model="volunteerForm.longitude" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="volunteerForm.start_at" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="volunteerForm.end_at" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="认领人数">
          <el-input v-model="volunteerForm.max_claims" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showVolunteer=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitVolunteer">发布</el-button>
      </span>
    </el-dialog>

    <el-dialog title="事件详情" :visible.sync="detailDialog.visible" width="860px">
      <el-card v-if="detail.id" style="margin-bottom:12px">
        <div slot="header">事件信息</div>
        <el-descriptions :column="2">
          <el-descriptions-item label="类型">{{ detail.event_type }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">{{ detail.urgency || "-" }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ detail.address || "未提供" }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ formatTime(detail.reported_at) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:8px;line-height:1.6;color:#374151" v-if="detail.description">
          {{ detail.description }}
        </div>
      </el-card>

      <MapViewer :latitude="detail.latitude" :longitude="detail.longitude" :height="220" />

      <el-card v-if="detail.attachments && detail.attachments.length > 0">
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
    </el-dialog>
  </el-card>
</template>

<script>
import { listRescueTasks, grabRescueTask, evaluateRescueTask, dispatchRescueTask, getEvent, createRescueVolunteerTask } from "@/api";
import MapViewer from "@/components/MapViewer.vue";

export default {
  name: "RescueTasks",
  components: { MapViewer },
  data() {
    return {
      list: [],
      loading: false,
      saving: false,
      activeTab: "ALL",
      showEval: false,
      showDispatch: false,
      showVolunteer: false,
      currentId: null,
      detailDialog: { visible: false },
      detail: {},
      evalForm: { need_rescue: true, note: "" },
      dispatchForm: { note: "", start: "", arrive: "", intake: "" },
      volunteerForm: {
        title: "",
        description: "",
        address: "",
        latitude: "",
        longitude: "",
        start_at: "",
        end_at: "",
        max_claims: ""
      }
    };
  },
  computed: {
    imageUrls() {
      return (this.detail.attachments || []).filter((u) => !this.isVideo(u));
    },
    videoUrls() {
      return (this.detail.attachments || []).filter((u) => this.isVideo(u));
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
        NEW: "待接收",
        GRABBED: "待评估",
        DISPATCHING: "待调度",
        ARRIVED: "已到达",
        INTAKE: "已入站",
        TREATING: "治疗中",
        CLOSED: "已闭环",
        REJECTED: "不救助"
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
        const status = this.activeTab === "ALL" ? "" : this.activeTab;
        const resp = await listRescueTasks({ status });
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
    async grab(row) {
      const resp = await grabRescueTask(row.id);
      if (resp.code === 0) {
        this.$message.success("已接收任务");
        this.fetch();
      }
    },
    evaluate(row) {
      this.currentId = row.id;
      this.evalForm = { need_rescue: true, note: "" };
      this.showEval = true;
    },
    dispatch(row) {
      this.currentId = row.id;
      this.dispatchForm = { note: "", start: "", arrive: "", intake: "" };
      this.showDispatch = true;
    },
    openVolunteer(row) {
      const title = `救助协助-${row.event_type || "事件"}-${row.event_id}`;
      this.volunteerForm = {
        title,
        description: "协助救助现场处理与物资搬运等",
        address: row.address || "",
        latitude: row.latitude || "",
        longitude: row.longitude || "",
        start_at: "",
        end_at: "",
        max_claims: 1
      };
      this.showVolunteer = true;
    },
    async saveEval() {
      this.saving = true;
      try {
        const resp = await evaluateRescueTask(this.currentId, {
          needRescue: this.evalForm.need_rescue,
          note: this.evalForm.note
        });
        if (resp.code === 0) {
          this.$message.success("评估已保存");
          this.showEval = false;
          this.fetch();
        }
      } finally {
        this.saving = false;
      }
    },
    async saveDispatch() {
      this.saving = true;
      try {
        const resp = await dispatchRescueTask(this.currentId, {
          note: this.dispatchForm.note,
          start: this.dispatchForm.start,
          arrive: this.dispatchForm.arrive,
          intake: this.dispatchForm.intake
        });
        if (resp.code === 0) {
          this.$message.success("调度已保存");
          this.showDispatch = false;
          this.fetch();
        }
      } finally {
        this.saving = false;
      }
    },
    async submitVolunteer() {
      if (!this.volunteerForm.title) {
        this.$message.warning("请填写任务标题");
        return;
      }
      this.saving = true;
      try {
        const resp = await createRescueVolunteerTask({
          title: this.volunteerForm.title,
          description: this.volunteerForm.description,
          address: this.volunteerForm.address,
          latitude: this.volunteerForm.latitude ? Number(this.volunteerForm.latitude) : null,
          longitude: this.volunteerForm.longitude ? Number(this.volunteerForm.longitude) : null,
          startAt: this.volunteerForm.start_at,
          endAt: this.volunteerForm.end_at,
          maxClaims: this.volunteerForm.max_claims ? Number(this.volunteerForm.max_claims) : 1
        });
        if (resp.code === 0) {
          this.$message.success("已发布志愿任务");
          this.showVolunteer = false;
        } else {
          this.$message.error(resp.message || "发布失败");
        }
      } finally {
        this.saving = false;
      }
    },
    async openDetail(row) {
      const resp = await getEvent(row.event_id);
      if (resp.code === 0) {
        this.detail = resp.data || {};
        this.detailDialog.visible = true;
      }
    }
  }
};
</script>
