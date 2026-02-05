<template>
  <el-card>
    <div slot="header">动物领养</div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="name" label="动物" />
      <el-table-column prop="status" label="状态" width="180" />
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="open(scope.row)">申请领养</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="领养申请" :visible.sync="show" width="520px">
      <el-form label-width="90px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="经验"><el-input type="textarea" v-model="form.exp" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
export default {
  name: "Adoption",
  data() {
    return {
      list: [{ id: 1, name: "小黄", status: "READY_FOR_ADOPTION" }],
      show: false,
      form: { name: "", address: "", exp: "" }
    };
  },
  methods: {
    open() {
      this.show = true;
    },
    submit() {
      this.$message.success("已提交领养申请（演示）");
      this.show = false;
    }
  }
};
</script>
