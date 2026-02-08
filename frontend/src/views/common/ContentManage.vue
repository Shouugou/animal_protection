<template>
  <el-card>
    <div slot="header" class="header-row">
      <span>动保课堂内容发布</span>
      <el-button type="primary" size="mini" @click="openPublish">发布内容</el-button>
    </div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="category_name" label="分类" width="160" />
      <el-table-column label="类型" width="120">
        <template slot-scope="scope">
          {{ scope.row.content_type === "VIDEO" ? "短视频" : "图文" }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">
          {{ statusText(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column prop="published_at" label="发布时间" width="180" />
    </el-table>

    <el-dialog title="发布内容" :visible.sync="showPublish" width="640px">
      <el-form label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
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
        <el-button type="primary" :loading="saving" @click="submit">发布</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import Uploader from "@/components/Uploader.vue";
import {
  listContentCategories,
  listLawContent,
  createLawContent,
  listRescueContent,
  createRescueContent,
  uploadFile
} from "@/api";

export default {
  name: "ContentManage",
  components: { Uploader },
  data() {
    return {
      list: [],
      loading: false,
      showPublish: false,
      saving: false,
      categories: [],
      form: {
        title: "",
        categoryId: null,
        contentType: "ARTICLE",
        body: "",
        coverUrl: "",
        videoUrl: ""
      },
      coverFiles: [],
      videoFiles: []
    };
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
    async init() {
      const resp = await listContentCategories();
      if (resp.code === 0) this.categories = resp.data || [];
      this.fetch();
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = this.$store.getters.roleCode === "LAW"
          ? await listLawContent()
          : await listRescueContent();
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
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
        const payload = {
          title: this.form.title,
          categoryId: this.form.categoryId,
          contentType: this.form.contentType,
          body: this.form.contentType === "ARTICLE" ? this.form.body : "",
          videoUrl,
          coverUrl
        };
        const resp = this.$store.getters.roleCode === "LAW"
          ? await createLawContent(payload)
          : await createRescueContent(payload);
        if (resp.code === 0) {
          this.$message.success("内容已发布");
          this.showPublish = false;
          this.fetch();
        }
      } finally {
        this.saving = false;
      }
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
</style>
