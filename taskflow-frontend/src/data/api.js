
const _base = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const API_BASE = _base.endsWith("/api") ? _base : `${_base}/api`;

// Helper to get authorization and content type headers
function getHeaders() {
  const headers = {
    "Content-Type": "application/json",
  };
  const token = localStorage.getItem("token");
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  return headers;
}

// Global Fetch Wrapper
async function request(url, options = {}) {
  try {
    const res = await fetch(`${API_BASE}${url}`, {
      ...options,
      headers: {
        ...getHeaders(),
        ...options.headers,
      },
    });

    if (!res.ok) {
      let errMsg = `Request failed with status ${res.status}`;
      try {
        const errJson = await res.json();
        if (errJson) {
          if (errJson.errors && Object.keys(errJson.errors).length > 0) {
            const validationErrors = Object.entries(errJson.errors)
              .map(([field, msg]) => `${field}: ${msg}`)
              .join("; ");
            errMsg = `${errJson.message || "Validation failed"}: ${validationErrors}`;
          } else if (errJson.message) {
            errMsg = errJson.message;
          }
        }
      } catch {
        try {
          const text = await res.text();
          if (text) errMsg = text;
        } catch {}
      }
      const error = new Error(errMsg);
      error.status = res.status;
      throw error;
    }

    if (res.status === 204) {
      return null;
    }
    return await res.json();
  } catch (err) {
    if (err.message.includes("Failed to fetch") || err.message.includes("NetworkError")) {
      throw new Error("Unable to connect to the TaskFlow server. Please ensure the backend is running.");
    }
    throw err;
  }
}

