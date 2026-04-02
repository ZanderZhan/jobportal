import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import '../styles/dashboard.css';
import '../styles/utilities.css';

export function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="app-bg">
      <div className="app-bg__mesh mesh--dim" />
      <div className="app-bg__grid mesh--dimmer" />
      <div className="app-bg__grain" />

      <header className="dashboard-header animate-fade-up">
        <a href="/" className="logo">
          <svg className="logo-emblem" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="44" height="44" rx="8" fill="#006B3C"/>
            <text x="22" y="29" textAnchor="middle" fontFamily="Syne, sans-serif" fontWeight="800" fontSize="18" fill="white">UL</text>
            <path d="M22 36 C22 36 19 32 19 30 C19 28.5 20.5 27.5 22 27.5 C23.5 27.5 25 28.5 25 30 C25 32 22 36 22 36Z" fill="white" opacity="0.9"/>
          </svg>
          Job<span>Portal</span>
        </a>

        <div className="dashboard-header-right">
          <div className="dashboard-user-info">
            <span className="role-badge">{user?.role?.replace('ROLE_', '').replace('_', ' ')}</span>
            <span className="dashboard-user-email">{user?.email}</span>
          </div>
          <button onClick={handleLogout} className="btn btn-ghost btn--sm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16,17 21,12 16,7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            Sign out
          </button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="dashboard-content">
          <div className="animate-fade-up-1 dashboard-welcome-hero">
            <div>
              <h1 className="dashboard-welcome-heading">
                Welcome back,{' '}
                <span className="text-primary">{user?.name?.split(' ')[0]}</span>
              </h1>
              <p className="dashboard-welcome-sub">Your career journey continues here</p>
            </div>
          </div>

          <div className="animate-fade-up-2 dashboard-stats-row">
            <div className="solid-card dashboard-stat-card">
              <div className="dashboard-stat-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" strokeWidth="1.8" strokeLinecap="round">
                  <rect x="2" y="7" width="20" height="14" rx="2" />
                  <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2" />
                </svg>
              </div>
              <div>
                <p className="dashboard-stat-value">0</p>
                <p className="dashboard-stat-label">Active applications</p>
              </div>
            </div>

            <div className="solid-card dashboard-stat-card">
              <div className="dashboard-stat-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" strokeWidth="1.8" strokeLinecap="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
              </div>
              <div>
                <p className="dashboard-stat-value">0</p>
                <p className="dashboard-stat-label">Messages</p>
              </div>
            </div>

            <div className="solid-card dashboard-stat-card">
              <div className="dashboard-stat-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" strokeWidth="1.8" strokeLinecap="round">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12,6 12,12 16,14" />
                </svg>
              </div>
              <div>
                <p className="dashboard-stat-value">0</p>
                <p className="dashboard-stat-label">Interviews</p>
              </div>
            </div>
          </div>

          <div className="animate-fade-up-3 dashboard-actions-section">
            <h2 className="dashboard-section-title">Quick actions</h2>
            <div className="dashboard-actions-grid">
              <button className="solid-card dashboard-action-card" onClick={() => navigate('/jobs')}>
                <div className="dashboard-action-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                  </svg>
                </div>
                <span className="dashboard-action-label">Browse jobs</span>
              </button>

              <button className="solid-card dashboard-action-card">
                <div className="dashboard-action-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14,2 14,8 20,8" />
                    <line x1="12" y1="18" x2="12" y2="12" />
                    <line x1="9" y1="15" x2="15" y2="15" />
                  </svg>
                </div>
                <span className="dashboard-action-label">My applications</span>
              </button>

              <button className="solid-card dashboard-action-card">
                <div className="dashboard-action-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                </div>
                <span className="dashboard-action-label">Profile</span>
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
