<template>
  <div class="layout">
    <aside class="sidebar">
      <h2>城市动保协同平台</h2>
      <div class="menu">
        <router-link v-for="item in menus" :key="item.path" :to="item.path">
          {{ item.label }}
        </router-link>
      </div>
    </aside>
    <div class="main">
      <header class="header">
        <div>
          <strong>{{ roleLabel }}</strong>
          <el-tag size="mini" style="margin-left:8px">{{ userName }}</el-tag>
        </div>
        <div>
          <el-link type="primary" @click="toMessages">消息中心</el-link>
          <el-button size="mini" style="margin-left:12px" @click="logout">退出</el-button>
        </div>
      </header>
      <div class="page">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";

export default {
  name: "Layout",
  computed: {
    ...mapGetters(["roleCode"]),
    roleLabel() {
      return {
        PUBLIC: "公众用户端",
        LAW: "执法部门端",
        RESCUE: "救助医疗机构端",
        ADMIN: "系统管理员端"
      }[this.roleCode] || "平台";
    },
    userName() {
      return this.$store.state.auth.profile.name || "用户";
    },
    menus() {
      const common = [{ label: "消息中心", path: "/messages" }];
      const roleMenus = {
        PUBLIC: [
          { label: "事件列表", path: "/public/events" },
          { label: "事件上报", path: "/public/events/new" },
          { label: "任务大厅", path: "/public/tasks" },
          { label: "我的任务", path: "/public/my-tasks" },
          { label: "动物领养", path: "/public/adoption" },
          { label: "回访管理", path: "/public/followups" },
          { label: "公益捐助", path: "/public/donations" },
          { label: "动保课堂", path: "/public/classroom" }
        ],
        LAW: [
          { label: "工单列表", path: "/law/workorders" },
          { label: "我的任务", path: "/law/my-tasks" },
          { label: "志愿任务", path: "/law/volunteer-tasks" },
          { label: "归档案件", path: "/law/archives" }
        ],
        RESCUE: [
          { label: "救助任务", path: "/rescue/tasks" },
          { label: "志愿任务", path: "/rescue/volunteer-tasks" },
          { label: "动物档案", path: "/rescue/animals" },
          { label: "治疗记录", path: "/rescue/animals/1/records" },
          { label: "库存管理", path: "/rescue/inventory" }
        ],
        ADMIN: [
          { label: "数据监控", path: "/admin/dashboard" },
          { label: "统计报表", path: "/admin/reports" },
          { label: "权限配置", path: "/admin/acl" },
          { label: "审核管理", path: "/admin/approvals" }
        ]
      };
      return [...roleMenus[this.roleCode] || [], ...common];
    }
  },
  methods: {
    logout() {
      this.$store.dispatch("auth/logout");
      this.$router.push("/login");
    },
    toMessages() {
      this.$router.push("/messages");
    }
  }
};
</script>
