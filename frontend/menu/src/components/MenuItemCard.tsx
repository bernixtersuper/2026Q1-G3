import { Leaf, Wheat, Milk, Salad } from 'lucide-react';
import type { MenuItem } from '@/shared/types';
import { useTheme } from '@/lib/ThemeContext';
import { formatCurrency } from '@/lib/utils';
import { imageSrc } from '@/shared/lib/imageUrl';

const dietaryIcons: Record<string, { Icon: typeof Leaf; color: string }> = {
  VEGAN: { Icon: Leaf, color: '#22c55e' },
  VEGETARIAN: { Icon: Salad, color: '#84cc16' },
  GLUTEN_FREE: { Icon: Wheat, color: '#eab308' },
  DAIRY_FREE: { Icon: Milk, color: '#06b6d4' },
};

interface MenuItemCardProps {
  item: MenuItem;
  onClick: () => void;
  onQuickAdd?: () => void;
}

export function MenuItemCard({ item, onClick, onQuickAdd }: MenuItemCardProps) {
  const { theme } = useTheme();

  const handleQuickAdd = (e: React.MouseEvent) => {
    e.stopPropagation();
    onQuickAdd?.();
  };

  return (
    <button
      onClick={onClick}
      className="group w-full text-left transition-all duration-200 active:scale-[0.98]"
    >
      <div 
        className="relative flex gap-3 p-3 transition-all hover:bg-black/[0.02]"
        style={{ backgroundColor: theme.cardBackground }}
      >
        {/* Content */}
        <div className="flex flex-1 flex-col justify-between min-w-0 overflow-hidden">
          <div className="min-w-0">
            <h3 
              className="font-semibold leading-tight truncate pr-1"
              style={{ color: theme.textColor }}
            >
              {item.name}
            </h3>
            
            {item.description && (
              <p 
                className="mt-1 text-sm leading-snug line-clamp-2"
                style={{ color: `${theme.textColor}70` }}
              >
                {item.description}
              </p>
            )}
          </div>

          <div className="mt-2 flex items-center gap-2">
            <span 
              className="text-base font-bold shrink-0"
              style={{ color: theme.textColor }}
            >
              {formatCurrency(item.price)}
            </span>
            
            {item.dietaryTags.length > 0 && (
              <div className="flex gap-1">
                {item.dietaryTags.slice(0, 3).map((tag) => {
                  const info = dietaryIcons[tag];
                  if (!info) return null;
                  const IconComponent = info.Icon;
                  return (
                    <span
                      key={tag}
                      className="flex h-5 w-5 items-center justify-center rounded-full shrink-0"
                      style={{ backgroundColor: `${info.color}15` }}
                      title={tag.replace('_', ' ')}
                    >
                      <IconComponent className="h-3 w-3" style={{ color: info.color }} />
                    </span>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Image + Add Button Container */}
        <div className="relative flex-shrink-0">
          {item.imageUrl ? (
            <div className="h-[72px] w-[72px] overflow-hidden rounded-xl">
              <img
                src={imageSrc(item.imageUrl)}
                alt={item.name}
                className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
              />
            </div>
          ) : (
            <div 
              className="flex h-[72px] w-[72px] items-center justify-center rounded-xl"
              style={{ backgroundColor: `${theme.primaryColor}08` }}
            >
              <svg 
                className="h-6 w-6" 
                fill="none" 
                stroke={`${theme.textColor}25`} 
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
          )}
          
          {/* Quick Add Button - positioned at bottom right corner of image */}
          {onQuickAdd && (
            <button
              onClick={handleQuickAdd}
              className="absolute -bottom-1 -right-1 z-10 flex h-7 w-7 items-center justify-center rounded-full shadow-lg transition-all active:scale-90"
              style={{ 
                backgroundColor: theme.primaryColor,
                color: '#fff',
                boxShadow: `0 2px 8px ${theme.primaryColor}50`,
              }}
            >
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
              </svg>
            </button>
          )}
        </div>
      </div>
    </button>
  );
}
