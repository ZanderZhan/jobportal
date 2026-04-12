export function normalizeRole(role: string | undefined): string | null {
  if (!role) {
    return null;
  }

  const normalized = role.replace(/^ROLE_/, '').trim().toUpperCase();
  return normalized || null;
}

export function isStudentRole(role: string | undefined, userType: string | undefined): boolean {
  const normalizedRole = normalizeRole(role);
  const normalizedUserType = normalizeRole(userType);
  return normalizedRole === 'STUDENT'
    || normalizedRole === 'JOB_SEEKER'
    || normalizedUserType === 'STUDENT'
    || normalizedUserType === 'JOB_SEEKER';
}

export function isEmployerRole(role: string | undefined, userType: string | undefined): boolean {
  const normalizedRole = normalizeRole(role);
  const normalizedUserType = normalizeRole(userType);
  return normalizedRole === 'HIRING'
    || normalizedRole === 'EMPLOYER'
    || normalizedUserType === 'HIRING'
    || normalizedUserType === 'EMPLOYER';
}
