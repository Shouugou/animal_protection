<template>
  <el-card>
    <div slot="header">{{ title }}</div>
    <el-upload
      action="/api/upload"
      :file-list="fileList"
      :auto-upload="false"
      multiple
      :on-change="onChange"
      :on-remove="onRemove"
    >
      <el-button size="small" type="primary">选择文件</el-button>
      <div slot="tip" class="el-upload__tip">支持图片/视频，单次选择多个</div>
    </el-upload>
  </el-card>
</template>

<script>
export default {
  name: "Uploader",
  props: {
    title: { type: String, default: "附件上传" }
  },
  data() {
    return {
      fileList: []
    };
  },
  methods: {
    onChange(file, fileList) {
      this.fileList = fileList;
      this.$emit("change", fileList.map((f) => f.raw || f));
    },
    onRemove(file, fileList) {
      this.fileList = fileList;
      this.$emit("change", fileList.map((f) => f.raw || f));
    }
  }
};
</script>
