<template>
  <el-card>
    <div slot="header">回访管理</div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="animal" label="动物" />
      <el-table-column prop="due" label="回访期限" width="160" />
      <el-table-column prop="status" label="状态" width="120" />
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
        <el-button type="primary" @click="submit">提交</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import Uploader from "@/components/Uploader.vue";

export default {
  name: "Followups",
  components: { Uploader },
  data() {
    return {
      list: [{ id: 1, animal: "小黄", due: "2026-03-05", status: "待提交" }],
      show: false,
      form: { answer: "" },
      files: []
    };
  },
  methods: {
    onFiles(files) {
      this.files = files;
    },
    open() {
      this.show = true;
    },
    submit() {
      this.$message.success("回访已提交（演示）");
      this.show = false;
    }
  }
};
</script>
