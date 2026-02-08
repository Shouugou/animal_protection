<template>
  <el-card>
    <div slot="header">共享病例</div>
    <el-tabs v-model="activeTab" @tab-click="fetch">
      <el-tab-pane label="正在共享" name="received" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="animal_name" label="动物" />
      <el-table-column prop="species" label="物种" width="120" />
      <el-table-column prop="from_org_name" label="共享机构" />
      <el-table-column prop="to_org_name" label="接收机构" />
      <el-table-column label="状态" width="120">
        <template slot-scope="scope">
          {{ statusText(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="共享时间" width="240">
        <template slot-scope="scope">
          <span class="nowrap">{{ formatTime(scope.row.created_at) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button size="mini" @click="openDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="共享病例详情" :visible.sync="detail.visible" width="760px">
      <el-descriptions :column="2" v-if="detail.data.id">
        <el-descriptions-item label="动物">{{ detail.data.animal_name || "-" }} / {{ detail.data.species }}</el-descriptions-item>
        <el-descriptions-item label="共享状态">{{ statusText(detail.data.status) }}</el-descriptions-item>
        <el-descriptions-item label="共享机构">{{ detail.data.from_org_name }} → {{ detail.data.to_org_name }}</el-descriptions-item>
        <el-descriptions-item label="共享说明">{{ detail.data.note || "-" }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail.data.health_summary" style="margin-top:8px;color:#374151">
        {{ detail.data.health_summary }}
      </div>
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
        <div slot="header">交流记录</div>
        <div class="chat-list">
          <div v-for="msg in detail.messages" :key="msg.id" class="chat-item">
            <div class="chat-meta">
              <strong>{{ msg.sender_org_name || "机构" }}</strong>
              <span class="chat-time">{{ formatTime(msg.created_at) }}</span>
            </div>
            <div class="chat-content">{{ msg.content }}</div>
          </div>
        </div>
        <div class="chat-input" v-if="detail.data.status !== 'CLOSED'">
          <el-input type="textarea" v-model="detail.content" rows="2" />
          <el-button type="primary" size="mini" :loading="detail.sending" @click="sendMessage">
            发送
          </el-button>
        </div>
      </el-card>
    </el-dialog>
  </el-card>
</template>

<script>
import { listCaseShares, getCaseShare, addCaseShareMessage } from "@/api";

export default {
  name: "RescueSharedCases",
  data() {
    return {
      activeTab: "received",
      list: [],
      loading: false,
      detail: { visible: false, data: {}, records: [], messages: [], content: "", loading: false, sending: false }
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      return { ACTIVE: "共享中", CLOSED: "已结束" }[status] || status || "未知";
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
    parseContent(raw) {
      if (!raw) return "";
      try {
        const obj = JSON.parse(raw);
        return obj.text || raw;
      } catch (e) {
        return raw;
      }
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = await listCaseShares({
          direction: this.activeTab === "received" ? "received" : undefined
        });
        if (resp.code === 0) {
          const data = resp.data || [];
          if (this.activeTab === "received") {
            this.list = data.filter((item) => item.status !== "CLOSED");
          } else {
            this.list = data;
          }
        }
      } finally {
        this.loading = false;
      }
    },
    async openDetail(row) {
      this.detail.visible = true;
      this.detail.loading = true;
      try {
        const resp = await getCaseShare(row.id);
        if (resp.code === 0) {
          this.detail.data = resp.data || {};
          this.detail.records = (resp.data.records || []).map((item) => ({
            ...item,
            record_type_text: this.recordTypeText(item.record_type),
            record_content: this.parseContent(item.record_content),
            recorded_at: this.formatTime(item.recorded_at)
          }));
          this.detail.messages = resp.data.messages || [];
        }
      } finally {
        this.detail.loading = false;
      }
    },
    async sendMessage() {
      if (!this.detail.data.id || !this.detail.content) return;
      this.detail.sending = true;
      try {
        const resp = await addCaseShareMessage(this.detail.data.id, { content: this.detail.content });
        if (resp.code === 0) {
          this.detail.content = "";
          const detailResp = await getCaseShare(this.detail.data.id);
          if (detailResp.code === 0) {
            this.detail.messages = detailResp.data.messages || [];
          }
        }
      } finally {
        this.detail.sending = false;
      }
    }
  }
};
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
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
