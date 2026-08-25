import { useEffect, useState } from "react";
import { api } from "../../data/api";
import { useAuth } from "../../components/common/AuthContext";
import Button from "../../components/ui/Button";
import { Mail, ShieldCheck, CalendarDays, UserRound, RefreshCw, Pencil, Save, X } from "lucide-react";

export default function AdminProfile() {
  const { updateCurrentUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = await api.users.getMe();
      setProfile(data);
      setName(data?.name || "");
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function saveProfile(e) {
    e.preventDefault();
    const cleanName = name.trim();
    if (cleanName.length < 2) {
      setError("Full name must contain at least 2 characters.");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");
    try {
      const updated = await api.users.updateMe(cleanName);
      setProfile(updated);
      setName(updated.name || "");
      updateCurrentUser(updated);
      setEditing(false);
      setMessage("Profile updated successfully.");
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  function cancelEdit() {
    setName(profile?.name || "");
    setEditing(false);
    setError("");
  }

  const initials = profile?.name?.charAt(0)?.toUpperCase() || "A";

  return (
    <div>
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs"><span>Admin</span><span>/</span><span>Profile</span></div>
          <h1>My Profile</h1>
          <p>View and update your TaskFlow account details.</p>
        </div>
        <button className="profile-refresh" onClick={load} disabled={loading}>
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {message && <div className="profile-success">{message}</div>}
      {loading ? (
        <div className="card profile-loading">Loading your profile…</div>
      ) : error && !profile ? (
        <div className="card profile-loading">{error}</div>
      ) : (
        <div className="profile-live-grid">
          <section className="card profile-identity">
            <div className="profile-avatar">{initials}</div>
            <h2>{profile.name}</h2>
            <span className="profile-role"><ShieldCheck size={15} />Administrator</span>
            <p>Authenticated TaskFlow account</p>
          </section>

          <section className="card profile-details">
            <div className="profile-section-heading">
              <div>
                <h3>Account details</h3>
                <p>Keep your display name up to date. Email and role are protected account fields.</p>
              </div>
              {!editing && (
                <Button size="sm" variant="secondary" icon={<Pencil size={15} />} onClick={() => { setEditing(true); setMessage(""); setError(""); }}>
                  Edit Profile
                </Button>
              )}
            </div>

            {editing ? (
              <form className="profile-edit-form" onSubmit={saveProfile}>
                <label htmlFor="admin-profile-name">Full name</label>
                <input
                  id="admin-profile-name"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  maxLength={100}
                  autoFocus
                />
                <small>This name is shown in the header, sidebar, and profile.</small>
                {error && <div className="profile-form-error">{error}</div>}
                <div className="profile-form-actions">
                  <Button type="button" variant="secondary" icon={<X size={15} />} onClick={cancelEdit} disabled={saving}>Cancel</Button>
                  <Button type="submit" icon={<Save size={15} />} loading={saving}>Save Changes</Button>
                </div>
              </form>
            ) : (
              <>
                {error && <div className="profile-form-error">{error}</div>}
                <ProfileRow icon={<UserRound />} label="Full name" value={profile.name} />
                <ProfileRow icon={<Mail />} label="Email address" value={profile.email} note="Used for sign in and cannot be edited here." />
                <ProfileRow icon={<ShieldCheck />} label="Workspace role" value={profile.role} note="Role changes are restricted." />
                <ProfileRow icon={<CalendarDays />} label="Member since" value={profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : "—"} />
              </>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

function ProfileRow({ icon, label, value, note }) {
  return (
    <div className="profile-row">
      <span className="profile-row-icon">{icon}</span>
      <div>
        <small>{label}</small>
        <strong>{value || "—"}</strong>
        {note && <span className="profile-row-note">{note}</span>}
      </div>
    </div>
  );
}
