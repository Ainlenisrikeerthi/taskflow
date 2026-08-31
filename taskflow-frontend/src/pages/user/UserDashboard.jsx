import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { api } from "../../data/api";
import { useAuth } from "../../components/common/AuthContext";
import { ClipboardCheck, CheckCircle, AlertCircle, Layers, ArrowRight, Clock } from "lucide-react";
import StatCard from "../../components/ui/StatCard";
import Badge from "../../components/ui/Badge";
import Button from "../../components/ui/Button";

export default function UserDashboard() {
  const { currentUser } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchDashboardData();
  }, []);

  async function fetchDashboardData() {
    setIsLoading(true);
    setError("");
    try {
      const tasksRes = await api.tasks.getPublished();
      const assignmentsRes = await api.assignments.getMyActive();
      setTasks(tasksRes);
      setAssignments(assignmentsRes);
    } catch (err) {
      setError(err.message || "Failed to fetch dashboard data.");
    } finally {
      setIsLoading(false);
    }
  }

  // Calculate real metrics
  const activeCount = assignments.filter((a) => a.status === "ASSIGNED_NOT_STARTED" || a.status === "STARTED_NOT_COMPLETED").length;
  const completedCount = assignments.filter((a) => a.status === "COMPLETED").length;
  const availableCount = tasks.filter((t) => !assignments.some((a) => a.taskId === t.id)).length;

  const recentAssignments = assignments.slice(0, 4);
  const availableTasks = tasks.filter((t) => !assignments.some((a) => a.taskId === t.id)).slice(0, 4);

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs">
            <span>User</span>
            <span>/</span>
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>Dashboard</span>
          </div>
          <h1>Welcome back, {currentUser?.name || "Team Member"}</h1>
          <p>Track your active assignments and discover new tasks.</p>
        </div>
        <Link to="/user/tasks" style={{ textDecoration: "none" }}>
          <Button variant="primary" icon={<Layers size={16} />}>
            Browse Tasks
          </Button>
        </Link>
      </div>

      {/* Error alert */}
      {error && (
        <div className="alert-box danger" style={{ marginBottom: 24 }}>
          <AlertCircle size={16} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {/* Metrics Grid */}
      {isLoading ? (
        <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
          Loading metrics from database...
        </div>
      ) : (
        <div>
          <div className="stats-grid">
            <StatCard
              icon={<Layers size={20} />}
              label="Available Tasks"
              value={availableCount}
              color="accent"
              subtitle="Open for self-assignment"
            />
            <StatCard
              icon={<Clock size={20} />}
              label="Active Assignments"
              value={activeCount}
              color="warning"
              subtitle="In progress & assigned"
            />
            <StatCard
              icon={<CheckCircle size={20} />}
              label="Completed Tasks"
              value={completedCount}
              color="success"
              subtitle="Verified & submitted"
            />
          </div>

          {/* Two Column Grid */}
          <div className="dashboard-columns">
            {/* Left column: My Active Tasks */}
            <div className="card" style={{ padding: "28px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
                <div>
                  <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)" }}>My Active Tasks</h3>
                  <p style={{ fontSize: 13, color: "var(--color-text-secondary)", marginTop: 2 }}>Tasks currently assigned to you.</p>
                </div>
                <Link to="/user/my-tasks" style={{ textDecoration: "none" }}>
                  <Button variant="secondary" size="sm">View All</Button>
                </Link>
              </div>

              {recentAssignments.length === 0 ? (
                <div style={{ padding: "32px", textAlign: "center", backgroundColor: "var(--color-surface-soft)", borderRadius: 12, border: "1px dashed var(--color-border)", color: "var(--color-text-secondary)" }}>
                  No active task assignments. Browse available tasks to assign yourself.
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                  {recentAssignments.map((ass) => (
                    <div key={ass.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "16px", border: "1px solid var(--color-border)", borderRadius: 12, background: "var(--color-surface)", boxShadow: "var(--shadow-xs)" }}>
                      <div>
                        <h4 style={{ fontWeight: 700, fontSize: 15, marginBottom: 4 }}>
                          <Link to={`/user/my-tasks`} style={{ color: "var(--color-navy)", textDecoration: "none" }}>
                            {ass.task?.title}
                          </Link>
                        </h4>
                        <span style={{ fontSize: 12, color: "var(--color-text-secondary)" }}>
                          Assigned: {ass.assignedAt ? ass.assignedAt.split("T")[0] : "—"}
                        </span>
                      </div>
                      <Badge status={ass.status} />
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Right column: Available Tasks */}
            <div className="card" style={{ padding: "28px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
                <div>
                  <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)" }}>Discover Tasks</h3>
                  <p style={{ fontSize: 13, color: "var(--color-text-secondary)", marginTop: 2 }}>Open published tasks you can claim.</p>
                </div>
                <Link to="/user/tasks" style={{ textDecoration: "none" }}>
                  <Button variant="secondary" size="sm">Explore</Button>
                </Link>
              </div>

              {availableTasks.length === 0 ? (
                <div style={{ padding: "32px", textAlign: "center", backgroundColor: "var(--color-surface-soft)", borderRadius: 12, border: "1px dashed var(--color-border)", color: "var(--color-text-secondary)" }}>
                  No new published tasks available at the moment.
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                  {availableTasks.map((task) => (
                    <div key={task.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "16px", border: "1px solid var(--color-border)", borderRadius: 12, background: "var(--color-surface)", boxShadow: "var(--shadow-xs)" }}>
                      <div>
                        <h4 style={{ fontWeight: 700, fontSize: 15, marginBottom: 4, color: "var(--color-navy)" }}>
                          {task.title}
                        </h4>
                        <span style={{ fontSize: 12, color: "var(--color-text-secondary)" }}>
                          Due: {task.deadline || "No deadline"}
                        </span>
                      </div>
                      <Link to="/user/tasks" style={{ textDecoration: "none" }}>
                        <Button variant="secondary" size="sm" icon={<ArrowRight size={13} />}>
                          View Task
                        </Button>
                      </Link>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
