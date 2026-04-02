import { Link } from 'react-router-dom';
import { type Job, formatSalary, formatEmploymentType, formatDate } from '../lib/jobApi';
import './JobCard.css';

interface JobCardProps {
  job: Job;
}

export default function JobCard({ job }: JobCardProps) {
  return (
    <Link to={`/jobs/${job.id}`} className="job-card">
      <div className="job-card-header">
        <div className="job-card-company-logo">
          {job.company.charAt(0).toUpperCase()}
        </div>
        <div className="job-card-header-info">
          <h3 className="job-card-title">{job.title}</h3>
          <p className="job-card-company">{job.company}</p>
        </div>
      </div>
      
      <div className="job-card-meta">
        {job.location && (
          <span className="job-card-meta-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
              <circle cx="12" cy="10" r="3"/>
            </svg>
            {job.location}
          </span>
        )}
        {job.employmentType && (
          <span className="job-card-meta-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
              <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
            </svg>
            {formatEmploymentType(job.employmentType)}
          </span>
        )}
      </div>
      
      <p className="job-card-salary">
        {formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency)}
      </p>
      
      {job.requirements.length > 0 && (
        <div className="job-card-tags">
          {job.requirements.slice(0, 4).map((req, index) => (
            <span key={index} className="job-card-tag">{req}</span>
          ))}
          {job.requirements.length > 4 && (
            <span className="job-card-tag job-card-tag-more">+{job.requirements.length - 4}</span>
          )}
        </div>
      )}
      
      <div className="job-card-footer">
        <span className="job-card-date">{formatDate(job.createdAt)}</span>
        <span className={`job-card-status job-card-status-${job.status.toLowerCase()}`}>
          {job.status}
        </span>
      </div>
    </Link>
  );
}
