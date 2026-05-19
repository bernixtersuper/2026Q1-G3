import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface AuthShellProps {
  children: ReactNode;
  className?: string;
}

export function AuthShell({ children, className }: AuthShellProps) {
  return (
    <div className={cn('min-h-screen flex items-center justify-center p-4', className)}>
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 bg-gradient-to-br from-primary/5 via-background to-background"
      />
      <div className="relative w-full max-w-md">{children}</div>
    </div>
  );
}
