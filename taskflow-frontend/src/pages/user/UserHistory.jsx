import { useState, useEffect } from "react";
import { api } from "../../data/api";
import { Calendar, CheckCircle2, AlertTriangle, ExternalLink, Search } from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";

export default function UserHistory() {
  const [history, setHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 8;

  useEffect(() => {
    fetchHistory();
  }, []);

  async function fetchHistory() {
    setIsLoading(true);
    try {
      const res = await api.assignments.getMyAll();
      setHistory(res);
    } catch (err) {
      console.error("Failed to load task history", err);
    } finally {
      setIsLoading(false);
    }
  }

  // Filter history logic
  const filteredHistory = history.filter(item => {
    const matchesSearch = item.task?.title?.toLowerCase().includes(search.toLowerCase());
    if (!matchesSearch) return false;

    if (filter === "completed") return item.status === "COMPLETED";
    if (filter === "removed") return item.status === "REMOVED";
    if (filter === "month") {
      const assignedDate = new Date(item.assignedDate || item.assignedAt);
      const now = new Date();
      return assignedDate.getMonth() === now.getMonth() && assignedDate.getFullYear() === now.getFullYear();
    }
    return true;
  });

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = filteredHistory.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(filteredHistory.length / itemsPerPage);

  function formatDate(dateStr) {
    if (!dateStr) return "—";
    const date = new Date(dateStr);
    return date.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
  }

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs">
            <span>User</span>
            <span>/</span>
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>Task History</span>
          </div>
          <h1>My Task History</h1>
          <p>Comprehensive audit record of all past task assignments, completions, and status changes.</p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div style={{ display: "flex", flexWrap: "wrap", gap: 16, justifyContent: "space-between", alignItems: "center", marginBottom: 28 }}>
        <div style={{ display: "flex", gap: 6 }}>
          <Button 
            onClick={() => { setFilter("all"); setCurrentPage(1); }} 
            variant={filter === "all" ? "primary" : "secondary"}
            size="sm"
          >
            All Time
          </Button>
          <Button 
            onClick={() => { setFilter("month"); setCurrentPage(1); }} 
            variant={filter === "month" ? "primary" : "secondary"}
            size="sm"
          >
            This Month
          </Button>
          <Button 
            onClick={() => { setFilter("completed"); setCurrentPage(1); }} 
            variant={filter === "completed" ? "primary" : "secondary"}
            size="sm"
          >
            Completed
          </Button>
          <Button 
            onClick={() => { setFilter("removed"); setCurrentPage(1); }} 
            variant={filter === "removed" ? "primary" : "secondary"}
            size="sm"
          >
            Removed
          </Button>
        </div>

        <div style={{ width: 280 }}>
          <Input
            prefixIcon={<Search size={16} />}
            placeholder="Search by task title..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setCurrentPage(1); }}
          />
        </div>
      </div>

      {/* History Table */}
      <div className="card" style={{ padding: "28px" }}>
        {isLoading ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            Loading history records...
          </div>
        ) : filteredHistory.length === 0 ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            No matching task records found in your history log.
          </div>
        ) : (
          <div>
            <div className="premium-table-wrapper">
              <table className="premium-table">
                <thead>
                  <tr>
                    <th>Task Name</th>
                    <th>Assigned Date</th>
                    <th>Completion Date</th>
                    <th>Submitted Proof</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {currentItems.map(item => (
                    <tr key={item.id}>
                      <td data-label="Task Name" style={{ fontWeight: 700, color: "var(--color-navy)" }}>
                        {item.task?.title}
                      </td>
                      <td data-label="Assigned Date" style={{ color: "var(--color-text-secondary)", fontSize: 13 }}>
                        {formatDate(item.assignedDate || item.assignedAt)}
                      </td>
                      <td data-label="Completion Date" style={{ color: "var(--color-text-secondary)", fontSize: 13 }}>
                        {item.status === "COMPLETED" ? formatDate(item.submittedAt) : "—"}
                      </td>
                      <td data-label="Submitted Proof">
                        {item.proofUrl ? (
                          <a 
                            href={item.proofUrl} 
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
                      <td data-label="Status">
                        <div>
                          <Badge status={item.status} />
                          {item.status === "REMOVED" && item.removedReason && (
                            <div style={{ fontSize: 11, color: "#EF4444", marginTop: 4 }}>
                              Reason: {item.removedReason}
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div style={{ display: "flex", justifyContent: "center", alignItems: "center", gap: 12, marginTop: 24 }}>
                <Button
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage(prev => prev - 1)}
                  variant="secondary"
                  size="sm"
                >
                  Previous
                </Button>
                <span style={{ fontSize: 13, color: "var(--color-text-secondary)", fontWeight: 600 }}>
                  Page {currentPage} of {totalPages}
                </span>
                <Button
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage(prev => prev + 1)}
                  variant="secondary"
                  size="sm"
                >
                  Next
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
