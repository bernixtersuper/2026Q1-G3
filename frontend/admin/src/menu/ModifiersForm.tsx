import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Plus, Trash2, GripVertical } from 'lucide-react';
import type { MenuItem, Modifier, ModifierType, CreateModifierRequest } from '@/shared/types';

interface ModifiersFormProps {
  open: boolean;
  onClose: () => void;
  item: MenuItem;
}

const modifierTypeLabels: Record<ModifierType, string> = {
  EXTRA: 'Extra (add-on)',
  REMOVAL: 'Removal (take out)',
  SUBSTITUTION: 'Substitution',
  SIZE: 'Size variation',
};

export function ModifiersForm({ open, onClose, item }: ModifiersFormProps) {
  const queryClient = useQueryClient();
  const [newModifier, setNewModifier] = useState<Partial<CreateModifierRequest>>({
    menuItemId: item.id,
    name: '',
    priceAdjustment: '0.00',
    modifierType: 'EXTRA',
    displayOrder: 0,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { data: modifiers, isLoading } = useQuery({
    queryKey: ['modifiers', item.id],
    queryFn: () => menuApi.getModifiers(item.id),
    enabled: open,
  });

  useEffect(() => {
    if (open) {
      setNewModifier({
        menuItemId: item.id,
        name: '',
        priceAdjustment: '0.00',
        modifierType: 'EXTRA',
        displayOrder: (modifiers?.length || 0) + 1,
      });
      setErrors({});
    }
  }, [open, item.id, modifiers?.length]);

  const createMutation = useMutation({
    mutationFn: menuApi.createModifier,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifiers', item.id] });
      setNewModifier(prev => ({
        ...prev,
        name: '',
        priceAdjustment: '0.00',
        displayOrder: (modifiers?.length || 0) + 2,
      }));
      setErrors({});
    },
    onError: (error: Error) => {
      setErrors({ submit: error.message });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: { available: boolean } & Partial<Modifier> }) =>
      menuApi.updateModifier(id, {
        name: data.name!,
        priceAdjustment: data.priceAdjustment!,
        modifierType: data.modifierType!,
        available: data.available,
        displayOrder: data.displayOrder!,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifiers', item.id] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: menuApi.deleteModifier,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifiers', item.id] });
    },
  });

  const handleAdd = () => {
    if (!newModifier.name?.trim()) {
      setErrors({ name: 'Name is required' });
      return;
    }
    if (newModifier.name.length > 100) {
      setErrors({ name: 'Name must be 100 characters or less' });
      return;
    }
    
    createMutation.mutate(newModifier as CreateModifierRequest);
  };

  const handleToggleAvailability = (modifier: Modifier) => {
    updateMutation.mutate({
      id: modifier.id,
      data: { ...modifier, available: !modifier.available },
    });
  };

  const handleDelete = (id: string, name: string) => {
    if (confirm(`Delete modifier "${name}"?`)) {
      deleteMutation.mutate(id);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Modifiers for {item.name}</DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Add new modifier */}
          <div className="rounded-lg border p-4 space-y-4">
            <h3 className="text-sm font-semibold">Add New Modifier</h3>
            
            {errors.submit && (
              <div className="p-2 text-sm text-destructive bg-destructive/10 rounded">
                {errors.submit}
              </div>
            )}
            
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Name *</Label>
                <Input
                  value={newModifier.name}
                  onChange={(e) => {
                    setNewModifier(prev => ({ ...prev, name: e.target.value }));
                    setErrors({});
                  }}
                  placeholder="e.g., Extra cheese"
                  maxLength={100}
                  className={errors.name ? 'border-destructive' : ''}
                />
                {errors.name && <p className="text-xs text-destructive">{errors.name}</p>}
              </div>
              <div className="space-y-2">
                <Label>Price Adjustment</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={newModifier.priceAdjustment}
                  onChange={(e) => setNewModifier(prev => ({ ...prev, priceAdjustment: e.target.value }))}
                  placeholder="0.00"
                />
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Type</Label>
                <Select
                  value={newModifier.modifierType}
                  onValueChange={(value) => setNewModifier(prev => ({ ...prev, modifierType: value as ModifierType }))}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(modifierTypeLabels).map(([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="flex items-end">
                <Button 
                  onClick={handleAdd} 
                  disabled={createMutation.isPending}
                  className="w-full"
                >
                  <Plus className="h-4 w-4 mr-2" />
                  {createMutation.isPending ? 'Adding...' : 'Add Modifier'}
                </Button>
              </div>
            </div>
          </div>

          {/* Existing modifiers */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold">
              Existing Modifiers ({modifiers?.length || 0})
            </h3>
            
            {isLoading ? (
              <p className="text-sm text-muted-foreground">Loading...</p>
            ) : modifiers?.length === 0 ? (
              <p className="text-sm text-muted-foreground py-4 text-center">
                No modifiers yet. Add one above.
              </p>
            ) : (
              <div className="space-y-2">
                {modifiers?.map((modifier) => (
                  <div 
                    key={modifier.id}
                    className={`flex items-center gap-3 rounded-lg border p-3 ${!modifier.available ? 'opacity-50' : ''}`}
                  >
                    <GripVertical className="h-4 w-4 text-muted-foreground cursor-grab" />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-medium truncate">{modifier.name}</span>
                        <span className="text-xs text-muted-foreground">
                          ({modifierTypeLabels[modifier.modifierType]})
                        </span>
                      </div>
                      <span className={`text-sm ${parseFloat(modifier.priceAdjustment) >= 0 ? 'text-primary' : 'text-green-600'}`}>
                        {parseFloat(modifier.priceAdjustment) > 0 ? '+' : ''}
                        ${modifier.priceAdjustment}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={modifier.available}
                        onCheckedChange={() => handleToggleAvailability(modifier)}
                      />
                      <Button
                        variant="ghost"
                        size="icon"
                        className="hover:bg-destructive/10 hover:text-destructive"
                        onClick={() => handleDelete(modifier.id, modifier.name)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex justify-end pt-4 border-t">
            <Button variant="outline" onClick={onClose}>
              Done
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