// API Services Export
export const api = {
  isMocked: () => false,
  
  auth: {
    login: async (email, password) => {
      return await request("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
    },
    register: async (name, email, password) => {
      // Role is always USER — backend enforces this regardless
      return await request("/auth/register", {
        method: "POST",
        body: JSON.stringify({ name, email, password }),
      });
    },
    google: async (googleId, email, name) => {
      return await request("/auth/google", {
        method: "POST",
        body: JSON.stringify({ googleId, email, name }),
      });
    },
    getMe: async () => {
      return await request("/auth/me");
    },
    forgotPassword: async (email) => {
      return await request("/auth/forgot-password", {
        method: "POST",
        body: JSON.stringify({ email }),
      });
    },
    resetPassword: async (token, newPassword) => {
      return await request("/auth/reset-password", {
        method: "POST",
        body: JSON.stringify({ token, newPassword }),
      });
    },
  },

  users: {
    getMe: async () => await request("/users/me"),
    updateMe: async (name) => await request("/users/me", {
      method: "PUT",
      body: JSON.stringify({ name }),
    }),
  },

  tasks: {
    getPublished: async () => {
      return await request("/tasks");
    },
    getById: async (id) => {
      return await request(`/tasks/${id}`);
    },
    getAll: async () => {
      return await request("/admin/tasks");
    },
    create: async (taskData) => {
      return await request("/admin/tasks", {
        method: "POST",
        body: JSON.stringify(taskData),
      });
    },
    update: async (id, taskData) => {
      return await request(`/admin/tasks/${id}`, {
        method: "PUT",
        body: JSON.stringify(taskData),
      });
    },
    publish: async (id) => {
      return await request(`/admin/tasks/${id}/publish`, {
        method: "PATCH",
      });
    },
    delete: async (id) => {
      return await request(`/admin/tasks/${id}`, {
        method: "DELETE",
      });
    }
  },

  assignments: {
    getMyActive: async () => {
      return await request("/assignments/my");
    },
    getMyAll: async () => {
      return await request("/assignments/my/all");
    },
    assign: async (taskId) => {
      return await request(`/assignments/assign?taskId=${taskId}`, {
        method: "POST",
      });
    },
    unassign: async (id) => {
      return await request(`/assignments/${id}`, {
        method: "DELETE",
      });
    },
    unassignByTaskId: async (taskId) => {
      return await request(`/tasks/${taskId}/assignment`, {
        method: "DELETE",
      });
    },
    updateStatus: async (id, status, proofUrl) => {
      return await request(`/assignments/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status, proofUrl }),
      });
    },
    getById: async (id) => {
      return await request(`/assignments/${id}`);
    },
    getProof: async (id) => {
      return await request(`/assignments/${id}/proof`);
    }
  },

  comments: {
    list: async (taskId) => await request(`/tasks/${taskId}/comments`),
    add: async (taskId, message) => await request(`/tasks/${taskId}/comments`, {
      method: "POST", body: JSON.stringify({ message }),
    }),
    delete: async (taskId, commentId) => await request(`/tasks/${taskId}/comments/${commentId}`, { method: "DELETE" }),
  },

  notifications: {
    list: async () => await request("/notifications"),
    unreadCount: async () => await request("/notifications/unread-count"),
    markRead: async (id) => await request(`/notifications/${id}/read`, { method: "PATCH" }),
    markAllRead: async () => await request("/notifications/read-all", { method: "PATCH" }),
    subscribe: (onNotification) => {
      const token = localStorage.getItem("token");
      if (!token) return () => {};
      const controller = new AbortController();
      let buffer = "";
      (async () => {
        try {
          const res = await fetch(`${API_BASE}/notifications/stream`, {
            headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
            signal: controller.signal,
          });
          if (!res.ok || !res.body) return;
          const reader = res.body.getReader();
          const decoder = new TextDecoder();
          while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const chunks = buffer.split("\n\n");
            buffer = chunks.pop() || "";
            for (const chunk of chunks) {
              const event = chunk.match(/^event:\s*(.+)$/m)?.[1]?.trim();
              const data = chunk.match(/^data:\s*(.+)$/m)?.[1]?.trim();
              if (event === "notification" && data) {
                try { onNotification(JSON.parse(data)); } catch {}
              }
            }
          }
        } catch (e) {
          if (e?.name !== "AbortError") console.warn("Notification stream disconnected");
        }
      })();
      return () => controller.abort();
    },
  },

  coding: {
    generate: async (data) => await request("/coding/generate", { method: "POST", body: JSON.stringify(data) }),
    saveTask: async (data) => await request("/coding/tasks", { method: "POST", body: JSON.stringify(data) }),
    getVisibleTests: async (taskId) => await request(`/coding/tasks/${taskId}/tests`),
    saveTests: async (taskId, tests) => await request(`/coding/tasks/${taskId}/tests`, { method: "PUT", body: JSON.stringify(tests) }),
    run: async (taskId, language, code) => await request(`/coding/tasks/${taskId}/run`, { method: "POST", body: JSON.stringify({ language, code }) }),
    submit: async (taskId, language, code) => await request(`/coding/tasks/${taskId}/submit`, { method: "POST", body: JSON.stringify({ language, code }) }),
    mySubmissions: async () => await request("/coding/submissions/me"),
    leaderboard: async (limit = 20) => await request(`/coding/leaderboard?limit=${limit}`),
  },

  admin: {
    getDashboard: async () => {
      return await request("/admin/dashboard");
    },
    getAssignments: async (searchTerm = "", status = "", taskId = "") => {
      const queryParams = new URLSearchParams();
      if (searchTerm) queryParams.append("searchTerm", searchTerm);
      if (status) queryParams.append("status", status);
      if (taskId) queryParams.append("taskId", taskId);
      return await request(`/admin/assignments?${queryParams.toString()}`);
    },
    getAssignmentsByTaskId: async (taskId) => {
      return await request(`/admin/tasks/${taskId}/assignments`);
    },
    getAssignmentsByUserId: async (userId) => {
      return await request(`/admin/users/${userId}/assignments`);
    },
    removeAssignment: async (id, reason) => {
      return await request(`/admin/assignments/${id}`, {
        method: "DELETE",
        body: JSON.stringify({ reason }),
      });
    },
    getUsers: async () => {
      return await request("/admin/users");
    }
  }
};
