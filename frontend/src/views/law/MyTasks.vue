<template>
  <div>
    <el-card>
      <div slot="header">我的任务</div>
      <el-tabs v-model="activeTab" @tab-click="onTabChange">
        <el-tab-pane label="全部" name="ALL" />
        <el-tab-pane label="待取证" name="ASSIGNED" />
        <el-tab-pane label="待录入结果" name="ON_SITE" />
      </el-tabs>
      <el-table :data="list" style="width:100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="event_id" label="事件ID" width="100" />
        <el-table-column prop="event_type" label="事件类型" />
        <el-table-column prop="address" label="地址" />
        <el-table-column label="状态" width="200">
          <template slot-scope="scope">
            <span style="white-space:nowrap">{{ statusText(scope.row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上报时间" width="240">
          <template slot-scope="scope">
            <span style="white-space:nowrap">{{ formatTime(scope.row.reported_at) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.status === 'ASSIGNED'"
              type="primary"
              size="mini"
              @click="openEvidence(scope.row)"
            >取证</el-button>
            <el-button
              v-else-if="scope.row.status === 'ON_SITE'"
              type="primary"
              size="mini"
              @click="openResult(scope.row)"
            >结果录入</el-button>
            <el-link type="primary" @click="open(scope.row)" style="margin-left:8px">查看</el-link>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="现场取证" :visible.sync="evidenceDialog.visible" width="560px">
      <el-form label-width="90px">
        <el-form-item label="说明">
          <el-input type="textarea" v-model="evidenceDialog.form.note" rows="3" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="evidenceDialog.form.address" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input v-model="evidenceDialog.form.latitude" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input v-model="evidenceDialog.form.longitude" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="mini" @click="locateEvidence">定位当前位置</el-button>
        </el-form-item>
        <MapViewer
          :latitude="evidenceDialog.form.latitude"
          :longitude="evidenceDialog.form.longitude"
          :height="200"
        />
        <Uploader title="取证附件" @change="onEvidenceFiles" />
      </el-form>
      <span slot="footer">
        <el-button @click="evidenceDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="evidenceDialog.loading" @click="submitEvidence">提交</el-button>
      </span>
    </el-dialog>

    <el-dialog title="结果录入" :visible.sync="resultDialog.visible" width="520px">
      <el-form label-width="90px">
        <el-form-item label="处理结果">
          <el-input type="textarea" v-model="resultDialog.form.result_text" rows="3" />
        </el-form-item>
        <el-form-item label="公示文本">
          <el-input type="textarea" v-model="resultDialog.form.public_text" rows="3" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="resultDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="resultDialog.loading" @click="submitResult">提交</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listMyWorkOrders, addLawEvidence, saveLawResult, uploadFile } from "@/api";
import Uploader from "@/components/Uploader.vue";
import MapViewer from "@/components/MapViewer.vue";

export default {
  name: "LawMyTasks",
  components: { Uploader, MapViewer },
  data() {
    return {
      activeTab: "ALL",
      status: "",
      list: [],
      loading: false,
      evidenceDialog: {
        visible: false,
        loading: false,
        workOrderId: null,
        form: { note: "", address: "", latitude: "", longitude: "" },
        files: []
      },
      resultDialog: {
        visible: false,
        loading: false,
        workOrderId: null,
        form: { result_text: "", public_text: "" }
      }
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      const map = {
        ASSIGNED: "待取证",
        ON_SITE: "待录入结果",
        FINISHED: "已完成",
        TRANSFERRED: "已推送救助医疗机构",
        ARCHIVED: "已归档"
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
    onTabChange() {
      this.status = this.activeTab === "ALL" ? "" : this.activeTab;
      this.fetch();
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = await listMyWorkOrders({ status: this.status });
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    open(row) {
      this.$router.push(`/law/workorders/${row.id}`);
    },
    openEvidence(row) {
      this.evidenceDialog.workOrderId = row.id;
      this.evidenceDialog.form = { note: "", address: "", latitude: "", longitude: "" };
      this.evidenceDialog.files = [];
      this.evidenceDialog.visible = true;
    },
    locateEvidence() {
      if (!window.BMap) {
        this.$message.warning("地图未加载，无法定位");
        return;
      }
      const geolocation = new window.BMap.Geolocation();
      geolocation.getCurrentPosition(
        (r) => {
          if (geolocation.getStatus() === window.BMAP_STATUS_SUCCESS) {
            const point = r.point;
            this.evidenceDialog.form.latitude = point.lat.toFixed(6);
            this.evidenceDialog.form.longitude = point.lng.toFixed(6);
            const geocoder = new window.BMap.Geocoder();
            geocoder.getLocation(point, (rs) => {
              if (rs && rs.address) {
                this.evidenceDialog.form.address = rs.address;
              }
            });
          } else {
            this.$message.warning("定位失败，请手动填写");
          }
        },
        { enableHighAccuracy: true }
      );
    },
    onEvidenceFiles(files) {
      this.evidenceDialog.files = files;
    },
    async uploadEvidenceFiles() {
      const urls = [];
      for (const f of this.evidenceDialog.files) {
        const resp = await uploadFile(f);
        if (resp.code === 0 && resp.data && resp.data.file_url) {
          urls.push(resp.data.file_url);
        }
      }
      return urls;
    },
    async submitEvidence() {
      this.evidenceDialog.loading = true;
      try {
        const attachments = await this.uploadEvidenceFiles();
        const resp = await addLawEvidence({
          workOrderId: this.evidenceDialog.workOrderId,
          note: this.evidenceDialog.form.note,
          address: this.evidenceDialog.form.address,
          latitude: this.evidenceDialog.form.latitude ? Number(this.evidenceDialog.form.latitude) : null,
          longitude: this.evidenceDialog.form.longitude ? Number(this.evidenceDialog.form.longitude) : null,
          attachments
        });
        if (resp.code === 0) {
          this.$message.success("取证已提交");
          this.evidenceDialog.visible = false;
          this.fetch();
        } else {
          this.$message.error(resp.message || "取证失败");
        }
      } finally {
        this.evidenceDialog.loading = false;
      }
    },
    openResult(row) {
      this.resultDialog.workOrderId = row.id;
      this.resultDialog.form = { result_text: "", public_text: "" };
      this.resultDialog.visible = true;
    },
    async submitResult() {
      if (!this.resultDialog.form.result_text) {
        this.$message.warning("请填写处理结果");
        return;
      }
      this.resultDialog.loading = true;
      try {
        const resp = await saveLawResult(this.resultDialog.workOrderId, {
          resultText: this.resultDialog.form.result_text,
          publicText: this.resultDialog.form.public_text
        });
        if (resp.code === 0) {
          this.$message.success("结果已录入");
          this.resultDialog.visible = false;
          this.fetch();
        } else {
          this.$message.error(resp.message || "录入失败");
        }
      } finally {
        this.resultDialog.loading = false;
      }
    }
  }
};
</script>
