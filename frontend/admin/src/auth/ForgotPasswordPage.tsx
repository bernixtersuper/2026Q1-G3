import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { KeyRound, Mail } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PasswordInput } from '@/components/ui/password-input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from '@/hooks/use-toast';
import { canUseCognitoAuth, confirmPasswordReset, requestPasswordReset } from './cognito';
import { AuthShell } from './AuthShell';
import { AuthStepIndicator } from './AuthStepIndicator';
import { OtpCodeField } from './OtpCodeField';
import { MIN_PASSWORD_LENGTH, PASSWORD_HINT, PASSWORD_RULE } from './passwordPolicy';

interface AmplifyErrorLike { name?: string; message?: string }

function describeResetError(err: unknown): string {
  if (err && typeof err === 'object') {
    const e = err as AmplifyErrorLike;
    switch (e.name) {
      case 'CodeMismatchException':
        return 'Incorrect verification code.';
      case 'ExpiredCodeException':
        return 'Code expired. Request a new one.';
      case 'InvalidPasswordException':
        return 'Password does not meet the required policy.';
      case 'UserNotFoundException':
        return 'No account found for this email.';
      case 'LimitExceededException':
      case 'TooManyRequestsException':
        return 'Too many attempts. Try again in a few minutes.';
    }
    if (e.message) return e.message;
  }
  return 'Password reset failed. Try again.';
}

type Stage = 'request' | 'confirm';

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [stage, setStage] = useState<Stage>('request');
  const [email, setEmail] = useState(params.get('email') || '');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const configured = canUseCognitoAuth();

  const handleRequest = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setError('Email is required.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await requestPasswordReset(email.trim());
      toast({ title: 'Code sent', description: 'Check your inbox for the reset code.', variant: 'success' });
      setStage('confirm');
    } catch (err) {
      setError(describeResetError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async (e: FormEvent) => {
    e.preventDefault();
    if (!code.trim()) {
      setError('Verification code is required.');
      return;
    }
    if (!PASSWORD_RULE.test(newPassword)) {
      setError(`Password must be at least ${MIN_PASSWORD_LENGTH} characters and include upper, lower, number, and symbol.`);
      return;
    }
    if (newPassword !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await confirmPasswordReset(email.trim(), code.trim(), newPassword);
      toast({ title: 'Password updated', description: 'You can now sign in.', variant: 'success' });
      navigate('/login', { replace: true });
    } catch (err) {
      setError(describeResetError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!email.trim()) return;
    setLoading(true);
    setError('');
    try {
      await requestPasswordReset(email.trim());
      toast({ title: 'Code resent', description: 'Check your inbox.', variant: 'success' });
    } catch (err) {
      setError(describeResetError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell>
      <Card className="border-border/60 shadow-lg">
        <CardHeader className="space-y-4">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
            {stage === 'request' ? <Mail className="h-6 w-6" /> : <KeyRound className="h-6 w-6" />}
          </div>
          <div className="space-y-1 text-center">
            <CardTitle className="text-2xl font-bold">Reset password</CardTitle>
            <CardDescription>
              {stage === 'request'
                ? "We'll email you a verification code"
                : 'Choose a new password for your account'}
            </CardDescription>
          </div>
          <AuthStepIndicator
            steps={['Email', 'New password']}
            current={stage === 'request' ? 1 : 2}
          />
        </CardHeader>

        {stage === 'request' ? (
          <form onSubmit={handleRequest}>
            <CardContent className="space-y-4">
              {error && <AuthError message={error} />}
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
              </div>
              <Button type="submit" className="w-full" disabled={!configured || loading}>
                {loading ? 'Sending...' : 'Send reset code'}
              </Button>
            </CardContent>
            <CardFooter>
              <p className="text-sm text-muted-foreground text-center w-full">
                Remembered it?{' '}
                <Link to="/login" className="text-primary hover:underline">
                  Back to sign in
                </Link>
              </p>
            </CardFooter>
          </form>
        ) : (
          <form onSubmit={handleConfirm}>
            <CardContent className="space-y-4">
              <div className="rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm">
                <p className="text-muted-foreground">Code sent to</p>
                <p className="font-medium truncate">{email}</p>
              </div>

              {error && <AuthError message={error} />}

              <OtpCodeField
                id="code"
                value={code}
                onChange={setCode}
                disabled={!configured || loading}
              />

              <div className="space-y-2">
                <Label htmlFor="new-password">New password</Label>
                <PasswordInput
                  id="new-password"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  disabled={!configured || loading}
                />
                <p className="text-xs text-muted-foreground">{PASSWORD_HINT}</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirm-password">Confirm new password</Label>
                <PasswordInput
                  id="confirm-password"
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  disabled={!configured || loading}
                />
              </div>

              <Button type="submit" className="w-full" disabled={!configured || loading}>
                {loading ? 'Updating...' : 'Update password'}
              </Button>

              <div className="flex flex-col gap-2">
                <Button
                  type="button"
                  variant="outline"
                  className="w-full"
                  onClick={handleResend}
                  disabled={!configured || loading}
                >
                  Resend code
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  className="w-full"
                  onClick={() => {
                    setStage('request');
                    setCode('');
                    setNewPassword('');
                    setConfirm('');
                    setError('');
                  }}
                  disabled={loading}
                >
                  Use a different email
                </Button>
              </div>
            </CardContent>
            <CardFooter>
              <p className="text-sm text-muted-foreground text-center w-full">
                <Link to="/login" className="text-primary hover:underline">
                  Back to sign in
                </Link>
              </p>
            </CardFooter>
          </form>
        )}
      </Card>
    </AuthShell>
  );
}

function AuthError({ message }: { message: string }) {
  return (
    <div className="rounded-md border border-destructive/20 bg-destructive/10 px-3 py-2 text-sm text-destructive">
      {message}
    </div>
  );
}
