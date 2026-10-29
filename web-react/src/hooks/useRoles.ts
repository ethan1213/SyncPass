import { useSession } from '../auth/SessionProvider';
import { ROLES_CLAIM } from '../config/auth';

/**
 * Lee los grupos del ID token que emite Amazon Cognito.
 */
export function useUserRoles() {
  const { user } = useSession();
  const claim = user?.[ROLES_CLAIM];
  if (!Array.isArray(claim)) return [];
  return claim.filter((r): r is string => typeof r === 'string');
}

/** True si el usuario tiene al menos uno de los roles indicados. */
export function useHasRole(...slugs: string[]): boolean {
  const roles = useUserRoles();
  return slugs.some((s) => roles.includes(s));
}
