import {
  startTransition,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import JobCard from '../../components/JobCard';
import { useAuth } from '../../hooks/useAuth';
import {
  formatEmploymentType,
  searchJobs,
  type Job,
  type JobSearchParams,
} from '../../lib/jobApi';
import './JobsPage.css';

const DEFAULT_SORT = 'createdAt,desc';
const CLIENT_SEARCH_PAGE_SIZE = 100;
const CLIENT_SEARCH_MAX_PAGES = 50;

const EMPLOYMENT_TYPES = [
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
];

const SALARY_CURRENCIES = [
  { value: '', label: 'Any Currency' },
  { value: 'EUR', label: 'EUR' },
  { value: 'GBP', label: 'GBP' },
  { value: 'USD', label: 'USD' },
];

const WORK_MODES = [
  { value: '', label: 'Any Mode' },
  { value: 'REMOTE', label: 'Remote' },
  { value: 'HYBRID', label: 'Hybrid' },
  { value: 'ONSITE', label: 'On-site' },
];

const POSTED_WINDOWS = [
  { value: '', label: 'Any Time' },
  { value: '1', label: 'Last 24 hours' },
  { value: '7', label: 'Last 7 days' },
  { value: '30', label: 'Last 30 days' },
  { value: '90', label: 'Last 90 days' },
];

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'createdAt,asc', label: 'Oldest first' },
  { value: 'salaryMax,desc', label: 'Highest salary' },
  { value: 'salaryMin,asc', label: 'Lowest salary' },
  { value: 'company,asc', label: 'Company A-Z' },
  { value: 'title,asc', label: 'Title A-Z' },
];

const DISCOVERY_WORD_STOPLIST = new Set([
  'the',
  'and',
  'for',
  'with',
  'from',
  'role',
  'jobs',
  'job',
  'senior',
  'junior',
  'lead',
  'manager',
]);

interface FacetValueCount {
  value: string;
  count: number;
}

interface SearchDiscoveryResponse {
  relatedSearches: string[];
  suggestedLocations: FacetValueCount[];
  suggestedCompanies: FacetValueCount[];
  suggestedEmploymentTypes: FacetValueCount[];
}

interface AppliedFilterChip {
  key:
    | 'title'
    | 'company'
    | 'location'
    | 'employmentTypes'
    | 'salaryRange'
    | 'salaryCurrency'
    | 'workMode'
    | 'postedWithinDays';
  label: string;
  value: string;
}

