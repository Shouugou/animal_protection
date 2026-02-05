import http from "./http";

export const login = (payload) => http.post("/auth/login", payload);
export const fetchMessages = (params) => http.get("/messages", { params });

export const createEvent = (payload) => http.post("/events", payload);
export const listEvents = (params) => http.get("/events", { params });
export const getEvent = (id) => http.get(`/events/${id}`);

export const listWorkOrders = (params) => http.get("/law/workorders", { params });
export const getWorkOrder = (id) => http.get(`/law/workorders/${id}`);

export const listRescueTasks = (params) => http.get("/rescue/tasks", { params });

export const listTasks = (params) => http.get("/tasks", { params });
export const claimTask = (id) => http.post(`/tasks/${id}/claim`);

export const listAnimals = (params) => http.get("/animals", { params });
export const listContent = (params) => http.get("/content", { params });
