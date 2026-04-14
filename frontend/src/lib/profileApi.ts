import { isAxiosError } from 'axios';
import { api } from './api';

export type ProfileVisibility = 'PRIVATE' | 'PUBLIC';

export interface EducationEntry {
  id?: number;
  institution: string;
  degree: string | null;
  fieldOfStudy: string | null;
  startDate: string | null;
  endDate: string | null;
}

export interface ExperienceEntry {
  id?: number;
  company: string;
  title: string;
  description: string | null;
  startDate: string | null;
  endDate: string | null;
}

export interface PortfolioLink {
  id?: number;
  label: string;
  url: string;
}

export interface StudentProfile {
  id: number | null;
  userId: string;
  headline: string | null;
  bio: string | null;
  location: string | null;
  phone: string | null;
  resumeReference: string | null;
  visibility: ProfileVisibility;
  jobSearchStatus: string | null;
  skills: string[];
  education: EducationEntry[];
  experience: ExperienceEntry[];
  portfolioLinks: PortfolioLink[];
  createdAt: string | null;
  updatedAt: string | null;
}

export interface StudentProfileUpdateRequest {
  headline?: string | null;
  bio?: string | null;
  location?: string | null;
  phone?: string | null;
  visibility?: ProfileVisibility;
  jobSearchStatus?: string | null;
  skills?: string[];
  education?: Omit<EducationEntry, 'id'>[];
  experience?: Omit<ExperienceEntry, 'id'>[];
  portfolioLinks?: Omit<PortfolioLink, 'id'>[];
}

export interface ProfileCompleteness {
  completedFields: number;
  totalFields: number;
  percentage: number;
  complete: boolean;
  missingFields: string[];
}

interface ErrorResponse {
  message?: string;
  errors?: Record<string, string>;
}

const PROFILE_API_PATH = '/api/profiles';

export async function getCurrentProfile(): Promise<StudentProfile> {
  const response = await api.get(`${PROFILE_API_PATH}/me`);
  return response.data;
}

export async function updateCurrentProfile(request: StudentProfileUpdateRequest): Promise<StudentProfile> {
  const response = await api.put(`${PROFILE_API_PATH}/me`, request);
  return response.data;
}

export async function getCurrentProfileCompleteness(): Promise<ProfileCompleteness> {
  const response = await api.get(`${PROFILE_API_PATH}/me/completeness`);
  return response.data;
}

export function getProfileErrorMessage(error: unknown, fallback: string): string {
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
