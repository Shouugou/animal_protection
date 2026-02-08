<template>
  <el-card>
    <div slot="header">动物领养</div>
    <el-tabs v-model="tab" @tab-click="handleTab">
      <el-tab-pane label="待领养动物" name="open">
        <el-table :data="list" v-loading="loading" style="width:100%">
          <el-table-column prop="id" label="发布ID" width="90" />
          <el-table-column prop="title" label="发布标题" />
          <el-table-column prop="animal_name" label="名称" width="120" />
          <el-table-column prop="species" label="物种" width="120" />
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <el-button size="mini" @click="openDetail(scope.row)">详情</el-button>
              <el-button type="primary" size="mini" @click="openApply(scope.row)">申请领养</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我领养的动物" name="mine">
        <el-table :data="myList" v-loading="loadingMine" style="width:100%">
          <el-table-column prop="id" label="领养ID" width="90" />
          <el-table-column prop="title" label="发布标题" />
          <el-table-column prop="animal_name" label="名称" width="120" />
          <el-table-column prop="species" label="物种" width="120" />
          <el-table-column label="状态" width="120">
            <template slot-scope="scope">
              {{ adoptStatusText(scope.row.status) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button size="mini" @click="openMyDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="动物详情" :visible.sync="showDetail" width="720px">
      <div v-if="detail">
        <div class="detail-row"><strong>名称：</strong>{{ detail.animal_name || "未命名" }}</div>
        <div class="detail-row"><strong>物种：</strong>{{ detail.species }}</div>
        <div class="detail-row"><strong>健康概述：</strong>{{ detail.health_summary || "无" }}</div>
        <div class="detail-row"><strong>发布说明：</strong>{{ detail.description || "无" }}</div>
        <div class="detail-row">
          <strong>发布图片：</strong>
          <div class="img-grid">
            <el-image v-for="(url, idx) in detail.attachments" :key="idx" :src="url" fit="cover" />
            <div v-if="!detail.attachments || detail.attachments.length === 0" class="muted">暂无图片</div>
          </div>
        </div>
        <div class="detail-row">
          <strong>档案记录：</strong>
          <el-table :data="detail.medical_records || []" size="mini" style="width:100%">
            <el-table-column prop="record_type" label="类型" width="120" />
            <el-table-column prop="recorded_at" label="时间" width="180" />
            <el-table-column label="内容">
              <template slot-scope="scope">
                {{ formatRecord(scope.row.record_content) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="showDetail=false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="领养申请" :visible.sync="showApply" width="520px">
      <el-form label-width="90px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="经验"><el-input type="textarea" v-model="form.exp" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showApply=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </span>
    </el-dialog>

    <el-dialog title="领养详情" :visible.sync="showMyDetail" width="720px">
      <div v-if="myDetail">
        <div class="detail-row"><strong>名称：</strong>{{ myDetail.animal_name || "未命名" }}</div>
        <div class="detail-row"><strong>物种：</strong>{{ myDetail.species }}</div>
        <div class="detail-row"><strong>健康概述：</strong>{{ myDetail.health_summary || "无" }}</div>
        <div class="detail-row">
          <strong>发布图片：</strong>
          <div class="img-grid">
            <el-image v-for="(url, idx) in myDetail.attachments" :key="idx" :src="url" fit="cover" />
            <div v-if="!myDetail.attachments || myDetail.attachments.length === 0" class="muted">暂无图片</div>
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="showMyDetail=false">关闭</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import {
  listAdoptionListings,
  getAdoptionListing,
  applyAdoption,
  listMyAdoptions
} from "@/api";

export default {
  name: "Adoption",
  data() {
    return {
      tab: "open",
      list: [],
      myList: [],
      loading: false,
      loadingMine: false,
      showDetail: false,
      showApply: false,
      showMyDetail: false,
      current: null,
      detail: null,
      myDetail: null,
      saving: false,
      form: { name: "", address: "", exp: "" }
    };
  },
  created() {
    this.fetchOpen();
  },
  methods: {
    adoptStatusText(status) {
      return {
        APPLIED: "已申请",
        APPROVED: "已领养",
        REJECTED: "已拒绝",
        CANCELLED: "已取消"
      }[status] || status || "未知";
    },
    formatRecord(content) {
      if (!content) return "";
      try {
        const obj = typeof content === "string" ? JSON.parse(content) : content;
        if (obj.text) return obj.text;
        return JSON.stringify(obj);
      } catch (e) {
        return content;
      }
    },
    handleTab() {
      if (this.tab === "open") {
        this.fetchOpen();
      } else {
        this.fetchMine();
      }
    },
    async fetchOpen() {
      this.loading = true;
      try {
        const resp = await listAdoptionListings();
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    async fetchMine() {
      this.loadingMine = true;
      try {
        const resp = await listMyAdoptions();
        if (resp.code === 0) this.myList = resp.data || [];
      } finally {
        this.loadingMine = false;
      }
    },
    async openDetail(row) {
      const resp = await getAdoptionListing(row.id);
      if (resp.code === 0) {
        this.detail = resp.data || null;
        this.showDetail = true;
      }
    },
    openApply(row) {
      this.current = row;
      this.showApply = true;
    },
    async submit() {
      if (!this.current) return;
      this.saving = true;
      try {
        const resp = await applyAdoption(this.current.id, {
          applyForm: JSON.stringify(this.form)
        });
        if (resp.code === 0) {
          this.$message.success("已提交领养申请");
          this.showApply = false;
          this.fetchOpen();
          this.fetchMine();
        }
      } finally {
        this.saving = false;
      }
    },
    openMyDetail(row) {
      this.myDetail = row;
      this.showMyDetail = true;
    }
  }
};
</script>

<style scoped>
.detail-row {
  margin-bottom: 12px;
}
.img-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.img-grid .el-image {
  width: 120px;
  height: 120px;
  border-radius: 6px;
}
.muted {
  color: #999;
}
</style>
