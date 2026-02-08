<template>
  <el-card>
    <div slot="header" class="header-row">
      <span>内容审核</span>
      <el-select v-model="status" size="mini" style="width:120px" @change="fetch">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
    </div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="category_name" label="分类" width="160" />
      <el-table-column prop="target_org_name" label="目标机构" width="160" />
      <el-table-column prop="author_name" label="投稿人" width="140" />
      <el-table-column prop="created_at" label="提交时间" width="180" />
      <el-table-column label="操作" width="180">
        <template slot-scope="scope">
          <el-button size="mini" @click="openPreview(scope.row)">预览</el-button>
          <el-button size="mini" type="success" v-if="scope.row.approval_status === 'PENDING'" @click="approve(scope.row)">通过</el-button>
          <el-button size="mini" type="danger" v-if="scope.row.approval_status === 'PENDING'" @click="reject(scope.row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="内容预览" :visible.sync="showPreview" width="760px">
      <div v-if="preview">
        <div class="preview-title">{{ preview.title }}</div>
        <div class="preview-meta">{{ preview.category_name }} | {{ preview.content_type === "VIDEO" ? "短视频" : "图文" }}</div>
        <div v-if="preview.cover_url" class="preview-cover">
          <img :src="preview.cover_url" alt="cover" />
        </div>
        <div v-if="preview.video_url" class="preview-video">
          <video controls :src="preview.video_url" />
        </div>
        <div v-else class="preview-body">{{ preview.body }}</div>
      </div>
      <span slot="footer">
        <el-button @click="showPreview=false">关闭</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import {
  listContentApprovals,
  approveContent,
  rejectContent,
  listLawContentApprovals,
  approveLawContent,
  rejectLawContent,
  listRescueContentApprovals,
  approveRescueContent,
  rejectRescueContent,
  getContent
} from "@/api";

export default {
  name: "ContentApprovals",
  data() {
    return {
      status: "PENDING",
      list: [],
      loading: false,
      showPreview: false,
      preview: null
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    async fetch() {
      this.loading = true;
      try {
        const role = this.$store.getters.roleCode;
        let resp;
        if (role === "LAW") {
          resp = await listLawContentApprovals({ status: this.status });
        } else if (role === "RESCUE") {
          resp = await listRescueContentApprovals({ status: this.status });
        } else {
          resp = await listContentApprovals({ status: this.status });
        }
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    openPreview(row) {
      getContent(row.content_id).then((resp) => {
        if (resp.code === 0) {
          this.preview = resp.data || row;
        } else {
          this.preview = row;
        }
        this.showPreview = true;
      });
    },
    async approve(row) {
      const role = this.$store.getters.roleCode;
      if (role === "LAW") {
        await approveLawContent(row.instance_id);
      } else if (role === "RESCUE") {
        await approveRescueContent(row.instance_id);
      } else {
        await approveContent(row.instance_id);
      }
      this.fetch();
    },
    async reject(row) {
      const role = this.$store.getters.roleCode;
      if (role === "LAW") {
        await rejectLawContent(row.instance_id, { note: "驳回" });
      } else if (role === "RESCUE") {
        await rejectRescueContent(row.instance_id, { note: "驳回" });
      } else {
        await rejectContent(row.instance_id, { note: "驳回" });
      }
      this.fetch();
    }
  }
};
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.preview-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}
.preview-meta {
  color: #666;
  margin-bottom: 12px;
}
.preview-cover img {
  max-width: 100%;
  border-radius: 6px;
}
.preview-video video {
  width: 100%;
  max-height: 420px;
  margin-top: 12px;
}
.preview-body {
  margin-top: 12px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
