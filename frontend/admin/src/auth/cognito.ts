import {
  signIn as amplifySignIn,
  signUp as amplifySignUp,
  confirmSignUp as amplifyConfirmSignUp,
  resendSignUpCode as amplifyResendSignUpCode,
  resetPassword,
  confirmResetPassword,
  signOut as amplifySignOut,
  fetchAuthSession,
  autoSignIn,
  type SignInOutput,
} from 'aws-amplify/auth';
import { jwtDecode } from 'jwt-decode';
import { isAmplifyAuthConfigured } from './amplifyConfig';

export interface CognitoTokens {
  idToken: string;
  accessToken: string;
}

export function canUseCognitoAuth() {
  return isAmplifyAuthConfigured();
}

export function isAlreadySignedInError(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false;
  const e = err as { name?: string; message?: string };
  if (e.name === 'UserAlreadyAuthenticatedException') return true;
  const msg = (e.message ?? '').toLowerCase();
  return msg.includes('already') && (msg.includes('signed in') || msg.includes('logged in'));
}

function emailFromIdToken(idToken: string): string | null {
  try {
    const claims = jwtDecode<{ email?: string }>(idToken);
    return claims.email?.trim().toLowerCase() ?? null;
  } catch {
    return null;
  }
}

/** Cognito tiene sesión activa (puede no coincidir con md_* en localStorage). */
export async function hasCognitoSession(): Promise<boolean> {
  const tokens = await getCurrentTokens();
  return tokens !== null;
}

export async function signInWithEmail(email: string, password: string): Promise<SignInOutput> {
  const username = email.trim();
  const normalizedEmail = username.toLowerCase();

  try {
    return await amplifySignIn({
      username,
      password,
      options: { authFlowType: 'USER_SRP_AUTH' },
    });
  } catch (err) {
    if (!isAlreadySignedInError(err)) throw err;

    const tokens = await getCurrentTokens();
    if (tokens) {
      const sessionEmail = emailFromIdToken(tokens.idToken);
      if (sessionEmail && sessionEmail !== normalizedEmail) {
        await amplifySignOut();
        return amplifySignIn({
          username,
          password,
          options: { authFlowType: 'USER_SRP_AUTH' },
        });
      }
    }

    // Misma cuenta (o sesión sin email en el token): completar con la sesión de Cognito.
    return { isSignedIn: true, nextStep: { signInStep: 'DONE' } };
  }
}

export async function signUpWithEmail(email: string, password: string) {
  return amplifySignUp({
    username: email,
    password,
    options: {
      userAttributes: { email },
      autoSignIn: true,
    },
  });
}

export async function confirmSignUpCode(email: string, code: string) {
  const result = await amplifyConfirmSignUp({ username: email, confirmationCode: code });
  if (result.isSignUpComplete) {
    try {
      await autoSignIn();
    } catch {
      // Auto sign-in failed; user will sign in manually.
    }
  }
  return result;
}

export async function resendConfirmationCode(email: string) {
  return amplifyResendSignUpCode({ username: email });
}

export async function requestPasswordReset(email: string) {
  return resetPassword({ username: email });
}

export async function confirmPasswordReset(email: string, code: string, newPassword: string) {
  return confirmResetPassword({ username: email, confirmationCode: code, newPassword });
}

export async function getCurrentTokens(): Promise<CognitoTokens | null> {
  try {
    const session = await fetchAuthSession();
    const idToken = session.tokens?.idToken?.toString();
    const accessToken = session.tokens?.accessToken?.toString();
    if (!idToken || !accessToken) return null;
    return { idToken, accessToken };
  } catch {
    return null;
  }
}

export async function signOutEverywhere() {
  await amplifySignOut();
}
