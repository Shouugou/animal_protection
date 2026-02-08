<template>
  <el-card>
    <div slot="header">动物领养</div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="species" label="物种" />
      <el-table-column label="状态" width="180">
        <template slot-scope="scope">
          {{ statusText(scope.row.status) }}
        </template>
      </el-table-column>
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
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import { listAnimals, createAdoption } from "@/api";

export default {
  name: "Adoption",
  data() {
    return {
      list: [],
      loading: false,
      show: false,
      saving: false,
      current: null,
      form: { name: "", address: "", exp: "" }
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      return {
        APPLIED: "已申请",
        APPROVED: "已通过",
        REJECTED: "已拒绝",
        CANCELLED: "已取消"
      }[status] || status || "未知";
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = await listAnimals();
        if (resp.code === 0) this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    open(row) {
      this.current = row;
      this.show = true;
    },
    async submit() {
      if (!this.current) return;
      this.saving = true;
      try {
        const resp = await createAdoption({
          animalId: this.current.id,
          applyForm: JSON.stringify(this.form)
        });
        if (resp.code === 0) {
          this.$message.success("已提交领养申请");
          this.show = false;
        }
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>
