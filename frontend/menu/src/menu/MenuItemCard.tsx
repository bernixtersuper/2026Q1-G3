import { useState } from 'react';
import { menuApi } from '@/shared/api/menuApi';
import { formatCurrency, getSessionId, cn } from '@/lib/utils';
import { UtensilsCrossed } from 'lucide-react';
import type { MenuItem, DietaryTag } from '@/shared/types';
import { imageSrc } from '@/shared/lib/imageUrl';

interface MenuItemCardProps {
  item: MenuItem;
  slug: string;
}

const tagColors: Record<DietaryTag, string> = {
  VEGAN: 'bg-teal-100 text-teal-800',
  VEGETARIAN: 'bg-green-100 text-green-800',
  GLUTEN_FREE: 'bg-amber-100 text-amber-800',
  DAIRY_FREE: 'bg-purple-100 text-purple-800',
};

const tagLabels: Record<DietaryTag, string> = {
  VEGAN: 'Vegan',
  VEGETARIAN: 'Vegetarian',
  GLUTEN_FREE: 'GF',
  DAIRY_FREE: 'DF',
};

export function MenuItemCard({ item, slug }: MenuItemCardProps) {
  const [imageError, setImageError] = useState(false);
  const [imageLoading, setImageLoading] = useState(true);

  const handleClick = () => {
    const sessionId = getSessionId();
    menuApi.recordEvent(slug, {
      eventType: 'ITEM_VIEW',
      itemId: item.id,
      sessionId,
    });
  };

  return (
    <div
      onClick={handleClick}
      className="bg-card rounded-lg border overflow-hidden cursor-pointer transition-shadow hover:shadow-md active:scale-[0.98]"
    >
      <div className="aspect-video relative bg-muted">
        {item.imageUrl && !imageError ? (
          <>
            {imageLoading && (
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
              </div>
            )}
            <img
              src={imageSrc(item.imageUrl)}
              alt={item.name}
              className={cn(
                'w-full h-full object-cover transition-opacity',
                imageLoading ? 'opacity-0' : 'opacity-100'
              )}
              onLoad={() => setImageLoading(false)}
              onError={() => {
                setImageError(true);
                setImageLoading(false);
              }}
            />
          </>
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <UtensilsCrossed className="h-12 w-12 text-muted-foreground/50" />
          </div>
        )}
      </div>
      <div className="p-4">
        <div className="flex items-start justify-between gap-2 mb-2">
          <h3 className="font-semibold text-foreground leading-tight">{item.name}</h3>
          <span className="font-bold text-primary whitespace-nowrap">
            {formatCurrency(item.price)}
          </span>
        </div>
        {item.description && (
          <p className="text-sm text-muted-foreground line-clamp-2 mb-3">
            {item.description}
          </p>
        )}
        {item.dietaryTags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {item.dietaryTags.map((tag) => {
              const dietaryTag = tag as DietaryTag;
              return (
                <span
                  key={tag}
                  className={cn(
                    'px-2 py-0.5 rounded-full text-xs font-medium',
                    tagColors[dietaryTag]
                  )}
                >
                  {tagLabels[dietaryTag]}
                </span>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
