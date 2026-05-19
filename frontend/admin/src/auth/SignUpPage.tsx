import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PasswordInput } from '@/components/ui/password-input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from '@/hooks/use-toast';
import { canUseCognitoAuth, signUpWithEmail } from './cognito';
import { MIN_PASSWORD_LENGTH, PASSWORD_HINT, PASSWORD_RULE } from './passwordPolicy';

interface AmplifyErrorLike { name?: string; message?: string }

function describeSignUpError(err: unknown): string {
  if (err && typeof err === 'object') {
    const e = err as AmplifyErrorLike;
    switch (e.name) {
      case 'UsernameExistsException':
        return 'An account with this email already exists. Try signing in.';
      case 'InvalidPasswordException':
        return 'Password does not meet the required policy.';
      case 'InvalidParameterException':
        return e.message || 'Invalid email or password.';
      case 'TooManyRequestsException':
      case 'LimitExceededException':
        return 'Too many attempts. Try again in a few minutes.';
    }
    if (e.message) return e.message;
  }
  return 'Sign-up failed. Try again.';
}

export function SignUpPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const configured = canUseCognitoAuth();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setError('Email is required.');
      return;
    }
    if (!PASSWORD_RULE.test(password)) {
      setError(`Password must be at least ${MIN_PASSWORD_LENGTH} characters and include upper, lower, number, and symbol.`);
      return;
    }
    if (password !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await signUpWithEmail(email.trim(), password);
      toast({ title: 'Check your inbox', description: 'We sent you a verification code.', variant: 'success' });
      navigate(`/confirm?email=${encodeURIComponent(email.trim())}`);
    } catch (err) {
      setError(describeSignUpError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold text-center">Create your account</CardTitle>
          <CardDescription className="text-center">
            You'll add restaurant details next
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            {error && (
              <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-md">
                {error}
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
                disabled={!configured || loading}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <PasswordInput
                id="password"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={!configured || loading}
              />
              <p className="text-xs text-muted-foreground">{PASSWORD_HINT}</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirm">Confirm password</Label>
              <PasswordInput
                id="confirm"
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                disabled={!configured || loading}
              />
            </div>
            <Button type="submit" className="w-full" disabled={!configured || loading}>
              {loading ? 'Creating account...' : 'Create account'}
            </Button>
          </CardContent>
          <CardFooter>
            <p className="text-sm text-muted-foreground text-center w-full">
              Already have an account?{' '}
              <Link to="/login" className="text-primary hover:underline">
                Sign in
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
