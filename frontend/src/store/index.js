import Vue from "vue";
import Vuex from "vuex";
import auth from "./modules/auth";

Vue.use(Vuex);

const store = new Vuex.Store({
  modules: { auth },
  getters: {
    token: (state) => state.auth.token,
    roleCode: (state) => state.auth.profile.role_code,
    permCodes: (state) => state.auth.permCodes
  }
});

export default store;
