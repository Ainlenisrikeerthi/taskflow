import { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../../data/api";
import { Users, Calendar, ArrowLeft, Mail, Shield, CheckCircle, AlertCircle, Trash2, ExternalLink, X } from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";
import Modal from "../../components/ui/Modal";

export default function UserActivity() {
  const { id } = useParams();
  const [user, setUser] = useState(null);
  const [assignments, setAssignments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Remove Modal State
  const [showRemoveModal, setShowRemoveModal] = useState(null);
  const [removeReason, setRemoveReason] = useState("");

  useEffect(() => {
    fetchUserAndActivity();
  }, [id]);

  async function fetchUserAndActivity() {
    setIsLoading(true);
    setError("");
    try {
      const usersList = await api.admin.getUsers();
      const foundUser = usersList.find((u) => u.id === Number(id));
      setUser(foundUser || null);

      const activityData = await api.admin.getAssignmentsByUserId(id);
      setAssignments(activityData);
    } catch (err) {
      setError(err.message || "Failed to load user activity data.");
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
      setSuccessMsg(`Assignment removed successfully.`);
      setShowRemoveModal(null);
      setRemoveReason("");
      await fetchUserAndActivity();
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      setError(err.message || "Failed to remove assignment.");
      setShowRemoveModal(null);
      setRemoveReason("");
    }
  }

  const totalAssigned = assignments.length;
  const completed = assignments.filter((a) => a.status === "COMPLETED").length;
  const inProgress = assignments.filter((a) => a.status === "STARTED_NOT_COMPLETED").length;
  const removed = assignments.filter((a) => a.status === "REMOVED").length;

  const initials = user?.name?.charAt(0).toUpperCase() || "U";

  if (isLoading) {
    return (
      <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
        Loading activity records...
      </div>
    );
  }

  if (error && !user) {
    return (
      <div className="alert-box danger" style={{ margin: "24px 0" }}>
        <AlertCircle size={16} style={{ flexShrink: 0 }} />
        <span>{error}</span>
      </div>
    );
  }

  if (!user) {
    return (
      <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
        User record not found.
      </div>
    );
  }

  return (
    <div>
      {/* Navigation */}
      <Link to="/admin/users" style={{ textDecoration: "none" }}>
        <Button variant="secondary" size="sm" icon={<ArrowLeft size={14} />} style={{ marginBottom: 24 }}>
          Back to Users Directory
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

      {/* User Summary Card */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: 24, alignItems: "start", marginBottom: 32 }}>
        <div className="card" style={{ padding: "28px", textAlign: "center" }}>
          <div
            style={{
              width: 72,
              height: 72,
              borderRadius: "50%",
              margin: "0 auto 16px",
              fontSize: 28,
              fontWeight: 800,
              color: "white",
              background: "linear-gradient(135deg, var(--color-electric-violet) 0%, var(--color-indigo) 100%)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {initials}
          </div>
          <h2 style={{ fontSize: 20, fontWeight: 800, color: "var(--color-navy)", marginBottom: 4 }}>{user.name}</h2>
          <Badge variant={user.role === "ADMIN" ? "danger" : "purple"} label={user.role} />

          <div style={{ marginTop: 20, borderTop: "1px solid var(--color-border)", paddingTop: 16, textAlign: "left", fontSize: 13 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--color-text-secondary)", marginBottom: 8 }}>
              <Mail size={14} color="var(--color-electric-violet)" /> <span>{user.email}</span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--color-text-secondary)" }}>
              <Calendar size={14} color="var(--color-electric-violet)" /> <span>Joined {user.createdAt ? user.createdAt.split("T")[0] : "—"}</span>
            </div>
          </div>
        </div>

        {/* User Performance Summary */}
        <div className="card" style={{ padding: "28px", height: "100%" }}>
          <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)", marginBottom: 20 }}>Performance Summary</h3>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))", gap: 16 }}>
            <div style={{ background: "#F8FAFC", border: "1px solid #E2E8F0", padding: "16px", borderRadius: 12 }}>
              <span style={{ fontSize: 12, color: "var(--color-text-secondary)", fontWeight: 600, textTransform: "uppercase" }}>Total Tasks</span>
              <strong style={{ display: "block", fontSize: 28, marginTop: 4, color: "var(--color-navy)", fontWeight: 800 }}>{totalAssigned}</strong>
            </div>
            <div style={{ background: "#ECFDF5", border: "1px solid #A7F3D0", padding: "16px", borderRadius: 12 }}>
              <span style={{ fontSize: 12, color: "#047857", fontWeight: 600, textTransform: "uppercase" }}>Completed</span>
              <strong style={{ display: "block", fontSize: 28, marginTop: 4, color: "#047857", fontWeight: 800 }}>{completed}</strong>
            </div>
            <div style={{ background: "#FEF3C7", border: "1px solid #FDE68A", padding: "16px", borderRadius: 12 }}>
              <span style={{ fontSize: 12, color: "#B45309", fontWeight: 600, textTransform: "uppercase" }}>In Progress</span>
              <strong style={{ display: "block", fontSize: 28, marginTop: 4, color: "#B45309", fontWeight: 800 }}>{inProgress}</strong>
            </div>
            <div style={{ background: "#FEE2E2", border: "1px solid #FECACA", padding: "16px", borderRadius: 12 }}>
              <span style={{ fontSize: 12, color: "#B91C1C", fontWeight: 600, textTransform: "uppercase" }}>Removed</span>
              <strong style={{ display: "block", fontSize: 28, marginTop: 4, color: "#B91C1C", fontWeight: 800 }}>{removed}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* Activity Logs Table */}
      <div className="card" style={{ padding: "28px" }}>
        <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)", marginBottom: 20 }}>Assignment Activity Logs</h3>

        {assignments.length === 0 ? (
          <div style={{ padding: "40px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            No task assignment activity recorded for this user.
          </div>
        ) : (
          <div className="premium-table-wrapper">
            <table className="premium-table">
              <thead>
                <tr>
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
                    <td data-label="Task Title" style={{ fontWeight: 600, color: "var(--color-navy)" }}>
                      {ass.task?.title}
                    </td>
                    <td data-label="Assigned Date" style={{ color: "var(--color-text-secondary)", fontSize: 13 }}>
                      {ass.assignedAt ? ass.assignedAt.split("T")[0] : "—"}
                    </td>
                    <td data-label="Status">
                      <div>
                        <Badge status={ass.status} />
                        {ass.status === "REMOVED" && ass.removedReason && (
                          <div style={{ fontSize: 11, color: "#EF4444", marginTop: 4 }}>
                            Log: {ass.removedReason}
                          </div>
                        )}
                      </div>
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
            You are removing <strong>{user?.name}</strong>'s assignment for <strong>"{showRemoveModal?.task?.title}"</strong>. An email notification will be dispatched.
          </p>

          <Input
            label="Reason for Removal (Optional)"
            as="textarea"
            placeholder="Provide a brief explanation..."
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
