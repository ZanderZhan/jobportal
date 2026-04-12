import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isEmployerRole } from '../../lib/authRoles';
import {
  formatApplicationStatus,
  getApplicationErrorMessage,
  getEmployerApplicationsForJob,
  getNextEmployerStatuses,
  type ApplicationRecord,
  type ApplicationStatus,
  updateApplicationStatus,
} from '../../lib/applicationApi';
import { formatDate, getJobById, type Job } from '../../lib/jobApi';
import '../../styles/dashboard.css';
import '../../styles/utilities.css';
import './MyApplicationsPage.css';
import './EmployerJobApplicationsPage.css';

function getStatusTone(status: ApplicationStatus) {
  if (status === 'HIRED') return 'success';
  if (status === 'REJECTED' || status === 'WITHDRAWN') return 'muted';
  if (status === 'INTERVIEW') return 'accent';
  return 'default';
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

export function EmployerJobApplicationsPage() {
  const { id } = useParams<{ id: string }>();
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [applications, setApplications] = useState<ApplicationRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const isEmployer = isEmployerRole(user?.role, user?.userType);
  const jobId = id ? Number(id) : null;

  const stats = useMemo(() => {
    return {
      total: applications.length,
      underReview: applications.filter((application) => application.status === 'UNDER_REVIEW').length,
      interview: applications.filter((application) => application.status === 'INTERVIEW').length,
    };
  }, [applications]);

  useEffect(() => {
    async function loadPage() {
      if (!isAuthenticated || !isEmployer || !jobId) {
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const [jobData, applicationsData] = await Promise.all([
          getJobById(jobId),
          getEmployerApplicationsForJob(jobId),
        ]);
        setJob(jobData);
        setApplications(applicationsData);
      } catch (requestError) {
        setError(getApplicationErrorMessage(requestError, 'Failed to load applicants for this job.'));
      } finally {
        setLoading(false);
      }
    }

    void loadPage();
  }, [isAuthenticated, isEmployer, jobId]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const handleStatusUpdate = async (applicationId: number, nextStatus: ApplicationStatus) => {
    setUpdatingId(applicationId);
    setError(null);
    try {
      const updated = await updateApplicationStatus(applicationId, {
        status: nextStatus,
        reason: `Moved to ${formatApplicationStatus(nextStatus)}`,
      });
      setApplications((current) =>
        current.map((application) => application.id === updated.id ? updated : application),
      );
    } catch (requestError) {
      setError(getApplicationErrorMessage(requestError, 'Failed to update application status.'));
    } finally {
      setUpdatingId(null);
    }
  };

  if (!isAuthenticated || !isEmployer) {
    return (
      <div className="employer-applications-page">
        <main className="employer-applications-shell">
          <section className="employer-applications-empty solid-card">
            <h1>Employer review only</h1>
            <p>This page is available to employer accounts only.</p>
            <div className="employer-applications-actions">
              <Link to="/dashboard" className="btn btn-secondary">Back to dashboard</Link>
              <Link to="/jobs" className="btn btn-primary">Browse jobs</Link>
            </div>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="employer-applications-page">
      <header className="dashboard-header employer-applications-topbar">
        <Link to="/" className="logo">
          <svg className="logo-emblem" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="44" height="44" rx="8" fill="#006B3C" />
            <text x="22" y="29" textAnchor="middle" fontFamily="Syne, sans-serif" fontWeight="800" fontSize="18" fill="white">UL</text>
            <path d="M22 36 C22 36 19 32 19 30 C19 28.5 20.5 27.5 22 27.5 C23.5 27.5 25 28.5 25 30 C25 32 22 36 22 36Z" fill="white" opacity="0.9" />
          </svg>
          Job<span>Portal</span>
        </Link>

        <div className="employer-applications-topbar-actions">
          <Link to="/dashboard" className="btn btn-secondary btn--sm">Homepage</Link>
          <Link to="/employer/jobs" className="btn btn-secondary btn--sm">Manage jobs</Link>
          <div className="jobs-topbar-user">
            <span className="role-badge">{user?.role?.replace('ROLE_', '').replace('_', ' ')}</span>
            <span className="jobs-topbar-email">{user?.email}</span>
          </div>
          <button onClick={handleLogout} className="btn btn-ghost btn--sm">Sign out</button>
        </div>
      </header>

      <main className="employer-applications-shell">
        <section className="employer-applications-hero solid-card">
          <div>
            <Link to="/employer/jobs" className="employer-applications-kicker">Back to jobs</Link>
            <h1>{job?.title ?? 'Job applications'}</h1>
            <p>
              Review applicants, move them through the hiring stages, and keep the timeline accurate for the student side.
            </p>
          </div>
          <div className="employer-applications-stats">
            <div className="employer-applications-stat">
              <strong>{stats.total}</strong>
              <span>Total applicants</span>
            </div>
            <div className="employer-applications-stat">
              <strong>{stats.underReview}</strong>
              <span>Under review</span>
            </div>
            <div className="employer-applications-stat">
              <strong>{stats.interview}</strong>
              <span>Interview stage</span>
            </div>
          </div>
        </section>

        {error && (
          <section className="employer-applications-error solid-card">
            <h2>Unable to load employer review</h2>
            <p>{error}</p>
          </section>
        )}

        {loading ? (
          <section className="employer-applications-empty solid-card">
            <div className="spinner spinner--lg" />
            <p>Loading applicants...</p>
          </section>
        ) : applications.length === 0 ? (
          <section className="employer-applications-empty solid-card">
            <h2>No applications yet</h2>
            <p>Student submissions for this job will appear here once they apply.</p>
          </section>
        ) : (
          <section className="employer-applications-list">
            {applications.map((application) => {
              const nextStatuses = getNextEmployerStatuses(application.status);
              return (
                <article key={application.id} className="employer-application-card solid-card">
                  <div className="employer-application-card-top">
                    <div>
                      <p className="employer-applications-kicker">Applicant</p>
                      <h2>{application.studentId}</h2>
                      <p>Application #{application.id}</p>
                    </div>
                    <span className={`applications-status applications-status--${getStatusTone(application.status)}`}>
                      {formatApplicationStatus(application.status)}
                    </span>
                  </div>

                  <dl className="employer-application-grid">
                    <div>
                      <dt>Submitted</dt>
                      <dd>{formatTimelineDate(application.submittedAt)}</dd>
                    </div>
                    <div>
                      <dt>Resume</dt>
                      <dd>{application.resumeReference}</dd>
                    </div>
                    <div>
                      <dt>Job status</dt>
                      <dd>{job?.status ?? 'Unknown'}</dd>
                    </div>
                    <div>
                      <dt>Created</dt>
                      <dd>{job ? formatDate(job.createdAt) : '—'}</dd>
                    </div>
                  </dl>

                  <div className="employer-application-actions">
                    <Link to={`/jobs/${application.jobId}`} className="btn btn-secondary">
                      View job
                    </Link>
                    {nextStatuses.length === 0 ? (
                      <span className="employer-application-note">No further employer actions are available for this status.</span>
                    ) : (
                      nextStatuses.map((nextStatus) => (
                        <button
                          key={nextStatus}
                          type="button"
                          className="btn btn-primary"
                          onClick={() => handleStatusUpdate(application.id, nextStatus)}
                          disabled={updatingId === application.id}
                        >
                          {updatingId === application.id ? 'Updating...' : `Move to ${formatApplicationStatus(nextStatus)}`}
                        </button>
                      ))
                    )}
                  </div>

                  <div className="employer-application-timeline">
                    <div className="employer-application-timeline-header">
                      <h3>Timeline</h3>
                      <span>{application.timeline.length} events</span>
                    </div>
                    {application.timeline.map((entry) => (
                      <div key={entry.id} className="employer-application-timeline-item">
                        <strong>{formatApplicationStatus(entry.newStatus)}</strong>
                        <span>{formatTimelineDate(entry.createdAt)}</span>
                        <p>{entry.reason}</p>
                      </div>
                    ))}
                  </div>
                </article>
              );
            })}
          </section>
        )}
      </main>
    </div>
  );
}
