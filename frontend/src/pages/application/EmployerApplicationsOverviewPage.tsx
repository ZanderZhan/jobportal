import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isEmployerRole } from '../../lib/authRoles';
import {
  formatApplicationStatus,
  getApplicationErrorMessage,
  getEmployerApplicationsForJob,
  type ApplicationRecord,
} from '../../lib/applicationApi';
import { formatDate, searchEmployerJobs, type Job } from '../../lib/jobApi';
import '../../styles/dashboard.css';
import '../../styles/utilities.css';
import './EmployerApplicationsOverviewPage.css';

interface EmployerApplicationBucket {
  job: Job;
  applications: ApplicationRecord[];
}

function getStatusSummary(applications: ApplicationRecord[]) {
  const summary = new Map<string, number>();
  applications.forEach((application) => {
    summary.set(application.status, (summary.get(application.status) ?? 0) + 1);
  });
  return Array.from(summary.entries());
}

export function EmployerApplicationsOverviewPage() {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [buckets, setBuckets] = useState<EmployerApplicationBucket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const isEmployer = isEmployerRole(user?.role, user?.userType);

  const stats = useMemo(() => {
    const totalJobs = buckets.length;
    const totalApplications = buckets.reduce((sum, bucket) => sum + bucket.applications.length, 0);
    const activePipelines = buckets.filter((bucket) =>
      bucket.applications.some((application) =>
        application.status === 'SUBMITTED'
        || application.status === 'UNDER_REVIEW'
        || application.status === 'INTERVIEW'),
    ).length;

    return { totalJobs, totalApplications, activePipelines };
  }, [buckets]);

  useEffect(() => {
    async function loadOverview() {
      if (!isAuthenticated || !isEmployer || !user?.id) {
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const jobsResponse = await searchEmployerJobs({
          employerId: user.id,
          page: 0,
          size: 100,
          sort: 'createdAt,desc',
        });

        const jobsWithApplications = await Promise.all(
          jobsResponse.content.map(async (job) => ({
            job,
            applications: await getEmployerApplicationsForJob(job.id),
          })),
        );

        setBuckets(jobsWithApplications.filter((bucket) => bucket.applications.length > 0));
      } catch (requestError) {
        setError(getApplicationErrorMessage(requestError, 'Failed to load applicant overview.'));
      } finally {
        setLoading(false);
      }
    }

    void loadOverview();
  }, [isAuthenticated, isEmployer, user?.id]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!isAuthenticated || !isEmployer) {
    return (
      <div className="employer-application-overview-page">
        <main className="employer-application-overview-shell">
          <section className="employer-application-overview-empty solid-card">
            <h1>Employer review only</h1>
            <p>This page is available to employer accounts only.</p>
            <div className="employer-application-overview-actions">
              <Link to="/dashboard" className="btn btn-secondary">Back to dashboard</Link>
              <Link to="/jobs" className="btn btn-primary">Browse jobs</Link>
            </div>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="employer-application-overview-page">
      <header className="dashboard-header employer-application-overview-topbar">
        <Link to="/" className="logo">
          <svg className="logo-emblem" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="44" height="44" rx="8" fill="#006B3C" />
            <text x="22" y="29" textAnchor="middle" fontFamily="Syne, sans-serif" fontWeight="800" fontSize="18" fill="white">UL</text>
            <path d="M22 36 C22 36 19 32 19 30 C19 28.5 20.5 27.5 22 27.5 C23.5 27.5 25 28.5 25 30 C25 32 22 36 22 36Z" fill="white" opacity="0.9" />
          </svg>
          Job<span>Portal</span>
        </Link>

        <div className="employer-application-overview-topbar-actions">
          <Link to="/dashboard" className="btn btn-secondary btn--sm">Homepage</Link>
          <Link to="/employer/jobs" className="btn btn-secondary btn--sm">Manage jobs</Link>
          <div className="jobs-topbar-user">
            <span className="role-badge">{user?.role?.replace('ROLE_', '').replace('_', ' ')}</span>
            <span className="jobs-topbar-email">{user?.email}</span>
          </div>
          <button onClick={handleLogout} className="btn btn-ghost btn--sm">Sign out</button>
        </div>
      </header>

      <main className="employer-application-overview-shell">
        <section className="employer-application-overview-hero solid-card">
          <div>
            <p className="employer-application-overview-kicker">Applicant overview</p>
            <h1>Review applicants by job</h1>
            <p>See which of your jobs have applications, then open the specific hiring pipeline for the role you want to review.</p>
          </div>
          <div className="employer-application-overview-stats">
            <div className="employer-application-overview-stat">
              <strong>{stats.totalJobs}</strong>
              <span>Jobs with applicants</span>
            </div>
            <div className="employer-application-overview-stat">
              <strong>{stats.totalApplications}</strong>
              <span>Total applications</span>
            </div>
            <div className="employer-application-overview-stat">
              <strong>{stats.activePipelines}</strong>
              <span>Active pipelines</span>
            </div>
          </div>
        </section>

        {error && (
          <section className="employer-application-overview-error solid-card">
            <h2>Unable to load applicants</h2>
            <p>{error}</p>
          </section>
        )}

        {loading ? (
          <section className="employer-application-overview-empty solid-card">
            <div className="spinner spinner--lg" />
            <p>Loading applicant overview...</p>
          </section>
        ) : buckets.length === 0 ? (
          <section className="employer-application-overview-empty solid-card">
            <h2>No applicants yet</h2>
            <p>Once students apply to one of your jobs, the review links will appear here.</p>
          </section>
        ) : (
          <section className="employer-application-overview-grid">
            {buckets.map((bucket) => (
              <article key={bucket.job.id} className="employer-application-overview-card solid-card">
                <div className="employer-application-overview-card-top">
                  <div>
                    <p className="employer-application-overview-kicker">Job</p>
                    <h2>{bucket.job.title}</h2>
                    <p>{bucket.job.company}</p>
                  </div>
                  <span className="status-badge status-active">{bucket.applications.length} applicants</span>
                </div>

                <dl className="employer-application-overview-card-grid">
                  <div>
                    <dt>Location</dt>
                    <dd>{bucket.job.location || '—'}</dd>
                  </div>
                  <div>
                    <dt>Posted</dt>
                    <dd>{formatDate(bucket.job.createdAt)}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>{bucket.job.status}</dd>
                  </div>
                </dl>

                <div className="employer-application-overview-summary">
                  {getStatusSummary(bucket.applications).map(([status, count]) => (
                    <span key={status} className="employer-application-overview-pill">
                      {count} {formatApplicationStatus(status as ApplicationRecord['status'])}
                    </span>
                  ))}
                </div>

                <div className="employer-application-overview-actions">
                  <Link to={`/employer/jobs/${bucket.job.id}/applications`} className="btn btn-primary">
                    Review applicants
                  </Link>
                  <Link to={`/jobs/${bucket.job.id}`} className="btn btn-secondary">
                    View job
                  </Link>
                </div>
              </article>
            ))}
          </section>
        )}
      </main>
    </div>
  );
}
