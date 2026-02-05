<template>
  <div>
    <el-card>
      <div slot="header" class="section-title">
        <span>事件详情 #{{ id }}</span>
        <el-button size="mini" @click="showSupplement = true">补充线索</el-button>
      </div>
      <el-descriptions :column="2">
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.event_type }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.reported_at }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <TimelineList :items="timeline" />

    <el-card>
      <div slot="header">处理反馈</div>
      <el-empty v-if="feedbacks.length === 0" description="暂无反馈" />
      <el-table v-else :data="feedbacks" style="width:100%">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="time" label="时间" width="160" />
      </el-table>
    </el-card>

    <el-card>
      <div slot="header">评论</div>
      <el-input type="textarea" v-model="comment" rows="3" placeholder="留言评论" />
      <el-button type="primary" size="mini" style="margin-top:8px" @click="postComment">
        发布
      </el-button>
      <el-divider />
      <el-empty v-if="comments.length === 0" description="暂无评论" />
      <el-card v-else v-for="(c, idx) in comments" :key="idx" style="margin-bottom:8px">
        {{ c }}
      </el-card>
    </el-card>

    <el-dialog title="补充线索" :visible.sync="showSupplement" width="520px">
      <el-input type="textarea" v-model="supplement" rows="4" placeholder="补充说明" />
      <Uploader title="补充附件" @change="onFiles" />
      <span slot="footer">
        <el-button @click="showSupplement = false">取消</el-button>
        <el-button type="primary" @click="submitSupplement">提交</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import TimelineList from "@/components/TimelineList.vue";
import Uploader from "@/components/Uploader.vue";

export default {
  name: "PublicEventDetail",
  components: { TimelineList, Uploader },
  data() {
    return {
      id: this.$route.params.id,
      detail: {
        status: "REPORTED",
        event_type: "受伤动物",
        address: "XX路",
        reported_at: "2026-02-05"
      },
      timeline: [
        { id: 1, node_type: "上报", content: "事件已上报", created_at: "2026-02-05" }
      ],
      feedbacks: [
        { title: "受理反馈", content: "工单已接收", time: "2026-02-05" }
      ],
      showSupplement: false,
      supplement: "",
      files: [],
      comment: "",
      comments: []
    };
  },
  methods: {
    onFiles(files) {
      this.files = files;
    },
    submitSupplement() {
      this.$message.success("补充成功（演示）");
      this.showSupplement = false;
    },
    postComment() {
      if (!this.comment) return;
      this.comments.push(this.comment);
      this.comment = "";
    }
  }
};
</script>
