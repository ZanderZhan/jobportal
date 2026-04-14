import { isAxiosError } from 'axios';
import { api } from './api';

export type ApplicationStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'INTERVIEW'
  | 'HIRED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface ApplicationTimelineEntry {
  id: number;
  oldStatus: ApplicationStatus | null;
  newStatus: ApplicationStatus;
  changedBy: string;
  reason: string;
  createdAt: string;
}

export interface ApplicationRecord {
  id: number;
  studentId: string;
  jobId: number;
  employerId: string | null;
  jobTitle: string;
  resumeReference: string;
  status: ApplicationStatus;
  submittedAt: string;
  updatedAt: string;
  timeline: ApplicationTimelineEntry[];
}

export interface CreateApplicationRequest {
  jobId: number;
  resumeReference: string;
}

export interface UpdateApplicationStatusRequest {
  status: ApplicationStatus;
  reason?: string;
}

interface ErrorResponse {
  message?: string;
  errors?: Record<string, string>;
}

const APPLICATION_API_PATH = '/api/applications';

export async function createApplication(request: CreateApplicationRequest): Promise<ApplicationRecord> {
  const response = await api.post(APPLICATION_API_PATH, request);
  return response.data;
}

export async function getStudentApplications(): Promise<ApplicationRecord[]> {
  const response = await api.get(APPLICATION_API_PATH);
  return response.data;
}

export async function getStudentApplicationById(id: number): Promise<ApplicationRecord> {
  const response = await api.get(`${APPLICATION_API_PATH}/${id}`);
  return response.data;
}

export async function withdrawApplication(id: number): Promise<ApplicationRecord> {
  const response = await api.put(`${APPLICATION_API_PATH}/${id}/withdraw`);
  return response.data;
}

export async function getEmployerApplicationsForJob(jobId: number): Promise<ApplicationRecord[]> {
  const response = await api.get(`${APPLICATION_API_PATH}/jobs/${jobId}`);
  return response.data;
}

export async function updateApplicationStatus(
  id: number,
  request: UpdateApplicationStatusRequest,
): Promise<ApplicationRecord> {
  const response = await api.put(`${APPLICATION_API_PATH}/${id}/status`, request);
  return response.data;
}

export function formatApplicationStatus(status: ApplicationStatus): string {
  return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase());
}

export function getNextEmployerStatuses(status: ApplicationStatus): ApplicationStatus[] {
  switch (status) {
    case 'SUBMITTED':
      return ['UNDER_REVIEW', 'REJECTED'];
    case 'UNDER_REVIEW':
      return ['INTERVIEW', 'HIRED', 'REJECTED'];
    case 'INTERVIEW':
      return ['HIRED', 'REJECTED'];
    default:
      return [];
  }
}

export function getApplicationErrorMessage(error: unknown, fallback: string): string {
  if (!isAxiosError(error)) {
    return fallback;
  }

  const data = error.response?.data as ErrorResponse | undefined;
  if (data?.message) {
    return data.message;
  }

  const firstValidationError = data?.errors && Object.values(data.errors)[0];
  return firstValidationError || fallback;
}
