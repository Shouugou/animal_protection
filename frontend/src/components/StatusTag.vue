<template>
  <span class="badge" :style="styleObj">{{ text }}</span>
</template>

<script>
export default {
  name: "StatusTag",
  props: {
    text: { type: String, default: "" },
    color: { type: String, default: "#e5e7eb" },
    textColor: { type: String, default: "#111827" }
  },
  computed: {
    styleObj() {
      return { background: this.color, color: this.textColor };
    }
  }
};
</script>
