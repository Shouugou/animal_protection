<template>
  <el-card>
    <div slot="header" class="section-title" style="width:100%">
      <span>任务大厅</span>
      <div style="margin-left:auto">
        <el-tag v-if="isVolunteer" size="mini" type="success">志愿者</el-tag>
        <el-button v-else size="mini" type="primary" @click="doRegister">注册志愿者</el-button>
      </div>
    </div>
    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="任务" />
      <el-table-column label="类型" width="140">
        <template slot-scope="scope">
          {{ taskTypeText(scope.row.task_type) }}
        </template>
      </el-table-column>
      <el-table-column label="可认领" width="120">
        <template slot-scope="scope">
          {{ claimCount(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" :disabled="!canClaim(scope.row)" @click="claim(scope.row)">认领</el-button>
          <el-button size="mini" @click="openDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog title="任务详情" :visible.sync="detailDialog.visible" width="520px">
      <el-descriptions :column="2" v-if="detail.id">
        <el-descriptions-item label="任务标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ taskTypeText(detail.task_type) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="认领人数">{{ claimCount(detail) }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address || "-" }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.start_at || "-" }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.end_at || "-" }}</el-descriptions-item>
      </el-descriptions>
      <MapViewer :latitude="detail.latitude" :longitude="detail.longitude" :height="200" />
      <div style="margin-top:10px" v-if="detail.description">
        {{ detail.description }}
      </div>
    </el-dialog>
  </el-card>
</template>

<script>
import { listTasks, claimTask, registerVolunteer } from "@/api";
import MapViewer from "@/components/MapViewer.vue";

export default {
  name: "PublicTasks",
  components: { MapViewer },
  data() {
    return {
      list: [],
      loading: false,
      detailDialog: { visible: false },
      detail: {}
    };
  },
  computed: {
    isVolunteer() {
      return Number(this.$store.state.auth.profile.is_volunteer || 0) === 1;
    }
  },
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      return {
        OPEN: "可认领",
        CLOSED: "已满员",
        CANCELLED: "已取消"
      }[status] || status || "未知";
    },
    taskTypeText(type) {
      return {
        PATROL: "巡护任务",
        RESCUE_SUPPORT: "救助协助"
      }[type] || type || "未知";
    },
    claimCount(row) {
      const max = Number(row.max_claims || 0);
      const claimed = Number(row.claimed_count || 0);
      if (max === 0) {
        return `${claimed}/不限`;
      }
      return `${claimed}/${max}`;
    },
    canClaim(row) {
      return this.isVolunteer && row.can_claim;
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = await listTasks();
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    async doRegister() {
      const resp = await registerVolunteer();
      if (resp.code === 0) {
        this.$message.success("已注册志愿者");
        this.$store.dispatch("auth/updateProfile", { is_volunteer: 1 });
      } else {
        this.$message.error(resp.message || "注册失败");
      }
    },
    async claim(item) {
      const resp = await claimTask(item.id);
      if (resp.code === 0) {
        const claimId = resp.data;
        this.$message.success("认领成功");
        if (item.task_type === "PATROL") {
          this.$router.push(`/public/patrol/submit/${claimId}`);
        } else {
          this.fetch();
        }
      } else {
        this.$message.error(resp.message || "认领失败");
      }
    },
    openDetail(row) {
      this.detail = row || {};
      this.detailDialog.visible = true;
    }
  }
};
</script>
