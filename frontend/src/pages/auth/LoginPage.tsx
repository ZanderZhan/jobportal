import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import '../../styles/auth-pages.css';
import '../../styles/utilities.css';

export function LoginPage() {
  const { login, loginWithGoogle, loginWithMicrosoft } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message || 'Invalid email or password';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-bg">
      <div className="app-bg__mesh" />
      <div className="app-bg__grid" />
      <div className="app-bg__grain" />

      <div className="glow-spot glow-spot--tl" />
      <div className="glow-spot glow-spot--br" style={{ animationDelay: '2s' }} />

      <div className="auth-page">
        <header className="animate-fade-up auth-header">
          <a href="/" className="logo">
            <svg className="logo-emblem" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="44" height="44" rx="8" fill="#006B3C"/>
              <text x="22" y="29" textAnchor="middle" fontFamily="Syne, sans-serif" fontWeight="800" fontSize="18" fill="white">UL</text>
              <path d="M22 36 C22 36 19 32 19 30 C19 28.5 20.5 27.5 22 27.5 C23.5 27.5 25 28.5 25 30 C25 32 22 36 22 36Z" fill="white" opacity="0.9"/>
            </svg>
            Job<span>Portal</span>
          </a>
        </header>

        <main className="auth-main">
          <div className="solid-card animate-scale-in auth-card">
            <div className="auth-card-content">
              <div className="animate-fade-up-1 auth-heading-group">
                <div className="ul-badge">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                    <path d="M22 10v6M2 10l10-6 10 6-10 6-10-6z"/>
                    <path d="M6 12v5c0 1 2 3 6 3s6-2 6-3v-5"/>
                  </svg>
                  University of Limerick
                </div>
                <h1 className="auth-heading">Welcome back</h1>
                <p className="auth-subheading">Sign in to access your career opportunities</p>
              </div>

              <form
                onSubmit={handleSubmit}
                className="animate-fade-up-2 auth-form"
                noValidate
              >
                <div className="field">
                  <label className="label" htmlFor="email">Email address</label>
                  <input
                    id="email"
                    type="email"
                    className={`input ${error ? 'error' : ''}`}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="you@studentmail.ul.ie"
                    autoComplete="email"
                    required
                  />
                </div>

                <div className="field">
                  <label className="label" htmlFor="password">Password</label>
                  <input
                    id="password"
                    type="password"
                    className={`input ${error ? 'error' : ''}`}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    autoComplete="current-password"
                    required
                  />
                </div>

                {error && (
                  <div className="error-banner animate-fade-up">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="flex-shrink-0 mt-1">
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="8" x2="12" y2="12" />
                      <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  className="btn btn-primary auth-submit-btn"
                  disabled={isLoading}
                >
                  {isLoading ? (
                    <>
                      <div className="spinner spinner--sm" />
                      Signing in…
                    </>
                  ) : (
                    'Sign in'
                  )}
                </button>
              </form>

              <div className="divider animate-fade-up-3">or continue with</div>

              <div className="animate-fade-up-4 auth-social-group">
                <button onClick={loginWithGoogle} className="btn-social">
                  <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                  </svg>
                  Sign in with Google
                </button>

                <button onClick={loginWithMicrosoft} className="btn-social">
                  <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                    <path fill="#F25022" d="M1 1h10v10H1z"/>
                    <path fill="#00A4EF" d="M1 13h10v10H1z"/>
                    <path fill="#7FBA00" d="M13 1h10v10H13z"/>
                    <path fill="#FFB900" d="M13 13h10v10H13z"/>
                  </svg>
                  Sign in with Microsoft
                </button>
              </div>

              <p className="animate-fade-up-5 auth-footer">
                Don't have an account?{' '}
                <Link to="/register" className="nav-link link-primary">
                  Create one
                </Link>
              </p>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
