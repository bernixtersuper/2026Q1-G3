import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  ResponsiveContainer,
  ComposedChart,
  Line,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { formatDate } from '@/lib/utils';
import { analyticsApi } from '@/shared/api/analyticsApi';
import { DailyViewsChart } from './DailyViewsChart';
import { FilterUsageChart } from './FilterUsageChart';
import { SectionEngagementChart } from './SectionEngagementChart';
import type { AnalyticsTrends } from '@/shared/types';

const DAY_OPTIONS = [7, 30, 90] as const;

export function TrendsSection() {
  const [days, setDays] = useState<number>(30);
  const [exportJobId, setExportJobId] = useState<string | null>(null);

  const { data: trends, isLoading } = useQuery({
    queryKey: ['analytics', 'trends', days],
    queryFn: () => analyticsApi.getTrends(days),
  });

  const exportMutation = useMutation({
    mutationFn: () => analyticsApi.startExport(days),
    onSuccess: (job) => setExportJobId(job.jobId),
  });

  const { data: exportStatus } = useQuery({
    queryKey: ['analytics', 'export', exportJobId],
    queryFn: () => analyticsApi.getExportStatus(exportJobId!),
    enabled: !!exportJobId,
    refetchInterval: (query) =>
      query.state.data?.status === 'RUNNING' ? 2000 : false,
  });

  if (isLoading || !trends) {
    return <p className="text-muted-foreground">Cargando tendencias…</p>;
  }

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
        <Button
          variant="secondary"
          size="sm"
          onClick={() => exportMutation.mutate()}
          disabled={exportMutation.isPending}
        >
          Exportar CSV
        </Button>
        {exportStatus && (
          <span className="text-sm text-muted-foreground">
            Export: {exportStatus.status}
            {exportStatus.downloadUrl && (
              <>
                {' — '}
                <a
                  href={exportStatus.downloadUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="underline"
                >
                  descargar
                </a>
              </>
            )}
          </span>
        )}
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <SummaryCard label="Pedidos" value={trends.totalOrders} />
        <SummaryCard label="Ingresos" value={`$${trends.totalRevenue.toFixed(0)}`} />
        <SummaryCard
          label="Sesiones únicas (batch)"
          value={sumUniqueSessions(trends)}
        />
        <SummaryCard label="Período" value={`${trends.days} días`} />
      </div>

      <TrendsOrdersChart data={trends} />
      <DailyViewsChart data={toDailyViews(trends)} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <FilterUsageChart data={trends.filterUsage} />
        <SectionEngagementChart data={trends.sectionEngagement} />
      </div>
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

function sumUniqueSessions(trends: AnalyticsTrends): number {
  return trends.series.reduce((sum, p) => sum + (p.uniqueMenuSessions ?? 0), 0);
}

function toDailyViews(trends: AnalyticsTrends) {
  return trends.series.map((p) => ({
    date: p.date,
    menuViews: p.menuViews,
    itemViews: p.itemViews,
  }));
}

function TrendsOrdersChart({ data }: { data: AnalyticsTrends }) {
  const chartData = data.series.map((d) => ({
    ...d,
    dateFormatted: formatDate(d.date),
    revenueNum: Number(d.revenue),
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Pedidos e ingresos</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
              <XAxis
                dataKey="dateFormatted"
                tick={{ fontSize: 12 }}
                tickLine={false}
                axisLine={false}
                interval="preserveStartEnd"
              />
              <YAxis yAxisId="left" tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
              <YAxis
                yAxisId="right"
                orientation="right"
                tick={{ fontSize: 12 }}
                tickLine={false}
                axisLine={false}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px',
                }}
              />
              <Legend />
              <Bar yAxisId="left" dataKey="orders" name="Pedidos" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
              <Line
                yAxisId="right"
                type="monotone"
                dataKey="revenueNum"
                name="Ingresos"
                stroke="#14b8a6"
                strokeWidth={2}
                dot={false}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
