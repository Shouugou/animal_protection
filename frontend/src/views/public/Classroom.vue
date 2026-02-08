<template>
  <div class="classroom">
    <div class="header">
      <div class="title">动保课堂</div>
      <div class="actions">
        <el-button type="primary" size="mini" @click="openPublish">发布内容</el-button>
      </div>
    </div>

    <el-tabs v-model="tab" @tab-click="handleTab">
      <el-tab-pane label="课堂内容" name="list">
        <div class="category-bar">
          <el-tag
            v-for="c in categories"
            :key="c.id || 'all'"
            :type="activeCategory === c.id ? 'primary' : 'info'"
            @click="selectCategory(c.id)"
          >
            {{ c.name }}
          </el-tag>
        </div>
        <div class="grid" v-loading="loading">
          <div v-for="item in list" :key="item.id" class="card" @click="openDetail(item)">
            <div class="cover">
              <img v-if="item.cover_url" :src="item.cover_url" alt="cover" />
              <div v-else class="placeholder">
                {{ item.content_type === "VIDEO" ? "视频" : "图文" }}
              </div>
            </div>
            <div class="card-body">
              <div class="meta">
                <el-tag size="mini" type="success">
                  {{ item.content_type === "VIDEO" ? "短视频" : "图文" }}
                </el-tag>
                <span class="category">{{ item.category_name || "未分类" }}</span>
              </div>
              <div class="title-text">{{ item.title }}</div>
              <div class="time">{{ item.published_at }}</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的投稿" name="mine">
        <el-table :data="myList" v-loading="loadingMine" style="width:100%">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="category_name" label="分类" width="160" />
          <el-table-column prop="content_type" label="类型" width="120">
            <template slot-scope="scope">
              {{ scope.row.content_type === "VIDEO" ? "短视频" : "图文" }}
            </template>
          </el-table-column>
          <el-table-column label="审核进度" width="200">
            <template slot-scope="scope">
              {{ approvalStepText(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="140">
            <template slot-scope="scope">
              {{ statusText(scope.row.status) }}
            </template>
          </el-table-column>
          <el-table-column prop="published_at" label="发布时间" width="180" />
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button size="mini" type="danger" @click="removeMy(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="内容详情" :visible.sync="showDetail" width="780px">
      <div v-if="current" class="detail">
        <div class="detail-title">{{ current.title }}</div>
        <div class="detail-meta">
          <el-tag size="mini" type="success">
            {{ current.content_type === "VIDEO" ? "短视频" : "图文" }}
          </el-tag>
          <span class="category">{{ current.category_name || "未分类" }}</span>
        </div>
        <div v-if="current.cover_url" class="cover">
          <img :src="current.cover_url" alt="cover" />
        </div>
        <div v-if="current.video_url" class="video-wrap">
          <video controls :src="current.video_url" />
        </div>
        <div v-else class="body">{{ current.body }}</div>
      </div>
      <span slot="footer">
        <el-button @click="showDetail=false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="发布内容" :visible.sync="showPublish" width="640px">
      <el-form label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option v-for="c in publishCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.contentType">
            <el-radio-button label="ARTICLE">图文</el-radio-button>
            <el-radio-button label="VIDEO">短视频</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面">
          <Uploader title="上传封面" @change="onCover" />
        </el-form-item>
        <el-form-item v-if="form.contentType === 'ARTICLE'" label="正文">
          <el-input type="textarea" v-model="form.body" rows="5" />
        </el-form-item>
        <el-form-item v-else label="视频">
          <Uploader title="上传视频" @change="onVideo" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showPublish=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交审核</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import Uploader from "@/components/Uploader.vue";
import {
  listContent,
  getContent,
  listContentCategories,
  createPublicContent,
  listMyContent,
  deleteMyContent,
  uploadFile
} from "@/api";

export default {
  name: "Classroom",
  components: { Uploader },
  data() {
    return {
      tab: "list",
      categories: [{ id: null, name: "全部" }],
      activeCategory: null,
      list: [],
      loading: false,
      showDetail: false,
      current: null,
      showPublish: false,
      form: {
        title: "",
        categoryId: null,
        contentType: "ARTICLE",
        body: "",
        coverUrl: "",
        videoUrl: ""
      },
      coverFiles: [],
      videoFiles: [],
      saving: false,
      myList: [],
      loadingMine: false
    };
  },
  computed: {
    publishCategories() {
      return this.categories.filter((c) => c.id);
    }
  },
  created() {
    this.init();
  },
  methods: {
    statusText(status) {
      return {
        PENDING: "待审核",
        PUBLISHED: "已发布",
        REJECTED: "已驳回"
      }[status] || status;
    },
    approvalStepText(row) {
      if (!row.steps) return "无";
      try {
        const steps = typeof row.steps === "string" ? JSON.parse(row.steps) : row.steps;
        const idx = (row.current_step || 1) - 1;
        const current = steps[idx] ? steps[idx].role : null;
        const nameMap = { ADMIN: "管理员", LAW: "执法机构", RESCUE: "救助机构" };
        const flow = steps.map((s) => nameMap[s.role] || s.role).join(" → ");
        const currentName = nameMap[current] || current || "完成";
        return `${currentName}（${flow}）`;
      } catch (e) {
        return "进行中";
      }
    },
    async init() {
      const resp = await listContentCategories();
      if (resp.code === 0) {
        this.categories = [{ id: null, name: "全部" }, ...(resp.data || [])];
      }
      this.fetch();
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = await listContent({ categoryId: this.activeCategory });
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    async fetchMine() {
      this.loadingMine = true;
      try {
        const resp = await listMyContent();
        if (resp.code === 0) this.myList = resp.data || [];
      } finally {
        this.loadingMine = false;
      }
    },
    handleTab() {
      if (this.tab === "mine") {
        this.fetchMine();
      }
    },
    selectCategory(id) {
      this.activeCategory = id;
      this.fetch();
    },
    async openDetail(row) {
      const resp = await getContent(row.id);
      if (resp.code === 0) {
        this.current = resp.data || {};
        this.showDetail = true;
      }
    },
    openPublish() {
      this.showPublish = true;
    },
    onCover(files) {
      this.coverFiles = files;
    },
    onVideo(files) {
      this.videoFiles = files;
    },
    async uploadOne(files) {
      if (!files || files.length === 0) return "";
      const resp = await uploadFile(files[0]);
      return resp.code === 0 && resp.data ? resp.data.file_url : "";
    },
    async submit() {
      if (!this.form.title || !this.form.categoryId) {
        this.$message.warning("请填写标题并选择分类");
        return;
      }
      this.saving = true;
      try {
        const coverUrl = await this.uploadOne(this.coverFiles);
        const videoUrl = this.form.contentType === "VIDEO" ? await this.uploadOne(this.videoFiles) : "";
        const resp = await createPublicContent({
          title: this.form.title,
          categoryId: this.form.categoryId,
          contentType: this.form.contentType,
          body: this.form.contentType === "ARTICLE" ? this.form.body : "",
          videoUrl,
          coverUrl
        });
        if (resp.code === 0) {
          this.$message.success("已提交审核");
          this.showPublish = false;
          this.fetchMine();
        } else {
          this.$message.error(resp.message || "提交失败");
        }
      } finally {
        this.saving = false;
      }
    }
    ,
    async removeMy(row) {
      await deleteMyContent(row.id);
      this.fetchMine();
    }
  }
};
</script>

<style scoped>
.classroom {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.category-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.card {
  border: 1px solid #eef0f2;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}
.cover {
  height: 140px;
  background: #f4f6f8;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder {
  color: #999;
  font-size: 16px;
}
.card-body {
  padding: 10px 12px 12px;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.category {
  color: #666;
  font-size: 12px;
}
.title-text {
  font-size: 14px;
  font-weight: 600;
  color: #222;
  line-height: 1.4;
}
.time {
  margin-top: 6px;
  color: #999;
  font-size: 12px;
}
.detail-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}
.detail-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.cover {
  width: 100%;
  height: 260px;
  margin-bottom: 12px;
  border-radius: 10px;
  overflow: hidden;
  background: #f4f6f8;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.video-wrap video {
  width: 100%;
  max-height: 420px;
}
.body {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
