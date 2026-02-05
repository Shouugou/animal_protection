<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="24">
        <el-card>
          <div slot="header">事件上报</div>
          <el-form :model="form" label-width="90px">
            <el-form-item label="事件类型">
              <el-select v-model="form.event_type">
                <el-option label="流浪动物" value="流浪动物" />
                <el-option label="受伤动物" value="受伤动物" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item label="紧急程度">
              <el-select v-model="form.urgency">
                <el-option label="低" value="低" />
                <el-option label="中" value="中" />
                <el-option label="高" value="高" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input type="textarea" v-model="form.description" rows="4" />
            </el-form-item>
          </el-form>
        </el-card>
        <MapPicker @pick="onPick" />
        <Uploader title="上传图片/视频" @change="onFiles" />
      </el-col>
    </el-row>
    <el-card v-if="files.length > 0" style="margin-top:12px">
      <div slot="header">上传预览</div>
      <div style="display:flex;flex-wrap:wrap;gap:12px">
        <el-image
          v-for="(url, idx) in imageUrls"
          :key="`img-${idx}`"
          :src="url"
          style="width:140px;height:100px"
          fit="cover"
        />
        <video
          v-for="(url, idx) in videoUrls"
          :key="`vid-${idx}`"
          :src="url"
          style="width:180px;height:120px"
          controls
        />
      </div>
    </el-card>
    <el-button type="primary" :loading="loading" style="margin-top:12px" @click="submit">
      提交上报
    </el-button>
  </div>
</template>

<script>
import MapPicker from "@/components/MapPicker.vue";
import Uploader from "@/components/Uploader.vue";
import { createEvent, uploadFile } from "@/api";

export default {
  name: "PublicEventNew",
  components: { MapPicker, Uploader },
  data() {
    return {
      form: {
        event_type: "受伤动物",
        urgency: "中",
        description: "",
        latitude: "",
        longitude: "",
        address: ""
      },
      files: [],
      loading: false
    };
  },
  computed: {
    imageUrls() {
      return this.previewUrls.filter((u) => !this.isVideo(u));
    },
    videoUrls() {
      return this.previewUrls.filter((u) => this.isVideo(u));
    },
    previewUrls() {
      return this.files.map((f) => URL.createObjectURL(f));
    }
  },
  methods: {
    isVideo(url) {
      return /\.(mp4|webm|ogg|mov)$/i.test(url || "");
    },
    onPick(payload) {
      this.form = { ...this.form, ...payload };
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
    async submit() {
      if (!this.form.description) {
        this.$message.warning("请填写描述");
        return;
      }
      this.loading = true;
      try {
        const attachments = await this.uploadAll();
        const resp = await createEvent({
          eventType: this.form.event_type,
          urgency: this.form.urgency,
          description: this.form.description,
          address: this.form.address,
          latitude: this.form.latitude,
          longitude: this.form.longitude,
          attachments
        });
        if (resp.code === 0) {
          this.$message.success("已提交");
          this.$router.push(`/public/events/${resp.data.id}`);
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
