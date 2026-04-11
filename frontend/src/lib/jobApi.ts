import { api } from './api';

export interface Job {
  id: number;
  employerId: string | null;
  title: string;
  description: string;
  company: string;
  location: string | null;
  employmentType: 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP' | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  requirements: string[];
  status: 'DRAFT' | 'ACTIVE' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

export interface JobSearchParams {
  title?: string;
  company?: string;
  location?: string;
  employmentType?: string;
  employmentTypes?: string[];
  salaryMin?: number;
  salaryMax?: number;
  salaryCurrency?: string;
  workMode?: 'REMOTE' | 'HYBRID' | 'ONSITE';
  postedWithinDays?: number;
  status?: string;
  employerId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface SearchRequestContext {
  userId?: string;
  sessionId?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface AutocompleteSuggestion {
  value: string;
  type: 'TITLE' | 'COMPANY' | 'LOCATION';
  count: number;
}

export interface JobAutocompleteResponse {
  suggestions: AutocompleteSuggestion[];
}

export interface FacetValueCount {
  value: string;
  count: number;
}

export interface SearchDiscoveryResponse {
  relatedSearches: string[];
  suggestedLocations: FacetValueCount[];
  suggestedCompanies: FacetValueCount[];
  suggestedEmploymentTypes: FacetValueCount[];
}

export interface SavedSearchRequest {
  name?: string;
  title?: string;
  company?: string;
  location?: string;
  employmentType?: string;
}

export interface SavedSearch {
  id: number;
  name: string;
  title: string | null;
  company: string | null;
  location: string | null;
  employmentType: string | null;
  createdAt: string;
  updatedAt: string;
}

const JOB_API_PATH = '/api/jobs';
const SEARCH_API_PATH = '/api/search/jobs';

function buildHeaders(context?: SearchRequestContext) {
  return {
    ...(context?.userId ? { 'X-Search-User-Id': context.userId } : {}),
    ...(context?.sessionId ? { 'X-Search-Session-Id': context.sessionId } : {}),
  };
}

function buildSearchParams(params: JobSearchParams = {}) {
  const searchParams = new URLSearchParams();

  if (params.title) searchParams.set('title', params.title);
  if (params.company) searchParams.set('company', params.company);
  if (params.location) searchParams.set('location', params.location);
  if (params.employmentType) searchParams.set('employmentType', params.employmentType);
  if (params.employmentTypes && params.employmentTypes.length > 0) {
    searchParams.set('employmentTypes', params.employmentTypes.join(','));
  }
  if (params.salaryMin !== undefined) searchParams.set('salaryMin', params.salaryMin.toString());
  if (params.salaryMax !== undefined) searchParams.set('salaryMax', params.salaryMax.toString());
  if (params.salaryCurrency) searchParams.set('salaryCurrency', params.salaryCurrency);
  if (params.workMode) searchParams.set('workMode', params.workMode);
  if (params.postedWithinDays !== undefined) searchParams.set('postedWithinDays', params.postedWithinDays.toString());
  if (params.status) searchParams.set('status', params.status);
  if (params.employerId) searchParams.set('employerId', params.employerId);
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());
  if (params.sort) searchParams.set('sort', params.sort);

  return searchParams;
}

export async function getJobs(params: JobSearchParams = {}): Promise<PagedResponse<Job>> {
  const searchParams = new URLSearchParams();

  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());
  if (params.sort) searchParams.set('sort', params.sort);

  const response = await api.get(`${JOB_API_PATH}?${searchParams.toString()}`);
  return response.data;
}

export async function searchJobs(
  params: JobSearchParams = {},
  context?: SearchRequestContext,
): Promise<PagedResponse<Job>> {
  const response = await api.get(`${SEARCH_API_PATH}?${buildSearchParams(params).toString()}`, {
    headers: buildHeaders(context),
  });
  return response.data;
}

export async function getJobAutocomplete(query: string): Promise<JobAutocompleteResponse> {
  const response = await api.get('/api/search/jobs/autocomplete', {
    params: { query },
  });
  return response.data;
}

export async function getSearchDiscovery(params: JobSearchParams = {}): Promise<SearchDiscoveryResponse> {
  const response = await api.get(`/api/search/jobs/discovery?${buildSearchParams(params).toString()}`);
  return response.data;
}

export async function getSavedSearches(userId: string): Promise<SavedSearch[]> {
  const response = await api.get('/api/search/saved-searches', {
    headers: buildHeaders({ userId }),
  });
  return response.data;
}

export async function saveSearch(userId: string, request: SavedSearchRequest): Promise<SavedSearch> {
  const response = await api.post('/api/search/saved-searches', request, {
    headers: buildHeaders({ userId }),
  });
  return response.data;
}

export async function deleteSavedSearch(userId: string, id: number): Promise<void> {
  await api.delete(`/api/search/saved-searches/${id}`, {
    headers: buildHeaders({ userId }),
  });
}

export async function trackSearchClick(userId: string | undefined, sessionId: string, jobId: number): Promise<void> {
  await api.post('/api/search/analytics/click', {
    sessionId,
    jobId,
  }, {
    headers: buildHeaders({ userId }),
  });
}

export async function trackSearchAbandon(userId: string | undefined, sessionId: string): Promise<void> {
  await api.post('/api/search/analytics/abandon', {
    sessionId,
  }, {
    headers: buildHeaders({ userId }),
  });
}

export async function getJobById(id: number): Promise<Job> {
  const response = await api.get(`${JOB_API_PATH}/${id}`);
  return response.data;
}

export interface JobRequest {
  title: string;
  description: string;
  company: string;
  location?: string;
  employmentType?: 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP';
  salaryMin?: number;
  salaryMax?: number;
  salaryCurrency?: string;
  requirements?: string[];
  status?: 'DRAFT' | 'ACTIVE' | 'CLOSED';
}

export async function createJob(job: JobRequest): Promise<Job> {
  const response = await api.post(JOB_API_PATH, job);
  return response.data;
}

export async function updateJob(id: number, job: JobRequest): Promise<Job> {
  const response = await api.put(`${JOB_API_PATH}/${id}`, job);
  return response.data;
}

export async function deleteJob(id: number): Promise<void> {
  await api.delete(`${JOB_API_PATH}/${id}`);
}

export function formatSalary(min: number | null, max: number | null, currency: string | null): string {
  if (!min && !max) return 'Salary not specified';

  const curr = currency || 'USD';
  const formatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: curr,
    maximumFractionDigits: 0,
  });

  if (min && max) {
    return `${formatter.format(min)} - ${formatter.format(max)}`;
  } else if (min) {
    return `From ${formatter.format(min)}`;
  } else if (max) {
    return `Up to ${formatter.format(max)}`;
  }
  return 'Salary not specified';
}

export function formatEmploymentType(type: string | null): string {
  if (!type) return 'Not specified';
  return type.replace('_', ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

export function formatDate(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return 'Today';
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays} days ago`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`;

  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}
