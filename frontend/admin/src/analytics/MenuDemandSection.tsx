import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { analyticsApi } from '@/shared/api/analyticsApi';
import { TopSoldTable } from './TopSoldTable';
import { ViewedVsSoldMatrix } from './ViewedVsSoldMatrix';
import type { SalesPeriod } from '@/shared/types';
import { SALES_PERIOD_OPTIONS } from './salesPeriod';

export function MenuDemandSection() {
  const [period, setPeriod] = useState<SalesPeriod>('today');

  const { data: menu, isLoading } = useQuery({
    queryKey: ['analytics', 'menu', period],
    queryFn: () => analyticsApi.getMenu(period),
  });

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-muted-foreground mr-1">Ventas:</span>
        {SALES_PERIOD_OPTIONS.map((opt) => (
          <Button
            key={opt.value}
            variant={period === opt.value ? 'default' : 'outline'}
            size="sm"
            onClick={() => setPeriod(opt.value)}
          >
            {opt.label}
          </Button>
        ))}
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Cargando demanda del menú…</p>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <TopSoldTable data={menu?.topSoldItems ?? []} period={period} />
          <ViewedVsSoldMatrix data={menu?.viewedVsSold ?? []} salesPeriod={period} />
        </div>
      )}
    </div>
  );
}
