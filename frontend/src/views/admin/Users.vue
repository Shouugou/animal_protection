<template>
  <div class="panel">
    <div class="panel-header">
      <h3>账号管理</h3>
      <div class="actions">
        <el-select v-model="filters.roleCode" clearable placeholder="角色" size="mini" @change="fetchList">
          <el-option label="公众用户" value="PUBLIC" />
          <el-option label="执法部门" value="LAW" />
          <el-option label="救助机构" value="RESCUE" />
          <el-option label="系统管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="filters.orgId" clearable placeholder="机构" size="mini" @change="fetchList">
          <el-option v-for="org in orgs" :key="org.id" :label="org.name" :value="org.id" />
        </el-select>
        <el-input v-model="filters.keyword" size="mini" placeholder="账号/姓名" @keyup.enter.native="fetchList" />
        <el-button size="mini" @click="fetchList">筛选</el-button>
        <el-button type="primary" size="mini" @click="openCreate">新增账号</el-button>
      </div>
    </div>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="role_code" label="角色" width="120" />
      <el-table-column prop="org_name" label="机构" min-width="160" />
      <el-table-column prop="phone" label="账号" width="140" />
      <el-table-column prop="nickname" label="姓名" min-width="120" />
      <el-table-column prop="is_volunteer" label="志愿者" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.is_volunteer === 1 ? 'success' : 'info'">
            {{ scope.row.is_volunteer === 1 ? "是" : "否" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? "启用" : "停用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template slot-scope="scope">
          <el-button size="mini" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="editingId ? '编辑账号' : '新增账号'" :visible.sync="dialogVisible" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色">
          <el-select v-model="form.role_code">
            <el-option label="公众用户" value="PUBLIC" />
            <el-option label="执法部门" value="LAW" />
            <el-option label="救助机构" value="RESCUE" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="机构">
          <el-select v-model="form.org_id" clearable>
            <el-option v-for="org in filteredOrgs" :key="org.id" :label="org.name" :value="org.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="志愿者">
          <el-select v-model="form.is_volunteer">
            <el-option :value="0" label="否" />
            <el-option :value="1" label="是" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
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
  listOrganizations,
  listAdminUsers,
  createAdminUser,
  updateAdminUser,
  deleteAdminUser
} from "@/api";

export default {
  name: "AdminUsers",
  data() {
    return {
      list: [],
      loading: false,
      orgs: [],
      filters: {
        roleCode: "",
        orgId: null,
        keyword: ""
      },
      dialogVisible: false,
      editingId: null,
      form: {
        role_code: "PUBLIC",
        org_id: null,
        phone: "",
        nickname: "",
        password: "",
        is_volunteer: 0,
        status: 1
      }
    };
  },
  computed: {
    filteredOrgs() {
      const role = this.form.role_code;
      if (!role || role === "PUBLIC") return [];
      if (role === "ADMIN") {
        return this.orgs.filter((o) => o.org_type === "PLATFORM");
      }
      return this.orgs.filter((o) => o.org_type === role);
    }
  },
  watch: {
    "form.role_code"() {
      if (this.form.role_code === "PUBLIC") {
        this.form.org_id = null;
      } else if (!this.filteredOrgs.some((o) => o.id === this.form.org_id)) {
        this.form.org_id = null;
      }
    }
  },
  created() {
    this.fetchOrgs();
    this.fetchList();
  },
  methods: {
    async fetchOrgs() {
      const resp = await listOrganizations();
      this.orgs = resp.data || [];
    },
    async fetchList() {
      this.loading = true;
      try {
        const resp = await listAdminUsers({
          roleCode: this.filters.roleCode || undefined,
          orgId: this.filters.orgId || undefined,
          keyword: this.filters.keyword || undefined
        });
        this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    openCreate() {
      this.editingId = null;
      this.form = {
        role_code: "PUBLIC",
        org_id: null,
        phone: "",
        nickname: "",
        password: "",
        is_volunteer: 0,
        status: 1
      };
      this.dialogVisible = true;
    },
    openEdit(row) {
      this.editingId = row.id;
      this.form = {
        role_code: row.role_code,
        org_id: row.org_id,
        phone: row.phone,
        nickname: row.nickname,
        password: "",
        is_volunteer: row.is_volunteer,
        status: row.status
      };
      this.dialogVisible = true;
    },
    async submit() {
      const payload = {
        roleCode: this.form.role_code,
        orgId: this.form.role_code === "PUBLIC" ? null : this.form.org_id,
        phone: this.form.phone,
        password: this.form.password,
        nickname: this.form.nickname,
        isVolunteer: this.form.is_volunteer,
        status: this.form.status
      };
      if (this.editingId) {
        await updateAdminUser(this.editingId, payload);
      } else {
        await createAdminUser(payload);
      }
      this.dialogVisible = false;
      this.fetchList();
    },
    async remove(row) {
      await deleteAdminUser(row.id);
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
.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.actions .el-input {
  width: 180px;
}
</style>
