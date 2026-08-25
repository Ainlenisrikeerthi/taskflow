import { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../../data/api";
import { useAuth } from "../../components/common/AuthContext";
import { Calendar, Layers, ArrowLeft, CheckCircle, AlertCircle, Trash2, ExternalLink } from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";
import Modal from "../../components/ui/Modal";
import CommentsPanel from "../../components/task/CommentsPanel";

export default function TaskDetails() {
  const { id } = useParams();
  const { isAdmin } = useAuth();

  const [task, setTask] = useState(null);
  const [assignment, setAssignment] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [confirmUnassign, setConfirmUnassign] = useState(false);

  // Status & Proof Form
  const [status, setStatus] = useState("ASSIGNED_NOT_STARTED");
  const [proofUrl, setProofUrl] = useState("");

  useEffect(() => {
    fetchTaskAndAssignment();
  }, [id]);

  async function fetchTaskAndAssignment() {
    setIsLoading(true);
    setError("");
    try {
      const taskData = await api.tasks.getById(id);
      setTask(taskData);

      if (!isAdmin) {
        const myActive = await api.assignments.getMyActive();
        const found = myActive.find((a) => a.taskId === Number(id));
        setAssignment(found || null);
        if (found) {
          setStatus(found.status);
          setProofUrl(found.proofUrl || "");
        }
      }
    } catch (err) {
      setError(err.message || "Failed to load task details.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleAssign() {
    setError("");
    setSuccessMsg("");
    try {
      const newAss = await api.assignments.assign(task.id);
      setSuccessMsg("Task self-assigned successfully!");
      setAssignment(newAss);
      setStatus(newAss.status);
      setProofUrl(newAss.proofUrl || "");
      await fetchTaskAndAssignment();
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      if (err.status === 409) {
        setError("This task is already assigned to you.");
      } else {
        setError(err.message || "Failed to assign task.");
      }
    }
  }

  async function handleUpdateProgress(e) {
    e.preventDefault();
    setError("");
    setSuccessMsg("");

    if (status === "COMPLETED" && (!proofUrl || !proofUrl.trim())) {
      setError("A valid proof URL is required to complete this task.");
      return;
    }

    try {
      await api.assignments.updateStatus(assignment.id, status, proofUrl);
      setSuccessMsg("Task progress saved successfully.");
      await fetchTaskAndAssignment();
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      setError(err.message || "Failed to update progress.");
    }
  }

  async function handleUnassign() {
    setError("");
    setSuccessMsg("");
    try {
      await api.assignments.unassign(assignment.id);
      setSuccessMsg("Successfully unassigned from task.");
      setAssignment(null);
      setConfirmUnassign(false);
      await fetchTaskAndAssignment();
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      setError(err.message || "Failed to unassign.");
      setConfirmUnassign(false);
    }
  }

  if (isLoading) {
    return (
      <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
        Loading task specification...
      </div>
    );
  }

  if (error && !task) {
    return (
      <div className="alert-box danger" style={{ margin: "24px 0" }}>
        <AlertCircle size={16} style={{ flexShrink: 0 }} />
        <span>{error}</span>
      </div>
    );
  }

  if (!task) {
    return (
      <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
        Task not found.
      </div>
    );
  }

  return (
    <div>
      {/* Back Navigation */}
      <Link to={isAdmin ? "/admin/tasks" : "/user/tasks"} style={{ textDecoration: "none" }}>
        <Button variant="secondary" size="sm" icon={<ArrowLeft size={14} />} style={{ marginBottom: 24 }}>
          Back to Catalog
        </Button>
      </Link>

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

      <div style={{ display: "grid", gridTemplateColumns: isAdmin ? "1fr" : "2fr 1fr", gap: 24, alignItems: "start" }}>
        {/* Left Side: Task Specifications */}
        <div className="card" style={{ padding: "32px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
            <Badge variant="purple" label={task.status} />
            <span style={{ fontSize: 13, color: "var(--color-text-secondary)", fontWeight: 500, display: "inline-flex", alignItems: "center", gap: 4 }}>
              <Calendar size={13} /> Due {task.deadline || "No deadline"}
            </span>
          </div>

          <h1 style={{ fontSize: 24, fontWeight: 800, color: "var(--color-navy)", marginBottom: 16 }}>
            {task.title}
          </h1>

          <div style={{ borderBottom: "1px solid var(--color-border)", paddingBottom: 20, marginBottom: 20 }}>
            <strong style={{ display: "block", color: "var(--color-text-secondary)", fontSize: 12, textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: 8 }}>
              Task Description
            </strong>
            <p style={{ color: "var(--color-navy)", fontSize: 15, lineHeight: 1.6 }}>
              {task.description}
            </p>
          </div>

          {task.instructions && (
            <div style={{ borderBottom: "1px solid var(--color-border)", paddingBottom: 20, marginBottom: 20 }}>
              <strong style={{ display: "block", color: "var(--color-text-secondary)", fontSize: 12, textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: 8 }}>
                Step-by-step Instructions
              </strong>
              <div style={{ 
                backgroundColor: "var(--color-surface-soft)", 
                border: "1px solid #E2E8F0",
                padding: "18px", 
                borderRadius: 12, 
                fontSize: 14, 
                lineHeight: 1.6,
                color: "var(--color-navy)"
              }}>
                {task.instructions}
              </div>
            </div>
          )}

          {task.proofRequirement && (
            <div>
              <strong style={{ display: "block", color: "var(--color-text-secondary)", fontSize: 12, textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: 6 }}>
                Required Proof of Completion
              </strong>
              <p style={{ color: "var(--color-navy)", fontSize: 14, fontWeight: 600 }}>
                {task.proofRequirement}
              </p>
            </div>
          )}
        </div>

        {/* Right Side: Assignment Control (Only for User) */}
        {!isAdmin && (
          <div className="card" style={{ padding: "28px" }}>
            <h3 style={{ fontSize: 18, fontWeight: 800, marginBottom: 16, color: "var(--color-navy)" }}>
              Execution Control
            </h3>

            {!assignment ? (
              <div>
                <p style={{ color: "var(--color-text-secondary)", fontSize: 14, marginBottom: 20, lineHeight: 1.5 }}>
                  You are not currently assigned to this task. Click below to self-assign and start work.
                </p>
                <Button onClick={handleAssign} variant="primary" fullWidth>
                  Assign Task
                </Button>
              </div>
            ) : (
              <form onSubmit={handleUpdateProgress} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                <Input
                  label="Execution Status"
                  as="select"
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                >
                  <option value="ASSIGNED_NOT_STARTED">Assigned / Not Started</option>
                  <option value="STARTED_NOT_COMPLETED">Started but Not Completed</option>
                  <option value="COMPLETED">Completed</option>
                </Input>

                <div>
                  <Input
                    label="Proof URL"
                    type="url"
                    placeholder="Paste URL (e.g. GitHub link)"
                    value={proofUrl}
                    onChange={(e) => setProofUrl(e.target.value)}
                  />
                  {proofUrl && (
                    <a
                      href={proofUrl}
                      target="_blank"
                      rel="noreferrer"
                      style={{ display: "inline-flex", alignItems: "center", gap: 4, marginTop: 6, fontSize: 13, color: "var(--color-electric-violet)", fontWeight: 600, textDecoration: "none" }}
                    >
                      Open Link <ExternalLink size={13} />
                    </a>
                  )}
                </div>

                <Button type="submit" variant="primary" fullWidth style={{ marginTop: 8 }}>
                  Save Progress
                </Button>

                <div style={{ borderTop: "1px solid var(--color-border)", paddingTop: 16, marginTop: 8 }}>
                  <Button
                    type="button"
                    onClick={() => setConfirmUnassign(true)}
                    variant="danger"
                    fullWidth
                    icon={<Trash2 size={13} />}
                  >
                    Unassign Task
                  </Button>
                </div>
              </form>
            )}
          </div>
        )}
      </div>

      <div style={{ marginTop: 24 }}><CommentsPanel taskId={Number(id)} /></div>

      {/* Confirmation Modal */}
      <Modal
        open={confirmUnassign}
        onClose={() => setConfirmUnassign(false)}
        title="Confirm Unassign"
        size="sm"
      >
        <div style={{ textAlign: "center", padding: "10px 0 20px" }}>
          <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)", marginBottom: 8 }}>Unassign from Task?</h3>
          <p style={{ color: "var(--color-text-secondary)", fontSize: 14, lineHeight: 1.5, marginBottom: 24 }}>
            Are you sure you want to unassign yourself from <strong>"{task.title}"</strong>?
          </p>
          <div style={{ display: "flex", gap: 12, justifyContent: "center" }}>
            <Button onClick={() => setConfirmUnassign(false)} variant="secondary">Cancel</Button>
            <Button onClick={handleUnassign} variant="danger">
              Confirm Unassign
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
