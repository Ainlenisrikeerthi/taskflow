import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { api } from "../../data/api";
import { ClipboardList, ExternalLink, AlertCircle, CheckCircle, Eye, Trash2, Send } from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";
import Modal from "../../components/ui/Modal";

export default function MyTasks() {
  const [assignments, setAssignments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [confirmUnassign, setConfirmUnassign] = useState(null);

  useEffect(() => {
    fetchMyTasks();
  }, []);

  async function fetchMyTasks() {
    setIsLoading(true);
    setError("");
    try {
      const data = await api.assignments.getMyActive();
      setAssignments(data);
    } catch (err) {
      setError(err.message || "Failed to load active assignments.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleStatusAndProofUpdate(assignmentId, newStatus, currentProofUrl) {
    setError("");
    setSuccessMsg("");

    if (newStatus === "COMPLETED" && (!currentProofUrl || !currentProofUrl.trim())) {
      setError("A valid proof URL is required when marking a task as Completed.");
      return;
    }

    try {
      await api.assignments.updateStatus(assignmentId, newStatus, currentProofUrl);
      setSuccessMsg("Task status updated successfully.");
      await fetchMyTasks();
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      setError(err.message || "Failed to update status.");
      await fetchMyTasks();
    }
  }

  function handleProofChange(assignmentId, newProofUrl) {
    setAssignments((prev) =>
      prev.map((a) => (a.id === assignmentId ? { ...a, proofUrl: newProofUrl } : a))
    );
  }

  async function handleSaveProof(ass) {
    setError("");
    setSuccessMsg("");
    try {
      await api.assignments.updateStatus(ass.id, ass.status, ass.proofUrl);
      setSuccessMsg("Proof URL saved successfully.");
      setTimeout(() => setSuccessMsg(""), 3000);
      await fetchMyTasks();
    } catch (err) {
      setError(err.message || "Failed to save proof URL.");
    }
  }

  async function handleConfirmUnassign() {
    if (!confirmUnassign) return;
    setError("");
    setSuccessMsg("");
    try {
      await api.assignments.unassign(confirmUnassign.id);
      setSuccessMsg("Successfully unassigned from task.");
      setConfirmUnassign(null);
      await fetchMyTasks();
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      setError(err.message || "Failed to unassign.");
      setConfirmUnassign(null);
    }
  }

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs">
            <span>User</span>
            <span>/</span>
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>My Tasks</span>
          </div>
          <h1>My Assigned Tasks</h1>
          <p>General tasks use proof URLs. Coding tasks open a code workspace with test execution and AI scoring.</p>
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

      {/* Assignments Table Card */}
      <div className="card" style={{ padding: "28px" }}>
        {isLoading ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            Loading assigned tasks from backend...
          </div>
        ) : assignments.length === 0 ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            <ClipboardList size={38} style={{ margin: "0 auto 16px", opacity: 0.3, color: "var(--color-electric-violet)" }} />
            <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)", marginBottom: 6 }}>No active assignments</h3>
            <p style={{ marginBottom: 20 }}>You are not assigned to any tasks right now.</p>
            <Link to="/user/tasks" style={{ textDecoration: "none" }}>
              <Button variant="primary">Browse Available Tasks</Button>
            </Link>
          </div>
        ) : (
          <div className="premium-table-wrapper">
            <table className="premium-table">
              <thead>
                <tr>
                  <th>Task Title</th>
                  <th>Assigned Date</th>
                  <th>Status</th>
                  <th>Proof Link</th>
                  <th style={{ textAlign: "center" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((ass) => (
                  <tr key={ass.id}>
                    <td data-label="Task Title" style={{ fontWeight: 700, color: "var(--color-navy)" }}>
                      <Link to={ass.task?.taskType === "CODING" ? `/user/coding/${ass.task?.id}` : `/user/tasks/${ass.task?.id}`} style={{ color: "var(--color-navy)", textDecoration: "none" }}>
                        {ass.task?.title}
                      </Link>
                    </td>
                    <td data-label="Assigned Date" style={{ color: "var(--color-text-secondary)", fontSize: 13 }}>
                      {ass.assignedAt ? ass.assignedAt.split("T")[0] : "—"}
                    </td>
                    <td data-label="Status">
                      {ass.task?.taskType === "CODING" ? (
                        <Badge status={ass.status} />
                      ) : (
                        <select
                          className="input-control"
                          style={{ padding: "6px 12px", fontSize: 13, width: 210, cursor: "pointer" }}
                          value={ass.status}
                          onChange={(e) => handleStatusAndProofUpdate(ass.id, e.target.value, ass.proofUrl)}
                        >
                          <option value="ASSIGNED_NOT_STARTED">Assigned / Not Started</option>
                          <option value="STARTED_NOT_COMPLETED">Started but Not Completed</option>
                          <option value="COMPLETED">Completed</option>
                        </select>
                      )}
                    </td>
                    <td data-label="Proof Link">
                      {ass.task?.taskType === "CODING" ? (
                        <span style={{ fontSize: 13, fontWeight: 700, color: "var(--color-electric-violet)" }}>Code submission required</span>
                      ) : (
                        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                          <input
                            type="url"
                            className="input-control"
                            style={{ padding: "6px 12px", fontSize: 13, minWidth: 220 }}
                            placeholder="Paste proof URL (e.g. GitHub link)"
                            value={ass.proofUrl || ""}
                            onChange={(e) => handleProofChange(ass.id, e.target.value)}
                            onBlur={() => handleSaveProof(ass)}
                          />
                          {ass.proofUrl && (
                            <a href={ass.proofUrl} target="_blank" rel="noreferrer" style={{ textDecoration: "none" }}>
                              <Button variant="secondary" size="sm" icon={<ExternalLink size={13} />} />
                            </a>
                          )}
                        </div>
                      )}
                    </td>
                    <td data-label="Actions" style={{ textAlign: "center" }}>
                      <div style={{ display: "flex", gap: 8, justifyContent: "center" }}>
                        <Link to={ass.task?.taskType === "CODING" ? `/user/coding/${ass.task?.id}` : `/user/tasks/${ass.task?.id}`} style={{ textDecoration: "none" }}>
                          <Button variant="secondary" size="sm" icon={<Eye size={12} />}>
                            {ass.task?.taskType === "CODING" ? "Open Code" : "View"}
                          </Button>
                        </Link>
                        <Button
                          onClick={() => setConfirmUnassign(ass)}
                          variant="danger"
                          size="sm"
                          icon={<Trash2 size={12} />}
                        >
                          Unassign
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Confirmation Modal */}
      <Modal
        open={Boolean(confirmUnassign)}
        onClose={() => setConfirmUnassign(null)}
        title="Confirm Unassign"
        size="sm"
      >
        <div style={{ textAlign: "center", padding: "10px 0 20px" }}>
          <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)", marginBottom: 8 }}>Unassign from Task?</h3>
          <p style={{ color: "var(--color-text-secondary)", fontSize: 14, lineHeight: 1.5, marginBottom: 24 }}>
            Are you sure you want to unassign yourself from <strong>"{confirmUnassign?.task?.title}"</strong>?
          </p>
          <div style={{ display: "flex", gap: 12, justifyContent: "center" }}>
            <Button onClick={() => setConfirmUnassign(null)} variant="secondary">Cancel</Button>
            <Button onClick={handleConfirmUnassign} variant="danger">
              Yes, Unassign
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
