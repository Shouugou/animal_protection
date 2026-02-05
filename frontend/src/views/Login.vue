<template>
  <div class="page">
    <el-card style="max-width:420px;margin:40px auto">
      <h3 style="margin-top:0">登录</h3>
      <el-form label-width="80px">
        <el-form-item label="角色">
          <el-select v-model="role">
            <el-option label="公众用户" value="PUBLIC" />
            <el-option label="执法部门" value="LAW" />
            <el-option label="救助医疗机构" value="RESCUE" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="name" placeholder="演示用户" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="login">进入系统</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "Login",
  data() {
    return {
      role: "PUBLIC",
      name: "演示用户"
    };
  },
  methods: {
    login() {
      this.$store.dispatch("auth/loginMock", { roleCode: this.role, name: this.name });
      const map = {
        PUBLIC: "/public/events",
        LAW: "/law/workorders",
        RESCUE: "/rescue/tasks",
        ADMIN: "/admin/dashboard"
      };
      this.$router.push(map[this.role] || "/messages");
    }
  }
};
</script>
