import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { DietaryFilterBar } from './DietaryFilterBar';
import { MenuSectionList } from './MenuSectionList';
import { getSessionId } from '@/lib/utils';
import { UtensilsCrossed } from 'lucide-react';
import type { DietaryTag, MenuSection, MenuItem } from '@/shared/types';

type SortOption = 'DEFAULT' | 'NAME_ASC' | 'NAME_DESC' | 'PRICE_ASC' | 'PRICE_DESC';

const sortOptions: Array<{ value: SortOption; label: string }> = [
  { value: 'DEFAULT', label: 'Recommended' },
  { value: 'NAME_ASC', label: 'Name (A-Z)' },
  { value: 'NAME_DESC', label: 'Name (Z-A)' },
  { value: 'PRICE_ASC', label: 'Price (Low to High)' },
  { value: 'PRICE_DESC', label: 'Price (High to Low)' },
];

function SkeletonLoader() {
  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto px-4 py-6">
        <div className="h-10 w-48 bg-muted rounded animate-pulse mb-6" />
        <div className="flex gap-2 mb-6 overflow-x-auto">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-10 w-24 bg-muted rounded-full animate-pulse flex-shrink-0" />
          ))}
        </div>
        {[...Array(2)].map((_, i) => (
          <div key={i} className="mb-8">
            <div className="h-6 w-32 bg-muted rounded animate-pulse mb-4" />
            <div className="grid grid-cols-1 gap-4">
              {[...Array(3)].map((_, j) => (
                <div key={j} className="bg-card rounded-lg border overflow-hidden">
                  <div className="aspect-video bg-muted animate-pulse" />
                  <div className="p-4 space-y-2">
                    <div className="h-5 w-32 bg-muted rounded animate-pulse" />
                    <div className="h-4 w-20 bg-muted rounded animate-pulse" />
                    <div className="h-4 w-full bg-muted rounded animate-pulse" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function parsePrice(price: string): number {
  const parsed = Number.parseFloat(price);
  return Number.isFinite(parsed) ? parsed : 0;
}

function sortItems(items: MenuItem[], sortBy: SortOption): MenuItem[] {
  const sortedItems = [...items];

  switch (sortBy) {
    case 'NAME_ASC':
      return sortedItems.sort((a, b) => a.name.localeCompare(b.name));
    case 'NAME_DESC':
      return sortedItems.sort((a, b) => b.name.localeCompare(a.name));
    case 'PRICE_ASC':
      return sortedItems.sort((a, b) => parsePrice(a.price) - parsePrice(b.price));
    case 'PRICE_DESC':
      return sortedItems.sort((a, b) => parsePrice(b.price) - parsePrice(a.price));
    case 'DEFAULT':
    default:
      return sortedItems.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  }
}

export function PublicMenuPage() {
  const { slug } = useParams<{ slug: string }>();
  const [activeFilters, setActiveFilters] = useState<DietaryTag[]>([]);
  const [sortBy, setSortBy] = useState<SortOption>('DEFAULT');
  const [priceMin, setPriceMin] = useState<number | null>(null);
  const [priceMax, setPriceMax] = useState<number | null>(null);
  const [availableOnly, setAvailableOnly] = useState(false);

  const { data: menu, isLoading, error } = useQuery({
    queryKey: ['menu', slug],
    queryFn: () => menuApi.getMenu(slug!),
    enabled: !!slug,
  });

  useEffect(() => {
    if (menu && slug) {
      const sessionId = getSessionId();
      menuApi.recordEvent(slug, {
        eventType: 'MENU_VIEW',
        sessionId,
      });
    }
  }, [menu, slug]);

  const handleFilterChange = (filter: DietaryTag) => {
    setActiveFilters((prev) => {
      const newFilters = prev.includes(filter)
        ? prev.filter((f) => f !== filter)
        : [...prev, filter];
      
      if (!prev.includes(filter) && slug) {
        const sessionId = getSessionId();
        menuApi.recordEvent(slug, {
          eventType: 'FILTER_USED',
          sessionId,
          metadata: { filter },
        });
      }
      
      return newFilters;
    });
  };

  const clearFilters = () => {
    setActiveFilters([]);
    setPriceMin(null);
    setPriceMax(null);
    setAvailableOnly(false);
  };

  const priceBounds = useMemo(() => {
    if (!menu) return { min: 0, max: 0 };

    const prices = menu.sections.flatMap((section) =>
      section.items.map((item) => parsePrice(item.price))
    );

    if (prices.length === 0) return { min: 0, max: 0 };

    return {
      min: Math.min(...prices),
      max: Math.max(...prices),
    };
  }, [menu]);

  const filteredSections: MenuSection[] = useMemo(() => {
    if (!menu) return [];

    const effectiveMin = priceMin ?? priceBounds.min;
    const effectiveMax = priceMax ?? priceBounds.max;

    const sections = menu.sections
      .map((section: MenuSection) => ({
        ...section,
        items: section.items.filter((item: MenuItem) => {
          const itemPrice = parsePrice(item.price);
          const matchesDietary =
            activeFilters.length === 0 ||
            activeFilters.every((filter) => item.dietaryTags.includes(filter));
          const matchesPrice = itemPrice >= effectiveMin && itemPrice <= effectiveMax;
          const matchesAvailability = !availableOnly || item.available !== false;

          return matchesDietary && matchesPrice && matchesAvailability;
        }),
      }))
      .filter((section: MenuSection) => section.items.length > 0);

    return sections.map((section) => ({
      ...section,
      items: sortItems(section.items, sortBy),
    }));
  }, [activeFilters, availableOnly, menu, priceBounds.max, priceBounds.min, priceMax, priceMin, sortBy]);

  if (isLoading) {
    return <SkeletonLoader />;
  }

  if (error || !menu) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center px-4">
        <div className="text-center">
          <UtensilsCrossed className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
          <h1 className="text-2xl font-bold mb-2">Menu Not Available</h1>
          <p className="text-muted-foreground">
            This menu could not be found or is temporarily unavailable.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto px-4 py-6">
        <header className="mb-6">
          <h1 className="text-3xl font-bold text-foreground">{menu.restaurantName}</h1>
        </header>

        <DietaryFilterBar
          activeFilters={activeFilters}
          onFilterChange={handleFilterChange}
          onClearAll={clearFilters}
        />

        <div className="mt-4 mb-6">
          <label htmlFor="sort-items" className="block text-sm font-medium text-foreground mb-2">
            Sort items
          </label>
          <select
            id="sort-items"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as SortOption)}
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
          >
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="mb-6 rounded-md border border-input p-4 space-y-4">
          <div>
            <p className="text-sm font-medium text-foreground mb-2">Price range</p>
            <div className="grid grid-cols-2 gap-2">
              <input
                type="number"
                min={priceBounds.min}
                max={priceBounds.max}
                step="0.01"
                placeholder={`Min (${priceBounds.min.toFixed(2)})`}
                value={priceMin ?? ''}
                onChange={(e) => setPriceMin(e.target.value === '' ? null : Number(e.target.value))}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
              />
              <input
                type="number"
                min={priceBounds.min}
                max={priceBounds.max}
                step="0.01"
                placeholder={`Max (${priceBounds.max.toFixed(2)})`}
                value={priceMax ?? ''}
                onChange={(e) => setPriceMax(e.target.value === '' ? null : Number(e.target.value))}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
              />
            </div>
          </div>

          <label className="inline-flex items-center gap-2 text-sm text-foreground">
            <input
              type="checkbox"
              checked={availableOnly}
              onChange={(e) => setAvailableOnly(e.target.checked)}
              className="h-4 w-4"
            />
            Show only available items
          </label>
        </div>

        {filteredSections.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-muted-foreground">No items match your filters.</p>
            <button
              onClick={clearFilters}
              className="mt-2 text-primary hover:underline"
            >
              Clear filters
            </button>
          </div>
        ) : (
          <MenuSectionList sections={filteredSections} slug={slug!} />
        )}

        <footer className="mt-12 pt-6 border-t text-center text-sm text-muted-foreground">
          <p>Powered by MenuQR</p>
        </footer>
      </div>
    </div>
  );
}