function parsePositiveNumber(value: string | null) {
  if (!value) {
    return undefined;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function parseEmploymentTypesParam(value: string | null) {
  if (!value) {
    return [];
  }

  return value
    .split(',')
    .map((entry) => entry.trim())
    .filter(Boolean);
}

function buildSearchUrlParams(params: JobSearchParams) {
  const next = new URLSearchParams();

  if (params.title) next.set('title', params.title);
  if (params.company) next.set('company', params.company);
  if (params.location) next.set('location', params.location);
  if (params.employmentTypes && params.employmentTypes.length > 0) {
    next.set('employmentTypes', params.employmentTypes.join(','));
  } else if (params.employmentType) {
    next.set('employmentType', params.employmentType);
  }
  if (params.salaryMin !== undefined) next.set('salaryMin', String(params.salaryMin));
  if (params.salaryMax !== undefined) next.set('salaryMax', String(params.salaryMax));
  if (params.salaryCurrency) next.set('salaryCurrency', params.salaryCurrency);
  if (params.workMode) next.set('workMode', params.workMode);
  if (params.postedWithinDays !== undefined) next.set('postedWithinDays', String(params.postedWithinDays));
  if (params.sort && params.sort !== DEFAULT_SORT) next.set('sort', params.sort);
  next.set('page', String(params.page ?? 0));

  return next;
}

function normalizeText(value: string | null | undefined) {
  return value?.trim().toLowerCase() ?? '';
}

function buildSalaryLabel(min?: number, max?: number) {
  if (min !== undefined && max !== undefined) {
    return `${min.toLocaleString()} - ${max.toLocaleString()}`;
  }
  if (min !== undefined) {
    return `From ${min.toLocaleString()}`;
  }
  if (max !== undefined) {
    return `Up to ${max.toLocaleString()}`;
  }
  return '';
}

function inferWorkMode(location: string | null | undefined) {
  const normalized = normalizeText(location);
  if (!normalized) {
    return undefined;
  }
  if (normalized.includes('remote')) {
    return 'REMOTE';
  }
  if (normalized.includes('hybrid')) {
    return 'HYBRID';
  }
  return 'ONSITE';
}

function isWithinDays(dateString: string, days: number) {
  const createdAt = new Date(dateString).getTime();
  if (Number.isNaN(createdAt)) {
    return false;
  }
  const ageMs = Date.now() - createdAt;
  return ageMs <= days * 24 * 60 * 60 * 1000;
}

function buildAppliedFilterChips(params: JobSearchParams): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (params.title) chips.push({ key: 'title', label: 'Title', value: params.title });
  if (params.company) chips.push({ key: 'company', label: 'Company', value: params.company });
  if (params.location) chips.push({ key: 'location', label: 'Location', value: params.location });

  if (params.employmentTypes && params.employmentTypes.length > 0) {
    chips.push({
      key: 'employmentTypes',
      label: 'Types',
      value: params.employmentTypes.map((entry) => formatEmploymentType(entry)).join(', '),
    });
  }

  if (params.salaryMin !== undefined || params.salaryMax !== undefined) {
    chips.push({
      key: 'salaryRange',
      label: 'Salary',
      value: buildSalaryLabel(params.salaryMin, params.salaryMax),
    });
  }

  if (params.salaryCurrency) {
    chips.push({
      key: 'salaryCurrency',
      label: 'Currency',
      value: params.salaryCurrency,
    });
  }

  if (params.workMode) {
    chips.push({
      key: 'workMode',
      label: 'Work Mode',
      value: WORK_MODES.find((entry) => entry.value === params.workMode)?.label ?? params.workMode,
    });
  }

  if (params.postedWithinDays !== undefined) {
    chips.push({
      key: 'postedWithinDays',
      label: 'Posted',
      value: POSTED_WINDOWS.find((entry) => Number(entry.value) === params.postedWithinDays)?.label ?? `${params.postedWithinDays} days`,
    });
  }

  return chips;
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

  if (normalizedCompany && normalizeText(job.company).includes(normalizedCompany)) {
    insights.push('Company match');
  }

  if (normalizedLocation && normalizeText(job.location).includes(normalizedLocation)) {
    insights.push('Location match');
  }

  if (params.employmentTypes && params.employmentTypes.length > 0 && job.employmentType && params.employmentTypes.includes(job.employmentType)) {
    insights.push(formatEmploymentType(job.employmentType));
  }

  return insights.slice(0, 3);
}

function buildDescriptionPreview(description: string) {
  const normalized = description.replace(/\s+/g, ' ').trim();
  if (normalized.length <= 160) {
    return normalized;
  }
  return `${normalized.slice(0, 157).trimEnd()}...`;
}

function filterJobsClientSide(jobs: Job[], params: JobSearchParams) {
  return jobs.filter((job) => {
    if (params.employmentTypes && params.employmentTypes.length > 0) {
      if (!job.employmentType || !params.employmentTypes.includes(job.employmentType)) {
        return false;
      }
    }

    if (params.salaryCurrency && job.salaryCurrency !== params.salaryCurrency) {
      return false;
    }

    if (params.workMode) {
      const inferred = inferWorkMode(job.location);
      if (inferred !== params.workMode) {
        return false;
      }
    }

    if (params.postedWithinDays !== undefined) {
      if (!isWithinDays(job.createdAt, params.postedWithinDays)) {
        return false;
      }
    }

    return true;
  });
}

function buildFacetCounts(values: Array<string | null | undefined>, limit = 6): FacetValueCount[] {
  const counts = new Map<string, number>();

  values
    .map((value) => value?.trim())
    .filter((value): value is string => Boolean(value))
    .forEach((value) => {
      counts.set(value, (counts.get(value) ?? 0) + 1);
    });

  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, limit)
    .map(([value, count]) => ({ value, count }));
}

