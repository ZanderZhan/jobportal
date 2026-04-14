import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isEmployerRole } from '../../lib/authRoles';
import {
  type Job,
  searchEmployerJobs,
  deleteJob,
  formatSalary,
  formatEmploymentType,
  formatDate,
} from '../../lib/jobApi';
import './EmployerJobsPage.css';

export function EmployerJobsPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const isEmployer = isEmployerRole(user?.role, user?.userType);

  useEffect(() => {
    if (isAuthenticated && isEmployer) {
      fetchJobs();
    }
  }, [isAuthenticated, isEmployer, page, statusFilter]);

  const fetchJobs = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await searchEmployerJobs({
        status: statusFilter || undefined,
        employerId: user!.id,
        page,
        size: 10,
        sort: 'createdAt,desc',
      });
      setJobs(response.content);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError('Failed to load jobs');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      setIsDeleting(true);
      await deleteJob(id);
      setDeleteConfirm(null);
      fetchJobs();
    } catch (err) {
      setError('Failed to delete job');
      console.error(err);
    } finally {
      setIsDeleting(false);
    }
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'status-active';
      case 'DRAFT':
        return 'status-draft';
      case 'CLOSED':
        return 'status-closed';
      default:
        return '';
    }
  };

  if (authLoading) {
    return (
      <div className="employer-jobs-page">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !isEmployer) {
    return (
      <div className="employer-jobs-page">
        <div className="access-denied">
          <h2>Access Denied</h2>
          <p>Only employers can access this page.</p>
          <Link to="/jobs" className="btn btn-primary">
            Browse Jobs
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="employer-jobs-page">
      <div className="employer-jobs-header">
        <div className="employer-jobs-header-content">
          <h1>My Job Listings</h1>
          <p>Manage your job postings</p>
          <div className="employer-jobs-header-actions">
            <Link to="/dashboard" className="btn btn-secondary">
              Homepage
            </Link>
          </div>
        </div>
      </div>

      <div className="employer-jobs-container">
        <div className="employer-jobs-toolbar">
          <div className="toolbar-filters">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="status-filter"
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="DRAFT">Draft</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>

          <Link to="/employer/jobs/new" className="btn btn-primary">
            + Create New Job
          </Link>
        </div>

        {error && <div className="error-message">{error}</div>}

        {isLoading ? (
          <div className="loading-container">
            <div className="loading-spinner"></div>
          </div>
        ) : jobs.length === 0 ? (
          <div className="empty-state">
            <h3>No jobs found</h3>
            <p>
              {statusFilter
                ? 'No jobs match the selected filter.'
                : "You haven't created any jobs yet."}
            </p>
            <Link to="/employer/jobs/new" className="btn btn-primary">
              Create Your First Job
            </Link>
          </div>
        ) : (
          <>
            <div className="jobs-table-container">
              <table className="jobs-table">
                <thead>
                  <tr>
                    <th>Job Title</th>
                    <th>Location</th>
                    <th>Type</th>
                    <th>Salary</th>
                    <th>Status</th>
                    <th>Posted</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job) => (
                    <tr key={job.id}>
                      <td>
                        <div className="job-title-cell">
                          <Link to={`/jobs/${job.id}`} className="job-title-link">
                            {job.title}
                          </Link>
                          <span className="job-company">{job.company}</span>
                        </div>
                      </td>
                      <td>{job.location || '—'}</td>
                      <td>{formatEmploymentType(job.employmentType)}</td>
                      <td>
                        {formatSalary(
                          job.salaryMin,
                          job.salaryMax,
                          job.salaryCurrency
                        )}
                      </td>
                      <td>
                        <span className={`status-badge ${getStatusClass(job.status)}`}>
                          {job.status}
                        </span>
                      </td>
                      <td>{formatDate(job.createdAt)}</td>
                      <td>
                        <div className="action-buttons">
                          <Link
                            to={`/employer/jobs/${job.id}/applications`}
                            className="btn btn-sm btn-primary"
                          >
                            Applicants
                          </Link>
                          <Link
                            to={`/employer/jobs/${job.id}/edit`}
                            className="btn btn-sm btn-secondary"
                          >
                            Edit
                          </Link>
                          {deleteConfirm === job.id ? (
                            <div className="delete-confirm">
                              <button
                                onClick={() => handleDelete(job.id)}
                                className="btn btn-sm btn-danger"
                                disabled={isDeleting}
                              >
                                {isDeleting ? '...' : 'Yes'}
                              </button>
                              <button
                                onClick={() => setDeleteConfirm(null)}
                                className="btn btn-sm btn-secondary"
                              >
                                No
                              </button>
                            </div>
                          ) : (
                            <button
                              onClick={() => setDeleteConfirm(job.id)}
                              className="btn btn-sm btn-danger-outline"
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="pagination">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="btn btn-secondary"
                >
                  Previous
                </button>
                <span className="page-info">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="btn btn-secondary"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
