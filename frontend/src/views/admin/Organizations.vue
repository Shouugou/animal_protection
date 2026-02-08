<template>
  <div class="panel">
    <div class="panel-header">
      <h3>机构管理</h3>
      <el-button type="primary" size="mini" @click="openCreate">新增机构</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="org_type" label="类型" width="120" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="region_code" label="区域编码" width="120" />
      <el-table-column prop="contact_name" label="联系人" width="120" />
      <el-table-column prop="contact_phone" label="联系电话" width="140" />
      <el-table-column prop="admin_name" label="管理员" width="140" />
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

    <el-dialog :title="editingId ? '编辑机构' : '新增机构'" :visible.sync="dialogVisible" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="机构类型">
          <el-select v-model="form.org_type">
            <el-option label="执法部门" value="LAW" />
            <el-option label="救助机构" value="RESCUE" />
            <el-option label="平台方" value="PLATFORM" />
          </el-select>
        </el-form-item>
        <el-form-item label="机构名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="区域编码">
          <el-input v-model="form.region_code" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact_name" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contact_phone" />
        </el-form-item>
        <el-form-item label="机构管理员">
          <el-select
            v-model="form.admin_user_id"
            clearable
            filterable
            placeholder="选择管理员账号"
          >
            <el-option
              v-for="user in adminCandidates"
              :key="user.id"
              :label="`${user.nickname || user.phone} (${user.phone})`"
              :value="user.id"
            />
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
  createOrganization,
  updateOrganization,
  deleteOrganization,
  listAdminUsers
} from "@/api";

export default {
  name: "AdminOrganizations",
  data() {
    return {
      list: [],
      loading: false,
      dialogVisible: false,
      editingId: null,
      adminCandidates: [],
      form: {
        id: null,
        org_type: "LAW",
        name: "",
        region_code: "",
        address: "",
        contact_name: "",
        contact_phone: "",
        admin_user_id: "",
        status: 1
      }
    };
  },
  watch: {
    "form.org_type"() {
      this.loadAdminCandidates();
    }
  },
  created() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      this.loading = true;
      try {
        const resp = await listOrganizations();
        this.list = resp.data || [];
      } finally {
        this.loading = false;
      }
    },
    openCreate() {
      this.editingId = null;
      this.form = {
        id: null,
        org_type: "LAW",
        name: "",
        region_code: "",
        address: "",
        contact_name: "",
        contact_phone: "",
        admin_user_id: "",
        status: 1
      };
      this.loadAdminCandidates();
      this.dialogVisible = true;
    },
    openEdit(row) {
      this.editingId = row.id;
      this.form = {
        id: row.id,
        org_type: row.org_type,
        name: row.name,
        region_code: row.region_code,
        address: row.address,
        contact_name: row.contact_name,
        contact_phone: row.contact_phone,
        admin_user_id: row.admin_user_id,
        status: row.status
      };
      this.loadAdminCandidates(row.id);
      this.dialogVisible = true;
    },
    async submit() {
      const payload = {
        orgType: this.form.org_type,
        name: this.form.name,
        regionCode: this.form.region_code,
        address: this.form.address,
        contactName: this.form.contact_name,
        contactPhone: this.form.contact_phone,
        adminUserId: this.form.admin_user_id || null,
        status: this.form.status
      };
      if (this.editingId) {
        await updateOrganization(this.editingId, payload);
      } else {
        await createOrganization(payload);
      }
      this.dialogVisible = false;
      this.fetchList();
    },
    async remove(row) {
      await deleteOrganization(row.id);
      this.fetchList();
    },
    async loadAdminCandidates(orgIdOverride) {
      const roleMap = { LAW: "LAW", RESCUE: "RESCUE", PLATFORM: "ADMIN" };
      const roleCode = roleMap[this.form.org_type] || "";
      const orgId = orgIdOverride || this.form.id || null;
      if (!orgId) {
        this.adminCandidates = [];
        return;
      }
      const resp = await listAdminUsers({
        roleCode: roleCode || undefined,
        orgId: orgId || undefined
      });
      if (resp.code === 0) {
        this.adminCandidates = resp.data || [];
      } else {
        this.adminCandidates = [];
      }
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
