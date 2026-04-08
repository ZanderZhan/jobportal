import {
  startTransition,
  useDeferredValue,
  useEffect,
  useRef,
  useState,
} from 'react';
import { useSearchParams } from 'react-router-dom';
import JobCard from '../../components/JobCard';
import { useAuth } from '../../hooks/useAuth';
import {
  deleteSavedSearch,
  formatEmploymentType,
  getJobAutocomplete,
  getSavedSearches,
  getSearchDiscovery,
  saveSearch,
  searchJobs,
  trackSearchAbandon,
  trackSearchClick,
  type AutocompleteSuggestion,
  type FacetValueCount,
  type Job,
  type JobSearchParams,
  type SavedSearch,
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

function buildSearchUrlParams(params: JobSearchParams) {
  const searchParams = new URLSearchParams();
  if (params.title) searchParams.set('title', params.title);
  if (params.company) searchParams.set('company', params.company);
  if (params.location) searchParams.set('location', params.location);
  if (params.employmentType) searchParams.set('employmentType', params.employmentType);
  searchParams.set('page', String(params.page ?? 0));
  return searchParams;
}

function buildSavedSearchSummary(savedSearch: SavedSearch) {
  return [savedSearch.title, savedSearch.company, savedSearch.location]
    .filter(Boolean)
    .join(' · ');
}

function createSearchSessionId() {
  return globalThis.crypto?.randomUUID?.() ?? `search-${Date.now()}`;
}

export default function JobsPage() {
  const { user, isAuthenticated } = useAuth();
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
  const [autocomplete, setAutocomplete] = useState<AutocompleteSuggestion[]>([]);
  const [showAutocomplete, setShowAutocomplete] = useState(false);
  const [savedSearches, setSavedSearches] = useState<SavedSearch[]>([]);
  const [savingSearch, setSavingSearch] = useState(false);

  const [searchTitle, setSearchTitle] = useState(searchParams.get('title') || '');
  const [searchCompany, setSearchCompany] = useState(searchParams.get('company') || '');
  const [searchLocation, setSearchLocation] = useState(searchParams.get('location') || '');
  const [employmentType, setEmploymentType] = useState(searchParams.get('employmentType') || '');

  const deferredSearchTitle = useDeferredValue(searchTitle);
  const activeSearchRef = useRef<ActiveSearchSession | null>(null);

  const currentSearchParams: JobSearchParams = {
    title: searchParams.get('title') || undefined,
    company: searchParams.get('company') || undefined,
    location: searchParams.get('location') || undefined,
    employmentType: searchParams.get('employmentType') || undefined,
    page: parseInt(searchParams.get('page') || '0', 10),
  };

  const hasFilters = Boolean(searchTitle || searchCompany || searchLocation || employmentType);

  useEffect(() => {
    setSearchTitle(searchParams.get('title') || '');
    setSearchCompany(searchParams.get('company') || '');
    setSearchLocation(searchParams.get('location') || '');
    setEmploymentType(searchParams.get('employmentType') || '');
  }, [searchParams]);

  useEffect(() => {
    let ignore = false;

    const loadAutocomplete = async () => {
      const query = deferredSearchTitle.trim();
      if (query.length < 2) {
        setAutocomplete([]);
        return;
      }

      try {
        const response = await getJobAutocomplete(query);
        if (!ignore) {
          setAutocomplete(response.suggestions);
        }
      } catch {
        if (!ignore) {
          setAutocomplete([]);
        }
      }
    };

    void loadAutocomplete();

    return () => {
      ignore = true;
    };
  }, [deferredSearchTitle]);

  useEffect(() => {
    let ignore = false;
    const abortPreviousSession = activeSearchRef.current;
    if (abortPreviousSession && !abortPreviousSession.clicked) {
      void trackSearchAbandon(user?.id, abortPreviousSession.sessionId);
    }

    const sessionId = createSearchSessionId();
    activeSearchRef.current = { sessionId, clicked: false };

    const fetchJobsAndDiscovery = async () => {
      setLoading(true);
      setError(null);

      try {
        const [response, discoveryResponse] = await Promise.all([
          searchJobs(
            {
              ...currentSearchParams,
              status: 'ACTIVE',
              size: pagination.size,
            },
            {
              userId: user?.id,
              sessionId,
            },
          ),
          getSearchDiscovery({
            ...currentSearchParams,
            status: 'ACTIVE',
          }),
        ]);

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
        setDiscovery(discoveryResponse);
      } catch (err) {
        if (!ignore) {
          setError('Failed to load jobs. Please try again.');
          console.error('Error fetching jobs:', err);
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
  }, [currentSearchParams.title, currentSearchParams.company, currentSearchParams.location, currentSearchParams.employmentType, currentSearchParams.page, pagination.size, user?.id]);

  useEffect(() => {
    let ignore = false;

    const loadSavedSearches = async () => {
      if (!isAuthenticated || !user?.id) {
        setSavedSearches([]);
        return;
      }

      try {
        const response = await getSavedSearches(user.id);
        if (!ignore) {
          setSavedSearches(response);
        }
      } catch (err) {
        if (!ignore) {
          console.error('Error loading saved searches:', err);
        }
      }
    };

    void loadSavedSearches();

    return () => {
      ignore = true;
    };
  }, [isAuthenticated, user?.id]);

  useEffect(() => {
    return () => {
      const activeSearch = activeSearchRef.current;
      if (activeSearch && !activeSearch.clicked) {
        void trackSearchAbandon(user?.id, activeSearch.sessionId);
      }
    };
  }, [user?.id]);

  const updateSearchParams = (params: JobSearchParams) => {
    startTransition(() => {
      setSearchParams(buildSearchUrlParams(params));
    });
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setShowAutocomplete(false);
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
    setAutocomplete([]);
    updateSearchParams({ page: 0 });
  };

  const handlePageChange = (page: number) => {
    updateSearchParams({
      ...currentSearchParams,
      page,
    });
  };

  const handleAutocompleteSelect = (suggestion: AutocompleteSuggestion) => {
    if (suggestion.type === 'COMPANY') {
      setSearchCompany(suggestion.value);
    } else if (suggestion.type === 'LOCATION') {
      setSearchLocation(suggestion.value);
    } else {
      setSearchTitle(suggestion.value);
    }
    setShowAutocomplete(false);
  };

  const handleApplyRelatedSearch = (value: string) => {
    setSearchTitle(value);
    updateSearchParams({
      ...currentSearchParams,
      title: value,
      page: 0,
    });
  };

  const handleApplyFacet = (field: 'company' | 'location' | 'employmentType', value: string) => {
    if (field === 'company') {
      setSearchCompany(value);
    } else if (field === 'location') {
      setSearchLocation(value);
    } else {
      setEmploymentType(value);
    }

    updateSearchParams({
      ...currentSearchParams,
      [field]: value,
      page: 0,
    });
  };

  const handleSaveCurrentSearch = async () => {
    if (!user?.id || !hasFilters) {
      return;
    }

    setSavingSearch(true);
    try {
      const savedSearch = await saveSearch(user.id, {
        title: searchTitle || undefined,
        company: searchCompany || undefined,
        location: searchLocation || undefined,
        employmentType: employmentType || undefined,
      });
      setSavedSearches((prev) => [savedSearch, ...prev.filter((entry) => entry.id !== savedSearch.id)]);
    } catch (err) {
      console.error('Error saving search:', err);
    } finally {
      setSavingSearch(false);
    }
  };

  const handleApplySavedSearch = (savedSearch: SavedSearch) => {
    setSearchTitle(savedSearch.title || '');
    setSearchCompany(savedSearch.company || '');
    setSearchLocation(savedSearch.location || '');
    setEmploymentType(savedSearch.employmentType || '');
    updateSearchParams({
      title: savedSearch.title || undefined,
      company: savedSearch.company || undefined,
      location: savedSearch.location || undefined,
      employmentType: savedSearch.employmentType || undefined,
      page: 0,
    });
  };

  const handleDeleteSavedSearch = async (savedSearchId: number) => {
    if (!user?.id) {
      return;
    }

    try {
      await deleteSavedSearch(user.id, savedSearchId);
      setSavedSearches((prev) => prev.filter((savedSearch) => savedSearch.id !== savedSearchId));
    } catch (err) {
      console.error('Error deleting saved search:', err);
    }
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
      <div className="product-section">
        <h3>{title}</h3>
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
      </div>
    );
  };

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
              <div className="filter-group autocomplete-group">
                <label htmlFor="search-title">Job Title</label>
                <input
                  id="search-title"
                  type="text"
                  placeholder="e.g., Software Engineer"
                  value={searchTitle}
                  onChange={(e) => {
                    setSearchTitle(e.target.value);
                    setShowAutocomplete(true);
                  }}
                  onFocus={() => setShowAutocomplete(true)}
                  onBlur={() => {
                    setTimeout(() => setShowAutocomplete(false), 120);
                  }}
                  className="input"
                  autoComplete="off"
                />
                {showAutocomplete && autocomplete.length > 0 && (
                  <div className="autocomplete-panel">
                    {autocomplete.map((suggestion) => (
                      <button
                        key={`${suggestion.type}-${suggestion.value}`}
                        type="button"
                        className="autocomplete-option"
                        onMouseDown={(e) => e.preventDefault()}
                        onClick={() => handleAutocompleteSelect(suggestion)}
                      >
                        <span>{suggestion.value}</span>
                        <small>{suggestion.type.toLowerCase()}</small>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="filter-group">
                <label htmlFor="search-company">Company</label>
                <input
                  id="search-company"
                  type="text"
                  placeholder="e.g., Northwind"
                  value={searchCompany}
                  onChange={(e) => setSearchCompany(e.target.value)}
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
              {isAuthenticated && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={!hasFilters || savingSearch}
                  onClick={handleSaveCurrentSearch}
                >
                  {savingSearch ? 'Saving...' : 'Save This Search'}
                </button>
              )}
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

          {isAuthenticated && savedSearches.length > 0 && (
            <div className="product-section">
              <h3>Saved Searches</h3>
              <div className="saved-search-list">
                {savedSearches.map((savedSearch) => (
                  <div key={savedSearch.id} className="saved-search-card">
                    <button
                      type="button"
                      className="saved-search-apply"
                      onClick={() => handleApplySavedSearch(savedSearch)}
                    >
                      <span className="saved-search-name">{savedSearch.name}</span>
                      <small>{buildSavedSearchSummary(savedSearch) || 'Saved filters'}</small>
                    </button>
                    <button
                      type="button"
                      className="saved-search-delete"
                      onClick={() => handleDeleteSavedSearch(savedSearch.id)}
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {renderFacetButtons('Suggested Locations', discovery.suggestedLocations, (value) => handleApplyFacet('location', value))}
          {renderFacetButtons('Suggested Companies', discovery.suggestedCompanies, (value) => handleApplyFacet('company', value))}
          {renderFacetButtons(
            'Suggested Types',
            discovery.suggestedEmploymentTypes,
            (value) => handleApplyFacet('employmentType', value),
            formatEmploymentType,
          )}
        </aside>

        <main className="jobs-main">
          <div className="jobs-results-header">
            <p className="jobs-count">
              {loading ? 'Loading...' : `${pagination.totalElements} jobs found`}
            </p>
            {discovery.relatedSearches.length > 0 && (
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
            )}
          </div>

          {error && (
            <div className="jobs-error">
              <p>{error}</p>
              <button onClick={() => updateSearchParams({ ...currentSearchParams })} className="btn btn-secondary">
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
              <p>Try one of the suggested filters or related searches to widen the results.</p>
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
                  <JobCard key={job.id} job={job} onClick={handleJobClick} />
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
