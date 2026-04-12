import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isStudentRole } from '../../lib/authRoles';
import {
  getCurrentProfile,
  getCurrentProfileCompleteness,
  getProfileErrorMessage,
  updateCurrentProfile,
  type EducationEntry,
  type ExperienceEntry,
  type PortfolioLink,
  type ProfileCompleteness,
  type ProfileVisibility,
  type StudentProfile,
} from '../../lib/profileApi';
import '../../styles/dashboard.css';
import '../../styles/utilities.css';
import './ProfilePage.css';

type ProfileFormState = {
  headline: string;
  bio: string;
  location: string;
  phone: string;
  visibility: ProfileVisibility;
  jobSearchStatus: string;
  skillsText: string;
  education: EducationEntry[];
  experience: ExperienceEntry[];
  portfolioLinks: PortfolioLink[];
};

const EMPTY_EDUCATION: EducationEntry = {
  institution: '',
  degree: '',
  fieldOfStudy: '',
  startDate: '',
  endDate: '',
};

const EMPTY_EXPERIENCE: ExperienceEntry = {
  company: '',
  title: '',
  description: '',
  startDate: '',
  endDate: '',
};

const EMPTY_LINK: PortfolioLink = {
  label: '',
  url: '',
};

function toInputValue(value: string | null | undefined) {
  return value ?? '';
}

