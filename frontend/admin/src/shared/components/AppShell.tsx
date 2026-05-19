import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/useAuth';
import { UtensilsCrossed, BarChart3, LogOut, QrCode, ClipboardList, Palette } from 'lucide-react';
import { cn } from '@/lib/utils';

const navItems = [
  { to: '/admin', icon: BarChart3, label: 'Analytics' },
  { to: '/admin/menu', icon: UtensilsCrossed, label: 'Menu' },
  { to: '/admin/tables', icon: QrCode, label: 'Tables & QR' },
  { to: '/admin/orders', icon: ClipboardList, label: 'Orders' },
  { to: '/admin/theme', icon: Palette, label: 'Theme' },
];

export function AppShell() {
  const { restaurantName, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="flex">
        <aside className="fixed left-0 top-0 z-40 h-screen w-64 border-r bg-card">
          <div className="flex h-full flex-col">
            <div className="border-b px-6 py-4">
              <h1 className="text-xl font-bold text-primary">MenuQR</h1>
              <p className="text-sm text-muted-foreground truncate">{restaurantName}</p>
            </div>
            
            <nav className="flex-1 space-y-1 px-3 py-4">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/admin'}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                    )
                  }
                >
                  <item.icon className="h-5 w-5" />
                  {item.label}
                </NavLink>
              ))}
            </nav>

            <div className="border-t p-3">
              <button
                onClick={handleLogout}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-destructive hover:text-destructive-foreground"
              >
                <LogOut className="h-5 w-5" />
                Logout
              </button>
            </div>
          </div>
        </aside>

        <main className="ml-64 flex-1 p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
