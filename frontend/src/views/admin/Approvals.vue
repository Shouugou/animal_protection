<template>
  <el-card>
    <div slot="header">审核管理</div>
    <el-tabs v-model="tab" @tab-click="handleTab">
      <el-tab-pane label="审核流程" name="flows">
        <div class="section-title">
          <span>流程定义</span>
          <el-button size="mini" type="primary" @click="openFlow()">新建流程</el-button>
        </div>
        <el-table :data="flows" style="width:100%">
          <el-table-column prop="flow_code" label="编码" width="160" />
          <el-table-column prop="flow_name" label="名称" />
          <el-table-column prop="biz_type" label="业务类型" width="160" />
          <el-table-column label="操作" width="160">
            <template slot-scope="scope">
              <el-button size="mini" @click="openFlow(scope.row)">编辑</el-button>
              <el-button size="mini" type="danger" @click="removeFlow(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="课堂分类" name="categories">
        <div class="section-title">
          <span>分类配置</span>
          <el-button size="mini" type="primary" @click="openCategory()">新增分类</el-button>
        </div>
        <el-table :data="categories" style="width:100%">
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="sort_no" label="排序" width="100" />
          <el-table-column label="审核流程" width="200">
            <template slot-scope="scope">
              {{ scope.row.flow_name || "未绑定" }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template slot-scope="scope">
              {{ scope.row.status === 1 ? "启用" : "停用" }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template slot-scope="scope">
              <el-button size="mini" @click="openCategory(scope.row)">编辑</el-button>
              <el-button size="mini" type="warning" @click="toggleCategory(scope.row)">
                {{ scope.row.status === 1 ? "停用" : "启用" }}
              </el-button>
              <el-button size="mini" type="danger" @click="removeCategory(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="内容审核" name="approvals">
        <div class="section-title">
          <span>待审核内容</span>
          <el-select v-model="approvalStatus" size="mini" style="width:120px" @change="fetchApprovals">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </div>
        <el-table :data="approvals" v-loading="loadingApprovals" style="width:100%">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="category_name" label="分类" width="160" />
          <el-table-column prop="target_org_name" label="目标机构" width="160" />
          <el-table-column prop="author_name" label="投稿人" width="140" />
          <el-table-column prop="author_role" label="角色" width="120" />
          <el-table-column prop="created_at" label="提交时间" width="180" />
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <el-button size="mini" @click="openPreview(scope.row)">预览</el-button>
              <el-button size="mini" type="success" @click="approve(scope.row)" v-if="scope.row.approval_status === 'PENDING'">
                通过
              </el-button>
              <el-button size="mini" type="danger" @click="reject(scope.row)" v-if="scope.row.approval_status === 'PENDING'">
                驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="流程定义" :visible.sync="showFlow" width="600px">
      <el-form label-width="90px">
        <el-form-item label="编码">
          <el-input v-model="flowForm.flowCode" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="flowForm.flowName" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="flowForm.bizType" />
        </el-form-item>
        <el-form-item label="审核节点">
          <el-checkbox-group v-model="flowForm.roles">
            <el-checkbox label="ADMIN">管理员</el-checkbox>
            <el-checkbox label="LAW">执法机构</el-checkbox>
            <el-checkbox label="RESCUE">救助机构</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">节点顺序为：管理员 → 执法机构 → 救助机构</div>
        </el-form-item>
        <el-form-item label="执法机构" v-if="flowForm.roles.includes('LAW')">
          <el-select v-model="flowForm.lawOrgId" placeholder="选择执法机构">
            <el-option v-for="o in lawOrgs" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="救助机构" v-if="flowForm.roles.includes('RESCUE')">
          <el-select v-model="flowForm.rescueOrgId" placeholder="选择救助机构">
            <el-option v-for="o in rescueOrgs" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showFlow=false">取消</el-button>
        <el-button type="primary" @click="saveFlow">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="分类配置" :visible.sync="showCategory" width="520px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="categoryForm.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sortNo" :min="0" /></el-form-item>
        <el-form-item label="审核流程">
          <el-select v-model="categoryForm.approvalFlowId" placeholder="选择流程">
            <el-option v-for="f in flows" :key="f.id" :label="f.flow_name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showCategory=false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="内容预览" :visible.sync="showPreview" width="760px">
      <div v-if="preview">
        <div class="preview-title">{{ preview.title }}</div>
        <div class="preview-meta">{{ preview.category_name }} | {{ preview.content_type === "VIDEO" ? "短视频" : "图文" }}</div>
        <div v-if="preview.cover_url" class="preview-cover">
          <img :src="preview.cover_url" alt="cover" />
        </div>
        <div v-if="preview.video_url" class="preview-video">
          <video controls :src="preview.video_url" />
        </div>
        <div v-else class="preview-body">{{ preview.body }}</div>
      </div>
      <span slot="footer">
        <el-button @click="showPreview=false">关闭</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import {
  listApprovalFlows,
  saveApprovalFlow,
  deleteApprovalFlow,
  listContentCategoriesAdmin,
  saveContentCategory,
  deleteContentCategory,
  listContentApprovals,
  approveContent,
  rejectContent,
  getContent,
  listDonationOrganizations
} from "@/api";

export default {
  name: "AdminApprovals",
  data() {
    return {
      tab: "flows",
      flows: [],
      showFlow: false,
      flowForm: { id: null, flowCode: "", flowName: "", bizType: "CONTENT", roles: [], lawOrgId: null, rescueOrgId: null },
      categories: [],
      showCategory: false,
      categoryForm: { id: null, name: "", sortNo: 0, status: 1, approvalFlowId: null },
      approvals: [],
      approvalStatus: "PENDING",
      loadingApprovals: false,
      showPreview: false,
      preview: null,
      lawOrgs: [],
      rescueOrgs: []
    };
  },
  created() {
    this.fetchFlows();
    this.fetchOrgs();
  },
  methods: {
    handleTab() {
      if (this.tab === "categories") {
        this.fetchCategories();
      } else if (this.tab === "approvals") {
        this.fetchApprovals();
      }
    },
    async fetchFlows() {
      const resp = await listApprovalFlows();
      if (resp.code === 0) this.flows = resp.data || [];
    },
    async fetchOrgs() {
      const resp = await listDonationOrganizations();
      if (resp.code === 0) {
        const all = resp.data || [];
        this.lawOrgs = all.filter((o) => o.org_type === "LAW");
        this.rescueOrgs = all.filter((o) => o.org_type === "RESCUE");
      }
    },
    openFlow(row) {
      if (row) {
        const roles = this.parseRoles(row.steps);
        const targets = this.parseTargets(row.steps);
        this.flowForm = {
          id: row.id,
          flowCode: row.flow_code,
          flowName: row.flow_name,
          bizType: row.biz_type,
          roles,
          lawOrgId: targets.lawOrgId,
          rescueOrgId: targets.rescueOrgId
        };
      } else {
        this.flowForm = { id: null, flowCode: "", flowName: "", bizType: "CONTENT", roles: [], lawOrgId: null, rescueOrgId: null };
      }
      this.showFlow = true;
    },
    async saveFlow() {
      if (!this.flowForm.flowCode || !this.flowForm.flowName) {
        this.$message.warning("请填写流程编码与名称");
        return;
      }
      const steps = this.buildSteps(this.flowForm.roles, this.flowForm.lawOrgId, this.flowForm.rescueOrgId);
      await saveApprovalFlow({
        id: this.flowForm.id,
        flowCode: this.flowForm.flowCode,
        flowName: this.flowForm.flowName,
        bizType: this.flowForm.bizType,
        steps: JSON.stringify(steps)
      });
      this.showFlow = false;
      this.fetchFlows();
    },
    async removeFlow(row) {
      await deleteApprovalFlow(row.id);
      this.fetchFlows();
    },
    async fetchCategories() {
      const resp = await listContentCategoriesAdmin();
      if (resp.code === 0) this.categories = resp.data || [];
    },
    openCategory(row) {
      if (row) {
        this.categoryForm = {
          id: row.id,
          name: row.name,
          sortNo: row.sort_no,
          status: row.status,
          approvalFlowId: row.approval_flow_id
        };
      } else {
        this.categoryForm = { id: null, name: "", sortNo: 0, status: 1, approvalFlowId: null };
      }
      this.showCategory = true;
    },
    async saveCategory() {
      if (!this.categoryForm.name) {
        this.$message.warning("请填写分类名称");
        return;
      }
      await saveContentCategory(this.categoryForm);
      this.showCategory = false;
      this.fetchCategories();
    },
    async toggleCategory(row) {
      await saveContentCategory({
        id: row.id,
        name: row.name,
        sortNo: row.sort_no,
        status: row.status === 1 ? 0 : 1,
        approvalFlowId: row.approval_flow_id
      });
      this.fetchCategories();
    },
    async removeCategory(row) {
      await deleteContentCategory(row.id);
      this.fetchCategories();
    },
    async fetchApprovals() {
      this.loadingApprovals = true;
      try {
        const resp = await listContentApprovals({ status: this.approvalStatus });
        if (resp.code === 0) this.approvals = resp.data || [];
      } finally {
        this.loadingApprovals = false;
      }
    },
    openPreview(row) {
      getContent(row.content_id).then((resp) => {
        if (resp.code === 0) {
          this.preview = resp.data || row;
        } else {
          this.preview = row;
        }
        this.showPreview = true;
      });
    },
    async approve(row) {
      await approveContent(row.instance_id);
      this.fetchApprovals();
    },
    async reject(row) {
      await rejectContent(row.instance_id, { note: "驳回" });
      this.fetchApprovals();
    }
    ,
    parseRoles(steps) {
      if (!steps) return [];
      try {
        const list = typeof steps === "string" ? JSON.parse(steps) : steps;
        return list.map((s) => s.role).filter(Boolean);
      } catch (e) {
        return [];
      }
    },
    parseTargets(steps) {
      if (!steps) return { lawOrgId: null, rescueOrgId: null };
      try {
        const list = typeof steps === "string" ? JSON.parse(steps) : steps;
        let lawOrgId = null;
        let rescueOrgId = null;
        list.forEach((s) => {
          if (s.role === "LAW") lawOrgId = s.orgId || null;
          if (s.role === "RESCUE") rescueOrgId = s.orgId || null;
        });
        return { lawOrgId, rescueOrgId };
      } catch (e) {
        return { lawOrgId: null, rescueOrgId: null };
      }
    },
    buildSteps(roles, lawOrgId, rescueOrgId) {
      const order = ["ADMIN", "LAW", "RESCUE"];
      return order
        .filter((r) => roles.includes(r))
        .map((r) => ({
          role: r,
          orgId: r === "LAW" ? lawOrgId : r === "RESCUE" ? rescueOrgId : null
        }));
    }
  }
};
</script>

<style scoped>
.form-tip {
  margin-top: 6px;
  color: #999;
  font-size: 12px;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.preview-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}
.preview-meta {
  color: #666;
  margin-bottom: 12px;
}
.preview-cover img {
  max-width: 100%;
  border-radius: 6px;
}
.preview-video video {
  width: 100%;
  max-height: 420px;
  margin-top: 12px;
}
.preview-body {
  margin-top: 12px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
