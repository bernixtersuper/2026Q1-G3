import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { useTheme } from '@/lib/ThemeContext';

type ToastType = 'success' | 'error' | 'info';

interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}

let toastId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((message: string, type: ToastType = 'info') => {
    const id = String(++toastId);
    setToasts(prev => [...prev, { id, message, type }]);
    
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </ToastContext.Provider>
  );
}

function ToastContainer({ toasts, onRemove }: { toasts: Toast[]; onRemove: (id: string) => void }) {
  const { theme } = useTheme();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-20 left-0 right-0 z-[200] flex flex-col items-center gap-2 px-4 pointer-events-none sm:bottom-24">
      {toasts.map(toast => (
        <ToastItem key={toast.id} toast={toast} onRemove={onRemove} theme={theme} />
      ))}
    </div>
  );
}

function ToastItem({ toast, onRemove, theme }: { toast: Toast; onRemove: (id: string) => void; theme: any }) {
  const config = {
    success: {
      icon: CheckCircle,
      bg: 'bg-green-600',
      iconColor: 'text-white',
    },
    error: {
      icon: AlertCircle,
      bg: 'bg-red-600',
      iconColor: 'text-white',
    },
    info: {
      icon: Info,
      bg: '',
      iconColor: '',
    },
  };

  const { icon: Icon, bg, iconColor } = config[toast.type];

  return (
    <div
      className={`pointer-events-auto flex items-center gap-3 rounded-xl px-4 py-3 shadow-lg animate-in slide-in-from-bottom-4 fade-in duration-300 ${bg || ''}`}
      style={{
        backgroundColor: !bg ? theme.cardBackground : undefined,
        color: !bg ? theme.textColor : 'white',
        boxShadow: '0 4px 20px rgba(0,0,0,0.15)',
        maxWidth: '90vw',
      }}
    >
      <Icon className={`h-5 w-5 shrink-0 ${iconColor || ''}`} style={{ color: !iconColor ? theme.primaryColor : undefined }} />
      <span className="text-sm font-medium">{toast.message}</span>
      <button
        onClick={() => onRemove(toast.id)}
        className="ml-2 shrink-0 rounded-full p-1 transition-colors hover:bg-black/10"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
