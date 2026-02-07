<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>动物档案</span>
      <el-button size="mini" type="primary" @click="show = true">新建档案</el-button>
    </div>
    <el-table :data="list" style="width:100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="rescue_task_id" label="任务ID" width="100" />
      <el-table-column prop="species" label="物种" />
      <el-table-column prop="status" label="状态" width="160" />
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-link type="primary" @click="$router.push(`/rescue/animals/${scope.row.id}/records`)">
            病历
          </el-link>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="动物建档" :visible.sync="show" width="520px">
      <el-form label-width="90px">
        <el-form-item label="救助任务">
          <el-select v-model="form.rescue_task_id" placeholder="选择救助任务">
            <el-option
              v-for="t in tasks"
              :key="t.id"
              :label="`${t.id} - ${t.event_type || '事件'}`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物种">
          <el-input v-model="form.species" />
        </el-form-item>
        <el-form-item label="体检摘要">
          <el-input type="textarea" v-model="form.summary" rows="3" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">保存</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import { listRescueAnimals, createRescueAnimal, listRescueTasks } from "@/api";

export default {
  name: "RescueAnimals",
  data() {
    return {
      list: [],
      tasks: [],
      show: false,
      form: { rescue_task_id: "", species: "", summary: "" },
      loading: false,
      saving: false
    };
  },
  created() {
    this.fetch();
    this.fetchTasks();
  },
  methods: {
    async fetch() {
      this.loading = true;
      try {
        const resp = await listRescueAnimals();
        if (resp.code === 0) {
          this.list = resp.data || [];
        }
      } finally {
        this.loading = false;
      }
    },
    async fetchTasks() {
      const resp = await listRescueTasks({ status: "" });
      if (resp.code === 0) {
        this.tasks = resp.data || [];
      }
    },
    async create() {
      if (!this.form.rescue_task_id || !this.form.species) {
        this.$message.warning("请填写救助任务和物种");
        return;
      }
      this.saving = true;
      try {
        const resp = await createRescueAnimal({
          rescueTaskId: this.form.rescue_task_id,
          species: this.form.species,
          summary: this.form.summary
        });
        if (resp.code === 0) {
          this.$message.success("建档成功");
          this.show = false;
          this.form = { rescue_task_id: "", species: "", summary: "" };
          this.fetch();
        }
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>
