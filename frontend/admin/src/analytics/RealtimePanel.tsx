import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Activity } from 'lucide-react';
import { formatNumber } from '@/lib/utils';
import type { RealtimeAnalytics } from '@/shared/types';

interface RealtimePanelProps {
  data: RealtimeAnalytics | undefined;
}

export function RealtimePanel({ data }: RealtimePanelProps) {
  if (!data) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5 text-teal-500" />
            Live Activity
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-64 flex items-center justify-center text-muted-foreground">
            Loading realtime data...
          </div>
        </CardContent>
      </Card>
    );
  }

  const chartData = data.buckets.map((bucket, index) => {
    const minutesAgo = (data.buckets.length - 1 - index) * 5;
    return {
      ...bucket,
      label: minutesAgo === 0 ? 'Now' : `-${minutesAgo}m`,
    };
  });

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-teal-500 animate-pulse" />
          Live Activity
        </CardTitle>
        <span className="text-xs text-muted-foreground">Last 60 minutes</span>
      </CardHeader>
      <CardContent>
        <div className="h-48">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#14b8a6" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#14b8a6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis
                dataKey="label"
                tick={{ fontSize: 10 }}
                tickLine={false}
                axisLine={false}
              />
              <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false} />
              <Tooltip
                formatter={(value: number) => [formatNumber(value), 'Events']}
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px',
                }}
              />
              <Area
                type="monotone"
                dataKey="count"
                stroke="#14b8a6"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#colorCount)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
        <div className="flex justify-center gap-4 mt-4">
          <Badge variant="secondary" className="flex items-center gap-2">
            <span className="text-muted-foreground">Last 5 min:</span>
            <span className="font-bold">{formatNumber(data.totalLast5Min)}</span>
          </Badge>
          <Badge variant="secondary" className="flex items-center gap-2">
            <span className="text-muted-foreground">Last hour:</span>
            <span className="font-bold">{formatNumber(data.totalLast60Min)}</span>
          </Badge>
        </div>
      </CardContent>
    </Card>
  );
}
