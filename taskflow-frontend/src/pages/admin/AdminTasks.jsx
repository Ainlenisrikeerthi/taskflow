import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { api } from "../../data/api";
import { 
  ListTodo, 
  Search, 
  Plus, 
  Edit2, 
  Trash2,
  Calendar, 
  AlertCircle, 
  CheckCircle,
  X,
  Send,
  Save,
  MessageSquare
} from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";
import Modal from "../../components/ui/Modal";

export default function AdminTasks() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [tasks, setTasks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  
  // Search and filter
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Edit modal state
  const [editingTask, setEditingTask] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [deletingTaskId, setDeletingTaskId] = useState(null);
  const [publishingTaskId, setPublishingTaskId] = useState(null);

  // Form states (shared for create/edit)
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [instructions, setInstructions] = useState("");
  const [deadline, setDeadline] = useState("");
  const [proofRequirement, setProofRequirement] = useState("");
  const [status, setStatus] = useState("PUBLISHED");

  useEffect(() => {
    fetchTasks();
  }, []);

  // Detect ?action=new or ?create=true
  useEffect(() => {
    if (searchParams.get("action") === "new" || searchParams.get("create") === "true") {
      openCreateModal();
      searchParams.delete("action");
      searchParams.delete("create");
      setSearchParams(searchParams);
    }
  }, [searchParams]);

  async function fetchTasks() {
    setIsLoading(true);
    try {
      const res = await api.tasks.getAll();
      setTasks(res);
    } catch {
      setError("Failed to load tasks from server.");
    } finally {
      setIsLoading(false);
    }
  }

  function openEditModal(task) {
    setEditingTask(task);
    setTitle(task.title);
    setDescription(task.description);
    setInstructions(task.instructions || "");
    setDeadline(task.deadline);
    setProofRequirement(task.proofRequirement || "");
    setStatus(task.status);
  }

  function openCreateModal() {
    setEditingTask(null);
    setTitle("");
    setDescription("");
    setInstructions("");
    setDeadline("");
    setProofRequirement("");
    setStatus("PUBLISHED");
    setShowCreateModal(true);
  }

  async function handleSaveTask(e) {
    e.preventDefault();
    setError("");
    setSuccessMsg("");
    try {
      const taskData = { title, description, instructions, deadline, proofRequirement, status };
      
      if (editingTask) {
        await api.tasks.update(editingTask.id, taskData);
        setSuccessMsg(status === "PUBLISHED" ? "Task updated and published successfully!" : "Task draft updated successfully!");
        setEditingTask(null);
      } else {
        await api.tasks.create(taskData);
        setSuccessMsg(status === "PUBLISHED" ? "Task created and published successfully!" : "Task created as draft successfully!");
        setShowCreateModal(false);
      }
      fetchTasks();
      setTimeout(() => setSuccessMsg(""), 4000);
    } catch (err) {
      setError(err.message || "Failed to save task parameters.");
      setTimeout(() => setError(""), 4000);
    }
  }

  async function handlePublishTask(taskId) {
    setError("");
    setSuccessMsg("");
    setPublishingTaskId(taskId);
    try {
      await api.tasks.publish(taskId);
      setSuccessMsg("Task published successfully! Users can now see and assign it.");
      fetchTasks();
      setTimeout(() => setSuccessMsg(""), 4000);
    } catch (err) {
      setError(err.message || "Failed to publish task.");
      setTimeout(() => setError(""), 4000);
    } finally {
      setPublishingTaskId(null);
    }
  }

  async function handleDeleteTask(taskId) {
    setError("");
    setSuccessMsg("");
    try {
      await api.tasks.delete(taskId);
      setSuccessMsg("Task deleted successfully.");
      setDeletingTaskId(null);
      fetchTasks();
      setTimeout(() => setSuccessMsg(""), 4000);
    } catch {
      setError("Failed to delete task.");
      setDeletingTaskId(null);
      setTimeout(() => setError(""), 4000);
    }
  }

  // Filter tasks
  const filteredTasks = tasks.filter(task => {
    const matchesSearch = task.title.toLowerCase().includes(search.toLowerCase()) || 
                          task.description.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter ? task.status === statusFilter : true;
    return matchesSearch && matchesStatus;
  });

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs">
            <span>Tasks</span>
            <span>/</span>
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>Task Management</span>
          </div>
          <h1>Manage Tasks</h1>
          <p>Create, edit, and publish tasks for team members.</p>
        </div>
        <Button onClick={openCreateModal} variant="primary" icon={<Plus size={16} />}>
          Create Task
        </Button>
      </div>

      {/* Alert Banners */}
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

      {/* Filter and Search */}
      <div style={{ display: "flex", flexWrap: "wrap", gap: 16, justifyContent: "space-between", alignItems: "center", marginBottom: 28 }}>
        <div style={{ display: "flex", gap: 6 }}>
          <Button 
            onClick={() => setStatusFilter("")} 
            variant={statusFilter === "" ? "primary" : "secondary"}
            size="sm"
          >
            All States
          </Button>
          <Button 
            onClick={() => setStatusFilter("PUBLISHED")} 
            variant={statusFilter === "PUBLISHED" ? "primary" : "secondary"}
            size="sm"
          >
            Published
          </Button>
          <Button 
            onClick={() => setStatusFilter("DRAFT")} 
            variant={statusFilter === "DRAFT" ? "primary" : "secondary"}
            size="sm"
          >
            Drafts
          </Button>
        </div>

        <div style={{ width: 280 }}>
          <Input
            prefixIcon={<Search size={16} />}
            placeholder="Search tasks..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Tasks Catalog Grid */}
      {isLoading ? (
        <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
          Loading task catalog from backend...
        </div>
      ) : filteredTasks.length === 0 ? (
        <div className="card" style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
          No published tasks found.
        </div>
      ) : (
        <div className="task-card-grid">
          {filteredTasks.map(task => (
            <div key={task.id} className="card card-hover" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
                  <Badge status={task.status} />
                  <span style={{ fontSize: 13, color: "var(--color-text-secondary)", fontWeight: 500, display: "inline-flex", alignItems: "center", gap: 4 }}>
                    <Calendar size={13} /> {task.deadline || "No deadline"}
                  </span>
                </div>

                <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 8, color: "var(--color-navy)" }}>{task.title}</h3>
                
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
                  {task.assignedCount || 0} assigned · {task.completedCount || 0} completed
                </span>
                
                <div style={{ display: "flex", gap: 8 }}>
                  {task.status === "DRAFT" && (
                    <Button
                      onClick={() => handlePublishTask(task.id)}
                      variant="primary"
                      size="sm"
                      loading={publishingTaskId === task.id}
                      icon={<Send size={13} />}
                    >
                      Publish
                    </Button>
                  )}
                  <Button
                    onClick={() => navigate(`/admin/tasks/${task.id}`)}
                    variant="secondary"
                    size="sm"
                    icon={<MessageSquare size={13} />}
                  >
                    Discuss
                  </Button>
                  <Button 
                    onClick={() => openEditModal(task)}
                    variant="secondary"
                    size="sm"
                    icon={<Edit2 size={13} />}
                  >
                    Edit
                  </Button>
                  <Button 
                    onClick={() => setDeletingTaskId(task.id)}
                    variant="danger"
                    size="sm"
                    icon={<Trash2 size={13} />}
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Task Creation & Editing Modal (Matching Reference Image 4 Layout) */}
      <Modal
        open={Boolean(editingTask || showCreateModal)}
        onClose={() => { setEditingTask(null); setShowCreateModal(false); }}
        title={editingTask ? `Edit Task: ${editingTask.title}` : "Create New Task"}
        size="lg"
      >
        <form onSubmit={handleSaveTask} style={{ display: "flex", flexDirection: "column", gap: 18 }}>
          <Input
            label="Task Title"
            placeholder="e.g. Update Q3 Marketing Strategy"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />

          <Input
            label="Task Description"
            as="textarea"
            placeholder="Provide detailed instructions and context for this task..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
          />

          <Input
            label="Detailed Steps / Instructions (Optional)"
            as="textarea"
            placeholder="Step-by-step instructions for team members..."
            value={instructions}
            onChange={(e) => setInstructions(e.target.value)}
          />

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
            <Input
              label="Deadline"
              type="date"
              value={deadline}
              onChange={(e) => setDeadline(e.target.value)}
              required
            />

            <Input
              label="Publish Status"
              as="select"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              <option value="PUBLISHED">Published (Active)</option>
              <option value="DRAFT">Save as Draft</option>
            </Input>
          </div>

          <Input
            label="Proof Requirement Description"
            placeholder="e.g. Provide completed document URL or PR link"
            value={proofRequirement}
            onChange={(e) => setProofRequirement(e.target.value)}
          />

          <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 12 }}>
            <Button 
              type="button" 
              onClick={() => { setEditingTask(null); setShowCreateModal(false); }}
              variant="secondary"
            >
              Cancel
            </Button>
            <Button 
              type="submit" 
              variant="primary"
              icon={status === "PUBLISHED" ? <Send size={16} /> : <Save size={16} />}
            >
              {status === "PUBLISHED" ? "Publish Task" : "Save Draft"}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={Boolean(deletingTaskId)}
        onClose={() => setDeletingTaskId(null)}
        title="Confirm Task Deletion"
        size="sm"
      >
        <div style={{ textAlign: "center", padding: "10px 0 20px" }}>
          <div style={{ width: 56, height: 56, borderRadius: "50%", background: "#FEE2E2", color: "#EF4444", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 16px" }}>
            <Trash2 size={24} />
          </div>
          <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)", marginBottom: 8 }}>Delete Task?</h3>
          <p style={{ color: "var(--color-text-secondary)", fontSize: 14, lineHeight: 1.5, marginBottom: 24 }}>
            Are you sure you want to delete this task? This action will remove the task record from the database.
          </p>
          <div style={{ display: "flex", gap: 12, justifyContent: "center" }}>
            <Button onClick={() => setDeletingTaskId(null)} variant="secondary">Cancel</Button>
            <Button onClick={() => handleDeleteTask(deletingTaskId)} variant="danger">
              Yes, Delete
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
