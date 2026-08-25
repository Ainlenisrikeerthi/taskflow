import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { api } from "../../data/api";
import { Users, Search, Calendar, Shield, Mail, Eye, AlertCircle } from "lucide-react";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import Badge from "../../components/ui/Badge";

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  useEffect(() => {
    fetchUsers();
  }, []);

  async function fetchUsers() {
    setIsLoading(true);
    setError("");
    try {
      const data = await api.admin.getUsers();
      setUsers(data);
    } catch (err) {
      setError(err.message || "Failed to load registered users.");
    } finally {
      setIsLoading(false);
    }
  }

  // Filter logic
  const filteredUsers = users.filter((u) => {
    return (
      u.name?.toLowerCase().includes(search.toLowerCase()) ||
      u.email?.toLowerCase().includes(search.toLowerCase())
    );
  });

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs">
            <span>Admin</span>
            <span>/</span>
            <span style={{ fontWeight: 600, color: "var(--color-navy)" }}>Users</span>
          </div>
          <h1>Registered Users</h1>
          <p>Directory of all registered team members and administrative personnel.</p>
        </div>
      </div>

      {/* Message alerts */}
      {error && (
        <div className="alert-box danger" style={{ marginBottom: 24 }}>
          <AlertCircle size={16} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {/* Main card */}
      <div className="card" style={{ padding: "28px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20, flexWrap: "wrap", gap: 16 }}>
          <h3 style={{ fontSize: 18, fontWeight: 800, color: "var(--color-navy)" }}>Workspace Members</h3>

          <div style={{ width: 280 }}>
            <Input
              prefixIcon={<Search size={16} />}
              placeholder="Search by name or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {/* Users Table */}
        {isLoading ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            Loading user registry...
          </div>
        ) : filteredUsers.length === 0 ? (
          <div style={{ padding: "60px", textAlign: "center", color: "var(--color-text-secondary)" }}>
            No registered users match your search.
          </div>
        ) : (
          <div className="premium-table-wrapper">
            <table className="premium-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>System Role</th>
                  <th>Registration Date</th>
                  <th style={{ textAlign: "center" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td data-label="Name" style={{ fontWeight: 700, color: "var(--color-navy)" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div className="avatar avatar-sm">
                          {user.name?.charAt(0).toUpperCase() || "U"}
                        </div>
                        {user.name}
                      </div>
                    </td>
                    <td data-label="Email" style={{ color: "var(--color-text-secondary)" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                        <Mail size={13} style={{ opacity: 0.6 }} /> {user.email}
                      </div>
                    </td>
                    <td data-label="System Role">
                      <Badge variant={user.role === "ADMIN" ? "danger" : "purple"} label={user.role} />
                    </td>
                    <td data-label="Registration Date" style={{ color: "var(--color-text-secondary)", fontSize: 13 }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                        <Calendar size={13} style={{ opacity: 0.6 }} /> {user.createdAt ? user.createdAt.split("T")[0] : "—"}
                      </div>
                    </td>
                    <td data-label="Actions" style={{ textAlign: "center" }}>
                      <Link to={`/admin/users/${user.id}`} style={{ textDecoration: "none" }}>
                        <Button variant="secondary" size="sm" icon={<Eye size={12} />}>
                          View Activity
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
