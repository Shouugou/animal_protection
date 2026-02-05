import axios from "axios";
import store from "@/store";

const http = axios.create({
  baseURL: "/api",
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = store.getters.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (resp) => resp.data,
  (error) => {
    if (error.response && error.response.status === 401) {
      store.dispatch("auth/logout");
      window.location.hash = "#/login";
    }
    return Promise.reject(error);
  }
);

export default http;
