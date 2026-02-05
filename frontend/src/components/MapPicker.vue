<template>
  <el-card>
    <div slot="header">地图选点</div>
    <div class="map-container" ref="map"></div>
    <el-form label-width="90px" style="margin-top:12px">
      <el-form-item label="地址">
        <el-input v-model="address" placeholder="自动填充/手动修改"></el-input>
      </el-form-item>
      <el-row :gutter="10">
        <el-col :span="12">
          <el-form-item label="纬度">
            <el-input v-model="latitude"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="经度">
            <el-input v-model="longitude"></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-button type="primary" size="small" @click="locate">定位当前位置</el-button>
      <el-button type="primary" size="small" @click="emitPick" style="margin-left:8px">确认位置</el-button>
    </el-form>
  </el-card>
</template>

<script>
export default {
  name: "MapPicker",
  data() {
    return {
      map: null,
      marker: null,
      geocoder: null,
      address: "",
      latitude: "",
      longitude: ""
    };
  },
  mounted() {
    if (window.BMap) {
      const map = new window.BMap.Map(this.$refs.map);
      const point = new window.BMap.Point(116.404, 39.915);
      map.centerAndZoom(point, 12);
      map.enableScrollWheelZoom(true);
      this.geocoder = new window.BMap.Geocoder();
      map.addEventListener("click", (e) => {
        this.setPoint(e.point);
      });
      this.map = map;
    }
  },
  methods: {
    setPoint(point) {
      this.latitude = point.lat.toFixed(6);
      this.longitude = point.lng.toFixed(6);
      if (this.marker) {
        this.map.removeOverlay(this.marker);
      }
      this.marker = new window.BMap.Marker(point);
      this.map.addOverlay(this.marker);
      if (this.geocoder) {
        this.geocoder.getLocation(point, (rs) => {
          if (rs && rs.address) this.address = rs.address;
        });
      }
    },
    locate() {
      if (!window.BMap || !this.map) return;
      const geolocation = new window.BMap.Geolocation();
      geolocation.getCurrentPosition(
        (r) => {
          if (geolocation.getStatus() === window.BMAP_STATUS_SUCCESS) {
            this.map.panTo(r.point);
            this.setPoint(r.point);
          } else {
            this.$message.warning("定位失败，请手动选择位置");
          }
        },
        { enableHighAccuracy: true }
      );
    },
    emitPick() {
      this.$emit("pick", {
        address: this.address,
        latitude: this.latitude,
        longitude: this.longitude,
        region_code: ""
      });
    }
  }
};
</script>