function toNullable(value: string) {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

function buildFormState(profile: StudentProfile): ProfileFormState {
  return {
    headline: toInputValue(profile.headline),
    bio: toInputValue(profile.bio),
    location: toInputValue(profile.location),
    phone: toInputValue(profile.phone),
    visibility: profile.visibility,
    jobSearchStatus: toInputValue(profile.jobSearchStatus),
    skillsText: profile.skills.join(', '),
    education: profile.education.length > 0
      ? profile.education.map((entry) => ({
          ...entry,
          degree: toInputValue(entry.degree),
          fieldOfStudy: toInputValue(entry.fieldOfStudy),
          startDate: toInputValue(entry.startDate),
          endDate: toInputValue(entry.endDate),
        }))
      : [{ ...EMPTY_EDUCATION }],
    experience: profile.experience.length > 0
      ? profile.experience.map((entry) => ({
          ...entry,
          description: toInputValue(entry.description),
          startDate: toInputValue(entry.startDate),
          endDate: toInputValue(entry.endDate),
        }))
      : [{ ...EMPTY_EXPERIENCE }],
    portfolioLinks: profile.portfolioLinks.length > 0
      ? profile.portfolioLinks.map((entry) => ({ ...entry }))
      : [{ ...EMPTY_LINK }],
  };
}

export function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const isStudent = isStudentRole(user?.role, user?.userType);

  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [completeness, setCompleteness] = useState<ProfileCompleteness | null>(null);
  const [form, setForm] = useState<ProfileFormState | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

  useEffect(() => {
    async function loadProfile() {
      if (!isStudent) {
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const [profileResponse, completenessResponse] = await Promise.all([
          getCurrentProfile(),
          getCurrentProfileCompleteness(),
        ]);
        setProfile(profileResponse);
        setCompleteness(completenessResponse);
        setForm(buildFormState(profileResponse));
      } catch (requestError) {
        setError(getProfileErrorMessage(requestError, 'Failed to load your profile.'));
      } finally {
        setIsLoading(false);
      }
    }

    void loadProfile();
  }, [isStudent]);

  const completionTone = useMemo(() => {
    if (!completeness) return 'default';
    if (completeness.percentage === 100) return 'success';
    if (completeness.percentage >= 60) return 'accent';
    return 'default';
  }, [completeness]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const updateForm = <K extends keyof ProfileFormState>(key: K, value: ProfileFormState[K]) => {
    setForm((current) => (current ? { ...current, [key]: value } : current));
  };

  const updateEducation = (index: number, key: keyof EducationEntry, value: string) => {
    setForm((current) => {
      if (!current) return current;
      const education = current.education.map((entry, entryIndex) =>
        entryIndex === index ? { ...entry, [key]: value } : entry,
      );
      return { ...current, education };
    });
  };

  const updateExperience = (index: number, key: keyof ExperienceEntry, value: string) => {
    setForm((current) => {
      if (!current) return current;
      const experience = current.experience.map((entry, entryIndex) =>
        entryIndex === index ? { ...entry, [key]: value } : entry,
      );
      return { ...current, experience };
    });
  };

  const updateLink = (index: number, key: keyof PortfolioLink, value: string) => {
    setForm((current) => {
      if (!current) return current;
      const portfolioLinks = current.portfolioLinks.map((entry, entryIndex) =>
        entryIndex === index ? { ...entry, [key]: value } : entry,
      );
      return { ...current, portfolioLinks };
    });
  };

  const addEducation = () => {
    setForm((current) => (current ? { ...current, education: [...current.education, { ...EMPTY_EDUCATION }] } : current));
  };

  const addExperience = () => {
    setForm((current) => (current ? { ...current, experience: [...current.experience, { ...EMPTY_EXPERIENCE }] } : current));
  };

  const addLink = () => {
    setForm((current) => (current ? { ...current, portfolioLinks: [...current.portfolioLinks, { ...EMPTY_LINK }] } : current));
  };

  const removeEducation = (index: number) => {
    setForm((current) => {
      if (!current) return current;
      const next = current.education.filter((_, entryIndex) => entryIndex !== index);
      return { ...current, education: next.length > 0 ? next : [{ ...EMPTY_EDUCATION }] };
    });
  };

  const removeExperience = (index: number) => {
    setForm((current) => {
      if (!current) return current;
      const next = current.experience.filter((_, entryIndex) => entryIndex !== index);
      return { ...current, experience: next.length > 0 ? next : [{ ...EMPTY_EXPERIENCE }] };
    });
  };

  const removeLink = (index: number) => {
    setForm((current) => {
      if (!current) return current;
      const next = current.portfolioLinks.filter((_, entryIndex) => entryIndex !== index);
      return { ...current, portfolioLinks: next.length > 0 ? next : [{ ...EMPTY_LINK }] };
    });
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form) {
      return;
    }

    setIsSaving(true);
    setError(null);
    setSaveMessage(null);

    try {
      const updatedProfile = await updateCurrentProfile({
        headline: toNullable(form.headline),
        bio: toNullable(form.bio),
        location: toNullable(form.location),
        phone: toNullable(form.phone),
        visibility: form.visibility,
        jobSearchStatus: toNullable(form.jobSearchStatus),
        skills: form.skillsText
          .split(',')
          .map((value) => value.trim())
          .filter(Boolean),
        education: form.education
          .filter((entry) => entry.institution.trim())
          .map((entry) => ({
            institution: entry.institution.trim(),
            degree: toNullable(toInputValue(entry.degree)),
            fieldOfStudy: toNullable(toInputValue(entry.fieldOfStudy)),
            startDate: toNullable(toInputValue(entry.startDate)),
            endDate: toNullable(toInputValue(entry.endDate)),
          })),
        experience: form.experience
          .filter((entry) => entry.company.trim() && entry.title.trim())
          .map((entry) => ({
            company: entry.company.trim(),
            title: entry.title.trim(),
            description: toNullable(toInputValue(entry.description)),
            startDate: toNullable(toInputValue(entry.startDate)),
            endDate: toNullable(toInputValue(entry.endDate)),
          })),
        portfolioLinks: form.portfolioLinks
          .filter((entry) => entry.label.trim() && entry.url.trim())
          .map((entry) => ({
            label: entry.label.trim(),
            url: entry.url.trim(),
          })),
      });

      const updatedCompleteness = await getCurrentProfileCompleteness();
      setProfile(updatedProfile);
      setCompleteness(updatedCompleteness);
      setForm(buildFormState(updatedProfile));
      setSaveMessage('Profile saved.');
    } catch (requestError) {
      setError(getProfileErrorMessage(requestError, 'Failed to save your profile.'));
    } finally {
      setIsSaving(false);
    }
  };

  if (!isStudent) {
    return (
      <div className="profile-page">
        <div className="app-bg">
          <div className="app-bg__mesh mesh--dim" />
          <div className="app-bg__grid mesh--dimmer" />
          <div className="app-bg__grain" />
        </div>
        <main className="profile-shell">
          <section className="profile-access solid-card">
            <p className="profile-kicker">Student area</p>
            <h1>Profile testing</h1>
            <p>This frontend page is wired to the student profile milestone only.</p>
            <div className="profile-access-actions">
              <Link to="/dashboard" className="btn btn-secondary">Back to dashboard</Link>
              <Link to="/jobs" className="btn btn-primary">Browse jobs</Link>
            </div>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <div className="app-bg">
        <div className="app-bg__mesh mesh--dim" />
        <div className="app-bg__grid mesh--dimmer" />
        <div className="app-bg__grain" />
      </div>

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
          <Link to="/dashboard" className="btn btn-secondary btn--sm">Dashboard</Link>
          <Link to="/applications" className="btn btn-ghost btn--sm">My applications</Link>
          <div className="jobs-topbar-user">
            <span className="role-badge">{user?.role?.replace('ROLE_', '').replace('_', ' ')}</span>
            <span className="jobs-topbar-email">{user?.email}</span>
          </div>
          <button onClick={handleLogout} className="btn btn-ghost btn--sm">Sign out</button>
        </div>
      </header>

      <main className="profile-shell">
        <section className="profile-hero solid-card">
          <div className="profile-hero-copy">
            <Link to="/dashboard" className="profile-kicker profile-link">Homepage</Link>
            <h1>Student profile</h1>
            <p>Use this page to test Milestone 3 from the frontend: bootstrap your profile, edit the student sections, and verify completeness updates immediately.</p>
          </div>
          <div className="profile-hero-summary">
            <div className={`profile-completeness-card profile-completeness-card--${completionTone}`}>
              <span>Completeness</span>
              <strong>{completeness ? `${completeness.percentage}%` : '--'}</strong>
              <small>{completeness ? `${completeness.completedFields} of ${completeness.totalFields} fields complete` : 'Loading summary'}</small>
            </div>
            <div className="profile-completeness-card">
              <span>Visibility</span>
              <strong>{form?.visibility ?? 'PRIVATE'}</strong>
              <small>{profile?.updatedAt ? `Updated ${new Date(profile.updatedAt).toLocaleString('en-IE')}` : 'Bootstrap on first visit'}</small>
            </div>
          </div>
        </section>

        {error ? (
          <section className="profile-feedback profile-feedback--error solid-card">
            <h2>Profile request failed</h2>
            <p>{error}</p>
          </section>
        ) : null}

        {saveMessage ? (
          <section className="profile-feedback profile-feedback--success solid-card">
            <p>{saveMessage}</p>
          </section>
        ) : null}

        {isLoading || !form ? (
          <section className="profile-loading solid-card">
            <div className="spinner spinner--lg" />
            <p>Loading your profile...</p>
          </section>
        ) : (
          <form className="profile-form solid-card" onSubmit={handleSubmit}>
            <div className="profile-section-header">
              <div>
                <p className="profile-kicker">Editable profile</p>
                <h2>Core details</h2>
              </div>
              <button type="submit" className="btn btn-primary btn--sm" disabled={isSaving}>
                {isSaving ? 'Saving...' : 'Save profile'}
              </button>
            </div>

            <div className="profile-form-grid">
              <label className="profile-field profile-field--full">
                <span>Headline</span>
                <input value={form.headline} onChange={(event) => updateForm('headline', event.target.value)} placeholder="Backend Engineer" />
              </label>

              <label className="profile-field profile-field--full">
                <span>Bio</span>
                <textarea value={form.bio} onChange={(event) => updateForm('bio', event.target.value)} rows={4} placeholder="Write a concise summary of your profile." />
              </label>

              <label className="profile-field">
                <span>Location</span>
                <input value={form.location} onChange={(event) => updateForm('location', event.target.value)} placeholder="Limerick" />
              </label>

              <label className="profile-field">
                <span>Phone</span>
                <input value={form.phone} onChange={(event) => updateForm('phone', event.target.value)} placeholder="+353 555 0101" />
              </label>

              <label className="profile-field">
                <span>Visibility</span>
                <select value={form.visibility} onChange={(event) => updateForm('visibility', event.target.value as ProfileVisibility)}>
                  <option value="PRIVATE">PRIVATE</option>
                  <option value="PUBLIC">PUBLIC</option>
                </select>
              </label>

              <label className="profile-field">
                <span>Job search status</span>
                <input value={form.jobSearchStatus} onChange={(event) => updateForm('jobSearchStatus', event.target.value)} placeholder="OPEN_TO_WORK" />
              </label>

              <label className="profile-field profile-field--full">
                <span>Skills</span>
                <input value={form.skillsText} onChange={(event) => updateForm('skillsText', event.target.value)} placeholder="Java, Spring Boot, PostgreSQL" />
                <small>Comma-separated. This maps to the student `skills` array in the new profile service.</small>
              </label>
            </div>

            <section className="profile-section">
              <div className="profile-section-header">
                <div>
                  <p className="profile-kicker">Student section</p>
                  <h3>Education</h3>
                </div>
                <button type="button" className="btn btn-secondary btn--sm" onClick={addEducation}>Add education</button>
              </div>
              <div className="profile-stack">
                {form.education.map((entry, index) => (
                  <div key={`education-${index}`} className="profile-item-card">
                    <div className="profile-item-header">
                      <strong>Education {index + 1}</strong>
                      <button type="button" className="btn btn-ghost btn--sm" onClick={() => removeEducation(index)}>Remove</button>
                    </div>
                    <div className="profile-form-grid">
                      <label className="profile-field">
                        <span>Institution</span>
                        <input value={entry.institution} onChange={(event) => updateEducation(index, 'institution', event.target.value)} />
                      </label>
                      <label className="profile-field">
                        <span>Degree</span>
                        <input value={toInputValue(entry.degree)} onChange={(event) => updateEducation(index, 'degree', event.target.value)} />
                      </label>
                      <label className="profile-field">
                        <span>Field of study</span>
                        <input value={toInputValue(entry.fieldOfStudy)} onChange={(event) => updateEducation(index, 'fieldOfStudy', event.target.value)} />
                      </label>
                      <label className="profile-field">
                        <span>Start date</span>
                        <input type="date" value={toInputValue(entry.startDate)} onChange={(event) => updateEducation(index, 'startDate', event.target.value)} />
                      </label>
                      <label className="profile-field">
                        <span>End date</span>
                        <input type="date" value={toInputValue(entry.endDate)} onChange={(event) => updateEducation(index, 'endDate', event.target.value)} />
                      </label>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="profile-section">
              <div className="profile-section-header">
                <div>
                  <p className="profile-kicker">Student section</p>
                  <h3>Experience</h3>
                </div>
                <button type="button" className="btn btn-secondary btn--sm" onClick={addExperience}>Add experience</button>
              </div>
              <div className="profile-stack">
                {form.experience.map((entry, index) => (
                  <div key={`experience-${index}`} className="profile-item-card">
                    <div className="profile-item-header">
                      <strong>Experience {index + 1}</strong>
                      <button type="button" className="btn btn-ghost btn--sm" onClick={() => removeExperience(index)}>Remove</button>
                    </div>
                    <div className="profile-form-grid">
                      <label className="profile-field">
                        <span>Company</span>
                        <input value={entry.company} onChange={(event) => updateExperience(index, 'company', event.target.value)} />
                      </label>
                      <label className="profile-field">
                        <span>Title</span>
                        <input value={entry.title} onChange={(event) => updateExperience(index, 'title', event.target.value)} />
                      </label>
                      <label className="profile-field profile-field--full">
                        <span>Description</span>
                        <textarea rows={3} value={toInputValue(entry.description)} onChange={(event) => updateExperience(index, 'description', event.target.value)} />
                      </label>
                      <label className="profile-field">
                        <span>Start date</span>
                        <input type="date" value={toInputValue(entry.startDate)} onChange={(event) => updateExperience(index, 'startDate', event.target.value)} />
                      </label>
                      <label className="profile-field">
                        <span>End date</span>
                        <input type="date" value={toInputValue(entry.endDate)} onChange={(event) => updateExperience(index, 'endDate', event.target.value)} />
                      </label>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="profile-section">
              <div className="profile-section-header">
                <div>
                  <p className="profile-kicker">Student section</p>
                  <h3>Portfolio links</h3>
                </div>
                <button type="button" className="btn btn-secondary btn--sm" onClick={addLink}>Add link</button>
              </div>
              <div className="profile-stack">
                {form.portfolioLinks.map((entry, index) => (
                  <div key={`link-${index}`} className="profile-item-card">
                    <div className="profile-item-header">
                      <strong>Link {index + 1}</strong>
                      <button type="button" className="btn btn-ghost btn--sm" onClick={() => removeLink(index)}>Remove</button>
                    </div>
                    <div className="profile-form-grid">
                      <label className="profile-field">
                        <span>Label</span>
                        <input value={entry.label} onChange={(event) => updateLink(index, 'label', event.target.value)} />
                      </label>
                      <label className="profile-field profile-field--full">
                        <span>URL</span>
                        <input value={entry.url} onChange={(event) => updateLink(index, 'url', event.target.value)} placeholder="https://github.com/your-handle" />
                      </label>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </form>
        )}
      </main>
    </div>
  );
}
