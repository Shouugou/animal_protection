<template>
  <el-card>
    <div slot="header" class="header-row">
      <span>动物领养</span>
      <el-button type="primary" size="mini" @click="openPublish">发布领养</el-button>
    </div>
    <el-tabs v-model="tab" @tab-click="handleTab">
      <el-tab-pane label="未领养动物" name="open">
        <el-table :data="openList" v-loading="loading" style="width:100%">
          <el-table-column prop="id" label="发布ID" width="90" />
          <el-table-column prop="title" label="发布标题" />
          <el-table-column prop="animal_name" label="名称" width="120" />
          <el-table-column prop="species" label="物种" width="120" />
          <el-table-column prop="published_at" label="发布时间" width="180" />
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <el-button size="mini" @click="openDetail(scope.row)">详情</el-button>
              <el-button size="mini" type="danger" @click="remove(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="已领养动物" name="adopted">
        <el-table :data="adoptedList" v-loading="loadingAdopted" style="width:100%">
          <el-table-column prop="id" label="发布ID" width="90" />
          <el-table-column prop="title" label="发布标题" />
          <el-table-column prop="animal_name" label="名称" width="120" />
          <el-table-column prop="species" label="物种" width="120" />
          <el-table-column prop="adopter_name" label="领养人" width="140" />
          <el-table-column label="回访状态" width="140">
            <template slot-scope="scope">
              {{ followupStatusText(scope.row.followup_status) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <el-button v-if="!scope.row.followup_status" size="mini" type="primary" @click="sendFollowup(scope.row)">
                发送回访问卷
              </el-button>
              <el-button v-else size="mini" @click="openFollowup(scope.row)">查看回访</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="发布领养" :visible.sync="showPublish" width="600px">
      <el-form label-width="100px">
        <el-form-item label="选择动物">
          <el-select v-model="publish.animalId" placeholder="请选择" style="width:100%">
            <el-option v-for="a in availableAnimals" :key="a.id" :label="animalLabel(a)" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布标题"><el-input v-model="publish.title" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="publish.description" rows="3" /></el-form-item>
      </el-form>
      <Uploader title="领养照片" @change="onFiles" />
      <span slot="footer">
        <el-button @click="showPublish=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPublish">发布</el-button>
      </span>
    </el-dialog>

    <el-dialog title="发布详情" :visible.sync="showDetail" width="640px">
      <div v-if="detail">
        <div class="detail-row"><strong>标题：</strong>{{ detail.title }}</div>
        <div class="detail-row"><strong>动物：</strong>{{ detail.animal_name }}（{{ detail.species }}）</div>
        <div class="detail-row"><strong>描述：</strong>{{ detail.description || "无" }}</div>
        <div class="detail-row">
          <strong>图片：</strong>
          <div class="img-grid">
            <el-image v-for="(url, idx) in detail.attachments" :key="idx" :src="url" fit="cover" />
            <div v-if="!detail.attachments || detail.attachments.length === 0" class="muted">暂无图片</div>
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="showDetail=false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="回访问卷" :visible.sync="showFollowup" width="640px">
      <div v-if="followup">
        <div class="detail-row"><strong>发送时间：</strong>{{ followup.sent_at }}</div>
        <div class="detail-row"><strong>状态：</strong>{{ followupStatusText(followup.task_status) }}</div>
        <el-divider />
        <div v-if="followup.answer">
          <div class="detail-row" v-for="item in answerItems(followup)" :key="item.key">
            <strong>{{ item.label }}：</strong>{{ item.value }}
          </div>
          <div class="detail-row">
            <strong>附件：</strong>
            <div class="img-grid">
              <el-image v-for="(url, idx) in followup.answer.attachments" :key="idx" :src="url" fit="cover" />
              <div v-if="!followup.answer.attachments || followup.answer.attachments.length === 0" class="muted">无附件</div>
            </div>
          </div>
        </div>
        <div v-else class="muted">暂无回访提交内容。</div>
      </div>
      <span slot="footer">
        <el-button @click="showFollowup=false">关闭</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import Uploader from "@/components/Uploader.vue";
import {
  listRescueAdoptionListings,
  createRescueAdoptionListing,
  deleteRescueAdoptionListing,
  sendRescueFollowup,
  getRescueFollowupDetail,
  listRescueAnimals,
  uploadFile
} from "@/api";

export default {
  name: "RescueAdoption",
  components: { Uploader },
  data() {
    return {
      tab: "open",
      openList: [],
      adoptedList: [],
      loading: false,
      loadingAdopted: false,
      showPublish: false,
      publish: { animalId: null, title: "", description: "" },
      saving: false,
      files: [],
      availableAnimals: [],
      showDetail: false,
      detail: null,
      showFollowup: false,
      followup: null
    };
  },
  created() {
    this.fetchOpen();
  },
  methods: {
    animalLabel(a) {
      return `${a.name || "未命名"}-${a.species}`;
    },
    followupStatusText(status) {
      return {
        PENDING: "待回收问卷",
        SUBMITTED: "已回收问卷"
      }[status] || (status ? status : "未发送");
    },
    handleTab() {
      if (this.tab === "open") {
        this.fetchOpen();
      } else {
        this.fetchAdopted();
      }
    },
    async fetchOpen() {
      this.loading = true;
      try {
        const resp = await listRescueAdoptionListings({ status: "OPEN" });
        if (resp.code === 0) this.openList = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    async fetchAdopted() {
      this.loadingAdopted = true;
      try {
        const resp = await listRescueAdoptionListings({ status: "ADOPTED" });
        if (resp.code === 0) this.adoptedList = resp.data || [];
      } finally {
        this.loadingAdopted = false;
      }
    },
    async openPublish() {
      this.showPublish = true;
      const resp = await listRescueAnimals();
      if (resp.code === 0) {
        this.availableAnimals =
          (resp.data || []).filter((a) => ["TREATED", "READY_FOR_ADOPTION"].includes(a.status));
      }
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
    async submitPublish() {
      this.saving = true;
      try {
        const attachments = await this.uploadAll();
        const resp = await createRescueAdoptionListing({
          animalId: this.publish.animalId,
          title: this.publish.title,
          description: this.publish.description,
          attachments
        });
        if (resp.code === 0) {
          this.$message.success("已发布领养");
          this.showPublish = false;
          this.fetchOpen();
        }
      } finally {
        this.saving = false;
      }
    },
    openDetail(row) {
      this.detail = row;
      this.showDetail = true;
    },
    async remove(row) {
      await deleteRescueAdoptionListing(row.id);
      this.fetchOpen();
    },
    async sendFollowup(row) {
      if (!row.adoption_id) {
        this.$message.warning("未找到领养信息");
        return;
      }
      const resp = await sendRescueFollowup(row.adoption_id);
      if (resp.code === 0) {
        this.$message.success("已发送回访问卷");
        this.fetchAdopted();
      }
    },
    async openFollowup(row) {
      if (!row.adoption_id) return;
      const resp = await getRescueFollowupDetail(row.adoption_id);
      if (resp.code === 0) {
        this.followup = resp.data || null;
        this.showFollowup = true;
      }
    },
    parsedAnswer(answer) {
      if (!answer) return {};
      try {
        return typeof answer === "string" ? JSON.parse(answer) : answer;
      } catch (e) {
        return { answer };
      }
    },
    parseTemplate(template) {
      if (!template) return { questions: [] };
      try {
        return typeof template === "string" ? JSON.parse(template) : template;
      } catch (e) {
        return { questions: [] };
      }
    },
    answerItems(detail) {
      const answers = this.parsedAnswer(detail.answer && detail.answer.questionnaire);
      const template = this.parseTemplate(detail.questionnaire_template);
      if (template && template.questions && template.questions.length > 0) {
        return template.questions.map((q) => ({
          key: q.key,
          label: q.label || q.key,
          value: answers[q.key] || ""
        }));
      }
      return Object.keys(answers).map((key) => ({
        key,
        label: key,
        value: answers[key]
      }));
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
.detail-row {
  margin-bottom: 10px;
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
