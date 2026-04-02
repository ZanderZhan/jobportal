import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import '../../styles/auth-pages.css';
import '../../styles/utilities.css';
import * as React from "react";

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      setError('Password must contain at least one letter and one number');
      return;
    }

    setIsLoading(true);
    try {
      await register(email, password, name);
      navigate('/login');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message || 'Registration failed. Try again.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-bg">
      <div className="app-bg__mesh mesh--reverse" />
      <div className="app-bg__grid" />
      <div className="app-bg__grain" />

      <div className="glow-spot glow-spot--tr" />
      <div className="glow-spot glow-spot--bl" style={{ animationDelay: '1.5s' }} />

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
          <div className="solid-card animate-scale-in auth-card register-card">
            <div className="auth-card-content">
              <div className="animate-fade-up-1 auth-heading-group">
                <div className="ul-badge">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                    <path d="M22 10v6M2 10l10-6 10 6-10 6-10-6z"/>
                    <path d="M6 12v5c0 1 2 3 6 3s6-2 6-3v-5"/>
                  </svg>
                  University of Limerick
                </div>
                <h1 className="auth-heading">Create account</h1>
                <p className="auth-subheading">Join the community and start your journey</p>
              </div>

              <form onSubmit={handleSubmit} className="animate-fade-up-2 auth-form" noValidate>
                <div className="field">
                  <label className="label" htmlFor="name">Full name</label>
                  <input
                    id="name"
                    type="text"
                    className="input"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="John Doe"
                    autoComplete="name"
                    required
                  />
                </div>

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
                    placeholder="Min. 8 characters"
                    autoComplete="new-password"
                    required
                  />
                </div>

                <div className="field">
                  <label className="label" htmlFor="confirm">Confirm password</label>
                  <input
                    id="confirm"
                    type="password"
                    className={`input ${error ? 'error' : ''}`}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Repeat your password"
                    autoComplete="new-password"
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
                      Creating account…
                    </>
                  ) : (
                    'Create account'
                  )}
                </button>
              </form>

              <div className="divider animate-fade-up-3">or</div>

              <div className="animate-fade-up-4 auth-footer">
                <span className="text-muted text-sm">
                  Already have an account?{' '}
                </span>
                <Link to="/login" className="nav-link link-primary">
                  Sign in
                </Link>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
