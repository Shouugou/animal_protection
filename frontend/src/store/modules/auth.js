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
  loginMock({ commit }, { roleCode, name }) {
    const payload = {
      token: "mock-token",
      profile: {
        role_code: roleCode,
        user_id: 1,
        org_id: roleCode === "PUBLIC" ? null : 1001,
        name: name || "演示用户"
      },
      permCodes: []
    };
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
