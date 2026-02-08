<template>
  <div>
    <el-card>
      <div slot="header">救助协助上报（认领ID: {{ claimId }}）</div>
      <el-form label-width="90px">
        <el-form-item label="协助详情">
          <el-input type="textarea" v-model="description" rows="3" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div slot="header" class="section-title">
        <span>现场定位</span>
      </div>
      <MapPicker @pick="onPick" />
    </el-card>

    <el-card>
      <div slot="header">现场照片</div>
      <Uploader title="现场照片" @change="onFiles" />
      <div v-if="attachmentUrls.length > 0" style="margin-top:10px;display:flex;flex-wrap:wrap;gap:10px">
        <el-image v-for="(url, idx) in attachmentUrls" :key="idx" :src="url" style="width:120px;height:90px" fit="cover" />
      </div>
    </el-card>

    <div style="margin-top:12px">
      <el-button @click="manualSaveDraft">暂存</el-button>
      <el-button type="primary" :loading="loading" @click="submit">提交完成</el-button>
    </div>
  </div>
</template>

<script>
import MapPicker from "@/components/MapPicker.vue";
import Uploader from "@/components/Uploader.vue";
import { submitRescueSupportReport, uploadFile } from "@/api";

export default {
  name: "RescueSupportSubmit",
  components: { MapPicker, Uploader },
  data() {
    return {
      claimId: Number(this.$route.params.claimId),
      description: "",
      address: "",
      latitude: "",
      longitude: "",
      files: [],
      attachmentUrls: [],
      loading: false,
      uploading: false,
      draftTimer: null
    };
  },
  mounted() {
    this.loadDraft();
  },
  watch: {
    description() {
      this.queueDraftSave();
    },
    address() {
      this.queueDraftSave();
    },
    latitude() {
      this.queueDraftSave();
    },
    longitude() {
      this.queueDraftSave();
    }
  },
  beforeDestroy() {
    this.saveDraft();
    if (this.draftTimer) {
      clearTimeout(this.draftTimer);
    }
  },
  methods: {
    onPick(payload) {
      this.address = payload.address || "";
      this.latitude = payload.latitude || "";
      this.longitude = payload.longitude || "";
    },
    onFiles(files) {
      this.files = files || [];
      if (this.files.length > 0) {
        this.uploadAttachments();
      }
    },
    async uploadAttachments() {
      this.uploading = true;
      const urls = [];
      for (const f of this.files) {
        const resp = await uploadFile(f);
        if (resp.code === 0 && resp.data && resp.data.file_url) {
          urls.push(resp.data.file_url);
        }
      }
      this.files = [];
      this.attachmentUrls = [...this.attachmentUrls, ...urls];
      this.uploading = false;
    },
    saveDraft() {
      const payload = {
        description: this.description,
        address: this.address,
        latitude: this.latitude,
        longitude: this.longitude,
        attachmentUrls: this.attachmentUrls
      };
      localStorage.setItem(`ap_rescue_support_draft_${this.claimId}`, JSON.stringify(payload));
    },
    queueDraftSave() {
      if (this.draftTimer) {
        clearTimeout(this.draftTimer);
      }
      this.draftTimer = setTimeout(() => {
        this.saveDraft();
      }, 400);
    },
    manualSaveDraft() {
      this.saveDraft();
      this.$message.success("已暂存");
    },
    loadDraft() {
      const raw = localStorage.getItem(`ap_rescue_support_draft_${this.claimId}`);
      if (!raw) return;
      try {
        const payload = JSON.parse(raw);
        this.description = payload.description || "";
        this.address = payload.address || "";
        this.latitude = payload.latitude || "";
        this.longitude = payload.longitude || "";
        this.attachmentUrls = payload.attachmentUrls || [];
      } catch (e) {
        localStorage.removeItem(`ap_rescue_support_draft_${this.claimId}`);
      }
    },
    async submit() {
      if (!this.address || !this.latitude || !this.longitude) {
        this.$message.warning("请先完成定位");
        return;
      }
      if (!this.description) {
        this.$message.warning("请填写协助详情");
        return;
      }
      this.loading = true;
      try {
        if (this.uploading) {
          this.$message.warning("附件上传中，请稍候");
          return;
        }
        const resp = await submitRescueSupportReport({
          claimId: this.claimId,
          description: this.description,
          address: this.address,
          latitude: Number(this.latitude),
          longitude: Number(this.longitude),
          attachments: this.attachmentUrls
        });
        if (resp.code === 0) {
          this.$message.success("已提交");
          localStorage.removeItem(`ap_rescue_support_draft_${this.claimId}`);
          this.$router.push("/public/my-tasks");
        } else {
          this.$message.error(resp.message || "提交失败");
        }
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