function deriveRelatedSearches(jobs: Job[], currentTitle?: string) {
  const counts = new Map<string, number>();
  const normalizedCurrent = normalizeText(currentTitle);

  jobs.forEach((job) => {
    job.title
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, ' ')
      .split(/\s+/)
      .filter((word) => word.length >= 4 && !DISCOVERY_WORD_STOPLIST.has(word))
      .forEach((word) => {
        if (word !== normalizedCurrent) {
          counts.set(word, (counts.get(word) ?? 0) + 1);
        }
      });
  });

  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, 8)
    .map(([word]) => word.charAt(0).toUpperCase() + word.slice(1));
}

function deriveDiscovery(jobs: Job[], params: JobSearchParams): SearchDiscoveryResponse {
  const suggestedLocations = buildFacetCounts(jobs.map((job) => job.location));
  const suggestedCompanies = buildFacetCounts(jobs.map((job) => job.company));
  const suggestedEmploymentTypes = buildFacetCounts(
    jobs.map((job) => job.employmentType),
  );

  return {
    relatedSearches: deriveRelatedSearches(jobs, params.title),
    suggestedLocations,
    suggestedCompanies,
    suggestedEmploymentTypes,
  };
}

function buildServerSearchParams(params: JobSearchParams, page: number, size: number): JobSearchParams {
  const singleEmploymentType = params.employmentTypes && params.employmentTypes.length === 1
    ? params.employmentTypes[0]
    : params.employmentType;

  return {
    title: params.title,
    company: params.company,
    location: params.location,
    employmentType: singleEmploymentType,
    salaryMin: params.salaryMin,
    salaryMax: params.salaryMax,
    status: params.status,
    page,
    size,
    sort: params.sort,
  };
}

