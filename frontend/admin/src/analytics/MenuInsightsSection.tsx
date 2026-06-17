import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { analyticsApi } from '@/shared/api/analyticsApi';
import { BoughtTogetherTable } from './BoughtTogetherTable';
import { MenuEngineeringChart } from './MenuEngineeringChart';
import { TopModifiersTable } from './TopModifiersTable';
import { formatNumber } from '@/lib/utils';

const DAY_OPTIONS = [7, 30, 90] as const;

export function MenuInsightsSection() {
  const [days, setDays] = useState<number>(30);

  const { data: insights, isLoading } = useQuery({
    queryKey: ['analytics', 'menu-insights', days],
    queryFn: () => analyticsApi.getMenuInsights(days),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        {DAY_OPTIONS.map((d) => (
          <Button
            key={d}
            variant={days === d ? 'default' : 'outline'}
            size="sm"
            onClick={() => setDays(d)}
          >
            {d} días
          </Button>
        ))}
      </div>

      {isLoading || !insights ? (
        <p className="text-muted-foreground">Cargando insights…</p>
      ) : (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
            <SummaryCard label="Pedidos analizados" value={formatNumber(insights.ordersAnalyzed)} />
            <SummaryCard label="Tamaño medio del pedido" value={`${insights.avgBasketSize.toFixed(1)} ítems`} />
            <SummaryCard label="Platos distintos vendidos" value={formatNumber(insights.distinctItemsSold)} />
          </div>

          <BoughtTogetherTable data={insights.frequentlyBoughtTogether} />
          <MenuEngineeringChart data={insights.menuEngineering} />
          <TopModifiersTable data={insights.topModifiers} />
        </>
      )}
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Card>
      <CardContent className="pt-6">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="text-2xl font-bold">{value}</p>
      </CardContent>
    </Card>
  );
}
