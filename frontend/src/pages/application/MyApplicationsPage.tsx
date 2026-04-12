import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isStudentRole } from '../../lib/authRoles';
import {
  formatApplicationStatus,
  getApplicationErrorMessage,
  getStudentApplicationById,
  getStudentApplications,
  type ApplicationRecord,
  withdrawApplication,
} from '../../lib/applicationApi';
import '../job/JobsPage.css';
import '../../styles/dashboard.css';
import '../../styles/utilities.css';
import './MyApplicationsPage.css';

function getApplicationTone(status: ApplicationRecord['status']) {
  if (status === 'HIRED') return 'success';
  if (status === 'REJECTED' || status === 'WITHDRAWN') return 'muted';
  if (status === 'INTERVIEW') return 'accent';
  return 'default';
}

function canWithdraw(status: ApplicationRecord['status']) {
  return status === 'SUBMITTED' || status === 'UNDER_REVIEW';
}

function formatTimelineDate(value: string) {
  return new Intl.DateTimeFormat('en-IE', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function MyApplicationsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [applications, setApplications] = useState<ApplicationRecord[]>([]);
  const [selectedApplication, setSelectedApplication] = useState<ApplicationRecord | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [isWithdrawing, setIsWithdrawing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);

  const isStudent = isStudentRole(user?.role, user?.userType);
  const selectedId = searchParams.get('selected');

  const stats = useMemo(() => {
    const total = applications.length;
    const active = applications.filter((application) => canWithdraw(application.status)).length;
    const interviews = applications.filter((application) => application.status === 'INTERVIEW').length;
    return { total, active, interviews };
  }, [applications]);

  useEffect(() => {
    async function loadApplications() {
      if (!isStudent) {
        setApplications([]);
        setSelectedApplication(null);
        setError(null);
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);
      try {
        const data = await getStudentApplications();
        setApplications(data);
      } catch (requestError) {
        setError(getApplicationErrorMessage(requestError, 'Failed to load applications. Please try again.'));
      } finally {
        setIsLoading(false);
      }
    }

    void loadApplications();
  }, [isStudent]);

  useEffect(() => {
    if (!isStudent) {
      setDetailError(null);
      setSelectedApplication(null);
      return;
    }

    if (applications.length === 0) {
      setDetailError(null);
      setSelectedApplication(null);
      if (selectedId) {
        setSearchParams({}, { replace: true });
      }
      return;
    }

    const normalizedSelectedId = selectedId ? Number(selectedId) : NaN;
    const selectedExists = Number.isFinite(normalizedSelectedId)
      && applications.some((application) => application.id === normalizedSelectedId);
    const nextSelectedId = selectedExists ? normalizedSelectedId : applications[0].id;

    if (String(nextSelectedId) !== selectedId) {
      setSearchParams({ selected: String(nextSelectedId) }, { replace: true });
      return;
    }

    async function loadSelectedApplication() {
      if (!selectedId) {
        setSelectedApplication(null);
        return;
      }

      setIsDetailLoading(true);
      setDetailError(null);
      try {
        const data = await getStudentApplicationById(Number(selectedId));
        setSelectedApplication(data);
      } catch (requestError) {
        setSelectedApplication(null);
        setDetailError(getApplicationErrorMessage(requestError, 'Failed to load application details.'));
      } finally {
        setIsDetailLoading(false);
      }
    }

    void loadSelectedApplication();
  }, [applications, isStudent, selectedId, setSearchParams]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const handleSelect = (applicationId: number) => {
    setSearchParams({ selected: String(applicationId) });
  };

  const handleWithdraw = async () => {
    if (!selectedApplication) {
      return;
    }

    setIsWithdrawing(true);
    setDetailError(null);
    try {
      const updated = await withdrawApplication(selectedApplication.id);
      setSelectedApplication(updated);
      setApplications((current) =>
        current.map((application) =>
          application.id === updated.id ? updated : application,
        ),
      );
    } catch (requestError) {
      setDetailError(getApplicationErrorMessage(requestError, 'Failed to withdraw application.'));
    } finally {
      setIsWithdrawing(false);
    }
  };

  if (!isStudent) {
    return (
      <div className="applications-page">
        <div className="app-bg">
          <div className="app-bg__mesh mesh--dim" />
          <div className="app-bg__grid mesh--dimmer" />
          <div className="app-bg__grain" />
        </div>
        <main className="applications-shell">
          <div className="applications-access solid-card">
            <p className="applications-kicker">Student area</p>
            <h1>My Applications</h1>
            <p>This page is available to student accounts only.</p>
            <div className="applications-access-actions">
              <Link to="/dashboard" className="btn btn-secondary">Back to dashboard</Link>
              <Link to="/jobs" className="btn btn-primary">Browse jobs</Link>
            </div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="applications-page">
      <div className="app-bg">
        <div className="app-bg__mesh mesh--dim" />
        <div className="app-bg__grid mesh--dimmer" />
        <div className="app-bg__grain" />
      </div>

      <header className="dashboard-header jobs-topbar">
        <Link to="/" className="logo">
          <svg className="logo-emblem" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="44" height="44" rx="8" fill="#006B3C" />
            <text x="22" y="29" textAnchor="middle" fontFamily="Syne, sans-serif" fontWeight="800" fontSize="18" fill="white">UL</text>
            <path d="M22 36 C22 36 19 32 19 30 C19 28.5 20.5 27.5 22 27.5 C23.5 27.5 25 28.5 25 30 C25 32 22 36 22 36Z" fill="white" opacity="0.9" />
          </svg>
          Job<span>Portal</span>
        </Link>

        <div className="jobs-topbar-actions">
          <Link to="/jobs" className="btn btn-secondary btn--sm">Browse jobs</Link>
          <Link to="/dashboard" className="btn btn-ghost btn--sm">Dashboard</Link>
          <div className="jobs-topbar-user">
            <span className="role-badge">{user?.role?.replace('ROLE_', '').replace('_', ' ')}</span>
            <span className="jobs-topbar-email">{user?.email}</span>
          </div>
          <button onClick={handleLogout} className="btn btn-ghost btn--sm">Sign out</button>
        </div>
      </header>

      <main className="applications-shell">
        <section className="applications-hero solid-card">
          <div className="applications-hero-copy">
            <Link to="/dashboard" className="applications-hero-kicker">Homepage</Link>
            <h1>My applications</h1>
            <p>Track your submissions, review each timeline, and withdraw when the current stage still allows it.</p>
          </div>
          <div className="applications-hero-stats">
            <div className="applications-hero-stat">
              <strong>{stats.total}</strong>
              <span>Total submissions</span>
            </div>
            <div className="applications-hero-stat">
              <strong>{stats.active}</strong>
              <span>Still active</span>
            </div>
            <div className="applications-hero-stat">
              <strong>{stats.interviews}</strong>
              <span>Interview stage</span>
            </div>
          </div>
        </section>

        {error ? (
          <section className="applications-error solid-card">
            <h2>Unable to load applications</h2>
            <p>{error}</p>
          </section>
        ) : (
          <div className="applications-layout">
            <section className="applications-list solid-card">
              <div className="applications-section-header">
                <div>
                  <p className="applications-kicker">Your submissions</p>
                  <h2>Application history</h2>
                </div>
                <Link to="/jobs" className="btn btn-secondary btn--sm">Find more jobs</Link>
              </div>

              {isLoading ? (
                <div className="applications-empty-state">
                  <div className="spinner spinner--lg" />
                  <p>Loading applications...</p>
                </div>
              ) : applications.length === 0 ? (
                <div className="applications-empty-state">
                  <h3>No applications yet</h3>
                  <p>When you apply for a role, it will appear here with its full timeline.</p>
                  <Link to="/jobs" className="btn btn-primary">Browse jobs</Link>
                </div>
              ) : (
                <div className="applications-list-items">
                  {applications.map((application) => {
                    const isSelected = application.id === selectedApplication?.id;
                    return (
                      <button
                        key={application.id}
                        type="button"
                        className={`applications-list-item ${isSelected ? 'is-selected' : ''}`}
                        onClick={() => handleSelect(application.id)}
                      >
                        <div className="applications-list-item-top">
                          <div>
                            <strong>{application.jobTitle}</strong>
                            <span>Application #{application.id}</span>
                          </div>
                          <span className={`applications-status applications-status--${getApplicationTone(application.status)}`}>
                            {formatApplicationStatus(application.status)}
                          </span>
                        </div>
                        <div className="applications-list-item-meta">
                          <span>Job ID {application.jobId}</span>
                          <span>Submitted {formatTimelineDate(application.submittedAt)}</span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </section>

            <aside className="applications-detail solid-card">
              <div className="applications-section-header">
                <div>
                  <p className="applications-kicker">Application detail</p>
                  <h2>Selected record</h2>
                </div>
              </div>

              {detailError ? (
                <div className="applications-inline-error">
                  <p>{detailError}</p>
                </div>
              ) : isDetailLoading ? (
                <div className="applications-empty-state applications-empty-state--compact">
                  <div className="spinner" />
                  <p>Loading application details...</p>
                </div>
              ) : !selectedApplication ? (
                <div className="applications-empty-state applications-empty-state--compact">
                  <h3>Select an application</h3>
                  <p>Choose a submission from the list to inspect its status and timeline.</p>
                </div>
              ) : (
                <div className="applications-detail-content">
                  <div className="applications-detail-card">
                    <div className="applications-detail-top">
                      <div>
                        <h3>{selectedApplication.jobTitle}</h3>
                        <p>Application #{selectedApplication.id}</p>
                      </div>
                      <span className={`applications-status applications-status--${getApplicationTone(selectedApplication.status)}`}>
                        {formatApplicationStatus(selectedApplication.status)}
                      </span>
                    </div>

                    <dl className="applications-detail-grid">
                      <div>
                        <dt>Job ID</dt>
                        <dd>{selectedApplication.jobId}</dd>
                      </div>
                      <div>
                        <dt>Employer</dt>
                        <dd>{selectedApplication.employerId ?? 'Not captured for this posting'}</dd>
                      </div>
                      <div>
                        <dt>Resume reference</dt>
                        <dd>{selectedApplication.resumeReference}</dd>
                      </div>
                      <div>
                        <dt>Last updated</dt>
                        <dd>{formatTimelineDate(selectedApplication.updatedAt)}</dd>
                      </div>
                    </dl>
                  </div>

                  <div className="applications-timeline">
                    <div className="applications-inline-header">
                      <h3>Timeline</h3>
                      <span>{selectedApplication.timeline.length} events</span>
                    </div>

                    {selectedApplication.timeline.map((entry) => (
                      <div key={entry.id} className="applications-timeline-item">
                        <div className="applications-timeline-marker" />
                        <div className="applications-timeline-content">
                          <div className="applications-timeline-top">
                            <strong>{formatApplicationStatus(entry.newStatus)}</strong>
                            <span>{formatTimelineDate(entry.createdAt)}</span>
                          </div>
                          <p>{entry.reason}</p>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="applications-detail-actions">
                    <Link to={`/jobs/${selectedApplication.jobId}`} className="btn btn-secondary">View job</Link>
                    {canWithdraw(selectedApplication.status) && (
                      <button
                        type="button"
                        className="btn btn-primary"
                        onClick={handleWithdraw}
                        disabled={isWithdrawing}
                      >
                        {isWithdrawing ? 'Withdrawing...' : 'Withdraw application'}
                      </button>
                    )}
                  </div>
                </div>
              )}
            </aside>
          </div>
        )}
      </main>
    </div>
  );
}
