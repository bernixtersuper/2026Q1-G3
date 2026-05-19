import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { MailCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from '@/hooks/use-toast';
import { authApi } from '@/shared/api/authApi';
import {
  canUseCognitoAuth,
  confirmSignUpCode,
  getCurrentTokens,
  resendConfirmationCode,
} from './cognito';
import { useAuth } from './useAuth';
import { AuthShell } from './AuthShell';
import { OtpCodeField } from './OtpCodeField';

interface AmplifyErrorLike { name?: string; message?: string }

function describeConfirmError(err: unknown): string {
  if (err && typeof err === 'object') {
    const e = err as AmplifyErrorLike;
    switch (e.name) {
      case 'CodeMismatchException':
        return 'Incorrect verification code.';
      case 'ExpiredCodeException':
        return 'Code expired. Request a new one.';
      case 'LimitExceededException':
      case 'TooManyRequestsException':
        return 'Too many attempts. Try again in a few minutes.';
    }
    if (e.message) return e.message;
  }
  return 'Verification failed. Try again.';
}

export function ConfirmSignUpPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { establishSession } = useAuth();
  const emailFromUrl = params.get('email')?.trim() ?? '';
  const emailLocked = emailFromUrl.length > 0;
  const [email, setEmail] = useState(emailFromUrl);
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const configured = canUseCognitoAuth();
  const resolvedEmail = emailLocked ? emailFromUrl : email.trim();

  useEffect(() => {
    if (emailFromUrl && email !== emailFromUrl) {
      setEmail(emailFromUrl);
    }
  }, [emailFromUrl, email]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!resolvedEmail || !code.trim()) {
      setError('Email and code are required.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await confirmSignUpCode(resolvedEmail, code.trim());
      const tokens = await getCurrentTokens();
      if (!tokens) {
        toast({ title: 'Email verified', description: 'Please sign in to continue.', variant: 'success' });
        navigate('/login', { replace: true });
        return;
      }
      try {
        const session = await authApi.bootstrapSession(tokens.idToken);
        establishSession(tokens.accessToken, session);
        navigate('/admin', { replace: true });
      } catch (apiError) {
        if (isAxiosError(apiError) && apiError.response?.status === 401 && apiError.response?.data?.code === 'UNKNOWN_USER') {
          navigate('/register', { replace: true });
          return;
        }
        setError('Verified, but could not start your session. Try signing in.');
      }
    } catch (err) {
      setError(describeConfirmError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!resolvedEmail) {
      setError('Enter your email first.');
      return;
    }
    setResending(true);
    setError('');
    try {
      await resendConfirmationCode(resolvedEmail);
      toast({ title: 'Code resent', description: 'Check your inbox.', variant: 'success' });
    } catch (err) {
      setError(describeConfirmError(err));
    } finally {
      setResending(false);
    }
  };

  return (
    <AuthShell>
      <Card className="border-border/60 shadow-lg">
        <CardHeader className="space-y-4">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
            <MailCheck className="h-6 w-6" />
          </div>
          <div className="space-y-1 text-center">
            <CardTitle className="text-2xl font-bold">Verify your email</CardTitle>
            <CardDescription>
              Enter the code we sent to complete your registration
            </CardDescription>
          </div>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            {error && (
              <div className="rounded-md border border-destructive/20 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                {error}
              </div>
            )}
            {emailLocked ? (
              <div className="rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm">
                <p className="text-muted-foreground">Code sent to</p>
                <p className="font-medium truncate">{emailFromUrl}</p>
              </div>
            ) : (
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  placeholder="you@restaurant.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={!configured || loading}
                />
                <p className="text-xs text-muted-foreground">
                  Use the same email you signed up with.
                </p>
              </div>
            )}
            <OtpCodeField
              id="code"
              value={code}
              onChange={setCode}
              disabled={!configured || loading}
            />
            <Button type="submit" className="w-full" disabled={!configured || loading}>
              {loading ? 'Verifying...' : 'Verify email'}
            </Button>
            <Button
              type="button"
              variant="outline"
              className="w-full"
              onClick={handleResend}
              disabled={!configured || resending}
            >
              {resending ? 'Sending...' : 'Resend code'}
            </Button>
          </CardContent>
          <CardFooter>
            <p className="text-sm text-muted-foreground text-center w-full">
              Wrong account?{' '}
              <Link to="/login" className="text-primary hover:underline">
                Back to sign in
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </AuthShell>
  );
}
