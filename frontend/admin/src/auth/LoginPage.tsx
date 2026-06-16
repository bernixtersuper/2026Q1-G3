import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PasswordInput } from '@/components/ui/password-input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from '@/hooks/use-toast';
import {
  canUseCognitoAuth,
  canUseGoogleAuth,
  confirmSignInWithTotp,
  hasCognitoSession,
  signInWithEmail,
  signInWithGoogle,
  signOutEverywhere,
} from './cognito';
import { bootstrapBackendSession } from './sessionBootstrap';
import { useAuth } from './useAuth';
import { OtpCodeField } from './OtpCodeField';

interface AmplifyErrorLike { name?: string; message?: string }

type LoginStep = 'credentials' | 'totp';

function describeSignInError(err: unknown): string {
  if (err && typeof err === 'object') {
    const e = err as AmplifyErrorLike;
    switch (e.name) {
      case 'NotAuthorizedException':
        return 'Incorrect email or password.';
      case 'UserNotFoundException':
        return 'No account found for this email.';
      case 'UserNotConfirmedException':
        return 'Please verify your email before signing in.';
      case 'PasswordResetRequiredException':
        return 'You need to reset your password before signing in.';
      case 'CodeMismatchException':
        return 'Incorrect authenticator code.';
      case 'TooManyRequestsException':
      case 'LimitExceededException':
        return 'Too many attempts. Try again in a few minutes.';
      case 'UserAlreadyAuthenticatedException':
        return 'You already have an active session. Completing sign-in...';
    }
    if (e.message) return e.message;
  }
  return 'Sign-in failed. Try again.';
}

function GoogleIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1Z"
      />
      <path
        fill="#34A853"
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23Z"
      />
      <path
        fill="#FBBC05"
        d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.18a11 11 0 0 0 0 9.88l3.66-2.84Z"
      />
      <path
        fill="#EA4335"
        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1A11 11 0 0 0 2.18 7.06l3.66 2.84C6.71 7.3 9.14 5.38 12 5.38Z"
      />
    </svg>
  );
}

