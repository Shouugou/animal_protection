<template>
  <el-card>
    <div slot="header">回访管理</div>
    <el-tabs v-model="tab" @tab-click="handleTab">
      <el-tab-pane label="待填写问卷" name="pending">
        <el-table :data="pending" v-loading="loading" style="width:100%">
          <el-table-column prop="id" label="任务ID" width="90" />
          <el-table-column prop="animal_name" label="动物名称" width="140" />
          <el-table-column prop="species" label="物种" width="120" />
          <el-table-column prop="title" label="领养发布" />
          <el-table-column prop="sent_at" label="发送时间" width="180" />
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" @click="openSubmit(scope.row)">填写</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="已填写问卷" name="submitted">
        <el-table :data="submitted" v-loading="loadingSubmitted" style="width:100%">
          <el-table-column prop="id" label="任务ID" width="90" />
          <el-table-column prop="animal_name" label="动物名称" width="140" />
          <el-table-column prop="species" label="物种" width="120" />
          <el-table-column prop="title" label="领养发布" />
          <el-table-column prop="submitted_at" label="提交时间" width="180" />
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button size="mini" @click="openDetail(scope.row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="填写回访问卷" :visible.sync="showSubmit" width="640px">
      <div v-if="current && template">
        <el-form label-width="140px">
          <el-form-item v-for="q in template.questions" :key="q.key" :label="q.label">
            <el-select v-if="q.type === 'select'" v-model="answers[q.key]" placeholder="请选择">
              <el-option v-for="opt in q.options" :key="opt" :label="opt" :value="opt" />
            </el-select>
            <el-input v-else v-model="answers[q.key]" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <Uploader title="回访附件" @change="onFiles" />
      </div>
      <span slot="footer">
        <el-button @click="showSubmit=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </span>
    </el-dialog>

    <el-dialog title="回访问卷详情" :visible.sync="showDetail" width="640px">
      <div v-if="detail">
        <div class="detail-row"><strong>动物：</strong>{{ detail.animal_name }}（{{ detail.species }}）</div>
        <div class="detail-row"><strong>发送时间：</strong>{{ detail.sent_at }}</div>
        <div class="detail-row" v-if="detail.answer">
          <strong>填写时间：</strong>{{ detail.answer.submitted_at }}
        </div>
        <el-divider />
        <div v-if="detail.answer">
          <div v-for="item in answerItems(detail)" :key="item.key" class="detail-row">
            <strong>{{ item.label }}：</strong>{{ item.value }}
          </div>
        </div>
        <div class="detail-row">
          <strong>附件：</strong>
          <div class="img-grid">
            <el-image v-for="(url, idx) in detail.answer && detail.answer.attachments ? detail.answer.attachments : []" :key="idx" :src="url" fit="cover" />
            <div v-if="!detail.answer || !detail.answer.attachments || detail.answer.attachments.length === 0" class="muted">无附件</div>
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="showDetail=false">关闭</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import Uploader from "@/components/Uploader.vue";
import {
  listFollowupTasks,
  getFollowupTask,
  submitFollowupTask,
  uploadFile
} from "@/api";

export default {
  name: "Followups",
  components: { Uploader },
  data() {
    return {
      tab: "pending",
      pending: [],
      submitted: [],
      loading: false,
      loadingSubmitted: false,
      showSubmit: false,
      showDetail: false,
      current: null,
      template: null,
      answers: {},
      files: [],
      saving: false,
      detail: null
    };
  },
  created() {
    this.fetchPending();
  },
  methods: {
    handleTab() {
      if (this.tab === "pending") {
        this.fetchPending();
      } else {
        this.fetchSubmitted();
      }
    },
    async fetchPending() {
      this.loading = true;
      try {
        const resp = await listFollowupTasks({ status: "PENDING" });
        if (resp.code === 0) this.pending = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    async fetchSubmitted() {
      this.loadingSubmitted = true;
      try {
        const resp = await listFollowupTasks({ status: "SUBMITTED" });
        if (resp.code === 0) this.submitted = resp.data || [];
      } finally {
        this.loadingSubmitted = false;
      }
    },
    onFiles(files) {
      this.files = files;
    },
    parsedAnswer(answer) {
      if (!answer) return {};
      try {
        return typeof answer === "string" ? JSON.parse(answer) : answer;
      } catch (e) {
        return { answer };
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
    },
    async openSubmit(row) {
      const resp = await getFollowupTask(row.id);
      if (resp.code === 0) {
        this.current = row;
        this.template = this.parseTemplate(resp.data && resp.data.questionnaire_template);
        this.answers = {};
        this.showSubmit = true;
      }
    },
    async openDetail(row) {
      const resp = await getFollowupTask(row.id);
      if (resp.code === 0) {
        this.detail = resp.data || null;
        this.showDetail = true;
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
    async submit() {
      if (!this.current) return;
      this.saving = true;
      try {
        const attachments = await this.uploadAll();
        const resp = await submitFollowupTask(this.current.id, {
          questionnaire: JSON.stringify(this.answers),
          attachments
        });
        if (resp.code === 0) {
          this.$message.success("回访问卷已提交");
          this.showSubmit = false;
          this.fetchPending();
          this.fetchSubmitted();
        }
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>

<style scoped>
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
