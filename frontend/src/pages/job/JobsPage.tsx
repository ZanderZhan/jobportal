import {
  startTransition,
  useEffect,
  useRef,
  useState,
} from 'react';
import { useSearchParams } from 'react-router-dom';
import JobCard from '../../components/JobCard';
import { useAuth } from '../../hooks/useAuth';
import {
  formatEmploymentType,
  getSearchDiscovery,
  searchJobs,
  trackSearchAbandon,
  trackSearchClick,
  type FacetValueCount,
  type Job,
  type JobSearchParams,
  type SearchDiscoveryResponse,
} from '../../lib/jobApi';
import './JobsPage.css';

const EMPLOYMENT_TYPES = [
  { value: '', label: 'All Types' },
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
];

interface ActiveSearchSession {
  sessionId: string;
  clicked: boolean;
}

interface AppliedFilterChip {
  key: 'title' | 'company' | 'location' | 'employmentType';
  label: string;
  value: string;
}

function buildSearchUrlParams(params: JobSearchParams) {
  const searchParams = new URLSearchParams();
  if (params.title) searchParams.set('title', params.title);
  if (params.company) searchParams.set('company', params.company);
  if (params.location) searchParams.set('location', params.location);
  if (params.employmentType) searchParams.set('employmentType', params.employmentType);
  searchParams.set('page', String(params.page ?? 0));
  return searchParams;
}

function createSearchSessionId() {
  return globalThis.crypto?.randomUUID?.() ?? `search-${Date.now()}`;
}

function normalizeText(value: string | null | undefined) {
  return value?.trim().toLowerCase() ?? '';
}

function buildAppliedFilterChips(params: JobSearchParams): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (params.title) {
    chips.push({ key: 'title', label: 'Title', value: params.title });
  }

  if (params.company) {
    chips.push({ key: 'company', label: 'Company', value: params.company });
  }

  if (params.location) {
    chips.push({ key: 'location', label: 'Location', value: params.location });
  }

  if (params.employmentType) {
    chips.push({
      key: 'employmentType',
      label: 'Type',
      value: formatEmploymentType(params.employmentType),
    });
  }

  return chips;
}

function buildSearchHeading(params: JobSearchParams) {
  if (params.title) {
    return `Results for "${params.title}"`;
  }

  if (params.company && params.location) {
    return `${params.company} roles in ${params.location}`;
  }

  if (params.company) {
    return `Open roles at ${params.company}`;
  }

  if (params.location) {
    return `Open roles in ${params.location}`;
  }

  if (params.employmentType) {
    return `${formatEmploymentType(params.employmentType)} roles`;
  }

  return 'Explore active opportunities';
}

function buildSearchDescription(
  params: JobSearchParams,
  totalElements: number,
) {
  const parts: string[] = [];

  if (params.title || params.company || params.location || params.employmentType) {
    parts.push(`${totalElements} matching roles available now.`);
  } else {
    parts.push(`${totalElements} active roles ready to browse.`);
  }

  parts.push('Use related searches and suggested filters to tighten the shortlist faster.');

  return parts.join(' ');
}

function buildJobInsights(job: Job, params: JobSearchParams) {
  const insights: string[] = [];
  const normalizedTitle = normalizeText(params.title);
  const normalizedCompany = normalizeText(params.company);
  const normalizedLocation = normalizeText(params.location);

  if (
    normalizedTitle &&
    (normalizeText(job.title).includes(normalizedTitle) ||
      normalizeText(job.description).includes(normalizedTitle))
  ) {
    insights.push('Title match');
  }

  if (
    normalizedCompany &&
    normalizeText(job.company).includes(normalizedCompany)
  ) {
    insights.push('Company match');
  }

  if (
    normalizedLocation &&
    normalizeText(job.location).includes(normalizedLocation)
  ) {
    insights.push('Location match');
  }

  if (
    params.employmentType &&
    job.employmentType &&
    params.employmentType === job.employmentType
  ) {
    insights.push(formatEmploymentType(job.employmentType));
  }

  return insights.slice(0, 3);
}

function buildDescriptionPreview(description: string) {
  const normalizedDescription = description.replace(/\s+/g, ' ').trim();
  if (normalizedDescription.length <= 160) {
    return normalizedDescription;
  }

  return `${normalizedDescription.slice(0, 157).trimEnd()}...`;
}

