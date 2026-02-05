<template>
  <div class="page">
    <el-card style="max-width:420px;margin:40px auto">
      <h3 style="margin-top:0">登录</h3>
      <el-form label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="phone" placeholder="手机号/账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="login">登录</el-button>
          <el-button type="text" @click="showRegister = true">注册账号</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog title="注册账号" :visible.sync="showRegister" width="420px">
      <el-form label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="regPhone" placeholder="手机号/账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="regPassword" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="regNickname" placeholder="可选" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="registerAccount">注册</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { register } from "@/api";

export default {
  name: "Login",
  data() {
    return {
      phone: "",
      password: "",
      loading: false,
      showRegister: false,
      regPhone: "",
      regPassword: "",
      regNickname: "",
      registering: false
    };
  },
  methods: {
    async login() {
      if (!this.phone || !this.password) {
        this.$message.warning("请输入账号和密码");
        return;
      }
      this.phone = this.phone.trim();
      this.password = this.password.trim();
      this.loading = true;
      try {
        await this.$store.dispatch("auth/login", { phone: this.phone, password: this.password });
        const role = this.$store.getters.roleCode;
        const map = {
          PUBLIC: "/public/events",
          LAW: "/law/workorders",
          RESCUE: "/rescue/tasks",
          ADMIN: "/admin/dashboard"
        };
        this.$router.push(map[role] || "/messages");
      } catch (e) {
        this.$message.error(e.message || "登录失败");
      } finally {
        this.loading = false;
      }
    },
    async registerAccount() {
      if (!this.regPhone || !this.regPassword) {
        this.$message.warning("请输入账号和密码");
        return;
      }
      this.registering = true;
      try {
        const resp = await register({
          phone: this.regPhone.trim(),
          password: this.regPassword.trim(),
          nickname: this.regNickname.trim()
        });
        if (resp.code === 0) {
          this.$message.success("注册成功，请登录");
          this.phone = this.regPhone.trim();
          this.password = this.regPassword.trim();
          this.showRegister = false;
          this.regPhone = "";
          this.regPassword = "";
          this.regNickname = "";
        } else {
          this.$message.error(resp.message || "注册失败");
        }
      } finally {
        this.registering = false;
      }
    }
  }
};
</script>
