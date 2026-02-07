<template>
  <div>
    <el-card>
      <div slot="header" class="section-title">
        <div>
          <div style="font-weight:600">事件列表</div>
          <div style="font-size:12px;color:#6b7280">查看自己上报的事件与处理进度</div>
        </div>
        <el-button type="primary" size="mini" @click="$router.push('/public/events/new')">上报事件</el-button>
      </div>
      <el-form inline style="background:#f8fafc;padding:10px 12px;border-radius:8px">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="类型/描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="status" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="已上报" value="REPORTED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已处置" value="RESOLVED" />
            <el-option label="已闭环" value="CLOSED" />
            <el-option label="不予受理" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" style="width:100%;margin-top:12px" stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="event_type" label="类型" />
        <el-table-column label="状态" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusType(scope.row.status)">
              {{ statusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="240">
          <template slot-scope="scope">
            {{ formatTime(scope.row.reported_at || scope.row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-link type="primary" @click="open(scope.row)">查看</el-link>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:12px;text-align:right">
        <el-pagination
          background
          layout="prev, pager, next"
          :page-size="pageSize"
          :current-page.sync="pageNo"
          :total="total"
          @current-change="fetch"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { listEvents } from "@/api";

export default {
  name: "PublicEvents",
  data() {
    return {
      keyword: "",
      status: "",
      list: [],
      loading: false,
      pageNo: 1,
      pageSize: 10,
      total: 0
    };
  },
  created() {
    this.fetch();
  },
  methods: {
    statusText(status) {
      const map = {
        REPORTED: "已上报",
        TRIAGED: "已分流",
        DISPATCHED: "已派单",
        PROCESSING: "处理中",
        PENDING_SUPPLEMENT: "待补充",
        RESOLVED: "已处置",
        CLOSED: "已办结",
        REJECTED: "不予受理",
        DUPLICATE: "重复事件"
      };
      return map[status] || status || "未知";
    },
    statusType(status) {
      const map = {
        REPORTED: "info",
        TRIAGED: "warning",
        DISPATCHED: "warning",
        PROCESSING: "warning",
        PENDING_SUPPLEMENT: "warning",
        RESOLVED: "success",
        CLOSED: "success",
        REJECTED: "danger",
        DUPLICATE: "danger"
      };
      return map[status] || "info";
    },
    formatTime(value) {
      if (!value) return "";
      let d = null;
      if (typeof value === "number") {
        d = new Date(value);
      } else if (typeof value === "string") {
        const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/);
        if (match) {
          d = new Date(
            Number(match[1]),
            Number(match[2]) - 1,
            Number(match[3]),
            Number(match[4]),
            Number(match[5]),
            Number(match[6])
          );
        } else {
          d = new Date(value);
        }
      } else if (value instanceof Date) {
        d = value;
      }
      if (!d || Number.isNaN(d.getTime())) return "";
      const pad = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日${pad(d.getHours())}时${pad(d.getMinutes())}分${pad(d.getSeconds())}秒`;
    },
    async fetch() {
      this.loading = true;
      try {
        const resp = await listEvents({
          keyword: this.keyword,
          status: this.status,
          pageNo: this.pageNo,
          pageSize: this.pageSize
        });
        if (resp.code === 0) {
          this.list = resp.data.records || [];
          this.total = resp.data.total || 0;
        }
      } finally {
        this.loading = false;
      }
    },
    search() {
      this.pageNo = 1;
      this.fetch();
    },
    open(row) {
      this.$router.push(`/public/events/${row.id}`);
    }
  }
};
</script>
