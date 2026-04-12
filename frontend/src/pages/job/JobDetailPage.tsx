import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isEmployerRole, isStudentRole } from '../../lib/authRoles';
import { createApplication, getApplicationErrorMessage } from '../../lib/applicationApi';
import { getJobById, deleteJob, type Job, formatSalary, formatEmploymentType, formatDate } from '../../lib/jobApi';
import './JobDetailPage.css';

export default function JobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [job, setJob] = useState<Job | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [selectedResumeFile, setSelectedResumeFile] = useState<File | null>(null);
  const [applicationError, setApplicationError] = useState<string | null>(null);
  const [applicationSuccess, setApplicationSuccess] = useState<string | null>(null);
  const [isSubmittingApplication, setIsSubmittingApplication] = useState(false);

  const isJobOwner = isAuthenticated && isEmployerRole(user?.role, user?.userType) && !!job && !!user && job.employerId === user.id;
  const isStudent = isStudentRole(user?.role, user?.userType);

  useEffect(() => {
    async function fetchJob() {
      if (!id) return;
      
      setLoading(true);
      setError(null);
      try {
        const data = await getJobById(parseInt(id));
        setJob(data);
      } catch (err) {
        setError('Job not found or failed to load.');
        console.error('Error fetching job:', err);
      } finally {
        setLoading(false);
      }
    }
    
    fetchJob();
  }, [id]);

  const handleDelete = async () => {
    if (!id) return;
    try {
      setIsDeleting(true);
      await deleteJob(parseInt(id));
      navigate('/employer/jobs');
    } catch (err) {
      setError('Failed to delete job');
      console.error(err);
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  const handleApply = async () => {
    if (!job) {
      return;
    }
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!isStudent) {
      setApplicationError('Only student accounts can submit applications.');
      return;
    }
    if (!selectedResumeFile || !user?.id) {
      setApplicationError('Please upload your resume before submitting the application.');
      return;
    }

    setIsSubmittingApplication(true);
    setApplicationError(null);
    setApplicationSuccess(null);
    try {
      await createApplication({
        jobId: job.id,
        resumeReference: buildResumeReference(user.id, selectedResumeFile),
      });
      setApplicationSuccess('Application submitted. You can now track it from My Applications.');
      setSelectedResumeFile(null);
    } catch (requestError) {
      setApplicationError(getApplicationErrorMessage(requestError, 'Failed to submit application.'));
    } finally {
      setIsSubmittingApplication(false);
    }
  };

  const handleResumeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;
    setSelectedResumeFile(file);
    setApplicationError(null);
    setApplicationSuccess(null);
  };

  if (loading) {
    return (
      <div className="job-detail-page">
        <div className="job-detail-loading">
          <div className="spinner"></div>
          <p>Loading job details...</p>
        </div>
      </div>
    );
  }

  if (error || !job) {
    return (
      <div className="job-detail-page">
        <div className="job-detail-error">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <h2>Job Not Found</h2>
          <p>{error}</p>
          <Link to="/jobs" className="btn btn-primary">Browse All Jobs</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="job-detail-page">
      <div className="job-detail-header">
        <button onClick={() => navigate(-1)} className="back-button">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="m15 18-6-6 6-6"/>
          </svg>
          Back
        </button>

        {isJobOwner && (
          <div className="employer-actions">
            <Link to={`/employer/jobs/${id}/edit`} className="btn btn-secondary">
              Edit Job
            </Link>
            {showDeleteConfirm ? (
              <div className="delete-confirm-inline">
                <span>Delete this job?</span>
                <button
                  onClick={handleDelete}
                  className="btn btn-danger"
                  disabled={isDeleting}
                >
                  {isDeleting ? 'Deleting...' : 'Yes, Delete'}
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
              </div>
            ) : (
              <button
                onClick={() => setShowDeleteConfirm(true)}
                className="btn btn-danger-outline"
              >
                Delete Job
              </button>
            )}
          </div>
        )}
      </div>

      <div className="job-detail-container">
        <main className="job-detail-main">
          <div className="job-detail-card">
            <div className="job-detail-title-section">
              <div className="job-company-logo">
                {job.company.charAt(0).toUpperCase()}
              </div>
              <div>
                <h1>{job.title}</h1>
                <p className="job-company-name">{job.company}</p>
              </div>
            </div>

            <div className="job-meta-grid">
              {job.location && (
                <div className="job-meta-item">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                    <circle cx="12" cy="10" r="3"/>
                  </svg>
                  <div>
                    <span className="meta-label">Location</span>
                    <span className="meta-value">{job.location}</span>
                  </div>
                </div>
              )}
              
              <div className="job-meta-item">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                  <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                </svg>
                <div>
                  <span className="meta-label">Employment Type</span>
                  <span className="meta-value">{formatEmploymentType(job.employmentType)}</span>
                </div>
              </div>

              <div className="job-meta-item">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="12" y1="1" x2="12" y2="23"/>
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                </svg>
                <div>
                  <span className="meta-label">Salary</span>
                  <span className="meta-value salary">{formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency)}</span>
                </div>
              </div>

              <div className="job-meta-item">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                  <line x1="16" y1="2" x2="16" y2="6"/>
                  <line x1="8" y1="2" x2="8" y2="6"/>
                  <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                <div>
                  <span className="meta-label">Posted</span>
                  <span className="meta-value">{formatDate(job.createdAt)}</span>
                </div>
              </div>
            </div>

            <section className="job-section">
              <h2>Description</h2>
              <div className="job-description">
                {job.description.split('\n').map((paragraph: string, index: number) => (
                  <p key={index}>{paragraph}</p>
                ))}
              </div>
            </section>

            {job.requirements.length > 0 && (
              <section className="job-section">
                <h2>Requirements</h2>
                <ul className="job-requirements-list">
                  {job.requirements.map((req: string, index: number) => (
                    <li key={index}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                      {req}
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </div>
        </main>

        <aside className="job-detail-sidebar">
          <div className="job-apply-card">
            <h3>Interested in this job?</h3>
            {isJobOwner ? (
              <p>You own this posting, so applications are managed from the employer workflow instead.</p>
            ) : !isAuthenticated ? (
              <>
                <p>Sign in as a student to submit an application and track its status.</p>
                <button className="btn btn-primary btn-lg" onClick={() => navigate('/login')}>
                  Sign In To Apply
                </button>
              </>
            ) : !isStudent ? (
              <p>Only student accounts can submit applications for open roles.</p>
            ) : (
              <>
                <p>Submit your application and start tracking it from your student dashboard.</p>
                <label className="job-apply-field">
                  <span>Upload resume</span>
                  <div className="job-apply-upload">
                    <input
                      id="job-resume-upload"
                      className="job-apply-upload-input"
                      type="file"
                      accept=".pdf,.doc,.docx"
                      onChange={handleResumeChange}
                    />
                    <label htmlFor="job-resume-upload" className="btn btn-secondary">
                      Choose Resume
                    </label>
                    <span className="job-apply-upload-text">
                      {selectedResumeFile ? selectedResumeFile.name : 'PDF, DOC, or DOCX'}
                    </span>
                  </div>
                </label>
                {applicationError && <p className="job-apply-error">{applicationError}</p>}
                {applicationSuccess && <p className="job-apply-success">{applicationSuccess}</p>}
                <button
                  className="btn btn-primary btn-lg"
                  onClick={handleApply}
                  disabled={isSubmittingApplication}
                >
                  {isSubmittingApplication ? 'Submitting...' : 'Apply Now'}
                </button>
                <button className="btn btn-secondary" onClick={() => navigate('/applications')}>
                  View My Applications
                </button>
              </>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}

function buildResumeReference(userId: string, file: File) {
  const sanitizedName = file.name.replace(/[^a-zA-Z0-9._-]/g, '-');
  return `upload://${userId}/${Date.now()}-${sanitizedName}`;
}
