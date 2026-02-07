<template>
  <div>
    <el-card>
      <div slot="header" class="section-title">
        <span>库存管理</span>
        <el-button size="mini" type="primary" @click="showItem = true">新增品项</el-button>
      </div>
      <el-tabs v-model="activeTab" @tab-click="onTabChange">
        <el-tab-pane label="品项管理" name="items" />
        <el-tab-pane label="出入库记录" name="txns" />
        <el-tab-pane name="alerts">
          <span slot="label">
            <el-badge :value="alerts.length" :hidden="alerts.length === 0" class="tab-badge">
              <span>库存预警</span>
            </el-badge>
          </span>
        </el-tab-pane>
      </el-tabs>

      <div v-if="activeTab === 'txns'" style="margin-bottom:12px">
        <el-select v-model="currentItemId" placeholder="选择品项" @change="onItemSelect">
          <el-option v-for="i in items" :key="i.id" :label="i.item_name" :value="i.id" />
        </el-select>
        <el-button size="mini" style="margin-left:8px" @click="reloadCurrent">刷新</el-button>
      </div>

      <el-table v-if="activeTab === 'items'" :data="items" style="width:100%" v-loading="loading">
        <el-table-column prop="item_name" label="品名" />
        <el-table-column prop="production_date" label="生产日期" width="140" />
        <el-table-column prop="expiry_date" label="有效期" width="140" />
        <el-table-column label="当前库存" width="120">
          <template slot-scope="scope">
            <span :style="{ color: isLow(scope.row) ? '#dc2626' : '#111827' }">
              {{ Number(scope.row.stock_qty || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="low_stock_threshold" label="最低库存" width="120" />
        <el-table-column prop="warning_days" label="效期预警" width="120" />
        <el-table-column label="操作" width="420">
          <template slot-scope="scope">
            <div style="white-space:nowrap">
              <el-button size="mini" type="primary" style="margin-right:6px" @click="openTxn(scope.row, 'IN')">入库</el-button>
              <el-button size="mini" style="margin-right:6px" @click="openTxn(scope.row, 'OUT')">出库</el-button>
              <el-button size="mini" style="margin-right:6px" @click="openTxn(scope.row, 'RETURN')">退货</el-button>
              <el-button size="mini" style="margin-right:6px" @click="openTxn(scope.row, 'LOSS')">报损</el-button>
              <el-button size="mini" type="danger" @click="removeItem(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-if="activeTab === 'txns'" :data="txns" style="width:100%" v-loading="loading">
        <el-table-column prop="txn_no" label="单号" width="160" />
        <el-table-column prop="txn_type" label="类型" width="120" />
        <el-table-column prop="qty" label="数量" width="120" />
        <el-table-column prop="note" label="备注" />
        <el-table-column prop="created_at" label="时间" width="180" />
      </el-table>

      <el-table v-if="activeTab === 'alerts'" :data="alerts" style="width:100%" v-loading="loading">
        <el-table-column prop="item_name" label="品名" />
        <el-table-column prop="stock_qty" label="当前库存" width="120" />
        <el-table-column prop="low_stock_threshold" label="最低库存" width="120" />
        <el-table-column prop="warning_days" label="预警天数" width="120" />
        <el-table-column prop="expiry_date" label="有效期" width="140" />
      </el-table>
    </el-card>

    <el-dialog title="新增品项" :visible.sync="showItem" width="520px">
      <el-form label-width="100px">
        <el-form-item label="品名">
          <el-input v-model="itemForm.item_name" />
        </el-form-item>
        <el-form-item label="生产日期">
          <el-date-picker v-model="itemForm.production_date" type="date" value-format="yyyy-MM-dd" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="itemForm.expiry_date" type="date" value-format="yyyy-MM-dd" />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input v-model="itemForm.stock_qty" />
        </el-form-item>
        <el-form-item label="最低库存">
          <el-input v-model="itemForm.low_stock_threshold" />
        </el-form-item>
        <el-form-item label="效期预警">
          <el-input v-model="itemForm.warning_days" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showItem=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createItem">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="出入库/报损/退货" :visible.sync="showTxn" width="420px">
      <el-form label-width="100px">
        <el-form-item label="品项">
          <el-input v-model="txnForm.item_name" disabled />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="txnForm.txn_type">
            <el-option label="入库" value="IN" />
            <el-option label="出库" value="OUT" />
            <el-option label="退货" value="RETURN" />
            <el-option label="报损" value="LOSS" />
            <el-option label="调整" value="ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input v-model="txnForm.qty" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="txnForm.note" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showTxn=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTxn">提交</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listInventoryItems, createInventoryItem, listInventoryTxns, addInventoryTxn, listInventoryAlerts, deleteInventoryItem } from "@/api";

export default {
  name: "Inventory",
  data() {
    return {
      activeTab: "items",
      loading: false,
      saving: false,
      items: [],
      txns: [],
      alerts: [],
      currentItemId: "",
      showItem: false,
      showTxn: false,
      itemForm: {
        item_name: "",
        production_date: "",
        expiry_date: "",
        stock_qty: "",
        low_stock_threshold: "",
        warning_days: ""
      },
      txnForm: {
        item_id: "",
        item_name: "",
        txn_type: "OUT",
        qty: "",
        note: ""
      }
    };
  },
  created() {
    this.fetchItems();
    this.fetchAlerts();
  },
  methods: {
    isLow(row) {
      const qty = Number(row.stock_qty || 0);
      const threshold = Number(row.low_stock_threshold || 0);
      return threshold > 0 && qty < threshold;
    },
    async fetchItems() {
      this.loading = true;
      try {
        const resp = await listInventoryItems();
        if (resp.code === 0) {
          this.items = resp.data || [];
          if (!this.currentItemId && this.items.length > 0) {
            this.currentItemId = this.items[0].id;
          }
        }
      } finally {
        this.loading = false;
      }
    },
    async fetchTxns(itemId) {
      this.loading = true;
      try {
        const resp = await listInventoryTxns(itemId);
        if (resp.code === 0) {
          this.txns = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    async fetchAlerts() {
      this.loading = true;
      try {
        const resp = await listInventoryAlerts();
        if (resp.code === 0) {
          this.alerts = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    onTabChange() {
      if (this.activeTab === "items") {
        this.fetchItems();
      } else if (this.activeTab === "txns") {
        if (this.currentItemId) this.fetchTxns(this.currentItemId);
      } else if (this.activeTab === "alerts") {
        this.fetchAlerts();
      }
    },
    onItemSelect(val) {
      this.currentItemId = val;
      this.reloadCurrent();
    },
    reloadCurrent() {
      if (!this.currentItemId) return;
      if (this.activeTab === "txns") {
        this.fetchTxns(this.currentItemId);
      }
    },
    async createItem() {
      if (!this.itemForm.item_name) {
        this.$message.warning("请填写品名");
        return;
      }
      this.saving = true;
      try {
        const resp = await createInventoryItem({
          itemName: this.itemForm.item_name,
          productionDate: this.itemForm.production_date,
          expiryDate: this.itemForm.expiry_date,
          stockQty: this.itemForm.stock_qty ? Number(this.itemForm.stock_qty) : 0,
          lowStockThreshold: this.itemForm.low_stock_threshold ? Number(this.itemForm.low_stock_threshold) : null,
          warningDays: this.itemForm.warning_days ? Number(this.itemForm.warning_days) : null
        });
        if (resp.code === 0) {
          this.$message.success("已保存");
          this.showItem = false;
          this.itemForm = {
            item_name: "",
            production_date: "",
            expiry_date: "",
            stock_qty: "",
            low_stock_threshold: "",
            warning_days: ""
          };
          this.fetchItems();
        }
      } finally {
        this.saving = false;
      }
    },
    openTxn(row, type) {
      this.currentItemId = row.id;
      this.txnForm = {
        item_id: row.id,
        item_name: row.item_name,
        txn_type: type,
        qty: "",
        note: ""
      };
      this.activeTab = "txns";
      this.fetchTxns(row.id);
      this.showTxn = true;
    },
    async submitTxn() {
      if (!this.txnForm.item_id || !this.txnForm.qty) {
        this.$message.warning("请填写数量");
        return;
      }
      this.saving = true;
      try {
        const resp = await addInventoryTxn({
          itemId: this.txnForm.item_id,
          txnType: this.txnForm.txn_type,
          qty: Number(this.txnForm.qty),
          note: this.txnForm.note,
          orgId: 0
        });
        if (resp.code === 0) {
          this.$message.success("提交成功");
          this.showTxn = false;
          this.fetchItems();
          this.fetchTxns(this.txnForm.item_id);
        }
      } finally {
        this.saving = false;
      }
    },
    removeItem(row) {
      this.$confirm("确定删除该品项吗？", "提示", { type: "warning" }).then(async () => {
        await deleteInventoryItem(row.id);
        this.$message.success("已删除");
        this.fetchItems();
      }).catch(() => {});
    }
  }
};
</script>

<style scoped>
.tab-badge {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}
.tab-badge ::v-deep .el-badge__content {
  position: static;
  transform: none;
  margin-left: 6px;
}
</style>
