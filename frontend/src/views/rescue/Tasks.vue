<template>
  <el-card>
    <div slot="header">救助任务</div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="need_rescue" label="是否救助">
        <template slot-scope="scope">
          {{ scope.row.need_rescue ? "是" : "否" }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template slot-scope="scope">
          <el-button size="mini" @click="evaluate(scope.row)">评估</el-button>
          <el-button size="mini" type="primary" @click="dispatch(scope.row)">调度</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="评估是否救助" :visible.sync="showEval" width="420px">
      <el-form label-width="80px">
        <el-form-item label="是否救助">
          <el-select v-model="evalForm.need_rescue">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="结论">
          <el-input type="textarea" v-model="evalForm.note" rows="3" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showEval=false">取消</el-button>
        <el-button type="primary" @click="saveEval">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="救助调度" :visible.sync="showDispatch" width="520px">
      <el-form label-width="90px">
        <el-form-item label="调度说明">
          <el-input type="textarea" v-model="dispatchForm.note" rows="3" />
        </el-form-item>
        <el-form-item label="出发时间">
          <el-date-picker v-model="dispatchForm.start" type="datetime" />
        </el-form-item>
        <el-form-item label="到达时间">
          <el-date-picker v-model="dispatchForm.arrive" type="datetime" />
        </el-form-item>
        <el-form-item label="入站时间">
          <el-date-picker v-model="dispatchForm.intake" type="datetime" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showDispatch=false">取消</el-button>
        <el-button type="primary" @click="saveDispatch">保存</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
export default {
  name: "RescueTasks",
  data() {
    return {
      list: [{ id: 5001, status: "NEW", need_rescue: true }],
      showEval: false,
      showDispatch: false,
      evalForm: { need_rescue: true, note: "" },
      dispatchForm: { note: "", start: "", arrive: "", intake: "" }
    };
  },
  methods: {
    evaluate() {
      this.showEval = true;
    },
    dispatch() {
      this.showDispatch = true;
    },
    saveEval() {
      this.$message.success("评估已保存（演示）");
      this.showEval = false;
    },
    saveDispatch() {
      this.$message.success("调度已保存（演示）");
      this.showDispatch = false;
    }
  }
};
</script>
