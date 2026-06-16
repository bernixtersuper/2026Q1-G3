import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Hub } from 'aws-amplify/utils';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from './useAuth';
import { bootstrapBackendSession } from './sessionBootstrap';
import { hasCognitoSession } from './cognito';

export function AuthCallbackPage() {
  const navigate = useNavigate();
  const { establishSession } = useAuth();
  const [status, setStatus] = useState('Completando inicio de sesión...');
  const [error, setError] = useState('');
  const hasRun = useRef(false);

  useEffect(() => {
    let cancelled = false;

    const finalize = async () => {
      if (hasRun.current) return;
      hasRun.current = true;

      const result = await bootstrapBackendSession(establishSession, navigate);
      if (cancelled) return;

      if (!result.ok) {
        setError(result.error);
        setStatus('');
      }
    };

    // Al volver del Hosted UI con `?code=`, Amplify intercambia el código por tokens
    // de forma asíncrona y recién entonces emite el evento `signInWithRedirect`.
    const unsubscribe = Hub.listen('auth', ({ payload }) => {
      if (payload.event === 'signInWithRedirect') {
        void finalize();
      } else if (payload.event === 'signInWithRedirect_failure') {
        if (!cancelled) {
          setError('El inicio de sesión con Cognito falló. Volvé a intentarlo.');
          setStatus('');
        }
      }
    });

    // Caso recarga de la página (la sesión de Cognito ya existe y no hay código que
    // procesar): finalizamos directamente sin esperar el evento del Hub.
    (async () => {
      if (cancelled) return;
      if (await hasCognitoSession()) {
        void finalize();
      }
    })();

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [establishSession, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold text-center">MenuQR</CardTitle>
          <CardDescription className="text-center">
            {status || 'Procesando...'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {error && (
            <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-md space-y-3">
              <p>{error}</p>
              <a href="/login" className="text-primary hover:underline text-sm">
                Volver al inicio de sesión
              </a>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
