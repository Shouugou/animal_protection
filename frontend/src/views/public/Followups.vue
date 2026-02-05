<template>
  <el-card>
    <div slot="header">回访管理</div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="adoption_id" label="领养ID" width="120" />
      <el-table-column prop="due_at" label="回访期限" width="160" />
      <el-table-column prop="submitted_at" label="提交时间" width="160" />
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" @click="open(scope.row)">提交</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="提交回访" :visible.sync="show" width="520px">
      <el-input type="textarea" v-model="form.answer" rows="4" placeholder="回访问卷" />
      <Uploader title="回访附件" @change="onFiles" />
      <span slot="footer">
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import Uploader from "@/components/Uploader.vue";
import { listFollowups, submitFollowup, uploadFile } from "@/api";

export default {
  name: "Followups",
  components: { Uploader },
  data() {
    return {
      list: [],
      loading: false,
      show: false,
      saving: false,
      current: null,
      form: { answer: "" },
      files: []
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    async fetch() {
      this.loading = true;
      try {
        const resp = await listFollowups();
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
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
    open(row) {
      this.current = row;
      this.show = true;
    },
    async submit() {
      if (!this.current) return;
      this.saving = true;
      try {
        const attachments = await this.uploadAll();
        const resp = await submitFollowup({
          adoptionId: this.current.adoption_id,
          questionnaire: JSON.stringify({ answer: this.form.answer }),
          dueAt: this.current.due_at,
          attachments
        });
        if (resp.code === 0) {
          this.$message.success("回访已提交");
          this.show = false;
          this.fetch();
        }
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>
