<template>
  <el-card>
    <div slot="header">权限配置</div>
    <el-table :data="roles" style="width:100%">
      <el-table-column prop="name" label="角色" width="140" />
      <el-table-column label="权限点">
        <template slot-scope="scope">
          <el-checkbox-group v-model="scope.row.perms">
            <el-checkbox label="VIEW" />
            <el-checkbox label="EDIT" />
            <el-checkbox label="EXPORT" />
          </el-checkbox-group>
        </template>
      </el-table-column>
    </el-table>
    <el-button type="primary" style="margin-top:12px">保存配置</el-button>
  </el-card>
</template>

<script>
export default {
  name: "AdminAcl",
  data() {
    return {
      roles: [
        { name: "公众用户", perms: ["VIEW"] },
        { name: "执法部门", perms: ["VIEW", "EDIT"] },
        { name: "救助医疗机构", perms: ["VIEW", "EDIT"] },
        { name: "系统管理员", perms: ["VIEW", "EDIT", "EXPORT"] }
      ]
    };
  }
};
</script>
