import axios from "axios";
import store from "@/store";

const http = axios.create({
  baseURL: "/api",
  timeout: 15000
});

const getStoredToken = () => {
  try {
    const raw = localStorage.getItem("ap_frontend_auth");
    if (!raw) return "";
    const parsed = JSON.parse(raw);
    return parsed.token || "";
  } catch (e) {
    return "";
  }
};

http.interceptors.request.use((config) => {
  const token = store.getters.token || getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (resp) => {
    const data = resp.data;
    if (data && data.code === -1 && data.message && data.message.includes("未登录")) {
      store.dispatch("auth/logout");
      window.location.hash = "#/login";
    }
    return data;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      store.dispatch("auth/logout");
      window.location.hash = "#/login";
    }
    return Promise.reject(error);
  }
);

export default http;
