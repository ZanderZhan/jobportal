import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isEmployerRole } from '../../lib/authRoles';
import {
  type JobRequest,
  getJobById,
  createJob,
  updateJob,
  getJobErrorMessage,
  isJobAuthorizationError,
} from '../../lib/jobApi';
import './JobFormPage.css';

const EMPLOYMENT_TYPES = [
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
];

const JOB_STATUSES = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'CLOSED', label: 'Closed' },
];

interface RequirementItem {
  id: string;
  text: string;
}

export function JobFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const isEmployer = isEmployerRole(user?.role, user?.userType);
  const isEditMode = !!id;
  const requirementCounter = useRef(0);

  const [formData, setFormData] = useState<Omit<JobRequest, 'requirements'>>({
    title: '',
    description: '',
    company: '',
    location: '',
    employmentType: 'FULL_TIME',
    salaryMin: undefined,
    salaryMax: undefined,
    salaryCurrency: 'USD',
    status: 'DRAFT',
  });
  const [requirements, setRequirements] = useState<RequirementItem[]>([]);
  const [requirementInput, setRequirementInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isFetching, setIsFetching] = useState(isEditMode);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isEditMode && id) {
      fetchJob(parseInt(id));
    }
  }, [id, isEditMode]);

  const fetchJob = async (jobId: number) => {
    try {
      setIsFetching(true);
      const job = await getJobById(jobId);
      setFormData({
        title: job.title,
        description: job.description,
        company: job.company,
        location: job.location || '',
        employmentType: job.employmentType || 'FULL_TIME',
        salaryMin: job.salaryMin ?? undefined,
        salaryMax: job.salaryMax ?? undefined,
        salaryCurrency: job.salaryCurrency ?? 'USD',
        status: job.status,
      });
      setRequirements((job.requirements || []).map((text) => ({ id: `req-${++requirementCounter.current}`, text })));
    } catch (err) {
      const fallback = isJobAuthorizationError(err)
        ? 'You are not allowed to edit this job.'
        : 'Failed to load job';
      setError(getJobErrorMessage(err, fallback));
      console.error(err);
    } finally {
      setIsFetching(false);
    }
  };

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value ? parseFloat(value) : undefined,
    }));
  };

  const handleAddRequirement = () => {
    if (requirementInput.trim()) {
      setRequirements((prev) => [...prev, { id: `req-${++requirementCounter.current}`, text: requirementInput.trim() }]);
      setRequirementInput('');
    }
  };

  const handleRemoveRequirement = (id: string) => {
    setRequirements((prev) => prev.filter((r) => r.id !== id));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const payload: JobRequest = { ...formData, requirements: requirements.map((r) => r.text) };
      if (isEditMode && id) {
        await updateJob(parseInt(id), payload);
      } else {
        await createJob(payload);
      }
      navigate('/employer/jobs');
    } catch (err: unknown) {
      const fallback = isJobAuthorizationError(err)
        ? 'You are not allowed to create or edit jobs with this account.'
        : 'Failed to save job';
      setError(getJobErrorMessage(err, fallback));
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  if (authLoading || isFetching) {
    return (
      <div className="job-form-page">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !isEmployer) {
    return (
      <div className="job-form-page">
        <div className="access-denied">
          <h2>Access Denied</h2>
          <p>Only employers can create or edit jobs.</p>
          <Link to="/jobs" className="btn btn-primary">
            Browse Jobs
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="job-form-page">
      <div className="job-form-header">
        <div className="job-form-header-content">
          <Link to="/employer/jobs" className="back-link">
            ← Back to My Jobs
          </Link>
          <h1>{isEditMode ? 'Edit Job' : 'Create New Job'}</h1>
          <p>
            {isEditMode
              ? 'Update your job listing details'
              : 'Fill in the details to create a new job posting'}
          </p>
        </div>
      </div>

      <div className="job-form-container">
        <form onSubmit={handleSubmit} className="job-form">
          {error && <div className="form-error">{error}</div>}

          <div className="form-section">
            <h3>Basic Information</h3>

            <div className="form-group">
              <label htmlFor="title">Job Title *</label>
              <input
                type="text"
                id="title"
                name="title"
                value={formData.title}
                onChange={handleInputChange}
                required
                placeholder="e.g. Senior Software Engineer"
              />
            </div>

            <div className="form-group">
              <label htmlFor="company">Company Name *</label>
              <input
                type="text"
                id="company"
                name="company"
                value={formData.company}
                onChange={handleInputChange}
                required
                placeholder="e.g. Tech Corp"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="location">Location</label>
                <input
                  type="text"
                  id="location"
                  name="location"
                  value={formData.location}
                  onChange={handleInputChange}
                  placeholder="e.g. San Francisco, CA or Remote"
                />
              </div>

              <div className="form-group">
                <label htmlFor="employmentType">Employment Type</label>
                <select
                  id="employmentType"
                  name="employmentType"
                  value={formData.employmentType}
                  onChange={handleInputChange}
                >
                  {EMPLOYMENT_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="description">Job Description *</label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                required
                rows={8}
                placeholder="Describe the role, responsibilities, and what you're looking for..."
              />
            </div>
          </div>

          <div className="form-section">
            <h3>Compensation</h3>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="salaryMin">Minimum Salary</label>
                <input
                  type="number"
                  id="salaryMin"
                  name="salaryMin"
                  value={formData.salaryMin || ''}
                  onChange={handleNumberChange}
                  placeholder="e.g. 80000"
                  min="0"
                />
              </div>

              <div className="form-group">
                <label htmlFor="salaryMax">Maximum Salary</label>
                <input
                  type="number"
                  id="salaryMax"
                  name="salaryMax"
                  value={formData.salaryMax || ''}
                  onChange={handleNumberChange}
                  placeholder="e.g. 120000"
                  min="0"
                />
              </div>

              <div className="form-group form-group-small">
                <label htmlFor="salaryCurrency">Currency</label>
                <select
                  id="salaryCurrency"
                  name="salaryCurrency"
                  value={formData.salaryCurrency}
                  onChange={handleInputChange}
                >
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                  <option value="GBP">GBP</option>
                  <option value="CAD">CAD</option>
                  <option value="AUD">AUD</option>
                </select>
              </div>
            </div>
          </div>

          <div className="form-section">
            <h3>Requirements</h3>

            <div className="form-group">
              <label>Skills & Requirements</label>
              <div className="requirement-input-row">
                <input
                  type="text"
                  value={requirementInput}
                  onChange={(e) => setRequirementInput(e.target.value)}
                  placeholder="e.g. React, Python, 5+ years experience"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleAddRequirement();
                    }
                  }}
                />
                <button
                  type="button"
                  onClick={handleAddRequirement}
                  className="btn btn-secondary"
                >
                  Add
                </button>
              </div>

              {requirements.length > 0 && (
                <div className="requirements-list">
                  {requirements.map((req) => (
                    <span key={req.id} className="requirement-tag">
                      {req.text}
                      <button
                        type="button"
                        onClick={() => handleRemoveRequirement(req.id)}
                        className="remove-tag"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="form-section">
            <h3>Status</h3>

            <div className="form-group">
              <label htmlFor="status">Job Status</label>
              <select
                id="status"
                name="status"
                value={formData.status}
                onChange={handleInputChange}
              >
                {JOB_STATUSES.map((status) => (
                  <option key={status.value} value={status.value}>
                    {status.label}
                  </option>
                ))}
              </select>
              <p className="form-help">
                Draft jobs are not visible to job seekers. Set to Active to publish.
              </p>
            </div>
          </div>

          <div className="form-actions">
            <Link to="/employer/jobs" className="btn btn-secondary">
              Cancel
            </Link>
            <button type="submit" className="btn btn-primary" disabled={isLoading}>
              {isLoading
                ? 'Saving...'
                : isEditMode
                ? 'Update Job'
                : 'Create Job'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
