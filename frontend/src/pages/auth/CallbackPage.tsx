import { useEffect, useState, useMemo, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import '../../styles/auth-pages.css';
import '../../styles/utilities.css';

export function CallbackPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { handleOAuthCallback } = useAuth();
  const [asyncError, setAsyncError] = useState('');
  const callbackAttempted = useRef(false);

  const params = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const code = params.get('code');
  const state = params.get('state');
  const errorParam = params.get('error');

  const syncError = useMemo(() => {
    if (errorParam) return `Access denied: ${errorParam}`;
    if (!code || !state) return 'Missing authentication data. Please try again.';
    return '';
  }, [errorParam, code, state]);

  // Detect provider from 'iss' parameter (Google returns iss=https://accounts.google.com)
  const provider = useMemo(() => {
    const iss = params.get('iss');
    if (iss?.includes('google')) return 'google';
    return 'microsoft';
  }, [params]);

  const displayError = syncError || asyncError;

  useEffect(() => {
    if (syncError) return;
    if (!code || !state) return;
    if (callbackAttempted.current) return;
    callbackAttempted.current = true;

    handleOAuthCallback(provider, code, state)
      .then(() => navigate('/dashboard'))
      .catch((err: unknown) => {
        setAsyncError(
          (err as { message?: string })?.message || 'Authentication failed. Please try again.'
        );
      });
  }, [provider, handleOAuthCallback, navigate, syncError, code, state]);

  return (
    <div className="app-bg">
      <div className="app-bg__mesh" />
      <div className="app-bg__grid" />
      <div className="app-bg__grain" />
      <div className="glow-spot glow-spot--center" />

      <div className="auth-page">
        <header className="auth-header">
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
          <div className="solid-card animate-scale-in callback-card">
            {displayError ? (
              <div className="callback-content">
                <div className="callback-icon-wrapper">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--color-error)" strokeWidth="1.5">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                </div>
                <h2 className="callback-heading-error">Authentication failed</h2>
                <p className="callback-error-msg">{displayError}</p>
                <button
                  onClick={() => navigate('/login')}
                  className="btn btn-primary auth-submit-btn"
                >
                  Back to sign in
                </button>
              </div>
            ) : (
              <div className="callback-content">
                <div className="callback-spinner-wrapper">
                  <div className="spinner spinner--lg" />
                  <div className="callback-spinner-glow" />
                </div>
                <h2 className="callback-heading">Signing you in</h2>
                <p className="callback-subheading">
                  Connecting to your account, please wait…
                </p>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
