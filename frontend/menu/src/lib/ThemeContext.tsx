import { createContext, useContext, useEffect, type ReactNode } from 'react';
import type { Theme } from '@/shared/types';

interface ThemeContextValue {
  theme: Theme;
}

const defaultTheme: Theme = {
  primaryColor: '#374151',
  secondaryColor: '#6b7280',
  accentColor: '#9ca3af',
  backgroundColor: '#f9fafb',
  textColor: '#111827',
  cardBackground: '#ffffff',
  gradientStart: '#374151',
  gradientEnd: '#1f2937',
  gradientDirection: 'to-br',
  fontFamily: 'Inter',
  borderRadius: 'lg',
  logoUrl: null,
  bannerUrl: null,
  showGradientHeader: true,
};

const ThemeContext = createContext<ThemeContextValue>({ theme: defaultTheme });

export function useTheme() {
  return useContext(ThemeContext);
}

interface ThemeProviderProps {
  theme: Theme | null;
  children: ReactNode;
}

export function ThemeProvider({ theme, children }: ThemeProviderProps) {
  const activeTheme = theme || defaultTheme;

  useEffect(() => {
    const root = document.documentElement;
    
    root.style.setProperty('--theme-primary', activeTheme.primaryColor);
    root.style.setProperty('--theme-secondary', activeTheme.secondaryColor);
    root.style.setProperty('--theme-accent', activeTheme.accentColor);
    root.style.setProperty('--theme-background', activeTheme.backgroundColor);
    root.style.setProperty('--theme-text', activeTheme.textColor);
    root.style.setProperty('--theme-card', activeTheme.cardBackground);
    root.style.setProperty('--theme-gradient-start', activeTheme.gradientStart);
    root.style.setProperty('--theme-gradient-end', activeTheme.gradientEnd);
    
    const radiusMap: Record<string, string> = {
      none: '0',
      sm: '0.25rem',
      md: '0.375rem',
      lg: '0.5rem',
      xl: '0.75rem',
      '2xl': '1rem',
      full: '9999px',
    };
    root.style.setProperty('--theme-radius', radiusMap[activeTheme.borderRadius] || '0.5rem');
    
    if (activeTheme.fontFamily) {
      root.style.setProperty('--theme-font', activeTheme.fontFamily);
    }
  }, [activeTheme]);

  return (
    <ThemeContext.Provider value={{ theme: activeTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}
