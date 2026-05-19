import type { MenuSection as MenuSectionType } from '@/shared/types';
import { MenuItemCard } from './MenuItemCard';
import { useTheme } from '@/lib/ThemeContext';

interface MenuSectionProps {
  section: MenuSectionType;
  onItemClick: (itemId: string) => void;
  onQuickAdd?: (itemId: string, name: string, price: string) => void;
}

export function MenuSection({ section, onItemClick, onQuickAdd }: MenuSectionProps) {
  const { theme } = useTheme();

  return (
    <section id={`section-${section.id}`} className="mb-8">
      {/* Section Header */}
      <div className="mx-auto max-w-3xl px-4 py-4">
        <h2 
          className="text-lg font-bold"
          style={{ color: theme.textColor }}
        >
          {section.name}
        </h2>
      </div>

      {/* Items List */}
      <div className="mx-auto max-w-3xl px-4">
        <div 
          className="rounded-2xl"
          style={{ backgroundColor: theme.cardBackground }}
        >
          {section.items.map((item, index) => (
            <div key={item.id} className="relative">
              {index > 0 && (
                <div 
                  className="mx-3 border-t"
                  style={{ borderColor: `${theme.textColor}08` }}
                />
              )}
              <MenuItemCard 
                item={item} 
                onClick={() => onItemClick(item.id)}
                onQuickAdd={onQuickAdd ? () => onQuickAdd(item.id, item.name, item.price) : undefined}
              />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
