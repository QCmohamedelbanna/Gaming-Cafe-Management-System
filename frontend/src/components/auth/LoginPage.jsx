import { useState } from "react";
import { useAuth } from "../../auth/AuthContext";

export default function LoginPage() {
    const { login } = useAuth();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");
        setSubmitting(true);
        try {
            await login(username, password);
        } catch (loginError) {
            setError(loginError.message || "Unable to sign in");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <main className="login-shell">
            <section className="login-card">
                <div className="login-brand-mark">🎮</div>
                <span className="page-label">SECURE ACCESS</span>
                <h1>Gaming Cafe</h1>
                <p>Sign in to manage operations, sales, and shifts.</p>
                <form onSubmit={handleSubmit} className="login-form">
                    <label>
                        Username
                        <input
                            autoFocus
                            autoComplete="username"
                            value={username}
                            onChange={(event) => setUsername(event.target.value)}
                            required
                        />
                    </label>
                    <label>
                        Password
                        <span className="login-password-field">
                            <input
                                id="login-password"
                                type={showPassword ? "text" : "password"}
                                autoComplete="current-password"
                                value={password}
                                onChange={(event) => setPassword(event.target.value)}
                                required
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowPassword((visible) => !visible)}
                                aria-label={showPassword ? "Hide password" : "Show password"}
                                aria-pressed={showPassword}
                                aria-controls="login-password"
                                title={showPassword ? "Hide password" : "Show password"}
                            >
                                <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                                    {showPassword ? (
                                        <>
                                            <path d="M3 3l18 18" />
                                            <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                                            <path d="M9.9 5.2A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a18.5 18.5 0 0 1-3.1 3.9" />
                                            <path d="M6.6 6.7C3.8 8.4 2 12 2 12s3.5 7 10 7a10.8 10.8 0 0 0 3.2-.5" />
                                        </>
                                    ) : (
                                        <>
                                            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
                                            <circle cx="12" cy="12" r="3" />
                                        </>
                                    )}
                                </svg>
                            </button>
                        </span>
                    </label>
                    {error && <div className="login-error">{error}</div>}
                    <button type="submit" className="login-submit" disabled={submitting}>
                        {submitting ? "Signing in…" : "Sign in"}
                    </button>
                </form>
                <small>Contact an administrator if you need an account.</small>
            </section>
        </main>
    );
}
