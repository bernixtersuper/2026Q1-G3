import { Amplify } from 'aws-amplify';

const userPoolId = import.meta.env.VITE_COGNITO_USER_POOL_ID?.trim();
const userPoolClientId = import.meta.env.VITE_COGNITO_CLIENT_ID?.trim();

export function isAmplifyAuthConfigured(): boolean {
  return Boolean(userPoolId && userPoolClientId);
}

export function configureAmplifyAuth() {
  if (!isAmplifyAuthConfigured()) {
    return;
  }

  Amplify.configure({
    Auth: {
      Cognito: {
        userPoolId: userPoolId!,
        userPoolClientId: userPoolClientId!,
        signUpVerificationMethod: 'code',
        loginWith: {
          email: true,
        },
      },
    },
  });
}
