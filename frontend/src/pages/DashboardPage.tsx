import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { isEmployerRole } from '../lib/authRoles';
import { getNotificationSummary, type NotificationSummary } from '../lib/notificationApi';
import '../styles/dashboard.css';
import '../styles/utilities.css';

export function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<NotificationSummary | null>(null);
  const [loadingSummary, setLoadingSummary] = useState(true);

  const isEmployer = isEmployerRole(user?.role, user?.userType);

  useEffect(() => {
    let cancelled = false;

    async function loadSummary() {
      setLoadingSummary(true);
      try {
        const nextSummary = await getNotificationSummary();
        if (!cancelled) {
          setSummary(nextSummary);
        }
      } finally {
        if (!cancelled) {
          setLoadingSummary(false);
        }
      }
    }

    loadSummary().catch(() => {
      if (!cancelled) {
        setLoadingSummary(false);
      }
    });

    return () => {
      cancelled = true;
    };
  }, []);

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
            <rect width="44" height="44" rx="8" fill="#006B3C" />
            <text x="22" y="29" textAnchor="middle" fontFamily="Syne, sans-serif" fontWeight="800" fontSize="18" fill="white">UL</text>
            <path d="M22 36 C22 36 19 32 19 30 C19 28.5 20.5 27.5 22 27.5 C23.5 27.5 25 28.5 25 30 C25 32 22 36 22 36Z" fill="white" opacity="0.9" />
          </svg>
          Job<span>Portal</span>
        </a>

        <div className="dashboard-header-right">
          <div className="dashboard-user-info">
            <span className="role-badge">{user?.role?.replace('ROLE_', '').replace('_', ' ')}</span>
            {user?.userType && (
              <span className="role-badge role-badge--accent">{user.userType}</span>
            )}
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
              <p className="dashboard-welcome-sub">
                {isEmployer ? 'Manage your job listings and follow message delivery' : 'Your career journey continues here'}
              </p>
            </div>
          </div>

          <div className="animate-fade-up-2 dashboard-stats-row">
            {isEmployer ? (
              <>
                <StatCard label="Active job listings" value="0" icon="briefcase" />
                <StatCard label="Applications received" value="0" icon="users" />
                <StatCard label="Unread messages" value={formatMetric(summary?.unreadCount, loadingSummary)} icon="message" />
                <StatCard label="Action needed" value={formatMetric(summary?.actionRequiredCount, loadingSummary)} icon="alert" />
              </>
            ) : (
              <>
                <StatCard label="Active applications" value="0" icon="briefcase" />
                <StatCard label="Unread messages" value={formatMetric(summary?.unreadCount, loadingSummary)} icon="message" />
                <StatCard label="Action needed" value={formatMetric(summary?.actionRequiredCount, loadingSummary)} icon="alert" />
                <StatCard label="Pending delivery" value={formatMetric(summary?.pendingCount, loadingSummary)} icon="clock" />
              </>
            )}
          </div>

          <div className="animate-fade-up-3 dashboard-actions-section">
            <h2 className="dashboard-section-title">Quick actions</h2>
            <div className="dashboard-actions-grid">
              {isEmployer ? (
                <>
                  <ActionCard label="Manage jobs" icon="briefcase" onClick={() => navigate('/employer/jobs')} />
                  <ActionCard label="Post new job" icon="plus" onClick={() => navigate('/employer/jobs/new')} />
                  <ActionCard label="View applicants" icon="users" onClick={() => navigate('/employer/applications')} />
                  <ActionCard label="Notifications" icon="message" onClick={() => navigate('/notifications')} />
                </>
              ) : (
                <>
                  <ActionCard label="Browse jobs" icon="search" onClick={() => navigate('/jobs')} />
                  <ActionCard label="My applications" icon="document" onClick={() => navigate('/applications')} />
                  <ActionCard label="Profile" icon="profile" onClick={() => navigate('/profile')} />
                  <ActionCard label="Notifications" icon="message" onClick={() => navigate('/notifications')} />
                </>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function formatMetric(value: number | undefined, loading: boolean) {
  if (loading) {
    return '--';
  }
  return String(value ?? 0);
}

function StatCard({ label, value, icon }: { label: string; value: string; icon: IconName }) {
  return (
    <div className="solid-card dashboard-stat-card">
      <div className="dashboard-stat-icon">
        <Icon name={icon} />
      </div>
      <div>
        <p className="dashboard-stat-value">{value}</p>
        <p className="dashboard-stat-label">{label}</p>
      </div>
    </div>
  );
}

function ActionCard({ label, onClick, icon }: { label: string; onClick: () => void; icon: IconName }) {
  return (
    <button className="solid-card dashboard-action-card" onClick={onClick}>
      <div className="dashboard-action-icon">
        <Icon name={icon} />
      </div>
      <span className="dashboard-action-label">{label}</span>
    </button>
  );
}

type IconName = 'briefcase' | 'users' | 'message' | 'clock' | 'alert' | 'search' | 'document' | 'profile' | 'plus';

function Icon({ name }: { name: IconName }) {
  switch (name) {
    case 'briefcase':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <rect x="2" y="7" width="20" height="14" rx="2" />
          <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2" />
        </svg>
      );
    case 'users':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
      );
    case 'message':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
      );
    case 'clock':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12,6 12,12 16,14" />
        </svg>
      );
    case 'alert':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <path d="M12 9v4" />
          <path d="M12 17h.01" />
          <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
        </svg>
      );
    case 'search':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
      );
    case 'document':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14,2 14,8 20,8" />
          <line x1="12" y1="18" x2="12" y2="12" />
          <line x1="9" y1="15" x2="15" y2="15" />
        </svg>
      );
    case 'profile':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      );
    case 'plus':
      return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      );
  }
}
