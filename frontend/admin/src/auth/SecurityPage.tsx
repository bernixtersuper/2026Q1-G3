import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { Shield, ShieldCheck, ShieldOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from '@/hooks/use-toast';
import {
  beginTotpSetup,
  disableTotpMfa,
  getTotpMfaStatus,
  type TotpMfaStatus,
  verifyAndEnableTotp,
} from './cognito';
import { OtpCodeField } from './OtpCodeField';

interface AmplifyErrorLike { name?: string; message?: string }

function describeMfaError(err: unknown, action: 'enable' | 'disable'): string {
  if (err && typeof err === 'object') {
    const e = err as AmplifyErrorLike;
    switch (e.name) {
      case 'CodeMismatchException':
      case 'EnableSoftwareTokenMFAException':
        return 'Incorrect authenticator code. Try the latest code from your app.';
      case 'InvalidParameterException':
        return action === 'disable'
          ? 'Could not disable MFA. Sign in again and retry.'
          : 'Invalid setup. Refresh the page and try again.';
      case 'LimitExceededException':
      case 'TooManyRequestsException':
        return 'Too many attempts. Try again in a few minutes.';
    }
    if (e.message) return e.message;
  }
  return action === 'disable' ? 'Could not disable MFA.' : 'Could not enable MFA.';
}

export function SecurityPage() {
  const [status, setStatus] = useState<TotpMfaStatus | null>(null);
  const [loadingStatus, setLoadingStatus] = useState(true);
  const [setupUri, setSetupUri] = useState<string | null>(null);
  const [verificationCode, setVerificationCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const refreshStatus = useCallback(async () => {
    setLoadingStatus(true);
    try {
      setStatus(await getTotpMfaStatus());
    } catch {
      setError('Could not load MFA settings.');
      setStatus('disabled');
    } finally {
      setLoadingStatus(false);
    }
  }, []);

  useEffect(() => {
    void refreshStatus();
  }, [refreshStatus]);

  const handleStartSetup = async () => {
    setError('');
    setBusy(true);
    try {
      setSetupUri(await beginTotpSetup());
      setVerificationCode('');
    } catch (err) {
      setError(describeMfaError(err, 'enable'));
    } finally {
      setBusy(false);
    }
  };

  const handleCancelSetup = () => {
    setSetupUri(null);
    setVerificationCode('');
    setError('');
  };

  const handleVerifySetup = async (e: FormEvent) => {
    e.preventDefault();
    if (verificationCode.trim().length < 6) {
      setError('Enter the 6-digit code from your authenticator app.');
      return;
    }
    setError('');
    setBusy(true);
    try {
      await verifyAndEnableTotp(verificationCode);
      setSetupUri(null);
      setVerificationCode('');
      setStatus('enabled');
      toast({
        title: 'Authenticator enabled',
        description: 'You will need a code from your app on future sign-ins.',
        variant: 'success',
      });
    } catch (err) {
      setError(describeMfaError(err, 'enable'));
    } finally {
      setBusy(false);
    }
  };

  const handleDisable = async () => {
    if (!window.confirm('Disable authenticator app MFA? You will only need your password to sign in.')) {
      return;
    }
    setError('');
    setBusy(true);
    try {
      await disableTotpMfa();
      setStatus('disabled');
      setSetupUri(null);
      toast({ title: 'Authenticator disabled', variant: 'success' });
    } catch (err) {
      setError(describeMfaError(err, 'disable'));
    } finally {
      setBusy(false);
    }
  };

  const enabled = status === 'enabled';

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Security</h1>
        <p className="text-muted-foreground">
          Optional two-factor authentication for your admin account (default: off).
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
              {enabled ? <ShieldCheck className="h-5 w-5" /> : <Shield className="h-5 w-5" />}
            </div>
            <div>
              <CardTitle>Authenticator app (TOTP)</CardTitle>
              <CardDescription>
                Any TOTP-compatible app (Authenticator, Authy, Microsoft Authenticator, etc.). Not required unless you enable it.
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {error && (
            <div className="rounded-md border border-destructive/20 bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {error}
            </div>
          )}

          <div className="rounded-lg border bg-muted/30 px-4 py-3 text-sm">
            <span className="text-muted-foreground">Status: </span>
            <span className="font-medium">
              {loadingStatus ? 'Loading…' : enabled ? 'Enabled' : 'Disabled (default)'}
            </span>
          </div>

          {!enabled && !setupUri && (
            <Button type="button" onClick={() => void handleStartSetup()} disabled={busy || loadingStatus}>
              {busy ? 'Preparing…' : 'Enable authenticator app'}
            </Button>
          )}

          {!enabled && setupUri && (
            <form onSubmit={handleVerifySetup} className="space-y-4">
              <p className="text-sm text-muted-foreground">
                Scan this QR code with your authenticator app, then enter the 6-digit code to confirm.
              </p>
              <div className="flex justify-center rounded-lg border bg-white p-4">
                <QRCodeSVG value={setupUri} size={192} level="M" />
              </div>
              <OtpCodeField
                id="totp-setup-code"
                label="Authenticator code"
                hint="Enter the 6-digit code from your authenticator app"
                value={verificationCode}
                onChange={setVerificationCode}
                disabled={busy}
              />
              <div className="flex flex-wrap gap-2">
                <Button type="submit" disabled={busy}>
                  {busy ? 'Verifying…' : 'Confirm and enable'}
                </Button>
                <Button type="button" variant="outline" onClick={handleCancelSetup} disabled={busy}>
                  Cancel
                </Button>
              </div>
            </form>
          )}

          {enabled && (
            <div className="space-y-3">
              <p className="text-sm text-muted-foreground">
                Sign-in will ask for your password and a code from your authenticator app.
              </p>
              <Button
                type="button"
                variant="outline"
                className="gap-2"
                onClick={() => void handleDisable()}
                disabled={busy || loadingStatus}
              >
                <ShieldOff className="h-4 w-4" />
                {busy ? 'Disabling…' : 'Disable authenticator app'}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
