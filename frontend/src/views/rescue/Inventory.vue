<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>库存管理</span>
      <el-button size="mini" type="primary" @click="show = true">新增品项</el-button>
    </div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="name" label="品项" />
      <el-table-column prop="qty" label="库存" width="120" />
      <el-table-column prop="threshold" label="阈值" width="120" />
    </el-table>

    <el-dialog title="新增品项" :visible.sync="show" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="阈值">
          <el-input v-model="form.threshold" />
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
  name: "Inventory",
  data() {
    return {
      list: [{ name: "药品A", qty: 12, threshold: 5 }],
      show: false,
      form: { name: "", threshold: "" }
    };
  },
  methods: {
    save() {
      this.$message.success("已保存（演示）");
      this.show = false;
    }
  }
};
</script>
