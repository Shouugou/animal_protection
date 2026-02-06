import Vue from "vue";
import Router from "vue-router";
import store from "../store";

import Layout from "@/components/Layout.vue";
import Login from "@/views/Login.vue";
import Messages from "@/views/common/Messages.vue";
import Forbidden from "@/views/common/Forbidden.vue";

import PublicEvents from "@/views/public/Events.vue";
import PublicEventDetail from "@/views/public/EventDetail.vue";
import PublicEventNew from "@/views/public/EventNew.vue";
import PublicTasks from "@/views/public/Tasks.vue";
import PatrolSubmit from "@/views/public/PatrolSubmit.vue";
import Adoption from "@/views/public/Adoption.vue";
import Followups from "@/views/public/Followups.vue";
import Donations from "@/views/public/Donations.vue";
import Classroom from "@/views/public/Classroom.vue";

import LawWorkOrders from "@/views/law/WorkOrders.vue";
import LawWorkOrderDetail from "@/views/law/WorkOrderDetail.vue";
import LawMyTasks from "@/views/law/MyTasks.vue";
import LawArchives from "@/views/law/ArchivedCases.vue";

import RescueTasks from "@/views/rescue/Tasks.vue";
import RescueAnimals from "@/views/rescue/Animals.vue";
import MedicalRecords from "@/views/rescue/MedicalRecords.vue";
import Inventory from "@/views/rescue/Inventory.vue";

import AdminDashboard from "@/views/admin/Dashboard.vue";
import AdminReports from "@/views/admin/Reports.vue";
import AdminAcl from "@/views/admin/ACL.vue";
import AdminApprovals from "@/views/admin/Approvals.vue";

Vue.use(Router);

const roleOnly = (role) => ({ meta: { roles: [role] } });
const roleAny = (roles) => ({ meta: { roles } });

const router = new Router({
  mode: "hash",
  routes: [
    { path: "/login", component: Login },
    {
      path: "/",
      component: Layout,
      children: [
        { path: "messages", component: Messages, ...roleAny(["PUBLIC", "LAW", "RESCUE", "ADMIN"]) },

        { path: "public/events", component: PublicEvents, ...roleOnly("PUBLIC") },
        { path: "public/events/new", component: PublicEventNew, ...roleOnly("PUBLIC") },
        { path: "public/events/:id", component: PublicEventDetail, ...roleOnly("PUBLIC") },
        { path: "public/tasks", component: PublicTasks, ...roleOnly("PUBLIC") },
        { path: "public/patrol/submit/:claimId", component: PatrolSubmit, ...roleOnly("PUBLIC") },
        { path: "public/adoption", component: Adoption, ...roleOnly("PUBLIC") },
        { path: "public/followups", component: Followups, ...roleOnly("PUBLIC") },
        { path: "public/donations", component: Donations, ...roleOnly("PUBLIC") },
        { path: "public/classroom", component: Classroom, ...roleOnly("PUBLIC") },

        { path: "law/workorders", component: LawWorkOrders, ...roleOnly("LAW") },
        { path: "law/workorders/:id", component: LawWorkOrderDetail, ...roleOnly("LAW") },
        { path: "law/my-tasks", component: LawMyTasks, ...roleOnly("LAW") },
        { path: "law/archives", component: LawArchives, ...roleOnly("LAW") },

        { path: "rescue/tasks", component: RescueTasks, ...roleOnly("RESCUE") },
        { path: "rescue/animals", component: RescueAnimals, ...roleOnly("RESCUE") },
        { path: "rescue/animals/:id/records", component: MedicalRecords, ...roleOnly("RESCUE") },
        { path: "rescue/inventory", component: Inventory, ...roleOnly("RESCUE") },

        { path: "admin/dashboard", component: AdminDashboard, ...roleOnly("ADMIN") },
        { path: "admin/reports", component: AdminReports, ...roleOnly("ADMIN") },
        { path: "admin/acl", component: AdminAcl, ...roleOnly("ADMIN") },
        { path: "admin/approvals", component: AdminApprovals, ...roleOnly("ADMIN") },

        { path: "403", component: Forbidden }
      ]
    }
  ]
});

const defaultRouteByRole = {
  PUBLIC: "/public/events",
  LAW: "/law/workorders",
  RESCUE: "/rescue/tasks",
  ADMIN: "/admin/dashboard"
};

router.beforeEach((to, from, next) => {
  const isLogin = to.path === "/login";
  const token = store.getters.token || (() => {
    try {
      const raw = localStorage.getItem("ap_frontend_auth");
      if (!raw) return "";
      const parsed = JSON.parse(raw);
      return parsed.token || "";
    } catch (e) {
      return "";
    }
  })();

  if (!token && !isLogin) {
    return next({ path: "/login" });
  }

  if (isLogin && token) {
    const role = store.getters.roleCode;
    return next({ path: defaultRouteByRole[role] || "/messages" });
  }

  if (to.path === "/") {
    const role = store.getters.roleCode;
    return next({ path: defaultRouteByRole[role] || "/messages" });
  }

  const roles = to.meta && to.meta.roles;
  if (roles && roles.length > 0) {
    const role = store.getters.roleCode;
    if (!roles.includes(role)) {
      return next({ path: "/403" });
    }
  }

  return next();
});

export default router;
