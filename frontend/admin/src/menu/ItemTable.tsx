import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { AvailabilityToggle } from './AvailabilityToggle';
import { ModifiersForm } from './ModifiersForm';
import { Pencil, Trash2, UtensilsCrossed, Settings2 } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';
import { toast } from '@/hooks/use-toast';
import type { MenuItem, DietaryTag } from '@/shared/types';
import { imageSrc } from '@/shared/lib/imageUrl';

interface ItemTableProps {
  items: MenuItem[];
  onEdit: (item: MenuItem) => void;
}

const tagVariants: Record<DietaryTag, 'vegan' | 'vegetarian' | 'glutenFree' | 'dairyFree'> = {
  VEGAN: 'vegan',
  VEGETARIAN: 'vegetarian',
  GLUTEN_FREE: 'glutenFree',
  DAIRY_FREE: 'dairyFree',
};

const tagLabels: Record<DietaryTag, string> = {
  VEGAN: 'Vegan',
  VEGETARIAN: 'Vegetarian',
  GLUTEN_FREE: 'Gluten-free',
  DAIRY_FREE: 'Dairy-free',
};

export function ItemTable({ items, onEdit }: ItemTableProps) {
  const queryClient = useQueryClient();
  const [modifiersItem, setModifiersItem] = useState<MenuItem | null>(null);

  const deleteMutation = useMutation({
    mutationFn: menuApi.deleteItem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] });
      toast({ title: 'Item Deleted', description: 'Menu item has been removed', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to delete item', variant: 'destructive' }),
  });

  const handleDelete = (id: string, name: string) => {
    if (confirm(`Delete "${name}"?`)) {
      deleteMutation.mutate(id);
    }
  };

  if (items.length === 0) {
    return (
      <p className="text-muted-foreground text-center py-8">
        No items in this section yet.
      </p>
    );
  }

  return (
    <>
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-16">Image</TableHead>
          <TableHead>Name</TableHead>
          <TableHead>Price</TableHead>
          <TableHead>Tags</TableHead>
          <TableHead className="w-24">Available</TableHead>
          <TableHead className="w-24">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .map((item) => (
            <TableRow key={item.id}>
              <TableCell>
                {item.imageUrl ? (
                  <img
                    src={imageSrc(item.imageUrl)}
                    alt={item.name}
                    className="w-12 h-12 object-cover rounded"
                  />
                ) : (
                  <div className="w-12 h-12 bg-muted rounded flex items-center justify-center">
                    <UtensilsCrossed className="h-5 w-5 text-muted-foreground" />
                  </div>
                )}
              </TableCell>
              <TableCell>
                <div>
                  <p className="font-medium">{item.name}</p>
                  {item.description && (
                    <p className="text-sm text-muted-foreground line-clamp-1">
                      {item.description}
                    </p>
                  )}
                </div>
              </TableCell>
              <TableCell>{formatCurrency(item.price)}</TableCell>
              <TableCell>
                <div className="flex flex-wrap gap-1">
                  {item.dietaryTags.map((tag) => (
                    <Badge key={tag} variant={tagVariants[tag]}>
                      {tagLabels[tag]}
                    </Badge>
                  ))}
                </div>
              </TableCell>
              <TableCell>
                <AvailabilityToggle itemId={item.id} available={item.available} />
              </TableCell>
              <TableCell>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => setModifiersItem(item)}
                    title="Manage modifiers"
                  >
                    <Settings2 className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onEdit(item)}
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="hover:bg-destructive/10 hover:text-destructive"
                    onClick={() => handleDelete(item.id, item.name)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
      </TableBody>
    </Table>

    {modifiersItem && (
      <ModifiersForm
        open={!!modifiersItem}
        onClose={() => setModifiersItem(null)}
        item={modifiersItem}
      />
    )}
    </>
  );
}
