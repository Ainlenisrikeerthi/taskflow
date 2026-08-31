import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { api } from "../../data/api";
import { useAuth } from "../../components/common/AuthContext";
import { Users, ListTodo, ClipboardCheck, AlertTriangle, ExternalLink, Plus, CheckCircle, Clock, ShieldAlert } from "lucide-react";
import StatCard from "../../components/ui/StatCard";
import Badge from "../../components/ui/Badge";
import Button from "../../components/ui/Button";

export default function AdminDashboard() {
  const { currentUser } = useAuth();
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchStats();
  }, []);

  async function fetchStats() {
    setIsLoading(true);
    setError("");
    try {
      const data = await api.admin.getDashboard();
      setStats(data);
    } catch (err) {
      setError(err.message || "Failed to load dashboard statistics.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs">
            <span>Admin</span>
            <span>/</span>
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>Dashboard</span>
          </div>
          <h1>Admin Overview</h1>
          <p>Welcome back, {currentUser?.name || "Administrator"}. Here is your live platform summary.</p>
        </div>
        <div style={{ display: "flex", gap: 12 }}>
          <Link to="/admin/tasks" style={{ textDecoration: "none" }}>
           
          </Link>
        </div>
      </div>

      {/* Message Alerts */}
      {error && (
        <div className="alert-box danger" style={{ marginBottom: 24 }}>
          <AlertTriangle size={16} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {/* Metrics Section */}
      {isLoading ? (
        <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
          Loading real-time metrics...
        </div>
      ) : (
        <div>
          <div className="stats-grid">
            <StatCard
              icon={<ListTodo size={20} />}
              label="Total Tasks"
              value={stats?.totalTasks ?? 0}
              color="accent"
              subtitle="Published catalog"
            />
            <StatCard
              icon={<Users size={20} />}
              label="Enrolled Users"
              value={stats?.totalUsers ?? 0}
              color="purple"
              subtitle="Active accounts"
            />
            <StatCard
              icon={<Clock size={20} />}
              label="Not Started"
              value={stats?.notStartedAssignments ?? 0}
              color="neutral"
              subtitle="Pending initial work"
            />
            <StatCard
              icon={<ClipboardCheck size={20} />}
              label="In Progress"
              value={stats?.inProgressAssignments ?? 0}
              color="warning"
              subtitle="Actively in work"
            />
            <StatCard
              icon={<CheckCircle size={20} />}
              label="Completed"
              value={stats?.completedAssignments ?? 0}
              color="success"
              subtitle="Verified with proof"
            />
          </div>

          {/* Recent User Activity Table */}
          <div className="card" style={{ marginTop: 24, padding: "28px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <div>
                <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)" }}>Recent User Assignments</h3>
                <p style={{ fontSize: 13, color: "var(--color-text-secondary)", marginTop: 2 }}>Latest activity across all team members.</p>
              </div>
              <Link to="/admin/assignments" style={{ textDecoration: "none" }}>
                <Button variant="secondary" size="sm">
                  View All Assignments
                </Button>
              </Link>
            </div>

            {!stats?.recentAssignments || stats.recentAssignments.length === 0 ? (
              <div style={{ padding: "40px", textAlign: "center", color: "var(--color-text-secondary)" }}>
                No recent assignments recorded yet.
              </div>
            ) : (
              <div className="premium-table-wrapper">
                <table className="premium-table">
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Task Title</th>
                      <th>Assigned Date</th>
                      <th>Status</th>
                      <th>Submitted Proof</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stats.recentAssignments.map((ass) => (
                      <tr key={ass.id}>
                        <td data-label="User">
                          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                            <div className="avatar avatar-sm">
                              {ass.user?.name?.charAt(0).toUpperCase() || "U"}
                            </div>
                            <div style={{ display: "flex", flexDirection: "column" }}>
                              <span style={{ fontWeight: 700, fontSize: 14, color: "var(--color-navy)" }}>{ass.user?.name}</span>
                              <span style={{ color: "var(--color-text-secondary)", fontSize: 12 }}>{ass.user?.email}</span>
                            </div>
                          </div>
                        </td>
                        <td data-label="Task Title" style={{ fontWeight: 600, color: "var(--color-navy)" }}>
                          {ass.task?.title}
                        </td>
                        <td data-label="Assigned Date" style={{ color: "var(--color-text-secondary)", fontSize: 13 }}>
                          {ass.assignedAt ? ass.assignedAt.split("T")[0] : "—"}
                        </td>
                        <td data-label="Status">
                          <Badge status={ass.status} />
                        </td>
                        <td data-label="Submitted Proof">
                          {ass.proofUrl ? (
                            <a
                              href={ass.proofUrl}
                              target="_blank"
                              rel="noreferrer"
                              style={{ display: "inline-flex", alignItems: "center", gap: 4, color: "var(--color-electric-violet)", fontWeight: 600, fontSize: 13, textDecoration: "none" }}
                            >
                              View Proof <ExternalLink size={13} />
                            </a>
                          ) : (
                            <span style={{ color: "var(--color-text-muted)", fontSize: 13 }}>None</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
