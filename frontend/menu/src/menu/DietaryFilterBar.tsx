import * as Toggle from '@radix-ui/react-toggle';
import { cn } from '@/lib/utils';
import type { DietaryTag } from '@/shared/types';

interface DietaryFilterBarProps {
  activeFilters: DietaryTag[];
  onFilterChange: (filter: DietaryTag) => void;
  onClearAll: () => void;
}

const filters: { value: DietaryTag; label: string; abbr: string }[] = [
  { value: 'VEGAN', label: 'Vegan', abbr: 'V' },
  { value: 'VEGETARIAN', label: 'Vegetarian', abbr: 'VG' },
  { value: 'GLUTEN_FREE', label: 'Gluten-free', abbr: 'GF' },
  { value: 'DAIRY_FREE', label: 'Dairy-free', abbr: 'DF' },
];

export function DietaryFilterBar({ activeFilters, onFilterChange, onClearAll }: DietaryFilterBarProps) {
  return (
    <div className="mb-6 -mx-4 px-4 overflow-x-auto scrollbar-hide">
      <div className="flex gap-2 pb-2">
        <button
          onClick={onClearAll}
          className={cn(
            'flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-colors',
            activeFilters.length === 0
              ? 'bg-primary text-primary-foreground'
              : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
          )}
        >
          All
        </button>
        {filters.map((filter) => (
          <Toggle.Root
            key={filter.value}
            pressed={activeFilters.includes(filter.value)}
            onPressedChange={() => onFilterChange(filter.value)}
            className={cn(
              'flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-colors',
              activeFilters.includes(filter.value)
                ? 'bg-primary text-primary-foreground'
                : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
            )}
          >
            {filter.label}
          </Toggle.Root>
        ))}
      </div>
    </div>
  );
}
