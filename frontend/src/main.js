import Vue from "vue";
import ElementUI from "element-ui";
import "element-ui/lib/theme-chalk/index.css";
import App from "./App.vue";
import router from "./router";
import store from "./store";
import "./assets/styles.css";

Vue.config.productionTip = false;
Vue.use(ElementUI);

Vue.directive("perm", {
  inserted(el, binding) {
    const perm = binding.value;
    const hasPerm = store.getters.permCodes.includes(perm);
    if (!hasPerm) {
      el.parentNode && el.parentNode.removeChild(el);
    }
  }
});

store.dispatch("auth/loadFromStorage");

new Vue({
  router,
  store,
  render: (h) => h(App)
}).$mount("#app");