export function LoginPage() {
  const navigate = useNavigate();
  const { establishSession, isAuthenticated } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [step, setStep] = useState<LoginStep>('credentials');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [recovering, setRecovering] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const configured = canUseCognitoAuth();
  const googleEnabled = canUseGoogleAuth();

  const completeSignIn = useCallback(async () => {
    const result = await bootstrapBackendSession(establishSession, navigate);
    if (result.ok) {
      toast({ title: 'Welcome back', variant: 'success' });
      return;
    }
    await signOutEverywhere();
    setError(result.error);
    setStep('credentials');
  }, [establishSession, navigate]);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/admin', { replace: true });
      return;
    }
    if (!configured) return;

    let cancelled = false;
    (async () => {
      if (!(await hasCognitoSession())) return;
      setRecovering(true);
      setError('');
      const result = await bootstrapBackendSession(establishSession, navigate);
      if (cancelled) return;
      if (!result.ok) {
        await signOutEverywhere();
        setError(result.error);
      }
      setRecovering(false);
    })();

    return () => {
      cancelled = true;
    };
  }, [configured, establishSession, isAuthenticated, navigate]);

  const handleEmailSignIn = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Email and password are required.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await signInWithEmail(email.trim(), password);

      if (!result.isSignedIn) {
        const signInStep = result.nextStep?.signInStep;
        if (signInStep === 'CONFIRM_SIGN_UP') {
          navigate(`/confirm?email=${encodeURIComponent(email.trim())}`);
          return;
        }
        if (signInStep === 'RESET_PASSWORD') {
          navigate(`/forgot-password?email=${encodeURIComponent(email.trim())}`);
          return;
        }
        if (signInStep === 'CONFIRM_SIGN_IN_WITH_TOTP_CODE') {
          setStep('totp');
          setTotpCode('');
          return;
        }
        setError(`Additional sign-in step required: ${signInStep ?? 'unknown'}`);
        return;
      }

      await completeSignIn();
    } catch (err) {
      setError(describeSignInError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleTotpSignIn = async (e: FormEvent) => {
    e.preventDefault();
    if (totpCode.trim().length < 6) {
      setError('Enter the 6-digit code from your authenticator app.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await confirmSignInWithTotp(totpCode);
      if (!result.isSignedIn) {
        setError('Sign-in is not complete. Try again or use a fresh authenticator code.');
        return;
      }
      await completeSignIn();
    } catch (err) {
      setError(describeSignInError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setError('');
    setGoogleLoading(true);
    try {
      // Redirige al Hosted UI de Cognito; el retorno lo maneja /auth/callback.
      await signInWithGoogle();
    } catch (err) {
      setError(describeSignInError(err));
      setGoogleLoading(false);
    }
  };

  const handleBackToCredentials = async () => {
    setError('');
    setTotpCode('');
    setStep('credentials');
    await signOutEverywhere();
  };

  const handleUseAnotherAccount = async () => {
    setError('');
    setLoading(true);
    try {
      await signOutEverywhere();
      setEmail('');
      setPassword('');
      setTotpCode('');
      setStep('credentials');
    } catch {
      setError('Could not sign out. Refresh the page and try again.');
    } finally {
      setLoading(false);
    }
  };

  const busy = loading || recovering || googleLoading;

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold text-center">MenuQR</CardTitle>
          <CardDescription className="text-center">
            {recovering
              ? 'Restoring your session...'
              : step === 'totp'
                ? 'Enter your authenticator code'
                : 'Sign in to manage your restaurant menu'}
          </CardDescription>
        </CardHeader>

        {step === 'credentials' ? (
          <form onSubmit={handleEmailSignIn}>
            <CardContent className="space-y-4">
              {error && (
                <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-md space-y-2">
                  <p>{error}</p>
                  <button
                    type="button"
                    onClick={() => void handleUseAnotherAccount()}
                    className="text-primary hover:underline text-xs"
                    disabled={busy}
                  >
                    Sign in with a different account
                  </button>
                </div>
              )}

              {googleEnabled && (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    className="w-full"
                    onClick={() => void handleGoogleSignIn()}
                    disabled={!configured || busy}
                  >
                    <GoogleIcon className="mr-2 h-4 w-4" />
                    {googleLoading ? 'Redirecting to Google…' : 'Continue with Google'}
                  </Button>
                  <div className="relative">
                    <div className="absolute inset-0 flex items-center">
                      <span className="w-full border-t" />
                    </div>
                    <div className="relative flex justify-center text-xs uppercase">
                      <span className="bg-card px-2 text-muted-foreground">
                        or continue with email
                      </span>
                    </div>
                  </div>
                </>
              )}

              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={!configured || busy}
                />
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="password">Password</Label>
                  <Link to="/forgot-password" className="text-xs text-primary hover:underline">
                    Forgot password?
                  </Link>
                </div>
                <PasswordInput
                  id="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={!configured || busy}
                />
              </div>
              <Button type="submit" className="w-full" disabled={!configured || busy}>
                {busy ? (recovering ? 'Restoring session...' : 'Signing in...') : 'Sign in'}
              </Button>

              {!configured && (
                <p className="text-xs text-muted-foreground text-center">
                  Configure Cognito to enable sign-in.
                </p>
              )}
            </CardContent>
            <CardFooter>
              <p className="text-sm text-muted-foreground text-center w-full">
                New here?{' '}
                <Link to="/signup" className="text-primary hover:underline">
                  Create an account
                </Link>
              </p>
            </CardFooter>
          </form>
        ) : (
          <form onSubmit={handleTotpSignIn}>
            <CardContent className="space-y-4">
              {error && (
                <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-md">
                  {error}
                </div>
              )}
              <div className="rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm">
                <p className="text-muted-foreground">Signing in as</p>
                <p className="font-medium truncate">{email}</p>
              </div>
              <OtpCodeField
                id="totp-sign-in-code"
                label="Authenticator code"
                hint="Enter the 6-digit code from your authenticator app"
                value={totpCode}
                onChange={setTotpCode}
                disabled={busy}
              />
              <Button type="submit" className="w-full" disabled={busy}>
                {busy ? 'Verifying…' : 'Continue'}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="w-full"
                onClick={() => void handleBackToCredentials()}
                disabled={busy}
              >
                Back to password
              </Button>
            </CardContent>
          </form>
        )}
      </Card>
    </div>
  );
}
