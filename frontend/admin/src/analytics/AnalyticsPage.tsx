import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '@/shared/api/analyticsApi';
import { KpiCards } from './KpiCards';
import { DailyViewsChart } from './DailyViewsChart';
import { HourlyHeatmap } from './HourlyHeatmap';
import { ItemRankingTable } from './ItemRankingTable';
import { FilterUsageChart } from './FilterUsageChart';
import { SectionEngagementChart } from './SectionEngagementChart';
import { RealtimePanel } from './RealtimePanel';
import { Skeleton } from '@/components/ui/skeleton';

export function AnalyticsPage() {
  const { data: analytics, isLoading } = useQuery({
    queryKey: ['analytics'],
    queryFn: analyticsApi.getDashboard,
  });

  const { data: realtime } = useQuery({
    queryKey: ['analytics', 'realtime'],
    queryFn: analyticsApi.getRealtime,
    refetchInterval: 30000,
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Analytics</h1>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <Skeleton key={i} className="h-32" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-80" />
          <Skeleton className="h-80" />
        </div>
      </div>
    );
  }

  if (!analytics) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">No analytics data available yet.</p>
        <p className="text-sm text-muted-foreground mt-2">
          Analytics will appear once customers start viewing your menu.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Analytics</h1>

      <KpiCards data={analytics} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <DailyViewsChart data={analytics.dailyViews} />
        <RealtimePanel data={realtime} />
      </div>

      <HourlyHeatmap data={analytics.hourlyHeatmap} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ItemRankingTable data={analytics.topItems} />
        <FilterUsageChart data={analytics.filterUsage} />
      </div>

      <SectionEngagementChart data={analytics.sectionEngagement} />
    </div>
  );
}
