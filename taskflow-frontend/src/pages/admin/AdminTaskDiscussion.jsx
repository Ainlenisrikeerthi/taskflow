import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Calendar } from "lucide-react";
import { api } from "../../data/api";
import Badge from "../../components/ui/Badge";
import CommentsPanel from "../../components/task/CommentsPanel";

export default function AdminTaskDiscussion() {
  const { id } = useParams();
  const [task, setTask] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => { api.tasks.getById(id).then(setTask).catch(e=>setError(e.message || "Unable to load task.")); }, [id]);

  if (error) return <div className="alert-box danger">{error}</div>;
  if (!task) return <div style={{padding:50,textAlign:"center",color:"var(--color-text-secondary)"}}>Loading task discussion…</div>;

  return <div>
    <div className="page-header">
      <div className="page-header-left">
        <Link to="/admin/tasks" style={{display:"inline-flex",alignItems:"center",gap:6,color:"var(--color-text-secondary)",textDecoration:"none",fontSize:13}}><ArrowLeft size={14}/>Back to Tasks</Link>
        <h1>{task.title}</h1>
        <p>Task discussion and team communication.</p>
      </div>
    </div>
    <div className="card" style={{padding:24,marginBottom:24}}>
      <div style={{display:"flex",justifyContent:"space-between",gap:16,alignItems:"center",marginBottom:14}}><Badge status={task.status}/><span style={{display:"inline-flex",alignItems:"center",gap:5,color:"var(--color-text-secondary)",fontSize:13}}><Calendar size={14}/>Due {task.deadline}</span></div>
      <p style={{color:"var(--color-text-secondary)",lineHeight:1.7,margin:0}}>{task.description}</p>
    </div>
    <CommentsPanel taskId={Number(id)} />
  </div>;
}
