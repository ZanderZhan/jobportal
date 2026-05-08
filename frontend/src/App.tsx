import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { CallbackPage } from './pages/auth/CallbackPage';
import { DashboardPage } from './pages/DashboardPage';
import JobsPage from './pages/job/JobsPage';
import JobDetailPage from './pages/job/JobDetailPage';
import { EmployerJobsPage } from './pages/job/EmployerJobsPage';
import { JobFormPage } from './pages/job/JobFormPage';
import { MyApplicationsPage } from './pages/application/MyApplicationsPage';
import { EmployerJobApplicationsPage } from './pages/application/EmployerJobApplicationsPage';
import { EmployerApplicationsOverviewPage } from './pages/application/EmployerApplicationsOverviewPage';
import { ProfilePage } from './pages/profile/ProfilePage';
import { NotificationsPage } from './pages/NotificationsPage';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/auth/callback" element={<CallbackPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route path="/jobs" element={<JobsPage />} />
          <Route path="/jobs/:id" element={<JobDetailPage />} />
          <Route
            path="/applications"
            element={
              <ProtectedRoute>
                <MyApplicationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          {/* Employer routes */}
          <Route
            path="/employer/jobs"
            element={
              <ProtectedRoute>
                <EmployerJobsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/jobs/new"
            element={
              <ProtectedRoute>
                <JobFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/jobs/:id/edit"
            element={
              <ProtectedRoute>
                <JobFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/jobs/:id/applications"
            element={
              <ProtectedRoute>
                <EmployerJobApplicationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/applications"
            element={
              <ProtectedRoute>
                <EmployerApplicationsOverviewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
