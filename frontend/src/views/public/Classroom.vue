<template>
  <el-card>
    <div slot="header">动保课堂</div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button size="mini" @click="open(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="内容详情" :visible.sync="show" width="600px">
      <p>{{ current.title }}</p>
      <p>{{ current.body }}</p>
      <span slot="footer">
        <el-button @click="show=false">关闭</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
export default {
  name: "Classroom",
  data() {
    return {
      list: [
        { id: 1, title: "如何科学救助流浪动物", type: "图文", body: "示例内容" },
        { id: 2, title: "常见犬猫疾病预防", type: "视频", body: "示例内容" }
      ],
      show: false,
      current: {}
    };
  },
  methods: {
    open(row) {
      this.current = row;
      this.show = true;
    }
  }
};
</script>
