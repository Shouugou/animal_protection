<template>
  <div>
    <el-card>
      <div slot="header">工单详情 #{{ id }}</div>
      <el-tabs v-model="tab">
        <el-tab-pane label="受理" name="accept">
          <el-form label-width="110px">
            <el-form-item label="是否执法">
              <el-select v-model="detail.need_law_enforcement">
                <el-option label="是" :value="true" />
                <el-option label="否" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="是否转送救助">
              <el-select v-model="detail.transfer_to_rescue">
                <el-option label="是" :value="true" />
                <el-option label="否" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理结论">
              <el-input type="textarea" v-model="detail.result_text" rows="3" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="save">保存受理</el-button>
              <el-button v-if="detail.transfer_to_rescue" @click="transfer">转送救助</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="分派" name="assign">
          <el-form label-width="90px">
            <el-form-item label="处理人员">
              <el-select v-model="assignUser" placeholder="选择执法人员">
                <el-option label="执法员A" value="1001" />
                <el-option label="执法员B" value="1002" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="assign">确认分派</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="取证" name="evidence">
          <el-input type="textarea" v-model="evidenceNote" rows="3" placeholder="取证说明" />
          <Uploader title="取证附件" @change="onFiles" />
          <el-button type="primary" @click="addEvidence">提交取证</el-button>
        </el-tab-pane>

        <el-tab-pane label="结果录入" name="result">
          <el-input type="textarea" v-model="publicText" rows="3" placeholder="公示文本" />
          <el-button type="primary" @click="publish">公示</el-button>
        </el-tab-pane>

        <el-tab-pane label="归档" name="archive">
          <el-input v-model="archiveNo" placeholder="归档编号" />
          <el-button type="primary" style="margin-top:8px" @click="archive">归档</el-button>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import Uploader from "@/components/Uploader.vue";

export default {
  name: "LawWorkOrderDetail",
  components: { Uploader },
  data() {
    return {
      id: this.$route.params.id,
      tab: "accept",
      detail: {
        need_law_enforcement: true,
        transfer_to_rescue: false,
        result_text: ""
      },
      assignUser: "",
      evidenceNote: "",
      publicText: "",
      archiveNo: "",
      files: []
    };
  },
  methods: {
    onFiles(files) {
      this.files = files;
    },
    save() {
      this.$message.success("已保存（演示）");
    },
    transfer() {
      this.$message.success("已转送救助（演示）");
    },
    assign() {
      this.$message.success("分派成功（演示）");
    },
    addEvidence() {
      this.$message.success("取证已提交（演示）");
    },
    publish() {
      this.$message.success("已公示（演示）");
    },
    archive() {
      this.$message.success("已归档（演示）");
    }
  }
};
</script>
