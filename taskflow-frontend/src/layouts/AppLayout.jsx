import { useEffect, useMemo, useState } from "react";
import { Outlet, Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../components/common/AuthContext";
import { useTheme } from "../components/common/ThemeContext";
import { api } from "../data/api";
import { LayoutDashboard,ListTodo,History,LogOut,ShieldCheck,Menu,X,ClipboardList,Layers,User,Users,ClipboardCheck,Plus,Search,Sun,Moon,Bell,CheckCheck } from "lucide-react";
import "../styles/app.css";

export default function AppLayout() {
  const { currentUser, logout, isAdmin } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [searchData, setSearchData] = useState({ tasks: [], users: [], assignments: [] });
  const [searchLoading, setSearchLoading] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [notificationOpen, setNotificationOpen] = useState(false);

  useEffect(() => { setQuery(""); }, [location.pathname]);

  useEffect(() => {
    if (query.trim().length < 2) return;
    let cancelled = false;
    const timer = setTimeout(async () => {
      setSearchLoading(true);
      try {
        const [tasks, users, assignments] = isAdmin
          ? await Promise.all([api.tasks.getAll(), api.admin.getUsers(), api.admin.getAssignments(query.trim())])
          : await Promise.all([api.tasks.getPublished(), Promise.resolve([]), api.assignments.getMyAll()]);
        if (!cancelled) setSearchData({ tasks: tasks || [], users: users || [], assignments: assignments || [] });
      } catch (e) { if (!cancelled) setSearchData({ tasks: [], users: [], assignments: [] }); }
      finally { if (!cancelled) setSearchLoading(false); }
    }, 250);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [query, isAdmin]);


  useEffect(() => {
    let unsubscribe = () => {};
    async function initNotifications() {
      try { setNotifications(await api.notifications.list()); } catch {}
      unsubscribe = api.notifications.subscribe((notification) => {
        setNotifications(prev => [notification, ...prev.filter(n => n.id !== notification.id)].slice(0, 50));
      });
    }
    initNotifications();
    return () => unsubscribe();
  }, [currentUser?.id]);

  const unreadNotifications = notifications.filter(n => !n.isRead).length;

  async function openNotification(n) {
    try { if (!n.isRead) await api.notifications.markRead(n.id); } catch {}
    setNotifications(prev => prev.map(x => x.id === n.id ? { ...x, isRead: true } : x));
    setNotificationOpen(false);
    if (n.taskId) navigate(isAdmin ? `/admin/tasks/${n.taskId}` : `/user/tasks/${n.taskId}`);
  }

  async function markAllNotificationsRead() {
    try { await api.notifications.markAllRead(); } catch {}
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
  }

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (q.length < 2) return [];
    const items = [];
    searchData.tasks.filter(t => [t.title,t.description,t.status].some(v => String(v || "").toLowerCase().includes(q))).slice(0,4)
      .forEach(t => items.push({ type:"Task", label:t.title, meta:t.status, to:isAdmin?"/admin/tasks":`/user/tasks/${t.id}` }));
    searchData.users.filter(u => [u.name,u.email,u.role].some(v => String(v || "").toLowerCase().includes(q))).slice(0,4)
      .forEach(u => items.push({ type:"User", label:u.name, meta:u.email, to:`/admin/users/${u.id}` }));
    searchData.assignments.filter(a => [a.task?.title,a.user?.name,a.user?.email,a.proofUrl,a.status].some(v => String(v || "").toLowerCase().includes(q))).slice(0,5)
      .forEach(a => items.push({ type:"Assignment", label:a.task?.title || `Assignment #${a.id}`, meta:a.user?.name || a.status, to:isAdmin?"/admin/assignments":"/user/my-tasks" }));
    return items.slice(0,9);
  }, [query, searchData, isAdmin]);

  function handleLogout(){ logout(); navigate("/"); }
  const rolePath = isAdmin ? "admin" : "user";
  function NavItem({to,icon,label}) { const active=location.pathname===to; return <Link to={to} onClick={()=>setIsMobileOpen(false)} className={`sidebar-nav-item${active?" active":""}`}><span className="sidebar-nav-icon">{icon}</span>{label}</Link>; }
  const initials=currentUser?.name?.charAt(0)?.toUpperCase() ?? "U";

  return <div className="app-shell">
    <header className="mobile-header"><div className="mobile-header-logo"><div className="sidebar-logo-mark">✓</div>TaskFlow</div><button className="mobile-menu-btn" onClick={()=>setIsMobileOpen(v=>!v)}>{isMobileOpen?<X size={20}/>:<Menu size={20}/>}</button></header>
    {isMobileOpen && <div onClick={()=>setIsMobileOpen(false)} style={{position:"fixed",inset:0,background:"rgba(15,10,7,.62)",backdropFilter:"blur(6px)",zIndex:"var(--z-backdrop)"}}/>}
    <aside className={`app-sidebar${isMobileOpen?" sidebar-open":""}`}><div className="sidebar-inner">
      <Link to={`/${rolePath}`} className="sidebar-logo"><div className="sidebar-logo-mark">✓</div><div><div style={{lineHeight:1.2,fontWeight:800}}>TaskFlow</div><div style={{fontSize:11,fontWeight:500,color:"var(--sidebar-text)"}}>{isAdmin?"Admin Workspace":"User Portal"}</div></div></Link>
      {isAdmin && <Link to="/admin/tasks?action=new" className="sidebar-new-task-btn"><Plus size={16}/>New Task</Link>}
      <nav className="sidebar-nav"><NavItem to={`/${rolePath}`} icon={<LayoutDashboard size={18}/>} label="Dashboard"/>{isAdmin?<><NavItem to="/admin/tasks" icon={<ListTodo size={18}/>} label="Tasks"/><NavItem to="/admin/assignments" icon={<ClipboardCheck size={18}/>} label="Assignments"/><NavItem to="/admin/users" icon={<Users size={18}/>} label="Users"/><NavItem to="/admin/profile" icon={<User size={18}/>} label="Profile"/></>:<><NavItem to="/user/tasks" icon={<Layers size={18}/>} label="Available Tasks"/><NavItem to="/user/my-tasks" icon={<ClipboardList size={18}/>} label="My Tasks"/><NavItem to="/user/history" icon={<History size={18}/>} label="Task History"/><NavItem to="/user/profile" icon={<User size={18}/>} label="Profile"/></>}</nav>
      <div className="sidebar-user"><div className="sidebar-user-info"><div className="avatar avatar-sm">{initials}</div><div style={{minWidth:0,flex:1}}><div className="sidebar-user-name">{currentUser?.name??"User"}</div><div className={`sidebar-user-role${isAdmin?" admin-role":""}`}>{isAdmin?<><ShieldCheck size={12}/>Admin</>:"User Member"}</div></div></div><button onClick={handleLogout} className="sidebar-logout"><LogOut size={16}/>Sign Out</button></div>
    </div></aside>
    <main className="content-area"><header className="app-top-header">
      <div className="global-search-wrap"><div className="top-header-search"><Search size={16}/><input value={query} onChange={e=>setQuery(e.target.value)} type="search" placeholder={isAdmin?"Search tasks, users, assignments...":"Search tasks and your work..."}/></div>
      {query.trim().length>=2 && <div className="global-search-results">{searchLoading?<div className="search-state">Searching live data…</div>:results.length?results.map((r,i)=><button key={`${r.type}-${i}`} onClick={()=>{navigate(r.to);setQuery("")}} className="search-result"><span className="search-result-type">{r.type}</span><span><strong>{r.label}</strong><small>{r.meta}</small></span></button>):<div className="search-state">No matching results.</div>}</div>}</div>
      <div className="top-header-actions">
        <div className="notification-wrap">
          <button className="icon-button notification-button" onClick={()=>setNotificationOpen(v=>!v)} title="Notifications"><Bell size={18}/>{unreadNotifications>0&&<span className="notification-badge">{unreadNotifications>99?"99+":unreadNotifications}</span>}</button>
          {notificationOpen&&<div className="notification-panel">
            <div className="notification-panel-head"><div><strong>Notifications</strong><small>{unreadNotifications} unread</small></div>{unreadNotifications>0&&<button onClick={markAllNotificationsRead}><CheckCheck size={15}/>Mark all read</button>}</div>
            <div className="notification-list">{notifications.length===0?<div className="notification-empty">No notifications yet.</div>:notifications.map(n=><button key={n.id} className={`notification-item${n.isRead?"":" unread"}`} onClick={()=>openNotification(n)}><span className="notification-dot"/><span><strong>{n.title}</strong><p>{n.message}</p><small>{new Date(n.createdAt).toLocaleString()}</small></span></button>)}</div>
          </div>}
        </div>
        <button className="theme-toggle" onClick={toggleTheme} title={`Switch to ${theme==="dark"?"light":"dark"} theme`}>{theme==="dark"?<Sun size={18}/>:<Moon size={18}/>}<span>{theme==="dark"?"Light":"Dark"}</span></button><div className="header-user"><div className="avatar avatar-sm">{initials}</div><span>{currentUser?.name??"User"}</span></div></div>
    </header><div className="content-inner"><Outlet/></div></main>
  </div>;
}