function needsClientDataset(params: JobSearchParams) {
  return Boolean(
    params.salaryCurrency
    || params.workMode
    || params.postedWithinDays !== undefined
    || (params.employmentTypes && params.employmentTypes.length > 1),
  );
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
  const [selectedEmploymentTypes, setSelectedEmploymentTypes] = useState<string[]>(
    parseEmploymentTypesParam(searchParams.get('employmentTypes') ?? searchParams.get('employmentType')),
  );
  const [salaryMinInput, setSalaryMinInput] = useState(searchParams.get('salaryMin') || '');
  const [salaryMaxInput, setSalaryMaxInput] = useState(searchParams.get('salaryMax') || '');
  const [salaryCurrency, setSalaryCurrency] = useState(searchParams.get('salaryCurrency') || '');
  const [workMode, setWorkMode] = useState(searchParams.get('workMode') || '');
  const [postedWithinDays, setPostedWithinDays] = useState(searchParams.get('postedWithinDays') || '');
  const [sortValue, setSortValue] = useState(searchParams.get('sort') || DEFAULT_SORT);

  const currentSearchParams: JobSearchParams = useMemo(() => ({
    title: searchParams.get('title') || undefined,
    company: searchParams.get('company') || undefined,
    location: searchParams.get('location') || undefined,
    employmentTypes: parseEmploymentTypesParam(searchParams.get('employmentTypes') ?? searchParams.get('employmentType')),
    salaryMin: parsePositiveNumber(searchParams.get('salaryMin')),
    salaryMax: parsePositiveNumber(searchParams.get('salaryMax')),
    salaryCurrency: searchParams.get('salaryCurrency') || undefined,
    workMode: (searchParams.get('workMode') as JobSearchParams['workMode']) || undefined,
    postedWithinDays: parsePositiveNumber(searchParams.get('postedWithinDays')),
    page: Number.parseInt(searchParams.get('page') || '0', 10),
    sort: searchParams.get('sort') || DEFAULT_SORT,
  }), [searchParams]);

  const hasDraftFilters = Boolean(
    searchTitle
    || searchCompany
    || searchLocation
    || selectedEmploymentTypes.length > 0
    || salaryMinInput
    || salaryMaxInput
    || salaryCurrency
    || workMode
    || postedWithinDays
    || sortValue !== DEFAULT_SORT,
  );

  const appliedFilterChips = buildAppliedFilterChips(currentSearchParams);
  const hasAppliedFilters = appliedFilterChips.length > 0;

  useEffect(() => {
    setSearchTitle(searchParams.get('title') || '');
    setSearchCompany(searchParams.get('company') || '');
    setSearchLocation(searchParams.get('location') || '');
    setSelectedEmploymentTypes(
      parseEmploymentTypesParam(searchParams.get('employmentTypes') ?? searchParams.get('employmentType')),
    );
    setSalaryMinInput(searchParams.get('salaryMin') || '');
    setSalaryMaxInput(searchParams.get('salaryMax') || '');
    setSalaryCurrency(searchParams.get('salaryCurrency') || '');
    setWorkMode(searchParams.get('workMode') || '');
    setPostedWithinDays(searchParams.get('postedWithinDays') || '');
    setSortValue(searchParams.get('sort') || DEFAULT_SORT);
  }, [searchParams]);

  useEffect(() => {
    let ignore = false;

    const fetchAllServerMatches = async (baseParams: JobSearchParams) => {
      const collected: Job[] = [];

      for (let page = 0; page < CLIENT_SEARCH_MAX_PAGES; page += 1) {
        const response = await searchJobs(
          buildServerSearchParams(baseParams, page, CLIENT_SEARCH_PAGE_SIZE),
        );
        collected.push(...response.content);
        if (response.last) {
          break;
        }
      }

      return collected;
    };

    const fetchJobs = async () => {
      setLoading(true);
      setError(null);

      try {
        const baseParams: JobSearchParams = {
          ...currentSearchParams,
          status: 'ACTIVE',
        };
        const requiresClientDataset = needsClientDataset(baseParams);

        if (requiresClientDataset) {
          const serverJobs = await fetchAllServerMatches(baseParams);
          if (ignore) {
            return;
          }

          const filtered = filterJobsClientSide(serverJobs, baseParams);
          const totalElements = filtered.length;
          const totalPages = Math.max(1, Math.ceil(totalElements / pagination.size));
          const safePage = Math.min(baseParams.page ?? 0, totalPages - 1);
          const start = safePage * pagination.size;
          const pagedJobs = filtered.slice(start, start + pagination.size);

          setJobs(pagedJobs);
          setPagination((previous) => ({
            ...previous,
            totalElements,
            totalPages,
            currentPage: safePage,
          }));
          setDiscovery(deriveDiscovery(filtered, baseParams));
          return;
        }

        const response = await searchJobs(
          buildServerSearchParams(baseParams, baseParams.page ?? 0, pagination.size),
        );
        if (ignore) {
          return;
        }

        const filteredPage = filterJobsClientSide(response.content, baseParams);

        setJobs(filteredPage);
        setPagination((previous) => ({
          ...previous,
          totalElements: response.totalElements,
          totalPages: response.totalPages,
          currentPage: response.number,
        }));
        setDiscovery(deriveDiscovery(response.content, baseParams));
      } catch {
        if (!ignore) {
          setError('Failed to load jobs. Please try again.');
          setJobs([]);
          setDiscovery({
            relatedSearches: [],
            suggestedLocations: [],
            suggestedCompanies: [],
            suggestedEmploymentTypes: [],
          });
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    void fetchJobs();

    return () => {
      ignore = true;
    };
  }, [
    currentSearchParams,
    pagination.size,
  ]);

  const updateSearchParams = (params: JobSearchParams) => {
    setShowMobileFilters(false);
    startTransition(() => {
      setSearchParams(buildSearchUrlParams(params));
    });
  };

  const applySearch = () => {
    updateSearchParams({
      title: searchTitle || undefined,
      company: searchCompany || undefined,
      location: searchLocation || undefined,
      employmentTypes: selectedEmploymentTypes,
      salaryMin: parsePositiveNumber(salaryMinInput),
      salaryMax: parsePositiveNumber(salaryMaxInput),
      salaryCurrency: salaryCurrency || undefined,
      workMode: (workMode || undefined) as JobSearchParams['workMode'],
      postedWithinDays: parsePositiveNumber(postedWithinDays),
      sort: sortValue,
      page: 0,
    });
  };

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    applySearch();
  };

  const handleClearFilters = () => {
    setSearchTitle('');
    setSearchCompany('');
    setSearchLocation('');
    setSelectedEmploymentTypes([]);
    setSalaryMinInput('');
    setSalaryMaxInput('');
    setSalaryCurrency('');
    setWorkMode('');
    setPostedWithinDays('');
    setSortValue(DEFAULT_SORT);
    updateSearchParams({ page: 0 });
  };

  const handleClearAppliedFilter = (field: AppliedFilterChip['key']) => {
    const nextParams: JobSearchParams = {
      ...currentSearchParams,
      page: 0,
    };

    switch (field) {
      case 'title':
        setSearchTitle('');
        nextParams.title = undefined;
        break;
      case 'company':
        setSearchCompany('');
        nextParams.company = undefined;
        break;
      case 'location':
        setSearchLocation('');
        nextParams.location = undefined;
        break;
      case 'employmentTypes':
        setSelectedEmploymentTypes([]);
        nextParams.employmentTypes = [];
        break;
      case 'salaryRange':
        setSalaryMinInput('');
        setSalaryMaxInput('');
        nextParams.salaryMin = undefined;
        nextParams.salaryMax = undefined;
        break;
      case 'salaryCurrency':
        setSalaryCurrency('');
        nextParams.salaryCurrency = undefined;
        break;
      case 'workMode':
        setWorkMode('');
        nextParams.workMode = undefined;
        break;
      case 'postedWithinDays':
        setPostedWithinDays('');
        nextParams.postedWithinDays = undefined;
        break;
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
      setSelectedEmploymentTypes([value]);
      nextParams.employmentTypes = [value];
    }

    updateSearchParams(nextParams);
  };

  const handleToggleEmploymentType = (value: string) => {
    setSelectedEmploymentTypes((previous) => (
      previous.includes(value)
        ? previous.filter((entry) => entry !== value)
        : [...previous, value]
    ));
  };

  const handleSortChange = (value: string) => {
    setSortValue(value);
    updateSearchParams({
      ...currentSearchParams,
      sort: value,
      page: 0,
    });
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
          {user?.email && (
            <div className="jobs-topbar-user">
              <span className="role-badge">{user.role?.replace('ROLE_', '').replace('_', ' ')}</span>
              <span className="jobs-topbar-email">{user.email}</span>
            </div>
          )}
          {hasAppliedFilters && (
            <button type="button" className="btn btn-ghost" onClick={handleClearFilters}>
              Browse All Jobs
            </button>
          )}
        </div>
      </header>

      <header className="jobs-header">
        <div className="jobs-header-content">
          <div className="jobs-header-top">
            <div className="jobs-header-copy">
              <Link to="/" className="jobs-header-kicker jobs-header-kicker-link">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                  <path d="m15 18-6-6 6-6" />
                </svg>
                <span>Homepage</span>
              </Link>
              <h1>Find roles with sharper filters</h1>
              <p className="jobs-header-text">
                Search the way modern job boards do it: core query first, advanced filters second,
                and practical controls that keep your place while you refine.
              </p>
            </div>
          </div>

          <div className="jobs-header-stats">
            <div className="jobs-header-stat">
              <strong>{loading ? '...' : pagination.totalElements}</strong>
              <span>matching roles</span>
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

      <section className="jobs-search-strip">
        <div className="jobs-search-strip-inner">
          <section className="jobs-search-card solid-card">
            <div className="jobs-search-card-header">
              <div>
                <p className="jobs-section-kicker">Search inputs</p>
                <h2>Search roles directly</h2>
                <p>Start with the primary fields, then open advanced filters only when you need them.</p>
              </div>

              <button
                type="button"
                className="btn btn-ghost jobs-search-filter-trigger"
                onClick={() => setShowMobileFilters(true)}
              >
                Advanced Filters
              </button>
            </div>

            <form className="jobs-search-form" onSubmit={handleSearch}>
              <div className="jobs-search-form-grid">
                <div className="filter-group jobs-search-field">
                  <label htmlFor="search-title">Job Title</label>
                  <input
                    id="search-title"
                    type="text"
                    placeholder="e.g., Platform Engineer"
                    value={searchTitle}
                    onChange={(event) => setSearchTitle(event.target.value)}
                    className="input"
                    autoComplete="off"
                  />
                </div>

                <div className="filter-group jobs-search-field">
                  <label htmlFor="search-company">Company</label>
                  <input
                    id="search-company"
                    type="text"
                    placeholder="e.g., Atlas Payments"
                    value={searchCompany}
                    onChange={(event) => setSearchCompany(event.target.value)}
                    className="input"
                  />
                </div>

                <div className="filter-group jobs-search-field">
                  <label htmlFor="search-location">Location</label>
                  <input
                    id="search-location"
                    type="text"
                    placeholder="e.g., Dublin or Remote"
                    value={searchLocation}
                    onChange={(event) => setSearchLocation(event.target.value)}
                    className="input"
                  />
                </div>
              </div>

              <div className="jobs-search-actions">
                <button type="submit" className="btn btn-primary">Search Jobs</button>
                {hasDraftFilters && (
                  <button type="button" className="btn btn-ghost" onClick={handleClearFilters}>
                    Reset
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn-ghost jobs-mobile-filter-toggle"
                  onClick={() => setShowMobileFilters(true)}
                >
                  Filters
                </button>
              </div>
            </form>
          </section>
        </div>
      </section>

      <div className="jobs-container jobs-layout">
        {showMobileFilters && (
          <button
            type="button"
            className="jobs-filter-backdrop"
            onClick={() => setShowMobileFilters(false)}
            aria-label="Close filters panel"
          />
        )}

        <aside
          className={`jobs-filters ${showMobileFilters ? 'is-open' : ''}`}
          aria-hidden={!showMobileFilters}
          role="dialog"
          aria-modal="true"
          aria-label="Advanced filters"
        >
          <section className="jobs-panel-card solid-card">
            <div className="jobs-filters-mobile-header jobs-filters-modal-header">
              <button
                type="button"
                className="jobs-filters-close"
                onClick={() => setShowMobileFilters(false)}
                aria-label="Close advanced filters"
              >
                ×
              </button>
            </div>

            <div className="jobs-panel-heading">
              <p className="jobs-section-kicker">Advanced filters</p>
              <h2>Narrow with intent</h2>
              <p>Compensation, work mode, posting recency, and role type all live here.</p>
            </div>

            <div>
              <div className="filter-section">
                <div className="jobs-inline-heading">
                  <h3>Compensation</h3>
                  <span>{salaryMinInput || salaryMaxInput || salaryCurrency ? 'Configured' : 'Optional'}</span>
                </div>

                <div className="jobs-filter-grid">
                  <div className="filter-group">
                    <label htmlFor="salary-min">Minimum Salary</label>
                    <input
                      id="salary-min"
                      type="number"
                      min="0"
                      placeholder="e.g., 60000"
                      value={salaryMinInput}
                      onChange={(event) => setSalaryMinInput(event.target.value)}
                      className="input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="salary-max">Maximum Salary</label>
                    <input
                      id="salary-max"
                      type="number"
                      min="0"
                      placeholder="e.g., 100000"
                      value={salaryMaxInput}
                      onChange={(event) => setSalaryMaxInput(event.target.value)}
                      className="input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="salary-currency">Salary Currency</label>
                    <select
                      id="salary-currency"
                      value={salaryCurrency}
                      onChange={(event) => setSalaryCurrency(event.target.value)}
                      className="input"
                    >
                      {SALARY_CURRENCIES.map((option) => (
                        <option key={option.value || 'any-currency'} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              <div className="filter-section">
                <div className="jobs-inline-heading">
                  <h3>Work setup</h3>
                  <span>{workMode || postedWithinDays ? 'Configured' : 'Optional'}</span>
                </div>

                <div className="jobs-filter-grid">
                  <div className="filter-group">
                    <label htmlFor="work-mode">Work Mode</label>
                    <select
                      id="work-mode"
                      value={workMode}
                      onChange={(event) => setWorkMode(event.target.value)}
                      className="input"
                    >
                      {WORK_MODES.map((option) => (
                        <option key={option.value || 'any-mode'} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="filter-group">
                    <label htmlFor="posted-within">Posted Within</label>
                    <select
                      id="posted-within"
                      value={postedWithinDays}
                      onChange={(event) => setPostedWithinDays(event.target.value)}
                      className="input"
                    >
                      {POSTED_WINDOWS.map((option) => (
                        <option key={option.value || 'any-time'} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              <div className="filter-section">
                <div className="filter-group">
                  <div className="jobs-inline-heading">
                    <h3>Employment Types</h3>
                    <span>{selectedEmploymentTypes.length > 0 ? 'Multi-select' : 'Optional'}</span>
                  </div>
                  <div className="jobs-checkbox-grid">
                    {EMPLOYMENT_TYPES.map((type) => {
                      const isChecked = selectedEmploymentTypes.includes(type.value);
                      return (
                        <label key={type.value} className={`jobs-checkbox-option ${isChecked ? 'is-selected' : ''}`}>
                          <input
                            type="checkbox"
                            checked={isChecked}
                            onChange={() => handleToggleEmploymentType(type.value)}
                          />
                          <span>{type.label}</span>
                        </label>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className="filter-actions">
                <button type="button" className="btn btn-primary" onClick={applySearch}>Apply Filters</button>
                {hasDraftFilters && (
                  <button type="button" className="btn btn-ghost" onClick={handleClearFilters}>
                    Clear Filters
                  </button>
                )}
              </div>
            </div>
          </section>
        </aside>

        <main className="jobs-main">
          <div className="jobs-results-toolbar">
            <div className="jobs-results-toolbar-summary">
              <strong>{loading ? 'Loading results...' : `${pagination.totalElements} roles found`}</strong>
              <span>Page {pagination.currentPage + 1} of {Math.max(1, pagination.totalPages || 1)}</span>
            </div>

            <div className="jobs-results-toolbar-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setShowMobileFilters(true)}>
                Advanced Filters
              </button>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => handlePageChange(0)}
                disabled={pagination.currentPage === 0}
              >
                First Page
              </button>
              <div className="jobs-toolbar-control">
                <label htmlFor="jobs-sort">Sort by</label>
                <select
                  id="jobs-sort"
                  className="input"
                  value={sortValue}
                  onChange={(event) => handleSortChange(event.target.value)}
                >
                  {SORT_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
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

          {(discovery.relatedSearches.length > 0
            || discovery.suggestedLocations.length > 0
            || discovery.suggestedCompanies.length > 0
            || discovery.suggestedEmploymentTypes.length > 0) && (
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
              <button type="button" onClick={applySearch} className="btn btn-ghost">
                Try Again
              </button>
            </div>
          )}

          {loading ? (
            <div className="jobs-loading solid-card">
              <div className="spinner" />
              <p>Loading jobs...</p>
            </div>
          ) : jobs.length === 0 ? (
            <div className="jobs-empty solid-card">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.35-4.35" />
              </svg>
              <h3>No jobs found</h3>
              <p>Try a different combination of compensation, work-mode, or role-type filters.</p>
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
                    onClick={() => handlePageChange(0)}
                  >
                    First
                  </button>
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
                  <button
                    type="button"
                    className="btn btn-ghost"
                    disabled={pagination.currentPage >= pagination.totalPages - 1}
                    onClick={() => handlePageChange(pagination.totalPages - 1)}
                  >
                    Last
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
