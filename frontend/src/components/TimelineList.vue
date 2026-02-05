<template>
  <el-card>
    <div slot="header">时间线</div>
    <el-timeline>
      <el-timeline-item
        v-for="item in items"
        :key="item.id"
        :timestamp="item.created_at"
        placement="top"
      >
        <el-tag size="mini" style="margin-bottom:6px">{{ item.node_type }}</el-tag>
        <div>{{ item.content }}</div>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<script>
export default {
  name: "TimelineList",
  props: {
    items: { type: Array, default: () => [] }
  }
};
</script>
