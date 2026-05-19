import { useOrder } from '@/lib/OrderContext';
import { useTheme } from '@/lib/ThemeContext';
import { formatCurrency } from '@/lib/utils';

interface CartButtonProps {
  onClick: () => void;
}

export function CartButton({ onClick }: CartButtonProps) {
  const { order, getItemCount, getTotal, isLoading } = useOrder();
  const { theme } = useTheme();
  const itemCount = getItemCount();
  const isSubmitted = order?.status && order.status !== 'DRAFT';

  if (itemCount === 0 && !isSubmitted) {
    return null;
  }

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 p-4">
      <div className="mx-auto max-w-3xl">
        <button
          onClick={onClick}
          disabled={isLoading}
          className="flex w-full items-center justify-between rounded-2xl p-4 shadow-xl transition-all active:scale-[0.98] disabled:opacity-70"
          style={{ 
            backgroundColor: theme.primaryColor,
            boxShadow: `0 10px 40px -10px ${theme.primaryColor}80`,
          }}
        >
          <div className="flex items-center gap-3">
            <div 
              className="flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold"
              style={{ 
                backgroundColor: 'rgba(255,255,255,0.2)',
                color: '#fff',
              }}
            >
              {isSubmitted ? (
                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              ) : (
                itemCount
              )}
            </div>
            <span className="font-semibold text-white">
              {isSubmitted ? 'View Order' : 'View Cart'}
            </span>
          </div>
          
          <span className="text-lg font-bold text-white">
            {formatCurrency(getTotal())}
          </span>
        </button>
      </div>
    </div>
  );
}
