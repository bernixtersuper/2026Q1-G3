import { useState, useEffect, useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Upload } from 'lucide-react';
import { toast } from '@/hooks/use-toast';
import type { MenuItem, MenuSection, DietaryTag, CreateItemRequest } from '@/shared/types';
import { imageSrc, normalizeImageUrlForForm } from '@/shared/lib/imageUrl';

interface MenuItemFormProps {
  open: boolean;
  onClose: () => void;
  item: MenuItem | null;
  sections: MenuSection[];
  defaultSectionId?: string;
}

const dietaryTags: { value: DietaryTag; label: string }[] = [
  { value: 'VEGAN', label: 'Vegan' },
  { value: 'VEGETARIAN', label: 'Vegetarian' },
  { value: 'GLUTEN_FREE', label: 'Gluten-free' },
  { value: 'DAIRY_FREE', label: 'Dairy-free' },
];

const MAX_NAME_LENGTH = 255;
const MAX_DESCRIPTION_LENGTH = 1000;
const MAX_PRICE = 99999999.99;

export function MenuItemForm({ open, onClose, item, sections, defaultSectionId }: MenuItemFormProps) {
  const queryClient = useQueryClient();
  const [uploading, setUploading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const prevOpenRef = useRef(false);
  const prevItemIdRef = useRef<string | null>(null);
  const [formData, setFormData] = useState<CreateItemRequest>({
    sectionId: '',
    name: '',
    description: '',
    price: '',
    imageUrl: '',
    dietaryTags: [],
    displayOrder: 0,
  });

  // Solo reiniciar al abrir el diálogo o al cambiar de ítem (no cuando `sections` se refresca tras el menú).
  useEffect(() => {
    const itemId = item?.id ?? null;
    const justOpened = open && !prevOpenRef.current;
    const itemChanged = open && itemId !== prevItemIdRef.current;

    if (!open) {
      prevOpenRef.current = false;
      prevItemIdRef.current = null;
      return;
    }

    if (justOpened || itemChanged) {
      if (item) {
        setFormData({
          sectionId: item.sectionId,
          name: item.name,
          description: item.description || '',
          price: item.price,
          imageUrl: normalizeImageUrlForForm(item.imageUrl || ''),
          dietaryTags: item.dietaryTags,
          displayOrder: item.displayOrder,
        });
      } else {
        setFormData({
          sectionId: defaultSectionId || sections[0]?.id || '',
          name: '',
          description: '',
          price: '',
          imageUrl: '',
          dietaryTags: [],
          displayOrder: 0,
        });
      }
      setErrors({});
    }

    prevOpenRef.current = open;
    prevItemIdRef.current = itemId;
  }, [open, item, defaultSectionId, sections]);

  // Si las secciones cargan después de abrir "Add Item", rellenar sectionId sin borrar la imagen subida.
  useEffect(() => {
    if (!open || item) return;
    const fallbackSectionId = defaultSectionId || sections[0]?.id || '';
    if (!fallbackSectionId) return;
    setFormData((prev) => (prev.sectionId ? prev : { ...prev, sectionId: fallbackSectionId }));
  }, [open, item, defaultSectionId, sections]);

  const createMutation = useMutation({
    mutationFn: menuApi.createItem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] });
      onClose();
      toast({ title: 'Item Created', description: 'Menu item has been added', variant: 'success' });
    },
    onError: (error: Error) => {
      setErrors({ submit: error.message || 'Failed to create item' });
      toast({ title: 'Error', description: error.message || 'Failed to create item', variant: 'destructive' });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateItemRequest }) =>
      menuApi.updateItem(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] });
      onClose();
      toast({ title: 'Item Updated', description: 'Menu item has been saved', variant: 'success' });
    },
    onError: (error: Error) => {
      setErrors({ submit: error.message || 'Failed to update item' });
      toast({ title: 'Error', description: error.message || 'Failed to update item', variant: 'destructive' });
    },
  });

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.sectionId) {
      newErrors.sectionId = 'Section is required';
    }

    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    } else if (formData.name.length > MAX_NAME_LENGTH) {
      newErrors.name = `Name must be ${MAX_NAME_LENGTH} characters or less`;
    }

    if (formData.description && formData.description.length > MAX_DESCRIPTION_LENGTH) {
      newErrors.description = `Description must be ${MAX_DESCRIPTION_LENGTH} characters or less`;
    }

    const price = parseFloat(formData.price);
    if (!formData.price || isNaN(price)) {
      newErrors.price = 'Valid price is required';
    } else if (price < 0) {
      newErrors.price = 'Price cannot be negative';
    } else if (price > MAX_PRICE) {
      newErrors.price = `Price cannot exceed ${MAX_PRICE.toLocaleString()}`;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const submitData = {
      ...formData,
      price: parseFloat(formData.price).toFixed(2),
    };

    if (item) {
      updateMutation.mutate({ id: item.id, data: submitData });
    } else {
      createMutation.mutate(submitData);
    }
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      setErrors(prev => ({ ...prev, image: 'Image must be less than 5MB' }));
      return;
    }

    setUploading(true);
    try {
      const url = await menuApi.uploadImage(file);
      setFormData((prev) => ({ ...prev, imageUrl: normalizeImageUrlForForm(url) }));
      setErrors(prev => {
        const { image, ...rest } = prev;
        return rest;
      });
    } catch (error) {
      setErrors(prev => ({ ...prev, image: 'Upload failed. Please try again.' }));
    } finally {
      setUploading(false);
    }
  };

  const toggleTag = (tag: DietaryTag) => {
    setFormData((prev) => ({
      ...prev,
      dietaryTags: prev.dietaryTags.includes(tag)
        ? prev.dietaryTags.filter((t) => t !== tag)
        : [...prev.dietaryTags, tag],
    }));
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{item ? 'Edit Item' : 'Add Item'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          {errors.submit && (
            <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-md">
              {errors.submit}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="section">Section *</Label>
            <Select
              value={formData.sectionId}
              onValueChange={(value) => {
                setFormData((prev) => ({ ...prev, sectionId: value }));
                setErrors(prev => {
                  const { sectionId, ...rest } = prev;
                  return rest;
                });
              }}
            >
              <SelectTrigger className={errors.sectionId ? 'border-destructive' : ''}>
                <SelectValue placeholder="Select a section" />
              </SelectTrigger>
              <SelectContent>
                {sections.map((section) => (
                  <SelectItem key={section.id} value={section.id}>
                    {section.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.sectionId && <p className="text-xs text-destructive">{errors.sectionId}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="name">Name * <span className="text-muted-foreground text-xs">({formData.name.length}/{MAX_NAME_LENGTH})</span></Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => {
                setFormData((prev) => ({ ...prev, name: e.target.value }));
                setErrors(prev => {
                  const { name, ...rest } = prev;
                  return rest;
                });
              }}
              maxLength={MAX_NAME_LENGTH}
              className={errors.name ? 'border-destructive' : ''}
            />
            {errors.name && <p className="text-xs text-destructive">{errors.name}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description <span className="text-muted-foreground text-xs">({formData.description.length}/{MAX_DESCRIPTION_LENGTH})</span></Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => {
                setFormData((prev) => ({ ...prev, description: e.target.value }));
                setErrors(prev => {
                  const { description, ...rest } = prev;
                  return rest;
                });
              }}
              maxLength={MAX_DESCRIPTION_LENGTH}
              rows={2}
              className={errors.description ? 'border-destructive' : ''}
            />
            {errors.description && <p className="text-xs text-destructive">{errors.description}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="price">Price *</Label>
            <Input
              id="price"
              type="number"
              step="0.01"
              min="0"
              max={MAX_PRICE}
              value={formData.price}
              onChange={(e) => {
                setFormData((prev) => ({ ...prev, price: e.target.value }));
                setErrors(prev => {
                  const { price, ...rest } = prev;
                  return rest;
                });
              }}
              className={errors.price ? 'border-destructive' : ''}
            />
            {errors.price && <p className="text-xs text-destructive">{errors.price}</p>}
          </div>

          <div className="space-y-2">
            <Label>Dietary Tags</Label>
            <div className="flex flex-wrap gap-4">
              {dietaryTags.map((tag) => (
                <div key={tag.value} className="flex items-center space-x-2">
                  <Checkbox
                    id={tag.value}
                    checked={formData.dietaryTags.includes(tag.value)}
                    onCheckedChange={() => toggleTag(tag.value)}
                  />
                  <Label htmlFor={tag.value} className="text-sm font-normal">
                    {tag.label}
                  </Label>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label>Image</Label>
            <div className="flex gap-2">
              <Input
                value={formData.imageUrl}
                onChange={(e) => setFormData((prev) => ({ ...prev, imageUrl: e.target.value }))}
                placeholder="Image URL"
                className="flex-1"
              />
              <Button type="button" variant="outline" disabled={uploading} asChild>
                <label className="cursor-pointer">
                  <Upload className="h-4 w-4 mr-2" />
                  {uploading ? 'Uploading...' : 'Upload'}
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleImageUpload}
                  />
                </label>
              </Button>
            </div>
            {errors.image && <p className="text-xs text-destructive">{errors.image}</p>}
            {formData.imageUrl && (
              <img
                src={imageSrc(formData.imageUrl)}
                alt="Preview"
                className="w-full h-32 object-cover rounded mt-2"
              />
            )}
          </div>


          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Saving...' : item ? 'Update' : 'Create'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
