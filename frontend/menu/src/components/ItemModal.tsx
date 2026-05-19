import { useState } from 'react';
import { Leaf, Wheat, Milk, Salad } from 'lucide-react';
import type { MenuItem, Modifier } from '@/shared/types';
import { useTheme } from '@/lib/ThemeContext';
import { useOrder } from '@/lib/OrderContext';
import { formatCurrency, getGuestName, setGuestName } from '@/lib/utils';
import { imageSrc } from '@/shared/lib/imageUrl';

interface ItemModalProps {
  item: MenuItem;
  qrToken: string;
  onClose: () => void;
}

const dietaryInfo: Record<string, { label: string; color: string; Icon: typeof Leaf }> = {
  VEGAN: { label: 'Vegan', color: '#22c55e', Icon: Leaf },
  VEGETARIAN: { label: 'Vegetarian', color: '#84cc16', Icon: Salad },
  GLUTEN_FREE: { label: 'Gluten Free', color: '#eab308', Icon: Wheat },
  DAIRY_FREE: { label: 'Dairy Free', color: '#06b6d4', Icon: Milk },
};

const modifierTypeLabels: Record<string, string> = {
  EXTRA: 'Extras',
  REMOVAL: 'Remove',
  SUBSTITUTION: 'Substitutions',
  SIZE: 'Size',
};

