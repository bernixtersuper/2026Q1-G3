import { Amplify } from 'aws-amplify';

const userPoolId = import.meta.env.VITE_COGNITO_USER_POOL_ID?.trim();
const userPoolClientId = import.meta.env.VITE_COGNITO_CLIENT_ID?.trim();

// OAuth / Hosted UI (login federado con Google). Opcional: si falta alguno,
// el SPA sigue funcionando solo con email/password.
const cognitoDomain = import.meta.env.VITE_COGNITO_DOMAIN?.trim();
const redirectSignIn = import.meta.env.VITE_COGNITO_REDIRECT_SIGN_IN?.trim();
const redirectSignOut = import.meta.env.VITE_COGNITO_REDIRECT_SIGN_OUT?.trim();

export function isAmplifyAuthConfigured(): boolean {
  return Boolean(userPoolId && userPoolClientId);
}

export function isOAuthConfigured(): boolean {
  return Boolean(cognitoDomain && redirectSignIn && redirectSignOut);
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
          ...(isOAuthConfigured()
            ? {
                oauth: {
                  domain: cognitoDomain!,
                  scopes: ['openid', 'email', 'profile'],
                  redirectSignIn: [redirectSignIn!],
                  redirectSignOut: [redirectSignOut!],
                  responseType: 'code',
                  providers: ['Google'],
                },
              }
            : {}),
        },
      },
    },
  });
}
