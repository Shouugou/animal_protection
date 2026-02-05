const STORAGE_KEY = "ap_frontend_auth";

const state = {
  token: "",
  profile: {
    role_code: "PUBLIC",
    user_id: 0,
    org_id: null,
    name: ""
  },
  permCodes: []
};

const mutations = {
  SET_AUTH(state, payload) {
    state.token = payload.token;
    state.profile = payload.profile;
    state.permCodes = payload.permCodes || [];
  },
  CLEAR_AUTH(state) {
    state.token = "";
    state.profile = { role_code: "PUBLIC", user_id: 0, org_id: null, name: "" };
    state.permCodes = [];
  }
};

import { login as loginApi } from "@/api";

const actions = {
  loadFromStorage({ commit }) {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    try {
      const parsed = JSON.parse(raw);
      commit("SET_AUTH", parsed);
    } catch (e) {
      localStorage.removeItem(STORAGE_KEY);
    }
  },
  async login({ commit }, { phone, password }) {
    const resp = await loginApi({ phone, password });
    if (resp.code !== 0) {
      throw new Error(resp.message || "登录失败");
    }
    const payload = resp.data;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    commit("SET_AUTH", payload);
  },
  logout({ commit }) {
    localStorage.removeItem(STORAGE_KEY);
    commit("CLEAR_AUTH");
  }
};

export default {
  namespaced: true,
  state,
  mutations,
  actions
};
