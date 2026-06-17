import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '@/shared/api/analyticsApi';
import { KpiCards } from './KpiCards';
import { HourlyHeatmap } from './HourlyHeatmap';
import { TopSoldTable } from './TopSoldTable';
import { ViewedVsSoldMatrix } from './ViewedVsSoldMatrix';
import { ItemRankingTable } from './ItemRankingTable';
import { RealtimePanel } from './RealtimePanel';
import { TrendsSection } from './TrendsSection';
import { MenuInsightsSection } from './MenuInsightsSection';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import type { ItemAnalytics } from '@/shared/types';

type Tab = 'overview' | 'trends' | 'insights';

export function AnalyticsPage() {
  const [tab, setTab] = useState<Tab>('overview');

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['analytics', 'summary'],
    queryFn: analyticsApi.getSummary,
  });

  const { data: menu } = useQuery({
    queryKey: ['analytics', 'menu'],
    queryFn: analyticsApi.getMenu,
  });

  const { data: operations } = useQuery({
    queryKey: ['analytics', 'operations'],
    queryFn: analyticsApi.getOperations,
  });

  const { data: realtime } = useQuery({
    queryKey: ['analytics', 'realtime'],
    queryFn: analyticsApi.getRealtime,
    refetchInterval: 30000,
  });

  if (summaryLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Analytics</h1>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <Skeleton key={i} className="h-32" />
          ))}
        </div>
        <Skeleton className="h-80" />
      </div>
    );
  }

  if (!summary) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">No hay datos de analytics todavía.</p>
        <p className="text-sm text-muted-foreground mt-2">
          Los datos aparecerán cuando los clientes usen el menú o realicen pedidos.
        </p>
      </div>
    );
  }

  const topViewedAsLegacy: ItemAnalytics[] = (menu?.topViewedItems ?? []).map((item) => ({
    itemId: item.itemId,
    itemName: item.itemName,
    viewCount: item.viewCount,
    viewRate: 0,
    trending: false,
  }));

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold">Analytics</h1>
        <div className="flex gap-2">
          <Button
            variant={tab === 'overview' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setTab('overview')}
          >
            Resumen
          </Button>
          <Button
            variant={tab === 'trends' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setTab('trends')}
          >
            Tendencias
          </Button>
          <Button
            variant={tab === 'insights' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setTab('insights')}
          >
            Insights
          </Button>
        </div>
      </div>

      {tab === 'trends' ? (
        <TrendsSection />
      ) : tab === 'insights' ? (
        <MenuInsightsSection />
      ) : (
        <>
          <KpiCards data={summary} />

          {operations && (
            <HourlyHeatmap
              data={operations.ordersHeatmap}
              title="Heatmap de pedidos"
              description="Pedidos por hora y día de la semana (últimos 7 días)"
              unitLabel="pedidos"
            />
          )}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <TopSoldTable data={menu?.topSoldItems ?? []} />
            <ViewedVsSoldMatrix data={menu?.viewedVsSold ?? []} />
          </div>

          <RealtimePanel data={realtime} />

          <ItemRankingTable data={topViewedAsLegacy} />

          {operations && (
            <HourlyHeatmap
              data={operations.viewsHeatmap}
              title="Heatmap de vistas"
              description="Vistas de menú por hora (capa secundaria)"
              unitLabel="vistas"
            />
          )}
        </>
      )}
    </div>
  );
}