export default function JobsPage() {
  const { user } = useAuth();
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
  const [discovery, setDiscovery] = useState<SearchDiscoveryResponse>({
    relatedSearches: [],
    suggestedLocations: [],
    suggestedCompanies: [],
    suggestedEmploymentTypes: [],
  });
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  const [searchTitle, setSearchTitle] = useState(searchParams.get('title') || '');
  const [searchCompany, setSearchCompany] = useState(searchParams.get('company') || '');
  const [searchLocation, setSearchLocation] = useState(searchParams.get('location') || '');
  const [employmentType, setEmploymentType] = useState(searchParams.get('employmentType') || '');

  const activeSearchRef = useRef<ActiveSearchSession | null>(null);

  const currentSearchParams: JobSearchParams = {
    title: searchParams.get('title') || undefined,
    company: searchParams.get('company') || undefined,
    location: searchParams.get('location') || undefined,
    employmentType: searchParams.get('employmentType') || undefined,
    page: parseInt(searchParams.get('page') || '0', 10),
  };

  const hasDraftFilters = Boolean(searchTitle || searchCompany || searchLocation || employmentType);
  const appliedFilterChips = buildAppliedFilterChips(currentSearchParams);
  const hasAppliedFilters = appliedFilterChips.length > 0;

  useEffect(() => {
    setSearchTitle(searchParams.get('title') || '');
    setSearchCompany(searchParams.get('company') || '');
    setSearchLocation(searchParams.get('location') || '');
    setEmploymentType(searchParams.get('employmentType') || '');
  }, [searchParams]);

  useEffect(() => {
    let ignore = false;
    const previousSearch = activeSearchRef.current;
    if (previousSearch && !previousSearch.clicked) {
      void trackSearchAbandon(user?.id, previousSearch.sessionId);
    }

    const sessionId = createSearchSessionId();
    activeSearchRef.current = { sessionId, clicked: false };

    const fetchJobsAndDiscovery = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await searchJobs(
          {
            ...currentSearchParams,
            status: 'ACTIVE',
            size: pagination.size,
          },
          {
            userId: user?.id,
            sessionId,
          },
        );

        if (ignore) {
          return;
        }

        setJobs(response.content);
        setPagination((prev) => ({
          ...prev,
          totalElements: response.totalElements,
          totalPages: response.totalPages,
          currentPage: response.number,
        }));
      } catch (err) {
        if (!ignore) {
          setError('Failed to load jobs. Please try again.');
          console.error('Error fetching jobs:', err);
        }
        return;
      }

      try {
        const discoveryResponse = await getSearchDiscovery({
          ...currentSearchParams,
          status: 'ACTIVE',
        });

        if (!ignore) {
          setDiscovery(discoveryResponse);
        }
      } catch (err) {
        if (!ignore) {
          setDiscovery({
            relatedSearches: [],
            suggestedLocations: [],
            suggestedCompanies: [],
            suggestedEmploymentTypes: [],
          });
          console.error('Error fetching search discovery:', err);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    void fetchJobsAndDiscovery();

    return () => {
      ignore = true;
    };
  }, [
    currentSearchParams.title,
    currentSearchParams.company,
    currentSearchParams.location,
    currentSearchParams.employmentType,
    currentSearchParams.page,
    pagination.size,
    user?.id,
  ]);

  useEffect(() => {
    return () => {
      const activeSearch = activeSearchRef.current;
      if (activeSearch && !activeSearch.clicked) {
        void trackSearchAbandon(user?.id, activeSearch.sessionId);
      }
    };
  }, [user?.id]);

  const updateSearchParams = (params: JobSearchParams) => {
    setShowMobileFilters(false);
    startTransition(() => {
      setSearchParams(buildSearchUrlParams(params));
    });
  };

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    updateSearchParams({
      title: searchTitle || undefined,
      company: searchCompany || undefined,
      location: searchLocation || undefined,
      employmentType: employmentType || undefined,
      page: 0,
    });
  };

  const handleClearFilters = () => {
    setSearchTitle('');
    setSearchCompany('');
    setSearchLocation('');
    setEmploymentType('');
    updateSearchParams({ page: 0 });
  };

  const handleClearAppliedFilter = (field: AppliedFilterChip['key']) => {
    const nextParams: JobSearchParams = {
      ...currentSearchParams,
      page: 0,
    };

    if (field === 'title') {
      setSearchTitle('');
      nextParams.title = undefined;
    } else if (field === 'company') {
      setSearchCompany('');
      nextParams.company = undefined;
    } else if (field === 'location') {
      setSearchLocation('');
      nextParams.location = undefined;
    } else {
      setEmploymentType('');
      nextParams.employmentType = undefined;
    }

    updateSearchParams(nextParams);
  };

  const handlePageChange = (page: number) => {
    updateSearchParams({
      ...currentSearchParams,
      page,
    });
  };

  const handleApplyRelatedSearch = (value: string) => {
    setSearchTitle(value);
    updateSearchParams({
      ...currentSearchParams,
      title: value,
      page: 0,
    });
  };

  const handleApplyFacet = (
    field: 'company' | 'location' | 'employmentType',
    value: string,
  ) => {
    const nextParams: JobSearchParams = {
      ...currentSearchParams,
      page: 0,
    };

    if (field === 'company') {
      setSearchCompany(value);
      nextParams.company = value;
    } else if (field === 'location') {
      setSearchLocation(value);
      nextParams.location = value;
    } else {
      setEmploymentType(value);
      nextParams.employmentType = value;
    }

    updateSearchParams(nextParams);
  };

  const handleJobClick = (job: Job) => {
    const activeSearch = activeSearchRef.current;
    if (!activeSearch || activeSearch.clicked) {
      return;
    }

    activeSearch.clicked = true;
    void trackSearchClick(user?.id, activeSearch.sessionId, job.id);
  };

  const renderFacetButtons = (
    title: string,
    values: FacetValueCount[],
    onApply: (value: string) => void,
    formatValue?: (value: string) => string,
  ) => {
    if (values.length === 0) {
      return null;
    }

    return (
      <section className="jobs-discovery-card solid-card">
        <div className="jobs-discovery-card-header">
          <p className="jobs-section-kicker">Suggested filters</p>
          <h3>{title}</h3>
        </div>
        <div className="filter-chip-list">
          {values.map((value) => (
            <button
              key={`${title}-${value.value}`}
              type="button"
              className="filter-chip"
              onClick={() => onApply(value.value)}
            >
              <span>{formatValue ? formatValue(value.value) : value.value}</span>
              <strong>{value.count}</strong>
            </button>
          ))}
        </div>
      </section>
    );
  };

  return (
    <div className="jobs-page">
      <header className="jobs-header">
        <div className="jobs-header-content">
          <div className="jobs-header-copy">
            <p className="jobs-header-kicker">Search workspace</p>
            <h1>Find roles with more direction</h1>
            <p className="jobs-header-text">
              Search with guided filters, related searches, and a clearer results
              workspace that keeps the shortlist moving in the right direction.
            </p>
          </div>

          <div className="jobs-header-stats">
            <div className="jobs-header-stat">
              <strong>{loading ? '...' : pagination.totalElements}</strong>
              <span>active roles</span>
            </div>
            <div className="jobs-header-stat">
              <strong>{discovery.relatedSearches.length}</strong>
              <span>related paths</span>
            </div>
            <div className="jobs-header-stat">
              <strong>{appliedFilterChips.length}</strong>
              <span>active filters</span>
            </div>
          </div>
        </div>
      </header>

      <div className="jobs-container">
        {showMobileFilters && (
          <button
            type="button"
            className="jobs-filter-backdrop"
            onClick={() => setShowMobileFilters(false)}
            aria-label="Close filters panel"
          />
        )}

        <aside className={`jobs-filters ${showMobileFilters ? 'is-open' : ''}`}>
          <div className="jobs-filters-mobile-header">
            <div>
              <p className="jobs-section-kicker">Search controls</p>
              <h2>Refine your search</h2>
            </div>
            <button
              type="button"
              className="jobs-filters-close"
              onClick={() => setShowMobileFilters(false)}
            >
              Close
            </button>
          </div>

          <section className="jobs-panel-card solid-card">
            <div className="jobs-panel-heading">
              <p className="jobs-section-kicker">Query</p>
              <h2>Search inputs</h2>
              <p>Start broad, then tighten the shortlist with structured filters.</p>
            </div>

            <form onSubmit={handleSearch}>
              <div className="filter-section">
                <div className="filter-group">
                  <label htmlFor="search-title">Job Title</label>
                  <input
                    id="search-title"
                    type="text"
                    placeholder="e.g., Software Engineer"
                    value={searchTitle}
                    onChange={(event) => setSearchTitle(event.target.value)}
                    className="input"
                    autoComplete="off"
                  />
                </div>

                <div className="filter-group">
                  <label htmlFor="search-company">Company</label>
                  <input
                    id="search-company"
                    type="text"
                    placeholder="e.g., Northwind"
                    value={searchCompany}
                    onChange={(event) => setSearchCompany(event.target.value)}
                    className="input"
                  />
                </div>

                <div className="filter-group">
                  <label htmlFor="search-location">Location</label>
                  <input
                    id="search-location"
                    type="text"
                    placeholder="e.g., Dublin"
                    value={searchLocation}
                    onChange={(event) => setSearchLocation(event.target.value)}
                    className="input"
                  />
                </div>
              </div>

              <div className="filter-section">
                <div className="jobs-inline-heading">
                  <h3>Primary filters</h3>
                  <span>{hasDraftFilters ? 'Ready to apply' : 'Optional'}</span>
                </div>

                <div className="filter-group">
                  <label htmlFor="employment-type">Employment Type</label>
                  <select
                    id="employment-type"
                    value={employmentType}
                    onChange={(event) => setEmploymentType(event.target.value)}
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
                {hasDraftFilters && (
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={handleClearFilters}
                  >
                    Clear Filters
                  </button>
                )}
              </div>
            </form>
          </section>
        </aside>

        <main className="jobs-main">
          <section className="jobs-results-intro solid-card">
            <div className="jobs-results-summary">
              <div className="jobs-results-copy">
                <p className="jobs-section-kicker">Results overview</p>
                <h2>{buildSearchHeading(currentSearchParams)}</h2>
                <p>
                  {loading
                    ? 'Refreshing the search workspace...'
                    : buildSearchDescription(currentSearchParams, pagination.totalElements)}
                </p>
              </div>

              <div className="jobs-results-actions">
                <button
                  type="button"
                  className="btn btn-ghost jobs-mobile-filter-toggle"
                  onClick={() => setShowMobileFilters(true)}
                >
                  Filters
                </button>
                {hasAppliedFilters && (
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={handleClearFilters}
                  >
                    Reset Search
                  </button>
                )}
              </div>
            </div>

            {hasAppliedFilters && (
              <div className="jobs-active-filters">
                {appliedFilterChips.map((chip) => (
                  <button
                    key={chip.key}
                    type="button"
                    className="jobs-active-filter"
                    onClick={() => handleClearAppliedFilter(chip.key)}
                  >
                    <span>{chip.label}: {chip.value}</span>
                    <strong>Remove</strong>
                  </button>
                ))}
              </div>
            )}
          </section>

          {(discovery.relatedSearches.length > 0 ||
            discovery.suggestedLocations.length > 0 ||
            discovery.suggestedCompanies.length > 0 ||
            discovery.suggestedEmploymentTypes.length > 0) && (
            <section className="jobs-discovery-grid">
              {discovery.relatedSearches.length > 0 && (
                <section className="jobs-discovery-card solid-card">
                  <div className="jobs-discovery-card-header">
                    <p className="jobs-section-kicker">Related searches</p>
                    <h3>Try a nearby search path</h3>
                  </div>
                  <div className="related-searches">
                    {discovery.relatedSearches.map((relatedSearch) => (
                      <button
                        key={relatedSearch}
                        type="button"
                        className="related-search-chip"
                        onClick={() => handleApplyRelatedSearch(relatedSearch)}
                      >
                        {relatedSearch}
                      </button>
                    ))}
                  </div>
                </section>
              )}

              {renderFacetButtons(
                'Locations people usually pair with this query',
                discovery.suggestedLocations,
                (value) => handleApplyFacet('location', value),
              )}
              {renderFacetButtons(
                'Companies that keep this search focused',
                discovery.suggestedCompanies,
                (value) => handleApplyFacet('company', value),
              )}
              {renderFacetButtons(
                'Employment types worth checking next',
                discovery.suggestedEmploymentTypes,
                (value) => handleApplyFacet('employmentType', value),
                formatEmploymentType,
              )}
            </section>
          )}

          {error && (
            <div className="jobs-error solid-card">
              <p>{error}</p>
              <button
                type="button"
                onClick={() => updateSearchParams({ ...currentSearchParams })}
                className="btn btn-ghost"
              >
                Try Again
              </button>
            </div>
          )}

          {loading ? (
            <div className="jobs-loading solid-card">
              <div className="spinner"></div>
              <p>Loading jobs...</p>
            </div>
          ) : jobs.length === 0 ? (
            <div className="jobs-empty solid-card">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="11" cy="11" r="8"/>
                <path d="m21 21-4.35-4.35"/>
              </svg>
              <h3>No jobs found</h3>
              <p>Try a related search or remove one filter at a time to widen the result set.</p>
              {hasAppliedFilters && (
                <button type="button" onClick={handleClearFilters} className="btn btn-primary">
                  Clear Filters
                </button>
              )}
            </div>
          ) : (
            <>
              <div className="jobs-grid">
                {jobs.map((job) => (
                  <JobCard
                    key={job.id}
                    job={job}
                    onClick={handleJobClick}
                    descriptionPreview={buildDescriptionPreview(job.description)}
                    matchInsights={buildJobInsights(job, currentSearchParams)}
                  />
                ))}
              </div>

              {pagination.totalPages > 1 && (
                <div className="jobs-pagination">
                  <button
                    type="button"
                    className="btn btn-ghost"
                    disabled={pagination.currentPage === 0}
                    onClick={() => handlePageChange(pagination.currentPage - 1)}
                  >
                    Previous
                  </button>
                  <span className="pagination-info">
                    Page {pagination.currentPage + 1} of {pagination.totalPages}
                  </span>
                  <button
                    type="button"
                    className="btn btn-ghost"
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
