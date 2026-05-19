import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value;
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(num);
}

export function getSessionId(): string {
  let sessionId = sessionStorage.getItem('md_session');
  if (!sessionId) {
    sessionId = crypto.randomUUID();
    sessionStorage.setItem('md_session', sessionId);
  }
  return sessionId;
}

export function getStoredTableSession(qrToken: string): { sessionId: string; sessionCode: string } | null {
  const stored = sessionStorage.getItem(`md_table_${qrToken}`);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  }
  return null;
}

export function storeTableSession(qrToken: string, sessionId: string, sessionCode: string): void {
  sessionStorage.setItem(`md_table_${qrToken}`, JSON.stringify({ sessionId, sessionCode }));
}

export function getGuestName(): string {
  return sessionStorage.getItem('md_guest_name') || '';
}

export function setGuestName(name: string): void {
  sessionStorage.setItem('md_guest_name', name);
}
