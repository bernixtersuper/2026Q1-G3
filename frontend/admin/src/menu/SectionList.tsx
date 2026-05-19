import { Trash2, GripVertical } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { MenuSection } from '@/shared/types';

interface SectionListProps {
  sections: MenuSection[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  onDelete: (id: string) => void;
}

export function SectionList({ sections, selectedId, onSelect, onDelete }: SectionListProps) {
  if (sections.length === 0) {
    return (
      <p className="text-sm text-muted-foreground text-center py-4">
        No sections yet. Create one to start adding menu items.
      </p>
    );
  }

  return (
    <div className="space-y-2">
      {sections
        .sort((a, b) => a.displayOrder - b.displayOrder)
        .map((section) => (
          <div
            key={section.id}
            className={cn(
              'flex items-center gap-2 p-3 rounded-lg cursor-pointer transition-colors',
              selectedId === section.id
                ? 'bg-primary text-primary-foreground'
                : 'hover:bg-muted'
            )}
            onClick={() => onSelect(section.id)}
          >
            <GripVertical className="h-4 w-4 opacity-50" />
            <span className="flex-1 font-medium">{section.name}</span>
            <span
              className={cn(
                'text-xs px-2 py-1 rounded-full',
                selectedId === section.id
                  ? 'bg-primary-foreground/20'
                  : 'bg-muted-foreground/10'
              )}
            >
              {section.items.length}
            </span>
            <Button
              variant="ghost"
              size="icon"
              className={cn(
                'h-8 w-8',
                selectedId === section.id
                  ? 'hover:bg-primary-foreground/20'
                  : 'hover:bg-destructive/10 hover:text-destructive'
              )}
              onClick={(e) => {
                e.stopPropagation();
                onDelete(section.id);
              }}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        ))}
    </div>
  );
}
