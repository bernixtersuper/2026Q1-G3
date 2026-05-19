import { useMutation, useQueryClient } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { Switch } from '@/components/ui/switch';

interface AvailabilityToggleProps {
  itemId: string;
  available: boolean;
}

export function AvailabilityToggle({ itemId, available }: AvailabilityToggleProps) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (newAvailable: boolean) => menuApi.toggleAvailability(itemId, newAvailable),
    onMutate: async (newAvailable) => {
      await queryClient.cancelQueries({ queryKey: ['menu'] });
      const previousMenu = queryClient.getQueryData(['menu']);
      
      queryClient.setQueryData(['menu'], (old: unknown) => {
        if (!old || typeof old !== 'object') return old;
        const menu = old as { sections: Array<{ items: Array<{ id: string; available: boolean }> }> };
        return {
          ...menu,
          sections: menu.sections.map((section) => ({
            ...section,
            items: section.items.map((item) =>
              item.id === itemId ? { ...item, available: newAvailable } : item
            ),
          })),
        };
      });

      return { previousMenu };
    },
    onError: (_err, _newAvailable, context) => {
      if (context?.previousMenu) {
        queryClient.setQueryData(['menu'], context.previousMenu);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] });
    },
  });

  return (
    <Switch
      checked={available}
      onCheckedChange={(checked) => mutation.mutate(checked)}
      disabled={mutation.isPending}
    />
  );
}
