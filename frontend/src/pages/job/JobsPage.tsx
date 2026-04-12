import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import JobCard from '../../components/JobCard';
import { searchJobs, type Job, type JobSearchParams, type PagedResponse } from '../../lib/jobApi';
import './JobsPage.css';

const EMPLOYMENT_TYPES = [
  { value: '', label: 'All Types' },
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
];

export default function JobsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pagination, setPagination] = useState({
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    size: 12,
  });

  // Filter state
  const [searchTitle, setSearchTitle] = useState(searchParams.get('title') || '');
  const [searchLocation, setSearchLocation] = useState(searchParams.get('location') || '');
  const [employmentType, setEmploymentType] = useState(searchParams.get('employmentType') || '');

  const fetchJobs = useCallback(async (params: JobSearchParams) => {
    setLoading(true);
    setError(null);
    try {
      const response: PagedResponse<Job> = await searchJobs({
        ...params,
        status: 'ACTIVE', // Only show active jobs
        size: pagination.size,
      });
      setJobs(response.content);
      setPagination(prev => ({
        ...prev,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        currentPage: response.number,
      }));
    } catch (err) {
      setError('Failed to load jobs. Please try again.');
      console.error('Error fetching jobs:', err);
    } finally {
      setLoading(false);
    }
  }, [pagination.size]);

  useEffect(() => {
    const params: JobSearchParams = {
      title: searchParams.get('title') || undefined,
      location: searchParams.get('location') || undefined,
      employmentType: searchParams.get('employmentType') || undefined,
      page: parseInt(searchParams.get('page') || '0'),
    };
    fetchJobs(params);
  }, [searchParams, fetchJobs]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (searchTitle) params.set('title', searchTitle);
    if (searchLocation) params.set('location', searchLocation);
    if (employmentType) params.set('employmentType', employmentType);
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleClearFilters = () => {
    setSearchTitle('');
    setSearchLocation('');
    setEmploymentType('');
    setSearchParams(new URLSearchParams());
  };

  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', page.toString());
    setSearchParams(params);
  };

  const hasFilters = searchTitle || searchLocation || employmentType;

  return (
    <div className="jobs-page">
      <header className="jobs-header">
        <div className="jobs-header-content">
          <h1>Find Your Next Opportunity</h1>
          <p>Discover jobs that match your skills and career goals</p>
        </div>
      </header>

      <div className="jobs-container">
        <aside className="jobs-filters">
          <form onSubmit={handleSearch}>
            <div className="filter-section">
              <h3>Search</h3>
              <div className="filter-group">
                <label htmlFor="search-title">Job Title</label>
                <input
                  id="search-title"
                  type="text"
                  placeholder="e.g., Software Engineer"
                  value={searchTitle}
                  onChange={(e) => setSearchTitle(e.target.value)}
                  className="input"
                />
              </div>
              <div className="filter-group">
                <label htmlFor="search-location">Location</label>
                <input
                  id="search-location"
                  type="text"
                  placeholder="e.g., San Francisco"
                  value={searchLocation}
                  onChange={(e) => setSearchLocation(e.target.value)}
                  className="input"
                />
              </div>
            </div>

            <div className="filter-section">
              <h3>Employment Type</h3>
              <div className="filter-group">
                <select
                  value={employmentType}
                  onChange={(e) => setEmploymentType(e.target.value)}
                  className="input"
                >
                  {EMPLOYMENT_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="filter-actions">
              <button type="submit" className="btn btn-primary">
                Search Jobs
              </button>
              {hasFilters && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={handleClearFilters}
                >
                  Clear Filters
                </button>
              )}
            </div>
          </form>
        </aside>

        <main className="jobs-main">
          <div className="jobs-results-header">
            <p className="jobs-count">
              {loading ? 'Loading...' : `${pagination.totalElements} jobs found`}
            </p>
          </div>

          {error && (
            <div className="jobs-error">
              <p>{error}</p>
              <button onClick={() => fetchJobs({})} className="btn btn-secondary">
                Try Again
              </button>
            </div>
          )}

          {loading ? (
            <div className="jobs-loading">
              <div className="spinner"></div>
              <p>Loading jobs...</p>
            </div>
          ) : jobs.length === 0 ? (
            <div className="jobs-empty">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="11" cy="11" r="8"/>
                <path d="m21 21-4.35-4.35"/>
              </svg>
              <h3>No jobs found</h3>
              <p>Try adjusting your search filters or check back later.</p>
              {hasFilters && (
                <button onClick={handleClearFilters} className="btn btn-primary">
                  Clear Filters
                </button>
              )}
            </div>
          ) : (
            <>
              <div className="jobs-grid">
                {jobs.map((job) => (
                  <JobCard key={job.id} job={job} />
                ))}
              </div>

              {pagination.totalPages > 1 && (
                <div className="jobs-pagination">
                  <button
                    className="btn btn-secondary"
                    disabled={pagination.currentPage === 0}
                    onClick={() => handlePageChange(pagination.currentPage - 1)}
                  >
                    Previous
                  </button>
                  <span className="pagination-info">
                    Page {pagination.currentPage + 1} of {pagination.totalPages}
                  </span>
                  <button
                    className="btn btn-secondary"
                    disabled={pagination.currentPage >= pagination.totalPages - 1}
                    onClick={() => handlePageChange(pagination.currentPage + 1)}
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </main>
      </div>
    </div>
  );
}
