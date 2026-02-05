<template>
  <el-card>
    <div slot="header" class="section-title">
      <span>动物档案</span>
      <el-button size="mini" type="primary" @click="show = true">新建档案</el-button>
    </div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="id" label="ID" width="90" />
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
        <el-form-item label="物种">
          <el-input v-model="form.species" />
        </el-form-item>
        <el-form-item label="体检摘要">
          <el-input type="textarea" v-model="form.summary" rows="3" />
        </el-form-item>
      </el-form>
      <Uploader title="档案附件" @change="onFiles" />
      <span slot="footer">
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" @click="create">保存</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import Uploader from "@/components/Uploader.vue";

export default {
  name: "RescueAnimals",
  components: { Uploader },
  data() {
    return {
      list: [{ id: 9001, species: "犬", status: "IN_CARE" }],
      show: false,
      form: { species: "", summary: "" },
      files: []
    };
  },
  methods: {
    onFiles(files) {
      this.files = files;
    },
    create() {
      this.$message.success("建档成功（演示）");
      this.show = false;
    }
  }
};
</script>
