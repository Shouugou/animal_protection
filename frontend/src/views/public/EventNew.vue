<template>
  <div>
    <el-card>
      <div slot="header">事件上报</div>
      <el-form :model="form" label-width="90px" style="max-width:520px">
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
    <el-button type="primary" @click="submit">提交上报</el-button>
  </div>
</template>

<script>
import MapPicker from "@/components/MapPicker.vue";
import Uploader from "@/components/Uploader.vue";

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
      files: []
    };
  },
  methods: {
    onPick(payload) {
      this.form = { ...this.form, ...payload };
    },
    onFiles(files) {
      this.files = files;
    },
    submit() {
      this.$message.success("已提交（演示）");
      this.$router.push("/public/events");
    }
  }
};
</script>
