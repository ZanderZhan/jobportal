import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import {
  bootstrapNotifications,
  getNotificationPreferences,
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  updateNotificationPreference,
  type NotificationBootstrapResponse,
  type NotificationItem,
  type NotificationPreference,
} from '../lib/notificationApi';
import '../styles/dashboard.css';
import '../styles/notifications.css';
import '../styles/utilities.css';

const eventLabels: Record<string, string> = {
  APPLICATION_SUBMITTED: 'Application submitted',
  APPLICATION_STATUS_CHANGED: 'Application status changed',
  APPLICATION_WITHDRAWN: 'Application withdrawn',
  JOB_POSTED: 'Job posted',
  EMPLOYER_VERIFIED: 'Employer verified',
};

export function NotificationsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [bootstrap, setBootstrap] = useState<NotificationBootstrapResponse | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [preferences, setPreferences] = useState<NotificationPreference[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingPreference, setSavingPreference] = useState<string | null>(null);
  const [filterUnreadOnly, setFilterUnreadOnly] = useState(false);
  const [filterActionOnly, setFilterActionOnly] = useState(false);

  const loadPage = useCallback(async () => {
    setLoading(true);
    try {
      const [bootstrapData, notificationPage, preferenceData] = await Promise.all([
        bootstrapNotifications(),
        getNotifications({
          unreadOnly: filterUnreadOnly,
          actionRequiredOnly: filterActionOnly,
        }),
        getNotificationPreferences(),
      ]);
      setBootstrap(bootstrapData);
      setNotifications(notificationPage.items);
      setPreferences(preferenceData);
    } finally {
      setLoading(false);
    }
  }, [filterActionOnly, filterUnreadOnly]);

  useEffect(() => {
    loadPage().catch(() => {
      setLoading(false);
    });
  }, [loadPage]);

  const handleMarkRead = async (notificationId: number) => {
    await markNotificationRead(notificationId);
    await loadPage();
  };

  const handleMarkAllRead = async () => {
    await markAllNotificationsRead();
    await loadPage();
  };

  const handlePreferenceChange = async (
    eventType: NotificationPreference['eventType'],
    field: 'inAppEnabled' | 'emailEnabled',
    value: boolean
  ) => {
    const current = preferences.find((item) => item.eventType === eventType);
    if (!current) {
      return;
    }

    setSavingPreference(eventType);
    try {
      await updateNotificationPreference(eventType, {
        inAppEnabled: field === 'inAppEnabled' ? value : current.inAppEnabled,
        emailEnabled: field === 'emailEnabled' ? value : current.emailEnabled,
      });
      await loadPage();
    } finally {
      setSavingPreference(null);
    }
  };

  return (
    <div className="app-bg">
      <div className="app-bg__mesh mesh--dim" />
      <div className="app-bg__grid mesh--dimmer" />
      <div className="app-bg__grain" />

      <main className="dashboard-main">
        <div className="dashboard-content">
          <section className="notifications-hero solid-card animate-fade-up">
            <div>
              <p className="notifications-eyebrow">Notification center</p>
              <h1 className="dashboard-welcome-heading">Your updates in one place</h1>
              <p className="dashboard-welcome-sub">
                We keep the in-app message first. Email is sent when the address is ready.
              </p>
            </div>
            <div className="notifications-hero-actions">
              <button className="btn btn-ghost btn--sm" onClick={() => navigate('/dashboard')}>
                Back to dashboard
              </button>
              <button className="btn btn-primary btn--sm" onClick={handleMarkAllRead}>
                Mark all as read
              </button>
            </div>
          </section>

          <section className="dashboard-stats-row animate-fade-up-1">
            <div className="solid-card dashboard-stat-card">
              <div className="dashboard-stat-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" strokeWidth="1.8" strokeLinecap="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
              </div>
              <div>
                <p className="dashboard-stat-value">{loading ? '--' : bootstrap?.summary.unreadCount ?? 0}</p>
                <p className="dashboard-stat-label">Unread messages</p>
              </div>
            </div>

            <div className="solid-card dashboard-stat-card">
              <div className="dashboard-stat-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" strokeWidth="1.8" strokeLinecap="round">
                  <path d="M12 2 4 5v6c0 5 3.4 9.7 8 11 4.6-1.3 8-6 8-11V5l-8-3Z" />
                </svg>
              </div>
              <div>
                <p className="dashboard-stat-value">{loading ? '--' : bootstrap?.summary.actionRequiredCount ?? 0}</p>
                <p className="dashboard-stat-label">Action needed</p>
              </div>
            </div>

            <div className="solid-card dashboard-stat-card">
              <div className="dashboard-stat-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" strokeWidth="1.8" strokeLinecap="round">
                  <path d="M4 4h16v16H4z" />
                  <path d="m22 6-10 7L2 6" />
                </svg>
              </div>
              <div>
                <p className="dashboard-stat-value">{loading ? '--' : bootstrap?.summary.pendingCount ?? 0}</p>
                <p className="dashboard-stat-label">Pending delivery</p>
              </div>
            </div>
          </section>

          <section className="notifications-grid animate-fade-up-2">
            <div className="solid-card notifications-panel">
              <div className="notifications-panel-header">
                <div>
                  <h2 className="dashboard-section-title">Delivery readiness</h2>
                  <p className="dashboard-stat-label">This shows if email can be sent right now.</p>
                </div>
              </div>
              <div className="notifications-readiness">
                <div>
                  <span className="notifications-meta-label">Signed in as</span>
                  <p className="notifications-meta-value">{user?.email}</p>
                </div>
                <div>
                  <span className="notifications-meta-label">Cached email</span>
                  <p className="notifications-meta-value">
                    {bootstrap?.recipientEmail ?? 'Not ready yet'}
                  </p>
                </div>
                <div>
                  <span className="notifications-meta-label">Recipient cache</span>
                  <p className="notifications-meta-value">
                    {bootstrap?.emailReady ? 'Ready for email' : 'Waiting for first sync'}
                  </p>
                </div>
              </div>
            </div>

            <div className="solid-card notifications-panel">
              <div className="notifications-panel-header">
                <div>
                  <h2 className="dashboard-section-title">Notification settings</h2>
                  <p className="dashboard-stat-label">Choose how each event should reach you.</p>
                </div>
              </div>
              <div className="notifications-preference-list">
                {preferences.map((preference) => (
                  <div className="notifications-preference-row" key={preference.eventType}>
                    <div>
                      <p className="notifications-preference-title">{eventLabels[preference.eventType] ?? preference.eventType}</p>
                      <p className="dashboard-stat-label">{savingPreference === preference.eventType ? 'Saving...' : 'In-app and email can be changed here.'}</p>
                    </div>
                    <div className="notifications-preference-toggles">
                      <label>
                        <input
                          type="checkbox"
                          checked={preference.inAppEnabled}
                          disabled={savingPreference === preference.eventType}
                          onChange={(event) => handlePreferenceChange(preference.eventType, 'inAppEnabled', event.target.checked)}
                        />
                        In app
                      </label>
                      <label>
                        <input
                          type="checkbox"
                          checked={preference.emailEnabled}
                          disabled={savingPreference === preference.eventType}
                          onChange={(event) => handlePreferenceChange(preference.eventType, 'emailEnabled', event.target.checked)}
                        />
                        Email
                      </label>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section className="solid-card notifications-panel animate-fade-up-3">
            <div className="notifications-panel-header">
              <div>
                <h2 className="dashboard-section-title">Recent notifications</h2>
                <p className="dashboard-stat-label">Use the filters to check unread or important items first.</p>
              </div>
              <div className="notifications-filter-row">
                <button
                  className={`btn btn-ghost btn--sm ${filterUnreadOnly ? 'notifications-filter-active' : ''}`}
                  onClick={() => setFilterUnreadOnly((value) => !value)}
                >
                  {filterUnreadOnly ? 'Unread only: on' : 'Unread only'}
                </button>
                <button
                  className={`btn btn-ghost btn--sm ${filterActionOnly ? 'notifications-filter-active' : ''}`}
                  onClick={() => setFilterActionOnly((value) => !value)}
                >
                  {filterActionOnly ? 'Action only: on' : 'Action only'}
                </button>
                <button className="btn btn-ghost btn--sm" onClick={() => loadPage()}>
                  Refresh
                </button>
              </div>
            </div>

            {loading ? (
              <p className="notifications-empty">Loading notifications...</p>
            ) : notifications.length === 0 ? (
              <p className="notifications-empty">No notifications match the current filters.</p>
            ) : (
              <div className="notifications-list">
                {notifications.map((notification) => (
                  <article className="notifications-item" key={notification.id}>
                    <div className="notifications-item-main">
                      <div className="notifications-item-top">
                        <span className="role-badge">{eventLabels[notification.eventType] ?? notification.eventType}</span>
                        <span className={`role-badge ${notification.actionRequired ? 'role-badge--accent' : ''}`}>
                          {notification.actionRequired ? 'Action needed' : 'Info'}
                        </span>
                        <span className="role-badge">{notification.status.replaceAll('_', ' ')}</span>
                      </div>
                      <h3 className="notifications-item-title">{notification.title}</h3>
                      <p className="notifications-item-body">{notification.body}</p>
                      <div className="notifications-item-meta">
                        <span>{formatDate(notification.createdAt)}</span>
                        {notification.lastDeliveryError && <span>{notification.lastDeliveryError}</span>}
                      </div>
                    </div>
                    <div className="notifications-item-side">
                      <span className={`notifications-read-chip ${notification.read ? 'is-read' : 'is-unread'}`}>
                        {notification.read ? 'Read' : 'Unread'}
                      </span>
                      {!notification.read && (
                        <button className="btn btn-ghost btn--sm" onClick={() => handleMarkRead(notification.id)}>
                          Mark read
                        </button>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-IE', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}
