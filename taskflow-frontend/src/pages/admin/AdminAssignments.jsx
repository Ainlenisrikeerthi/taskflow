import { useState, useEffect } from "react";
import { api } from "../../data/api";
import { ClipboardList, Search, Filter, ExternalLink, Trash2, CheckCircle, AlertCircle, X } from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";
import Modal from "../../components/ui/Modal";

export default function AdminAssignments() {
  const [assignments, setAssignments] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Filters & Search
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [taskFilter, setTaskFilter] = useState("");

  // Remove Modal State
  const [showRemoveModal, setShowRemoveModal] = useState(null);
  const [removeReason, setRemoveReason] = useState("");

  useEffect(() => {
    fetchTasks();
  }, []);

  useEffect(() => {
    fetchAssignments();
  }, [searchTerm, statusFilter, taskFilter]);

  async function fetchTasks() {
    try {
      const data = await api.tasks.getAll();
      setTasks(data);
    } catch (err) {
      console.error("Failed to load tasks for filter", err);
    }
  }

  async function fetchAssignments() {
    setIsLoading(true);
    setError("");
    try {
      const data = await api.admin.getAssignments(searchTerm, statusFilter, taskFilter);
      setAssignments(data);
    } catch (err) {
      setError(err.message || "Failed to load assignments.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleRemoveAssignment() {
    if (!showRemoveModal) return;
    setError("");
    setSuccessMsg("");
    try {
      await api.admin.removeAssignment(showRemoveModal.id, removeReason);
      setSuccessMsg(`Assignment for "${showRemoveModal.task?.title}" assigned to "${showRemoveModal.user?.name}" successfully removed.`);
      setShowRemoveModal(null);
      setRemoveReason("");
      await fetchAssignments();
      setTimeout(() => setSuccessMsg(""), 4000);
    } catch (err) {
      setError(err.message || "Failed to remove assignment.");
      setShowRemoveModal(null);
      setRemoveReason("");
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
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>Assignments</span>
          </div>
          <h1>User Assignments Registry</h1>
          <p>Search, filter, and audit all active and past assignments across team members.</p>
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

      {/* Table Card */}
      <div className="card" style={{ padding: "28px" }}>
        {/* Filter Toolbar */}
        <div style={{ display: "flex", flexWrap: "wrap", gap: 16, marginBottom: 24, alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ width: 280 }}>
            <Input
              prefixIcon={<Search size={16} />}
              placeholder="Search by user name or email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <Filter size={14} style={{ color: "var(--color-text-secondary)" }} />
              <select
                className="input-control"
                style={{ padding: "8px 12px", fontSize: 13, width: 180 }}
                value={taskFilter}
                onChange={(e) => setTaskFilter(e.target.value)}
              >
                <option value="">All Tasks</option>
                {tasks.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.title}
                  </option>
                ))}
              </select>
            </div>

            <select
              className="input-control"
              style={{ padding: "8px 12px", fontSize: 13, width: 180 }}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="ASSIGNED_NOT_STARTED">Not Started</option>
              <option value="STARTED_NOT_COMPLETED">In Progress</option>
              <option value="COMPLETED">Completed</option>
              <option value="REMOVED">Removed</option>
            </select>
          </div>
        </div>

        {/* Master Table */}
        {isLoading ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            Loading assignment records...
          </div>
        ) : assignments.length === 0 ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            No assignments found matching criteria.
          </div>
        ) : (
          <div className="premium-table-wrapper">
            <table className="premium-table">
              <thead>
                <tr>
                  <th>User Details</th>
                  <th>Task Title</th>
                  <th>Assigned Date</th>
                  <th>Status</th>
                  <th>Submitted Proof</th>
                  <th style={{ textAlign: "center" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((ass) => (
                  <tr key={ass.id}>
                    <td data-label="User Details">
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div className="avatar avatar-sm">
                          {ass.user?.name?.charAt(0).toUpperCase() || "U"}
                        </div>
                        <div style={{ display: "flex", flexDirection: "column" }}>
                          <span style={{ fontWeight: 700, fontSize: 14, color: "var(--color-navy)" }}>
                            {ass.user?.name}
                          </span>
                          <span style={{ color: "var(--color-text-secondary)", fontSize: 12 }}>
                            {ass.user?.email}
                          </span>
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
                          style={{
                            display: "inline-flex",
                            alignItems: "center",
                            gap: 4,
                            color: "var(--color-electric-violet)",
                            fontWeight: 600,
                            fontSize: 13,
                            textDecoration: "none"
                          }}
                        >
                          View Proof <ExternalLink size={13} />
                        </a>
                      ) : (
                        <span style={{ color: "var(--color-text-muted)", fontSize: 13 }}>None</span>
                      )}
                    </td>
                    <td data-label="Actions" style={{ textAlign: "center" }}>
                      {ass.status !== "REMOVED" ? (
                        <Button
                          onClick={() => setShowRemoveModal(ass)}
                          variant="danger"
                          size="sm"
                          icon={<Trash2 size={12} />}
                        >
                          Remove
                        </Button>
                      ) : (
                        <span style={{ color: "var(--color-text-muted)", fontSize: 12, fontStyle: "italic" }}>
                          Archived
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Remove Modal */}
      <Modal
        open={Boolean(showRemoveModal)}
        onClose={() => { setShowRemoveModal(null); setRemoveReason(""); }}
        title="Remove User Assignment"
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <p style={{ color: "var(--color-text-secondary)", fontSize: 14, lineHeight: 1.5 }}>
            You are removing <strong>{showRemoveModal?.user?.name}</strong>'s assignment for <strong>"{showRemoveModal?.task?.title}"</strong>. An automated notification email will be dispatched.
          </p>

          <Input
            label="Reason for Removal (Optional)"
            as="textarea"
            placeholder="e.g. Task cancelled or reassigned to another team member..."
            value={removeReason}
            onChange={(e) => setRemoveReason(e.target.value)}
          />

          <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 12 }}>
            <Button
              onClick={() => { setShowRemoveModal(null); setRemoveReason(""); }}
              variant="secondary"
            >
              Cancel
            </Button>
            <Button
              onClick={handleRemoveAssignment}
              variant="danger"
            >
              Confirm Removal
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
