import { useState } from "react";
import { Link } from "react-router-dom";
import { AlertCircle, CheckCircle } from "lucide-react";
import { api } from "../../data/api";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import "../../styles/auth.css";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      const result = await api.auth.forgotPassword(email);
      if (!result?.delivered) {
        throw new Error(result?.message || "The reset email could not be delivered.");
      }
      setSubmitted(true);
    } catch (err) {
      setError(
        err.message ||
          "Failed to send reset email. Please check your email address and try again."
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
          <h1 className="auth-title">Reset Password</h1>
          <p className="auth-subtitle">
            Enter your email to receive password reset instructions.
          </p>
        </header>

        {error && (
          <div className="alert-box danger" style={{ marginBottom: "20px" }}>
            <AlertCircle size={16} style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        {submitted ? (
          <div className="alert-box success" style={{ marginBottom: "20px" }}>
            <CheckCircle size={16} style={{ flexShrink: 0 }} />
            <span>
              A password reset link has been sent to <strong>{email}</strong>.
              Please check your inbox (and spam folder).
            </span>
          </div>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit}>
            <Input
              label="Email Address"
              type="email"
              placeholder="name@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={isLoading}
              autoComplete="email"
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              fullWidth
              loading={isLoading}
              style={{ marginTop: 8 }}
            >
              Send Reset Link
            </Button>
          </form>
        )}

        <div className="auth-footer">
          Remembered your password?{" "}
          <Link to="/login">Back to Sign In</Link>
        </div>
      </div>
    </main>
  );
}