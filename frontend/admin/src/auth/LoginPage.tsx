import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PasswordInput } from '@/components/ui/password-input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from '@/hooks/use-toast';
import { canUseCognitoAuth, hasCognitoSession, signInWithEmail, signOutEverywhere } from './cognito';
import { bootstrapBackendSession } from './sessionBootstrap';
import { useAuth } from './useAuth';

interface AmplifyErrorLike { name?: string; message?: string }

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

export function LoginPage() {
  const navigate = useNavigate();
  const { establishSession, isAuthenticated } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [recovering, setRecovering] = useState(false);
  const configured = canUseCognitoAuth();

  const completeSignIn = useCallback(async () => {
    const result = await bootstrapBackendSession(establishSession, navigate);
    if (result.ok) {
      toast({ title: 'Welcome back', variant: 'success' });
      return;
    }
    await signOutEverywhere();
    setError(result.error);
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
        const step = result.nextStep?.signInStep;
        if (step === 'CONFIRM_SIGN_UP') {
          navigate(`/confirm?email=${encodeURIComponent(email.trim())}`);
          return;
        }
        if (step === 'RESET_PASSWORD') {
          navigate(`/forgot-password?email=${encodeURIComponent(email.trim())}`);
          return;
        }
        setError(`Additional sign-in step required: ${step ?? 'unknown'}`);
        return;
      }

      await completeSignIn();
    } catch (err) {
      setError(describeSignInError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleUseAnotherAccount = async () => {
    setError('');
    setLoading(true);
    try {
      await signOutEverywhere();
      setEmail('');
      setPassword('');
    } catch {
      setError('Could not sign out. Refresh the page and try again.');
    } finally {
      setLoading(false);
    }
  };

  const busy = loading || recovering;

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold text-center">MenuQR</CardTitle>
          <CardDescription className="text-center">
            {recovering ? 'Restoring your session...' : 'Sign in to manage your restaurant menu'}
          </CardDescription>
        </CardHeader>
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
      </Card>
    </div>
  );
}
