import { useEffect, useState } from "react";
import { MessageSquare, Send, Trash2 } from "lucide-react";
import { api } from "../../data/api";
import { useAuth } from "../common/AuthContext";
import Button from "../ui/Button";

export default function CommentsPanel({ taskId }) {
  const { currentUser, isAdmin } = useAuth();
  const [comments, setComments] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { load(); }, [taskId]);

  async function load() {
    setLoading(true);
    try { setComments(await api.comments.list(taskId)); setError(""); }
    catch (e) { setError(e.message || "Unable to load discussion."); }
    finally { setLoading(false); }
  }

  async function submit(e) {
    e.preventDefault();
    const clean = message.trim();
    if (!clean) return;
    setSending(true); setError("");
    try {
      const created = await api.comments.add(taskId, clean);
      setComments(prev => [...prev, created]);
      setMessage("");
    } catch (e) { setError(e.message || "Unable to post comment."); }
    finally { setSending(false); }
  }

  async function remove(commentId) {
    try {
      await api.comments.delete(taskId, commentId);
      setComments(prev => prev.filter(c => c.id !== commentId));
    } catch (e) { setError(e.message || "Unable to delete comment."); }
  }

  return (
    <section className="card discussion-card">
      <div className="discussion-heading">
        <div><h3><MessageSquare size={19}/> Task Discussion</h3><p>Keep questions and task updates in one place.</p></div>
        <span className="discussion-count">{comments.length}</span>
      </div>

      {error && <div className="alert-box danger" style={{marginBottom:16}}>{error}</div>}
      <div className="discussion-list">
        {loading ? <div className="discussion-empty">Loading discussion…</div> : comments.length === 0 ?
          <div className="discussion-empty">No comments yet. Start the conversation.</div> :
          comments.map(c => {
            const canDelete = isAdmin || c.userId === currentUser?.id;
            return <article key={c.id} className="comment-row">
              <div className="avatar avatar-sm">{c.userName?.charAt(0)?.toUpperCase() || "U"}</div>
              <div className="comment-body">
                <div className="comment-meta"><strong>{c.userName}</strong><span className="comment-role">{c.userRole}</span><time>{new Date(c.createdAt).toLocaleString()}</time></div>
                <p>{c.message}</p>
              </div>
              {canDelete && <button className="comment-delete" title="Delete comment" onClick={() => remove(c.id)}><Trash2 size={14}/></button>}
            </article>;
          })}
      </div>

      <form className="discussion-form" onSubmit={submit}>
        <textarea value={message} onChange={e=>setMessage(e.target.value)} maxLength={2000} placeholder="Write a comment or update…" rows={3}/>
        <div className="discussion-form-footer"><span>{message.length}/2000</span><Button type="submit" variant="primary" size="sm" loading={sending} icon={<Send size={14}/>}>Post Comment</Button></div>
      </form>
    </section>
  );
}
