import { useState, useEffect, useMemo, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { ThemeProvider } from '@/lib/ThemeContext';
import { OrderProvider, useOrder } from '@/lib/OrderContext';
import { ToastProvider } from '@/components/Toast';
import { Header } from '@/components/Header';
import { MenuSection } from '@/components/MenuSection';
import { ItemModal } from '@/components/ItemModal';
import { CartButton } from '@/components/CartButton';
import { CartDrawer } from '@/components/CartDrawer';
import { getStoredTableSession, storeTableSession } from '@/lib/utils';
import type { MenuItem, TableMenuResponse } from '@/shared/types';

function TableMenuContent({ data, qrToken }: { data: TableMenuResponse; qrToken: string }) {
  const { setSession, setOrder, sessionId, addItem, subscribeToUpdates } = useOrder();
  // Evita duplicar MENU_VIEW por re-renders.
  const menuViewSentRef = useRef(false);
  const [selectedItem, setSelectedItem] = useState<MenuItem | null>(null);
  const [showCart, setShowCart] = useState(false);
  const [activeSection, setActiveSection] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isJoining, setIsJoining] = useState(false);
  const [isCategoryFilterEnabled, setIsCategoryFilterEnabled] = useState(false);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);

  const joinMutation = useMutation({
    mutationFn: async () => {
      const stored = getStoredTableSession(qrToken);
      return menuApi.joinTable(qrToken, stored?.sessionCode);
    },
    onSuccess: (result) => {
      setSession(result.sessionId, result.sessionCode, result.tableNumber);
      storeTableSession(qrToken, result.sessionId, result.sessionCode);
      if (result.currentOrder) {
        setOrder(result.currentOrder);
        if (result.currentOrder.status !== 'DRAFT') {
          setTimeout(() => subscribeToUpdates(qrToken), 100);
        }
      }
      setIsJoining(false);
    },
    onError: () => {
      setIsJoining(false);
    },
  });

  useEffect(() => {
    if (!sessionId && !isJoining) {
      setIsJoining(true);
      joinMutation.mutate();
    }
  }, [sessionId, isJoining]);

  useEffect(() => {
    if (data.sections.length > 0 && !activeSection) {
      setActiveSection(data.sections[0].id);
    }
  }, [data.sections, activeSection]);

  useEffect(() => {
    if (!selectedCategoryId) return;
    const exists = data.sections.some((section) => section.id === selectedCategoryId);
    if (!exists) {
      setSelectedCategoryId(null);
    }
  }, [data.sections, selectedCategoryId]);

  useEffect(() => {
    if (!sessionId || menuViewSentRef.current) return;

    menuViewSentRef.current = true;
    menuApi.recordEvent(data.slug, {
      eventType: 'MENU_VIEW',
      sessionId,
      metadata: { source: 'TABLE_QR' },
    }).catch(() => {
      // Analytics no debe bloquear el flujo de pedido.
    });
  }, [data.slug, sessionId]);

  const filteredSections = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();

    const searchFiltered = !normalizedQuery
      ? data.sections
      : data.sections
          .map((section) => ({
            ...section,
            items: section.items.filter((item) =>
              item.name.toLowerCase().includes(normalizedQuery) ||
              item.description?.toLowerCase().includes(normalizedQuery)
            ),
          }))
          .filter((section) => section.items.length > 0);

    if (!isCategoryFilterEnabled || !selectedCategoryId) {
      return searchFiltered;
    }

    return searchFiltered.filter((section) => section.id === selectedCategoryId);
  }, [data.sections, isCategoryFilterEnabled, searchQuery, selectedCategoryId]);

  const handleItemClick = (itemId: string) => {
    const item = data.sections
      .flatMap(s => s.items)
      .find(i => i.id === itemId);
    if (item) {
      setSelectedItem(item);
    }
  };

  const handleQuickAdd = async (itemId: string, name: string, price: string) => {
    if (!sessionId) return;
    try {
      await addItem(qrToken, itemId, name, price, 1);
    } catch (e) {
      console.error('Failed to add item:', e);
    }
  };

  const scrollToSection = (sectionId: string) => {
    setActiveSection(sectionId);
    const element = document.getElementById(`section-${sectionId}`);
    if (element) {
      const header = document.querySelector('header');
      const headerHeight = header?.getBoundingClientRect().height || 160;
      const elementPosition = element.getBoundingClientRect().top + window.scrollY;
      const offsetPosition = elementPosition - headerHeight - 10;
      window.scrollTo({ top: offsetPosition, behavior: 'smooth' });
    }
  };

  const handleCategoryAction = (sectionId: string) => {
    if (isCategoryFilterEnabled) {
      setSelectedCategoryId((prev) => (prev === sectionId ? null : sectionId));
      return;
    }
    scrollToSection(sectionId);
  };

  if (isJoining || joinMutation.isPending) {
    return (
      <div className="flex min-h-screen items-center justify-center" style={{ backgroundColor: data.theme.backgroundColor }}>
        <div className="flex flex-col items-center gap-3">
          <div 
            className="h-8 w-8 animate-spin rounded-full border-3 border-t-transparent"
            style={{ borderColor: `${data.theme.primaryColor}30`, borderTopColor: data.theme.primaryColor }}
          />
          <p className="text-sm" style={{ color: data.theme.textColor }}>Joining table...</p>
        </div>
      </div>
    );
  }

  return (
    <div 
      className="min-h-screen"
      style={{ 
        backgroundColor: data.theme.backgroundColor,
        fontFamily: `${data.theme.fontFamily}, system-ui, sans-serif`,
      }}
    >
      {/* Gradient Header (when showGradientHeader is true) */}
      {data.theme.showGradientHeader && (
        <Header 
          restaurantName={data.restaurantName}
          tableNumber={data.tableNumber}
          logoUrl={data.theme.logoUrl}
        />
      )}

      {/* Sticky Navigation Header */}
      <header 
        className="sticky top-0 z-50 border-b"
        style={{ 
          backgroundColor: data.theme.cardBackground,
          borderColor: `${data.theme.textColor}10`,
        }}
      >
        {/* Simple header info (when gradient header is not shown) */}
        {!data.theme.showGradientHeader && (
          <div className="mx-auto max-w-3xl px-4 py-3">
            <div className="flex items-center justify-between">
              <div>
                <h1 
                  className="text-lg font-bold"
                  style={{ color: data.theme.textColor }}
                >
                  {data.restaurantName}
                </h1>
                <p 
                  className="text-sm"
                  style={{ color: `${data.theme.textColor}60` }}
                >
                  Table {data.tableNumber}
                </p>
              </div>
              {data.theme.logoUrl && (
                <img 
                  src={data.theme.logoUrl} 
                  alt={data.restaurantName}
                  className="h-10 w-10 rounded-full object-cover"
                />
              )}
            </div>
          </div>
        )}

        {/* Search Bar */}
        <div className="mx-auto max-w-3xl px-4 pb-3 pt-3">
          <div className="relative">
            <svg 
              className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2" 
              fill="none" 
              stroke={`${data.theme.textColor}40`}
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search menu..."
              className="w-full rounded-xl py-2.5 pl-10 pr-4 text-sm outline-none transition-all"
              style={{ 
                backgroundColor: `${data.theme.textColor}08`,
                color: data.theme.textColor,
              }}
            />
          </div>
          <label className="mt-3 inline-flex items-center gap-2 text-xs" style={{ color: `${data.theme.textColor}80` }}>
            <input
              type="checkbox"
              checked={isCategoryFilterEnabled}
              onChange={(e) => {
                setIsCategoryFilterEnabled(e.target.checked);
                if (!e.target.checked) {
                  setSelectedCategoryId(null);
                }
              }}
              className="h-4 w-4"
            />
            Filter by category
          </label>
        </div>

        {/* Category Tabs */}
        {!searchQuery && filteredSections.length > 1 && (
          <div className="scrollbar-hide overflow-x-auto pb-2">
            <div className="mx-auto flex max-w-3xl gap-1.5 px-4">
              {filteredSections.map((section) => (
                <button
                  key={section.id}
                  onClick={() => handleCategoryAction(section.id)}
                  className="shrink-0 whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition-all"
                  style={{ 
                    backgroundColor:
                      (isCategoryFilterEnabled ? selectedCategoryId === section.id : activeSection === section.id)
                        ? data.theme.primaryColor
                        : `${data.theme.textColor}08`,
                    color:
                      (isCategoryFilterEnabled ? selectedCategoryId === section.id : activeSection === section.id)
                        ? '#fff'
                        : `${data.theme.textColor}80`,
                  }}
                >
                  {section.name}
                </button>
              ))}
            </div>
          </div>
        )}
      </header>

      {/* Menu Content */}
      <main className="pb-32 pt-4">
        {filteredSections.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 px-4">
            <svg 
              className="h-16 w-16" 
              fill="none" 
              stroke={`${data.theme.textColor}30`} 
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <h3 
              className="mt-4 text-lg font-semibold"
              style={{ color: data.theme.textColor }}
            >
              No items found
            </h3>
            <p 
              className="mt-1 text-sm text-center"
              style={{ color: `${data.theme.textColor}60` }}
            >
              Try searching for something else
            </p>
            <button
              onClick={() => {
                setSearchQuery('');
                setSelectedCategoryId(null);
              }}
              className="mt-4 rounded-full px-6 py-2 text-sm font-medium"
              style={{ 
                backgroundColor: data.theme.primaryColor,
                color: '#fff',
              }}
            >
              Clear filters
            </button>
          </div>
        ) : (
          filteredSections.map((section) => (
            <MenuSection 
              key={section.id} 
              section={section} 
              onItemClick={handleItemClick}
              onQuickAdd={sessionId ? handleQuickAdd : undefined}
            />
          ))
        )}
      </main>

      {/* Cart Button */}
      <CartButton onClick={() => setShowCart(true)} />

      {/* Item Modal */}
      {selectedItem && (
        <ItemModal 
          item={selectedItem} 
          qrToken={qrToken}
          onClose={() => setSelectedItem(null)} 
        />
      )}

      {/* Cart Drawer */}
      {showCart && (
        <CartDrawer 
          qrToken={qrToken}
          slug={data.slug}
          menuItems={data.sections.flatMap(s => s.items)}
          onClose={() => setShowCart(false)} 
        />
      )}
    </div>
  );
}

export function TableMenu() {
  const { qrToken } = useParams<{ qrToken: string }>();

  const { data, isLoading, error } = useQuery({
    queryKey: ['table-menu', qrToken],
    queryFn: () => menuApi.getTableMenu(qrToken!),
    enabled: !!qrToken,
  });

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="flex flex-col items-center gap-4">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-gray-300 border-t-gray-900" />
          <p className="text-sm font-medium text-gray-600">Loading menu...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 p-6">
        <div className="max-w-sm text-center">
          <h1 className="text-2xl font-bold text-gray-900">Table Not Found</h1>
          <p className="mt-3 text-sm text-gray-600">
            This QR code may be invalid or the table is currently inactive. Please ask a staff member for assistance.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="mt-6 rounded-lg bg-gray-900 px-8 py-3 text-sm font-semibold text-white transition-colors hover:bg-gray-800"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <ThemeProvider theme={data.theme}>
      <ToastProvider>
        <OrderProvider>
          <TableMenuContent data={data} qrToken={qrToken!} />
        </OrderProvider>
      </ToastProvider>
    </ThemeProvider>
  );
}
