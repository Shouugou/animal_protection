<template>
  <div class="panel">
    <div class="panel-header">
      <h3>员工管理</h3>
      <el-button v-if="isOrgAdmin" type="primary" size="mini" @click="openCreate">新增员工</el-button>
    </div>
    <el-alert
      v-if="!isOrgAdmin"
      title="仅机构管理员可管理员工"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom:12px"
    />
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="phone" label="账号" width="140" />
      <el-table-column prop="nickname" label="姓名" min-width="160" />
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? "启用" : "停用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="180" />
      <el-table-column label="操作" width="220">
        <template slot-scope="scope">
          <el-button size="mini" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="editingId ? '编辑员工' : '新增员工'" :visible.sync="dialogVisible" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="账号">
          <el-input v-model="form.phone" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="选择状态">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button size="mini" @click="dialogVisible = false">取消</el-button>
        <el-button size="mini" type="primary" @click="submit">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listLawEmployees,
  createLawEmployee,
  updateLawEmployee,
  deleteLawEmployee
} from "@/api";

export default {
  name: "LawEmployees",
  data() {
    return {
      list: [],
      loading: false,
      dialogVisible: false,
      editingId: null,
      form: {
        phone: "",
        nickname: "",
        password: "",
        status: 1
      }
    };
  },
  computed: {
    isOrgAdmin() {
      return !!this.$store.state.auth.profile.is_org_admin;
    }
  },
  created() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      this.loading = true;
      try {
        const resp = await listLawEmployees();
        this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    openCreate() {
      this.editingId = null;
      this.form = { phone: "", nickname: "", password: "", status: 1 };
      this.dialogVisible = true;
    },
    openEdit(row) {
      this.editingId = row.id;
      this.form = {
        phone: row.phone,
        nickname: row.nickname,
        password: "",
        status: row.status
      };
      this.dialogVisible = true;
    },
    async submit() {
      if (this.editingId) {
        await updateLawEmployee(this.editingId, {
          nickname: this.form.nickname,
          password: this.form.password,
          status: this.form.status
        });
      } else {
        await createLawEmployee({
          phone: this.form.phone,
          nickname: this.form.nickname,
          password: this.form.password,
          status: this.form.status
        });
      }
      this.dialogVisible = false;
      this.fetchList();
    },
    async remove(row) {
      await deleteLawEmployee(row.id);
      this.fetchList();
    }
  }
};
</script>

<style scoped>
.panel {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
</style>
