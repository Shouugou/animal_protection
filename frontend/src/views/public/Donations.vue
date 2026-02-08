<template>
  <el-card>
    <div slot="header">公益捐助</div>
    <el-tabs v-model="tab" @tab-click="handleTab">
      <el-tab-pane label="发起捐赠" name="form">
        <el-form label-width="90px" style="max-width:520px">
          <el-form-item label="目标类型">
            <el-radio-group v-model="form.target_type" @change="onTypeChange">
              <el-radio-button label="EVENT">事件</el-radio-button>
              <el-radio-button label="ORG">机构</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="捐助目标">
            <el-select v-if="form.target_type === 'EVENT'" v-model="form.target_id" filterable placeholder="请选择事件">
              <el-option v-for="e in events" :key="e.id" :label="eventLabel(e)" :value="e.id" />
            </el-select>
            <el-select v-else v-model="form.target_id" filterable placeholder="请选择机构">
              <el-option v-for="o in orgs" :key="o.id" :label="orgLabel(o)" :value="o.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="金额">
            <el-input-number v-model="form.amount" :min="1" :step="10" controls-position="right" />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="form.anonymous">匿名捐赠</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="donateSubmit">确认捐赠</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="我的捐赠记录" name="mine">
        <el-table :data="myList" v-loading="loadingMine" style="width:100%">
          <el-table-column prop="id" label="记录ID" width="90" />
          <el-table-column label="捐赠目标">
            <template slot-scope="scope">
              <span v-if="scope.row.target_type === 'EVENT'">
                事件 #{{ scope.row.target_id }} {{ scope.row.event_type || "" }} {{ scope.row.address || "" }}
              </span>
              <span v-else>
                机构 {{ scope.row.org_name || "" }}（{{ orgTypeText(scope.row.org_type) }}）
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120" />
          <el-table-column prop="donated_at" label="时间" width="180" />
          <el-table-column prop="status" label="状态" width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script>
import { donate, listDonationEvents, listDonationOrganizations, listMyDonations } from "@/api";

export default {
  name: "Donations",
  data() {
    return {
      tab: "form",
      form: { target_type: "EVENT", target_id: null, amount: 10, anonymous: false },
      loading: false,
      loadingMine: false,
      myList: [],
      events: [],
      orgs: []
    };
  },
  created() {
    this.loadEvents();
  },
  methods: {
    eventLabel(e) {
      return `#${e.id} ${e.event_type || "事件"} ${e.address || ""}`.trim();
    },
    orgLabel(o) {
      return `${o.name}（${this.orgTypeText(o.org_type)}）`;
    },
    orgTypeText(type) {
      return {
        LAW: "执法部门",
        RESCUE: "救助机构",
        PLATFORM: "平台"
      }[type] || type || "机构";
    },
    onTypeChange() {
      this.form.target_id = null;
      if (this.form.target_type === "EVENT") {
        this.loadEvents();
      } else {
        this.loadOrgs();
      }
    },
    handleTab() {
      if (this.tab === "mine") {
        this.loadMyDonations();
      }
    },
    async loadEvents() {
      const resp = await listDonationEvents();
      if (resp.code === 0) this.events = resp.data || [];
    },
    async loadOrgs() {
      const resp = await listDonationOrganizations();
      if (resp.code === 0) this.orgs = resp.data || [];
    },
    async loadMyDonations() {
      this.loadingMine = true;
      try {
        const resp = await listMyDonations();
        if (resp.code === 0) this.myList = resp.data || [];
      } finally {
        this.loadingMine = false;
      }
    },
    async donateSubmit() {
      if (!this.form.target_id) {
        this.$message.warning("请选择捐助目标");
        return;
      }
      if (!this.form.amount || this.form.amount <= 0) {
        this.$message.warning("请输入有效金额");
        return;
      }
      this.loading = true;
      try {
        const resp = await donate({
          targetType: this.form.target_type,
          targetId: Number(this.form.target_id),
          amount: Number(this.form.amount),
          anonymous: this.form.anonymous
        });
        if (resp.code === 0) {
          this.$message.success("捐赠已提交，感谢您的支持");
          this.form.target_id = null;
          this.loadMyDonations();
        }
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
