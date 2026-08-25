import { Routes, Route } from "react-router-dom";

import PublicLayout from "../layouts/PublicLayout";
import AppLayout from "../layouts/AppLayout";

import Landing from "../pages/public/Landing";
import Login from "../pages/public/Login";
import Register from "../pages/public/Register";
import ForgotPassword from "../pages/public/ForgotPassword";
import ResetPassword from "../pages/public/ResetPassword";

// User pages
import UserDashboard from "../pages/user/UserDashboard";
import AvailableTasks from "../pages/user/AvailableTasks";
import TaskDetails from "../pages/user/TaskDetails";
import MyTasks from "../pages/user/MyTasks";
import UserHistory from "../pages/user/UserHistory";
import UserProfile from "../pages/user/UserProfile";

// Admin pages
import AdminDashboard from "../pages/admin/AdminDashboard";
import AdminTasks from "../pages/admin/AdminTasks";
import AdminAssignments from "../pages/admin/AdminAssignments";
import AdminUsers from "../pages/admin/AdminUsers";
import UserActivity from "../pages/admin/UserActivity";
import AdminProfile from "../pages/admin/AdminProfile";
import AdminTaskDiscussion from "../pages/admin/AdminTaskDiscussion";

import ProtectedRoute from "../components/common/ProtectedRoute";

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Landing />} />
      </Route>

      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* User Dashboard - Protected */}
      <Route
        element={
          <ProtectedRoute allowedRoles={["USER"]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/user" element={<UserDashboard />} />
        <Route path="/user/tasks" element={<AvailableTasks />} />
        <Route path="/user/tasks/:id" element={<TaskDetails />} />
        <Route path="/user/my-tasks" element={<MyTasks />} />
        <Route path="/user/history" element={<UserHistory />} />
        <Route path="/user/profile" style={{ flex: 1 }} element={<UserProfile />} />
      </Route>

      {/* Admin Dashboard - Protected */}
      <Route
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/admin/tasks" element={<AdminTasks />} />
        <Route path="/admin/tasks/new" element={<AdminTasks />} />
        <Route path="/admin/tasks/:id" element={<AdminTaskDiscussion />} />
        <Route path="/admin/assignments" element={<AdminAssignments />} />
        <Route path="/admin/users" element={<AdminUsers />} />
        <Route path="/admin/users/:id" element={<UserActivity />} />
        <Route path="/admin/profile" element={<AdminProfile />} />
      </Route>

      <Route
        path="*"
        element={
          <div style={{ padding: "100px 50px", textAlign: "center", fontFamily: "sans-serif", color: "var(--color-text)" }}>
            <h2 style={{ fontSize: "28px", marginBottom: "15px" }}>Page Not Found</h2>
            <p style={{ color: "var(--color-text-secondary)" }}>The requested page could not be located.</p>
          </div>
        }
      />
    </Routes>
  );
}