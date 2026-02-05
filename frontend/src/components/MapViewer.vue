<template>
  <el-card>
    <div slot="header">事件位置</div>
    <div v-if="!hasPoint" style="color:#6b7280">无定位信息</div>
    <div v-else class="map-container" ref="map"></div>
  </el-card>
</template>

<script>
export default {
  name: "MapViewer",
  props: {
    latitude: { type: [String, Number], default: "" },
    longitude: { type: [String, Number], default: "" }
  },
  computed: {
    hasPoint() {
      return this.latitude !== "" && this.longitude !== "" && !Number.isNaN(Number(this.latitude)) && !Number.isNaN(Number(this.longitude));
    }
  },
  data() {
    return {
      map: null,
      marker: null,
      pending: false
    };
  },
  mounted() {
    this.renderMap();
  },
  watch: {
    latitude() {
      this.renderMap();
    },
    longitude() {
      this.renderMap();
    }
  },
  methods: {
    renderMap() {
      if (!this.hasPoint || !window.BMap || !window.BMap.Map) return;
      if (this.pending) return;
      this.pending = true;
      this.$nextTick(() => {
        try {
          const container = this.$refs.map;
          if (!container) return;
          const lat = Number(this.latitude);
          const lng = Number(this.longitude);
          if (Number.isNaN(lat) || Number.isNaN(lng)) return;
          if (!this.map) {
            this.map = new window.BMap.Map(container);
            this.map.enableScrollWheelZoom(true);
          }
          const point = new window.BMap.Point(lng, lat);
          this.map.centerAndZoom(point, 14);
          if (this.marker) {
            this.map.removeOverlay(this.marker);
          }
          this.marker = new window.BMap.Marker(point);
          this.map.addOverlay(this.marker);
        } finally {
          this.pending = false;
        }
      });
    }
  }
};
</script>
