import { api } from './api';

export type NotificationEventType =
  | 'APPLICATION_SUBMITTED'
  | 'APPLICATION_STATUS_CHANGED'
  | 'APPLICATION_WITHDRAWN'
  | 'JOB_POSTED'
  | 'EMPLOYER_VERIFIED';

export type NotificationStatus =
  | 'CREATED'
  | 'PENDING_RECIPIENT'
  | 'RETRY_SCHEDULED'
  | 'PARTIALLY_DELIVERED'
  | 'DELIVERED'
  | 'FAILED'
  | 'SUPPRESSED';

export interface NotificationItem {
  id: number;
  eventKey: string;
  eventType: NotificationEventType;
  recipientUserId: string;
  recipientEmail: string | null;
  recipientName: string | null;
  title: string;
  body: string;
  actionRequired: boolean;
  status: NotificationStatus;
  read: boolean;
  createdAt: string;
  readAt: string | null;
  lastDeliveryError: string | null;
  nextRetryAt: string | null;
}

export interface NotificationSummary {
  totalCount: number;
  unreadCount: number;
  actionRequiredCount: number;
  failedCount: number;
  pendingRecipientCount: number;
  retryScheduledCount: number;
  latestNotificationAt: string | null;
}

export interface NotificationBootstrapResponse {
  recipientUserId: string;
  recipientEmail: string | null;
  recipientName: string | null;
  emailReady: boolean;
  summary: NotificationSummary;
}

export interface NotificationPageResponse {
  items: NotificationItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface NotificationPreference {
  eventType: NotificationEventType;
  inAppEnabled: boolean;
  emailEnabled: boolean;
}

export async function bootstrapNotifications() {
  const { data } = await api.get<NotificationBootstrapResponse>('/api/notifications/bootstrap');
  return data;
}

export async function getNotificationSummary() {
  const { data } = await api.get<NotificationSummary>('/api/notifications/summary');
  return data;
}

export async function getNotifications(params?: {
  unreadOnly?: boolean;
  actionRequiredOnly?: boolean;
  page?: number;
  size?: number;
}) {
  const { data } = await api.get<NotificationPageResponse>('/api/notifications/me', {
    params: {
      unreadOnly: params?.unreadOnly ?? false,
      actionRequiredOnly: params?.actionRequiredOnly ?? false,
      page: params?.page ?? 0,
      size: params?.size ?? 20,
    },
  });
  return data;
}

export async function markNotificationRead(notificationId: number) {
  const { data } = await api.patch<NotificationItem>(`/api/notifications/${notificationId}/read`);
  return data;
}

export async function markAllNotificationsRead() {
  await api.patch('/api/notifications/read-all');
}

export async function getNotificationPreferences() {
  const { data } = await api.get<NotificationPreference[]>('/api/notification-preferences/me');
  return data;
}

export async function updateNotificationPreference(
  eventType: NotificationEventType,
  payload: { inAppEnabled: boolean; emailEnabled: boolean }
) {
  const { data } = await api.put<NotificationPreference>(`/api/notification-preferences/me/${eventType}`, payload);
  return data;
}
