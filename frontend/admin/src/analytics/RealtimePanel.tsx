import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, Legend } from 'recharts';
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
            Actividad en vivo
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-64 flex items-center justify-center text-muted-foreground">
            Cargando datos en tiempo real...
          </div>
        </CardContent>
      </Card>
    );
  }

  const chartData = data.buckets.map((bucket, index) => {
    const minutesAgo = (data.buckets.length - 1 - index) * 5;
    return {
      ...bucket,
      label: minutesAgo === 0 ? 'Ahora' : `-${minutesAgo}m`,
    };
  });

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-teal-500 animate-pulse" />
          Actividad en vivo
        </CardTitle>
        <span className="text-xs text-muted-foreground">Últimos 60 minutos</span>
      </CardHeader>
      <CardContent>
        <div className="h-48">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="colorEvents" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#14b8a6" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#14b8a6" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="colorOrders" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="label" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} />
              <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false} />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px',
                }}
              />
              <Legend />
              <Area
                type="monotone"
                dataKey="eventCount"
                name="Eventos"
                stroke="#14b8a6"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#colorEvents)"
              />
              <Area
                type="monotone"
                dataKey="orderCount"
                name="Pedidos"
                stroke="#10b981"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#colorOrders)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
        <div className="flex flex-wrap justify-center gap-4 mt-4">
          <Badge variant="secondary" className="flex items-center gap-2">
            <span className="text-muted-foreground">Eventos 5 min:</span>
            <span className="font-bold">{formatNumber(data.totalEventsLast5Min)}</span>
          </Badge>
          <Badge variant="secondary" className="flex items-center gap-2">
            <span className="text-muted-foreground">Pedidos 5 min:</span>
            <span className="font-bold">{formatNumber(data.totalOrdersLast5Min)}</span>
          </Badge>
          <Badge variant="secondary" className="flex items-center gap-2">
            <span className="text-muted-foreground">Eventos 1 h:</span>
            <span className="font-bold">{formatNumber(data.totalEventsLast60Min)}</span>
          </Badge>
          <Badge variant="secondary" className="flex items-center gap-2">
            <span className="text-muted-foreground">Pedidos 1 h:</span>
            <span className="font-bold">{formatNumber(data.totalOrdersLast60Min)}</span>
          </Badge>
        </div>
      </CardContent>
    </Card>
  );
}
