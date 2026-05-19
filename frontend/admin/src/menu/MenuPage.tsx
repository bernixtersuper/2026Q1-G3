import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { SectionList } from './SectionList';
import { ItemTable } from './ItemTable';
import { MenuItemForm } from './MenuItemForm';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { Plus } from 'lucide-react';
import { toast } from '@/hooks/use-toast';
import type { MenuItem } from '@/shared/types';

export function MenuPage() {
  const queryClient = useQueryClient();
  const [selectedSection, setSelectedSection] = useState<string | null>(null);
  const [editingItem, setEditingItem] = useState<MenuItem | null>(null);
  const [isItemFormOpen, setIsItemFormOpen] = useState(false);
  const [newSectionName, setNewSectionName] = useState('');
  const [isSectionDialogOpen, setIsSectionDialogOpen] = useState(false);

  const { data: menu, isLoading } = useQuery({
    queryKey: ['menu'],
    queryFn: menuApi.getMenu,
  });

  const createSectionMutation = useMutation({
    mutationFn: menuApi.createSection,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] });
      setNewSectionName('');
      setIsSectionDialogOpen(false);
      toast({ title: 'Section Created', description: 'New menu section has been added', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to create section', variant: 'destructive' }),
  });

  const deleteSectionMutation = useMutation({
    mutationFn: menuApi.deleteSection,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] });
      if (selectedSection) {
        setSelectedSection(null);
      }
      toast({ title: 'Section Deleted', description: 'Menu section has been removed', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to delete section', variant: 'destructive' }),
  });

  const handleCreateSection = () => {
    if (!newSectionName.trim()) return;
    const maxOrder = Math.max(0, ...(menu?.sections.map((s) => s.displayOrder) || []));
    createSectionMutation.mutate({
      name: newSectionName,
      displayOrder: maxOrder + 1,
    });
  };

  const handleDeleteSection = (id: string) => {
    if (confirm('Delete this section and all its items?')) {
      deleteSectionMutation.mutate(id);
    }
  };

  const handleEditItem = (item: MenuItem) => {
    setEditingItem(item);
    setIsItemFormOpen(true);
  };

  const handleCreateItem = () => {
    setEditingItem(null);
    setIsItemFormOpen(true);
  };

  const handleItemFormClose = () => {
    setIsItemFormOpen(false);
    setEditingItem(null);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-3 gap-6">
          <Skeleton className="h-96" />
          <Skeleton className="h-96 col-span-2" />
        </div>
      </div>
    );
  }

  const currentSection = selectedSection
    ? menu?.sections.find((s) => s.id === selectedSection)
    : menu?.sections[0];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">{menu?.restaurantName}</h1>
          <p className="text-muted-foreground">menu/{menu?.slug}</p>
        </div>
        <Button onClick={handleCreateItem} disabled={!menu?.sections.length}>
          <Plus className="h-4 w-4 mr-2" />
          Add Item
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
            <CardTitle className="text-lg">Sections</CardTitle>
            <Dialog open={isSectionDialogOpen} onOpenChange={setIsSectionDialogOpen}>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline">
                  <Plus className="h-4 w-4" />
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Add Section</DialogTitle>
                </DialogHeader>
                <div className="space-y-4 pt-4">
                  <Input
                    placeholder="Section name (e.g., Appetizers)"
                    value={newSectionName}
                    onChange={(e) => setNewSectionName(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleCreateSection()}
                  />
                  <Button onClick={handleCreateSection} className="w-full">
                    Create Section
                  </Button>
                </div>
              </DialogContent>
            </Dialog>
          </CardHeader>
          <CardContent>
            <SectionList
              sections={menu?.sections || []}
              selectedId={currentSection?.id || null}
              onSelect={setSelectedSection}
              onDelete={handleDeleteSection}
            />
          </CardContent>
        </Card>

        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>{currentSection?.name || 'Select a section'}</CardTitle>
            </CardHeader>
            <CardContent>
              {currentSection ? (
                <ItemTable
                  items={currentSection.items}
                  onEdit={handleEditItem}
                />
              ) : (
                <p className="text-muted-foreground text-center py-8">
                  {menu?.sections.length
                    ? 'Select a section to view items'
                    : 'Create a section to get started'}
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      <MenuItemForm
        open={isItemFormOpen}
        onClose={handleItemFormClose}
        item={editingItem}
        sections={menu?.sections || []}
        defaultSectionId={currentSection?.id}
      />
    </div>
  );
}
