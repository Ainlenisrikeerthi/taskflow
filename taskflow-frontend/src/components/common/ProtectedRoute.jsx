import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function ProtectedRoute({ children, allowedRoles }) {
  const { currentUser, isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div style={{
        display: "grid",
        placeItems: "center",
        height: "100vh",
        fontSize: "18px",
        fontWeight: "600",
        fontFamily: "sans-serif"
      }}>
        Loading your workspace...
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to login page, preserving requested location
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(currentUser?.role)) {
    // Redirect to home if they don't have access to this role
    return <Navigate to={currentUser?.role === "ADMIN" ? "/admin" : "/user"} replace />;
  }

  return children;
}
