import type { SalesPeriod } from '@/shared/types';

export const SALES_PERIOD_OPTIONS: { value: SalesPeriod; label: string }[] = [
  { value: 'today', label: 'Hoy' },
  { value: '30d', label: '30 días' },
  { value: 'all', label: 'Histórico' },
];

export function salesPeriodLabel(period: SalesPeriod): string {
  return SALES_PERIOD_OPTIONS.find((o) => o.value === period)?.label ?? 'Hoy';
}
