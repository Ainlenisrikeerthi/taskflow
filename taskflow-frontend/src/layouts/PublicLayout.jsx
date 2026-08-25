import { Outlet, Link } from "react-router-dom";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "../components/common/ThemeContext";
export default function PublicLayout(){const {theme,toggleTheme}=useTheme();return <><header className="public-nav"><div className="nav-inner"><Link to="/" className="brand"><span className="brand-mark">T</span><span>TaskFlow</span></Link><nav><a href="#features">Features</a><a href="#about">About</a><button className="public-theme-toggle" onClick={toggleTheme} aria-label="Toggle theme">{theme==="dark"?<Sun size={17}/>:<Moon size={17}/>}</button><Link to="/login">Log in</Link><Link to="/register" className="nav-cta">Register</Link></nav></div></header><Outlet/></>}
