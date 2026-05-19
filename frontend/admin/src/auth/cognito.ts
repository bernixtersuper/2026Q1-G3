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
} from 'aws-amplify/auth';
import { isAmplifyAuthConfigured } from './amplifyConfig';

export interface CognitoTokens {
  idToken: string;
  accessToken: string;
}

export function canUseCognitoAuth() {
  return isAmplifyAuthConfigured();
}

export async function signInWithEmail(email: string, password: string) {
  return amplifySignIn({
    username: email,
    password,
    options: { authFlowType: 'USER_SRP_AUTH' },
  });
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
