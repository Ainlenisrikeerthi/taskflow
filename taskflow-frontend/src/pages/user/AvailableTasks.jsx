import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { api } from "../../data/api";
import { Search, Calendar, Layers, ArrowRight, CheckCircle, AlertCircle, Eye, Check } from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";

export default function AvailableTasks() {
  const [tasks, setTasks] = useState([]);
  const [activeAssignments, setActiveAssignments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [assigningTaskId, setAssigningTaskId] = useState(null);

  // Search
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    fetchTasksAndAssignments();
  }, []);

  async function fetchTasksAndAssignments() {
    setIsLoading(true);
    setError("");
    try {
      const publishedTasks = await api.tasks.getPublished();
      const myActive = await api.assignments.getMyActive();
      setTasks(publishedTasks);
      setActiveAssignments(myActive);
    } catch (err) {
      setError(err.message || "Failed to load available tasks.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleAssign(taskId) {
    setError("");
    setSuccessMsg("");
    setAssigningTaskId(taskId);
    try {
      await api.assignments.assign(taskId);
      setSuccessMsg("Task assigned successfully! It now appears in 'My Tasks'.");
      await fetchTasksAndAssignments();
      setTimeout(() => setSuccessMsg(""), 4000);
    } catch (err) {
      if (err.status === 409) {
        setError("This task is already assigned to you.");
      } else {
        setError(err.message || "Failed to assign task.");
      }
      setTimeout(() => setError(""), 5000);
    } finally {
      setAssigningTaskId(null);
    }
  }

  // Filter logic
  const filteredTasks = tasks.filter((task) => {
    return (
      task.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      task.description?.toLowerCase().includes(searchTerm.toLowerCase())
    );
  });

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs">
            <span>User</span>
            <span>/</span>
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>Available Tasks</span>
          </div>
          <h1>Published Tasks</h1>
          <p>Discover available tasks created by administrators and self-assign work.</p>
        </div>
      </div>

      {/* Messages */}
      {error && (
        <div className="alert-box danger" style={{ marginBottom: 24 }}>
          <AlertCircle size={16} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}
      {successMsg && (
        <div className="alert-box success" style={{ marginBottom: 24 }}>
          <CheckCircle size={16} style={{ flexShrink: 0 }} />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Search Controls */}
      <div style={{ display: "flex", flexWrap: "wrap", gap: 16, justifyContent: "space-between", alignItems: "center", marginBottom: 28 }}>
        <div style={{ width: 320 }}>
          <Input
            prefixIcon={<Search size={16} />}
            placeholder="Search available tasks..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Tasks Catalog Grid */}
      {isLoading ? (
        <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
          Loading available tasks from server...
        </div>
      ) : filteredTasks.length === 0 ? (
        <div className="card" style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
          <Layers size={38} style={{ margin: "0 auto 16px", opacity: 0.3, color: "var(--color-electric-violet)" }} />
          <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)", marginBottom: 6 }}>No tasks available</h3>
          <p>There are currently no published tasks matching your criteria.</p>
        </div>
      ) : (
        <div className="task-card-grid">
          {filteredTasks.map((task) => {
            const assignment = activeAssignments.find((a) => a.taskId === task.id);
            const isAssigned = !!assignment;

            return (
              <div key={task.id} className="card card-hover" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
                    <Badge variant="purple" label="Published" />
                    <span style={{ fontSize: 13, color: "var(--color-text-secondary)", fontWeight: 500, display: "inline-flex", alignItems: "center", gap: 4 }}>
                      <Calendar size={13} /> {task.deadline || "No deadline"}
                    </span>
                  </div>

                  <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 8, color: "var(--color-navy)" }}>
                    {task.title}
                  </h3>

                  <p style={{ color: "var(--color-text-secondary)", fontSize: 14, lineHeight: 1.5, marginBottom: 16 }}>
                    {task.description}
                  </p>

                  {task.proofRequirement && (
                    <div style={{ fontSize: 13, color: "var(--color-text-secondary)", background: "#F8FAFC", padding: "8px 12px", borderRadius: 8, border: "1px solid #E2E8F0", marginBottom: 16 }}>
                      <strong style={{ color: "var(--color-navy)" }}>Proof Required:</strong> {task.proofRequirement}
                    </div>
                  )}
                </div>

                <div style={{ borderTop: "1px solid var(--color-border)", paddingTop: 14, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{ fontSize: 13, color: "var(--color-text-secondary)", fontWeight: 500 }}>
                    {task.assignedCount || 0} assigned
                  </span>

                  <div style={{ display: "flex", gap: 8 }}>
                    <Link to={`/user/tasks/${task.id}`} style={{ textDecoration: "none" }}>
                      <Button variant="secondary" size="sm" icon={<Eye size={13} />}>
                        View
                      </Button>
                    </Link>

                    {isAssigned ? (
                      <Link to="/user/my-tasks" style={{ textDecoration: "none" }}>
                        <Button variant="secondary" size="sm" icon={<Check size={13} />}>
                          My Tasks
                        </Button>
                      </Link>
                    ) : (
                      <Button
                        onClick={() => handleAssign(task.id)}
                        variant="primary"
                        size="sm"
                        loading={assigningTaskId === task.id}
                        icon={<ArrowRight size={13} />}
                      >
                        Assign
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
