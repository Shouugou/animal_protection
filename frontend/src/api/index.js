import http from "./http";

export const login = (payload) => http.post("/auth/login", payload);
export const register = (payload) => http.post("/auth/register", payload);
export const fetchMessages = (params) => http.get("/messages", { params });
export const uploadFile = (file) => {
  const form = new FormData();
  form.append("file", file);
  return http.post("/upload", form, {
    headers: { "Content-Type": "multipart/form-data" }
  });
};

export const createEvent = (payload) => http.post("/events", payload);
export const listEvents = (params) => http.get("/events", { params });
export const getEvent = (id) => http.get(`/events/${id}`);
export const getEventTimeline = (id) => http.get(`/events/${id}/timeline`);
export const addEventSupplement = (id, payload) => http.post(`/events/${id}/supplements`, payload);
export const listEventComments = (id) => http.get(`/events/${id}/comments`);
export const addEventComment = (id, payload) => http.post(`/events/${id}/comments`, payload);
export const deleteEvent = (id) => http.delete(`/events/${id}`);

export const listTasks = (params) => http.get("/tasks", { params });
export const claimTask = (id) => http.post(`/tasks/${id}/claim`);
export const startTaskClaim = (id) => http.post(`/task-claims/${id}/start`);
export const submitPatrolReport = (payload) => http.post("/patrol-reports", payload);

export const listAnimals = (params) => http.get("/animals", { params });
export const createAdoption = (payload) => http.post("/adoptions", payload);
export const listFollowups = (params) => http.get("/followups", { params });
export const submitFollowup = (payload) => http.post("/followups", payload);
export const donate = (payload) => http.post("/donations", payload);

export const listContent = (params) => http.get("/content", { params });
export const getContent = (id) => http.get(`/content/${id}`);

export const listWorkOrders = (params) => http.get("/law/workorders", { params });
export const getWorkOrder = (id) => http.get(`/law/workorders/${id}`);

export const listRescueTasks = (params) => http.get("/rescue/tasks", { params });
