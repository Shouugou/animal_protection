<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>动物档案</span>
      <el-button size="mini" type="primary" @click="show = true">新建档案</el-button>
    </div>
    <el-table :data="list" style="width:100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="rescue_task_id" label="任务ID" width="100" />
      <el-table-column prop="name" label="名称" width="120" />
      <el-table-column prop="species" label="物种" />
      <el-table-column label="状态" width="160">
        <template slot-scope="scope">
          {{ animalStatusText(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-link type="primary" @click="openDetail(scope.row)">
            详情
          </el-link>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="动物建档" :visible.sync="show" width="520px">
      <el-form label-width="90px">
        <el-form-item label="救助任务">
          <el-select v-model="form.rescue_task_id" placeholder="选择救助任务">
            <el-option
              v-for="t in tasks"
              :key="t.id"
              :label="`${t.id} - ${t.event_type || '事件'}`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <div v-for="(item, idx) in form.items" :key="idx" class="animal-row">
          <div class="animal-row-header">
            <span>动物 {{ idx + 1 }}</span>
            <el-button v-if="form.items.length > 1" type="text" @click="removeItem(idx)">移除</el-button>
          </div>
          <el-form-item label="名称">
            <el-input v-model="item.name" placeholder="可选" />
          </el-form-item>
          <el-form-item label="物种">
            <el-input v-model="item.species" />
          </el-form-item>
          <el-form-item label="体检摘要">
            <el-input type="textarea" v-model="item.summary" rows="3" />
          </el-form-item>
          <el-divider />
        </div>
        <el-form-item>
          <el-button size="mini" @click="addItem">新增一只</el-button>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="动物档案详情" :visible.sync="detail.visible" width="760px">
      <el-card v-if="detail.item">
        <el-descriptions :column="2">
          <el-descriptions-item label="名称">{{ detail.item.name || "-" }}</el-descriptions-item>
          <el-descriptions-item label="物种">{{ detail.item.species }}</el-descriptions-item>
        <el-descriptions-item label="任务ID" :content-style="{ 'white-space': 'nowrap' }">
          {{ detail.item.rescue_task_id }}
        </el-descriptions-item>
          <el-descriptions-item label="状态">{{ animalStatusText(detail.item.status) }}</el-descriptions-item>
          <el-descriptions-item label="建档时间">{{ formatTime(detail.item.created_at) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detail.item.health_summary" style="margin-top:8px;color:#374151">
          {{ detail.item.health_summary }}
        </div>
      </el-card>
      <el-divider />
      <el-table :data="detail.records" v-loading="detail.loading" style="width:100%">
        <el-table-column prop="record_type_text" label="类型" width="140" />
        <el-table-column prop="record_content" label="内容" />
        <el-table-column label="时间" width="240">
          <template slot-scope="scope">
            <span class="nowrap">{{ scope.row.recorded_at }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />
      <el-card>
        <div slot="header">病例共享</div>
        <el-form label-width="90px" class="share-form">
          <el-form-item label="共享机构">
            <el-select v-model="shareForm.targetOrgId" placeholder="选择救助机构">
              <el-option
                v-for="org in shareTargets"
                :key="org.id"
                :label="org.name"
                :value="org.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="共享说明">
            <el-input v-model="shareForm.note" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="mini" :loading="shareForm.loading" @click="submitShare">
              发起共享
            </el-button>
          </el-form-item>
        </el-form>

        <el-table :data="detail.shares" v-loading="detail.shareLoading" style="width:100%">
          <el-table-column prop="to_org_name" label="目标机构" />
          <el-table-column label="状态" width="120">
            <template slot-scope="scope">
              {{ shareStatusText(scope.row.status) }}
            </template>
          </el-table-column>
          <el-table-column label="共享时间" width="240">
            <template slot-scope="scope">
              <span class="nowrap">{{ formatTime(scope.row.created_at) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template slot-scope="scope">
              <el-button size="mini" @click="openShare(scope.row)">
                {{ scope.row.status === "CLOSED" ? "查看交流记录" : "交流" }}
              </el-button>
              <el-button
                size="mini"
                type="danger"
                v-if="canCloseShare(scope.row)"
                @click="closeShare(scope.row)"
              >结束</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-dialog>

    <el-dialog title="共享交流" :visible.sync="shareDialog.visible" width="720px">
      <el-descriptions :column="2" v-if="shareDialog.detail.id">
        <el-descriptions-item label="动物">{{ shareDialog.detail.animal_name || "-" }} / {{ shareDialog.detail.species }}</el-descriptions-item>
        <el-descriptions-item label="共享状态">{{ shareStatusText(shareDialog.detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="共享机构">{{ shareDialog.detail.from_org_name }} → {{ shareDialog.detail.to_org_name }}</el-descriptions-item>
        <el-descriptions-item label="共享说明">{{ shareDialog.detail.note || "-" }}</el-descriptions-item>
      </el-descriptions>

      <el-card style="margin-top:12px">
        <div slot="header">交流记录</div>
        <div class="chat-list">
          <div v-for="msg in shareDialog.messages" :key="msg.id" class="chat-item">
            <div class="chat-meta">
              <strong>{{ msg.sender_org_name || "机构" }}</strong>
              <span class="chat-time">{{ formatTime(msg.created_at) }}</span>
            </div>
            <div class="chat-content">{{ msg.content }}</div>
          </div>
        </div>
        <div class="chat-input" v-if="shareDialog.detail.status !== 'CLOSED'">
          <el-input type="textarea" v-model="shareDialog.content" rows="2" />
          <el-button type="primary" size="mini" :loading="shareDialog.sending" @click="sendShareMessage">
            发送
          </el-button>
        </div>
      </el-card>
    </el-dialog>
  </el-card>
</template>

<script>
import {
  listRescueAnimals,
  createRescueAnimal,
  listRescueTasks,
  listMedicalRecords,
  listRescueOrganizations,
  createCaseShare,
  listCaseShares,
  getCaseShare,
  addCaseShareMessage,
  closeCaseShare
} from "@/api";

export default {
  name: "RescueAnimals",
  data() {
    return {
      list: [],
      tasks: [],
      show: false,
      form: { rescue_task_id: "", items: [{ name: "", species: "", summary: "" }] },
      detail: { visible: false, item: null, records: [], loading: false, shares: [], shareLoading: false },
      shareTargets: [],
      shareForm: { targetOrgId: null, note: "", loading: false },
      shareDialog: { visible: false, detail: {}, messages: [], content: "", sending: false },
      loading: false,
      saving: false
    };
  },
  created() {
    this.fetch();
    this.fetchTasks();
    this.loadShareTargets();
    const taskId = this.$route.query.taskId;
    if (taskId) {
      this.show = true;
      this.form.rescue_task_id = Number(taskId);
    }
  },
  methods: {
    animalStatusText(status) {
      return {
        IN_CARE: "照护中",
        TREATED: "治疗完成",
        READY_FOR_ADOPTION: "可领养",
        ADOPTED: "已领养",
        RELEASED: "已放归",
        DECEASED: "已死亡"
      }[status] || status || "未知";
    },
    addItem() {
      this.form.items.push({ name: "", species: "", summary: "" });
    },
    removeItem(idx) {
      this.form.items.splice(idx, 1);
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = await listRescueAnimals();
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    async fetchTasks() {
      const resp = await listRescueTasks({ status: "INTAKE" });
      if (resp.code === 0) {
        this.tasks = resp.data || [];
      }
    },
    async create() {
      if (!this.form.rescue_task_id) {
        this.$message.warning("请选择救助任务");
        return;
      }
      if (this.form.items.some((item) => !item.species)) {
        this.$message.warning("请填写物种");
        return;
      }
      this.saving = true;
      try {
        for (const item of this.form.items) {
          const resp = await createRescueAnimal({
            rescueTaskId: this.form.rescue_task_id,
            name: item.name,
            species: item.species,
            summary: item.summary
          });
          if (resp.code !== 0) {
            this.$message.error(resp.message || "建档失败");
            return;
          }
        }
        this.$message.success("建档成功");
        this.show = false;
        this.form = { rescue_task_id: "", items: [{ name: "", species: "", summary: "" }] };
        this.fetch();
      } finally {
        this.saving = false;
      }
    },
    async openDetail(row) {
      this.detail.visible = true;
      this.detail.item = row;
      this.detail.loading = true;
      try {
        const resp = await listMedicalRecords(row.id);
        if (resp.code === 0) {
          this.detail.records = (resp.data || []).map((item) => ({
            ...item,
            record_type_text: this.recordTypeText(item.record_type),
            record_content: this.parseContent(item.record_content),
            recorded_at: this.formatTime(item.recorded_at)
          }));
        }
      } finally {
        this.detail.loading = false;
      }
      this.loadShares(row.id);
    },
    async loadShareTargets() {
      const resp = await listRescueOrganizations();
      if (resp.code === 0) {
        const orgId = this.$store.state.auth.profile.org_id;
        this.shareTargets = (resp.data || []).filter((o) => o.id !== orgId);
      }
    },
    async loadShares(animalId) {
      this.detail.shareLoading = true;
      try {
        const resp = await listCaseShares();
        if (resp.code === 0) {
          this.detail.shares = (resp.data || []).filter((s) => s.animal_id === animalId);
        }
      } finally {
        this.detail.shareLoading = false;
      }
    },
    shareStatusText(status) {
      return { ACTIVE: "共享中", CLOSED: "已结束" }[status] || status || "未知";
    },
    canCloseShare(share) {
      const orgId = this.$store.state.auth.profile.org_id;
      return share.from_org_id === orgId && share.status === "ACTIVE";
    },
    async submitShare() {
      if (!this.detail.item || !this.shareForm.targetOrgId) {
        this.$message.warning("请选择共享机构");
        return;
      }
      this.shareForm.loading = true;
      try {
        const resp = await createCaseShare({
          animalId: this.detail.item.id,
          targetOrgId: this.shareForm.targetOrgId,
          note: this.shareForm.note
        });
        if (resp.code === 0) {
          this.$message.success("共享已发起");
          this.shareForm = { targetOrgId: null, note: "", loading: false };
          this.loadShares(this.detail.item.id);
        } else {
          this.$message.error(resp.message || "共享失败");
        }
      } finally {
        this.shareForm.loading = false;
      }
    },
    async openShare(share) {
      const resp = await getCaseShare(share.id);
      if (resp.code === 0) {
        this.shareDialog.detail = resp.data || {};
        this.shareDialog.messages = resp.data.messages || [];
        this.shareDialog.content = "";
        this.shareDialog.visible = true;
      }
    },
    async sendShareMessage() {
      if (!this.shareDialog.content || !this.shareDialog.detail.id) {
        return;
      }
      this.shareDialog.sending = true;
      try {
        const resp = await addCaseShareMessage(this.shareDialog.detail.id, {
          content: this.shareDialog.content
        });
        if (resp.code === 0) {
          this.shareDialog.content = "";
          const detailResp = await getCaseShare(this.shareDialog.detail.id);
          if (detailResp.code === 0) {
            this.shareDialog.messages = detailResp.data.messages || [];
          }
        }
      } finally {
        this.shareDialog.sending = false;
      }
    },
    async closeShare(share) {
      const resp = await closeCaseShare(share.id);
      if (resp.code === 0) {
        this.$message.success("共享已结束");
        this.loadShares(this.detail.item.id);
      }
    },
    recordTypeText(type) {
      return {
        CHECKUP: "检查",
        TREATMENT: "治疗",
        MEDICATION: "用药",
        REHAB: "康复",
        OTHER: "其他"
      }[type] || type || "未知";
    },
    parseContent(raw) {
      if (!raw) return "";
      try {
        const obj = JSON.parse(raw);
        return obj.text || raw;
      } catch (e) {
        return raw;
      }
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
    }
  }
};
</script>

<style scoped>
.animal-row {
  padding: 8px 0;
}
.animal-row-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-weight: 600;
}
.nowrap {
  white-space: nowrap;
}
.share-form {
  margin-bottom: 12px;
}
.chat-list {
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid #eee;
  padding: 8px;
  border-radius: 6px;
}
.chat-item + .chat-item {
  margin-top: 8px;
}
.chat-meta {
  display: flex;
  justify-content: space-between;
  color: #6b7280;
  font-size: 12px;
}
.chat-content {
  margin-top: 4px;
}
.chat-input {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
</style>