export function ItemModal({ item, qrToken, onClose }: ItemModalProps) {
  const { theme } = useTheme();
  const { addItem, isLoading } = useOrder();
  const [quantity, setQuantity] = useState(1);
  const [notes, setNotes] = useState('');
  const [guestName, setGuestNameState] = useState(getGuestName());
  const [selectedModifiers, setSelectedModifiers] = useState<Set<string>>(new Set());

  const handleAdd = async () => {
    if (guestName) {
      setGuestName(guestName);
    }
    await addItem(
      qrToken, 
      item.id, 
      item.name, 
      item.price, 
      quantity, 
      notes || undefined, 
      guestName || undefined,
      Array.from(selectedModifiers)
    );
    onClose();
  };

  const toggleModifier = (modifierId: string) => {
    setSelectedModifiers(prev => {
      const next = new Set(prev);
      if (next.has(modifierId)) {
        next.delete(modifierId);
      } else {
        next.add(modifierId);
      }
      return next;
    });
  };

  const calculateTotal = () => {
    let total = parseFloat(item.price) * quantity;
    if (item.modifiers) {
      item.modifiers.forEach(mod => {
        if (selectedModifiers.has(mod.id)) {
          total += parseFloat(mod.priceAdjustment) * quantity;
        }
      });
    }
    return total;
  };

  const groupedModifiers = item.modifiers?.reduce<Record<string, Modifier[]>>((acc, mod) => {
    if (!acc[mod.modifierType]) {
      acc[mod.modifierType] = [];
    }
    acc[mod.modifierType].push(mod);
    return acc;
  }, {}) || {};

  const hasModifiers = item.modifiers && item.modifiers.length > 0;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4"
      onClick={onClose}
    >
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black/70 backdrop-blur-sm transition-opacity"
        aria-hidden="true"
      />
      
      {/* Modal */}
      <div 
        className="relative flex w-full max-w-lg flex-col overflow-hidden rounded-t-3xl sm:rounded-3xl"
        style={{ 
          backgroundColor: theme.cardBackground,
          maxHeight: '90vh',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Drag indicator for mobile - tap to close hint */}
        <div className="flex justify-center pt-3 pb-1">
          <div 
            className="h-1 w-10 rounded-full"
            style={{ backgroundColor: `${theme.textColor}20` }}
          />
        </div>
        
        {/* Image */}
        {item.imageUrl && (
          <div className="relative h-44 w-full flex-shrink-0 sm:h-52">
            <img
              src={imageSrc(item.imageUrl)}
              alt={item.name}
              className="h-full w-full object-cover"
            />
            <div 
              className="pointer-events-none absolute inset-0"
              style={{ 
                background: `linear-gradient(180deg, rgba(0,0,0,0) 60%, ${theme.cardBackground})`,
              }}
            />
          </div>
        )}
        
        <div 
          className="overflow-y-auto p-5 sm:p-6" 
          style={{ maxHeight: item.imageUrl ? '55vh' : '80vh' }}
        >
          {/* Header */}
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 
                className="text-xl font-bold sm:text-2xl"
                style={{ color: theme.textColor }}
              >
                {item.name}
              </h2>
              {item.dietaryTags.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {item.dietaryTags.map((tag) => {
                    const info = dietaryInfo[tag];
                    if (!info) return null;
                    const IconComponent = info.Icon;
                    return (
                      <span
                        key={tag}
                        className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium"
                        style={{ 
                          backgroundColor: `${info.color}12`,
                          color: info.color,
                        }}
                      >
                        <IconComponent className="h-3.5 w-3.5" />
                        {info.label}
                      </span>
                    );
                  })}
                </div>
              )}
            </div>
            <span 
              className="text-xl font-bold sm:text-2xl"
              style={{ color: theme.primaryColor }}
            >
              {formatCurrency(item.price)}
            </span>
          </div>
          
          {item.description && (
            <p 
              className="mt-3 text-sm leading-relaxed"
              style={{ color: `${theme.textColor}80` }}
            >
              {item.description}
            </p>
          )}

          {/* Modifiers */}
          {hasModifiers && (
            <div className="mt-5 space-y-4">
              {Object.entries(groupedModifiers).map(([type, modifiers]) => (
                <div key={type}>
                  <h3 
                    className="mb-2 text-xs font-semibold uppercase tracking-wider"
                    style={{ color: `${theme.textColor}60` }}
                  >
                    {modifierTypeLabels[type] || type}
                  </h3>
                  <div className="space-y-1.5">
                    {modifiers.map((mod) => {
                      const isSelected = selectedModifiers.has(mod.id);
                      const priceNum = parseFloat(mod.priceAdjustment);
                      return (
                        <button
                          key={mod.id}
                          type="button"
                          onClick={() => toggleModifier(mod.id)}
                          className="flex w-full items-center justify-between rounded-xl p-3 text-left transition-all"
                          style={{
                            backgroundColor: isSelected ? `${theme.primaryColor}12` : `${theme.textColor}05`,
                          }}
                        >
                          <div className="flex items-center gap-3">
                            <div 
                              className="flex h-5 w-5 items-center justify-center rounded-full transition-all"
                              style={{
                                backgroundColor: isSelected ? theme.primaryColor : `${theme.textColor}15`,
                              }}
                            >
                              {isSelected && (
                                <svg className="h-3 w-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                </svg>
                              )}
                            </div>
                            <span 
                              className="text-sm font-medium"
                              style={{ color: theme.textColor }}
                            >
                              {mod.name}
                            </span>
                          </div>
                          {priceNum !== 0 && (
                            <span 
                              className="text-sm font-semibold"
                              style={{ color: priceNum > 0 ? theme.primaryColor : '#22c55e' }}
                            >
                              {priceNum > 0 ? '+' : ''}{formatCurrency(mod.priceAdjustment)}
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Form Fields */}
          <div className="mt-5 space-y-4">
            <div>
              <label 
                className="mb-2 block text-xs font-semibold uppercase tracking-wider"
                style={{ color: `${theme.textColor}60` }}
              >
                Your Name
              </label>
              <input
                type="text"
                value={guestName}
                onChange={(e) => setGuestNameState(e.target.value)}
                placeholder="So your table knows who ordered"
                className="w-full rounded-xl border-2 px-4 py-3 text-sm outline-none transition-all focus:border-current"
                style={{ 
                  borderColor: `${theme.textColor}15`,
                  backgroundColor: `${theme.backgroundColor}`,
                  color: theme.textColor,
                }}
              />
            </div>

            <div>
              <label 
                className="mb-2 block text-xs font-semibold uppercase tracking-wider"
                style={{ color: `${theme.textColor}60` }}
              >
                Special Instructions
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Allergies, preferences, modifications..."
                rows={2}
                className="w-full resize-none rounded-xl border-2 px-4 py-3 text-sm outline-none transition-all focus:border-current"
                style={{ 
                  borderColor: `${theme.textColor}15`,
                  backgroundColor: `${theme.backgroundColor}`,
                  color: theme.textColor,
                }}
              />
            </div>
          </div>

          {/* Quantity & Add Button */}
          <div className="mt-5 flex items-center gap-4">
            <div 
              className="flex items-center gap-1 rounded-2xl p-1"
              style={{ backgroundColor: `${theme.textColor}08` }}
            >
              <button
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
                className="flex h-11 w-11 items-center justify-center rounded-xl text-xl font-bold transition-colors hover:bg-white"
                style={{ color: theme.primaryColor }}
              >
                -
              </button>
              <span 
                className="min-w-[3ch] text-center text-xl font-bold"
                style={{ color: theme.textColor }}
              >
                {quantity}
              </span>
              <button
                onClick={() => setQuantity(quantity + 1)}
                className="flex h-11 w-11 items-center justify-center rounded-xl text-xl font-bold transition-colors hover:bg-white"
                style={{ color: theme.primaryColor }}
              >
                +
              </button>
            </div>

            <button
              onClick={handleAdd}
              disabled={isLoading}
              className="flex flex-1 items-center justify-center gap-2 rounded-2xl py-4 text-base font-bold text-white shadow-lg transition-all hover:shadow-xl active:scale-[0.98] disabled:opacity-50"
              style={{ 
                background: `linear-gradient(135deg, ${theme.gradientStart}, ${theme.gradientEnd})`,
                boxShadow: `0 4px 20px ${theme.primaryColor}40`,
              }}
            >
              {isLoading ? (
                <span className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
              ) : (
                <>
                  <span>Add to Order</span>
                  <span className="mx-1">|</span>
                  <span>{formatCurrency(calculateTotal())}</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
