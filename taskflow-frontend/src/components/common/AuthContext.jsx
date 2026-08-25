import { createContext, useContext, useState, useEffect } from "react";
import { api } from "../../data/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem("currentUser");
    return saved ? JSON.parse(saved) : null;
  });
  
  const [token, setToken] = useState(() => {
    return localStorage.getItem("token") || null;
  });

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Initial load check
    setIsLoading(false);
  }, []);

  async function login(email, password) {
    setIsLoading(true);
    try {
      const res = await api.auth.login(email, password);
      setSession(res);
      return res;
    } finally {
      setIsLoading(false);
    }
  }

  async function register(name, email, password, role) {
    setIsLoading(true);
    try {
      const res = await api.auth.register(name, email, password, role);
      setSession(res);
      return res;
    } finally {
      setIsLoading(false);
    }
  }

  async function loginWithGoogle(googleId, email, name) {
    setIsLoading(true);
    try {
      const res = await api.auth.google(googleId, email, name);
      setSession(res);
      return res;
    } finally {
      setIsLoading(false);
    }
  }

  function updateCurrentUser(profile) {
    setCurrentUser((previous) => {
      const updated = {
        ...(previous || {}),
        id: profile.id ?? previous?.id,
        name: profile.name ?? previous?.name,
        email: profile.email ?? previous?.email,
        role: profile.role ?? previous?.role,
      };
      localStorage.setItem("currentUser", JSON.stringify(updated));
      return updated;
    });
  }

  function logout() {
    setCurrentUser(null);
    setToken(null);
    localStorage.removeItem("currentUser");
    localStorage.removeItem("token");
  }

  function setSession(authResponse) {
    const user = {
      id: authResponse.id,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role
    };
    setCurrentUser(user);
    setToken(authResponse.token);
    localStorage.setItem("currentUser", JSON.stringify(user));
    localStorage.setItem("token", authResponse.token);
  }

  const value = {
    currentUser,
    token,
    isLoading,
    login,
    register,
    loginWithGoogle,
    updateCurrentUser,
    logout,
    isAdmin: currentUser?.role === "ADMIN",
    isUser: currentUser?.role === "USER",
    isAuthenticated: !!token
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
