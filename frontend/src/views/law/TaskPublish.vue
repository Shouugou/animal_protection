<template>
  <div>
    <el-card>
      <div slot="header" class="section-title">
        <span>志愿任务（巡护）</span>
        <el-button size="mini" type="primary" @click="showForm=true">发布任务</el-button>
      </div>
      <el-tabs v-model="activeTab" @tab-click="onTabChange">
        <el-tab-pane label="任务发布" name="tasks" />
        <el-tab-pane label="巡护上报" name="reports" />
      </el-tabs>

      <el-table v-if="activeTab === 'tasks'" :data="list" v-loading="loading" style="width:100%">
        <el-table-column prop="title" label="任务标题" />
        <el-table-column label="状态" width="120">
          <template slot-scope="scope">
            {{ statusText(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="认领情况" width="140">
          <template slot-scope="scope">
            {{ claimCount(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="发布时间" width="200" />
        <el-table-column label="操作" width="160">
          <template slot-scope="scope">
            <el-button size="mini" @click="openDetail(scope.row)">详情</el-button>
            <el-button size="mini" type="danger" @click="removeTask(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-else>
        <el-tabs v-model="reportTab" @tab-click="onReportTabChange">
          <el-tab-pane label="全部" name="ALL" />
          <el-tab-pane label="未完成" name="UNFINISHED" />
          <el-tab-pane label="已完成" name="FINISHED" />
        </el-tabs>
        <el-table :data="reportList" v-loading="loading" style="width:100%">
          <el-table-column prop="title" label="任务标题" />
          <el-table-column label="志愿者" width="160">
            <template slot-scope="scope">
              {{ scope.row.volunteer_name || "-" }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template slot-scope="scope">
              {{ reportStatusText(scope.row.claim_status) }}
            </template>
          </el-table-column>
          <el-table-column label="上报时间" width="200">
            <template slot-scope="scope">
              <span style="white-space:nowrap">{{ formatTime(scope.row.submitted_at) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button size="mini" :disabled="!scope.row.report_id" @click="openReportDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog title="发布巡护志愿任务" :visible.sync="showForm" width="600px">
      <el-form label-width="100px">
        <el-form-item label="任务标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input type="textarea" v-model="form.description" rows="3" />
        </el-form-item>
        <MapPicker @pick="onPick" />
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.start_at" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.end_at" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="认领人数">
          <el-input v-model="form.max_claims" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showForm=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">发布</el-button>
      </span>
    </el-dialog>

    <el-dialog title="任务详情" :visible.sync="detailDialog.visible" width="520px">
      <el-descriptions :column="2" v-if="detail.id">
        <el-descriptions-item label="任务标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.task_type }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="认领人数">{{ detail.max_claims }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address || "-" }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.start_at || "-" }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.end_at || "-" }}</el-descriptions-item>
      </el-descriptions>
      <MapViewer :latitude="detail.latitude" :longitude="detail.longitude" :height="200" />
      <div style="margin-top:10px" v-if="detail.description">
        {{ detail.description }}
      </div>
    </el-dialog>

    <el-dialog title="巡护上报详情" :visible.sync="reportDialog.visible" width="860px">
      <el-descriptions :column="2" v-if="reportDetail.id">
        <el-descriptions-item label="任务标题">{{ reportDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="志愿者">{{ reportDetail.volunteer_name || "-" }}</el-descriptions-item>
        <el-descriptions-item label="距离(km)">{{ reportDetail.distance_km || "-" }}</el-descriptions-item>
        <el-descriptions-item label="时长(s)">{{ reportDetail.duration_sec || "-" }}</el-descriptions-item>
        <el-descriptions-item label="上报时间">{{ formatTime(reportDetail.submitted_at) }}</el-descriptions-item>
      </el-descriptions>

      <el-card style="margin-top:12px">
        <div slot="header">巡护总结</div>
        <div>{{ reportDetail.summary || "-" }}</div>
      </el-card>

      <el-card style="margin-top:12px">
        <div slot="header">轨迹点</div>
        <el-table :data="reportDetail.track_points || []" style="width:100%">
          <el-table-column prop="seq_no" label="序号" width="80" />
          <el-table-column prop="address" label="地址" />
          <el-table-column prop="point_time" label="时间" />
        </el-table>
      </el-card>

      <el-card style="margin-top:12px">
        <div slot="header">风险点</div>
        <el-table :data="reportDetail.risk_points || []" style="width:100%">
          <el-table-column prop="risk_type" label="类型" width="140" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="address" label="地址" />
          <el-table-column prop="found_at" label="时间" width="180" />
        </el-table>
      </el-card>
    </el-dialog>
  </div>
</template>

<script>
import { createLawVolunteerTask, listLawVolunteerTasks, getLawVolunteerTask, deleteLawVolunteerTask, listLawPatrolReports, getLawPatrolReport } from "@/api";
import MapPicker from "@/components/MapPicker.vue";
import MapViewer from "@/components/MapViewer.vue";

export default {
  name: "LawVolunteerTasks",
  components: { MapPicker, MapViewer },
  data() {
    return {
      loading: false,
      saving: false,
      showForm: false,
      activeTab: "tasks",
      reportTab: "ALL",
      list: [],
      reportList: [],
      detailDialog: { visible: false },
      detail: {},
      reportDialog: { visible: false },
      reportDetail: {},
      form: {
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
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      return {
        OPEN: "可认领",
        CLOSED: "已满员",
        CANCELLED: "已取消"
      }[status] || status || "未知";
    },
    onPick(payload) {
      this.form.address = payload.address || "";
      this.form.latitude = payload.latitude || "";
      this.form.longitude = payload.longitude || "";
    },
    reportStatusText(status) {
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
    claimCount(row) {
      const max = Number(row.max_claims || 0);
      const claimed = Number(row.claimed_count || 0);
      if (max === 0) {
        return `${claimed}/不限`;
      }
      return `${claimed}/${max}`;
    },
    async fetch() {
      this.loading = true;
      try {
        if (this.activeTab === "tasks") {
          const resp = await listLawVolunteerTasks();
          if (resp.code === 0) {
            this.list = resp.data || [];
          }
        } else {
          const status = this.reportTab === "ALL" ? "" : this.reportTab;
          const resp = await listLawPatrolReports({ status });
          if (resp.code === 0) {
            this.reportList = resp.data || [];
          }
        }
      } finally {
        this.loading = false;
      }
    },
    onTabChange() {
      this.fetch();
    },
    onReportTabChange() {
      this.fetch();
    },
    async submit() {
      if (!this.form.title) {
        this.$message.warning("请填写任务标题");
        return;
      }
      this.saving = true;
      try {
        const resp = await createLawVolunteerTask({
          title: this.form.title,
          description: this.form.description,
          address: this.form.address,
          latitude: this.form.latitude ? Number(this.form.latitude) : null,
          longitude: this.form.longitude ? Number(this.form.longitude) : null,
          startAt: this.form.start_at,
          endAt: this.form.end_at,
          maxClaims: this.form.max_claims ? Number(this.form.max_claims) : 1
        });
        if (resp.code === 0) {
          this.$message.success("已发布巡护任务");
          this.showForm = false;
          this.form = {
            title: "",
            description: "",
            address: "",
            latitude: "",
            longitude: "",
            start_at: "",
            end_at: "",
            max_claims: ""
          };
          this.fetch();
        } else {
          this.$message.error(resp.message || "发布失败");
        }
      } finally {
        this.saving = false;
      }
    },
    async openDetail(row) {
      const resp = await getLawVolunteerTask(row.id);
      if (resp.code === 0) {
        this.detail = resp.data || {};
        this.detailDialog.visible = true;
      }
    },
    async openReportDetail(row) {
      const resp = await getLawPatrolReport(row.report_id);
      if (resp.code === 0) {
        this.reportDetail = resp.data || {};
        await this.fillTrackAddresses();
        this.reportDialog.visible = true;
      }
    },
    async fillTrackAddresses() {
      if (!window.BMap || !this.reportDetail.track_points || this.reportDetail.track_points.length === 0) {
        return;
      }
      const geocoder = new window.BMap.Geocoder();
      const points = this.reportDetail.track_points;
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
      this.reportDetail.track_points = [...points];
    },
    removeTask(row) {
      this.$confirm("确定删除该任务吗？", "提示", { type: "warning" }).then(async () => {
        const resp = await deleteLawVolunteerTask(row.id);
        if (resp.code === 0) {
          this.$message.success("已删除");
          this.fetch();
        } else {
          this.$message.error(resp.message || "删除失败");
        }
      }).catch(() => {});
    }
  }
};
</script>
