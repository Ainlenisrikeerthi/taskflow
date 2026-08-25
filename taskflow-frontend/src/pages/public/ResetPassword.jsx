import { useState, useEffect } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { AlertCircle, CheckCircle, KeyRound } from "lucide-react";
import { api } from "../../data/api";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import "../../styles/auth.css";

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const token = searchParams.get("token");

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  // No token in URL — show error immediately
  const hasToken = Boolean(token);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (newPassword.length < 6) {
      setError("Password must be at least 6 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsLoading(true);
    try {
      await api.auth.resetPassword(token, newPassword);
      setSuccess(true);
      // Redirect to login after 3 seconds
      setTimeout(() => navigate("/login"), 3000);
    } catch (err) {
      setError(
        err.message ||
          "Failed to reset password. The link may have expired. Please request a new one."
      );
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        {/* Logo */}
        <Link to="/" className="auth-brand-logo">
          <div className="auth-brand-mark">✓</div>
          TaskFlow
        </Link>

        <header className="auth-header">
          <h1 className="auth-title">Set New Password</h1>
          <p className="auth-subtitle">Enter your new password below.</p>
        </header>

        {error && (
          <div className="alert-box danger" style={{ marginBottom: "20px" }}>
            <AlertCircle size={16} style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        {!hasToken ? (
          <div className="alert-box danger" style={{ marginBottom: "20px" }}>
            <AlertCircle size={16} style={{ flexShrink: 0 }} />
            <span>
              Invalid or missing reset link. Please request a new password reset.
            </span>
          </div>
        ) : success ? (
          <div className="alert-box success" style={{ marginBottom: "20px" }}>
            <CheckCircle size={16} style={{ flexShrink: 0 }} />
            <span>
              Your password has been reset successfully! Redirecting to Sign In…
            </span>
          </div>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit}>
            <Input
              label="New Password"
              type="password"
              placeholder="••••••••"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              disabled={isLoading}
              autoComplete="new-password"
            />

            <Input
              label="Confirm New Password"
              type="password"
              placeholder="••••••••"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              disabled={isLoading}
              autoComplete="new-password"
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              fullWidth
              loading={isLoading}
              style={{ marginTop: 8 }}
            >
              Reset Password
            </Button>
          </form>
        )}

        <div className="auth-footer">
          <Link to="/forgot-password">Request a new reset link</Link>
          {" · "}
          <Link to="/login">Sign In</Link>
        </div>
      </div>
    </main>
  );
}
