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
export const submitRescueSupportReport = (payload) => http.post("/rescue-support-reports", payload);
export const listMyTaskClaims = () => http.get("/task-claims");
export const getMyTaskClaim = (id) => http.get(`/task-claims/${id}`);
export const registerVolunteer = () => http.post("/volunteer/register");
export const createLawVolunteerTask = (payload) => http.post("/law/volunteer-tasks", payload);
export const createRescueVolunteerTask = (payload) => http.post("/rescue/volunteer-tasks", payload);
export const listLawVolunteerTasks = () => http.get("/law/volunteer-tasks");
export const listRescueVolunteerTasks = () => http.get("/rescue/volunteer-tasks");
export const getLawVolunteerTask = (id) => http.get(`/law/volunteer-tasks/${id}`);
export const deleteLawVolunteerTask = (id) => http.delete(`/law/volunteer-tasks/${id}`);
export const getRescueVolunteerTask = (id) => http.get(`/rescue/volunteer-tasks/${id}`);
export const deleteRescueVolunteerTask = (id) => http.delete(`/rescue/volunteer-tasks/${id}`);
export const listLawPatrolReports = (params) => http.get("/law/patrol-reports", { params });
export const getLawPatrolReport = (id) => http.get(`/law/patrol-reports/${id}`);
export const listRescueVolunteerReports = (params) => http.get("/rescue/volunteer-reports", { params });
export const getRescueVolunteerReport = (id) => http.get(`/rescue/volunteer-reports/${id}`);

export const listAnimals = (params) => http.get("/animals", { params });
export const createAdoption = (payload) => http.post("/adoptions", payload);
export const listFollowups = (params) => http.get("/followups", { params });
export const submitFollowup = (payload) => http.post("/followups", payload);
export const donate = (payload) => http.post("/donations", payload);

export const listContent = (params) => http.get("/content", { params });
export const getContent = (id) => http.get(`/content/${id}`);

export const listWorkOrders = (params) => http.get("/law/workorders", { params });
export const getWorkOrder = (id) => http.get(`/law/workorders/${id}`);
export const acceptWorkOrder = (id, payload) => http.post(`/law/workorders/${id}/accept`, payload);
export const assignWorkOrder = (id, payload) => http.post(`/law/workorders/${id}/assign`, payload);
export const addLawEvidence = (payload) => http.post("/law/evidence", payload);
export const saveLawResult = (id, payload) => http.post(`/law/workorders/${id}/result`, payload);
export const archiveWorkOrder = (id, payload) => http.post(`/law/workorders/${id}/archive`, payload);
export const listLawAssignees = () => http.get("/law/assignees");
export const listMyWorkOrders = (params) => http.get("/law/my-workorders", { params });
export const listArchivedWorkOrders = (params) => http.get("/law/archived-workorders", { params });

export const listRescueTasks = (params) => http.get("/rescue/tasks", { params });

export const grabRescueTask = (id) => http.post(`/rescue/tasks/${id}/grab`);
export const evaluateRescueTask = (id, payload) => http.post(`/rescue/tasks/${id}/evaluate`, payload);
export const dispatchRescueTask = (id, payload) => http.post(`/rescue/tasks/${id}/dispatch`, payload);
export const listRescueAnimals = () => http.get("/rescue/animals");
export const createRescueAnimal = (payload) => http.post("/rescue/animals", payload);
export const listMedicalRecords = (animalId) => http.get("/rescue/medical-records", { params: { animalId } });
export const addMedicalRecord = (payload) => http.post("/rescue/medical-records", payload);
export const shareMedicalRecord = (payload) => http.post("/rescue/medical-records/share", payload);
export const listInventoryItems = () => http.get("/rescue/inventory/items");
export const createInventoryItem = (payload) => http.post("/rescue/inventory/items", payload);
export const addInventoryTxn = (payload) => http.post("/rescue/inventory/txns", payload);
export const listInventoryTxns = (itemId) => http.get("/rescue/inventory/txns", { params: { itemId } });
export const listInventoryAlerts = () => http.get("/rescue/inventory/alerts");
export const deleteInventoryItem = (id) => http.delete(`/rescue/inventory/items/${id}`);
