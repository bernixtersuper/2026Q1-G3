import { useTheme } from '@/lib/ThemeContext';
import { useOrder } from '@/lib/OrderContext';

interface HeaderProps {
  restaurantName: string;
  tableNumber: string;
  logoUrl?: string | null;
}

export function Header({ restaurantName, tableNumber, logoUrl }: HeaderProps) {
  const { theme } = useTheme();
  const { sessionCode } = useOrder();

  return (
    <header className="relative overflow-hidden">
      {/* Animated gradient background */}
      <div 
        className="absolute inset-0"
        style={{
          background: theme.showGradientHeader
            ? `linear-gradient(${theme.gradientDirection === 'to-br' ? '135deg' : 
                theme.gradientDirection === 'to-r' ? '90deg' : 
                theme.gradientDirection === 'to-b' ? '180deg' : '45deg'}, 
              ${theme.gradientStart}, ${theme.gradientEnd})`
            : theme.primaryColor,
        }}
      />
      
      {/* Decorative elements */}
      <div 
        className="absolute -right-10 -top-10 h-40 w-40 rounded-full opacity-20"
        style={{ backgroundColor: theme.accentColor }}
      />
      <div 
        className="absolute -bottom-5 -left-5 h-24 w-24 rounded-full opacity-10"
        style={{ backgroundColor: '#fff' }}
      />
      
      <div className="relative mx-auto max-w-3xl px-4 py-6 sm:py-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            {logoUrl ? (
              <img 
                src={logoUrl} 
                alt={restaurantName} 
                className="h-14 w-14 rounded-2xl object-cover shadow-lg ring-2 ring-white/30 sm:h-16 sm:w-16"
              />
            ) : (
              <div 
                className="flex h-14 w-14 items-center justify-center rounded-2xl text-2xl font-bold shadow-lg sm:h-16 sm:w-16"
                style={{ 
                  backgroundColor: 'rgba(255,255,255,0.2)',
                  color: '#fff',
                }}
              >
                {restaurantName.charAt(0)}
              </div>
            )}
            <div>
              <h1 className="text-xl font-bold text-white drop-shadow-sm sm:text-2xl">
                {restaurantName}
              </h1>
              <div className="mt-1 flex items-center gap-2">
                <span 
                  className="rounded-full px-2.5 py-0.5 text-xs font-medium backdrop-blur-sm"
                  style={{ backgroundColor: 'rgba(255,255,255,0.2)', color: '#fff' }}
                >
                  Table {tableNumber}
                </span>
              </div>
            </div>
          </div>
          
          {sessionCode && (
            <div className="text-right">
              <p className="text-[10px] uppercase tracking-wider text-white/60">
                Session Code
              </p>
              <p 
                className="mt-0.5 rounded-lg px-3 py-1 font-mono text-lg font-bold tracking-widest text-white backdrop-blur-sm"
                style={{ backgroundColor: 'rgba(255,255,255,0.15)' }}
              >
                {sessionCode}
              </p>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
