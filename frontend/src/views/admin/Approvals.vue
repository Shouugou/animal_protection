<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>审核管理</span>
      <el-button size="mini" type="primary" @click="show = true">新建流程</el-button>
    </div>
    <el-table :data="flows" style="width:100%">
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="biz" label="业务类型" width="160" />
    </el-table>

    <el-dialog title="流程定义" :visible.sync="show" width="600px">
      <el-form label-width="90px">
        <el-form-item label="编码">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="form.biz" />
        </el-form-item>
        <el-form-item label="节点JSON">
          <el-input type="textarea" v-model="form.steps" rows="6" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
export default {
  name: "AdminApprovals",
  data() {
    return {
      flows: [{ code: "ADOPTION", name: "领养审核", biz: "ADOPTION" }],
      show: false,
      form: { code: "", name: "", biz: "", steps: "[]" }
    };
  },
  methods: {
    save() {
      this.$message.success("流程已保存（演示）");
      this.show = false;
    }
  }
};
</script>
